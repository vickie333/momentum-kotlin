package com.momentum.engine

import kotlin.math.max
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

/** Weeks run Monday to Sunday everywhere in the engine. */
object TrainingCalendar {
    var timeZone: TimeZone = TimeZone.currentSystemDefault()

    fun weekStart(date: Instant): Instant {
        val d = date.toLocalDateTime(timeZone).date
        val monday = d.minus(d.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
        return monday.atStartOfDayIn(timeZone)
    }

    /** Whole weeks between the weeks containing start and date (never negative). */
    fun weeksBetween(start: Instant, date: Instant): Int {
        val days = weekStart(start).toLocalDateTime(timeZone).date
            .daysUntil(weekStart(date).toLocalDateTime(timeZone).date)
        return max(0, days / 7)
    }

    fun daysBetween(from: Instant, to: Instant): Int =
        from.toLocalDateTime(timeZone).date.daysUntil(to.toLocalDateTime(timeZone).date)

    fun sameDay(a: Instant, b: Instant): Boolean =
        a.toLocalDateTime(timeZone).date == b.toLocalDateTime(timeZone).date
}
