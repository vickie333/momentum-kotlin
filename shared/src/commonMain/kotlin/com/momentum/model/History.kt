package com.momentum.model

import com.momentum.i18n.L
import com.momentum.util.IsoInstant
import com.momentum.util.newId
import kotlinx.serialization.Serializable

@Serializable enum class WorkoutFeedback { tooEasy, justRight, tooHard;
    val id get() = name
    val title get() = when(this) { tooEasy -> L("Too easy"); justRight -> L("Just right"); tooHard -> L("Too hard") }
    val symbol get() = when(this) { tooEasy -> "hare.fill"; justRight -> "hand.thumbsup.fill"; tooHard -> "tortoise.fill" }
}
@Serializable enum class Effort { easy, good, hard;
    val id get() = name
    val title get() = when(this) { easy -> L("Easy"); good -> L("Good"); hard -> L("Hard") }
}
@Serializable enum class Readiness { low, normal, great;
    val id get() = name
    val title get() = when(this) { low -> L("Rough"); normal -> L("Okay"); great -> L("Great") }
    val symbol get() = when(this) { low -> "battery.25"; normal -> "battery.75"; great -> "battery.100" }
}
@Serializable data class SetLog(val reps: Int, val weightKg: Double, val seconds: Int)
@Serializable data class ExerciseLog(
    val id: String = newId(), val exerciseID: String, val targetSets: Int, val target: Int,
    val sets: List<SetLog>, val repMin: Int? = null, val repMax: Int? = null, val effort: Effort? = null
)
@Serializable data class WorkoutSession(
    val id: String = newId(), val date: IsoInstant, val title: String, val durationSeconds: Int,
    val calories: Int, val plannedSets: Int, val completedSets: Int, val logs: List<ExerciseLog>,
    val feedback: WorkoutFeedback? = null, val readiness: Readiness? = null, val wasDeload: Boolean? = null
) { val completion get() = if(plannedSets == 0) 0.0 else completedSets.toDouble() / plannedSets }
@Serializable data class WeightEntry(val id: String = newId(), val date: IsoInstant, val kg: Double)
