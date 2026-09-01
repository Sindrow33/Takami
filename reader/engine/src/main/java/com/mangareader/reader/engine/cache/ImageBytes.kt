package com.mangareader.reader.engine.cache

/**
 * Проверка, что байты вообще похожи на изображение, — по сигнатуре
 * формата, до всякого декодирования.
 *
 * Нужна на входе в дисковый кеш. Кеш хранит записи неделями, поэтому
 * цена ошибки несимметрична: один раз сохранённый мусор отдаётся при
 * каждом последующем открытии главы, и «перезагрузить» его нельзя —
 * запись выглядит валидной. Ошибка сети чинится повтором, ошибка кеша
 * не чинится ничем, кроме очистки вручную.
 *
 * Откуда берётся мусор:
 *  - тело картинки, прошедшее через `String` и перекодированное в UTF-8:
 *    все байты вне ASCII заменяются на U+FFFD, длина меняется, файл
 *    больше не изображение;
 *  - HTML-страница с ошибкой или капчей, отданная с кодом 200;
 *  - оборванная загрузка.
 *
 * Проверяются сигнатуры, а не расширение URL: хостинги картинок сплошь
 * и рядом отдают WebP по адресу с `.jpg`.
 */
object ImageBytes {

    /** Минимальный осмысленный размер: меньше — это не картинка. */
    const val MIN_SIZE_BYTES = 64

    fun looksLikeImage(bytes: ByteArray): Boolean {
        if (bytes.size < MIN_SIZE_BYTES) return false
        return isJpeg(bytes) || isPng(bytes) || isWebp(bytes) || isGif(bytes) || isBmp(bytes) || isAvif(bytes)
    }

    /** JPEG: FF D8 FF */
    private fun isJpeg(b: ByteArray): Boolean =
        b[0] == 0xFF.toByte() && b[1] == 0xD8.toByte() && b[2] == 0xFF.toByte()

    /** PNG: 89 50 4E 47 0D 0A 1A 0A */
    private fun isPng(b: ByteArray): Boolean =
        b[0] == 0x89.toByte() && b[1] == 0x50.toByte() && b[2] == 0x4E.toByte() && b[3] == 0x47.toByte() &&
            b[4] == 0x0D.toByte() && b[5] == 0x0A.toByte() && b[6] == 0x1A.toByte() && b[7] == 0x0A.toByte()

    /** WebP: "RIFF" .... "WEBP" */
    private fun isWebp(b: ByteArray): Boolean =
        ascii(b, 0, "RIFF") && ascii(b, 8, "WEBP")

    /** GIF87a / GIF89a */
    private fun isGif(b: ByteArray): Boolean = ascii(b, 0, "GIF8")

    /** BMP: "BM" */
    private fun isBmp(b: ByteArray): Boolean = ascii(b, 0, "BM")

    /** AVIF/HEIF: ....ftyp + бренд */
    private fun isAvif(b: ByteArray): Boolean =
        ascii(b, 4, "ftyp") && (ascii(b, 8, "avif") || ascii(b, 8, "avis") || ascii(b, 8, "heic"))

    private fun ascii(b: ByteArray, offset: Int, text: String): Boolean {
        if (offset + text.length > b.size) return false
        for (i in text.indices) {
            if (b[offset + i] != text[i].code.toByte()) return false
        }
        return true
    }
}
