package core.validate

import core.extract.ExtractionTrace
import core.model.*
import core.parse.UrlTools

/* =====================================================================
 * 1. ПРОБЛЕМЫ
 * ===================================================================== */

enum class Severity { INFO, WARN, ERROR }

enum class IssueCode {
    LIST_EMPTY,
    LIST_TOO_SMALL,
    LIST_TOO_LARGE,
    COUNT_REGRESSION,
    FIELD_MISSING,
    FIELD_LOW_FILL,
    FIELD_FILL_DROP,
    FIELD_CONSTANT,
    FIELD_JUNK,
    FIELD_MARKUP_LEAK,
    FIELD_NOT_URL,
    FIELD_PLACEHOLDER,
    KEY_DUPLICATE,
    KEY_INVALID,
    ITEMS_DROPPED,
    FALLBACK_USED,
    NUMBERING_MISSING,
    NUMBERING_GAPS,
    CONTENT_EMPTY,
    CONTENT_BLOCKED,
    CONTENT_HOST_MIX,
    TEXT_TOO_SHORT,
    BODY_TRUNCATED,
    DRIFT_DETECTED,
}

data class Issue(
    val code: IssueCode,
    val severity: Severity,
    val message: String,
    val field: String? = null,
    val penalty: Double = defaultPenalty(severity),
    val samples: List<String> = emptyList(),
) {
    companion object {
        fun defaultPenalty(s: Severity): Double = when (s) {
            Severity.INFO -> 0.0
            Severity.WARN -> 0.08
            Severity.ERROR -> 0.35
        }
    }
}

/* =====================================================================
 * 2. ВЕРДИКТ
 * ===================================================================== */

sealed interface Verdict {
    data class Ok(val confidence: Double) : Verdict
    data class Degraded(val confidence: Double, val brokenFields: Set<String>) : Verdict
    data class Broken(val brokenFields: Set<String>, val reason: String) : Verdict
    data class EmptyByDesign(val reason: String) : Verdict

    val confidenceOrZero: Double get() = when (this) {
        is Ok -> confidence
        is Degraded -> confidence
        is Broken -> 0.0
        is EmptyByDesign -> 1.0
    }

    val isUsable: Boolean get() = this is Ok || this is Degraded
}

data class ContractReport(
    val verdict: Verdict,
    val issues: List<Issue>,
    val fillRates: Map<String, Double>,
    val itemCount: Int,
    val trace: ExtractionTrace? = null,
) {
    val brokenFields: Set<String>
        get() = when (verdict) {
            is Verdict.Degraded -> verdict.brokenFields
            is Verdict.Broken -> verdict.brokenFields
            else -> emptySet()
        }

    val hasBrokenFields: Boolean get() = brokenFields.isNotEmpty()

    val worstSeverity: Severity?
        get() = issues.maxByOrNull { it.severity.ordinal }?.severity

    fun withIssues(extra: List<Issue>): ContractReport {
        if (extra.isEmpty()) return this
        val all = issues + extra
        val penalty = extra.sumOf { it.penalty }
        val newVerdict = when (verdict) {
            is Verdict.Ok -> {
                val c = (verdict.confidence - penalty).coerceAtLeast(0.0)
                val broken = extra.filter { it.severity == Severity.ERROR }.mapNotNull { it.field }.toSet()
                when {
                    broken.isNotEmpty() -> Verdict.Degraded(c, broken)
                    c < Thresholds.DEGRADED_BELOW -> Verdict.Degraded(c, emptySet())
                    else -> Verdict.Ok(c)
                }
            }
            is Verdict.Degraded -> verdict.copy(
                confidence = (verdict.confidence - penalty).coerceAtLeast(0.0),
                brokenFields = verdict.brokenFields +
                    extra.filter { it.severity == Severity.ERROR }.mapNotNull { it.field },
            )
            else -> verdict
        }
        return copy(verdict = newVerdict, issues = all)
    }

    companion object {
        fun empty(reason: String) = ContractReport(
            Verdict.EmptyByDesign(reason), emptyList(), emptyMap(), 0,
        )
    }
}

