# DECISIONS — technical decision log

Short, dated-by-topic log of choices made while building this module and
why, so a future contributor (or the merge-integration pass) understands
the reasoning without re-deriving it.

1. **`PageKey` as a `@JvmInline value class` wrapping a formatted string**,
   not a data class with separate hash/width/height fields. Rationale:
   Room, Compose state, and `kotlinx.serialization` all handle a single
   `String` column trivially; keeping width/height *encoded in* the string
   (`"d:<hash>:<w>x<h>"`) means a `PageKey` is self-describing without a
   second lookup, which the §3 Hamming-fallback matcher needs cheaply and
   often.

2. **dHash (64-bit, 9x8 grayscale) over pHash/aHash.** dHash is simpler to
   compute (no DCT), cheap enough to run inline on every decode (§3's
   explicit "microseconds" requirement), and empirically robust enough to
   resize/re-encode/mirror changes for this use case. A DCT-based pHash
   would be marginally more robust to rotation/mirroring, which manga
   pages essentially never undergo in practice, so the extra cost wasn't
   justified.

3. **Dual decode is enforced at the `PageDecoder` API level, not left as a
   convention.** `decodeForDisplay()` and `decodeForAnalysis()` are
   separate functions with no shared bitmap object, specifically to make
   it structurally impossible to "optimize" by trying to read pixels back
   off a HARDWARE bitmap (§6's explicit warning) — a mistake that would
   silently crash or silently return garbage depending on API level.

4. **Integer-accumulator layout (`SeamlessLayoutEngine`) instead of
   `ceil`/`floor`-per-item rounding.** Verified algebraically: tracking
   the ideal cumulative height as a `Double` and deriving each item's
   pixel height as `round(cumulative) - round(previousCumulative)`
   guarantees `sum(itemHeights) == round(totalHeight)` and that no two
   adjacent items' rounding can produce a 1px gap or overlap — this is
   the same trick used in audio waveform/timeline renderers for the
   identical reason (§5.6a's explicit "hairline gap bothers more than an
   honest gap" callout).

5. **Background color is derived from content (`EdgeColorSampler`), never
   a theme/settings constant.** This was the single most emphasized
   "aha" in the spec (§5.6b: "background is a property of the content,
   not a setting") — implemented by sampling a band of pixels (not one
   line) per edge and taking a per-channel median, specifically to avoid
   a stray bubble border or a single bright/dark speckle skewing the
   whole seam color.

6. **Peek gesture is a pure, Android-framework-free state machine
   (`PeekGestureController`).** Kept entirely out of `View`/`MotionEvent`
   so it's unit-testable without instrumentation and so the exact
   250ms/touch-slop contract in §5.3 is verifiable in isolation from
   rendering/animation concerns. `WebtoonFeedView` only feeds it
   coordinates/timestamps and reacts to phase transitions.

7. **Translation is data (`TextBlock` with normalized polygon/bbox), never
   a baked bitmap** — this was non-negotiable per §4 and drives almost
   every other decision in `:translate:*`: instant mode switching, instant
   peek, in-place editing, and orientation/size-independent persistence
   all fall out of this one choice for free. The corollary decision —
   storing `List<TextBlock>` as a single JSON string per (pageKey, dstLang)
   row rather than a normalized per-block SQL table — follows because
   blocks are always read/written as a whole-page unit and a few KB of
   JSON is cheaper than a join for that access pattern (§4/§8).

8. **`:translate:mt` depends on `:translate:api` and `:core:database`
   ONLY — never on `:reader:engine`/`:reader:ui`/`:core:model`'s
   manga-specific types beyond what's unavoidable.** This is the module
   boundary that makes the "shared glossary with the novel reader"
   requirement (§7) actually true rather than aspirational — if this
   module had taken a `Bitmap` or a `PageRef` anywhere in its public API,
   the novel reader could not honestly reuse it.

9. **NNAPI is opt-in per model and self-disabling on failure
   (`OnnxSessionFactory`), never a blanket flag.** §7 explicitly warns
   NNAPI can produce garbage on some devices without throwing — a crash
   would be caught by the try/catch already needed for session creation,
   but silent bad output would not. The `sanityCheck` callback plus a
   process-lifetime blacklist per model+device is the minimum viable
   guard against that failure mode without needing a device allowlist
   database this project has no way to maintain.

10. **Inpainting is split into `FastFillInpainter` (default, ~90% of
    blocks) and `LamaInpainter` (patch-only, ≤512×512, for the hard
    cases)** rather than always running LaMa. Running a generative model
    over every bubble on every page would violate §9's "never block
    reading" and §7's own latency framing; flat-fill-with-feathering is
    visually indistinguishable from a proper inpaint on the common case
    of a solid-colored bubble, so gating LaMa to blocks where that
    heuristic is expected to fail (busy art behind free text, SFX on
    detailed backgrounds) is the right cost/quality trade.

11. **`FolderPageSource`/`CbzPageSource` are treated as production code,
    not test fixtures**, per the task's own framing (§2 "they later
    become the offline mode, the work isn't thrown away"). They live in
    `:reader:engine.source` alongside the interface they implement, not
    in a `test`/`sample` source set, and are documented in
    INTEGRATION.md as the literal offline/download code path.

12. **Scope cut for this pass: full Compose chrome and the `:feature:reader`
    DI graph were left as sketched extension points, not fully built.**
    Given the breadth of the spec (a full Mihon-equivalent reader PLUS a
    full four-stage translation stack), the pass prioritized getting the
    five ⭐-starred requirements (PageKey matching, seamless layout, peek
    gesture, cross-chapter feed, dual-decode/two-layer render) fully
    correct and testable over spreading effort thin across every settings
    tab and every reading mode variant. This is recorded here rather than
    silently — see ARCHITECTURE.md §6 and INTEGRATION.md §7 for the exact
    list of what's stubbed.
