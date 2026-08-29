package app.takami.core

import java.time.LocalDate
import kotlin.test.*

class SourcePickerTest {
    private var now = 1_000_000L
    private val reg = SourceRegistry(clock = { now })

    private val big = SourceInfo("a", "ReManga", lastChapter = 124)
    private val mid = SourceInfo("b", "MangaLib", lastChapter = 98)
    private val small = SourceInfo("c", "MangaDex", lang = "en", lastChapter = 40)
    private val all = listOf(small, mid, big)

    @Test fun `берётся источник с наибольшим номером главы`() {
        val p = reg.pick(all) as Pick.Auto
        assertEquals("a", p.src.id)
        assertFalse(p.degraded)
    }

    @Test fun `падение уводит на следующий по полноте`() {
        reg.reportFailure("a")
        val p = reg.pick(all) as Pick.Auto
        assertEquals("b", p.src.id)
        assertTrue(p.degraded)
        assertEquals(26, p.lostChapters)
    }

    @Test fun `кулдаун удваивается и истекает`() {
        reg.reportFailure("a")
        assertEquals(60_000L, reg.cooldownLeftMs("a"))
        reg.reportFailure("a")
        assertEquals(120_000L, reg.cooldownLeftMs("a"))
        now += 121_000L
        assertFalse(reg.isDown("a"))
        assertEquals("a", (reg.pick(all) as Pick.Auto).src.id)
    }

    @Test fun `закреплённый важнее полноты, но падение не снимает закрепление`() {
        assertEquals("c", (reg.pick(all, pinnedId = "c") as Pick.Pinned).src.id)
        reg.reportFailure("c")
        val p = reg.pick(all, pinnedId = "c") as Pick.PinnedDown
        assertEquals("a", p.src.id)
        assertEquals("c", p.pinned.id)
    }

    @Test fun `все упали - явное состояние, а не пустой список`() {
        all.forEach { reg.reportFailure(it.id) }
        assertEquals(Pick.NoneAvailable, reg.pick(all))
    }
}

class ChapterMatcherTest {
    @Test fun `склейка два в один распознаётся`() {
        val from = SourceInfo("a", "A", lastChapter = 124)
        val to = SourceInfo("b", "B", lastChapter = 62)
        val m = ChapterMatcher.detect(from, to)
        assertEquals(2, m.merge)
        assertEquals(0, m.offset)
        assertTrue(m.reliable)
    }

    @Test fun `сдвиг нумерации на единицу распознаётся`() {
        val from = SourceInfo("a", "A", lastChapter = 50)
        val to = SourceInfo("b", "B", lastChapter = 51, gaps = setOf(1))
        val m = ChapterMatcher.detect(from, to)
        assertEquals(1, m.offset)
        assertEquals(1, m.merge)
    }

    @Test fun `разные тайтлы дают низкую уверенность`() {
        val from = SourceInfo("a", "A", lastChapter = 200)
        val to = SourceInfo("b", "B", lastChapter = 12)
        assertFalse(ChapterMatcher.detect(from, to).reliable)
    }

    @Test fun `прогресс не перепрыгивает при переносе`() {
        val to = SourceInfo("b", "B", lastChapter = 62)
        val m = Match(offset = 0, merge = 2, confidence = 1.0, matched = 124, total = 124)
        val p = ChapterMatcher.translate(Progress.Paged(100, page = 7), m, to)
        assertEquals(50, p.chapter)
        assertEquals(0, (p as Progress.Paged).page)
    }

    @Test fun `главы за пределами цели откатываются к ближайшей`() {
        val to = SourceInfo("b", "B", lastChapter = 40)
        val m = Match(0, 1, 1.0, 40, 40)
        assertEquals(40, ChapterMatcher.translate(Progress.Paged(90), m, to).chapter)
    }
}

class ReadingTest {
    private val h = 3_600_000L

    @Test fun `подряд прочитанное схлопывается в одну сессию`() {
        val e = (40..47).mapIndexed { i, c ->
            ReadEvent("t1", Format.MANGA, c, 1_000_000L + i * 5 * 60_000L, "ReManga")
        }
        val s = ReadingLog.sessions(e)
        assertEquals(1, s.size)
        assertEquals(40, s[0].from)
        assertEquals(47, s[0].to)
        assertEquals(8, s[0].chapters)
    }

    @Test fun `большой перерыв начинает новую сессию`() {
        val e = listOf(
            ReadEvent("t1", Format.MANGA, 1, 0L),
            ReadEvent("t1", Format.MANGA, 2, 7 * h)
        )
        assertEquals(2, ReadingLog.sessions(e).size)
    }

    @Test fun `форматы одного тайтла не смешиваются`() {
        val e = listOf(
            ReadEvent("t1", Format.MANGA, 5, 0L),
            ReadEvent("t1", Format.ANIME, 3, 60_000L)
        )
        assertEquals(2, ReadingLog.sessions(e).size)
    }

    @Test fun `серия считается подряд идущими днями`() {
        val today = LocalDate.of(2026, 5, 20)
        val days = (0..4).map { today.minusDays(it.toLong()) }.toSet()
        assertEquals(5, Streak.current(days, today).days)
    }

    @Test fun `пропуск дня гасится заморозкой, а не обнуляет серию`() {
        val today = LocalDate.of(2026, 5, 20)
        val days = setOf(today, today.minusDays(1), today.minusDays(3))
        val r = Streak.current(days, today)
        assertEquals(3, r.days)
        assertEquals(1, r.freezesUsed)
    }

    @Test fun `заморозки кончаются и серия останавливается`() {
        val today = LocalDate.of(2026, 5, 20)
        val days = setOf(today, today.minusDays(4))
        assertEquals(1, Streak.current(days, today).days)
    }
}
