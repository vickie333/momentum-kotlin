package com.momentum.model

import com.momentum.util.MomentumJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

@Serializable data class ExerciseRecord(
    val id: String, val name: String, val muscle: MuscleGroup, val equipment: Equipment,
    val level: FitnessLevel, val kind: ExerciseKind, val met: Double, val load: Double? = null,
    val symbol: String? = null, val pattern: Pattern, val compound: Boolean, val stress: List<BodyArea>,
    val impact: Boolean, val ladder: Ladder? = null, val rung: Int, val priority: Int,
    val video: String? = null, val steps: List<String>
) {
    val exercise get() = Exercise(id, name, muscle, equipment, level, kind, met, load, symbol ?: muscle.symbol, steps)
    val meta get() = ExerciseMeta(pattern, compound, stress, impact, ladder, rung, priority)
}
object ExerciseLibrary {
    val records: List<ExerciseRecord> by lazy {
        try { MomentumJson.decodeFromString<List<ExerciseRecord>>(GeneratedData.exercisesJson) }
        catch(e: Exception) { throw IllegalStateException("data/exercises.json failed to decode", e) }
    }
    val all: List<Exercise> by lazy { records.map { it.exercise } }
    val byID: Map<String, Exercise> by lazy { all.associateBy { it.id } }
    fun exercise(id: String): Exercise = byID[id] ?: all[0]
    val curatedVideos: Map<String, String> by lazy { records.mapNotNull { r -> r.video?.let { r.id to it } }.toMap() }
}
