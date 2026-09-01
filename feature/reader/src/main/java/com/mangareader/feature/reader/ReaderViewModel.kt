package com.mangareader.feature.reader

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mangareader.core.model.ChapterInfo
import com.mangareader.core.model.FailureKind
import com.mangareader.core.model.MangaPageSource
import com.mangareader.core.model.PageLoad
import com.mangareader.core.model.PageRef
import com.mangareader.core.model.ReaderEvent
import com.mangareader.reader.engine.cache.DiskLruPageCache
import com.mangareader.reader.engine.cache.PagePrefetcher
import com.mangareader.reader.engine.decode.PageDecoder
import com.mangareader.reader.engine.feed.FeedController
import com.mangareader.reader.engine.feed.FeedEvent
import com.mangareader.reader.engine.layout.FeedItem
import com.mangareader.reader.engine.layout.PageState
import com.mangareader.reader.engine.settings.ReaderSettings
import com.mangareader.reader.engine.settings.ReaderSettingsStore
import com.mangareader.translate.api.TranslationMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Состояние экрана читалки, поднятое из [FeedController].
 *
 * ViewModel сознательно не знает про библиотеку и прогресс чтения: всё,
 * что должно долететь до приложения, уходит в [ReaderEventBus]
 * (INTEGRATION.md §2). Слой перевода тут пока не подключён — режим
 * хранится и прокидывается в UI, оркестратор включается вместе с DI-графом
 * (INTEGRATION.md §7).
 */
data class ReaderUiState(
    val items: List<FeedItem> = emptyList(),
    val currentChapterId: String? = null,
    val currentChapterNumber: Float? = null,
    val currentPage: Int = 0,
    val totalPagesInChapter: Int = 0,
    val translationMode: TranslationMode = TranslationMode.ORIGINAL,
    val chromeVisible: Boolean = false,
    val settingsVisible: Boolean = false,
    val settings: ReaderSettings = ReaderSettings.DEFAULT,
    val loading: Boolean = true,
    val error: String? = null,
    /** Запрошенный слайдером переход на элемент ленты; одноразовый. */
    val pendingScrollIndex: Int? = null,
) {
    /**
     * Индексы элементов текущей главы в ленте — область, по которой
     * ездит слайдер. Слайдер намеренно ограничен ОДНОЙ главой, хотя
     * лента непрерывна: иначе его цена деления зависела бы от того,
     * сколько глав успело догрузиться, и позиция ручки прыгала бы сама
     * по себе при догрузке.
     */
    fun chapterPageIndices(): List<Int> =
        items.indices.filter { i ->
            (items[i] as? FeedItem.Page)?.chapterId == currentChapterId
        }
}

