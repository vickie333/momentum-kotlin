package com.momentum.model

import com.momentum.i18n.L
import com.momentum.i18n.Lp
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

object SessionSummary {
    fun line(set: SetLog, exercise: Exercise, units: UnitSystem): String = when {
        exercise.kind == ExerciseKind.timed -> L("{0} sec", set.seconds)
        exercise.isLoaded && set.weightKg > 0 -> L("{0} × {1}", set.reps, units.formatWeight(set.weightKg))
        else -> Lp(set.reps, "{0} rep", "{0} reps")
    }
    fun summary(log: ExerciseLog, units: UnitSystem): String {
        val exercise = ExerciseLibrary.byID[log.exerciseID] ?: return ""
        return log.sets.joinToString(" · ") { line(it, exercise, units) }
    }
    data class Month(val start: Instant, val sessions: List<WorkoutSession>) { val id get() = start }
    fun months(sessions: List<WorkoutSession>, timeZone: TimeZone = TimeZone.currentSystemDefault()): List<Month> =
        sessions.groupBy { s ->
            val local = s.date.toLocalDateTime(timeZone)
            LocalDate(local.year, local.month, 1).atStartOfDayIn(timeZone)
        }.map { (start, grouped) -> Month(start, grouped.sortedByDescending { it.date }) }.sortedByDescending { it.start }
}
