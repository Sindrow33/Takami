# INTEGRATION — Wiring the parser, the host app, and the novel reader

## 1. What the parser must implement

A single interface, in `:core:model`:

```kotlin
interface MangaPageSource {
    suspend fun pages(chapterId: String): List<PageRef>
    fun open(page: PageRef): Flow<PageLoad>
    suspend fun nextChapter(chapterId: String): String?
    suspend fun prevChapter(chapterId: String): String?
}
```

Rules the parser must follow so the reader behaves correctly:

- `pages()` must return pages in reading order, index 0 first.
- Populate `PageRef.width`/`height` whenever the source can cheaply know
  them (a manifest, EXIF, a CBZ central directory) — this lets the feed
  reserve exact layout height and eliminates jump entirely (§5.6d). If
  unknown, leave null; the reader falls back to a running-average estimate
  and corrects it once decoded.
- `PageRef.headers` (Referer/Cookie/auth) is entirely the parser's
  responsibility — the reader passes headers through unread.
- `open(page)` must emit `PageLoad.Progress` (optional, for UI) and
  exactly one terminal `PageLoad.Done(file)` or `PageLoad.Error`. The
  `File` in `Done` must be a stable, fully-written local file — the
  engine decodes it (twice — see ARCHITECTURE.md §2 step 5), possibly
  after the flow completes.
- `nextChapter`/`prevChapter` return `null` at the true edges of the
  known series — this null is exactly what triggers the two legitimate
  end-cap surfaces (§5.6) and nothing else should.
- The reader never inspects `PageRef.uri`'s scheme; `https://`, `file://`,
  or a fully custom scheme (as `CbzPageSource` uses `cbz://`) all work
  identically as long as `open()` understands its own `uri`s.

Two reference implementations are provided and are NOT throwaway: they
double as the real offline/downloaded-chapter code path (§2.1). A future
"download chapter" feature only needs to save files into the layout
`FolderPageSource` or `CbzPageSource` expects — no separate offline mode
needs to be built.

## 2. What the host application must listen for

```kotlin
sealed interface ReaderEvent {
    data class PageRead(val chapterId: String, val page: Int, val total: Int) : ReaderEvent
    data class ChapterCompleted(val chapterId: String) : ReaderEvent
    data class ChapterChanged(val fromId: String, val toId: String) : ReaderEvent
    data class TranslationReady(val pageKey: PageKey) : ReaderEvent
    data class Failure(val kind: FailureKind, val message: String) : ReaderEvent
}
```

Collect `MangaReaderImpl.events` from a process-scoped coroutine scope
(e.g. an Application-level `SharedFlow` collector), because:

- **`PageRead`** — host writes this to its own library/progress database.
  The reader deliberately does not touch that database itself (§2.3).
- **`ChapterCompleted`** — fires once the viewport has scrolled past a
  chapter's last page. In the seamless feed this fires *while scrolling
  continues into the next chapter* — do not treat it as a navigation
  event or show any "chapter finished" UI; it is purely a signal for the
  host to mark the chapter read/sync a tracker if desired.
- **`ChapterChanged`** — the "current chapter" (for title/progress
  display purposes) changed. Useful for host-side breadcrumbs; the
  reader's own UI already reflects this quietly per §5.7.
- **`TranslationReady`** — informational; the host has no obligation to
  act on it (the reader UI already reacts on its own), but a future
  cross-component feature (e.g. surfacing "N pages translated" in a
  library grid) can hook it.
- **`Failure`** — the reader always shows *original* content and a
  retry affordance itself (§5.4/§9); this event is for host-side logging/
  telemetry, not for driving reader UI.

## 3. `:core:*` modules shared with the rest of the future app

- `:core:model` — `Manga`/`Chapter` identity types, `PageKey`, the whole
  `MangaPageSource`/`MangaReader` contract. The auto-parser depends on
  this module to implement `MangaPageSource`; nothing else from this
  reader project should leak into the parser's dependency graph.
- `:core:network` — `NetworkClients.base()` + `PersistentCookieJar`. Both
  the parser and the reader's own bulk-download path should build their
  OkHttp clients from this so they share one connection pool/cache/cookie
  jar rather than each opening their own.
