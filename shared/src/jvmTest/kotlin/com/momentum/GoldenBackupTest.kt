package com.momentum

import com.momentum.model.*
import com.momentum.util.MomentumJson
import kotlin.test.*
import kotlinx.serialization.json.*
import kotlin.time.Instant

class GoldenBackupTest {
    private fun fixture(name: String) = requireNotNull(javaClass.getResource("/golden/backups/$name.json")) { name }.readText()
    @Test fun currentRoundTrips() {
        val source = fixture("current")
        val now = Instant.parse(MomentumJson.parseToJsonElement(source).jsonObject.getValue("exportedAt").jsonPrimitive.content)
        assertJsonEquivalent(MomentumJson.parseToJsonElement(source), MomentumJson.parseToJsonElement(Backup.export(Backup.read(source), now)))
    }
    @Test fun legacyLoads() {
        val restored = Backup.read(fixture("legacy"))
        assertEquals("Sam", restored.profile.name)
        assertEquals(TrainingHistory.sixTo24Months, restored.profile.history)
        assertEquals(RecentFrequency.threeToFour, restored.profile.frequency)
        assertEquals(UserProfile().healthSync, restored.profile.healthSync)
        assertEquals(restored, Backup.read(Backup.export(restored, Instant.parse("2026-01-06T09:00:00Z"))))
    }
    @Test fun futureRejected() {
        val source = fixture("future")
        val version = MomentumJson.parseToJsonElement(source).jsonObject.getValue("version").jsonPrimitive.int
        assertEquals(version, assertFailsWith<BackupError.NewerVersion> { Backup.read(source) }.version)
    }
}
