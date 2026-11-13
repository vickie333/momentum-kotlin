package com.momentum

import com.momentum.i18n.*
import com.momentum.model.*
import kotlin.test.*
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

class HistoryTest {
    @BeforeTest fun english() { Loc.language = AppLanguage.en }
    @Test fun lines() {
        val bench = ExerciseLibrary.exercise("db_bench"); val pushup = ExerciseLibrary.exercise("pushup"); val plank = ExerciseLibrary.exercise("plank")
        assertEquals("10 × 20 kg", SessionSummary.line(SetLog(10,20.0,0), bench, UnitSystem.metric))
        assertEquals("10 × 44 lb", SessionSummary.line(SetLog(10,20.0,0), bench, UnitSystem.imperial))
        assertEquals("12 reps", SessionSummary.line(SetLog(12,0.0,0), pushup, UnitSystem.metric))
        assertEquals("1 rep", SessionSummary.line(SetLog(1,0.0,0), pushup, UnitSystem.metric))
        assertEquals("45 sec", SessionSummary.line(SetLog(0,0.0,45), plank, UnitSystem.metric))
        assertEquals("8 reps", SessionSummary.line(SetLog(8,0.0,0), bench, UnitSystem.metric))
    }
    @Test fun summary() {
        val log = T.log("db_bench", listOf(10 to 20.0,9 to 20.0,8 to 20.0),8,12)
        assertEquals("10 × 20 kg · 9 × 20 kg · 8 × 20 kg", SessionSummary.summary(log, UnitSystem.metric))
        assertEquals("", SessionSummary.summary(log.copy(exerciseID = "removed_in_v2"), UnitSystem.metric))
    }
    @Test fun months() {
        val sessions = listOf("2026-01-03", "2026-03-01", "2026-01-20", "2025-12-31").map { T.session(Instant.parse("${it}T12:00:00Z"), emptyList()) }
        val months = SessionSummary.months(sessions, TimeZone.UTC)
        assertEquals(3, months.size)
        assertEquals(listOf(3,1,12), months.map { it.start.toString().substring(5,7).toInt() })
        assertEquals(listOf(20,3), months[1].sessions.map { it.date.toString().substring(8,10).toInt() })
        assertTrue(SessionSummary.months(emptyList()).isEmpty())
    }
}
