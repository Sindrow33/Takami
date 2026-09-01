package dev.anime.player.host

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EpisodeQueueTest {

    private val episodes = listOf(ep(1), ep(2), ep(3))

    @Test
    fun findsNeighbours() {
        assertEquals("e2", EpisodeQueue.next(episodes, "e1")?.id)
        assertEquals("e1", EpisodeQueue.previous(episodes, "e2")?.id)
    }

    @Test
    fun noNextAfterTheLastEpisode() {
        assertNull(EpisodeQueue.next(episodes, "e3"))
    }

    @Test
    fun noPreviousBeforeTheFirst() {
        assertNull(EpisodeQueue.previous(episodes, "e1"))
    }

    @Test
    fun unknownEpisodeHasNoNeighbours() {
        assertNull(EpisodeQueue.next(episodes, "missing"))
        assertNull(EpisodeQueue.previous(episodes, "missing"))
    }

    @Test
    fun singleEpisodeHasNoNeighbours() {
        val one = listOf(ep(1))
        assertNull(EpisodeQueue.next(one, "e1"))
        assertNull(EpisodeQueue.previous(one, "e1"))
    }

    @Test
    fun promptsOnlyNearTheEnd() {
        val duration = 24 * 60 * 1000L
        assertFalse(EpisodeQueue.shouldPromptNext(0L, duration, true))
        assertFalse(EpisodeQueue.shouldPromptNext(duration / 2, duration, true))
        assertTrue(EpisodeQueue.shouldPromptNext(duration - 5_000L, duration, true))
    }

    @Test
    fun neverPromptsWithoutANextEpisode() {
        val duration = 24 * 60 * 1000L
        assertFalse(EpisodeQueue.shouldPromptNext(duration - 1_000L, duration, false))
    }

    @Test
    fun neverPromptsWhileDurationIsUnknown() {
        // Иначе предложение выскакивает на первой секунде, пока файл открывается.
        assertFalse(EpisodeQueue.shouldPromptNext(0L, 0L, true))
        assertFalse(EpisodeQueue.shouldPromptNext(500L, 0L, true))
    }

    @Test
    fun labelUsesNumberWhenKnown() {
        assertEquals("Следующая: серия 2", EpisodeQueue.nextLabel(ep(2)))
        assertEquals(
            "Следующая: special",
            EpisodeQueue.nextLabel(
                AnimeCatalog.Episode("s", "special", 0, "file:///s.mkv", true)
            ),
        )
        assertNull(EpisodeQueue.nextLabel(null))
    }

    private fun ep(number: Int) = AnimeCatalog.Episode(
        id = "e$number",
        title = "Episode $number",
        number = number,
        url = "file:///e$number.mkv",
        isLocal = true,
    )
}
