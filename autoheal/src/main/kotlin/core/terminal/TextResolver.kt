package core.terminal

import core.model.BlockReason
import core.model.TerminalContent
import core.model.TerminalSpec
import core.model.TextChapter
import core.parse.Density
import core.parse.Dom
import core.parse.Node

/* ═══════════════════════════════════════════════════════════════════
   ТЕЛО ГЛАВЫ РАНОБЭ
   Задача обратна картиночной: текста на странице много, и почти весь он
   не относится к главе — меню, комментарии, «похожие новеллы», футер.
   Отличает тело главы плотность связного текста: длинные абзацы, мало
   ссылок. Ровно это и считает Density.
   ═══════════════════════════════════════════════════════════════════ */

class TextResolver {

    fun resolve(dom: Dom, spec: TerminalSpec?): TerminalContent {
        spec?.textSelectors?.forEach { sel ->
            dom.selectFirst(sel)?.let { node ->
                val chapter = chapterOf(node, dom)
                if (chapter != null) return TerminalContent.Text(chapter)
            }
        }

        val best = bestProseContainer(dom)
            ?: return TerminalContent.Unavailable(
                BlockReason.UNKNOWN, "тело главы не найдено: связного текста на странице нет",
            )

        val chapter = chapterOf(best, dom)
            ?: return TerminalContent.Unavailable(
                BlockReason.UNKNOWN, "найденный блок оказался пустым после чистки",
            )
        return TerminalContent.Text(chapter)
    }

    /**
     * Кандидат — узел с максимальной «прозой». Считаем не длину текста
     * (её максимум всегда у body), а произведение длины на плотность:
     * так побеждает самый глубокий узел, всё ещё содержащий главу целиком.
     */
    internal fun bestProseContainer(dom: Dom): Node? {
        var best: Node? = null
        var bestScore = 0.0

        for (n in dom.walk(WALK_LIMIT)) {
            if (n.tag in SKIP_TAGS) continue
            val m = Density.of(n)
            if (m.textLength < MIN_CHAPTER_CHARS) continue
            if (m.linkRatio > MAX_LINK_RATIO) continue
            if (m.paragraphCount < MIN_PARAGRAPHS) continue

            // Глубина как множитель: при равном тексте выигрывает
            // внутренний контейнер, а не обёртка вокруг него.
            val score = m.textLength * m.textPerElement * (1 + n.depth * 0.02)
            if (score > bestScore) { bestScore = score; best = n }
        }
        return best
    }

    private fun chapterOf(node: Node, dom: Dom): TextChapter? {
        val paragraphs = paragraphsOf(node)
        if (paragraphs.isEmpty()) return null
        val chapter = TextChapter(title = titleOf(dom), paragraphs = paragraphs)
        return chapter.takeIf { it.charCount >= MIN_CHAPTER_CHARS }
    }

    /**
     * Абзацы: сначала <p>, и только если их нет — разбиение по <br>.
     * Второй путь нужен для старых движков, где вся глава — один <div>
     * с переводами строк.
     */
    private fun paragraphsOf(node: Node): List<String> {
        val ps = node.select("p")
            .map { it.text().trim() }
            .filter { it.length >= MIN_PARAGRAPH_CHARS && !isJunk(it) }
        if (ps.size >= MIN_PARAGRAPHS) return ps

        return node.html()
            .split(BR_SPLIT)
            .map { stripTags(it).trim() }
            .filter { it.length >= MIN_PARAGRAPH_CHARS && !isJunk(it) }
    }

    private fun titleOf(dom: Dom): String? =
        (dom.selectFirst("h1") ?: dom.selectFirst("h2"))?.text()?.trim()?.takeIf { it.isNotBlank() }
            ?: dom.title

    /** Служебные строки, которые почти всегда попадают в тело. */
    private fun isJunk(s: String): Boolean {
        val low = s.lowercase().trim()
        if (low.length > 200) return false
        return JUNK_MARKERS.any { low.contains(it) }
    }

    private fun stripTags(s: String): String =
        s.replace(TAG, " ").replace("&nbsp;", " ").replace(WS, " ")

    private companion object {
        const val WALK_LIMIT = 2500
        const val MIN_CHAPTER_CHARS = 400
        const val MIN_PARAGRAPH_CHARS = 20
        const val MIN_PARAGRAPHS = 3
        const val MAX_LINK_RATIO = 0.25

        val SKIP_TAGS = setOf("script", "style", "nav", "header", "footer", "aside", "form")
        val BR_SPLIT = Regex("""(?i)<br\s*/?>\s*(<br\s*/?>\s*)+""")
        val TAG = Regex("""<[^>]+>""")
        val WS = Regex("""\s+""")

        val JUNK_MARKERS = listOf(
            "реклама", "поделиться", "комментар", "читайте также", "следующая глава",
            "предыдущая глава", "оглавление", "перевод:", "редактура:", "донат",
            "advertisement", "share this", "next chapter", "previous chapter",
        )
    }
}
