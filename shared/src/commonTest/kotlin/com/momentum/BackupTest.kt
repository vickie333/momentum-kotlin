package com.momentum

import com.momentum.i18n.*
import com.momentum.model.*
import com.momentum.util.MomentumJson
import kotlin.test.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*

class BackupTest {
    @BeforeTest fun english() { Loc.language = AppLanguage.en }
    private fun sampleData(): PersonalData = PersonalData(
        T.profile().copy(name = "Sam", language = AppLanguage.es, limitations = listOf(BodyArea.knees)),
        listOf(T.session(T.day(2), listOf(T.log("db_bench", listOf(10 to 20.0, 9 to 20.0), 8, 12))),
            T.session(T.day(0), listOf(T.log("pushup", listOf(12 to 0.0, 10 to 0.0), 8, 15)))),
        listOf(WeightEntry(date = T.day(1), kg = 81.5), WeightEntry(date = T.day(0), kg = 82.0)))
    @Test fun backupRoundTripsEverything() {
        val original = sampleData()
        val restored = Backup.read(Backup.export(original, T.day(5)))
        assertEquals("Sam", restored.profile.name)
        assertEquals(AppLanguage.es, restored.profile.language)
        assertEquals(listOf(BodyArea.knees), restored.profile.limitations)
        assertEquals(listOf(2,4,6), restored.profile.trainingWeekdays)
        assertEquals(2, restored.sessions.size)
        assertEquals(original.sessions.sortedBy { it.date }.map { it.id }, restored.sessions.map { it.id })
        assertEquals(listOf(SetLog(10,20.0,0), SetLog(9,20.0,0)), restored.sessions.last().logs.first().sets)
        assertEquals(listOf(82.0,81.5), restored.weights.map { it.kg })
    }
    @Test fun backupHasHeader() {
        val obj = MomentumJson.parseToJsonElement(Backup.export(sampleData(), T.day(5))).jsonObject
        assertEquals("momentum-backup", obj["format"]?.jsonPrimitive?.content)
        assertEquals(1, obj["version"]?.jsonPrimitive?.int)
        assertNotNull(obj["exportedAt"])
        assertEquals(obj.keys.sorted(), obj.keys.toList())
        val profile = obj.getValue("profile").jsonObject
        assertEquals(profile.keys.sorted(), profile.keys.toList())
    }
    @Test fun rejectsNonBackups() {
        for(text in listOf("hello", "{}", """{"format":"something-else","version":1}""", MomentumJson.encodeToString(sampleData())))
            assertFailsWith<BackupError.NotABackup> { Backup.read(text) }
    }
    @Test fun newerVersion() { assertEquals(99, assertFailsWith<BackupError.NewerVersion> { Backup.read("""{"format":"momentum-backup","version":99,"somethingNew":true}""") }.version) }
    @Test fun stringVersionRejected() { assertFailsWith<BackupError.NotABackup> { Backup.read("""{"format":"momentum-backup","version":"2"}""") } }
    @Test fun brokenBackupFailsWhole() { assertFailsWith<BackupError.NotABackup> { Backup.read(Backup.export(sampleData()).replace("\"sessions\"", "\"sessionz\"")) } }
    @Test fun corruptSessionFeedbackRejectsWholeBackup() {
        val obj = MomentumJson.parseToJsonElement(Backup.export(sampleData())).jsonObject
        val sessions = obj.getValue("sessions").jsonArray
        val first = sessions.first().jsonObject
        val corrupt = JsonObject(obj + ("sessions" to JsonArray(listOf(JsonObject(first + ("feedback" to JsonPrimitive("bogus")))) + sessions.drop(1))))
        assertFailsWith<BackupError.NotABackup> { Backup.read(corrupt.toString()) }
    }
    @Test fun nullLogIdRejectsWholeBackup() {
        val obj = MomentumJson.parseToJsonElement(Backup.export(sampleData())).jsonObject
        val sessions = obj.getValue("sessions").jsonArray
        val first = sessions.first().jsonObject
        val logs = first.getValue("logs").jsonArray
        val corruptLog = JsonObject(logs.first().jsonObject + ("id" to JsonNull))
        val corruptSession = JsonObject(first + ("logs" to JsonArray(listOf(corruptLog) + logs.drop(1))))
        val corrupt = JsonObject(obj + ("sessions" to JsonArray(listOf(corruptSession) + sessions.drop(1))))
        assertFailsWith<BackupError.NotABackup> { Backup.read(corrupt.toString()) }
    }
    @Test fun invalidExerciseLadderRejected() {
        val record = MomentumJson.encodeToJsonElement(ExerciseLibrary.records.first()).jsonObject
        val corrupt = JsonObject(record + ("ladder" to JsonPrimitive("bogus")))
        assertFails { MomentumJson.decodeFromJsonElement<ExerciseRecord>(corrupt) }
    }
    @Test fun impossibleWeightsDropped() {
        val data = sampleData().copy(weights = sampleData().weights + listOf(WeightEntry(date = T.day(3), kg = -5.0), WeightEntry(date = T.day(4), kg = 0.0)))
        assertEquals(2, Backup.read(Backup.export(data)).weights.size)
    }
    @Test fun ownSaveFileLoads() { assertEquals(2, MomentumJson.decodeFromString<PersonalData>(MomentumJson.encodeToString(sampleData())).sessions.size) }
    @Test fun csvRowsInDateOrder() {
        val rows = Backup.csv(sampleData().sessions).trimEnd().lines()
        assertEquals("date,workout,exercise_id,exercise,set,reps,weight_kg,seconds,effort", rows.first())
        assertEquals(5, rows.size)
        assertContains(rows[1], "pushup")
        assertContains(rows[3], "db_bench")
        assertContains(rows[3], ",1,10,20,0,")
    }
    @Test fun csvEscapes() {
        val session = T.session(T.day(0), listOf(T.log("pushup", listOf(10 to 0.0), 8, 15))).copy(title = "=HYPERLINK(\"x\"), \"quoted\"")
        assertContains(Backup.csv(listOf(session)).lines()[1], "\"'=HYPERLINK(\"\"x\"\"), \"\"quoted\"\"\"")
    }
    @Test fun csvEnglish() {
        Loc.language = AppLanguage.es
        assertContains(Backup.csv(sampleData().sessions), "Push-Up")
        Loc.language = AppLanguage.en
    }
}
