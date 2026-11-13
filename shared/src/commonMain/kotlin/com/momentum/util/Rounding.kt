package com.momentum.util

import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.truncate

fun Double.roundHalfAwayFromZero(): Double {
    val t = truncate(this)
    return if (abs(this - t) >= 0.5) t + sign(this) else t
}
