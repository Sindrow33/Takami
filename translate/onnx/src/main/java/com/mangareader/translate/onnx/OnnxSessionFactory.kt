package com.mangareader.translate.onnx

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * Creates ONNX Runtime sessions with XNNPACK enabled (§7: "Plug in ONNX
 * Runtime as `onnxruntime-android` with XNNPACK") and an OPTIONAL,
 * explicitly-fallback-guarded NNAPI execution provider.
 *
 * §7 is explicit that NNAPI must not be trusted blindly: "NNAPI is
 * optional and must always have a fallback: on some devices it produces
 * garbage output." We implement that as a runtime capability probe —
 * [tryCreateWithNnapi] catches session-creation failures AND validates
 * the very first inference against a known-good sanity check supplied by
 * the caller; if either fails, we tear the session down and rebuild
 * XNNPACK-only, permanently disabling NNAPI for that model+device for the
 * remainder of the process (cached in [nnapiBlacklist]).
 */
class OnnxSessionFactory(private val context: Context) {

    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val nnapiBlacklist = mutableSetOf<String>()

    /**
     * @param assetModelPath path within `assets/` (e.g. "models/comic_text_detector.onnx").
     * @param preferNnapi caller's per-model policy; still subject to the sanity-check fallback below.
     */
    fun create(
        assetModelPath: String,
        preferNnapi: Boolean,
        sanityCheck: ((OrtSession) -> Boolean)? = null,
    ): OrtSession {
        val modelFile = materializeAsset(assetModelPath)

        if (preferNnapi && assetModelPath !in nnapiBlacklist) {
            val nnapiSession = tryCreateWithNnapi(modelFile, sanityCheck)
            if (nnapiSession != null) return nnapiSession
            nnapiBlacklist += assetModelPath
            Log.w(TAG, "NNAPI rejected for $assetModelPath, falling back to XNNPACK")
        }

        return createXnnpackOnly(modelFile)
    }

    private fun tryCreateWithNnapi(modelFile: File, sanityCheck: ((OrtSession) -> Boolean)?): OrtSession? {
        return try {
            val options = OrtSession.SessionOptions().apply {
                addNnapi()
                addXnnpack(this) // keep XNNPACK too so unsupported ops still run
            }
            val session = env.createSession(modelFile.absolutePath, options)
            if (sanityCheck != null && !sanityCheck(session)) {
                session.close()
                return null
            }
            session
        } catch (t: Throwable) {
            Log.w(TAG, "NNAPI session creation failed for ${modelFile.name}", t)
            null
        }
    }

    private fun createXnnpackOnly(modelFile: File): OrtSession {
        val options = OrtSession.SessionOptions()
        addXnnpack(options)
        return env.createSession(modelFile.absolutePath, options)
    }

    private fun addXnnpack(options: OrtSession.SessionOptions) {
        try {
            options.addXnnpack(emptyMap())
        } catch (t: Throwable) {
            Log.w(TAG, "XNNPACK EP unavailable, using default CPU EP", t)
        }
    }

    private fun OrtSession.SessionOptions.addNnapi() {
        addNnapi(emptySet())
    }

    /** Copies the model out of assets to app-internal storage so OrtEnvironment can mmap it by path. */
    private fun materializeAsset(assetModelPath: String): File {
        val dest = File(context.filesDir, "onnx_models/$assetModelPath")
        if (dest.exists() && dest.length() > 0) return dest
        dest.parentFile?.mkdirs()
        context.assets.open(assetModelPath).use { input ->
            FileOutputStream(dest).use { output -> input.copyTo(output) }
        }
        return dest
    }

    companion object {
        private const val TAG = "OnnxSessionFactory"
    }
}
