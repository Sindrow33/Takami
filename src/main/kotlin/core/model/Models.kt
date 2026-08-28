package core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/* =====================================================================
 * 1. ЧТО ЗА КОНТЕНТ
 * ===================================================================== */

@Serializable
enum class MediaKind { VIDEO, MANGA, NOVEL }

/**
 * Профиль описывает ожидания от типа контента: какие поля обязательны,
 * как называется единица (серия/глава), в каких пределах бывает список.
 * Валидация (файл 10) целиком опирается на профиль — без него она не знает,
 * что считать нормой.
 */
@Serializable
data class ContentProfile(
    val kind: MediaKind,
    val requiredFields: Set<String>,
    val optionalFields: Set<String> = emptySet(),
    val unitNoun: String,
    val unitNounPlural: String,
    val listingMin: Int = 3,
    val listingMax: Int = 200,
    val unitsMin: Int = 1,
    val unitsMax: Int = 5000,
) {
    val knownFields: Set<String> get() = requiredFields + optionalFields

    val listingSizeRange: IntRange get() = listingMin..listingMax
    val unitsSizeRange: IntRange get() = unitsMin..unitsMax

    companion object {
        val VIDEO = ContentProfile(
            kind = MediaKind.VIDEO,
            requiredFields = setOf("title", "url"),
            optionalFields = setOf("cover", "year", "episodes", "status", "rating", "genres"),
            unitNoun = "серия",
            unitNounPlural = "серии",
            unitsMin = 1, unitsMax = 2000,
        )

        val MANGA = ContentProfile(
            kind = MediaKind.MANGA,
            requiredFields = setOf("title", "url"),
            optionalFields = setOf("cover", "chapters", "status", "author", "genres"),
            unitNoun = "глава",
            unitNounPlural = "главы",
            unitsMin = 1, unitsMax = 5000,
        )

        val NOVEL = MANGA.copy(kind = MediaKind.NOVEL)

        fun of(kind: MediaKind): ContentProfile = when (kind) {
            MediaKind.VIDEO -> VIDEO
            MediaKind.MANGA -> MANGA
            MediaKind.NOVEL -> NOVEL
        }
    }
}

/* =====================================================================
 * 2. ДАННЫЕ
 * ===================================================================== */

/**
 * Общий интерфейс для всего, что валидатор проверяет по колонкам.
 * Благодаря ему checkListing/checkEntry/checkUnits не знают о конкретных
 * типах и работают с любым набором полей — включая те, которых нет
 * в модели, но которые пользователь назначил в пикере.
 */
interface FieldBearing {
    fun field(name: String): String?
    fun fieldNames(): Set<String>
}

@Serializable
data class MediaItem(
    /** Стабильный ключ для дедупликации. Обычно нормализованный url. */
    val key: String,
    val title: String,
    val url: String,
    val cover: String? = null,
    val extras: Map<String, String> = emptyMap(),
) : FieldBearing {
    override fun field(name: String): String? = when (name) {
        "key" -> key
        "title" -> title
        "url" -> url
        "cover" -> cover
        else -> extras[name]
    }

    override fun fieldNames(): Set<String> = BASE + extras.keys

    companion object { private val BASE = setOf("key", "title", "url", "cover") }
}

@Serializable
data class MediaEntry(
    val key: String,
    val title: String,
    val url: String,
    val cover: String? = null,
    val description: String? = null,
    val units: List<MediaUnit> = emptyList(),
    val extras: Map<String, String> = emptyMap(),
) : FieldBearing {
    override fun field(name: String): String? = when (name) {
        "key" -> key
        "title" -> title
        "url" -> url
        "cover" -> cover
        "description" -> description
        else -> extras[name]
    }

    override fun fieldNames(): Set<String> = BASE + extras.keys

    companion object { private val BASE = setOf("key", "title", "url", "cover", "description") }
}

