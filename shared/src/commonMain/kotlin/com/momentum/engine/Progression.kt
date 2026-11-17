package com.momentum.engine

import com.momentum.i18n.L
import com.momentum.model.*
import com.momentum.util.roundHalfAwayFromZero
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Instant

data class RepRange(val min: Int, val max: Int)

/** What to prescribe for the next time an exercise is done. */
data class Decision(val target: Int, val repMin: Int, val repMax: Int, val weightKg: Double?,
    val reason: String?, val addSet: Boolean = false)

object Comeback {
    fun factor(daysSinceLast: Int): Double = when {
        daysSinceLast < 10 -> 1.0
        daysSinceLast < 21 -> 0.95
        daysSinceLast < 35 -> 0.85
        daysSinceLast < 60 -> 0.75
        else -> 0.65
    }
    fun setsToRemove(daysSinceLast: Int): Int = if (daysSinceLast >= 14) 1 else 0
    fun percentLighter(daysSinceLast: Int): Int = ((1 - factor(daysSinceLast)) * 100).roundHalfAwayFromZero().toInt()
}

object Progression {
    const val deloadSetFactor = 0.6
    const val deloadLoadFactor = 0.9
    val barbellIDs: Set<String> = setOf("bench_press", "back_squat", "deadlift", "overhead_press", "barbell_row", "barbell_curl")

    fun repRange(exercise: Exercise, goal: Goal, level: FitnessLevel): RepRange {
        if (exercise.muscle == MuscleGroup.mobility) return RepRange(30, 30)
        if (exercise.kind == ExerciseKind.timed) {
            if (exercise.muscle == MuscleGroup.cardio) return RepRange(30, 45)
            return if (level == FitnessLevel.beginner) RepRange(15, 30) else RepRange(20, 45)
        }
        if (exercise.isLoaded) {
            if (exercise.isCompound) return when (goal) {
                Goal.buildMuscle -> if (level == FitnessLevel.beginner) RepRange(8, 12) else RepRange(6, 10)
                Goal.getStronger -> {
                    if (exercise.id in barbellIDs || exercise.id == "pullup") when (level) {
                        FitnessLevel.beginner -> RepRange(6, 8)
                        FitnessLevel.intermediate -> RepRange(5, 8)
                        FitnessLevel.advanced -> RepRange(3, 6)
                    } else if (level == FitnessLevel.advanced) RepRange(5, 8) else RepRange(6, 8)
                }
                Goal.loseFat -> RepRange(10, 15)
                Goal.stayFit -> RepRange(8, 12)
            }
            return when (goal) {
                Goal.buildMuscle -> RepRange(10, 15)
                Goal.getStronger -> RepRange(8, 12)
                Goal.loseFat -> RepRange(12, 20)
                Goal.stayFit -> RepRange(10, 15)
            }
        }
        if (exercise.muscle == MuscleGroup.cardio) return RepRange(8, 15)
        if (exercise.isBodyweightLadderMember && exercise.isCompound) return when (goal) {
            Goal.buildMuscle -> RepRange(8, 15)
            Goal.getStronger -> RepRange(5, 10)
            Goal.loseFat -> RepRange(10, 15)
            Goal.stayFit -> RepRange(8, 12)
        }
        return if (goal == Goal.loseFat) RepRange(12, 20) else RepRange(10, 20)
    }

    fun rest(exercise: Exercise, range: RepRange, goal: Goal, age: Int): Int {
        if (exercise.muscle == MuscleGroup.mobility) return 10
        var rest: Double = when {
            exercise.muscle == MuscleGroup.cardio -> 30.0
            exercise.kind == ExerciseKind.timed -> 40.0
            exercise.isLoaded && exercise.isCompound -> if (range.max <= 6) 150.0 else if (range.max <= 10) 120.0 else 90.0
            exercise.isLoaded -> 60.0
            else -> if (exercise.isCompound) 60.0 else 45.0
        }
        if (goal == Goal.loseFat && exercise.muscle != MuscleGroup.cardio) rest = max(30.0, rest * 0.67)
        if (age >= 50) rest *= 1.15
        return (rest / 5).roundHalfAwayFromZero().toInt() * 5
    }

