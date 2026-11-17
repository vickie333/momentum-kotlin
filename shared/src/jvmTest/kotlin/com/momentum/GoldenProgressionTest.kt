package com.momentum

import com.momentum.engine.*
import com.momentum.i18n.AppLanguage
import com.momentum.i18n.Loc
import com.momentum.model.*
import com.momentum.util.MomentumJson
import kotlin.math.abs
import kotlin.test.*
import kotlinx.datetime.TimeZone
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*

class GoldenProgressionTest {
    private var oldLanguage = Loc.language
    private var oldZone = TrainingCalendar.timeZone
    @BeforeTest fun setUp() { oldLanguage = Loc.language; oldZone = TrainingCalendar.timeZone; Loc.language = AppLanguage.en; TrainingCalendar.timeZone = TimeZone.UTC }
    @AfterTest fun tearDown() { Loc.language = oldLanguage; TrainingCalendar.timeZone = oldZone }

    @Test fun matchesSwiftGoldenCases() {
        val source = requireNotNull(javaClass.getResource("/golden/progression.json")) { "progression.json" }.readText()
        val cases = MomentumJson.parseToJsonElement(source).jsonArray
        assertEquals(120, cases.size)
        for ((index, case) in cases.withIndex()) {
            val input = case.jsonObject.getValue("input").jsonObject
            val expected = case.jsonObject.getValue("expected").jsonObject
            val scenario = input.getValue("scenario").jsonPrimitive.content
            val id = input.getValue("exerciseID").jsonPrimitive.content
            val context = "case $index ($scenario, $id)"
            val profile = MomentumJson.decodeFromJsonElement<UserProfile>(input.getValue("profile"))
            val history = MomentumJson.decodeFromJsonElement<List<WorkoutSession>>(input.getValue("history"))
            val range = input.getValue("range").jsonObject
            val decision = Progression.decide(ExerciseLibrary.exercise(id), profile, history,
                RepRange(range.getValue("min").jsonPrimitive.int, range.getValue("max").jsonPrimitive.int),
                input.getValue("holdLoad").jsonPrimitive.boolean)
            assertEquals(expected.getValue("target").jsonPrimitive.int, decision.target, "$context target")
            assertEquals(expected.getValue("repMin").jsonPrimitive.int, decision.repMin, "$context repMin")
            assertEquals(expected.getValue("repMax").jsonPrimitive.int, decision.repMax, "$context repMax")
            assertEquals(expected.getValue("addSet").jsonPrimitive.boolean, decision.addSet, "$context addSet")
            val reason = expected["reason"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content
            assertEquals(reason, decision.reason, "$context reason")
            val weight = expected["weightKg"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.double
            if (weight == null) assertNull(decision.weightKg, "$context weightKg")
            else {
                val actualWeight = assertNotNull(decision.weightKg, "$context weightKg")
                assertTrue(abs(weight - actualWeight) <= 1e-9, "$context weightKg: $weight != ${decision.weightKg}")
            }
        }
    }
}
