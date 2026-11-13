package com.momentum

import com.momentum.util.*
import com.momentum.i18n.*
import kotlin.test.*
import kotlin.time.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class UtilTest {
    @BeforeTest fun english() { Loc.language = AppLanguage.en }
    @Test fun rounding() {
        assertEquals(3.0, 2.5.roundHalfAwayFromZero())
        assertEquals(-3.0, (-2.5).roundHalfAwayFromZero())
        assertEquals(-1.0, (-0.5).roundHalfAwayFromZero())
        assertEquals(4.5e15, 4.5e15.roundHalfAwayFromZero())
        assertEquals(0.0, 0.49999999999999994.roundHalfAwayFromZero())
        assertEquals(2.0, 1.5.roundHalfAwayFromZero())
    }
    @Test fun formatting() {
        assertEquals("12.2", formatFixed(12.25, 1))
        assertEquals("12.3", formatFixed(12.35, 1))
        assertEquals("0", formatFixed(0.5, 0))
        assertEquals("2", formatFixed(1.5, 0))
        assertEquals("44.0", formatFixed(44.0, 1))
        assertEquals("-0.0", formatFixed(-0.04, 1))
        assertEquals("Up 2 kg", L("Up {0} kg", 2))
        assertEquals("12.5", Loc.number(12.5))
        assertEquals("0", Loc.number(-0.04))
        Loc.language = AppLanguage.es
        assertEquals("12,5", Loc.number(12.5))
        Loc.language = AppLanguage.en
    }
    @Test fun instant() {
        val seconds = Instant.parse("2026-01-05T09:00:00Z")
        assertEquals("\"2026-01-05T09:00:00Z\"", MomentumJson.encodeToString(IsoInstantSerializer, seconds))
        assertEquals(seconds, MomentumJson.decodeFromString(IsoInstantSerializer, "\"2026-01-05T09:00:00Z\""))
        assertEquals("\"2026-01-05T09:00:00Z\"", MomentumJson.encodeToString(IsoInstantSerializer, Instant.parse("2026-01-05T09:00:00.123Z")))
    }
}