class ReaderViewModel(
    private val source: MangaPageSource,
    private val chapterLookup: suspend (String) -> ChapterInfo,
    private val eventBus: ReaderEventBus,
    private val settingsStore: ReaderSettingsStore = ReaderSettingsStore.None,
    private val seriesId: String = "",
    /**
     * Дисковый кеш страниц между сессиями. Без него каждое открытие
     * читалки качает главу заново — на локальных файлах незаметно, на
     * сетевом источнике это полная перезагрузка.
     */
    private val diskCache: DiskLruPageCache? = null,
) : ViewModel() {

    private val feed = FeedController(
        source = source,
        chapterLookup = chapterLookup,
        scope = viewModelScope,
    )

    /**
     * Упреждающая загрузка байтов страниц за пределами окна
     * декодирования. Декодировать заранее нельзя — битмапы съедят
     * память, а вот скачать можно и нужно: на сетевом источнике без
     * этого каждая страница ждёт полный round-trip в момент, когда
     * пользователь до неё домотал.
     *
     * Класс был написан, но не подключён ни к чему — до этой правки
     * упреждающей загрузки не существовало.
     */
    private val prefetcher = PagePrefetcher(
        source = source,
        scope = viewModelScope,
        diskCache = diskCache,
    )

    private val _state = MutableStateFlow(ReaderUiState())
    val state: StateFlow<ReaderUiState> = _state.asStateFlow()

    /** Декодированные битмапы окна (±3 страницы), ключ — id страницы. */
    private val bitmaps = ConcurrentHashMap<String, Bitmap>()
    private val loadedFiles = ConcurrentHashMap<String, File>()
    private val decodeRequested = ConcurrentHashMap.newKeySet<String>()

    private var lastCenterIndex = -1
    private var startPageRequested = 0

    init {
        if (seriesId.isNotEmpty()) {
            _state.value = _state.value.copy(settings = settingsStore.load(seriesId))
        }
        viewModelScope.launch {
            feed.state.collect { feedState ->
                val current = feedState.currentChapterId
                val pagesInChapter = feedState.items.count { it is FeedItem.Page && it.chapterId == current }
                _state.value = _state.value.copy(
                    items = feedState.items,
                    currentChapterId = current,
                    currentChapterNumber = (feedState.items.firstOrNull {
                        it is FeedItem.Page && it.chapterId == current
                    } as? FeedItem.Page)?.chapterNumber,
                    totalPagesInChapter = pagesInChapter,
                    loading = feedState.items.isEmpty(),
                )
            }
        }
        viewModelScope.launch {
            feed.events.collect { event ->
                when (event) {
                    is FeedEvent.ChapterCompleted ->
                        eventBus.emit(ReaderEvent.ChapterCompleted(event.chapterId))
                    is FeedEvent.ChapterChanged ->
                        eventBus.emit(ReaderEvent.ChapterChanged(event.fromId, event.toId))
                }
            }
        }
    }

    /** Последняя открытая глава — нужна кнопке «Повторить». */
    private var currentRequest: Pair<String, Int>? = null

    fun retry() {
        val (chapterId, startPage) = currentRequest ?: return
        _state.value = _state.value.copy(error = null, loading = true)
        open(chapterId, startPage)
    }

    fun open(chapterId: String, startPage: Int) {
        startPageRequested = startPage
        currentRequest = chapterId to startPage
        viewModelScope.launch {
            runCatching { feed.start(chapterId, startPage) }
                .onFailure { t ->
                    _state.value = _state.value.copy(
                        loading = false,
                        error = t.message ?: "Не удалось открыть главу",
                    )
                    eventBus.emit(
                        ReaderEvent.Failure(FailureKind.CHAPTER_LOAD, t.message ?: t.toString())
                    )
                }
            // Позиция открытия: элемент ленты со стартовой страницей.
            // До этой правки startPage записывался и не читался — глава
            // всегда открывалась с первой страницы, а сохранённый
            // прогресс никуда не применялся.
            val startIndex = indexOfPage(chapterId, startPage)
            ensureWindowDecoded(startIndex)
            if (startPage > 0) {
                _state.value = _state.value.copy(
                    pendingScrollIndex = startIndex,
                    currentPage = startPage,
                )
            }
        }
    }

    /** Вызывается вьюхой, когда центр вьюпорта устоялся на элементе [centerIndex]. */
    fun onViewportSettled(centerIndex: Int) {
        if (centerIndex == lastCenterIndex) return
        lastCenterIndex = centerIndex
        feed.onViewportMoved(centerIndex)
        ensureWindowDecoded(centerIndex)

        val page = _state.value.items.getOrNull(centerIndex) as? FeedItem.Page ?: return
        val pageNumber = page.pageRef.index
        val total = _state.value.items.count { it is FeedItem.Page && it.chapterId == page.chapterId }
        _state.value = _state.value.copy(currentPage = pageNumber, totalPagesInChapter = total)
        viewModelScope.launch {
            eventBus.emit(ReaderEvent.PageRead(page.chapterId, pageNumber, total))
        }
    }

    fun toggleChrome() {
        val visible = !_state.value.chromeVisible
        // Закрытие хрома закрывает и настройки: иначе шит остаётся
        // висеть над лентой без панели, из которой он открыт.
        _state.value = _state.value.copy(
            chromeVisible = visible,
            settingsVisible = if (visible) _state.value.settingsVisible else false,
        )
    }

    fun openSettings() {
        _state.value = _state.value.copy(settingsVisible = true)
    }

    fun closeSettings() {
        _state.value = _state.value.copy(settingsVisible = false)
    }

    fun updateSettings(transform: (ReaderSettings) -> ReaderSettings) {
        val updated = transform(_state.value.settings)
        _state.value = _state.value.copy(settings = updated)
        // Сохраняем сразу: пользователь меняет режим чтения один раз и
        // ждёт, что он останется. Запись редкая, поэтому не батчим.
        if (seriesId.isNotEmpty()) settingsStore.save(seriesId, updated)
    }

    /**
     * Переход на страницу [pageIndex] текущей главы — вызывается
     * слайдером. Индекс страницы переводится в индекс элемента ленты:
     * лента сквозная и содержит соседние главы, поэтому номер страницы
     * и позиция в списке — разные вещи.
     */
    fun seekToPage(pageIndex: Int) {
        val indices = _state.value.chapterPageIndices()
        if (indices.isEmpty()) return
        val target = indices.getOrNull(pageIndex.coerceIn(0, indices.lastIndex)) ?: return
        _state.value = _state.value.copy(pendingScrollIndex = target, currentPage = pageIndex)
    }

    fun onScrollHandled() {
        if (_state.value.pendingScrollIndex != null) {
            _state.value = _state.value.copy(pendingScrollIndex = null)
        }
    }

    fun setTranslationMode(mode: TranslationMode) {
        _state.value = _state.value.copy(translationMode = mode)
    }

    fun bitmapFor(item: FeedItem.Page): Bitmap? = bitmaps[item.pageRef.id]

    /** Индекс элемента ленты для страницы [pageIndex] главы [chapterId]. */
    private fun indexOfPage(chapterId: String, pageIndex: Int): Int =
        _state.value.items.indexOfFirst {
            it is FeedItem.Page && it.chapterId == chapterId && it.pageRef.index == pageIndex
        }.coerceAtLeast(0)

    /**
     * Держит декодированным окно ±[FeedController.DECODE_WINDOW_RADIUS]
     * вокруг центра и освобождает всё, что за его пределами. Измеренная
     * высота уходит обратно в контроллер, чтобы вёрстка перестала быть
     * оценочной и лента не прыгала.
     */
    private fun ensureWindowDecoded(centerIndex: Int) {
        val items = _state.value.items
        if (items.isEmpty()) return
        val radius = FeedController.DECODE_WINDOW_RADIUS
        val range = (centerIndex - radius)..(centerIndex + radius)

        val keepIds = range.mapNotNull { (items.getOrNull(it) as? FeedItem.Page)?.pageRef?.id }.toSet()
        bitmaps.keys.filter { it !in keepIds }.forEach { id ->
            bitmaps.remove(id)?.let { if (!it.isRecycled) it.recycle() }
            decodeRequested.remove(id)
        }

        for (index in range) {
            val page = items.getOrNull(index) as? FeedItem.Page ?: continue
            if (!decodeRequested.add(page.pageRef.id)) continue
            viewModelScope.launch(Dispatchers.IO) { loadAndDecode(index, page.pageRef) }
        }

        prefetchAround(centerIndex, items)
    }

    /**
     * Качает вперёд дальше, чем декодирует: PREFETCH_RADIUS страниц по
     * ходу чтения. Загрузки, ушедшие далеко за спину, отменяются — при
     * быстрой перемотке они бы забивали канал и тормозили ту страницу,
     * на которую пользователь смотрит прямо сейчас.
     */
    private fun prefetchAround(centerIndex: Int, items: List<FeedItem>) {
        val range = (centerIndex - 1)..(centerIndex + PREFETCH_RADIUS)
        val wanted = LinkedHashMap<String, PageRef>()
        for (index in range) {
            val page = items.getOrNull(index) as? FeedItem.Page ?: continue
            if (loadedFiles.containsKey(page.pageRef.id)) continue
            wanted[page.pageRef.id] = page.pageRef
        }

        prefetcher.trimTo(wanted.keys)
        // Приоритет по удалённости от центра: ближайшая страница важнее.
        wanted.values.forEachIndexed { offset, pageRef ->
            prefetcher.request(pageRef, priority = offset)
        }
    }

    private suspend fun loadAndDecode(globalIndex: Int, pageRef: PageRef) {
        // Порядок важен: память → упреждающая загрузка → диск → сеть.
        // Диск проверяется по URI И заголовкам страницы: один и тот же
        // URL с разным Referer у хостингов картинок отдаёт разное —
        // настоящую страницу или заглушку с 403.
        val cached = loadedFiles[pageRef.id]
            ?: prefetcher.cachedFile(pageRef.id)
            ?: diskCache?.get(pageRef)
        val file = cached ?: runCatching { fetch(pageRef) }.getOrElse { t ->
            decodeRequested.remove(pageRef.id)
            feed.reportPageState(globalIndex, PageState.ERROR)
            eventBus.emit(ReaderEvent.Failure(FailureKind.PAGE_LOAD, t.message ?: t.toString()))
            return
        }
        loadedFiles[pageRef.id] = file
        // Кладём в дисковый кеш только то, что скачано сейчас: файл,
        // пришедший из самого кеша или из локального источника,
        // копировать в него бессмысленно.
        if (cached == null) cacheOnDisk(pageRef, file)

        runCatching { PageDecoder.decodeForDisplay(file) }
            .onSuccess { bitmap ->
                bitmaps[pageRef.id] = bitmap
                feed.reportPageState(globalIndex, PageState.DECODED)
                val bounds = runCatching { PageDecoder.readBounds(file) }.getOrNull()
                if (bounds != null && bounds.width > 0) {
                    val width = layoutWidthPx
                    if (width > 0) {
                        feed.reportMeasuredHeight(
                            globalIndex,
                            (width.toFloat() * bounds.height / bounds.width).toInt(),
                        )
                    }
                }
            }
            .onFailure { t ->
                decodeRequested.remove(pageRef.id)
                feed.reportPageState(globalIndex, PageState.ERROR)
                eventBus.emit(ReaderEvent.Failure(FailureKind.PAGE_LOAD, t.message ?: t.toString()))
            }
    }

    /**
     * Сохраняет скачанную страницу на диск.
     *
     * Копируем только то, что кеш иначе потеряет. Не копируем:
     *  - локальные источники (папка, CBZ) — файл и так на устройстве;
     *  - файлы, уже лежащие в поднадзорном каталоге: сетевой источник
     *    сохраняет страницы сам, и копия означала бы два экземпляра
     *    каждой страницы вместо одного.
     *
     * Во втором случае вместо копирования просто поджимаем общий объём
     * под лимит — у самого источника ни лимита, ни вытеснения нет.
     */
    private suspend fun cacheOnDisk(pageRef: PageRef, file: File) {
        val cache = diskCache ?: return
        if (!pageRef.uri.startsWith("http", ignoreCase = true)) return

        if (cache.isManaged(file)) {
            runCatching { cache.enforceLimit() }
            return
        }
        // put вернёт null, если байты не похожи на изображение — тогда
        // в кеш не попадёт ни битое тело, ни HTML с капчей под кодом 200.
        runCatching { cache.put(pageRef, file.readBytes()) }
    }

    /**
     * Ширина ленты в px; вьюха сообщает её после первого layout и при
     * повороте. Прокидывается в [FeedController] — от неё зависит оценка
     * высоты ещё не декодированных страниц, а это основной путь для
     * источников без width/height в PageRef.
     */
    @Volatile
    var layoutWidthPx: Int = 0
        private set

    fun onLayoutWidth(widthPx: Int) {
        if (widthPx <= 0 || widthPx == layoutWidthPx) return
        layoutWidthPx = widthPx
        feed.updateLayoutWidth(widthPx)
        if (lastCenterIndex >= 0) ensureWindowDecoded(lastCenterIndex)
    }

    private suspend fun fetch(pageRef: PageRef): File {
        var result: File? = null
        var failure: Throwable? = null
        source.open(pageRef).collect { load ->
            when (load) {
                is PageLoad.Done -> result = load.file
                is PageLoad.Error -> failure = load.cause
                is PageLoad.Progress -> Unit
            }
        }
        failure?.let { throw it }
        return result ?: error("Источник не отдал файл страницы: ${pageRef.id}")
    }

    override fun onCleared() {
        prefetcher.cancelAll()
        bitmaps.values.forEach { if (!it.isRecycled) it.recycle() }
        bitmaps.clear()
        super.onCleared()
    }

    private companion object {
        /** Насколько страниц вперёд качаем байты (декодируем меньше). */
        const val PREFETCH_RADIUS = 8
    }

    class Factory(
        private val source: MangaPageSource,
        private val chapterLookup: suspend (String) -> ChapterInfo,
        private val eventBus: ReaderEventBus,
        private val settingsStore: ReaderSettingsStore = ReaderSettingsStore.None,
        private val seriesId: String = "",
        private val diskCache: DiskLruPageCache? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ReaderViewModel(
                source, chapterLookup, eventBus, settingsStore, seriesId, diskCache,
            ) as T
    }
}
