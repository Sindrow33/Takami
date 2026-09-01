package dev.anime.player.enhance

/**
 * Можно ли включить ИИ-функции для этой серии.
 *
 * Правило одно и жёсткое: **только для скачанного файла**. По сетевому потоку
 * звуковую дорожку не достать иначе как перекачиванием серии по кругу —
 * `MediaExtractor.seekTo` по HLS-сегментам означает повторную загрузку каждого
 * окна. Поэтому это не «пока не поддерживается», а осознанная граница.
 *
 * Чистые функции над строкой URL, чтобы правило проверялось тестом: решение
 * «показывать ли кнопку» иначе разъезжается с решением «сработает ли она».
 */
object EnhancerAvailability {

    /** Схемы, у которых есть локальные байты и произвольный доступ. */
    private val LOCAL_SCHEMES = setOf("file", "content")

    fun isLocal(url: String): Boolean {
        val scheme = url.substringBefore(':', missingDelimiterValue = "").lowercase()
        // Путь без схемы — это тоже локальный файл.
        if (scheme.isEmpty()) return url.startsWith("/")
        return scheme in LOCAL_SCHEMES
    }

    /** Доступны ли ИИ-субтитры и озвучка. */
    fun isAvailable(url: String): Boolean = isLocal(url)

    /** Что показать пользователю, когда недоступно. */
    fun unavailableReason(url: String): String? =
        if (isAvailable(url)) null
        else "ИИ-субтитры и озвучка работают только для скачанных серий: " +
            "по онлайн-потоку звуковую дорожку не получить."
}