/* =====================================================================
 * 3. ПОРОГИ
 * ===================================================================== */

/**
 * Вынесены в один объект намеренно. Это догадки, которые придётся
 * калибровать на реальных сайтах, и держать их размазанными по коду —
 * значит гарантировать, что калибровка никогда не случится.
 */
object Thresholds {
    const val LOW_FILL = 0.60
    const val FILL_DROP_ABS = 0.20
    const val CRITICAL_FILL = 0.25
    const val CONSTANCY_MAX = 0.85
    const val DEGRADED_BELOW = 0.65
    const val REGRESSION_SOFT = 0.30
    const val REGRESSION_HARD = 0.60
    const val JUNK_SHARE_MAX = 0.40
    const val DUPLICATE_SHARE_MAX = 0.15
    const val DROPPED_SHARE_MAX = 0.30
    const val NUMBERING_MIN = 0.70
    const val TEXT_MIN_CHARS = 400
    const val MIN_SAMPLE_FOR_CONSTANCY = 4
    const val MIN_HISTORY_FOR_REGRESSION = 3
}

/* =====================================================================
 * 4. ДЕШЁВЫЕ ДЕТЕКТОРЫ
 * ===================================================================== */

/**
 * Ловят «тихий мусор»: значения, которые формально извлеклись,
 * а по сути являются навигацией, разметкой или заглушкой.
 */
object ValueSanity {

    private val NAV_WORDS = setOf(
        "главная", "каталог", "поиск", "войти", "регистрация", "меню", "закрыть",
        "назад", "вперёд", "вперед", "далее", "ещё", "еще", "все", "показать",
        "home", "search", "login", "sign in", "register", "menu", "close",
        "next", "prev", "previous", "more", "show all", "back",
    )

    private val MARKUP = Regex("""<[a-z/][^>]{0,80}>|&(nbsp|amp|lt|gt|quot|#\d{2,5});""", RegexOption.IGNORE_CASE)

    private val PLACEHOLDER = listOf(
        "data:image", "placeholder", "no-image", "noimage", "nocover", "no_cover",
        "blank.gif", "blank.png", "lazy", "spacer", "1x1", "default.jpg", "stub",
    )

    fun looksLikeNavJunk(value: String): Boolean {
        val v = value.trim().lowercase()
        if (v.isEmpty() || v.length > 40) return false
        return v in NAV_WORDS || NAV_WORDS.any { v == it || v.removeSuffix("»").trim() == it }
    }

    fun hasMarkupLeak(value: String): Boolean = MARKUP.containsMatchIn(value)

    fun looksLikeUrl(value: String): Boolean = UrlTools.looksLikeUrl(value)

    fun looksLikePlaceholderImage(value: String): Boolean {
        val low = value.lowercase()
        return PLACEHOLDER.any { low.contains(it) }
    }

    /** Доля самого частого значения. Единица означает «одно и то же везде». */
    fun constancyRatio(values: List<String>): Double {
        if (values.size < 2) return 0.0
        val top = values.groupingBy { it }.eachCount().values.max()
        return top.toDouble() / values.size
    }
}

/* =====================================================================
 * 5. ПРОВЕРКА КОЛОНКИ
 * ===================================================================== */

/**
 * Одна колонка — одно поле у всех элементов. Вся специфика типов
 * контента сводится к тому, какие колонки считать обязательными;
 * сама проверка везде одинакова, и это единственная её реализация.
 */
