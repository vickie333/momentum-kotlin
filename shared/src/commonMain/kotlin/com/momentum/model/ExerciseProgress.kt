package com.momentum.model

import com.momentum.engine.Progression
import kotlin.math.max
import kotlin.time.Instant

/** How one exercise has improved over time. */
object ExerciseProgress {
    enum class Metric { oneRepMax, reps, seconds }
    data class Point(val date: Instant, val value: Double) { val id get() = date }

    fun metric(exercise: Exercise): Metric {
        if (exercise.kind == ExerciseKind.timed) return Metric.seconds
        return if (exercise.isLoaded) Metric.oneRepMax else Metric.reps
    }

    /** One point per session that trained the exercise, oldest first. */
    fun series(exerciseID: String, sessions: List<WorkoutSession>): List<Point> {
        val exercise = ExerciseLibrary.byID[exerciseID] ?: return emptyList()
        val metric = metric(exercise)
        val points = mutableListOf<Point>()
        for (session in sessions.sortedBy { it.date }) {
            var best: Double? = null
            for (log in session.logs.filter { it.exerciseID == exerciseID }) {
                for (set in log.sets) {
                    val value: Double? = when (metric) {
                        Metric.oneRepMax -> Progression.estimatedOneRepMax(set.weightKg, set.reps)
                        Metric.reps -> if (set.reps > 0) set.reps.toDouble() else null
                        Metric.seconds -> if (set.seconds > 0) set.seconds.toDouble() else null
                    }
                    if (value != null) best = max(best ?: 0.0, value)
                }
            }
            if (best != null) points.add(Point(session.date, best))
        }
        return points
    }

    fun change(points: List<Point>): Double? =
        if (points.size < 2) null else points.last().value - points.first().value
}
