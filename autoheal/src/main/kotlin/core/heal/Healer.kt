package core.heal

import core.model.ElementSignature
import core.model.FieldSpec
import core.model.RepairProposal
import core.model.SourceConfig
import core.parse.Dom
import core.parse.Node
import core.parse.ValueReader
import core.validate.ContractReport
import core.validate.IssueCode
import core.validate.Severity
import core.validate.SourceHealth

/* ═══════════════════════════════════════════════════════════════════
   РЕМОНТ
   Валидатор сказал «сломано». Задача хилера — предложить новый селектор,
   но НЕ применять его: применение проходит через кворум и испытательный
   срок в RevisionManager. Хилер только выдвигает гипотезы и честно
   сообщает свою уверенность.
   ═══════════════════════════════════════════════════════════════════ */

/** Бюджет: ре-поиск дорог, а вызывается он в момент, когда всё уже плохо. */
data class HealBudget(
    val maxFields: Int = 6,
    val maxNodesPerField: Int = 1200,
    val maxCandidatesPerField: Int = 6,
)

data class HealResult(
    val proposals: List<RepairProposal>,
    val unrepairable: Set<String>,
    val notes: List<String> = emptyList(),
) {
    val isEmpty: Boolean get() = proposals.isEmpty()
}

class Healer(
    private val budget: HealBudget = HealBudget(),
    private val now: () -> Long = System::currentTimeMillis,
) {

    /**
     * Главный вход: по отчёту валидатора понять, что чинить,
     * и попытаться найти уехавшие узлы на свежем документе.
     */
    fun heal(
        dom: Dom,
        config: SourceConfig,
        report: ContractReport,
        health: SourceHealth? = null,
    ): HealResult {
        val targets = targetsOf(report).take(budget.maxFields)
        if (targets.isEmpty()) return HealResult(emptyList(), emptySet())

        val proposals = ArrayList<RepairProposal>(targets.size)
        val failed = HashSet<String>()
        val notes = ArrayList<String>()

        for (field in targets) {
            if (field == SourceConfig.ITEM_KEY) {
                val p = healItemSelector(dom, config, report, health)
                if (p != null) proposals += p else failed += field
                continue
            }
            val p = healField(dom, config, field)
            if (p != null) proposals += p else failed += field
        }

        if (failed.isNotEmpty()) notes += "не удалось опознать: ${failed.sorted()}"
        return HealResult(proposals, failed, notes)
    }

    /**
     * Что именно чинить. Порядок важен: селектор карточки первым —
     * если уехал он, все поля «сломаны» лишь как следствие, и чинить
     * их по отдельности бессмысленно.
     */
    internal fun targetsOf(report: ContractReport): List<String> {
        val out = LinkedHashSet<String>()

        val listBroken = report.issues.any {
            it.code == IssueCode.LIST_EMPTY || it.code == IssueCode.LIST_TOO_LARGE ||
                it.code == IssueCode.COUNT_REGRESSION && it.severity == Severity.ERROR
        } || SourceConfig.ITEM_KEY in report.brokenFields

        if (listBroken) out += SourceConfig.ITEM_KEY

        // Отдельные поля: только настоящие признаки уехавшего селектора.
        // FIELD_MARKUP_LEAK или PLACEHOLDER — дефект значения, ремонт селектора
        // тут не поможет и лишь потратит бюджет.
        for (i in report.issues) {
            val f = i.field ?: continue
            if (f == SourceConfig.ITEM_KEY) continue
            if (i.severity != Severity.ERROR) continue
            if (i.code in REPAIRABLE) out += f
        }
        return out.toList()
    }

    /* ---------------- поле внутри карточки / документа ---------------- */

    private fun healField(dom: Dom, config: SourceConfig, field: String): RepairProposal? {
        val spec = config.fieldSpec(field) ?: return null

        // 1. Отпечаток — самый надёжный путь: ищем тот же узел заново.
        spec.signature?.let { sig ->
            SignatureSearch.find(dom, sig)?.let { m ->
                bestSelector(m.node, dom, spec)?.let { cand ->
                    return RepairProposal(
                        field = field,
                        selector = cand.css,
                        confidence = confidenceOf(m.score, cand.stability, viaSignature = true),
                        signature = Signatures.capture(m.node, now()),
                        transform = spec.transform,
                    )
                }
            }
        }

        // 2. Эталонное значение: знаем, что там было написано — ищем по тексту.
        spec.goldenValue?.takeIf { it.isNotBlank() }?.let { golden ->
            val byText = dom.findByOwnText(golden, limit = 6, exact = true)
                .ifEmpty { dom.findByAttrValue("href", golden, limit = 6) }
            val node = byText.firstOrNull()
            if (node != null) {
                bestSelector(node, dom, spec)?.let { cand ->
                    return RepairProposal(
                        field = field,
                        selector = cand.css,
                        confidence = confidenceOf(0.80, cand.stability, viaSignature = false),
                        signature = Signatures.capture(node, now()),
                        transform = spec.transform,
                    )
                }
            }
        }

        return null
    }

    /* ---------------- селектор карточки списка ---------------- */

    /**
     * Карточку нельзя опознать по одному узлу: нужен повторяющийся блок.
     * Стратегия — найти по отпечатку одну карточку, затем синтезировать
     * селектор, ловящий её братьев, и сверить количество с историей.
     */
    private fun healItemSelector(
        dom: Dom, config: SourceConfig, report: ContractReport, health: SourceHealth?,
    ): RepairProposal? {
        val listing = config.listing ?: return null

        /*
         * Сколько карточек ждать. В момент поломки itemCount равен нулю
         * ровно потому, что селектор мёртв — брать его как ориентир
         * бессмысленно. Единственный осмысленный источник — история:
         * сколько элементов источник отдавал, когда всё работало.
         */
        val expected = health?.lastGoodCount?.takeIf { it > 0 }
            ?: report.itemCount.takeIf { it > 0 }
            ?: 0

        // Опорный узел: карточка, внутри которой лежит поле с отпечатком.
        // Зрелые отпечатки идут первыми — им больше доверия.
        val anchors = listing.fields.values
            .mapNotNull { it.signature }
            .sortedByDescending { it.confirmed }

        val sample: Node? = anchors
            .firstNotNullOfOrNull { sig -> SignatureSearch.find(dom, sig)?.node?.let { cardOf(it) } }
            ?: guessRepeatingBlock(dom)

        val node = sample ?: return null
        val candidates = SelectorSynth.forRepeating(node, dom, expected)
            .take(budget.maxCandidatesPerField)
        val best = candidates.firstOrNull() ?: return null

        return RepairProposal(
            field = SourceConfig.ITEM_KEY,
            selector = best.css,
            confidence = (best.stability * 0.9).coerceIn(0.0, 0.95),
            signature = Signatures.capture(node, now()),
        )
    }

    /**
     * От найденного поля вверх до карточки: карточка — ближайший предок,
     * у которого есть однотипные братья и внутри есть ссылка.
     */
    private fun cardOf(fieldNode: Node): Node? {
        var cur: Node? = fieldNode
        var hops = 0
        while (cur != null && hops < 6) {
            val siblings = cur.siblingsOfSameTag
            if (siblings >= 3 && cur.selectFirst("a[href]") != null) return cur
            cur = cur.parent()
            hops++
        }
        return null
    }

    /**
     * Последняя надежда, когда отпечатков нет вообще: ищем контейнер
     * с наибольшим числом однотипных детей, каждый со ссылкой.
     * Грубо, зато работает на любой сетке каталога.
     */
    internal fun guessRepeatingBlock(dom: Dom): Node? {
        var best: Node? = null
        var bestScore = 0
        for (n in dom.walk(budget.maxNodesPerField)) {
            val kids = n.children()
            if (kids.size < 4) continue
            val byTag = kids.groupBy { it.tag }.maxByOrNull { it.value.size } ?: continue
            val group = byTag.value
            if (group.size < 4) continue
            val withLinks = group.count { it.selectFirst("a[href]") != null }
            if (withLinks < group.size * 0.8) continue
            val score = withLinks
            if (score > bestScore) { bestScore = score; best = group.first() }
        }
        return best
    }

    /* ---------------- общее ---------------- */

    private fun bestSelector(node: Node, dom: Dom, spec: FieldSpec): SelectorCandidate? {
        val scope = node.ancestors(6).lastOrNull { it.siblingsOfSameTag >= 3 }
        return SelectorSynth.forNode(node, dom, scope)
            .take(budget.maxCandidatesPerField)
            .firstOrNull { it.isUnique || it.stability >= 0.7 }
    }

    /**
     * Итоговая уверенность. Умышленно консервативна: предложение с 0.7
     * ещё не применяется — нужен кворум из трёх независимых совпадений.
     * Завышенная уверенность здесь означает мусор в конфиге через сутки.
     */
    private fun confidenceOf(matchScore: Double, stability: Double, viaSignature: Boolean): Double {
        val base = matchScore * 0.6 + stability * 0.4
        val bonus = if (viaSignature) 0.05 else 0.0
        return (base + bonus).coerceIn(0.0, 0.95)
    }

    private companion object {
        val REPAIRABLE = setOf(
            IssueCode.FIELD_MISSING,
            IssueCode.FIELD_LOW_FILL,
            IssueCode.FIELD_FILL_DROP,
            IssueCode.FIELD_CONSTANT,
            IssueCode.FIELD_JUNK,
            IssueCode.FIELD_NOT_URL,
        )
    }
}

