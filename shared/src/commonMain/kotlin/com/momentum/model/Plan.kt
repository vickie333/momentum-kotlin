package com.momentum.model

import com.momentum.i18n.L
import com.momentum.util.roundHalfAwayFromZero
import kotlin.time.Instant

data class RampSet(val weightKg: Double, val reps: Int)
data class PlannedExercise(
    val exerciseID: String, val sets: Int, val target: Int, val repMin: Int = target, val repMax: Int = target,
    val restSeconds: Int, val weightKg: Double? = null, val repsInReserve: Int? = null,
    val reason: String? = null, val rampSets: List<RampSet> = emptyList()
) {
    val id get() = exerciseID
    val exercise get() = ExerciseLibrary.exercise(exerciseID)
    val workSeconds get() = if(exercise.kind == ExerciseKind.timed) target else (target * 3.5).roundHalfAwayFromZero().toInt()
    val totalSeconds get() = sets * (workSeconds + restSeconds) + 15 + rampSets.sumOf { (it.reps * 3.5).roundHalfAwayFromZero().toInt() + 45 }
    fun calories(weightKg: Double): Double = (exercise.met * weightKg * (sets * workSeconds) + 3.0 * weightKg * (sets * restSeconds + 15)) / 3600.0
    val targetLabel get() = if(exercise.kind == ExerciseKind.timed) "$sets × ${target}s" else if(repMax > repMin) "$sets × $repMin-$repMax" else "$sets × $target"
}
data class WeekPhase(val weekInBlock: Int, val blockLength: Int, val isDeload: Boolean, val repsInReserve: Int) {
    val label get() = if(isDeload) L("Deload week") else L("Week {0} of {1}", weekInBlock + 1, blockLength - 1)
}
data class Workout(
    val id: String, val title: String, val subtitle: String, val theme: MuscleGroup,
    val warmup: List<PlannedExercise> = emptyList(), val exercises: List<PlannedExercise>,
    val phase: WeekPhase? = null, val note: String? = null, val date: Instant? = null
) {
    val totalSeconds get() = (warmup + exercises).sumOf { it.totalSeconds }
    val minutes get() = maxOf(1, (totalSeconds / 60.0).roundHalfAwayFromZero().toInt())
    val totalSets get() = exercises.sumOf { it.sets }
    fun calories(weightKg: Double) = (warmup + exercises).sumOf { it.calories(weightKg) }.roundHalfAwayFromZero().toInt()
}
