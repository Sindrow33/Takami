# ARCHITECTURE — Manga Reader Module

Standalone Android component: an immersive Mihon-style manga reader plus an
automatic, offline-and-online translation layer. This document maps the
module graph, the data flow, and every public contract. No build was run —
see the repo root for the reason (task constraint).

## 1. Module graph

```
                    ┌────────────────┐
                    │  :core:model   │  Manga/Chapter/PageRef/PageKey,
                    │                │  MangaPageSource, MangaReader contract
                    └───────▲────────┘
                            │ (api)
        ┌───────────────────┼────────────────────┐
        │                   │                    │
┌───────┴──────┐   ┌────────┴───────┐    ┌────────┴────────┐
│ :core:network│   │ :reader:engine │    │ :translate:api  │
│ OkHttp, cookie│   │ decode/layout/ │    │ Detector/Ocr/   │
│ jar (shared)  │   │ feed/cache/    │    │ Translator/     │
└──────────────┘   │ gesture, +     │    │ Inpainter iface,│
                    │ Folder/Cbz     │    │ PageTranslation,│
┌──────────────┐    │ sources        │    │ TextBlock,      │
│:core:database│    └───────▲────────┘    │ PageKey matching│
│ Room common  │            │             │ policy          │
│ converters   │            │ (impl)      └────────▲────────┘
└──────┬───────┘   ┌────────┴───────┐              │ (api)
       │           │  :reader:ui    │      ┌───────┼────────┬───────────┐
       │           │  WebtoonFeedView│      │       │        │           │
       │           │  (custom View)  │ ┌────┴───┐ ┌─┴────┐ ┌─┴──────┐ ┌──┴──────┐
       │           │  + Compose      │ │:translate│:translate│:translate│:translate│
       │           │  chrome         │ │  :onnx  ││ :ocr  ││  :mt   ││ :render │
       │           └───────▲────────┘ │ComicText││ MLKit ││MLKit MT││ Polygon │
       │                   │           │Detector,││ OCR,  ││CloudMT,││ typo,   │
       │                   │           │LaMa/Fast││ crops ││Glossary││ SFX draw│
       │                   │           │Inpainter││       ││(shared │└─────────┘
       │                   │           └─────────┘└───────┘│w/ novel│
       │                   │                                │reader) │
       │                   │                                └───▲────┘
       │                   │           ┌──────────────┐         │
       └───────────────────┼───────────┤:translate:core├─────────┘
                            │           │ orchestrator,│ (implementation
                            │           │ Room caches, │  depends on all
                            │           │ scheduler    │  translate:* + core:*)
                            │           └──────▲───────┘
                            │                  │
                    ┌───────┴──────────────────┴───┐
                    │      :feature:reader          │  DI wiring, public
                    │  MangaReaderImpl, ReaderActivity│ MangaReader impl —
                    │  (the ONLY module the host      │ the module's single
                    │   application depends on,       │ external entry point
                    │   together with :core:model)    │
                    └──────────────────────────────┘
```

Dependency direction is strictly downward/rightward in the diagram above;
`:translate:mt` and `:translate:api` know nothing about bitmaps, panels, or
manga — a deliberate design choice so `:translate:mt`'s glossary and MT
provider classes are literally reusable, unmodified, by the future
light-novel reader (§2.2/§7 of the task spec).

## 2. Data flow — reading a page (online or offline, same path)

1. Host calls `MangaReader.open(context, ReaderParams(mangaId, chapterId,
   startPage, sourceId))` → starts `ReaderActivity`.
2. `ReaderViewModel` (in `:feature:reader`, DI-constructed) resolves a bound
   `MangaPageSource` for `sourceId` and hands it to a
   `FeedController` (`:reader:engine`).
3. `FeedController` loads the chapter's `List<PageRef>`, builds a flat list
   of `FeedItem.Page` (§2.2), estimates layout heights, and starts
   prefetching. It has **no notion of "online" vs "offline"** — a
   `FolderPageSource`/`CbzPageSource` (bundled, offline reference impls) or
   a future parser-provided online source are interchangeable.
4. `PagePrefetcher`/`DiskLruPageCache` (`:reader:engine.cache`) fetch page
   bytes via `MangaPageSource.open()`, writing to local files.
5. `PageDecoder` (`:reader:engine.decode`) performs the **dual decode**:
   - HARDWARE bitmap for on-screen display (fast, GPU-resident, unreadable).
   - Separate software ARGB_8888 decode (~1280px long side) for analysis:
     `DHash.compute()` → `PageKey`, `EdgeColorSampler.sample()` → `EdgeColors`.
6. `SeamlessLayoutEngine` lays out `FeedItem`s with integer accumulation
   (no sub-pixel seam) and the resulting `WebtoonFeedView` (`:reader:ui`)
   draws pages back to back, painting the seam gradient from each page's
   own sampled edge colors (§5.6b) instead of a fixed background.
7. If the title's `TitleTranslationSettings.enabled == true`,
   `TranslationOrchestrator` (`:translate:core`) is asked for this page's
   translation, keyed by the just-computed `PageKey` — NOT by URL. It first
   checks `TranslationRepository` (Room, via `PageKeyMatcher`'s exact +
   Hamming-distance-4 fallback, §3) and only runs the detect→OCR→
   translate pipeline (§7) on a cache miss.
8. `WebtoonFeedView`'s text layer draws `TextBlock`s (normalized 0..1
   coordinates) using the SAME transform matrix as the image layer,
   clipped to that page's own bounds — instant ORIGINAL/OVERLAY/REPLACE
   switching, instant peek gesture, no re-decoding.
