package com.momentum

import com.momentum.i18n.*
import com.momentum.model.*
import com.momentum.util.MomentumJson
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlin.test.*

class LocalizationTest {
    private val translated = listOf(AppLanguage.es, AppLanguage.pt)

    @BeforeTest fun before() { Loc.language = AppLanguage.en }
    @AfterTest fun after() { Loc.language = AppLanguage.en }

    @Test fun languageDetection() {
        assertEquals(AppLanguage.pt, AppLanguage.detect(listOf("pt-PT", "en")))
        assertEquals(AppLanguage.es, AppLanguage.detect(listOf("es-MX")))
        assertEquals(AppLanguage.es, AppLanguage.detect(listOf("fr-FR", "es")))
        assertEquals(AppLanguage.en, AppLanguage.detect(listOf("ja")))
        assertEquals(AppLanguage.en, AppLanguage.detect(emptyList()))
    }

    @Test fun placeholdersInAnyOrder() {
        assertEquals("b then a", L("{1} then {0}", "a", "b"))
        assertEquals("no placeholders", L("no placeholders"))
    }

    @Test fun unknownKeysFallBackToEnglish() {
        for (language in translated) {
            Loc.language = language
            assertEquals("This sentence is not in any table 3", L("This sentence is not in any table {0}", 3))
        }
    }

    @Test fun listJoinsInEveryLanguage() {
        for ((language, expected) in listOf(
            AppLanguage.en to "a, b and c", AppLanguage.es to "a, b y c", AppLanguage.pt to "a, b e c"
        )) {
            Loc.language = language
            assertEquals(expected, Loc.list(listOf("a", "b", "c")))
        }
    }

    @Test fun numbersUseDecimalSeparator() {
        for ((language, expected) in listOf(
            AppLanguage.en to "12.5", AppLanguage.es to "12,5", AppLanguage.pt to "12,5"
        )) {
            Loc.language = language
            assertEquals(expected, Loc.number(12.5))
        }
    }

    @Test fun profileLanguageChoiceIsSavedAndOptional() {
        assertNull(UserProfile().language)
        val profile = UserProfile(language = AppLanguage.pt)
        assertEquals(AppLanguage.pt, MomentumJson.decodeFromString<UserProfile>(MomentumJson.encodeToString(profile)).language)
        assertNull(MomentumJson.decodeFromString<UserProfile>("""{"name":"Sam"}""").language)
    }

    @Test fun tablesHaveSameKeysInEveryLanguage() {
        val es = Loc.translatedKeys(AppLanguage.es)
        val pt = Loc.translatedKeys(AppLanguage.pt)
        assertTrue(es.isNotEmpty())
        assertEquals(es, pt)
    }

    private fun placeholders(text: String): List<String> = Regex("\\{\\d+\\}").findAll(text).map { it.value }.sorted().toList()

    @Test fun translationsKeepPlaceholders() {
        for (language in translated) for (key in Loc.translatedKeys(language)) {
            val value = Loc.translate(key, language)
            assertEquals(placeholders(key), placeholders(value), "$language: $key")
            assertTrue(value.isNotBlank(), "$language: $key")
        }
    }

    @Test fun everyExerciseTranslatedWithSameNumberOfSteps() {
        for (language in translated) {
            assertEquals(ExerciseLibrary.all.map { it.id }.toSet(), ExerciseText.translatedIDs(language), "$language")
            for (exercise in ExerciseLibrary.all) {
                val entry = assertNotNull(ExerciseText.entry(exercise.id, language), "$language: ${exercise.id}")
                assertTrue(entry.name.isNotBlank(), exercise.id)
                assertEquals(exercise.englishSteps.size, entry.steps.size, "$language: ${exercise.id}")
                assertTrue(entry.steps.all { it.isNotBlank() }, "$language: ${exercise.id}")
            }
        }
    }

    @Test fun exerciseTextFollowsTheLanguage() {
        val pushup = ExerciseLibrary.exercise("pushup")
        assertEquals(pushup.englishName, pushup.name)
        for (language in translated) {
            Loc.language = language
            assertNotEquals(pushup.englishName, pushup.name)
            assertEquals(pushup.englishSteps.size, pushup.steps.size)
        }
    }

    private fun percentDecode(value: String): String {
        val bytes = mutableListOf<Byte>()
        var index = 0
        while (index < value.length) {
            if (value[index] == '%') {
                bytes += value.substring(index + 1, index + 3).toInt(16).toByte()
                index += 3
            } else {
                bytes += value[index].code.toByte()
                index++
            }
        }
        return bytes.toByteArray().decodeToString()
    }

    @Test fun videoSearchUsesTranslatedName() {
        Loc.language = AppLanguage.es
        val exercise = ExerciseLibrary.exercise("pushup")
        val query = percentDecode(exercise.videoURL.substringAfter("search_query="))
        assertContains(query, exercise.name)
        assertFalse("proper form" in query)
    }

    @Test fun enumTitlesAndSummariesAreTranslated() {
        val english = buildList {
            addAll(FitnessLevel.entries.flatMap { listOf(it.title, it.summary) })
            addAll(Goal.entries.flatMap { listOf(it.title, it.summary) })
            addAll(Equipment.entries.flatMap { listOf(it.title, it.summary) })
            addAll(Sex.entries.map { it.title })
            addAll(BodyArea.entries.map { it.title })
            addAll(TrainingHistory.entries.map { it.title })
            addAll(RecentFrequency.entries.map { it.title })
            addAll(UnitSystem.entries.map { it.title })
            addAll(MuscleGroup.entries.map { it.title })
            addAll(WorkoutFeedback.entries.map { it.title })
            addAll(Effort.entries.map { it.title })
            addAll(Readiness.entries.map { it.title })
            addAll(Ladder.entries.map { it.title })
        }
        for (language in translated) {
            val keys = Loc.translatedKeys(language)
            for (key in english) assertTrue(key in keys, "$language missing $key")
        }
    }

    @Test fun weekPhaseLabelIsTranslated() {
        val phase = WeekPhase(0, 4, false, 2)
        val english = phase.label
        Loc.language = AppLanguage.es
        assertFalse('{' in phase.label)
        assertNotEquals(english, phase.label)
    }
}