/** Серия, глава, том — одна воспроизводимая единица. */
@Serializable
data class MediaUnit(
    val key: String,
    val url: String,
    val number: Double? = null,
    val name: String? = null,
    val extras: Map<String, String> = emptyMap(),
) : FieldBearing {
    override fun field(name: String): String? = when (name) {
        "key" -> key
        "url" -> url
        "number" -> number?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }
        "name" -> name
        else -> extras[name]
    }

    override fun fieldNames(): Set<String> = BASE + extras.keys

    /** Для UI: «Серия 12» или «Серия 12 — Название». */
    fun label(profile: ContentProfile): String {
        val n = number?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }
        return when {
            n != null && !name.isNullOrBlank() -> "${profile.unitNoun.replaceFirstChar(Char::uppercase)} $n — $name"
            n != null -> "${profile.unitNoun.replaceFirstChar(Char::uppercase)} $n"
            !name.isNullOrBlank() -> name
            else -> url.substringAfterLast('/')
        }
    }

    companion object { private val BASE = setOf("key", "url", "number", "name") }
}

/* =====================================================================
 * 3. ТЕРМИНАЛЬНЫЙ КОНТЕНТ
 * ===================================================================== */

@Serializable
enum class StreamKind { HLS, DASH, PROGRESSIVE }

@Serializable
data class VideoStream(
    val url: String,
    val kind: StreamKind,
    val quality: String? = null,
    val height: Int? = null,
    val bandwidth: Long? = null,
    val codec: String? = null,
    /** Referer / User-Agent / Origin — без них многие CDN отдают 403. */
    val headers: Map<String, String> = emptyMap(),
    val audioTracks: List<AudioTrack> = emptyList(),
    val subtitles: List<Subtitle> = emptyList(),
    val isMaster: Boolean = false,
)

@Serializable
data class AudioTrack(val url: String?, val language: String?, val name: String?, val isDefault: Boolean = false)

@Serializable
data class Subtitle(val url: String, val language: String?, val name: String?, val format: String? = null)

@Serializable
data class ImagePage(
    val index: Int,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val width: Int? = null,
    val height: Int? = null,
)

@Serializable
data class TextChapter(val title: String?, val paragraphs: List<String>) {
    val charCount: Int get() = paragraphs.sumOf { it.length }
}

@Serializable
enum class BlockReason { DRM, PAYWALL, LOGIN_REQUIRED, GEO_BLOCKED, REMOVED, UNKNOWN }

@Serializable
sealed interface TerminalContent {
    @Serializable data class Streams(val streams: List<VideoStream>) : TerminalContent
    @Serializable data class Images(val pages: List<ImagePage>) : TerminalContent
    @Serializable data class Text(val chapter: TextChapter) : TerminalContent
    @Serializable data class Unavailable(val reason: BlockReason, val message: String) : TerminalContent

    val isEmpty: Boolean get() = when (this) {
        is Streams -> streams.isEmpty()
        is Images -> pages.isEmpty()
        is Text -> chapter.paragraphs.isEmpty()
        is Unavailable -> true
    }

    val size: Int get() = when (this) {
        is Streams -> streams.size
        is Images -> pages.size
        is Text -> chapter.paragraphs.size
        is Unavailable -> 0
    }
}

/* =====================================================================
 * 4. СИГНАТУРА УЗЛА (только данные)
 * ===================================================================== */

/**
 * Переехала сюда из core/heal: на неё ссылается FieldSpec, а модельный
 * слой не должен зависеть от слоя лечения. Логика capture() и Similarity
 * остаётся в core/heal и работает с этой структурой снаружи.
 */
@Serializable
data class ElementSignature(
    val tag: String,
    val stableClasses: List<String> = emptyList(),
    val stableAttrs: Map<String, String> = emptyMap(),
    val normalizedText: String = "",
    val textTokens: List<String> = emptyList(),
    val tagPath: List<String> = emptyList(),
    val shape: String = "",
    val neighbourTags: List<String> = emptyList(),
    val ancestorClasses: List<String> = emptyList(),
    val indexInParent: Int = 0,
    val depth: Int = 0,
    val confirmed: Int = 1,
    val capturedAt: Long = 0L,
) {
    val isMature: Boolean get() = confirmed >= 5
}

/* =====================================================================
 * 5. КОНФИГ ИСТОЧНИКА
 * ===================================================================== */

@Serializable
enum class ConfigOrigin { USER_PICKED, HEALED, HEALED_PROBATION, BUNDLED, GENERIC }

/**
 * Как достать значение из найденного узла.
 * attr = null означает текст узла.
 */
