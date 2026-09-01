package com.mangareader.core.model

/**
 * Four sampled edge colors of a decoded page, ARGB ints.
 *
 * Computed once per decode (§5.6b) by median-sampling several rows/columns
 * of pixels along each edge — never a single pixel, to avoid latching onto
 * an outlier speckle. Used by the webtoon feed to build the seam-hiding
 * background gradient between adjacent pages, and by the loading
 * placeholder to match the color of the page above it before it has even
 * finished decoding.
 */
data class EdgeColors(
    val top: Int,
    val bottom: Int,
    val left: Int,
    val right: Int,
)

/**
 * Cheap, content-derived metadata about one decoded page, computed on the
 * decode thread and cached by [PageKey] so it never needs to be
 * recomputed for a page whose pixel content (hence [PageKey]) is unchanged
 * — even across mirrors, re-encodes, or online→offline transitions.
 *
 * Persistence of this type is owned by whichever module wires a concrete
 * `PageMetaCache` (see `:reader:engine`'s interface of the same shape) —
 * in this project that is `:translate:core`'s Room `page_meta` table — but
 * the type itself lives in `:core:model` because both the engine and the
 * translation layer need to read/write it without depending on each other.
 */
data class PageMeta(
    val pageKey: PageKey,
    val width: Int,
    val height: Int,
    val edgeColors: EdgeColors,
)