internal class ColumnCheck(
    val field: String,
    val values: List<String?>,
    val required: Boolean,
    val isUrlField: Boolean,
) {
    val present: List<String> = values.filterNotNull().filter { it.isNotBlank() }
    val fillRate: Double = if (values.isEmpty()) 0.0 else present.size.toDouble() / values.size

    fun issues(history: SourceHealth?): List<Issue> {
        val out = mutableListOf<Issue>()
        if (values.isEmpty()) return out

        val wasStable = history?.stableFields?.contains(field) == true

        when {
            present.isEmpty() -> out += Issue(
                IssueCode.FIELD_MISSING,
                if (required || wasStable) Severity.ERROR else Severity.INFO,
                "поле пустое у всех ${values.size} элементов",
                field,
            )
            fillRate < Thresholds.CRITICAL_FILL -> out += Issue(
                IssueCode.FIELD_LOW_FILL,
                if (required || wasStable) Severity.ERROR else Severity.WARN,
                "заполнено ${pct(fillRate)} элементов",
                field,
                samples = present.take(2),
            )
            fillRate < Thresholds.LOW_FILL -> out += Issue(
                IssueCode.FIELD_LOW_FILL,
                if (required) Severity.WARN else Severity.INFO,
                "заполнено ${pct(fillRate)} элементов",
                field,
            )
        }

        val historic = history?.fieldFill?.get(field)
        if (historic != null && historic >= 0.85 && fillRate > 0.0) {
            val drop = historic - fillRate
            if (drop >= Thresholds.FILL_DROP_ABS) {
                out += Issue(
                    IssueCode.FIELD_FILL_DROP,
                    if (required || wasStable) Severity.ERROR else Severity.WARN,
                    "заполненность упала с ${pct(historic)} до ${pct(fillRate)}",
                    field,
                    penalty = 0.35,
                )
            }
        }

        if (present.size >= Thresholds.MIN_SAMPLE_FOR_CONSTANCY) {
            val constancy = ValueSanity.constancyRatio(present)
            if (constancy > Thresholds.CONSTANCY_MAX) {
                // Самый сильный сигнал поломки после пустоты: селектор
                // уехал на статический элемент шаблона.
                out += Issue(
                    IssueCode.FIELD_CONSTANT,
                    if (wasStable) Severity.ERROR else Severity.WARN,
                    "одно и то же значение у ${pct(constancy)} элементов",
                    field,
                    samples = listOf(present.groupingBy { it }.eachCount().maxBy { it.value }.key.take(60)),
                )
            }
        }

        val junk = present.count { ValueSanity.looksLikeNavJunk(it) }
        if (junk > 0 && junk.toDouble() / present.size > Thresholds.JUNK_SHARE_MAX) {
            out += Issue(
                IssueCode.FIELD_JUNK, Severity.ERROR,
                "$junk из ${present.size} значений похожи на навигацию",
                field,
                samples = present.filter { ValueSanity.looksLikeNavJunk(it) }.take(3),
            )
        }

        present.firstOrNull { ValueSanity.hasMarkupLeak(it) }?.let {
            out += Issue(IssueCode.FIELD_MARKUP_LEAK, Severity.WARN,
                "в значение протекает разметка", field, samples = listOf(it.take(60)))
        }

        if (isUrlField && present.isNotEmpty()) {
            val notUrl = present.count { !ValueSanity.looksLikeUrl(it) }
            if (notUrl > present.size / 2) out += Issue(
                IssueCode.FIELD_NOT_URL, Severity.ERROR,
                "$notUrl из ${present.size} значений не похожи на ссылки",
                field, samples = present.filter { !ValueSanity.looksLikeUrl(it) }.take(2),
            )
            val stubs = present.count { ValueSanity.looksLikePlaceholderImage(it) }
            if (stubs > present.size / 2) out += Issue(
                IssueCode.FIELD_PLACEHOLDER,
                if (field == "cover") Severity.WARN else Severity.INFO,
                "$stubs из ${present.size} значений выглядят заглушками",
                field, samples = present.filter { ValueSanity.looksLikePlaceholderImage(it) }.take(2),
            )
        }

        return out
    }

    private fun pct(v: Double) = "${(v * 100).toInt()}%"
}

/* =====================================================================
 * 6. ЧЕКЕР
 * ===================================================================== */

class StandardContractChecker(private val profile: ContentProfile) {

