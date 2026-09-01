package com.mangareader.translate.core

import android.graphics.Bitmap
import com.mangareader.core.model.PageKey
import com.mangareader.translate.api.BatchRequest
import com.mangareader.translate.api.BlockKind
import com.mangareader.translate.api.GlossaryRepository
import com.mangareader.translate.api.Inpainter
import com.mangareader.translate.api.OcrEngine
import com.mangareader.translate.api.PageTranslation
import com.mangareader.translate.api.RawBlock
import com.mangareader.translate.api.TextBlock
import com.mangareader.translate.api.TextDetector
import com.mangareader.translate.api.Translator
import com.mangareader.translate.api.TranslationRepository
import com.mangareader.translate.api.VlmOcrTranslator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.PriorityQueue
import java.util.concurrent.ConcurrentHashMap

/**
 * The end-to-end pipeline orchestrator (§7): detect -> group/sort reading
 * order -> batch-OCR crops -> batch-translate whole page -> persist as
 * [PageTranslation]. This is the ONE place that stitches
 * `:translate:onnx` / `:translate:ocr` / `:translate:mt` /
 * `:translate:render`-shaped data together; none of those modules depend
 * on each other directly.
 *
 * Also owns the priority scheduler described in §7: current page highest
 * priority, then +-3 pages, then rest of chapter in background, bounded
 * concurrency, with cancellation when the reader scrolls far ahead of a
 * queued job (§7 "queue with cancellation for scrolling far ahead").
 */
class TranslationOrchestrator(
    private val detector: TextDetector,
    private val ocrEngine: OcrEngine,
    private val translator: Translator,
    private val inpainter: Inpainter,
    private val vlmOcrTranslator: VlmOcrTranslator?, // non-null only for the Quality/cloud profile
    private val translationRepository: TranslationRepository,
    private val glossaryRepository: GlossaryRepository,
    private val scope: CoroutineScope,
    maxParallelJobs: Int = 3, // "concurrency limit 2-3" §7
) {
    private val semaphore = Semaphore(maxParallelJobs)
    private val jobs = ConcurrentHashMap<String, Job>() // key = "$seriesId:$pageKeyRaw:$dstLang"

    /**
     * Enqueues translation of one page at [priority] (0 = current page,
     * lower is more urgent — mirrors §7's ordering: current, then +-3,
     * then rest of chapter). Idempotent: re-requesting an in-flight or
     * already-cached page is a no-op.
     */
    fun requestPage(
        seriesId: String,
        pageBitmap: Bitmap, // analysis-resolution software bitmap, see PageDecoder.decodeForAnalysis
        pageKey: PageKey,
        srcLang: String,
        dstLang: String,
        priority: Int,
        isRtl: Boolean,
        isWebtoon: Boolean,
    ) {
        val key = "$seriesId:${pageKey.v}:$dstLang"
        if (jobs.containsKey(key)) return

        val job = scope.launch {
            semaphore.withPermit {
                if (translationRepository.isTranslated(pageKey, dstLang)) return@withPermit
                runPipeline(seriesId, pageBitmap, pageKey, srcLang, dstLang, isRtl, isWebtoon)
            }
        }
        jobs[key] = job
    }

    /** Cancels queued/in-flight work for pages the reader has scrolled far away from (§7). */
    fun cancelExcept(keepKeys: Set<String>) {
        val toCancel = jobs.keys.filter { it !in keepKeys }
        for (k in toCancel) jobs.remove(k)?.cancel()
    }

    private suspend fun runPipeline(
        seriesId: String,
        pageBitmap: Bitmap,
        pageKey: PageKey,
        srcLang: String,
        dstLang: String,
        isRtl: Boolean,
        isWebtoon: Boolean,
    ) {
        // 1. Detect.
        val rawBlocks = detector.detect(pageBitmap)
        if (rawBlocks.isEmpty()) return

        // 2. Group into reading order (§7: RTL clusters by horizontal band
        // then right-to-left within a band; webtoon is simply top-to-bottom).
        val ordered = ReadingOrderSorter.sort(rawBlocks, isRtl = isRtl, isWebtoon = isWebtoon)

        // 3. Batch OCR (only needed when not using the VLM combined path).
        val glossary = glossaryRepository.entriesFor(seriesId)
        val srcTexts: List<String>
        val dstTexts: List<String>

        if (vlmOcrTranslator != null) {
            val crops = com.mangareader.translate.ocr.CropExtractor.extract(pageBitmap, ordered)
            val results = vlmOcrTranslator.ocrAndTranslate(crops, srcLang, dstLang, glossary)
            srcTexts = results.map { it.recognizedSrcText }
            dstTexts = results.map { it.translatedText }
        } else {
            val crops = com.mangareader.translate.ocr.CropExtractor.extract(pageBitmap, ordered)
            srcTexts = ocrEngine.recognize(crops)
            // 4. Batch translate whole page as ONE request (§7 quality requirement).
            val batchReq = BatchRequest(seriesId, srcLang, dstLang, srcTexts, glossary)
            dstTexts = translator.translate(batchReq)
        }

        // 5. Assemble normalized TextBlocks (§4: coordinates normalized 0..1).
        val w = pageBitmap.width.toFloat()
        val h = pageBitmap.height.toFloat()
        val blocks = ordered.mapIndexed { index, raw ->
            TextBlock(
                id = index,
                polygon = normalize(raw.polygonPx, w, h),
                bboxLeft = raw.bboxPx.left / w,
                bboxTop = raw.bboxPx.top / h,
                bboxRight = raw.bboxPx.right / w,
                bboxBottom = raw.bboxPx.bottom / h,
                order = index,
                vertical = raw.vertical,
                src = srcTexts.getOrElse(index) { "" },
                dst = dstTexts.getOrElse(index) { srcTexts.getOrElse(index) { "" } }, // never show empty on mismatch (§5.4)
                fgColor = android.graphics.Color.BLACK,
                bgColor = android.graphics.Color.WHITE,
                kind = raw.kind,
                confidence = raw.confidence,
            )
        }

        translationRepository.put(
            PageTranslation.create(
                pageKey = pageKey,
                srcLang = srcLang,
                dstLang = dstLang,
                blocks = blocks,
                engine = if (vlmOcrTranslator != null) "cloud-vlm" else "onnx+mlkit",
            )
        )
    }

    private fun normalize(polygonPx: FloatArray, w: Float, h: Float): FloatArray {
        val out = FloatArray(polygonPx.size)
        for (i in polygonPx.indices step 2) {
            out[i] = polygonPx[i] / w
            out[i + 1] = polygonPx[i + 1] / h
        }
        return out
    }
}

