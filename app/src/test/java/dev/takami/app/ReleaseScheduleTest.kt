package dev.takami.app

import dev.takami.app.calendar.ReleaseSchedule
import dev.takami.app.home.ContentType
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseScheduleTest {

    private val today = 1_700_000_000_000L
    private val schedule = ReleaseSchedule.demo(today)

    private fun day(offset: Int) = today + TimeUnit.DAYS.toMillis(offset.toLong())

    @Test
    fun `релизы дня отсортированы по времени`() {
        val times = schedule.releasesOn(day(0)).map { it.time }
        assertEquals(times.sorted(), times)
    }

    @Test
    fun `точка на каждый тип контента, а не на каждый релиз`() {
        // В день 3 два релиза: аниме и манга — значит две точки.
        val types = schedule.typesOn(day(3))
        assertEquals(2, types.size)
        assertEquals(types.distinct(), types)

        // В день 0 тоже два разных типа.
        assertTrue(schedule.typesOn(day(0)).containsAll(listOf(ContentType.Manga, ContentType.Anime)))
    }

    @Test
    fun `пустой день не выдумывает релизов`() {
        assertTrue(schedule.releasesOn(day(9)).isEmpty())
        assertTrue(schedule.typesOn(day(9)).isEmpty())
    }

    @Test
    fun `окно дней включает сегодня и прошлое`() {
        val days = schedule.daysAround(today)
        assertTrue(days.contains(today))
        assertTrue(days.first() < today)
        assertTrue(days.last() > today)
        // Дни идут подряд, без дыр и повторов.
        assertEquals(days.distinct(), days)
        assertEquals(days.sorted(), days)
    }

    @Test
    fun `релизы привязаны к началу суток и не смешиваются с соседним днём`() {
        val d0 = schedule.releasesOn(day(0))
        assertTrue(d0.isNotEmpty())
        assertTrue(d0.all { it.dayStart == day(0) })
    }
}
