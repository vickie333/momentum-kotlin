package com.momentum

import com.momentum.engine.*
import com.momentum.i18n.AppLanguage
import com.momentum.i18n.Loc
import com.momentum.model.*
import kotlin.test.*
import kotlinx.datetime.TimeZone

class PeriodizationTest {
    private var oldLanguage = Loc.language
    private var oldZone = TrainingCalendar.timeZone
    @BeforeTest fun setUp() { oldLanguage = Loc.language; oldZone = TrainingCalendar.timeZone; Loc.language = AppLanguage.en; TrainingCalendar.timeZone = TimeZone.UTC }
    @AfterTest fun tearDown() { Loc.language = oldLanguage; TrainingCalendar.timeZone = oldZone }

    @Test fun blockLengthsAndReserve() {
        assertEquals(6, Periodization.blockLength(FitnessLevel.beginner))
        assertEquals(5, Periodization.blockLength(FitnessLevel.intermediate))
        assertEquals(4, Periodization.blockLength(FitnessLevel.advanced))
        assertEquals(4, Periodization.repsInReserve(1, FitnessLevel.advanced, true))
        assertEquals(2, Periodization.repsInReserve(4, FitnessLevel.beginner, false))
        assertEquals(3, Periodization.repsInReserve(0, FitnessLevel.advanced, false))
    }
    @Test fun phaseAcrossFullBlock() {
        val p = T.profile(level = FitnessLevel.intermediate)
        for (week in 0..5) {
            val phase = Periodization.phase(T.week(week), p, emptyList(), T.week(week))
            assertEquals(week % 5, phase.weekInBlock)
            assertEquals(week == 4, phase.isDeload)
        }
    }
    @Test fun forcedDeloadAndLongBreak() {
        val p = T.profile(level = FitnessLevel.intermediate).copy(deloadWeekStart = T.week(1))
        val forced = Periodization.phase(T.week(1), p, emptyList(), T.week(1))
        assertTrue(forced.isDeload); assertEquals(4, forced.repsInReserve)
        val now = T.week(3)
        val reset = Periodization.phase(now, p.copy(deloadWeekStart = null), listOf(T.okSession(T.day(6))), now)
        assertEquals(0, reset.weekInBlock)
    }
    @Test fun restartBlockUsesOldBlockNumber() {
        val p = T.profile(level = FitnessLevel.intermediate).copy(blockOffset = 1)
        val start = T.week(2)
        val restarted = Periodization.restartBlock(p, start, T.week(5))
        assertEquals(3, restarted.blockOffset); assertEquals(start, restarted.blockStart)
        assertEquals(1, p.blockOffset)
    }
    @Test fun volumeTargetAndFailedLog() {
        assertEquals(VolumeTarget(9, 10), VolumePlanner.target(MuscleGroup.chest, FitnessLevel.intermediate, Goal.getStronger))
        assertTrue(Recovery.logFailed(T.log("db_bench", listOf(5 to 20.0, 5 to 20.0, 8 to 20.0), 8, 12)))
    }
}