/**
 * Reading-order sorter (§7): "For RTL: cluster blocks into horizontal
 * bands with ~5% page-height tolerance, sort right-to-left within a band.
 * For webtoon: just sort by y." Order is critical — feeding a batch MT
 * call scrambled dialogue causes it to misattribute who's speaking.
 */
internal object ReadingOrderSorter {
    private const val BAND_TOLERANCE_FRACTION = 0.05f

    fun sort(blocks: List<RawBlock>, isRtl: Boolean, isWebtoon: Boolean): List<RawBlock> {
        if (isWebtoon || !isRtl) {
            return blocks.sortedWith(compareBy({ it.bboxPx.top }, { if (isRtl) -it.bboxPx.left else it.bboxPx.left }))
        }
        // RTL manga page: cluster into horizontal bands, then sort each
        // band right-to-left (largest x first).
        val sorted = blocks.sortedBy { it.bboxPx.top }
        val bands = mutableListOf<MutableList<RawBlock>>()
        var bandTop = Float.NaN
        for (block in sorted) {
            if (bands.isEmpty() || kotlin.math.abs(block.bboxPx.top - bandTop) > BAND_TOLERANCE_FRACTION * referenceHeight(blocks)) {
                bands.add(mutableListOf(block))
                bandTop = block.bboxPx.top
            } else {
                bands.last().add(block)
            }
        }
        return bands.flatMap { band -> band.sortedByDescending { it.bboxPx.left } }
    }

    private fun referenceHeight(blocks: List<RawBlock>): Float =
        blocks.maxOfOrNull { it.bboxPx.bottom } ?: 1000f
}
