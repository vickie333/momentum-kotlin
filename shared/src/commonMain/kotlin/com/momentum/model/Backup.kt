package com.momentum.model

import com.momentum.util.IsoInstant
import com.momentum.util.MomentumJson
import com.momentum.util.MomentumPrettyJson
import com.momentum.util.sortedKeys
import com.momentum.util.roundHalfAwayFromZero
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.jsonPrimitive

@Serializable data class PersonalData(val profile: UserProfile, val sessions: List<WorkoutSession>, val weights: List<WeightEntry>)
@Serializable data class BackupFile(val format: String, val version: Int, val exportedAt: IsoInstant, val profile: UserProfile, val sessions: List<WorkoutSession>, val weights: List<WeightEntry>) {
    companion object { const val FORMAT = "momentum-backup"; const val CURRENT_VERSION = 1 }
}
sealed class BackupError : Exception() {
    data object NotABackup : BackupError()
    data class NewerVersion(val version: Int) : BackupError()
}
object Backup {
    fun export(data: PersonalData, now: Instant = Clock.System.now()): String {
        val file = BackupFile(BackupFile.FORMAT, BackupFile.CURRENT_VERSION, now, data.profile, data.sessions.sortedBy { it.date }, data.weights.sortedBy { it.date })
        return MomentumPrettyJson.encodeToString(MomentumJson.encodeToJsonElement(file).sortedKeys())
    }
    fun read(text: String): PersonalData {
        val obj = runCatching { MomentumJson.parseToJsonElement(text) as? JsonObject }.getOrNull() ?: throw BackupError.NotABackup
        if((obj["format"] as? JsonPrimitive)?.content != BackupFile.FORMAT) throw BackupError.NotABackup
        val versionElement = obj["version"]
        val version = if(versionElement == null) null else {
            val primitive = versionElement as? JsonPrimitive ?: throw BackupError.NotABackup
            if(primitive.isString) throw BackupError.NotABackup
            primitive.longOrNull ?: throw BackupError.NotABackup
        }
        if(version != null && version > BackupFile.CURRENT_VERSION) throw BackupError.NewerVersion(version.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
        val file = runCatching { MomentumJson.decodeFromJsonElement<BackupFile>(obj) }.getOrNull() ?: throw BackupError.NotABackup
        return PersonalData(file.profile, file.sessions.sortedBy { it.date }, file.weights.filter { it.kg.isFinite() && it.kg > 0 }.sortedBy { it.date })
    }
    fun csv(sessions: List<WorkoutSession>): String {
        val lines = mutableListOf("date,workout,exercise_id,exercise,set,reps,weight_kg,seconds,effort")
        for(session in sessions.sortedBy { it.date }) for(log in session.logs) {
            val name = ExerciseLibrary.byID[log.exerciseID]?.englishName ?: log.exerciseID
            log.sets.forEachIndexed { index, set ->
                val fields = listOf(Instant.fromEpochSeconds(session.date.epochSeconds).toString(), session.title, log.exerciseID, name,
                    (index + 1).toString(), set.reps.toString(), trimmed(set.weightKg), set.seconds.toString(), log.effort?.name ?: "")
                lines.add(fields.joinToString(",") { escape(it) })
            }
        }
        return lines.joinToString("\n") + "\n"
    }
    private fun trimmed(value: Double): String = if(value == value.roundHalfAwayFromZero() && value >= Long.MIN_VALUE && value <= Long.MAX_VALUE) value.toLong().toString() else value.toString()
    private fun escape(field: String): String {
        val text = if(field.firstOrNull() in listOf('=', '+', '-', '@')) "'$field" else field
        return if(text.any { it == ',' || it == '"' || it == '\n' }) "\"${text.replace("\"", "\"\"")}\"" else text
    }
}
