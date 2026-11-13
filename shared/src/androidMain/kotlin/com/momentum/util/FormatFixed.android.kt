package com.momentum.util

actual fun formatFixed(value: Double, digits: Int): String = when {
    value.isNaN() -> "nan"
    value == Double.POSITIVE_INFINITY -> "inf"
    value == Double.NEGATIVE_INFINITY -> "-inf"
    else -> {
        val text = java.math.BigDecimal(value).setScale(digits, java.math.RoundingMode.HALF_EVEN).toPlainString()
        if (value < 0 || (value == 0.0 && 1.0 / value < 0)) { if (text.startsWith("-")) text else "-$text" } else text
    }
}
