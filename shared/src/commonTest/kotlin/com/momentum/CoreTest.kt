package com.momentum

import com.momentum.i18n.*
import com.momentum.model.*
import com.momentum.util.MomentumJson
import kotlin.test.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlin.math.abs

class CoreTest {
    @BeforeTest fun english() { Loc.language = AppLanguage.en }
    @Test fun everyExerciseHasMotion() {
        assertEquals(75, ExerciseLibrary.all.size)
        assertTrue(MotionLibrary.all.isNotEmpty())
        for(exercise in ExerciseLibrary.all) {
            val motion = assertNotNull(MotionLibrary.motion(exercise.id), exercise.id)
            assertEquals(4, motion.bounds.size, exercise.id)
            assertTrue(motion.frames.size >= 2, exercise.id)
            assertTrue(motion.frames.all { it.size == 14 }, exercise.id)
            assertTrue(motion.dur > 0.2, exercise.id)
            assertEquals(14, motion.pose(1.234).size, exercise.id)
        }
        assertNull(MotionLibrary.motion("does_not_exist"))
    }
    @Test fun motionBlendAndLoop() {
        val a = MutableList(14) { 0.0 }; val b = a.toMutableList(); a[2] = 170.0; b[2] = -170.0
        assertTrue(abs(abs(Motion.blend(a,b,0.5)[2]) - 180.0) < 0.001)
        val motion = assertNotNull(MotionLibrary.motion("pushup"))
        motion.pose(0.0).zip(motion.pose(motion.dur * motion.frames.size)).forEach { (x,y) -> assertTrue(abs(x-y) < 0.001) }
    }
    @Test fun feetAndBounds() {
        val squat = assertNotNull(MotionLibrary.motion("bw_squat"))
        squat.frames.forEach { assertTrue(abs(Skeleton.joints(it, squat.isFront).ankleNear.y - 0.05) < 0.02) }
        for((id,motion) in MotionLibrary.all) for(i in motion.frames.indices) for(step in 0..10) {
            val pose = Motion.blend(motion.frames[i], motion.frames[(i+1)%motion.frames.size], step / 10.0)
            val j = Skeleton.joints(pose, motion.isFront)
            val ys = listOf(j.ankleNear.y, j.ankleFar.y, j.toeNear.y, j.toeFar.y, j.wristNear.y, j.head.y - Body.headRadius)
            assertTrue(ys.min() + 0.001 >= motion.bounds[2], "$id frame $i step $step")
        }
    }
    @Test fun videoUrls() {
        assertTrue(ExerciseLibrary.curatedVideos.isEmpty())
        for(exercise in ExerciseLibrary.all) {
            val url = exercise.videoURL
            assertTrue(url.startsWith("https://www.youtube.com/results?search_query="), exercise.id)
            val encoded = url.substringAfter("search_query=")
            val bytes = mutableListOf<Byte>()
            var i = 0
            while(i < encoded.length) {
                if(encoded[i] == '%') { bytes.add(encoded.substring(i + 1, i + 3).toInt(16).toByte()); i += 3 }
                else { bytes.add(encoded[i].code.toByte()); i++ }
            }
            assertEquals("${exercise.name} proper form tutorial", bytes.toByteArray().decodeToString(), exercise.id)
        }
    }
    @Test fun profileOldAndUnknown() {
        val old = MomentumJson.decodeFromString<UserProfile>("""{"name":"Sam","level":"intermediate","onboarded":true,"sessionMinutes":40}""")
        assertEquals("Sam", old.name); assertEquals(FitnessLevel.intermediate, old.level); assertEquals(40, old.sessionMinutes)
        assertTrue(old.onboarded); assertFalse(old.healthSync); assertTrue(old.limitations.isEmpty()); assertTrue(old.rungs.isEmpty())
        val unknown = MomentumJson.decodeFromString<UserProfile>("""{"name":"Sam","goal":"someFutureGoal","level":"advanced"}""")
        assertEquals(FitnessLevel.advanced, unknown.level); assertEquals(Goal.stayFit, unknown.goal)
        val badInt = MomentumJson.decodeFromString<UserProfile>("""{"history":7,"level":"advanced"}""")
        assertEquals(TrainingHistory.never, badInt.history); assertEquals(FitnessLevel.advanced, badInt.level)
        val unknownLanguage = MomentumJson.decodeFromString<UserProfile>("""{"language":"someFutureLanguage"}""")
        assertNull(unknownLanguage.language)
    }
    @Test fun profileRoundTripAndOldLog() {
        val profile = UserProfile().copy(startDate = T.day(0), blockStart = T.day(0), healthSync = true, goal = Goal.getStronger, weightKg = 82.5,
            limitations = listOf(BodyArea.knees, BodyArea.wrists), trainingWeekdays = listOf(2,4,6),
            rungs = mapOf("push" to 2), swapPreferences = mapOf("bench_press" to "db_bench"),
            check = FitnessCheck(12,null,40,1))
        assertEquals(profile, MomentumJson.decodeFromString<UserProfile>(MomentumJson.encodeToString(profile)))
        val log = MomentumJson.decodeFromString<ExerciseLog>("""{"exerciseID":"pushup","targetSets":3,"target":10,"sets":[{"reps":10,"weightKg":0,"seconds":0}],"id":"6F1C8D3E-1234-4B6A-9C2F-0A1B2C3D4E5F"}""")
        assertNull(log.repMin); assertNull(log.effort); assertEquals("6F1C8D3E-1234-4B6A-9C2F-0A1B2C3D4E5F", log.id)
    }
    @Test fun unitsAndWorkout() {
        val pounds = UnitSystem.imperial.displayWeight(82.5)
        assertTrue(abs(UnitSystem.imperial.kg(pounds) - 82.5) < 0.0001)
        assertEquals("20 kg", UnitSystem.metric.formatWeight(20.0)); assertEquals("12.5 kg", UnitSystem.metric.formatWeight(12.5))
        assertEquals("44 lb", UnitSystem.imperial.formatWeight(20.0))
        val p = PlannedExercise("db_bench", 3, 10, 6, 10, 90, 20.0)
        val w = Workout("w", "t", "s", MuscleGroup.chest, exercises = listOf(p,p))
        assertTrue(w.minutes > 5); assertTrue(w.calories(80.0) > 20); assertEquals("3 × 6-10", p.targetLabel)
    }
    @Test fun laddersDeterministic() {
        for((ladder,rungs) in ExerciseMetaTable.ladders) {
            assertEquals(rungs.keys.maxOrNull(), ExerciseMetaTable.maxRung(ladder))
            rungs.values.forEach { assertEquals(it.sorted(), it) }
        }
    }
}
