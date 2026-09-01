package com.mangareader.reader.engine

import com.mangareader.reader.engine.decode.BorderCropper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BorderCropperTest {

    private fun page(top: Int, content: Int, bottom: Int, fieldValue: Float = 255f) =
        FloatArray(top + content + bottom) { i ->
            when {
                i < top -> fieldValue
                i >= top + content -> fieldValue
                else -> 40f + (i % 7)
            }
        }

    @Test
    fun `белые поля сверху и снизу находятся`() {
        val crop = BorderCropper.detect(page(top = 20, content = 200, bottom = 30))
        assertEquals(20, crop.top)
        assertEquals(30, crop.bottom)
    }

    @Test
    fun `чёрные поля находятся так же, как белые`() {
        val crop = BorderCropper.detect(page(top = 15, content = 200, bottom = 15, fieldValue = 0f))
        assertEquals(15, crop.top)
        assertEquals(15, crop.bottom)
    }

    @Test
    fun `страница без полей не обрезается`() {
        val crop = BorderCropper.detect(FloatArray(200) { 30f + (it % 11) })
        assertTrue(crop.isEmpty)
    }

    @Test
    fun `однотонная страница не срезается целиком`() {
        // Разделитель или пустой лист: «поле» — это вся страница.
        val crop = BorderCropper.detect(FloatArray(120) { 255f })
        assertTrue(crop.isEmpty, "однотонную страницу резать нечего, иначе она схлопнется в ноль")
    }

    @Test
    fun `обрезка не превышает четверти страницы`() {
        // Огромное поле: 80% страницы белые. Резать всё нельзя —
        // иначе одна кривая страница уедет по высоте от соседних.
        val crop = BorderCropper.detect(page(top = 400, content = 100, bottom = 0))
        assertTrue(
            crop.top <= (500 * BorderCropper.MAX_CROP_FRACTION).toInt(),
            "обрезка сверху ${crop.top} превысила предел",
        )
    }

    @Test
    fun `край в один-два пикселя не считается полем`() {
        val crop = BorderCropper.detect(page(top = 2, content = 200, bottom = 1))
        assertTrue(crop.isEmpty, "дрожание на пиксель между страницами хуже, чем неубранный край")
    }

    @Test
    fun `слишком короткий вход не ломает расчёт`() {
        assertTrue(BorderCropper.detect(FloatArray(0)).isEmpty)
        assertTrue(BorderCropper.detect(FloatArray(3) { 255f }).isEmpty)
    }
}