    fun step(exercise: Exercise, at: Double): Double {
        if (exercise.id in barbellIDs) return if (exercise.muscle == MuscleGroup.legs) 5.0 else 2.5
        if (exercise.equipment == Equipment.fullGym) return if (exercise.muscle == MuscleGroup.legs) 5.0 else 2.5
        if (at < 10) return 1.0
        return if (at < 30) 2.0 else 2.5
    }

    fun minimumLoad(exercise: Exercise): Double {
        if (exercise.id in barbellIDs) return 20.0
        return if (exercise.equipment == Equipment.fullGym) 5.0 else 1.0
    }

    fun roundLoad(load: Double, exercise: Exercise, down: Boolean = false, clampToMinimum: Boolean = true): Double {
        val s = step(exercise, load)
        val steps = if (down) floor(load / s) else (load / s).roundHalfAwayFromZero()
        return max(if (clampToMinimum) minimumLoad(exercise) else s, steps * s)
    }

    fun dumbbellCap(exercise: Exercise, profile: UserProfile): Double? =
        if (exercise.equipment == Equipment.dumbbells && profile.maxDumbbellKg > 0) profile.maxDumbbellKg else null

    fun initialLoad(exercise: Exercise, profile: UserProfile): Double? {
        val ratio = exercise.loadRatio ?: return null
        val upper = exercise.muscle in setOf(MuscleGroup.chest, MuscleGroup.back, MuscleGroup.shoulders, MuscleGroup.arms)
        val sexFactor = when (profile.sex) {
            Sex.male -> 1.0
            Sex.female -> if (upper) 0.65 else 0.85
            Sex.unspecified -> if (upper) 0.83 else 0.92
        }
        val ageFactor = if (profile.age >= 65) 0.75 else if (profile.age >= 50) 0.9 else 1.0
        val raw = ratio * profile.weightKg * profile.level.weightFactor * sexFactor * ageFactor * 0.85
        var load = roundLoad(raw, exercise)
        dumbbellCap(exercise, profile)?.let { load = min(load, it) }
        return load
    }

    fun estimatedOneRepMax(weightKg: Double, reps: Int): Double? {
        if (weightKg <= 0 || reps !in 1..15) return null
        return weightKg * (1 + reps.toDouble() / 30)
    }

    fun recentLogs(exerciseID: String, history: List<WorkoutSession>, limit: Int): List<ExerciseLog> {
        val result = mutableListOf<ExerciseLog>()
        for (session in history.sortedByDescending { it.date }) {
            val log = session.logs.firstOrNull { it.exerciseID == exerciseID && it.sets.isNotEmpty() }
            if (log != null) {
                result.add(log)
                if (result.size == limit) break
            }
        }
        return result
    }

    fun lastDate(exerciseID: String, history: List<WorkoutSession>): Instant? = history
        .filter { session -> session.logs.any { it.exerciseID == exerciseID && it.sets.isNotEmpty() } }
        .maxOfOrNull { it.date }

    fun lastDateForMovementOf(exercise: Exercise, history: List<WorkoutSession>): Instant? = history
        .filter { session -> session.logs.any { log ->
            val logged = ExerciseLibrary.byID[log.exerciseID]
            log.sets.isNotEmpty() && logged != null && (logged.id == exercise.id || logged.pattern == exercise.pattern)
        } }.maxOfOrNull { it.date }

    fun trainedContinuously(since: Instant, history: List<WorkoutSession>, now: Instant): Boolean {
        val dates = history.map { it.date }.filter { it > since && it <= now }.sorted() + now
        var previous = since
        for (next in dates) {
            if (TrainingCalendar.daysBetween(previous, next) >= 14) return false
            previous = next
        }
        return true
    }

