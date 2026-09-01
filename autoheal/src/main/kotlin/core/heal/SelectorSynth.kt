package core.heal

import core.parse.Css
import core.parse.Dom
import core.parse.Node

/* ═══════════════════════════════════════════════════════════════════
   СИНТЕЗ СЕЛЕКТОРА
   Отпечаток нашёл узел — но в конфиг нужно положить строку CSS.
   Плохой селектор либо не найдёт ничего завтра, либо поймает пол-страницы.
   Поэтому кандидаты генерируются от самых устойчивых к самым отчаянным
   и проверяются на реальном документе: должен находить именно этот узел.
   ═══════════════════════════════════════════════════════════════════ */

data class SelectorCandidate(
    val css: String,
    /** Устойчивость к редизайну: id/data-* высоко, nth-child низко. */
    val stability: Double,
    val hits: Int,
) {
    val isUnique: Boolean get() = hits == 1
}

object SelectorSynth {

    /**
     * Кандидаты для одиночного узла (поле карточки, кнопка).
     * scope — предок, внутри которого селектор должен работать
     * (карточка для полей списка), либо null для документа.
     */
    fun forNode(node: Node, dom: Dom, scope: Node? = null): List<SelectorCandidate> {
        val out = LinkedHashMap<String, Double>()

        node.attr("id").takeIf { it.isNotBlank() && !Signatures.isVolatileClass(it) }?.let {
            out["#${esc(it)}"] = 0.95
        }

        for (key in STRONG_ATTRS) {
            val v = node.attr(key)
            if (v.isNotBlank() && v.length < 64 && !Signatures.isVolatileClass(v)) {
                out["${node.tag}[$key=\"${esc(v)}\"]"] = 0.92
                out["[$key=\"${esc(v)}\"]"] = 0.88
            }
        }

        val classes = Signatures.stableClassesOf(node)
        if (classes.isNotEmpty()) {
            out["${node.tag}.${classes.joinToString(".") { esc(it) }}"] = 0.80
            classes.forEach { out["${node.tag}.${esc(it)}"] = 0.74 }
            classes.forEach { out[".${esc(it)}"] = 0.68 }
        }

        // Путь через стабильного предка: класс уехал у узла, но остался у обёртки.
        val anchor = node.ancestors(4).lastOrNull { Signatures.stableClassesOf(it).isNotEmpty() }
        if (anchor != null) {
            val ac = Signatures.stableClassesOf(anchor).first()
            out["${anchor.tag}.${esc(ac)} ${node.tag}"] = 0.62
            if (classes.isNotEmpty()) out["${anchor.tag}.${esc(ac)} ${node.tag}.${esc(classes.first())}"] = 0.70
        }

        // Отчаянные варианты: только структура. Живут до первого редизайна,
        // но лучше, чем отдать пользователю пустой экран.
        out[node.tag] = 0.30
        if (node.indexInParent >= 0) {
            out["${node.tag}:nth-of-type(${node.indexInParent + 1})"] = 0.22
        }

        return out.entries
            .mapNotNull { (css, stab) -> evaluate(css, stab, node, dom, scope) }
            .sortedWith(compareByDescending<SelectorCandidate> { it.isUnique }.thenByDescending { it.stability })
    }