    fun check(
        payload: ParsedPayload,
        trace: ExtractionTrace?,
        health: SourceHealth?,
        truncated: Boolean = false,
    ): ContractReport = when (payload) {
        is ParsedPayload.Listing -> listing(payload, trace, health, truncated)
        is ParsedPayload.Entry -> entry(payload, trace, health)
        is ParsedPayload.Units -> units(payload, trace, health)
        is ParsedPayload.Content -> content(payload.content, health)
    }

    /* --- список ------------------------------------------------------ */

    private fun listing(
        p: ParsedPayload.Listing, trace: ExtractionTrace?, health: SourceHealth?, truncated: Boolean,
    ): ContractReport {
        val items = p.items
        val issues = mutableListOf<Issue>()

        if (items.isEmpty()) {
            // Ключевая развилка: пустота — это поломка только на фоне истории.
            val hadContent = (health?.lastGoodCount ?: 0) > 0
            return if (!hadContent) ContractReport.empty("источник ещё ни разу не отдавал элементы")
            else ContractReport(
                Verdict.Broken(setOf(SourceConfig.ITEM_KEY), "пусто при истории ${health?.lastGoodCount}"),
                listOf(Issue(IssueCode.LIST_EMPTY, Severity.ERROR,
                    "0 элементов при обычных ${health?.lastGoodCount}")),
                emptyMap(), 0, trace,
            )
        }

        val range = profile.listingSizeRange
        if (items.size < range.first) issues += Issue(
            IssueCode.LIST_TOO_SMALL, Severity.WARN,
            "${items.size} элементов, ожидалось хотя бы ${range.first}")
        if (items.size > range.last) issues += Issue(
            IssueCode.LIST_TOO_LARGE, Severity.WARN,
            "${items.size} элементов — селектор, вероятно, слишком широкий")

        issues += regression(items.size, health)
        issues += columns(items, health)
        issues += keys(items.map { it.key })
        issues += traceIssues(trace, items.size)
        if (truncated) issues += Issue(IssueCode.BODY_TRUNCATED, Severity.WARN,
            "тело ответа обрезано по лимиту — часть элементов могла потеряться")

        return finish(issues, items, items.size, trace)
    }

    /* --- карточка ---------------------------------------------------- */

    private fun entry(
        p: ParsedPayload.Entry, trace: ExtractionTrace?, health: SourceHealth?,
    ): ContractReport {
        val e = p.entry
        val issues = mutableListOf<Issue>()

        if (e.title.isBlank()) issues += Issue(
            IssueCode.FIELD_MISSING, Severity.ERROR, "нет заголовка", "title")

        // Единственный элемент: констанси и заполненность бессмысленны,
        // проверяем только качество самих значений.
        for ((field, value) in listOf(
            "title" to e.title, "cover" to e.cover, "description" to e.description,
        ) + e.extras.toList()) {
            if (value.isNullOrBlank()) continue
            if (ValueSanity.hasMarkupLeak(value)) issues += Issue(
                IssueCode.FIELD_MARKUP_LEAK, Severity.WARN, "разметка в значении", field)
            if (ValueSanity.looksLikeNavJunk(value)) issues += Issue(
                IssueCode.FIELD_JUNK, Severity.ERROR, "значение похоже на элемент навигации", field,
                samples = listOf(value.take(40)))
            if (field in URL_FIELDS && !ValueSanity.looksLikeUrl(value)) issues += Issue(
                IssueCode.FIELD_NOT_URL, Severity.WARN, "не похоже на ссылку", field)
        }

        // Поля, которые раньше стабильно были, а теперь исчезли.
        health?.stableFields?.forEach { f ->
            if (e.field(f).isNullOrBlank()) issues += Issue(
                IssueCode.FIELD_MISSING, Severity.ERROR, "поле было стабильным и пропало", f)
        }

        issues += traceIssues(trace, 1)
        return finish(issues, listOf(e), 1, trace)
    }

    /* --- серии ------------------------------------------------------- */

