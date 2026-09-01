package com.mangareader.translate.api

import android.graphics.Bitmap
import android.graphics.RectF

/**
 * The four-stage translation pipeline (§7). Each stage is a narrow,
 * independently swappable interface so the two engine profiles ("fast /
 * offline" ML Kit-based, "quality" ONNX-detector + cloud VLM) can mix and
 * match implementations — e.g. use the ONNX detector with ML Kit OCR, if
 * that ever turns out to be the best combination for some language.
 *
 * Concrete implementations live in `:translate:onnx`, `:translate:ocr`,
 * `:translate:mt`; orchestration (ordering, batching, reading-order sort,
 * caching, glossary application) lives in `:translate:core`.
 */
interface TextDetector {
    suspend fun detect(bmp: Bitmap): List<RawBlock>
}

interface OcrEngine {
    suspend fun recognize(crops: List<Bitmap>): List<String>
}

interface Translator {
    suspend fun translate(req: BatchRequest): List<String>
}

interface Inpainter {
    suspend fun erase(bmp: Bitmap, mask: Bitmap): Bitmap
}

/**
 * The "Quality" engine profile's shortcut (§7): a cloud VLM that performs
 * OCR AND translation in a single call, given the page's text-block crops
 * in reading order. When a bound engine implements this interface,
 * `:translate:core`'s orchestrator skips the separate [OcrEngine] +
 * [Translator] round trip entirely and calls this once per page — half
 * the latency, and markedly better accuracy on vertical text where ML Kit
 * (the "fast/offline" profile's OCR) tends to struggle (§7).
 */
interface VlmOcrTranslator {
    suspend fun ocrAndTranslate(
        crops: List<Bitmap>,
        srcLang: String?,
        dstLang: String,
        glossary: List<GlossaryEntry>,
    ): List<OcrTranslateResult>
}

data class OcrTranslateResult(
    val recognizedSrcText: String,
    val translatedText: String,
)

/**
 * Output of [TextDetector.detect]: a raw detected text region, before OCR
 * or translation and before reading-order sorting groups it into the
 * final [TextBlock]. [polygon] is in the SAME pixel space as the bitmap
 * passed to [TextDetector.detect] (i.e. the analysis-decode resolution,
 * §6 — normalization to 0..1 happens later, once the block survives into
 * a [TextBlock]).
 */
data class RawBlock(
    val polygonPx: FloatArray,
    val bboxPx: RectF,
    val vertical: Boolean,
    val kind: BlockKind,
    val confidence: Float,
)

/**
 * One batched translation request for a whole page (§7: "sending a whole
 * page's lines as one request is a quality requirement, not an
 * optimization" — grouping context lets the MT engine disambiguate
 * pronouns/register across a conversation instead of translating isolated
 * fragments).
 */
data class BatchRequest(
    val seriesId: String,
    val srcLang: String,
    val dstLang: String,
    /** One entry per text block, IN READING ORDER. */
    val lines: List<String>,
    /** Accumulated title-level glossary terms to bias/pin translations (§7, §3's shared-with-novel-reader glossary). */
    val glossary: List<GlossaryEntry>,
)

data class GlossaryEntry(
    val term: String,
    val translation: String,
)
