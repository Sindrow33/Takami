package com.mangareader.translate.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.mangareader.translate.api.OcrEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * [OcrEngine] for the "Fast / offline" engine profile (§7): ML Kit Text
 * Recognition, fully on-device, works with zero network, tiny download,
 * free forever. Covers Japanese, Korean, Chinese and Latin scripts —
 * exactly the coverage the "fast/offline" profile promises.
 *
 * One [TextRecognizer] instance is built per source-script hint because
 * ML Kit's script-specific recognizers (Japanese/Korean/Chinese) are
 * separate models; [scriptHint] lets `:translate:core` pick the right one
 * per manga's known source language instead of guessing per-crop.
 */
class MlKitOcrEngine(private val scriptHint: ScriptHint) : OcrEngine {

    enum class ScriptHint { LATIN, JAPANESE, KOREAN, CHINESE }

    private val recognizer: TextRecognizer = when (scriptHint) {
        ScriptHint.LATIN -> TextRecognition.getClient(
            com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS
        )
        ScriptHint.JAPANESE -> TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
        ScriptHint.KOREAN -> TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
        ScriptHint.CHINESE -> TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    }

    override suspend fun recognize(crops: List<Bitmap>): List<String> = withContext(Dispatchers.Default) {
        // ML Kit recognizers are not internally batched; we fan out
        // sequentially per crop but keep everything off the caller's
        // thread. `:translate:core` is responsible for parallelizing
        // across multiple OcrEngine instances/script hints if needed —
        // this class stays a simple, predictable unit.
        crops.map { crop ->
            val image = InputImage.fromBitmap(crop, 0)
            val result = recognizer.process(image).await()
            result.text
        }
    }

    fun close() {
        recognizer.close()
    }
}