    private fun units(
        p: ParsedPayload.Units, trace: ExtractionTrace?, health: SourceHealth?,
    ): ContractReport {
        val units = p.units
        val issues = mutableListOf<Issue>()

        if (units.isEmpty()) {
            val hadContent = (health?.lastGoodCount ?: 0) > 0
            return if (!hadContent) ContractReport.empty("у тайтла нет ${profile.unitNounPlural}")
            else ContractReport(
                Verdict.Broken(setOf(SourceConfig.ITEM_KEY), "список пуст при истории"),
                listOf(Issue(IssueCode.LIST_EMPTY, Severity.ERROR, "нет ${profile.unitNounPlural}")),
                emptyMap(), 0, trace,
            )
        }

        if (units.size > profile.unitsSizeRange.last) issues += Issue(
            IssueCode.LIST_TOO_LARGE, Severity.WARN, "${units.size} — подозрительно много")

        issues += regression(units.size, health)
        issues += columns(units, health)
        issues += keys(units.map { it.key })
        issues += numbering(units)
        issues += traceIssues(trace, units.size)

        return finish(issues, units, units.size, trace)
    }

    /**
     * Нумерация проверяется мягко: пропуски бывают законными
     * (сайт выложил серии 1-3 и 7), а вот полное отсутствие номеров
     * там, где они раньше были, — признак смены разметки.
     */
    private fun numbering(units: List<MediaUnit>): List<Issue> {
        val out = mutableListOf<Issue>()
        val numbered = units.count { it.number != null }
        val share = numbered.toDouble() / units.size

        if (share < Thresholds.NUMBERING_MIN && units.size >= 3) {
            out += Issue(IssueCode.NUMBERING_MISSING, Severity.WARN,
                "номера распознаны только у ${(share * 100).toInt()}%")
        }

        val nums = units.mapNotNull { it.number }.sorted()
        if (nums.size >= 5) {
            val expected = (nums.last() - nums.first() + 1).toInt()
            val missing = expected - nums.size
            if (missing > 0 && missing > expected * 0.25) out += Issue(
                IssueCode.NUMBERING_GAPS, Severity.INFO,
                "пропущено около $missing номеров из $expected")
        }
        return out
    }

    /* --- терминальный контент ----------------------------------------- */

    private fun content(c: TerminalContent, health: SourceHealth?): ContractReport {
        val issues = mutableListOf<Issue>()

        if (c is TerminalContent.Unavailable) {
            return ContractReport(
                Verdict.Broken(emptySet(), c.message),
                listOf(Issue(IssueCode.CONTENT_BLOCKED, Severity.ERROR, c.message)),
                emptyMap(), 0,
            )
        }

        if (c.isEmpty) {
            return ContractReport(
                Verdict.Broken(setOf("content"), "ничего не извлечено"),
                listOf(Issue(IssueCode.CONTENT_EMPTY, Severity.ERROR, "нет воспроизводимого контента")),
                emptyMap(), 0,
            )
        }

        when (c) {
            is TerminalContent.Streams -> {
                val bad = c.streams.count { !ValueSanity.looksLikeUrl(it.url) }
                if (bad > 0) issues += Issue(IssueCode.FIELD_NOT_URL, Severity.ERROR,
                    "$bad потоков с некорректным URL", "stream")
            }
            is TerminalContent.Images -> {
                val hosts = c.pages.mapNotNull { hostOf(it.url).takeIf { h -> h.isNotBlank() } }.distinct()
                // Страницы одной главы почти всегда с одного хоста.
                // Разнобой означает, что в список попала реклама.
                if (hosts.size > 2) issues += Issue(IssueCode.CONTENT_HOST_MIX, Severity.WARN,
                    "изображения с ${hosts.size} разных хостов", samples = hosts.take(3))
                val stubs = c.pages.count { ValueSanity.looksLikePlaceholderImage(it.url) }
                if (stubs > c.pages.size / 3) issues += Issue(IssueCode.FIELD_PLACEHOLDER,
                    Severity.ERROR, "$stubs страниц выглядят заглушками", "image")
            }
            is TerminalContent.Text -> {
                if (c.chapter.charCount < Thresholds.TEXT_MIN_CHARS) issues += Issue(
                    IssueCode.TEXT_TOO_SHORT, Severity.ERROR,
                    "всего ${c.chapter.charCount} символов — вероятно, не тело главы")
            }
            else -> Unit
        }

        val confidence = (1.0 - issues.sumOf { it.penalty }).coerceIn(0.0, 1.0)
        val verdict = when {
            issues.any { it.severity == Severity.ERROR } ->
                Verdict.Degraded(confidence, setOf("content"))
            confidence < Thresholds.DEGRADED_BELOW -> Verdict.Degraded(confidence, emptySet())
            else -> Verdict.Ok(confidence)
        }
        return ContractReport(verdict, issues, emptyMap(), c.size)
    }