    private fun values(log: ExerciseLog, timed: Boolean): List<Int> = log.sets.map { if (timed) it.seconds else it.reps }
    private fun didEnoughSets(log: ExerciseLog, done: Int): Boolean = done >= max(2, log.targetSets - 1)

    fun reachedTop(log: ExerciseLog, exercise: Exercise, range: RepRange): Boolean {
        val timed = exercise.kind == ExerciseKind.timed
        if (exercise.isLoaded) {
            val load = log.sets.maxOfOrNull { it.weightKg } ?: 0.0
            val sets = log.sets.filter { it.weightKg >= load * 0.97 }
            return didEnoughSets(log, sets.size) && sets.all { it.reps >= range.max }
        }
        val hi = max(range.max, log.repMax ?: range.max)
        val v = values(log, timed)
        return didEnoughSets(log, v.size) && v.all { it >= hi }
    }

    fun failed(log: ExerciseLog, exercise: Exercise, range: RepRange, tolerance: Int = 0): Boolean {
        val timed = exercise.kind == ExerciseKind.timed
        val floor = max(1, range.min - tolerance)
        if (exercise.isLoaded) {
            val load = log.sets.maxOfOrNull { it.weightKg } ?: 0.0
            val sets = log.sets.filter { it.weightKg >= load * 0.97 }
            if (sets.isEmpty()) return false
            return sets.count { it.reps < floor } * 2 >= sets.size
        }
        val v = values(log, timed)
        if (v.isEmpty()) return false
        return v.count { it < floor } * 2 >= v.size
    }

    fun decide(exercise: Exercise, profile: UserProfile, history: List<WorkoutSession>, range: RepRange,
        holdLoad: Boolean = false): Decision {
        val logs = recentLogs(exercise.id, history, 2)
        val last = logs.firstOrNull() ?: return firstTime(exercise, profile, range)
        if (exercise.isLoaded) {
            val load = last.sets.maxOfOrNull { it.weightKg } ?: 0.0
            if (load <= 0) return firstTime(exercise, profile, range)
            val decision = decideLoaded(exercise, profile, range, logs, load)
            if (holdLoad && decision.weightKg != null && decision.weightKg > load) {
                return decision.copy(weightKg = load,
                    target = max(range.min, min(range.max, last.sets.minOfOrNull { it.reps } ?: range.min)),
                    reason = L("You're not feeling great today: same weight, no jump."))
            }
            return decision
        }
        return decideBodyweight(exercise, profile, range, last, holdLoad)
    }

    private fun firstTime(exercise: Exercise, profile: UserProfile, range: RepRange): Decision {
        if (exercise.isLoaded) return Decision((range.min + range.max) / 2, range.min, range.max,
            initialLoad(exercise, profile), L("First time: a light starting weight. Change it if it feels off."))
        return Decision(range.min, range.min, range.max, null,
            L("First time: start at a comfortable number and add a little each session."))
    }

