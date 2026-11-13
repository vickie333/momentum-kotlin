package com.momentum.model

import com.momentum.util.MomentumJson
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

@Serializable data class MotionProp(val t: String, val p: List<Double>)
@Serializable data class Motion(val view: String, val dur: Double, val held: String? = null, val bounds: List<Double>, val props: List<MotionProp>, val frames: List<List<Double>>) {
    val isFront get() = view == "front"
    val restingPose get() = frames.getOrNull(minOf(1, frames.lastIndex)) ?: emptyList()
    fun pose(at: Double): List<Double> {
        val count = frames.size
        if(count <= 1) return frames.firstOrNull() ?: emptyList()
        val position = maxOf(0.0, at) / maxOf(dur, 0.1)
        val index = floor(position).toInt()
        val t = position - index
        val eased = t * t * (3 - 2 * t)
        return blend(frames[index % count], frames[(index + 1) % count], eased)
    }
    companion object {
        fun blend(a: List<Double>, b: List<Double>, t: Double): List<Double> = a.mapIndexed { k, value ->
            if(k >= b.size) value else if(k < 2) value + (b[k] - value) * t else {
                var delta = (b[k] - value) % 360.0
                if(delta > 180) delta -= 360 else if(delta < -180) delta += 360
                value + delta * t
            }
        }
    }
}
object MotionLibrary {
    val all: Map<String, Motion> by lazy { runCatching { MomentumJson.decodeFromString<Map<String, Motion>>(GeneratedData.motionsJson) }.getOrDefault(emptyMap()) }
    fun motion(forExerciseID: String): Motion? = all[forExerciseID]
}
data class Pt(val x: Double, val y: Double)
object Body { const val torso = 0.50; const val neck = 0.16; const val upperArm = 0.29; const val foreArm = 0.27; const val thigh = 0.44; const val shin = 0.43; const val foot = 0.14; const val headRadius = 0.10; const val shoulderHalf = 0.17; const val hipHalf = 0.10 }
data class Joints(
    val hip: Pt, val neck: Pt, val head: Pt, val shoulderNear: Pt, val shoulderFar: Pt, val hipNear: Pt, val hipFar: Pt,
    val elbowNear: Pt, val wristNear: Pt, val elbowFar: Pt, val wristFar: Pt,
    val kneeNear: Pt, val ankleNear: Pt, val toeNear: Pt, val kneeFar: Pt, val ankleFar: Pt, val toeFar: Pt
)
object Skeleton {
    fun advance(p: Pt, degrees: Double, length: Double): Pt {
        val r = degrees * kotlin.math.PI / 180
        return Pt(p.x + sin(r) * length, p.y - cos(r) * length)
    }
    fun joints(f: List<Double>, front: Boolean): Joints {
        val hip = Pt(f[0], f[1]); val neck = advance(hip, f[2], Body.torso); val head = advance(neck, f[3], Body.neck)
        val shoulderNear: Pt; val shoulderFar: Pt; val hipNear: Pt; val hipFar: Pt
        if(front) {
            shoulderNear = Pt(neck.x + Body.shoulderHalf, neck.y); shoulderFar = Pt(neck.x - Body.shoulderHalf, neck.y)
            hipNear = Pt(hip.x + Body.hipHalf, hip.y); hipFar = Pt(hip.x - Body.hipHalf, hip.y)
        } else { shoulderNear = neck; shoulderFar = neck; hipNear = hip; hipFar = hip }
        val elbowNear = advance(shoulderNear, f[4], Body.upperArm); val wristNear = advance(elbowNear, f[5], Body.foreArm)
        val elbowFar = advance(shoulderFar, f[6], Body.upperArm); val wristFar = advance(elbowFar, f[7], Body.foreArm)
        val kneeNear = advance(hipNear, f[8], Body.thigh); val ankleNear = advance(kneeNear, f[9], Body.shin)
        val toeNear = advance(ankleNear, f[10], Body.foot); val kneeFar = advance(hipFar, f[11], Body.thigh)
        val ankleFar = advance(kneeFar, f[12], Body.shin); val toeFar = advance(ankleFar, f[13], Body.foot)
        return Joints(hip, neck, head, shoulderNear, shoulderFar, hipNear, hipFar, elbowNear, wristNear, elbowFar, wristFar, kneeNear, ankleNear, toeNear, kneeFar, ankleFar, toeFar)
    }
}