    /* --- общие части --------------------------------------------------- */

    private fun columns(bearers: List<FieldBearing>, health: SourceHealth?): List<Issue> {
        if (bearers.isEmpty()) return emptyList()
        val fields = bearers.flatMap { it.fieldNames() }.toSet() - "key"
        return fields.flatMap { field ->
            ColumnCheck(
                field = field,
                values = bearers.map { it.field(field) },
                required = field in profile.requiredFields,
                isUrlField = field in URL_FIELDS,
            ).issues(health)
        }
    }

    private fun fillRatesOf(bearers: List<FieldBearing>): Map<String, Double> {
        if (bearers.isEmpty()) return emptyMap()
        val fields = bearers.flatMap { it.fieldNames() }.toSet() - "key"
        return fields.associateWith { f ->
            bearers.count { !it.field(f).isNullOrBlank() }.toDouble() / bearers.size
        }
    }

    private fun keys(keys: List<String>): List<Issue> {
        val out = mutableListOf<Issue>()
        val blank = keys.count { it.isBlank() }
        if (blank > 0) out += Issue(IssueCode.KEY_INVALID, Severity.ERROR,
            "$blank элементов без ключа")

        val dupes = keys.size - keys.distinct().size
        if (dupes > 0) {
            val share = dupes.toDouble() / keys.size
            out += Issue(
                IssueCode.KEY_DUPLICATE,
                if (share > Thresholds.DUPLICATE_SHARE_MAX) Severity.ERROR else Severity.WARN,
                "$dupes повторяющихся ключей из ${keys.size}",
                samples = keys.groupingBy { it }.eachCount()
                    .filter { it.value > 1 }.keys.take(2).toList(),
            )
        }
        return out
    }

    private fun regression(count: Int, health: SourceHealth?): List<Issue> {
        val last = health?.lastGoodCount ?: return emptyList()
        if (last <= 0 || (health.successCount) < Thresholds.MIN_HISTORY_FOR_REGRESSION) return emptyList()
        if (count >= last) return emptyList()
        val drop = 1.0 - count.toDouble() / last
        return when {
            drop >= Thresholds.REGRESSION_HARD -> listOf(Issue(
                IssueCode.COUNT_REGRESSION, Severity.ERROR,
                "$count вместо обычных $last — падение ${(drop * 100).toInt()}%"))
            drop >= Thresholds.REGRESSION_SOFT -> listOf(Issue(
                IssueCode.COUNT_REGRESSION, Severity.WARN,
                "$count вместо обычных $last"))
            else -> emptyList()
        }
    }

    /**
     * След извлечения превращается в проблемы. Важнейшая из них —
     * использованный фолбэк: данные внешне в порядке, но основной
     * селектор уже мёртв, и без этой отметки никто не узнает.
     */
    private fun traceIssues(trace: ExtractionTrace?, total: Int): List<Issue> {
        if (trace == null) return emptyList()
        val out = mutableListOf<Issue>()

        if (trace.itemRung > 0) out += Issue(
            IssueCode.FALLBACK_USED, Severity.WARN,
            "карточки найдены только запасным селектором №${trace.itemRung}",
            SourceConfig.ITEM_KEY)

        for ((field, rung) in trace.fieldRungs) if (rung > 0) out += Issue(
            IssueCode.FALLBACK_USED, Severity.WARN,
            "значение получено запасным селектором №$rung", field)

        if (trace.droppedItems > 0 && total > 0) {
            val share = trace.droppedItems.toDouble() / (total + trace.droppedItems)
            if (share > Thresholds.DROPPED_SHARE_MAX) out += Issue(
                IssueCode.ITEMS_DROPPED, Severity.ERROR,
                "${trace.droppedItems} элементов отброшено без ссылки или названия")
        }
        return out
    }

