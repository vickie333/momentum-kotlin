package com.momentum.model

import com.momentum.i18n.AppLanguage
import com.momentum.i18n.L
import com.momentum.i18n.Loc
import com.momentum.util.IsoInstant
import com.momentum.util.roundHalfAwayFromZero
import kotlin.time.Clock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.KeepGeneratedSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

@Serializable enum class FitnessLevel {
    beginner, intermediate, advanced;
    val id get() = name
    val title get() = when(this) { beginner -> L("Beginner"); intermediate -> L("Intermediate"); advanced -> L("Advanced") }
    val summary get() = when(this) { beginner -> L("New to training or coming back after a long break."); intermediate -> L("Training regularly and comfortable with the basics."); advanced -> L("Years of consistent training. Ready for volume and load.") }
    val rank get() = ordinal
    val weightFactor get() = when(this) { beginner -> 0.6; intermediate -> 1.0; advanced -> 1.4 }
    companion object { fun from(rank: Int): FitnessLevel = entries[rank.coerceIn(0, entries.lastIndex)] }
}
@Serializable enum class Goal {
    buildMuscle, getStronger, loseFat, stayFit;
    val id get() = name
    val title get() = when(this) { buildMuscle -> L("Build muscle"); getStronger -> L("Get stronger"); loseFat -> L("Lose fat"); stayFit -> L("General fitness") }
    val summary get() = when(this) { buildMuscle -> L("More muscle size. Moderate reps, plenty of weekly volume."); getStronger -> L("Lift heavier over time. Fewer reps, longer rests."); loseFat -> L("Higher reps, shorter rests and a cardio finisher."); stayFit -> L("A balanced routine to feel strong and energetic.") }
    val symbol get() = when(this) { buildMuscle -> "dumbbell.fill"; getStronger -> "bolt.fill"; loseFat -> "flame.fill"; stayFit -> "heart.fill" }
    val volumeFactor get() = when(this) { buildMuscle -> 1.0; getStronger, loseFat -> 0.85; stayFit -> 0.7 }
}
@Serializable enum class Equipment {
    bodyweight, dumbbells, fullGym;
    val id get() = name
    val rank get() = ordinal
    val title get() = when(this) { bodyweight -> L("No equipment"); dumbbells -> L("Dumbbells"); fullGym -> L("Full gym") }
    val summary get() = when(this) { bodyweight -> L("Just you, a wall and a sturdy chair."); dumbbells -> L("A pair (or set) of dumbbells at home."); fullGym -> L("Barbells, machines, cables and a pull-up bar.") }
    val symbol get() = when(this) { bodyweight -> "figure.walk"; dumbbells -> "dumbbell"; fullGym -> "figure.strengthtraining.traditional" }
}
@Serializable enum class Sex { female, male, unspecified;
    val id get() = name
    val title get() = when(this) { female -> L("Female"); male -> L("Male"); unspecified -> L("Prefer not to say") }
}
@Serializable enum class BodyArea { knees, lowerBack, shoulders, wrists;
    val id get() = name
    val title get() = when(this) { knees -> L("Knees"); lowerBack -> L("Lower back"); shoulders -> L("Shoulders"); wrists -> L("Wrists") }
    val symbol get() = when(this) { knees -> "figure.walk"; lowerBack -> "figure.stand"; shoulders -> "figure.arms.open"; wrists -> "hand.raised.fill" }
}
@Serializable(with = TrainingHistory.Serializer::class)
enum class TrainingHistory { never, under6Months, sixTo24Months, over2Years;
    val raw get() = ordinal
    val id get() = raw
    val title get() = when(this) { never -> L("Never, or just starting"); under6Months -> L("Less than 6 months"); sixTo24Months -> L("6 months to 2 years"); over2Years -> L("More than 2 years") }
    object Serializer : KSerializer<TrainingHistory> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("TrainingHistory", PrimitiveKind.INT)
        override fun serialize(encoder: Encoder, value: TrainingHistory) = encoder.encodeInt(value.raw)
        override fun deserialize(decoder: Decoder): TrainingHistory = entries.getOrNull(decoder.decodeInt()) ?: throw SerializationException("Unknown TrainingHistory")
    }
}
@Serializable(with = RecentFrequency.Serializer::class)
enum class RecentFrequency { none, oneToTwo, threeToFour, fivePlus;
    val raw get() = ordinal
    val id get() = raw
    val title get() = when(this) { none -> L("None lately"); oneToTwo -> L("1-2 times a week"); threeToFour -> L("3-4 times a week"); fivePlus -> L("5+ times a week") }
    object Serializer : KSerializer<RecentFrequency> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("RecentFrequency", PrimitiveKind.INT)
        override fun serialize(encoder: Encoder, value: RecentFrequency) = encoder.encodeInt(value.raw)
        override fun deserialize(decoder: Decoder): RecentFrequency = entries.getOrNull(decoder.decodeInt()) ?: throw SerializationException("Unknown RecentFrequency")
    }
}
@Serializable data class FitnessCheck(val pushups: Int? = null, val squats: Int? = null, val plankSeconds: Int? = null, val pullups: Int? = null)
@Serializable enum class UnitSystem { metric, imperial;
    val id get() = name
    val title get() = if(this == metric) L("Metric (kg, cm)") else L("Imperial (lb, in)")
    val weightLabel get() = if(this == metric) "kg" else "lb"
    val heightLabel get() = if(this == metric) "cm" else "in"
    fun displayWeight(kg: Double) = if(this == metric) kg else kg * 2.2046226218
    fun kg(fromDisplay: Double) = if(this == metric) fromDisplay else fromDisplay / 2.2046226218
    fun displayHeight(cm: Double) = if(this == metric) cm else cm / 2.54
    fun cm(fromDisplay: Double) = if(this == metric) fromDisplay else fromDisplay * 2.54
    val weightStep get() = if(this == metric) 0.5 else 1.0
    fun formatWeight(kg: Double) = "${Loc.number((displayWeight(kg) * 2).roundHalfAwayFromZero() / 2)} $weightLabel"
}
@KeepGeneratedSerializer
@Serializable(with = UserProfileSerializer::class)
data class UserProfile(
    val name: String = "", val level: FitnessLevel = FitnessLevel.beginner, val goal: Goal = Goal.stayFit,
    val equipment: Equipment = Equipment.bodyweight, val sessionMinutes: Int = 30, val weightKg: Double = 70.0,
    val heightCm: Double = 170.0, val age: Int = 30, val sex: Sex = Sex.unspecified, val units: UnitSystem = UnitSystem.metric,
    val startDate: IsoInstant = Clock.System.now(), val onboarded: Boolean = false, val healthSync: Boolean = false,
    val reminderMinutes: Int? = null, val language: AppLanguage? = null, val history: TrainingHistory = TrainingHistory.never,
    val frequency: RecentFrequency = RecentFrequency.none, val check: FitnessCheck = FitnessCheck(),
    val trainingWeekdays: List<Int> = emptyList(), val limitations: List<BodyArea> = emptyList(),
    val lowImpactOnly: Boolean = false, val hasPullUpBar: Boolean = false, val maxDumbbellKg: Double = 0.0,
    val excludedExercises: List<String> = emptyList(), val swapPreferences: Map<String, String> = emptyMap(),
    val intensity: Int = 0, val rungs: Map<String, Int> = emptyMap(), val blockStart: IsoInstant = Clock.System.now(),
    val blockOffset: Int = 0, val deloadWeekStart: IsoInstant? = null, val levelChangedAt: IsoInstant? = null,
    val rungChangedAt: Map<String, IsoInstant> = emptyMap()
) { val daysPerWeek get() = trainingWeekdays.ifEmpty { listOf(1,2,3) }.size }

object UserProfileSerializer : KSerializer<UserProfile> {
    private val generated get() = UserProfile.generatedSerializer()
    override val descriptor get() = generated.descriptor
    override fun serialize(encoder: Encoder, value: UserProfile) = generated.serialize(encoder, value)
    override fun deserialize(decoder: Decoder): UserProfile {
        val jsonDecoder = decoder as? JsonDecoder ?: throw SerializationException("UserProfile requires JSON")
        val obj = jsonDecoder.decodeJsonElement() as? JsonObject ?: throw SerializationException("UserProfile must be an object")
        val json = jsonDecoder.json
        val valid = obj.filter { (key, value) ->
            runCatching { json.decodeFromJsonElement(generated, JsonObject(mapOf(key to value))) }.isSuccess
        }
        return json.decodeFromJsonElement(generated, JsonObject(valid))
    }
}