- `:core:database` — only generic Room `TypeConverter`s. No manga-specific
  table lives here; it exists purely so any future database in the app
  (library, novel reader, this reader's own `:translate:core` DB) can
  reuse the same JSON/collection converters instead of redefining them.

## 4. Sharing `:translate:mt` with the light-novel reader

This is the one deliberate cross-component coupling in the whole project
(§2.2/§7): character names, technique names, honorifics, and other
title-level terminology must resolve identically whether the user is
reading the manga or the novel of the same series.

Concretely:

- `GlossaryDatabase` (in `:translate:mt`) is a Room database file named
  `glossary.db`, built via `GlossaryDatabase.build(context)`. **Both** the
  manga reader's DI graph and the future novel reader's DI graph must
  construct it with this exact class/file name so they open the same
  physical file, regardless of which reader initializes it first.
- `GlossaryRepositoryImpl`, `GlossaryEntry`, `GlossaryRepository` and
  `GlossaryAutoLearner` are the only types the novel reader needs to
  depend on from `:translate:mt` — none of them reference bitmaps, pages,
  or panels.
- Both readers should key glossary rows by the SAME `seriesId` string for
  a given logical title. That id-agreement scheme (e.g. a shared
  "work id" distinct from either component's own release id) is the
  host application's responsibility to define and is out of scope here.
- `MlKitTranslator`/`CloudMtProvider` (also in `:translate:mt`) are
  reusable as-is by the novel reader for the same reason — they operate
  on plain `BatchRequest`/`List<String>`, never on images.

## 5. Compose host integration (if not using Activities)

`MangaReader.open()` returns an `Intent`. If the merged app navigates
exclusively via Compose Navigation instead of `startActivity`, wrap the
`Intent` launch in a `rememberLauncherForActivityResult`/`ActivityResultContracts.StartActivityForResult`
call from the destination composable — no additional contract is needed
on this module's side; a Composable-destination-returning variant was
intentionally left out of the minimal public contract (§2.3) to keep the
surface small, per the task's explicit ask for a single entry point.

## 6. Checklist for gluing this into the final merged app

- [ ] Parser implements `MangaPageSource`, registers a binding for each
      `sourceId` string it handles (`Map<String, MangaPageSource>` or a
      factory — host's DI choice).
- [ ] Host provides a `Context`-scoped directory for `DiskLruPageCache`
      and for `:translate:onnx`'s `OnnxSessionFactory` model materialization.
- [ ] Host vendors the two ONNX model assets under
      `:translate:onnx/src/main/assets/models/` at app-assembly time (not
      committed to this repo — see ARCHITECTURE.md §5).
- [ ] Host wires a `CloudMtProvider`/`VlmOcrTranslator` endpoint + API key
      supplier for the "Quality" engine profile (§7); the "Fast/offline"
      profile (ML Kit) needs no host configuration beyond normal ML Kit
      Play Services availability.
- [ ] Host collects `MangaReaderImpl.events` at process scope and forwards
      `PageRead`/`ChapterCompleted` into its own library/progress/tracker
      database — this module intentionally does not do it.
- [ ] Novel-reader team and manga-reader team agree on the shared
      `seriesId` scheme before either ships glossary auto-learning to
      production (§4 above).

## 7. Remaining work (honest status, see ARCHITECTURE.md §6)

- `:feature:reader`'s DI module (concrete Hilt/Koin bindings tying
  `ReaderViewModel` to `FeedController` + `TranslationOrchestrator` +
  all repositories) is not yet written — `MangaReaderImpl`/`ReaderActivity`
  compile against the full contract but the object graph itself is the
  next task.
- `:reader:ui`'s Compose chrome (top bar, page slider with per-page
  translated-dot markers §5.4, settings bottom sheet with all tabs §5.1,
  block-tap "original/translation side by side" card and the full block
  editor §5.5, end-cap card composables §5.6, paged LTR/RTL reading mode
  View, tablet double-spread) is sketched with extension points in
  `WebtoonFeedView`/`WebtoonReaderScreen` but not fully built out.
- `ComicTextDetector`'s exact input/output tensor names should be
  reconciled against whatever `comic-text-detector` ONNX export the host
  actually vendors (names used here — `images`/`blocks`/`scores`/
  `is_vertical`/`is_sfx` — follow the common community export but are not
  guaranteed to match every published conversion byte-for-byte).
- 120ms peek cross-fade and end-cap card UI need actual `ValueAnimator`/
  Compose `Animatable` wiring (state machine and hook points exist in
  `PeekGestureController`/`WebtoonFeedView`, animation implementation is
  a small follow-up).