    private fun decideLoaded(exercise: Exercise, profile: UserProfile, range: RepRange,
        logs: List<ExerciseLog>, load: Double): Decision {
        val last = logs[0]
        val sets = last.sets.filter { it.weightKg >= load * 0.97 }
        val reps = sets.map { it.reps }
        val n = sets.size
        val minReps = reps.minOrNull() ?: 0
        val effort = last.effort ?: Effort.good
        val enough = didEnoughSets(last, n)
        val stepKg = step(exercise, load)
        val cap = dumbbellCap(exercise, profile)
        val units = profile.units
        fun decision(target: Int, weight: Double, reason: String, repMax: Int = range.max) =
            Decision(target, range.min, repMax, weight, reason)
        val hitTop = reps.all { it >= range.max }
        val easyAlmost = effort == Effort.easy && reps.all { it >= range.max - 1 }
        if (enough && (hitTop || easyAlmost)) {
            if (effort == Effort.hard && hitTop) return decision(range.max, load,
                L("You reached {0} reps but it felt hard: repeat this weight to lock it in.", range.max))
            val beatBy3 = reps.all { it >= range.max + 3 } && effort != Effort.hard
            var next = load + stepKg * (if (beatBy3) 2 else 1)
            if (cap != null && next > cap) {
                if (load >= cap - 0.001) {
                    val newMax = min(20, max(range.max, minReps + 2))
                    val target = min(newMax, minReps + 1)
                    return decision(target, load,
                        L("This is your heaviest dumbbell, so keep adding reps (up to {0}).", newMax), newMax)
                }
                next = cap
            }
            next = roundLoad(next, exercise, clampToMinimum = false)
            if (cap != null) next = min(next, cap)
            val amount = units.formatWeight(next - load)
            val sentence = if (hitTop) L("Up {0}: you hit {1} reps on every set.", amount, range.max)
                else L("Up {0}: you reached the top of the range and it felt easy.", amount)
            return decision(range.min, next, sentence)
        }
        if (!enough) return decision(range.min, load,
            L("You skipped some sets last time: same weight, get all your sets in."))
        val below = reps.count { it < range.min }
        if (below == 0) {
            val target = min(range.max, minReps + 1)
            return decision(target, load, L("Same weight: aim for {0} reps on every set.", target))
        }
        if (below * 2 >= n) {
            val severe = minReps <= range.min - 4
            var previousFailed = false
            if (logs.size > 1) {
                val prev = logs[1]
                val prevLoad = prev.sets.maxOfOrNull { it.weightKg } ?: 0.0
                previousFailed = prevLoad >= load * 0.97 && failed(prev, exercise, range)
            }
            if (severe || previousFailed) {
                val lighter = roundLoad(load * 0.9, exercise, down = true, clampToMinimum = false)
                val why = if (severe && !previousFailed)
                    L("That was much heavier than planned: dropping about 10%.")
                else L("Two tough sessions in a row: dropping about 10% to rebuild.")
                return decision(range.min, min(lighter, load), why)
            }
            return decision(range.min, load, L("You missed the minimum reps: repeat this weight and beat it."))
        }
        return decision(range.min, load, L("One set fell short: repeat the weight and get every set."))
    }

    private fun decideBodyweight(exercise: Exercise, profile: UserProfile, range: RepRange,
        last: ExerciseLog, holdLoad: Boolean): Decision {
        val timed = exercise.kind == ExerciseKind.timed
        val v = values(last, timed)
        val minV = v.minOrNull() ?: 0
        val effort = last.effort ?: Effort.good
        val hiEff = max(range.max, last.repMax ?: range.max)
        val enough = didEnoughSets(last, v.size)
        val tolerance = if (timed) 5 else 1
        val hitTop = enough && v.all { it >= hiEff }
        val easyAlmost = enough && effort == Effort.easy && v.all { it >= hiEff - tolerance }
        fun decision(target: Int, reason: String, repMax: Int = hiEff, addSet: Boolean = false) =
            Decision(target, range.min, repMax, null, reason, addSet)
        if ((hitTop || easyAlmost) && !holdLoad) {
            val ceiling = if (timed) 90 else 25
            if (hiEff >= ceiling) return decision(hiEff,
                L("You've maxed this out: add a set, or switch to a harder variation."), addSet = true)
            val newMax = min(ceiling, hiEff + if (timed) 10 else 3)
            val target = min(newMax, hiEff + if (timed) 5 else 1)
            val extended = if (timed)
                L("You reached the top and there's no harder variation available: adding seconds.")
            else L("You reached the top and there's no harder variation available: adding reps.")
            return decision(target, extended, newMax)
        }
        if (!enough) return decision(range.min, L("You skipped some sets last time: get all your sets in."))
        val below = v.count { it < range.min }
        if (below == 0) {
            val target = min(hiEff, minV + if (timed) 5 else 1)
            return decision(target, if (timed) L("Aim for {0} seconds on every set.", target)
                else L("Aim for {0} reps on every set.", target))
        }
        return decision(range.min, L("Below the minimum last time: repeat and build back up."))
    }
}