    private fun finish(
        issues: List<Issue>, bearers: List<FieldBearing>, count: Int, trace: ExtractionTrace?,
    ): ContractReport {
        val fills = fillRatesOf(bearers)
        val penalty = issues.sumOf { it.penalty }
        val confidence = (1.0 - penalty).coerceIn(0.0, 1.0)

        val brokenFields = issues
            .filter { it.severity == Severity.ERROR }
            .mapNotNull { it.field }
            .toSet()

        // Каркас считается сломанным, только если рухнули обязательные поля
        // или сам список. Пропавшая обложка — это деградация, а не поломка.
        val coreBroken = brokenFields.any { it in profile.requiredFields || it == SourceConfig.ITEM_KEY }
        val hasErrors = issues.any { it.severity == Severity.ERROR }

        val verdict = when {
            coreBroken -> Verdict.Broken(brokenFields, "сломаны обязательные поля")
            hasErrors -> Verdict.Degraded(confidence, brokenFields)
            confidence < Thresholds.DEGRADED_BELOW -> Verdict.Degraded(confidence, brokenFields)
            else -> Verdict.Ok(confidence)
        }

        return ContractReport(verdict, issues, fills, count, trace)
    }

    private companion object {
        val URL_FIELDS = setOf("url", "link", "cover", "poster", "image", "thumbnail", "src")
    }
}

/* =====================================================================
 * 7. ЗДОРОВЬЕ ИСТОЧНИКА
 * ===================================================================== */

/**
 * Память о том, как источник вёл себя раньше. Без неё половина проверок
 * бессмысленна: «ноль элементов» и «поле пропало» имеют смысл только
 * в сравнении с прошлым.
 */
