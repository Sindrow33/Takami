package com.mangareader.translate.mt

import com.mangareader.translate.api.GlossaryRepository

/**
 * Automatic glossary population "as the reader reads" (§7): "The
 * glossary is automatically populated as reading progresses and is
 * stored at the manga level. It's precisely what removes 'Kaguya /
 * Kaguyya / Kaguya' inconsistencies across neighbouring pages."
 *
 * This class does NOT do named-entity recognition itself — that's a
 * modelling concern better handled by the cloud VLM path (which can be
 * asked to flag proper nouns it translated) or by a lightweight
 * heuristic upstream. What it owns is the *policy* of turning repeated
 * observations into a pinned glossary entry: a candidate term/translation
 * pair is proposed by the caller (e.g. `:translate:core`'s orchestrator,
 * after a VLM response tags certain spans as names), and only gets
 * pinned into the glossary once it's been observed consistently enough
 * to be trustworthy — a single one-off OCR misread should not corrupt
 * the shared glossary that the light-novel reader also reads from.
 */
class GlossaryAutoLearner(
    private val repository: GlossaryRepository,
    private val minObservationsToConfirm: Int = 2,
) {
    private val pendingCounts = HashMap<String, Int>()

    /**
     * Call once per detected named-entity candidate on a translated page.
     * [term] is the source-language surface form, [translation] the MT's
     * proposed rendering.
     */
    suspend fun observe(seriesId: String, term: String, translation: String) {
        val existing = repository.entriesFor(seriesId).firstOrNull { it.term.equals(term, ignoreCase = true) }
        if (existing != null) {
            // Already pinned: just bump hit count, keep the FIRST pinned
            // translation stable rather than flip-flopping between two
            // plausible renderings of the same name page to page.
            repository.recordHit(seriesId, term)
            return
        }

        val key = "$seriesId::${term.lowercase()}"
        val count = (pendingCounts[key] ?: 0) + 1
        pendingCounts[key] = count
        if (count >= minObservationsToConfirm) {
            repository.upsert(seriesId, term, translation)
            pendingCounts.remove(key)
        }
    }
}
