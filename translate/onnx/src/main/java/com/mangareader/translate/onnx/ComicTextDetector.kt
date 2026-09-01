package com.mangareader.translate.onnx

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import com.mangareader.translate.api.BlockKind
import com.mangareader.translate.api.RawBlock
import com.mangareader.translate.api.TextDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.FloatBuffer

/**
 * [TextDetector] backed by the `comic-text-detector` ONNX model (§7:
 * "ONNX detector comic-text-detector (~11MB)"), used by the "Quality"
 * engine profile ahead of a cloud VLM OCR+translate call.
 *
 * Model contract assumed (standard comic-text-detector export):
 *  - input: `images`, float32 NCHW, 1x3xHxW, RGB, normalized 0..1, H=W=1024
 *  - outputs: `blocks` (Nx8 polygon corners, pixel space of the resized
 *    input), `scores` (N), `is_vertical` (N, 0/1), `is_sfx` (N, 0/1)
 *
 * This class only performs the tensor plumbing; it does NOT decide
 * reading order (that's `:translate:core`'s `ReadingOrderSorter`, since
 * order depends on manga-wide RTL/webtoon policy that this narrow
 * detector interface has no business knowing about).
 */
class ComicTextDetector(
    context: Context,
    sessionFactory: OnnxSessionFactory = OnnxSessionFactory(context),
    modelAssetPath: String = "models/comic_text_detector.onnx",
    private val inputSize: Int = 1024,
    private val scoreThreshold: Float = 0.35f,
) : TextDetector {

    private val session: OrtSession = sessionFactory.create(
        assetModelPath = modelAssetPath,
        preferNnapi = true,
        sanityCheck = { s -> s.inputNames.isNotEmpty() },
    )

    override suspend fun detect(bmp: Bitmap): List<RawBlock> = withContext(Dispatchers.Default) {
        val (tensor, scaleX, scaleY) = preprocess(bmp)
        val env = session.let { ai.onnxruntime.OrtEnvironment.getEnvironment() }
        val inputName = session.inputNames.iterator().next()

        val results = session.run(mapOf(inputName to tensor))
        tensor.close()

        try {
            val blocksTensor = results[0].value as Array<FloatArray> // N x 8
            val scoresTensor = results.getOrNull(1)?.value as? FloatArray ?: FloatArray(blocksTensor.size) { 1f }
            val verticalTensor = results.getOrNull(2)?.value as? FloatArray
            val sfxTensor = results.getOrNull(3)?.value as? FloatArray

            blocksTensor.indices.mapNotNull { i ->
                val score = scoresTensor.getOrElse(i) { 1f }
                if (score < scoreThreshold) return@mapNotNull null
                val poly = blocksTensor[i]
                val scaledPoly = FloatArray(poly.size)
                for (p in poly.indices step 2) {
                    scaledPoly[p] = poly[p] / scaleX
                    scaledPoly[p + 1] = poly[p + 1] / scaleY
                }
                val bbox = polygonBbox(scaledPoly)
                val isVertical = (verticalTensor?.getOrNull(i) ?: 0f) > 0.5f
                val isSfx = (sfxTensor?.getOrNull(i) ?: 0f) > 0.5f
                RawBlock(
                    polygonPx = scaledPoly,
                    bboxPx = bbox,
                    vertical = isVertical,
                    kind = if (isSfx) BlockKind.SFX else BlockKind.BUBBLE,
                    confidence = score,
                )
            }
        } finally {
            results.close()
        }
    }

    private fun preprocess(bmp: Bitmap): Triple<OnnxTensor, Float, Float> {
        val scaled = Bitmap.createScaledBitmap(bmp, inputSize, inputSize, true)
        val scaleX = inputSize.toFloat() / bmp.width
        val scaleY = inputSize.toFloat() / bmp.height

        val floatBuffer = FloatBuffer.allocate(3 * inputSize * inputSize)
        val pixels = IntArray(inputSize * inputSize)
        scaled.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        // NCHW, channel-planar, normalized 0..1.
        val plane = inputSize * inputSize
        for (i in pixels.indices) {
            val p = pixels[i]
            floatBuffer.put(i, ((p shr 16) and 0xFF) / 255f)          // R plane
            floatBuffer.put(plane + i, ((p shr 8) and 0xFF) / 255f)   // G plane
            floatBuffer.put(2 * plane + i, (p and 0xFF) / 255f)       // B plane
        }
        floatBuffer.rewind()
        if (scaled !== bmp) scaled.recycle()

        val env = ai.onnxruntime.OrtEnvironment.getEnvironment()
        val tensor = OnnxTensor.createTensor(env, floatBuffer, longArrayOf(1, 3, inputSize.toLong(), inputSize.toLong()))
        return Triple(tensor, scaleX, scaleY)
    }

    private fun polygonBbox(poly: FloatArray): RectF {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        for (i in poly.indices step 2) {
            minX = minOf(minX, poly[i]); maxX = maxOf(maxX, poly[i])
            minY = minOf(minY, poly[i + 1]); maxY = maxOf(maxY, poly[i + 1])
        }
        return RectF(minX, minY, maxX, maxY)
    }

    fun close() {
        session.close()
    }
}
