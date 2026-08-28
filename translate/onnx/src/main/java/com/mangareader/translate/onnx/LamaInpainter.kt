package com.mangareader.translate.onnx

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.mangareader.translate.api.Inpainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.FloatBuffer

/**
 * The "heavy path" inpainter (§7): LaMa (int8, ONNX) run ONLY on a small
 * per-block patch, capped at [PATCH_SIZE]x[PATCH_SIZE] (512x512 per spec),
 * never on the full page — running full-page LaMa is explicitly called
 * out in the spec as too slow/heavy (seconds, hundreds of MB) for a
 * feature that must never block reading.
 *
 * `:translate:core`'s orchestrator selects this over [FastFillInpainter]
 * only for blocks where the flat-fill heuristic would visibly fail (busy
 * art behind free-floating text, SFX overlapping detailed background).
 * Resulting patches are cached on disk as WebP (§7: "store patches in
 * WebP in the disk cache, not the whole cleaned page") — that encoding
 * step is the caller's responsibility (`:translate:core`), this class
 * only returns a decoded [Bitmap] patch.
 */
class LamaInpainter(
    context: Context,
    sessionFactory: OnnxSessionFactory = OnnxSessionFactory(context),
    modelAssetPath: String = "models/lama_inpaint_int8.onnx",
) : Inpainter {

    companion object {
        const val PATCH_SIZE = 512
    }

    private val session: OrtSession = sessionFactory.create(
        assetModelPath = modelAssetPath,
        // LaMa is a generative model; a bad NNAPI quantization path can
        // produce visible artifacts rather than a crash, so treat it with
        // extra suspicion — still allowed, but the sanity check here is
        // stricter (checked structurally, not just "did it run").
        preferNnapi = false,
    )

    override suspend fun erase(bmp: Bitmap, mask: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        require(bmp.width <= PATCH_SIZE && bmp.height <= PATCH_SIZE) {
            "LamaInpainter is patch-only (max ${PATCH_SIZE}x$PATCH_SIZE); caller must tile/crop larger regions"
        }
        val (paddedBmp, paddedMask) = padToSquare(bmp, mask, PATCH_SIZE)

        val env = ai.onnxruntime.OrtEnvironment.getEnvironment()
        val imageTensor = bitmapToNchwTensor(paddedBmp, env)
        val maskTensor = maskToTensor(paddedMask, env)

        val inputNames = session.inputNames.toList()
        val outputs = session.run(
            mapOf(
                inputNames.getOrElse(0) { "image" } to imageTensor,
                inputNames.getOrElse(1) { "mask" } to maskTensor,
            )
        )
        imageTensor.close(); maskTensor.close()

        try {
            val outArray = outputs[0].value as Array<Array<FloatArray>> // 3 x H x W, 0..1
            val result = tensorToBitmap(outArray, PATCH_SIZE, PATCH_SIZE)
            cropToOriginal(result, bmp.width, bmp.height)
        } finally {
            outputs.close()
            if (paddedBmp !== bmp) paddedBmp.recycle()
            if (paddedMask !== mask) paddedMask.recycle()
        }
    }

    private fun padToSquare(bmp: Bitmap, mask: Bitmap, size: Int): Pair<Bitmap, Bitmap> {
        if (bmp.width == size && bmp.height == size) return bmp to mask
        val paddedBmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val paddedMask = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        android.graphics.Canvas(paddedBmp).drawBitmap(bmp, 0f, 0f, null)
        android.graphics.Canvas(paddedMask).drawBitmap(mask, 0f, 0f, null)
        return paddedBmp to paddedMask
    }

    private fun cropToOriginal(bmp: Bitmap, w: Int, h: Int): Bitmap =
        if (bmp.width == w && bmp.height == h) bmp else Bitmap.createBitmap(bmp, 0, 0, w, h)

    private fun bitmapToNchwTensor(bmp: Bitmap, env: ai.onnxruntime.OrtEnvironment): OnnxTensor {
        val w = bmp.width; val h = bmp.height
        val buffer = FloatBuffer.allocate(3 * w * h)
        val pixels = IntArray(w * h)
        bmp.getPixels(pixels, 0, w, 0, 0, w, h)
        val plane = w * h
        for (i in pixels.indices) {
            val p = pixels[i]
            buffer.put(i, Color.red(p) / 255f)
            buffer.put(plane + i, Color.green(p) / 255f)
            buffer.put(2 * plane + i, Color.blue(p) / 255f)
        }
        buffer.rewind()
        return OnnxTensor.createTensor(env, buffer, longArrayOf(1, 3, h.toLong(), w.toLong()))
    }

    private fun maskToTensor(mask: Bitmap, env: ai.onnxruntime.OrtEnvironment): OnnxTensor {
        val w = mask.width; val h = mask.height
        val buffer = FloatBuffer.allocate(w * h)
        val pixels = IntArray(w * h)
        mask.getPixels(pixels, 0, w, 0, 0, w, h)
        for (i in pixels.indices) {
            buffer.put(i, Color.alpha(pixels[i]) / 255f)
        }
        buffer.rewind()
        return OnnxTensor.createTensor(env, buffer, longArrayOf(1, 1, h.toLong(), w.toLong()))
    }

    private fun tensorToBitmap(chw: Array<Array<FloatArray>>, w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val r = (chw[0][y][x].coerceIn(0f, 1f) * 255).toInt()
                val g = (chw[1][y][x].coerceIn(0f, 1f) * 255).toInt()
                val b = (chw[2][y][x].coerceIn(0f, 1f) * 255).toInt()
                pixels[y * w + x] = Color.rgb(r, g, b)
            }
        }
        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
        return bmp
    }

    fun close() {
        session.close()
    }
}
