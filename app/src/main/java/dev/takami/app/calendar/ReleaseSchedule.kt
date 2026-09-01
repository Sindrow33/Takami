package dev.takami.app.calendar

import dev.takami.app.home.ContentType
import java.util.Calendar
import java.util.concurrent.TimeUnit

/** Один релиз в календаре. */
data class DayRelease(
    val dayStart: Long,
    val type: ContentType,
    val title: String,
    val issue: String,
    val time: String,
)

/**
 * Расписание релизов. Пока наполняется локально; когда автопарсер
 * начнёт отдавать даты выхода, сюда встанет его выборка — экран
 * работает с этим типом, а не с источником данных.
 *
 * Дни нормализованы к началу суток: сравнение по `Long` вместо
 * `Calendar` избавляет от вечных ошибок «тот же день, но другое время».
 */
class ReleaseSchedule(private val releases: List<DayRelease>) {

    fun releasesOn(dayStart: Long): List<DayRelease> =
        releases.filter { it.dayStart == dayStart }.sortedBy { it.time }

    /** Типы контента, выходящего в этот день — по одной точке на тип. */
    fun typesOn(dayStart: Long): List<ContentType> =
        releases.filter { it.dayStart == dayStart }
            .map { it.type }
            .distinct()
            .sortedBy { it.ordinal }

    /** Окно дней вокруг сегодняшнего: неделя назад и три вперёд. */
    fun daysAround(today: Long, back: Int = 7, forward: Int = 21): List<Long> =
        (-back..forward).map { today + TimeUnit.DAYS.toMillis(it.toLong()) }

    companion object {
        fun demo(today: Long = startOfToday()): ReleaseSchedule {
            fun day(offset: Int) = today + TimeUnit.DAYS.toMillis(offset.toLong())
            return ReleaseSchedule(
                listOf(
                    DayRelease(day(0), ContentType.Manga, "Ветер над Хакконэ", "глава 85", "10:00"),
                    DayRelease(day(0), ContentType.Anime, "Сталь и сакура", "эпизод 9", "18:30"),
                    DayRelease(day(1), ContentType.Novel, "Тихий дом на холме", "том 3, глава 13", "12:00"),
                    DayRelease(day(2), ContentType.Manga, "Полночный экспресс", "глава 7", "09:00"),
                    DayRelease(day(3), ContentType.Anime, "Клинок и облако", "эпизод 14", "21:00"),
                    DayRelease(day(3), ContentType.Manga, "Ветер над Хакконэ", "глава 86", "10:00"),
                    DayRelease(day(5), ContentType.Novel, "Слова без ветра", "том 1, глава 31", "15:00"),
                ),
            )
        }
    }
}
