package org.jellyfin.androidtv.ui.playback

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

class ResumeProgressDecisionTests : FunSpec({
	val itemId = UUID.randomUUID()
	val otherItemId = UUID.randomUUID()
	val userId = UUID.randomUUID()
	val zone = ZoneId.of("Asia/Shanghai")
	val now = LocalDateTime.now(zone)

	test("ResumeProgressDecision does not restore an item the server already has") {
		val playback = LastPlayback(itemId, 600_000, now.atZone(zone).toInstant())

		ResumeProgressDecision.needsRestore(playback, setOf(itemId), now, userId, zone) shouldBe false
	}

	test("ResumeProgressDecision restores an item that is missing and newer than the server") {
		val playback = LastPlayback(itemId, 600_000, now.atZone(zone).toInstant())

		ResumeProgressDecision.needsRestore(playback, setOf(otherItemId), now.minusDays(1), userId, zone) shouldBe true
	}

	test("ResumeProgressDecision does not restore an item that is older than the server") {
		val playback = LastPlayback(itemId, 600_000, now.minusDays(2).atZone(zone).toInstant())

		ResumeProgressDecision.needsRestore(playback, setOf(otherItemId), now.minusDays(1), userId, zone) shouldBe false
	}

	test("ResumeProgressDecision restores when the server has no items at all") {
		val playback = LastPlayback(itemId, 600_000, now.atZone(zone).toInstant())

		ResumeProgressDecision.needsRestore(playback, emptySet(), null, userId, zone) shouldBe true
	}

	test("ResumeProgressDecision does not restore without a last playback") {
		ResumeProgressDecision.needsRestore(null, setOf(otherItemId), now, userId, zone) shouldBe false
	}

	test("ResumeProgressDecision restores an item that the server lost minutes ago") {
		// The times of the server arrive in the local time of the device. The time of the last
		// playback has to be turned into the local time as well, otherwise the two are compared with
		// a difference of the time zone offset and a recent item is never restored.
		val playback = LastPlayback(itemId, 600_000, now.atZone(zone).toInstant())
		val serverNewest = now.minusMinutes(5)

		ResumeProgressDecision.needsRestore(playback, setOf(otherItemId), serverNewest, userId, zone) shouldBe true
	}

	test("ResumeProgressDecision does not restore an item of another account") {
		val playback = LastPlayback(itemId, 600_000, now.atZone(zone).toInstant(), userId)

		ResumeProgressDecision.needsRestore(playback, setOf(otherItemId), now.minusDays(1), UUID.randomUUID(), zone) shouldBe false
	}

	test("ResumeProgressDecision does not restore a position of zero") {
		val playback = LastPlayback(itemId, 0, now.atZone(zone).toInstant(), userId)

		ResumeProgressDecision.needsRestore(playback, setOf(otherItemId), now.minusDays(1), userId, zone) shouldBe false
	}

	test("ResumeProgressSync sends the local time of the device because the server turns it into UTC") {
		// 2026-11-13 11:17:33 in Asia/Shanghai is 2026-11-13 03:17:33 in UTC
		val timestamp = Instant.parse("2026-11-13T03:17:33Z")

		ResumeProgressSync.lastPlayedDate(timestamp, ZoneId.of("Asia/Shanghai")) shouldBe
			LocalDateTime.of(2026, 11, 13, 11, 17, 33)
	}
})