@Serializable
data class ValueTransform(
    val attr: String? = null,
    /** Первая группа регулярки становится значением. */
    val regex: String? = null,
    val absoluteUrl: Boolean = false,
    val stripHtml: Boolean = true,
    val joinSeparator: String? = null,
) {
    @Transient
    val compiled: Regex? = regex?.let { runCatching { Regex(it) }.getOrNull() }

    companion object {
        val TEXT = ValueTransform()
        val HREF = ValueTransform(attr = "href", absoluteUrl = true)
        val SRC = ValueTransform(attr = "src", absoluteUrl = true)
    }
}

@Serializable
data class FieldSpec(
    val selector: String,
    val fallbackSelectors: List<String> = emptyList(),
    val transform: ValueTransform = ValueTransform.TEXT,
    val required: Boolean = false,
    val signature: ElementSignature? = null,
    /** Эталонное значение для дешёвой канареечной проверки (файл 14). */
    val goldenValue: String? = null,
) {
    /** Все селекторы по порядку: боевой, затем запасные. */
    val ladder: List<String> get() = listOf(selector) + fallbackSelectors

    /** Повышает запасной селектор в боевые, старый уходит в конец. */
    fun promote(newSelector: String): FieldSpec {
        if (newSelector == selector) return this
        val rest = (listOf(selector) + fallbackSelectors).filter { it != newSelector }
        return copy(selector = newSelector, fallbackSelectors = rest.take(MAX_FALLBACKS))
    }

    /** Сдвигает лестницу на одну ступень вниз. Возвращает null, если запасных нет. */
    fun nextFallback(): FieldSpec? {
        val next = fallbackSelectors.firstOrNull() ?: return null
        return copy(selector = next, fallbackSelectors = fallbackSelectors.drop(1) + selector)
    }

    companion object { const val MAX_FALLBACKS = 5 }
}

@Serializable
data class ListingSpec(
    val itemSelector: String,
    val fallbackItemSelectors: List<String> = emptyList(),
    val fields: Map<String, FieldSpec> = emptyMap(),
    val nextPageSelector: String? = null,
    /** Шаблон постраничности, если ссылки «дальше» нет: /catalog?page={n} */
    val pageUrlTemplate: String? = null,
) {
    val itemLadder: List<String> get() = listOf(itemSelector) + fallbackItemSelectors
}

@Serializable
data class EntrySpec(val fields: Map<String, FieldSpec> = emptyMap())

@Serializable
data class UnitsSpec(
    val unitSelector: String,
    val fallbackUnitSelectors: List<String> = emptyList(),
    val fields: Map<String, FieldSpec> = emptyMap(),
    /** Список серий часто отдаётся отдельным запросом. */
    val ajaxUrlTemplate: String? = null,
    val reverseOrder: Boolean = false,
) {
    val unitLadder: List<String> get() = listOf(unitSelector) + fallbackUnitSelectors
}

@Serializable
data class TerminalSpec(
    val expect: TerminalExpect,
    val playerSelectors: List<String> = emptyList(),
    val imageArraySelectors: List<String> = emptyList(),
    val textSelectors: List<String> = emptyList(),
    val requiredHeaders: Map<String, String> = emptyMap(),
)

@Serializable
enum class TerminalExpect { VIDEO, IMAGES, TEXT }

/** Гипотеза о JSON-эндпоинте, замеченном при рендере. Не активна до проверки. */
@Serializable
data class ApiCandidate(
    val urlTemplate: String,
    val method: String = "GET",
    val seenAt: Long,
    val verified: Boolean = false,
)

