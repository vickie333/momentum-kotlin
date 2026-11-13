package com.momentum.util

import platform.Foundation.NSString
import platform.Foundation.stringWithFormat

actual fun formatFixed(value: Double, digits: Int): String = NSString.stringWithFormat("%.*f", digits, value)
