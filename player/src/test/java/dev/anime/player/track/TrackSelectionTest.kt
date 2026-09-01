package dev.anime.player.track

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackNamingTest {

    @Test
    fun knownLanguagesGetReadableNames() {
        assertEquals("Русский", TrackNaming.languageName("ru"))
        assertEquals("Русский", TrackNaming.languageName("rus"))
        assertEquals("Русский", TrackNaming.languageName("ru-RU"))
        assertEquals("English", TrackNaming.languageName("eng"))
        assertEquals("日本語", TrackNaming.languageName("ja"))
    }

    @Test
    fun unknownLanguageFallsBackToItsCode() {
        assertEquals("SV", TrackNaming.languageName("sv"))
    }

    @Test
    fun blankLanguageIsNull() {
        assertNull(TrackNaming.languageName(null))
        assertNull(TrackNaming.languageName("  "))
    }
}

class MediaTrackNamingTest {

    @Test
    fun labelWinsOverLanguage() {
        val track = audio(language = "ru", label = "AniLibria")
        assertEquals("AniLibria", track.displayName(1))
    }

    @Test
    fun languageIsUsedWhenLabelIsBlank() {
        assertEquals("Русский", audio(language = "ru", label = "  ").displayName(1))
    }

    @Test
    fun numberedFallbackForFullyAnonymousTrack() {
        assertEquals("Дорожка 3", audio(language = null, label = null).displayName(3))
    }

    @Test
    fun surroundAndForcedAreMarked() {
        assertEquals(
            "Русский · 5.1",
            audio(language = "ru", label = null, channels = 6).displayName(1),
        )
        assertEquals(
            "English · форсированные",
            MediaTrack(0, 0, TrackKind.Subtitle, "en", null, isForced = true).displayName(1),
        )
    }

    private fun audio(language: String?, label: String?, channels: Int = 2) =
        MediaTrack(0, 0, TrackKind.Audio, language, label, channels = channels)
}

class TrackSelectionTest {

    private val russian = MediaTrack(0, 0, TrackKind.Audio, "rus", "AniLibria")
    private val japanese = MediaTrack(0, 1, TrackKind.Audio, "jpn", "Original", isDefault = true)
    private val english = MediaTrack(0, 2, TrackKind.Audio, "eng", "Dub")

    private val subRu = MediaTrack(1, 0, TrackKind.Subtitle, "rus", "Full")
    private val subRuForced = MediaTrack(1, 1, TrackKind.Subtitle, "rus", "Signs", isForced = true)
    private val subEn = MediaTrack(1, 2, TrackKind.Subtitle, "eng", "Full")

    private val all = listOf(russian, japanese, english, subRu, subRuForced, subEn)

    @Test
    fun prefersConfiguredAudioLanguage() {
        assertEquals(russian, TrackSelection.preferredAudio(all, listOf("ru")))
        assertEquals(english, TrackSelection.preferredAudio(all, listOf("de", "en")))
    }

    @Test
    fun fallsBackToDefaultTrackThenFirst() {
        assertEquals(japanese, TrackSelection.preferredAudio(all, listOf("de")))
        val noDefault = listOf(russian, english)
        assertEquals(russian, TrackSelection.preferredAudio(noDefault, listOf("de")))
    }

    @Test
    fun noAudioTracksMeansNoSelection() {
        assertNull(TrackSelection.preferredAudio(listOf(subRu), listOf("ru")))
    }

    @Test
    fun subtitlesAreSkippedWhenAudioIsAlreadyUnderstood() {
        // Русская озвучка — полные русские субтитры не нужны, только надписи.
        assertEquals(subRuForced, TrackSelection.preferredSubtitle(all, listOf("ru"), "rus"))
    }

    @Test
    fun subtitlesAreEnabledForForeignAudio() {
        assertEquals(subRu, TrackSelection.preferredSubtitle(all, listOf("ru"), "jpn"))
    }

    @Test
    fun forcedSubtitleIsNotChosenAsTheMainOne() {
        val onlyForcedRu = listOf(subRuForced, subEn)
        assertEquals(subEn, TrackSelection.preferredSubtitle(onlyForcedRu, listOf("en"), "jpn"))
    }

    @Test
    fun understoodAudioWithoutForcedTrackLeavesSubtitlesOff() {
        val subsOnlyFull = listOf(subRu, subEn)
        assertNull(TrackSelection.preferredSubtitle(subsOnlyFull, listOf("ru"), "rus"))
    }

    @Test
    fun languageCodesOfDifferentLengthMatch() {
        assertTrue(TrackSelection.matches("rus", "ru"))
        assertTrue(TrackSelection.matches("ru", "rus"))
        assertTrue(TrackSelection.matches("ru-RU", "ru"))
        assertTrue(TrackSelection.matches("RU", "ru"))
    }

    @Test
    fun differentLanguagesDoNotMatch() {
        assertEquals(false, TrackSelection.matches("rus", "en"))
        assertEquals(false, TrackSelection.matches(null, "ru"))
        assertEquals(false, TrackSelection.matches("ru", ""))
    }

    @Test
    fun labelsNumberAnonymousTracks() {
        val labels = TrackSelection.labels(
            listOf(
                MediaTrack(0, 0, TrackKind.Audio, null, null),
                MediaTrack(0, 1, TrackKind.Audio, "ru", null),
            )
        )
        assertEquals(listOf("Дорожка 1", "Русский"), labels)
    }
}
