package com.momentum.model

import com.momentum.i18n.L
import kotlinx.serialization.Serializable

@Serializable enum class Pattern {
    squat, lunge, hinge, calves, horizontalPush, verticalPush, chestIsolation, shoulderIsolation, rearDelt,
    horizontalPull, verticalPull, biceps, triceps, coreStability, coreFlexion, coreRotation, cardio, fullBody, mobility;
    val muscle get() = when(this) {
        squat, lunge, hinge, calves -> MuscleGroup.legs
        horizontalPush, chestIsolation -> MuscleGroup.chest
        verticalPush, shoulderIsolation, rearDelt -> MuscleGroup.shoulders
        horizontalPull, verticalPull -> MuscleGroup.back
        biceps, triceps -> MuscleGroup.arms
        coreStability, coreFlexion, coreRotation -> MuscleGroup.core
        cardio -> MuscleGroup.cardio
        fullBody -> MuscleGroup.fullBody
        mobility -> MuscleGroup.mobility
    }
    val fallbacks: List<Pattern> get() = when(this) {
        verticalPull -> listOf(horizontalPull); chestIsolation -> listOf(horizontalPush)
        squat -> listOf(lunge); lunge, calves -> listOf(squat)
        shoulderIsolation -> listOf(verticalPush); coreRotation -> listOf(coreFlexion)
        coreFlexion -> listOf(coreStability); coreStability -> listOf(coreFlexion)
        else -> emptyList()
    }
    val ladder: Ladder? get() = when(this) {
        horizontalPush -> Ladder.push; verticalPush -> Ladder.verticalPush
        horizontalPull, verticalPull -> Ladder.pull; squat -> Ladder.squat
        lunge -> Ladder.lunge; hinge -> Ladder.hinge; triceps -> Ladder.triceps
        coreStability -> Ladder.coreStability; coreFlexion -> Ladder.coreFlexion
        else -> null
    }
}
@Serializable enum class Ladder { push, verticalPush, pull, squat, lunge, hinge, triceps, coreStability, coreFlexion;
    val title get() = L(when(this) { push -> "Push"; verticalPush -> "Shoulder press"; pull -> "Pull"; squat -> "Squat"; lunge -> "Lunge"; hinge -> "Hip hinge"; triceps -> "Triceps"; coreStability -> "Core stability"; coreFlexion -> "Core strength" })
}
data class ExerciseMeta(val pattern: Pattern, val isCompound: Boolean, val stress: List<BodyArea>, val isImpact: Boolean, val ladder: Ladder?, val rung: Int, val priority: Int)
object ExerciseMetaTable {
    val fallback = ExerciseMeta(Pattern.fullBody, false, emptyList(), false, null, 0, 9)
    val table: Map<String, ExerciseMeta> by lazy { ExerciseLibrary.records.associate { it.id to it.meta } }
    fun meta(id: String) = table[id] ?: fallback
    val needsPullUpBar: Set<String> = setOf("pullup")
    val ladders: Map<Ladder, Map<Int, List<String>>> by lazy {
        Ladder.entries.mapNotNull { ladder ->
            val members = table.entries.filter { it.value.ladder == ladder }.groupBy({ it.value.rung }, { it.key })
            if(members.isEmpty()) null else ladder to members.entries.sortedBy { it.key }.associate { it.key to it.value.sorted() }
        }.toMap()
    }
    fun maxRung(ladder: Ladder) = ladders[ladder]?.keys?.maxOrNull() ?: 0
}
val Exercise.meta get() = ExerciseMetaTable.meta(id)
val Exercise.pattern get() = meta.pattern
val Exercise.isCompound get() = meta.isCompound
val Exercise.isBodyweightLadderMember get() = meta.ladder != null
