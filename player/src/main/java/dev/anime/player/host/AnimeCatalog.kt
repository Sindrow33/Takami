package dev.anime.player.host

import java.io.File

/**
 * Каталог аниме для экрана-обёртки [AnimeScreen].
 *
 * Ровно та же модель, что у манги в `LocalLibrary`: то, что лежит на устройстве,
 * плюс — до появления сетевых источников — несколько публичных тестовых потоков,
 * чтобы плеер можно было проверить на живом устройстве без парсера.
 *
 * Раскладка локальных файлов:
 * ```
 * <files>/anime/<Название>/01 - Серия.mp4
 * <files>/anime/<Название>/02.mkv
 * ```
 *
 * Сетевой источник эпизодов приедет отдельной реализацией и встанет в тот же
 * список — экран разницы не увидит.
 */
object AnimeCatalog {

    /** Контейнеры, которые умеет Media3 без дополнительных расширений. */
    val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "webm", "m4v", "ts", "m3u8")

    data class Episode(
        val id: String,
        val title: String,
        val number: Int,
        val url: String,
        val isLocal: Boolean,
    )

    data class Title(
        val id: String,
        val name: String,
        val episodes: List<Episode>,
    ) {
        val episodeCount: Int get() = episodes.size
    }

    /**
     * Папка с аниме. [base] — либо внутренний `filesDir`, либо папка, выбранная
     * пользователем: внутренняя память приложения с телефона недоступна, поэтому
     * положить туда файлы вручную нельзя, и корень обязан быть переопределяемым.
     */
    fun rootDir(base: File): File = File(base, "anime").apply { mkdirs() }

    /**
     * Номер серии из имени файла: первое целое число в начале имени, иначе первое
     * число вообще. `"01 - Начало.mp4"` -> 1, `"Серия 12.mkv"` -> 12, `"intro.mp4"` -> null.
     */
    fun episodeNumber(fileName: String): Int? {
        val base = fileName.substringBeforeLast('.')
        val leading = Regex("""^\s*(\d{1,4})""").find(base)?.groupValues?.get(1)
        val any = leading ?: Regex("""(\d{1,4})""").find(base)?.groupValues?.get(1)
        return any?.toIntOrNull()
    }

    fun isVideo(fileName: String): Boolean =
        fileName.substringAfterLast('.', "").lowercase() in VIDEO_EXTENSIONS

    /**
     * Порядок серий: по распознанному номеру, затем по имени. Файлы без номера
     * уезжают в конец, а не в начало — иначе «intro.mp4» становился первой серией.
     */
    fun sortEpisodes(episodes: List<Episode>): List<Episode> =
        episodes.sortedWith(
            compareBy(
                { if (it.number > 0) 0 else 1 },
                { if (it.number > 0) it.number else Int.MAX_VALUE },
                { it.title.lowercase() },
            )
        )

    fun scan(base: File): List<Title> {
        val root = rootDir(base)
        val dirs = root.listFiles { f: File -> f.isDirectory }?.sortedBy { it.name } ?: return emptyList()
        return dirs.mapNotNull { dir ->
            val files = dir.listFiles { f: File -> f.isFile && isVideo(f.name) } ?: return@mapNotNull null
            if (files.isEmpty()) return@mapNotNull null
            val episodes = files.map { f ->
                Episode(
                    id = dir.name + "/" + f.name,
                    title = f.nameWithoutExtension,
                    number = episodeNumber(f.name) ?: 0,
                    url = f.toURI().toString(),
                    isLocal = true,
                )
            }
            Title(id = dir.name, name = dir.name, episodes = sortEpisodes(episodes))
        }
    }

    /**
     * Публичные тестовые потоки. Нужны, пока каталога и сетевых источников нет:
     * без них экран аниме нечем проверить на устройстве.
     */
    fun demoStreams(): List<Title> = listOf(
        Title(
            id = "demo-streams",
            name = "Тестовые потоки",
            episodes = listOf(
                Episode(
                    id = "demo/mp4",
                    title = "MP4 · Big Buck Bunny",
                    number = 1,
                    url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    isLocal = false,
                ),
                Episode(
                    id = "demo/hls",
                    title = "HLS · многодорожечный поток",
                    number = 2,
                    url = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                    isLocal = false,
                ),
            ),
        )
    )
}
