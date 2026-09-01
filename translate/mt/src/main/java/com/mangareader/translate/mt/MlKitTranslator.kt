package com.mangareader.translate.mt

import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.mangareader.translate.api.BatchRequest
import com.mangareader.translate.api.Translator
import kotlinx.coroutines.tasks.await

/**
 * [Translator] for the "Fast / offline" engine profile (§7): ML Kit
 * Translate with downloadable per-language-pair models. Works fully
 * offline once the model pair is downloaded once; free.
 *
 * ML Kit's translator has no native batching API — it translates one
 * string at a time — but it is fast enough locally that this is an
 * acceptable trade for the offline profile, distinct from the "batch
 * whole page as one request" requirement in §7, which specifically
 * targets the CLOUD path (`CloudMtProvider`) where a single request
 * carries far more benefit (shared context, one round trip, quality).
 * We still preserve line order and count/length invariants so callers
 * that assume batch semantics (`:translate:core`'s orchestrator) don't
 * need an ML-Kit-specific code path.
 */
class MlKitTranslator : Translator {

    private val translatorCache = HashMap<String, com.google.mlkit.nl.translate.Translator>()

    override suspend fun translate(req: BatchRequest): List<String> {
        val translator = translatorFor(req.srcLang, req.dstLang)
        translator.downloadModelIfNeeded().await()

        // Apply pinned glossary terms as a pre/post substitution pass since
        // ML Kit has no native glossary/terminology injection API. This is
        // a best-effort layer: exact literal matches are swapped after MT
        // output to enforce consistent naming (§7's "Kaguya" example).
        return req.lines.map { line ->
            val raw = translator.translate(line).await()
            applyGlossary(raw, req.glossary)
        }
    }

    private fun applyGlossary(text: String, glossary: List<com.mangareader.translate.api.GlossaryEntry>): String {
        var result = text
        for (entry in glossary) {
            // Best-effort literal replace; a smarter fuzzy pass belongs to
            // :translate:core if this proves too naive for a given language.
            result = result.replace(entry.term, entry.translation, ignoreCase = true)
        }
        return result
    }

    private fun translatorFor(srcLang: String, dstLang: String): com.google.mlkit.nl.translate.Translator {
        val key = "$srcLang->$dstLang"
        return translatorCache.getOrPut(key) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.fromLanguageTag(srcLang) ?: TranslateLanguage.JAPANESE)
                .setTargetLanguage(TranslateLanguage.fromLanguageTag(dstLang) ?: TranslateLanguage.ENGLISH)
                .build()
            Translation.getClient(options)
        }
    }

    fun close() {
        translatorCache.values.forEach { it.close() }
        translatorCache.clear()
    }
}
