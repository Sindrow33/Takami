package dev.takami.app.source

import android.content.Context
import com.mangareader.core.model.ChapterInfo
import com.mangareader.feature.reader.ReaderSourceRegistry
import core.engine.ParseEngine
import core.model.SourceConfig
import core.net.OkHttpClientAdapter
import core.store.SourceRegistry
import dev.takami.source.web.HttpPageFetcher
import dev.takami.source.web.WebMangaPageSource
import java.io.File

/**
 * Регистрация сетевого источника страниц в читалке.
 *
 * Место, где сходятся автопарсер и читалка: движок парсера живёт в
 * `:autoheal`, контракт страниц — в `:source:web`, а знание о том, где
 * на устройстве лежит состояние и кеш, есть только у приложения.
 */
object WebSources {

    /**
     * Один `SourceRegistry` на процесс — он владеет файлами конфигов,
     * здоровья и бюджета. Второй экземпляр поверх тех же файлов означал
     * бы две несогласованные копии состояния ремонта.
     */
    private var shared: SourceRegistry? = null

    private fun registry(context: Context): SourceRegistry =
        shared ?: SourceRegistry(File(context.filesDir, "sources")).also { shared = it }

    fun sourceIdFor(config: SourceConfig, mangaId: String) = "web:${config.host}:$mangaId"

    /**
     * Регистрирует источник, если он ещё не зарегистрирован, и
     * возвращает его `sourceId` для `ReaderParams`.
     *
     * @param chapterUrlOf URL главы по её id — знание тайтла, не источника.
     * @param chapterIdsOf порядок глав для «следующая/предыдущая».
     */
    fun register(
        context: Context,
        config: SourceConfig,
        mangaId: String,
        chapterUrlOf: suspend (String) -> String,
        chapterIdsOf: suspend (String) -> List<String>,
        chapterLookup: suspend (String) -> ChapterInfo,
    ): String {
        val sourceId = sourceIdFor(config, mangaId)
        if (ReaderSourceRegistry.isRegistered(sourceId)) return sourceId

        val registry = registry(context)
        val client = OkHttpClientAdapter()

        val source = WebMangaPageSource(
            engine = ParseEngine(registry),
            fetcher = HttpPageFetcher(client),
            bundledConfig = config,
            /*
             * Скачанные страницы — в cacheDir, отдельно от дискового
             * кеша движка: система вправе вычистить и то и другое, а
             * пользовательские главы в filesDir не тронет.
             */
            downloadDir = File(context.cacheDir, "web-pages").apply { mkdirs() },
            chapterUrlOf = chapterUrlOf,
            chapterIdsOf = chapterIdsOf,
            /*
             * Отремонтированный конфиг обязан лечь на диск: иначе
             * следующая глава пойдёт по сломанным селекторам и будет
             * лечиться заново, каждый раз.
             */
            persistConfig = { repaired -> registry.persist(repaired) },
        )

        ReaderSourceRegistry.register(
            sourceId = sourceId,
            source = source,
            chapterLookup = chapterLookup,
        )
        return sourceId
    }
}
