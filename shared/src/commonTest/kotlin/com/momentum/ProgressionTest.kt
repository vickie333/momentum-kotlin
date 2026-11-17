package com.momentum

import com.momentum.engine.*
import com.momentum.i18n.AppLanguage
import com.momentum.i18n.Loc
import com.momentum.model.*
import kotlin.test.*
import kotlinx.datetime.TimeZone

class ProgressionTest {
    private var oldLanguage = Loc.language
    private var oldZone = TrainingCalendar.timeZone
    private val bench = "db_bench"
    @BeforeTest fun setUp() { oldLanguage = Loc.language; oldZone = TrainingCalendar.timeZone; Loc.language = AppLanguage.en; TrainingCalendar.timeZone = TimeZone.UTC }
    @AfterTest fun tearDown() { Loc.language = oldLanguage; TrainingCalendar.timeZone = oldZone }
    private fun profile(goal: Goal = Goal.buildMuscle, maxDumbbell: Double = 0.0) =
        T.profile(level = FitnessLevel.intermediate, goal = goal, equipment = Equipment.dumbbells, maxDumbbell = maxDumbbell)
    private fun decide(id: String, p: UserProfile, history: List<WorkoutSession>, hold: Boolean = false) =
        Progression.decide(ExerciseLibrary.exercise(id), p, history, T.range(id, p), hold)
    private fun hist(p: UserProfile, sets: List<Pair<Int, Double>>, effort: Effort? = null, targetSets: Int? = null): List<WorkoutSession> {
        val r = T.range(bench, p)
        return listOf(T.session(T.day(-3), listOf(T.log(bench, sets, r.min, r.max, effort, targetSets = targetSets))))
    }

