package dev.anime.player.enhance

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EnhancerAvailabilityTest {

    @Test
    fun localFilesAndDocumentsAreEligible() {
        assertTrue(EnhancerAvailability.isAvailable("file:///storage/emulated/0/a.mkv"))
        assertTrue(EnhancerAvailability.isAvailable("content://com.android.providers/document/1"))
        assertTrue(EnhancerAvailability.isAvailable("/storage/emulated/0/a.mp4"))
    }

    @Test
    fun networkStreamsAreNot() {
        assertFalse(EnhancerAvailability.isAvailable("https://example.com/a.m3u8"))
        assertFalse(EnhancerAvailability.isAvailable("http://example.com/a.mp4"))
        assertFalse(EnhancerAvailability.isAvailable("rtmp://example.com/live"))
    }

    @Test
    fun schemeCaseDoesNotMatter() {
        assertTrue(EnhancerAvailability.isAvailable("FILE:///a.mkv"))
        assertFalse(EnhancerAvailability.isAvailable("HTTPS://example.com/a.m3u8"))
    }

    @Test
    fun reasonIsShownOnlyWhenUnavailable() {
        assertNull(EnhancerAvailability.unavailableReason("file:///a.mkv"))
        assertNotNull(EnhancerAvailability.unavailableReason("https://example.com/a.m3u8"))
    }
}
