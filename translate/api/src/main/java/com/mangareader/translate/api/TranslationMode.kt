package com.mangareader.translate.api

/**
 * The three-state display mode from §5.2. Translation is a STATE the
 * whole title sits in, not a per-page action — there is deliberately no
 * "translate this page" verb anywhere in this module's API surface.
 */
enum class TranslationMode {
    /** Art as-is, no text layer drawn at all. */
    ORIGINAL,

    /** Art untouched; translation drawn on top with a semi-opaque backing. Default for language learners. */
    OVERLAY,

    /** Original text erased (inpainted/patched); translation drawn in its place. Default for casual reading. */
    REPLACE,
}

/** How SFX (onomatopoeia outside speech bubbles) are handled — a setting, per §7. */
enum class SfxPolicy { REDRAW, CAPTION, IGNORE }

/** Which engine profile drives detection/OCR/translation for a title (§7). */
enum class EngineProfile { FAST_OFFLINE, QUALITY_CLOUD }