    @Test fun repRangesAreSaneForEveryCombination() {
        for (e in ExerciseLibrary.all) for (goal in Goal.entries) for (level in FitnessLevel.entries) {
            val r = Progression.repRange(e, goal, level)
            assertTrue(r.min <= r.max, "${e.id} $goal $level")
            assertTrue(r.min >= 3, e.id)
            assertTrue(r.max <= 45, e.id)
        }
    }
    @Test fun strengthGoalUsesHeavyRangesOnlyOnBarbellMainLifts() {
        assertEquals(RepRange(3, 6), Progression.repRange(ExerciseLibrary.exercise("bench_press"), Goal.getStronger, FitnessLevel.advanced))
        assertEquals(RepRange(5, 8), Progression.repRange(ExerciseLibrary.exercise("db_lunge"), Goal.getStronger, FitnessLevel.advanced))
        assertEquals(RepRange(6, 10), Progression.repRange(ExerciseLibrary.exercise("bench_press"), Goal.buildMuscle, FitnessLevel.intermediate))
    }
    @Test fun restScalesWithRoleGoalAndAge() {
        val squat = ExerciseLibrary.exercise("back_squat")
        assertEquals(150, Progression.rest(squat, RepRange(3, 6), Goal.getStronger, 30))
        assertEquals(120, Progression.rest(squat, RepRange(6, 10), Goal.buildMuscle, 30))
        val curl = ExerciseLibrary.exercise("db_curl")
        assertEquals(60, Progression.rest(curl, RepRange(10, 15), Goal.buildMuscle, 30))
        assertTrue(Progression.rest(curl, RepRange(10, 15), Goal.loseFat, 30) < 60)
        assertTrue(Progression.rest(curl, RepRange(10, 15), Goal.buildMuscle, 60) > 60)
        assertEquals(40, Progression.rest(ExerciseLibrary.exercise("plank"), RepRange(20, 45), Goal.stayFit, 30))
    }
    @Test fun loadStepsAndMinimums() {
        val db = ExerciseLibrary.exercise("db_curl")
        assertEquals(1.0, Progression.step(db, 8.0))
        assertEquals(2.0, Progression.step(db, 20.0))
        assertEquals(2.5, Progression.step(db, 40.0))
        assertEquals(5.0, Progression.step(ExerciseLibrary.exercise("back_squat"), 100.0))
        assertEquals(2.5, Progression.step(ExerciseLibrary.exercise("bench_press"), 60.0))
        assertEquals(20.0, Progression.roundLoad(8.0, ExerciseLibrary.exercise("bench_press")))
        assertEquals(50.0, Progression.roundLoad(51.0, ExerciseLibrary.exercise("bench_press")))
        assertEquals(55.0, Progression.roundLoad(59.0, ExerciseLibrary.exercise("back_squat"), down = true))
    }
    @Test fun initialLoadDependsOnBodyAndPerson() {
        val male = T.profile(level = FitnessLevel.intermediate, sex = Sex.male, weight = 80.0)
        val female = male.copy(sex = Sex.female)
        val goblet = ExerciseLibrary.exercise("goblet_squat")
        val b = ExerciseLibrary.exercise(bench)
        assertTrue(Progression.initialLoad(goblet, male)!! > 0)
        assertTrue(Progression.initialLoad(b, female)!! < Progression.initialLoad(b, male)!!)
        assertTrue(Progression.initialLoad(b, male.copy(age = 70))!! < Progression.initialLoad(b, male)!!)
        assertTrue(Progression.initialLoad(b, male.copy(level = FitnessLevel.beginner))!! < Progression.initialLoad(b, male)!!)
    }
    @Test fun initialLoadRespectsTheHeaviestDumbbell() {
        val p = T.profile(level = FitnessLevel.advanced, equipment = Equipment.dumbbells, maxDumbbell = 12.0, weight = 100.0)
        assertTrue(Progression.initialLoad(ExerciseLibrary.exercise("goblet_squat"), p)!! <= 12)
    }
    @Test fun bodyweightExercisesHaveNoLoad() { assertNull(Progression.initialLoad(ExerciseLibrary.exercise("pushup"), T.profile())) }
    @Test fun estimatedOneRepMax() {
        assertEquals(100 * (1 + 5.0 / 30), Progression.estimatedOneRepMax(100.0, 5)!!, 0.001)
        assertNull(Progression.estimatedOneRepMax(100.0, 25)); assertNull(Progression.estimatedOneRepMax(0.0, 5))
    }
    @Test fun firstTimeStartsLightInTheMiddleOfTheRange() {
        val p = profile(); val d = decide(bench, p, emptyList()); val r = T.range(bench, p)
        assertEquals((r.min + r.max) / 2, d.target); assertNotNull(d.weightKg); assertTrue(d.reason!!.contains("First time"))
    }
    @Test fun addsOneStepWhenEverySetHitsTheTop() {
        val p = profile(); val r = T.range(bench, p)
        val d = decide(bench, p, hist(p, List(3) { r.max to 20.0 }))
        assertEquals(22.0, d.weightKg); assertEquals(r.min, d.target); assertTrue(d.reason!!.startsWith("Up "))
    }
    @Test fun addsTwoStepsWhenTheTopWasBeatenByThree() {
        val p = profile(); val r = T.range(bench, p)
        assertEquals(24.0, decide(bench, p, hist(p, List(3) { (r.max + 3) to 20.0 })).weightKg)
    }
    @Test fun holdsWhenTheTopWasHitButItFeltHard() {
        val p = profile(); val r = T.range(bench, p)
        val d = decide(bench, p, hist(p, List(3) { r.max to 20.0 }, Effort.hard))
        assertEquals(20.0, d.weightKg); assertTrue(d.reason!!.contains("hard"))
    }
    @Test fun easyAndWithinOneRepCountsAsReachingTheTop() {
        val p = profile(); val r = T.range(bench, p)
        val sets = listOf((r.max - 1) to 20.0, r.max to 20.0, (r.max - 1) to 20.0)
        assertEquals(22.0, decide(bench, p, hist(p, sets, Effort.easy)).weightKg)
        assertEquals(20.0, decide(bench, p, hist(p, sets, Effort.good)).weightKg)
    }
    @Test fun withinRangeHoldsTheWeightAndAsksForOneMoreRep() {
        val p = profile(); val r = T.range(bench, p)
        val d = decide(bench, p, hist(p, listOf((r.min + 1) to 20.0, (r.min + 2) to 20.0, (r.min + 1) to 20.0)))
        assertEquals(20.0, d.weightKg); assertEquals(r.min + 2, d.target)
    }
    @Test fun oneShortSetRepeatsTheWeight() {
        val p = profile(); val r = T.range(bench, p)
        val d = decide(bench, p, hist(p, listOf((r.min + 1) to 20.0, (r.min + 1) to 20.0, (r.min - 1) to 20.0)))
        assertEquals(20.0, d.weightKg); assertEquals(r.min, d.target)
    }
    @Test fun mostSetsBelowTheMinimumRepeatsOnceThenDropsTenPercent() {
        val p = profile(); val r = T.range(bench, p)
        val miss = T.log(bench, listOf((r.min - 1) to 30.0, (r.min - 1) to 30.0, (r.min - 2) to 30.0), r.min, r.max)
        assertEquals(30.0, decide(bench, p, listOf(T.session(T.day(-3), listOf(miss)))).weightKg)
        val twice = decide(bench, p, listOf(T.session(T.day(-6), listOf(miss)), T.session(T.day(-3), listOf(miss))))
        val twiceWeight = assertNotNull(twice.weightKg)
        assertTrue(twiceWeight < 30); assertTrue(twiceWeight >= 26)
    }
    @Test fun badlyOverestimatedLoadDropsImmediately() {
        val p = profile(); val r = T.range(bench, p)
        val awful = T.log(bench, listOf((r.min - 5) to 30.0, (r.min - 5) to 30.0, (r.min - 6) to 30.0), r.min, r.max)
        val d = decide(bench, p, listOf(T.session(T.day(-3), listOf(awful))))
        assertTrue(d.weightKg!! < 30); assertTrue(d.reason!!.contains("much heavier"))
    }
    @Test fun skippedSetsDoNotTriggerProgression() {
        val p = profile(); val r = T.range(bench, p)
        assertEquals(20.0, decide(bench, p, hist(p, listOf(r.max to 20.0), targetSets = 4)).weightKg)
    }
    @Test fun dumbbellCapStopsAddingWeightAndExtendsReps() {
        val p = profile(maxDumbbell = 24.0); val r = T.range(bench, p)
        val d = decide(bench, p, hist(p, List(3) { r.max to 24.0 }))
        assertEquals(24.0, d.weightKg); assertTrue(d.repMax > r.max); assertTrue(d.repMax <= 20)
        assertTrue(d.reason!!.contains("heaviest dumbbell"))
    }
    @Test fun stepOverTheCapIsClampedToTheCap() {
        val p = profile(maxDumbbell = 21.0); val r = T.range(bench, p)
        assertTrue(decide(bench, p, hist(p, List(3) { r.max to 20.0 })).weightKg!! <= 21)
    }
    @Test fun lowReadinessNeverIncreasesTheWeight() {
        val p = profile(); val r = T.range(bench, p)
        val d = decide(bench, p, hist(p, List(3) { r.max to 20.0 }), true)
        assertEquals(20.0, d.weightKg); assertTrue(d.reason!!.contains("not feeling great"))
    }
    @Test fun usesTheMostRecentSession() {
        val p = profile(); val r = T.range(bench, p)
        val old = T.session(T.day(-30), listOf(T.log(bench, List(3) { r.max to 10.0 }, r.min, r.max)))
        val recent = T.session(T.day(-3), listOf(T.log(bench, List(3) { (r.min + 1) to 20.0 }, r.min, r.max)))
        assertEquals(20.0, decide(bench, p, listOf(old, recent)).weightKg)
    }
    @Test fun bodyweightAddsARepUntilTheTop() {
        val p = T.profile(level = FitnessLevel.beginner, goal = Goal.stayFit, equipment = Equipment.bodyweight)
        val r = T.range("knee_pushup", p)
        val h = listOf(T.session(T.day(-3), listOf(T.log("knee_pushup", listOf((r.min + 1) to 0.0, (r.min + 2) to 0.0), r.min, r.max))))
        val d = decide("knee_pushup", p, h)
        assertNull(d.weightKg); assertEquals(r.min + 2, d.target)
    }
    @Test fun bodyweightAtTheTopExtendsTheRangeWhenNothingHarderExists() {
        val p = T.profile(level = FitnessLevel.beginner, goal = Goal.stayFit, equipment = Equipment.bodyweight)
        val r = T.range("knee_pushup", p)
        val h = listOf(T.session(T.day(-3), listOf(T.log("knee_pushup", List(2) { r.max to 0.0 }, r.min, r.max))))
        val d = decide("knee_pushup", p, h)
        assertTrue(d.repMax > r.max); assertTrue(d.target > r.max)
    }
    @Test fun bodyweightCeilingAddsASet() {
        val p = T.profile(level = FitnessLevel.beginner, goal = Goal.stayFit, equipment = Equipment.bodyweight)
        val r = T.range("knee_pushup", p)
        val h = listOf(T.session(T.day(-3), listOf(T.log("knee_pushup", List(2) { 25 to 0.0 }, r.min, 25))))
        assertTrue(decide("knee_pushup", p, h).addSet)
    }
    @Test fun holdsProgressInSeconds() {
        val p = T.profile(level = FitnessLevel.intermediate, goal = Goal.stayFit, equipment = Equipment.bodyweight)
        val r = T.range("plank", p)
        val h = listOf(T.session(T.day(-3), listOf(T.log("plank", listOf((r.min + 5) to 0.0, (r.min + 10) to 0.0), r.min, r.max, seconds = true))))
        assertEquals(r.min + 10, decide("plank", p, h).target)
    }
    @Test fun comebackFactors() {
        assertEquals(1.0, Comeback.factor(3)); assertEquals(0.95, Comeback.factor(12))
        assertEquals(0.85, Comeback.factor(25)); assertEquals(0.75, Comeback.factor(40))
        assertEquals(0.65, Comeback.factor(90)); assertEquals(0, Comeback.setsToRemove(13))
        assertEquals(1, Comeback.setsToRemove(14))
    }
}
