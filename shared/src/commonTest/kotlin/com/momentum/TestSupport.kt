package com.momentum

import com.momentum.model.*
import kotlin.time.Instant
import kotlin.time.Duration.Companion.days

object T {
    fun day(offset: Int): Instant = Instant.parse("2026-01-05T09:00:00Z") + offset.days
    fun profile(level: FitnessLevel = FitnessLevel.intermediate, goal: Goal = Goal.buildMuscle,
        equipment: Equipment = Equipment.dumbbells, weekdays: List<Int> = listOf(2,4,6)): UserProfile =
        UserProfile(level = level, goal = goal, equipment = equipment, trainingWeekdays = weekdays,
            sessionMinutes = 45, sex = Sex.male, weightKg = 80.0, startDate = day(0), blockStart = day(0), onboarded = true)
    fun log(id: String, sets: List<Pair<Int, Double>>, min: Int, max: Int, effort: Effort? = null): ExerciseLog =
        ExerciseLog(exerciseID = id, targetSets = sets.size, target = min, sets = sets.map { SetLog(it.first, it.second, 0) },
            repMin = min, repMax = max, effort = effort)
    fun session(date: Instant, logs: List<ExerciseLog>): WorkoutSession {
        val total = logs.sumOf { it.sets.size }
        return WorkoutSession(date = date, title = "t", durationSeconds = 2400, calories = 200,
            plannedSets = total, completedSets = total, logs = logs, feedback = WorkoutFeedback.justRight)
    }
}
