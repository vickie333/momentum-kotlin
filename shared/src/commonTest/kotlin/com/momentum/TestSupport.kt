package com.momentum

import com.momentum.engine.Progression
import com.momentum.engine.RepRange
import com.momentum.model.*
import kotlin.time.Instant
import kotlin.time.Duration.Companion.days

object T {
    val monday: Instant get() = day(0)
    fun day(offset: Int): Instant = Instant.parse("2026-01-05T09:00:00Z") + offset.days
    fun week(n: Int): Instant = day(7 * n)

    fun profile(level: FitnessLevel = FitnessLevel.intermediate, goal: Goal = Goal.buildMuscle,
        equipment: Equipment = Equipment.dumbbells, weekdays: List<Int> = listOf(2, 4, 6),
        minutes: Int = 45, limitations: List<BodyArea> = emptyList(), lowImpact: Boolean = false,
        maxDumbbell: Double = 0.0, sex: Sex = Sex.male, age: Int = 30, weight: Double = 80.0): UserProfile =
        UserProfile(level = level, goal = goal, equipment = equipment, trainingWeekdays = weekdays,
            sessionMinutes = minutes, limitations = limitations, lowImpactOnly = lowImpact,
            maxDumbbellKg = maxDumbbell, sex = sex, age = age, weightKg = weight,
            startDate = monday, blockStart = monday, onboarded = true)
        // Assessment.place is ported in Phase 4c; rungs remain empty here.

    // The first five parameters preserve Phase 2/3 positional call sites.
    fun log(id: String, sets: List<Pair<Int, Double>>, min: Int, max: Int, effort: Effort? = null,
        target: Int? = null, targetSets: Int? = null, seconds: Boolean = false): ExerciseLog =
        ExerciseLog(exerciseID = id, targetSets = targetSets ?: sets.size, target = target ?: min,
            sets = sets.map { SetLog(if (seconds) 0 else it.first, it.second, if (seconds) it.first else 0) },
            repMin = min, repMax = max, effort = effort)

    fun session(date: Instant, logs: List<ExerciseLog>, feedback: WorkoutFeedback? = WorkoutFeedback.justRight,
        planned: Int? = null, done: Int? = null): WorkoutSession {
        val total = logs.sumOf { it.sets.size }
        return WorkoutSession(date = date, title = "t", durationSeconds = 2400, calories = 200,
            plannedSets = planned ?: total, completedSets = done ?: total, logs = logs, feedback = feedback)
    }

    fun okSession(on: Instant): WorkoutSession = session(on, emptyList(), planned = 10, done = 10)
    fun range(id: String, profile: UserProfile): RepRange =
        Progression.repRange(ExerciseLibrary.exercise(id), profile.goal, profile.level)
}
