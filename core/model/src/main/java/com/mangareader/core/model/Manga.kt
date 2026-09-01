package com.mangareader.core.model

/**
 * Minimal, source-agnostic identity of a manga title.
 *
 * The reader module treats this as an opaque handle: it never fetches it,
 * never lists it, never searches for it. The host application (library
 * screen, future auto-parser) owns the full catalogue model; this is only
 * the subset the reader needs to render a title bar and to scope
 * translation state (language pair, glossary) to "this manga".
 *
 * [id] must be stable across sources/mirrors for a given logical title so
 * that translation glossary and page-translation cache correctly survive a
 * mirror change. Building that stable id is the host app's job.
 */
data class MangaInfo(
    val id: String,
    val title: String,
    val coverUrl: String? = null,
    /** ISO 639-1/639-3 code of the language the raw pages are written in, if known. */
    val originalLanguage: String? = null,
)

/**
 * Minimal identity of a single chapter within a manga.
 *
 * Ordering ([number]) is required because the seamless reader (5.7) builds
 * one continuous window across chapter boundaries and must be able to ask
 * "what comes after/before this chapter" independent of [MangaPageSource]
 * round-trips when only cached metadata is available.
 */
data class ChapterInfo(
    val id: String,
    val mangaId: String,
    val number: Float,
    val title: String? = null,
    /** Scanlation/translation language of the raw source pages, if known. */
    val language: String? = null,
)
