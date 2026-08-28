package com.mangareader.translate.api

import android.graphics.RectF
import com.mangareader.core.model.PageKey
import kotlinx.serialization.Serializable

/**
 * A page's translation is DATA, not a baked bitmap (§4 — deliberately the
 * opposite of "burn the translated text into a bitmap"). This is what
 * lets:
 *  - ORIGINAL/OVERLAY/REPLACE switch instantly (§5.2): the same
 *    [TextBlock] list is just drawn or not drawn / masked or not masked
 *    on the text layer, no re-encoding.
 *  - The peek gesture (§5.3) be a pure alpha toggle on the text layer, at
 *    60fps, on any device.
 *  - Editing (§5.5) mutate one [TextBlock] in place and re-render instantly.
 *  - The exact same layout survive being displayed at a 800px preview size
 *    and a 3000px full double-page spread, and survive Activity recreation
 *    / orientation change, because [TextBlock.polygon] and [TextBlock.bbox]
 *    are normalized 0..1 relative to the page, not page-pixel absolute.
 *
 * Persisted in Room as a single JSON string per (pageKey, dstLang) row —
 * a few KB per page (§4, §8) — via `:translate:core`'s DAO layer, which
 * this module's interfaces don't need to know about.
 */
@Serializable
data class PageTranslation(
    val pageKeyRaw: String, // PageKey.v — kept as raw string for kotlinx.serialization simplicity
    val srcLang: String,
    val dstLang: String,
    val blocks: List<TextBlock>,
    val engine: String,
    val createdAt: Long,
) {
    val pageKey: PageKey get() = PageKey(pageKeyRaw)

    companion object {
        fun create(
            pageKey: PageKey,
            srcLang: String,
            dstLang: String,
            blocks: List<TextBlock>,
            engine: String,
            createdAt: Long = System.currentTimeMillis(),
        ) = PageTranslation(pageKey.v, srcLang, dstLang, blocks, engine, createdAt)
    }
}

@Serializable
data class TextBlock(
    val id: Int,
    /** Normalized 0..1 polygon points, flattened as [x0,y0,x1,y1,...]. */
    val polygon: FloatArray,
    /** Normalized 0..1 axis-aligned bounding box, redundant with [polygon] but cheap to query/clip against. */
    val bboxLeft: Float,
    val bboxTop: Float,
    val bboxRight: Float,
    val bboxBottom: Float,
    /** Reading order index within the page (§7 — critical so dialogue isn't scrambled before translation). */
    val order: Int,
    val vertical: Boolean,
    val src: String,
    val dst: String,
    val fgColor: Int,
    val bgColor: Int,
    val kind: BlockKind,
    val confidence: Float,
    val edited: Boolean = false,
) {
    val bbox: RectF get() = RectF(bboxLeft, bboxTop, bboxRight, bboxBottom)

    /**
     * Value equality including the [FloatArray] field, which does not get
     * structural equals for free from a data class. Needed so ViewModels
     * relying on equality-based recomposition/diffing (e.g. edited-block
     * detection) behave correctly.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TextBlock) return false
        return id == other.id &&
            polygon.contentEquals(other.polygon) &&
            bboxLeft == other.bboxLeft && bboxTop == other.bboxTop &&
            bboxRight == other.bboxRight && bboxBottom == other.bboxBottom &&
            order == other.order && vertical == other.vertical &&
            src == other.src && dst == other.dst &&
            fgColor == other.fgColor && bgColor == other.bgColor &&
            kind == other.kind && confidence == other.confidence && edited == other.edited
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + polygon.contentHashCode()
        result = 31 * result + bboxLeft.hashCode()
        result = 31 * result + bboxTop.hashCode()
        result = 31 * result + bboxRight.hashCode()
        result = 31 * result + bboxBottom.hashCode()
        result = 31 * result + order
        result = 31 * result + vertical.hashCode()
        result = 31 * result + src.hashCode()
        result = 31 * result + dst.hashCode()
        result = 31 * result + fgColor
        result = 31 * result + bgColor
        result = 31 * result + kind.hashCode()
        result = 31 * result + confidence.hashCode()
        result = 31 * result + edited.hashCode()
        return result
    }
}

@Serializable
enum class BlockKind { BUBBLE, SFX, CAPTION, FREE_TEXT }