9. `FeedController` emits `ReaderEvent.PageRead` / `ChapterCompleted` /
   `ChapterChanged` / `TranslationReady` through `ReaderEventBus`, which
   `MangaReaderImpl.events` exposes to the host. **The reader never writes
   to a library database or calls a tracker itself** (§2.3).

## 3. Public contract (the only thing the host should compile against)

```kotlin
// :core:model — com.mangareader.core.model
interface MangaReader {
    fun open(context: Context, params: ReaderParams): Intent
    val events: Flow<ReaderEvent>
}
data class ReaderParams(val mangaId: String, val chapterId: String, val startPage: Int = 0, val sourceId: String)
sealed interface ReaderEvent { /* PageRead, ChapterCompleted, ChapterChanged, TranslationReady, Failure */ }

interface MangaPageSource {                         // implemented by the future parser AND by
    suspend fun pages(chapterId: String): List<PageRef>   // FolderPageSource/CbzPageSource (offline)
    fun open(page: PageRef): Flow<PageLoad>
    suspend fun nextChapter(chapterId: String): String?
    suspend fun prevChapter(chapterId: String): String?
}
```

`:feature:reader`'s `MangaReaderImpl` is the sole implementation of
`MangaReader`. Everything else in every module is `internal` or otherwise
not intended for cross-module use outside the dependency graph above.

## 4. Key modules, responsibility summary

| Module | Owns | Notably does NOT own |
|---|---|---|
| `:core:model` | `PageKey`, `PageRef`/`PageLoad`, `MangaPageSource`, `MangaReader` contract, `ReaderEvent` | HTTP, DB, bitmaps |
| `:core:network` | Shared OkHttp client/cookie jar builder | Site-specific interceptors (parser's job) |
| `:core:database` | Generic Room `TypeConverter`s | Any manga-specific table |
| `:reader:engine` | Decode (dual HARDWARE/software, dHash, edge colors, tiling), seamless layout, cross-chapter feed window, disk LRU + prefetch, peek/tap-zone/zoom gesture math | Any Android View/Compose UI, any translation logic |
| `:reader:ui` | `WebtoonFeedView` custom View, Compose chrome/host | Persistence, DI, pipeline orchestration |
| `:translate:api` | `PageTranslation`/`TextBlock` model, `TextDetector`/`OcrEngine`/`Translator`/`Inpainter`/`VlmOcrTranslator` interfaces, repository interfaces, `TranslationMode`/`EngineProfile`/`SfxPolicy` enums | Any concrete engine, any persistence |
| `:translate:onnx` | ONNX Runtime plumbing (XNNPACK + guarded NNAPI fallback), `ComicTextDetector`, `FastFillInpainter`, `LamaInpainter` | OCR text recognition, MT |
| `:translate:ocr` | ML Kit OCR wrapper, crop/mask extraction | Detection, translation |
| `:translate:mt` | ML Kit Translate + Cloud MT provider, **glossary Room DB shared with the future novel reader**, auto-learner | Bitmaps, pages, panels — this module is intentionally manga-agnostic |
| `:translate:render` | Polygon-fit horizontal/vertical typography, block painter (OVERLAY/REPLACE/SFX drawing) | Pipeline orchestration, persistence |
| `:translate:core` | `TranslationOrchestrator` (the §7 pipeline glue + §7 priority scheduler), Room DAOs/entities for `page_meta`/`page_translation`/`reader_prefs`/`title_translation_settings`, `PageKeyMatcher` | Any UI |
| `:feature:reader` | DI wiring, `MangaReaderImpl`, `ReaderActivity` | Nothing — it is intentionally the thin assembly layer and the only exported surface |

## 5. Model assets (not vendored)

`:translate:onnx`'s `src/main/assets/models/` expects, at app-assembly
time, two binary weight files this checkout does not include (binary
weights don't belong in source control):

- `comic_text_detector.onnx` (~11MB) — text/bubble detector.
- `lama_inpaint_int8.onnx` — patch-only (≤512×512) inpainting model.

## 6. What was implemented vs. structurally stubbed in this pass

Given the scope of a full Mihon-equivalent reader plus a full translation
stack, this pass prioritized **breadth of correct contracts and depth on
the spec's five starred (⭐) requirements** (PageKey matching §3, seamless
layout §5.6a/b, peek gesture §5.3, cross-chapter feed §5.7, dual-decode +
two-layer render §6) over exhaustive UI chrome. Concretely:

- **Fully implemented**: `:core:model`, `:reader:engine` (decode, tiling,
  layout, feed controller, cache/prefetch, gesture math, Folder/Cbz
  sources), `:translate:api`, `:translate:onnx`, `:translate:ocr`,
  `:translate:mt` (including the shared glossary DB), `:translate:render`,
  most of `:translate:core` (entities/DAOs/repositories/orchestrator/
  reading-order sorter/PageKey matcher).
- **Structurally present, intentionally trimmed**: `:reader:ui`'s
  `WebtoonFeedView` implements the seamless layout, seam gradient, peek
  gesture wiring and two-layer draw hook, but the full Compose chrome
  (top bar, page slider with translated-dot markers, settings bottom
  sheet, edit-block card, end-cap card UI, paged/LTR/RTL/tablet-spread
  reading modes) is sketched with clear extension points rather than
  fully built out — see INTEGRATION.md "Remaining work" for the precise
  list.
- **Not started**: `:feature:reader`'s DI module (Hilt/Koin bindings) and
  the ONNX session sanity-check calibration are left as the next-session
  entry point; `ReaderActivity`/`MangaReaderImpl` compile against the full
  contract but do not yet wire a concrete DI graph.