@kotlinx.serialization.Serializable
data class SourceHealth(
    val host: String,
    val lastGoodCount: Int = 0,
    val emaFill: Double = 0.0,
    val emaConfidence: Double = 0.0,
    val stableFields: Set<String> = emptySet(),
    val fieldFill: Map<String, Double> = emptyMap(),
    val successCount: Int = 0,
    val failureCount: Int = 0,
    val degradedCount: Int = 0,
    val failStreak: Int = 0,
    val plainFetchFailStreak: Int = 0,
    val everNeededBrowserHeaders: Boolean = false,
    val avgBodySize: Int = 0,
    val lastSuccessAt: Long = 0L,
    val lastFailureAt: Long = 0L,
) {
    val successRate: Double
        get() {
            val total = successCount + degradedCount + failureCount
            return if (total == 0) 1.0
                   else (successCount + degradedCount * 0.5) / total
        }

    val isStable: Boolean get() = successCount >= 10 && successRate >= 0.9

    /** Относительное изменение размера тела. Ключевой признак подмены страницы. */
    fun bodySizeDelta(newSize: Int): Double =
        if (avgBodySize <= 0) 0.0 else (newSize - avgBodySize).toDouble() / avgBodySize

    fun onSuccess(report: ContractReport, bodySize: Int, usedBrowserHeaders: Boolean): SourceHealth {
        val conf = report.verdict.confidenceOrZero
        val avgFill = report.fillRates.values.average().takeIf { !it.isNaN() } ?: emaFill

        // Стабильными считаются поля, заполненные почти всегда.
        // Именно по их пропаже потом определяется поломка.
        val nowStable = report.fillRates.filterValues { it >= 0.9 }.keys
        val updatedStable = if (successCount < 5) stableFields + nowStable
                            else stableFields.intersect(nowStable) + nowStable.filter { it !in stableFields }
        val updatedFieldFill = report.fillRates.mapValues { (f, v) ->
            fieldFill[f]?.let { ema(it, v) } ?: v
        } + fieldFill.filterKeys { it !in report.fillRates }

        return copy(
            lastGoodCount = if (report.itemCount > 0) report.itemCount else lastGoodCount,
            emaFill = ema(emaFill, avgFill),
            emaConfidence = ema(emaConfidence, conf),
            stableFields = updatedStable,
            fieldFill = updatedFieldFill,
            successCount = successCount + 1,
            failStreak = 0,
            plainFetchFailStreak = 0,
            everNeededBrowserHeaders = everNeededBrowserHeaders || usedBrowserHeaders,
            avgBodySize = if (avgBodySize == 0) bodySize else ema(avgBodySize.toDouble(), bodySize.toDouble()).toInt(),
            lastSuccessAt = System.currentTimeMillis(),
        )
    }

    fun onPartial(report: ContractReport, bodySize: Int = 0): SourceHealth {
        val avgFill = report.fillRates.values.average().takeIf { !it.isNaN() } ?: emaFill
        return copy(
            emaFill = ema(emaFill, avgFill),
            emaConfidence = ema(emaConfidence, report.verdict.confidenceOrZero),
            degradedCount = degradedCount + 1,
            failStreak = 0,
            avgBodySize = if (bodySize <= 0) avgBodySize
                          else if (avgBodySize == 0) bodySize
                          else ema(avgBodySize.toDouble(), bodySize.toDouble()).toInt(),
            lastSuccessAt = System.currentTimeMillis(),
        )
    }

    fun onFailure(plainFetchFailed: Boolean = false): SourceHealth = copy(
        failureCount = failureCount + 1,
        failStreak = failStreak + 1,
        plainFetchFailStreak = if (plainFetchFailed) plainFetchFailStreak + 1 else plainFetchFailStreak,
        emaConfidence = ema(emaConfidence, 0.0),
        lastFailureAt = System.currentTimeMillis(),
    )

    private fun ema(old: Double, new: Double, alpha: Double = 0.25): Double =
        if (old == 0.0) new else old * (1 - alpha) + new * alpha
}

/* =====================================================================
 * 8. БЮДЖЕТ ОШИБОК
 * ===================================================================== */

/**
 * Когда автоматика исчерпала право на попытки. Смысл в том, чтобы
 * перестать бесконечно чинить безнадёжное и вовремя позвать человека.
 */
class ErrorBudget(private val targetSuccessRate: Double = 0.97) {

    private val window = HashMap<String, ArrayDeque<Double>>()
    private val capacity = 50
    private val minSample = 20
    private val minBadEvents = 3.0

    @Synchronized fun recordSuccess(host: String) = record(host, 1.0)
    @Synchronized fun recordDegraded(host: String) = record(host, 0.5)
    @Synchronized fun recordFailure(host: String) = record(host, 0.0)

    private fun record(host: String, weight: Double) {
        val q = window.getOrPut(host) { ArrayDeque() }
        if (q.size >= capacity) q.removeFirst()
        q.addLast(weight)
    }

    @Synchronized
    fun isExhausted(host: String): Boolean {
        val q = window[host] ?: return false
        if (q.size < minSample) return false
        val rate = q.sum() / q.size
        val allowedFailures = (1.0 - targetSuccessRate) * q.size
        val actualFailures = q.sumOf { 1.0 - it }
        return rate < targetSuccessRate &&
               actualFailures >= minBadEvents &&
               actualFailures > allowedFailures * 3
    }

    @Synchronized
    fun reset(host: String) { window.remove(host) }

    /** Восстановление окна из хранилища при старте. */
    @Synchronized fun replay(host: String, weight: Double) = record(host, weight)

    @Synchronized fun snapshot(host: String): List<Double> = window[host]?.toList().orEmpty()
}
