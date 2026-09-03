package com.mangareader.translate.api

import kotlinx.coroutines.flow.Flow

/**
 * Откуда берутся веса моделей перевода.
 *
 * Интерфейс живёт здесь, а не в реализации, ровно по той же причине, по
 * которой здесь живут репозитории: читалке нужно знать, доступен ли
 * перевод, и не нужно знать ни про Play Feature Delivery, ни про HTTP,
 * ни про файлы на диске.
 *
 * Веса в APK не кладутся принципиально. Нативные библиотеки перевода уже
 * весят по 17 МБ на архитектуру, и модели поверх них сделали бы сборку
 * такой, которую никто не станет ставить — то есть перевод, встроенный в
 * APK, не работает не потому, что он плох, а потому, что до него не
 * доходит установка.
 */
interface TranslationModelProvider {

    /**
     * Текущее состояние. Поток, а не разовый вызов: состояние меняется
     * само — загрузка идёт, сеть пропадает, место кончается, — и
     * интерфейс обязан это отражать без того, чтобы кто-то опрашивал.
     */
    val availability: Flow<TranslationAvailability>

    /** Мгновенный снимок — для мест, где поток избыточен. */
    fun current(): TranslationAvailability

    /**
     * Начинает загрузку. Повторный вызов во время загрузки ничего не
     * делает: две параллельные загрузки одного файла — это удвоенный
     * трафик и гонка за один и тот же файл на диске.
     *
     * @param allowMetered качать ли по мобильной сети. Решение
     *   пользователя, а не догадка: без явного согласия загрузка на
     *   сотню мегабайт по мобильному тарифу — это счёт, которого он не
     *   ждал.
     */
    suspend fun download(allowMetered: Boolean)

    /** Отменяет загрузку. Частично скачанное удаляется. */
    fun cancelDownload()

    /**
     * Путь к файлу модели, или null если её нет.
     *
     * Возвращает именно путь, а не байты: ONNX Runtime открывает файл
     * сам и мапит его в память, а чтение сотни мегабайт в heap ради
     * последующей записи во временный файл — это лишняя копия и почти
     * гарантированный OOM на слабом устройстве.
     */
    fun modelPath(model: TranslationModel): String?

    /** Удаляет скачанное — пункт «освободить место» в настройках. */
    suspend fun deleteDownloaded()
}

/**
 * Какие модели нужны переводу.
 *
 * Перечисление, а не строки: опечатка в имени файла модели проявилась бы
 * только в рантайме и только на устройстве, где загрузка уже прошла.
 */
enum class TranslationModel(val fileName: String, val approxBytes: Long) {
    /** Поиск текстовых блоков на странице. */
    TEXT_DETECTOR("comic_text_detector.onnx", 24L * 1024 * 1024),

    /** Затирание оригинального текста для режима «заменять». */
    INPAINTER("lama_fp32.onnx", 51L * 1024 * 1024);

    companion object {
        val totalBytes: Long get() = entries.sumOf { it.approxBytes }
    }
}

/**
 * Провайдер-заглушка: моделей нет и взять их неоткуда.
 *
 * Существует не как временная затычка, а как честный ответ на реальную
 * ситуацию: весов в проекте пока нет. Всё остальное — интерфейс,
 * состояния, поведение читалки при недоступном переводе — от этого не
 * зависит и проверяется уже сейчас. Когда веса появятся, меняется одна
 * привязка, а не логика.
 */
class UnavailableModelProvider(
    private val reason: String = "Модели перевода пока не поставляются",
) : TranslationModelProvider {

    private val state = TranslationAvailability.DeviceUnsupported(reason)

    override val availability: Flow<TranslationAvailability> =
        kotlinx.coroutines.flow.flowOf(state)

    override fun current(): TranslationAvailability = state

    override suspend fun download(allowMetered: Boolean) = Unit

    override fun cancelDownload() = Unit

    override fun modelPath(model: TranslationModel): String? = null

    override suspend fun deleteDownloaded() = Unit
}