/* ═══════════════════════════════════════════════════════════════════
   ЗАХВАТ ОТПЕЧАТКОВ
   Отпечаток нельзя снять в момент поломки — узла уже нет. Его снимают,
   пока всё работает. Без этого шага весь ремонт по сигнатурам мёртв.
   ═══════════════════════════════════════════════════════════════════ */

object SignatureCapture {

    /**
     * Обновляет отпечатки полей по успешно разобранному документу.
     * confirmed растёт: зрелый отпечаток (5+ подтверждений) заслуживает
     * доверия при ремонте, свежий — нет.
     */
    fun refresh(dom: Dom, config: SourceConfig, report: ContractReport): SourceConfig {
        if (!report.verdict.isUsable) return config
        val stamp = System.currentTimeMillis()
        var cfg = config

        config.listing?.let { spec ->
            val card = spec.itemLadder.firstNotNullOfOrNull { dom.select(it).firstOrNull() }
            val updated = spec.fields.mapValues { (field, fs) ->
                val scope = card ?: dom.root
                val node = fs.ladder.firstNotNullOfOrNull { scope.selectFirst(it) } ?: return@mapValues fs
                val fresh = Signatures.capture(node, stamp)
                fs.copy(
                    signature = merge(fs.signature, fresh),
                    goldenValue = fs.goldenValue ?: ValueReader.read(node, fs.transform)?.take(80),
                )
            }
            cfg = cfg.copy(listing = spec.copy(fields = updated))
        }

        config.entry?.let { spec ->
            val updated = spec.fields.mapValues { (_, fs) ->
                val node = fs.ladder.firstNotNullOfOrNull { dom.selectFirst(it) } ?: return@mapValues fs
                fs.copy(signature = merge(fs.signature, Signatures.capture(node, stamp)))
            }
            cfg = cfg.copy(entry = spec.copy(fields = updated))
        }

        return cfg
    }

    /** Тот же узел — растим счётчик; изменился — берём новый снимок с нуля. */
    private fun merge(old: ElementSignature?, fresh: ElementSignature): ElementSignature {
        if (old == null) return fresh
        val same = Similarity.score(old, fresh) >= Similarity.STRONG
        return if (same) fresh.copy(confirmed = old.confirmed + 1) else fresh
    }
}
