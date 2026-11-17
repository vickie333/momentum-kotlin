package com.momentum

import com.momentum.engine.TrainingCalendar
import com.momentum.i18n.AppLanguage
import com.momentum.i18n.Loc
import com.momentum.model.*
import kotlin.test.*
import kotlinx.datetime.TimeZone

class ExerciseProgressTest {
    private var oldLanguage = Loc.language
    private var oldZone = TrainingCalendar.timeZone
    @BeforeTest fun setUp() { oldLanguage = Loc.language; oldZone = TrainingCalendar.timeZone; Loc.language = AppLanguage.en; TrainingCalendar.timeZone = TimeZone.UTC }
    @AfterTest fun tearDown() { Loc.language = oldLanguage; TrainingCalendar.timeZone = oldZone }

    @Test fun weightedLiftsUseEstimatedOneRepMaxOfTheBestSet() {
        val sessions = listOf(
            T.session(T.day(0), listOf(T.log("db_bench", listOf(10 to 20.0, 8 to 22.5), 8, 12))),
            T.session(T.day(7), listOf(T.log("db_bench", listOf(10 to 22.5), 8, 12))))
        val points = ExerciseProgress.series("db_bench", sessions)
        assertEquals(2, points.size)
        assertEquals(22.5 * (1 + 8.0 / 30), points[0].value, 0.001)
        assertEquals(22.5 * (1 + 10.0 / 30), points[1].value, 0.001)
        assertTrue(ExerciseProgress.change(points)!! > 0)
    }
    @Test fun bodyweightUsesRepsAndHoldsUseSeconds() {
        val reps = ExerciseProgress.series("pushup", listOf(
            T.session(T.day(0), listOf(T.log("pushup", listOf(8 to 0.0, 10 to 0.0), 8, 15))),
            T.session(T.day(7), listOf(T.log("pushup", listOf(12 to 0.0), 8, 15)))))
        assertEquals(listOf(10.0, 12.0), reps.map { it.value })
        assertEquals(ExerciseProgress.Metric.seconds, ExerciseProgress.metric(ExerciseLibrary.exercise("plank")))
        val holds = ExerciseProgress.series("plank", listOf(
            T.session(T.day(0), listOf(T.log("plank", listOf(30 to 0.0), 20, 45, seconds = true)))))
        assertEquals(1, holds.size)
    }
    @Test fun orderedByDateAndSkipsOtherExercisesAndEmptySets() {
        val sessions = listOf(
            T.session(T.day(7), listOf(T.log("pushup", listOf(12 to 0.0), 8, 15))),
            T.session(T.day(3), listOf(T.log("bw_squat", listOf(20 to 0.0), 10, 20))),
            T.session(T.day(0), listOf(T.log("pushup", listOf(9 to 0.0), 8, 15))))
        val points = ExerciseProgress.series("pushup", sessions)
        assertEquals(listOf(9.0, 12.0), points.map { it.value })
        assertNull(ExerciseProgress.change(listOf(points[0])))
        assertTrue(ExerciseProgress.series("unknown_id", sessions).isEmpty())
    }
}
