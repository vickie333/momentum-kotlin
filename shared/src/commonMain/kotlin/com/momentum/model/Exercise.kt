package com.momentum.model

import com.momentum.i18n.ExerciseText
import com.momentum.i18n.L
import kotlinx.serialization.Serializable

@Serializable enum class MuscleGroup { chest, back, legs, shoulders, arms, core, cardio, fullBody, mobility;
    val id get() = name
    val title get() = L(when(this) { chest -> "Chest"; back -> "Back"; legs -> "Legs"; shoulders -> "Shoulders"; arms -> "Arms"; core -> "Core"; cardio -> "Cardio"; fullBody -> "Full body"; mobility -> "Mobility" })
    val symbol get() = when(this) { chest -> "figure.strengthtraining.traditional"; back -> "figure.rower"; legs -> "figure.step.training"; shoulders -> "figure.arms.open"; arms -> "dumbbell.fill"; core -> "figure.core.training"; cardio -> "figure.highintensity.intervaltraining"; fullBody -> "figure.cross.training"; mobility -> "figure.flexibility" }
}
@Serializable enum class ExerciseKind { reps, timed }
data class Exercise(
    val id: String, val englishName: String, val muscle: MuscleGroup, val equipment: Equipment,
    val level: FitnessLevel, val kind: ExerciseKind, val met: Double, val loadRatio: Double?,
    val symbol: String, val englishSteps: List<String>
) {
    val name get() = ExerciseText.name(id, englishName)
    val steps get() = ExerciseText.steps(id, englishSteps)
    val isLoaded get() = loadRatio != null
    val videoURL: String get() = ExerciseLibrary.curatedVideos[id] ?: "https://www.youtube.com/results?search_query=" + percentEncode(L("{0} proper form tutorial", name))
}
private fun percentEncode(text: String): String = buildString {
    val hex = "0123456789ABCDEF"
    text.encodeToByteArray().forEach { byte ->
        val n = byte.toInt() and 255
        if ((n in 65..90) || (n in 97..122) || (n in 48..57) || n == 45 || n == 46 || n == 95 || n == 126) append(n.toChar())
        else { append('%'); append(hex[n shr 4]); append(hex[n and 15]) }
    }
}