    /**
     * Кандидаты для селектора КАРТОЧКИ: тут уникальность вредна —
     * нужен селектор, ловящий все однотипные элементы списка.
     * Оценка идёт по числу попаданий рядом с ожидаемым.
     */
    fun forRepeating(sample: Node, dom: Dom, expectedCount: Int): List<SelectorCandidate> {
        val out = LinkedHashMap<String, Double>()

        val classes = Signatures.stableClassesOf(sample)
        if (classes.isNotEmpty()) {
            out["${sample.tag}.${classes.joinToString(".") { esc(it) }}"] = 0.86
            classes.forEach { out["${sample.tag}.${esc(it)}"] = 0.80 }
            classes.forEach { out[".${esc(it)}"] = 0.70 }
        }

        for (key in STRONG_ATTRS) {
            val v = sample.attr(key)
            if (v.isNotBlank() && v.length < 40) out["${sample.tag}[$key]"] = 0.75
        }
        if (sample.hasAttr("itemtype")) {
            out["[itemtype*=\"${esc(sample.attr("itemtype").substringAfterLast('/'))}\"]"] = 0.84
        }

        // Через родителя-контейнер: >div ловит именно детей сетки,
        // не задевая вложенные обёртки внутри карточек.
        sample.parent()?.let { p ->
            val pc = Signatures.stableClassesOf(p)
            if (pc.isNotEmpty()) {
                out["${p.tag}.${esc(pc.first())} > ${sample.tag}"] = 0.78
                if (classes.isNotEmpty())
                    out["${p.tag}.${esc(pc.first())} > ${sample.tag}.${esc(classes.first())}"] = 0.88
            }
        }

        /*
         * Привязка к области документа. Каталог и блок рекомендаций
         * обычно свёрстаны одинаково — один и тот же класс карточки
         * ловит и то и другое. Отличает их только ландмарк-предок
         * (main против aside), поэтому такие варианты обязательны:
         * без них любой синтезированный селектор подмешивает
         * рекомендации в каталог, а валидатор потом видит рост числа
         * элементов и падение заполненности.
         */
        val self = if (classes.isNotEmpty()) "${sample.tag}.${esc(classes.first())}" else sample.tag
        for (anc in sample.ancestors(6).reversed()) {
            if (anc.tag in LANDMARKS) {
                out["${anc.tag} $self"] = 0.90
                out["${anc.tag} ${sample.tag}"] = 0.72
            }
            val ac = Signatures.stableClassesOf(anc).firstOrNull() ?: continue
            if (anc.tag !in LANDMARKS) out["${anc.tag}.${esc(ac)} $self"] = 0.82
        }

        return out.entries
            .mapNotNull { (css, stab) ->
                if (!Css.isValid(css)) return@mapNotNull null
                val hits = dom.select(css)
                if (hits.isEmpty() || sample !in hits) return@mapNotNull null
                SelectorCandidate(css, stab * countAffinity(hits.size, expectedCount), hits.size)
            }
            .sortedByDescending { it.stability }
    }

    /**
     * Насколько число найденных похоже на ожидаемое. Селектор, дающий
     * 32 карточки там, где всегда было 24, скорее всего подмешал
     * рекомендации — его балл нужно уронить, а не принять как есть.
     */
    private fun countAffinity(hits: Int, expected: Int): Double {
        if (expected <= 0) return 0.6
        val ratio = minOf(hits, expected).toDouble() / maxOf(hits, expected)
        return when {
            hits == expected -> 1.0
            ratio >= 0.9 -> 0.9
            ratio >= 0.7 -> 0.7
            ratio >= 0.4 -> 0.4
            else -> 0.15
        }
    }

    private fun evaluate(
        css: String, stability: Double, node: Node, dom: Dom, scope: Node?,
    ): SelectorCandidate? {
        if (!Css.isValid(css)) return null
        val hits = (scope ?: dom.root).select(css)
        if (hits.isEmpty()) return null
        if (node !in hits) return null
        // Внутри карточки уникальность обязательна: иначе поле будет
        // читаться из случайного из нескольких совпадений.
        val penalty = if (hits.size == 1) 1.0 else 1.0 / (1.0 + (hits.size - 1) * 0.35)
        return SelectorCandidate(css, stability * penalty, hits.size)
    }

    /** Экранирование для CSS-идентификаторов: класс вида "2col" валиден в HTML, но не в CSS. */
    private fun esc(s: String): String =
        s.replace("\\", "\\\\").replace("\"", "\\\"")
            .let { if (it.firstOrNull()?.isDigit() == true) "\\3${it.first()} ${it.drop(1)}" else it }

    private val STRONG_ATTRS = listOf("data-testid", "data-id", "itemprop", "name", "role", "rel")

    /** Смысловые области страницы: отличают контент от врезок. */
    private val LANDMARKS = setOf("main", "aside", "nav", "footer", "header", "section", "ul", "ol", "table")
}
