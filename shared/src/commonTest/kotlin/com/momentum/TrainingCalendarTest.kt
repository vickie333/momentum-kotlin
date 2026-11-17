package com.momentum

import com.momentum.engine.TrainingCalendar
import kotlin.test.*
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

class TrainingCalendarTest {
    private var oldZone = TrainingCalendar.timeZone
    @BeforeTest fun setUp() { oldZone = TrainingCalendar.timeZone; TrainingCalendar.timeZone = TimeZone.UTC }
    @AfterTest fun tearDown() { TrainingCalendar.timeZone = oldZone }
    private fun date(text: String) = Instant.parse(text)

    @Test fun weekStartUsesMondayAndCrossesYear() {
        assertEquals(date("2026-01-05T00:00:00Z"), TrainingCalendar.weekStart(date("2026-01-11T12:00:00Z")))
        assertEquals(date("2026-01-05T00:00:00Z"), TrainingCalendar.weekStart(date("2026-01-05T12:00:00Z")))
        assertEquals(date("2025-12-29T00:00:00Z"), TrainingCalendar.weekStart(date("2026-01-01T12:00:00Z")))
    }
    @Test fun weeksBetweenClampsNegativeSpans() {
        assertEquals(2, TrainingCalendar.weeksBetween(date("2025-12-29T00:00:00Z"), date("2026-01-12T00:00:00Z")))
        assertEquals(0, TrainingCalendar.weeksBetween(date("2026-01-12T00:00:00Z"), date("2025-12-29T00:00:00Z")))
    }
    @Test fun calendarDaysSurviveDaylightSavingChange() {
        TrainingCalendar.timeZone = TimeZone.of("Europe/Madrid")
        assertEquals(2, TrainingCalendar.daysBetween(date("2026-03-28T11:00:00Z"), date("2026-03-30T10:00:00Z")))
        assertEquals(date("2026-03-29T22:00:00Z"), TrainingCalendar.weekStart(date("2026-03-30T22:00:00Z")))
        // Sunday 29 Mar 10:00 CEST belongs to the week that began Monday 23 Mar 00:00 CET.
        assertEquals(date("2026-03-22T23:00:00Z"), TrainingCalendar.weekStart(date("2026-03-29T08:00:00Z")))
        // The week containing the change is only 167 h long but still counts as one week.
        assertEquals(1, TrainingCalendar.weeksBetween(date("2026-03-22T23:00:00Z"), date("2026-03-29T22:00:00Z")))
    }
    @Test fun sameDayUsesLocalDate() {
        assertFalse(TrainingCalendar.sameDay(date("2026-01-05T23:30:00Z"), date("2026-01-06T00:30:00Z")))
        assertTrue(TrainingCalendar.sameDay(date("2026-01-05T08:00:00Z"), date("2026-01-05T23:30:00Z")))
        TrainingCalendar.timeZone = TimeZone.of("Europe/Madrid")
        // Different UTC dates, same local date (31 Mar 00:30 and 12:00 CEST).
        assertTrue(TrainingCalendar.sameDay(date("2026-03-30T22:30:00Z"), date("2026-03-31T10:00:00Z")))
        // Same UTC date, different local dates (5 Jan 23:30 and 6 Jan 00:30 CET).
        assertFalse(TrainingCalendar.sameDay(date("2026-01-05T22:30:00Z"), date("2026-01-05T23:30:00Z")))
    }
}
