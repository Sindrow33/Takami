package dev.anime.player.dub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class RoundRobinVoiceMapperTest {

    @Test
    fun sameSpeakerAlwaysGetsSameVoice() {
        val mapper = RoundRobinVoiceMapper(listOf(TtsVoice("v1", "ru"), TtsVoice("v2", "ru")))
        val first = mapper.voiceFor("A", "ru")
        val second = mapper.voiceFor("A", "ru")
        assertEquals(first.id, second.id)
    }

    @Test
    fun differentSpeakersTendToGetDifferentVoices() {
        val mapper = RoundRobinVoiceMapper(listOf(TtsVoice("v1", "ru"), TtsVoice("v2", "ru")))
        val a = mapper.voiceFor("A", "ru")
        val b = mapper.voiceFor("B", "ru")
        assertNotEquals(a.id, b.id)
    }

    @Test
    fun nullSpeakerGetsADefaultVoiceConsistently() {
        val mapper = RoundRobinVoiceMapper(listOf(TtsVoice("v1", "ru")))
        assertEquals(mapper.voiceFor(null, "ru").id, mapper.voiceFor(null, "ru").id)
    }

    @Test
    fun fallsBackToAnyVoiceWhenLanguageHasNoMatch() {
        val mapper = RoundRobinVoiceMapper(listOf(TtsVoice("v1", "en")))
        assertEquals("v1", mapper.voiceFor("A", "ru").id)
    }

    @Test(expected = IllegalArgumentException::class)
    fun requiresAtLeastOneVoice() {
        RoundRobinVoiceMapper(emptyList())
    }
}
