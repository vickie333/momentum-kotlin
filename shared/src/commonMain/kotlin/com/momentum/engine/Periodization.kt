package com.momentum.engine

import com.momentum.model.*
import com.momentum.util.roundHalfAwayFromZero
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Instant

object Periodization {
    fun blockLength(level: FitnessLevel): Int = when (level) {
        FitnessLevel.beginner -> 6
        FitnessLevel.intermediate -> 5
        FitnessLevel.advanced -> 4
    }

    fun repsInReserve(weekInBlock: Int, level: FitnessLevel, isDeload: Boolean): Int {
        if (isDeload) return 4
        return max(if (level == FitnessLevel.beginner) 2 else 1, 3 - weekInBlock)
    }

    fun phase(date: Instant, profile: UserProfile, history: List<WorkoutSession>, now: Instant): WeekPhase {
        val length = blockLength(profile.level)
        val weekStart = TrainingCalendar.weekStart(date)
        val forced = profile.deloadWeekStart
        if (forced != null && TrainingCalendar.sameDay(TrainingCalendar.weekStart(forced), weekStart)) {
            return WeekPhase(length - 1, length, true, 4)
        }
        var position = TrainingCalendar.weeksBetween(profile.blockStart, date) % length
        val last = history.map { it.date }.maxOrNull()
        if (last != null && TrainingCalendar.daysBetween(last, now) >= 14 &&
            TrainingCalendar.sameDay(TrainingCalendar.weekStart(now), weekStart)) {
            position = 0
        }
        val isDeload = position == length - 1
        return WeekPhase(position, length, isDeload, repsInReserve(position, profile.level, isDeload))
    }

    fun blockNumber(date: Instant, profile: UserProfile): Int =
        profile.blockOffset + max(0, TrainingCalendar.weeksBetween(profile.blockStart, date)) / blockLength(profile.level)

    fun restartBlock(profile: UserProfile, start: Instant, date: Instant): UserProfile {
        val next = blockNumber(date, profile) + 1
        return profile.copy(blockOffset = next, blockStart = start)
    }
}

data class FatigueScore(val score: Int, val badSessions: Int, val sessions: Int)

object Recovery {
    fun logFailed(log: ExerciseLog): Boolean {
        val minimum = log.repMin ?: return false
        if (log.sets.isEmpty()) return false
        val exercise = ExerciseLibrary.byID[log.exerciseID] ?: return false
        var sets = log.sets
        if (exercise.isLoaded) {
            val load = sets.maxOfOrNull { it.weightKg } ?: 0.0
            sets = sets.filter { it.weightKg >= load * 0.97 }
        }
        val below = sets.count { (if (exercise.kind == ExerciseKind.timed) it.seconds else it.reps) < minimum }
        return below * 2 >= sets.size
    }

    fun fatigueScore(history: List<WorkoutSession>, now: Instant): FatigueScore {
        val recent = history.filter { it.date <= now && TrainingCalendar.daysBetween(it.date, now) <= 14 }
            .sortedByDescending { it.date }.take(3)
        var total = 0
        var bad = 0
        for (session in recent) {
            var points = 0
            if (session.feedback == WorkoutFeedback.tooHard) points += 2
            if (session.completion < 0.75) points += 2
            points += min(2, session.logs.count { logFailed(it) })
            if (points > 0) bad++
            total += points
        }
        return FatigueScore(total, bad, recent.size)
    }

    fun shouldDeload(profile: UserProfile, history: List<WorkoutSession>, phase: WeekPhase, now: Instant): Boolean {
        if (phase.isDeload) return false
        if (TrainingCalendar.daysBetween(profile.startDate, now) < 14 || history.size < 4) return false
        val fatigue = fatigueScore(history, now)
        if (fatigue.sessions < 2 || fatigue.badSessions < 2 || fatigue.score < 5) return false
        val last = profile.deloadWeekStart
        if (last != null && TrainingCalendar.daysBetween(last, now) < 21) return false
        return true
    }
}

data class VolumeTarget(val low: Int, val high: Int) {
    val mid: Double get() = (low + high).toDouble() / 2
}

object VolumePlanner {
    val muscles = listOf(MuscleGroup.chest, MuscleGroup.back, MuscleGroup.legs, MuscleGroup.shoulders, MuscleGroup.arms, MuscleGroup.core)

    private fun base(muscle: MuscleGroup, level: FitnessLevel): Pair<Int, Int> {
        val row = when (muscle) {
            MuscleGroup.chest, MuscleGroup.back -> listOf(6 to 8, 10 to 12, 12 to 16)
            MuscleGroup.legs -> listOf(8 to 10, 14 to 18, 16 to 20)
            MuscleGroup.shoulders -> listOf(4 to 6, 6 to 8, 8 to 12)
            MuscleGroup.arms -> listOf(4 to 6, 6 to 10, 8 to 12)
            MuscleGroup.core -> listOf(4 to 6, 6 to 8, 6 to 10)
            else -> return 0 to 0
        }
        return row[level.rank]
    }

    fun target(muscle: MuscleGroup, level: FitnessLevel, goal: Goal): VolumeTarget {
        val (low, high) = base(muscle, level)
        if (high <= 0) return VolumeTarget(0, 0)
        val factor = goal.volumeFactor
        return VolumeTarget(max(2, (low * factor).roundHalfAwayFromZero().toInt()),
            max(3, (high * factor).roundHalfAwayFromZero().toInt()))
    }

    fun weeklyTargets(profile: UserProfile): Map<MuscleGroup, VolumeTarget> {
        val result = linkedMapOf<MuscleGroup, VolumeTarget>()
        for (muscle in muscles) result[muscle] = target(muscle, profile.level, profile.goal)
        return result
    }

    fun completedSets(sessions: List<WorkoutSession>, weekContaining: Instant): Map<MuscleGroup, Int> {
        val start = TrainingCalendar.weekStart(weekContaining)
        val counts = mutableMapOf<MuscleGroup, Int>()
        for (session in sessions) if (TrainingCalendar.sameDay(TrainingCalendar.weekStart(session.date), start)) {
            for (log in session.logs) {
                val muscle = ExerciseLibrary.byID[log.exerciseID]?.muscle ?: continue
                counts[muscle] = (counts[muscle] ?: 0) + log.sets.size
            }
        }
        val result = linkedMapOf<MuscleGroup, Int>()
        for (muscle in muscles) counts[muscle]?.let { result[muscle] = it }
        for (muscle in MuscleGroup.entries) if (muscle !in result) counts[muscle]?.let { result[muscle] = it }
        return result
    }
}