@Serializable
data class SourceConfig(
    val host: String,
    val profile: ContentProfile,
    val baseUrl: String? = null,
    val referer: String? = null,
    /** Статический HTML пуст — сразу поднимать WebView (см. FetchLadder). */
    val requiresRendering: Boolean = false,
    val searchUrlTemplate: String? = null,
    val listing: ListingSpec? = null,
    val entry: EntrySpec? = null,
    val units: UnitsSpec? = null,
    val terminal: TerminalSpec? = null,
    val canaryUrls: List<String> = emptyList(),
    val apiCandidates: List<ApiCandidate> = emptyList(),
    val origin: ConfigOrigin = ConfigOrigin.BUNDLED,
    val createdAt: Long = 0L,
    val notes: List<String> = emptyList(),
) {

    fun fieldSpec(field: String): FieldSpec? =
        listing?.fields?.get(field) ?: entry?.fields?.get(field) ?: units?.fields?.get(field)

    /**
     * Сдвигает лестницу фолбэков у указанных полей. Ровно это делает
     * RepairAction.TryFallback — самая дешёвая ступень починки.
     */
    fun withFallbacks(fields: Set<String>): SourceConfig {
        if (fields.isEmpty()) return this
        var cfg = this
        for (f in fields) {
            cfg = when (f) {
                ITEM_KEY -> cfg.shiftItemSelector()
                else -> cfg.shiftFieldSelector(f)
            }
        }
        return cfg
    }

    /** Применяет предложения хилера: селектор + свежая сигнатура. */
    fun withRepairs(repairs: Map<String, RepairProposal>): SourceConfig {
        if (repairs.isEmpty()) return this
        var cfg = this
        for ((field, p) in repairs) {
            cfg = if (field == ITEM_KEY) {
                cfg.copy(listing = cfg.listing?.let {
                    it.copy(
                        itemSelector = p.selector,
                        fallbackItemSelectors = (listOf(it.itemSelector) + it.fallbackItemSelectors)
                            .filter { s -> s != p.selector }.take(FieldSpec.MAX_FALLBACKS),
                    )
                })
            } else {
                cfg.mapField(field) { spec ->
                    spec.promote(p.selector).copy(signature = p.signature ?: spec.signature)
                }
            }
        }
        return cfg
    }

    private fun shiftItemSelector(): SourceConfig {
        val l = listing ?: return this
        val next = l.fallbackItemSelectors.firstOrNull() ?: return this
        return copy(listing = l.copy(
            itemSelector = next,
            fallbackItemSelectors = l.fallbackItemSelectors.drop(1) + l.itemSelector,
        ))
    }

    private fun shiftFieldSelector(field: String): SourceConfig =
        mapField(field) { it.nextFallback() ?: it }

    private fun mapField(field: String, f: (FieldSpec) -> FieldSpec): SourceConfig {
        listing?.fields?.get(field)?.let { spec ->
            return copy(listing = listing.copy(fields = listing.fields + (field to f(spec))))
        }
        entry?.fields?.get(field)?.let { spec ->
            return copy(entry = entry!!.copy(fields = entry!!.fields + (field to f(spec))))
        }
        units?.fields?.get(field)?.let { spec ->
            return copy(units = units!!.copy(fields = units!!.fields + (field to f(spec))))
        }
        return this
    }

    companion object {
        /** Псевдополе для селектора карточки — чтобы чинить его тем же кодом. */
        const val ITEM_KEY = "__item"
    }
}

/** Предложение починки: результат ре-поиска или фонового ремонта. */
@Serializable
data class RepairProposal(
    val field: String,
    val selector: String,
    val confidence: Double,
    val signature: ElementSignature? = null,
    val transform: ValueTransform? = null,
)

/* =====================================================================
 * 6. НОРМАЛИЗАЦИЯ КЛЮЧЕЙ
 * ===================================================================== */

/**
 * Ключ должен быть одинаковым для /anime/naruto, /anime/naruto/,
 * /anime/naruto?from=main и https://www.site.tv/anime/naruto.
 * Иначе KEY_DUPLICATE будет ложно срабатывать, а избранное — дублироваться.
 */
object KeyMaker {

    private val TRACKING = setOf(
        "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
        "from", "ref", "referrer", "fbclid", "gclid", "yclid", "_openstat",
    )

    fun fromUrl(url: String): String {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return ""
        val noHash = trimmed.substringBefore('#')
        val path = noHash.substringBefore('?')
        val query = noHash.substringAfter('?', "")

        val cleanPath = path
            .removePrefix("https://").removePrefix("http://")
            .let { if (it.startsWith("www.")) it.removePrefix("www.") else it }
            .trimEnd('/')
            .lowercase()

        val keptQuery = query.split('&')
            .filter { it.isNotBlank() && it.substringBefore('=').lowercase() !in TRACKING }
            .sorted()
            .joinToString("&")

        return if (keptQuery.isEmpty()) cleanPath else "$cleanPath?$keptQuery"
    }
}

fun hostOf(url: String): String = runCatching {
    java.net.URI(url).host?.removePrefix("www.")?.lowercase().orEmpty()
}.getOrDefault("")

fun originOf(url: String): String? = runCatching {
    val u = java.net.URI(url)
    if (u.scheme == null || u.host == null) null else "${u.scheme}://${u.host}"
}.getOrNull()
