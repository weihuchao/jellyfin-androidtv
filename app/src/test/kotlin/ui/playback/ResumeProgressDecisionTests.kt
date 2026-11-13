package org.jellyfin.androidtv.ui.playback

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID

class ResumeProgressDecisionTests : FunSpec({
	val itemId = UUID.randomUUID()
	val otherItemId = UUID.randomUUID()
	val now = LocalDateTime.now(ZoneOffset.UTC)

	test("ResumeProgressDecision does not restore an item the server already has") {
		val playback = LastPlayback(itemId, 600_000, now.toInstant(ZoneOffset.UTC))

		ResumeProgressDecision.needsRestore(playback, setOf(itemId), now) shouldBe false
	}

	test("ResumeProgressDecision restores an item that is missing and newer than the server") {
		val playback = LastPlayback(itemId, 600_000, now.toInstant(ZoneOffset.UTC))

		ResumeProgressDecision.needsRestore(playback, setOf(otherItemId), now.minusDays(1)) shouldBe true
	}

	test("ResumeProgressDecision does not restore an item that is older than the server") {
		val playback = LastPlayback(itemId, 600_000, now.minusDays(2).toInstant(ZoneOffset.UTC))

		ResumeProgressDecision.needsRestore(playback, setOf(otherItemId), now.minusDays(1)) shouldBe false
	}

	test("ResumeProgressDecision restores when the server has no items at all") {
		val playback = LastPlayback(itemId, 600_000, now.toInstant(ZoneOffset.UTC))

		ResumeProgressDecision.needsRestore(playback, emptySet(), null) shouldBe true
	}

	test("ResumeProgressDecision does not restore without a last playback") {
		ResumeProgressDecision.needsRestore(null, setOf(otherItemId), now) shouldBe false
	}

	test("ResumeProgressSync sends the local time of the device because the server turns it into UTC") {
		// 2026-11-13 11:17:33 in Asia/Shanghai is 2026-11-13 03:17:33 in UTC
		val timestamp = Instant.parse("2026-11-13T03:17:33Z")

		ResumeProgressSync.lastPlayedDate(timestamp, ZoneId.of("Asia/Shanghai")) shouldBe
			LocalDateTime.of(2026, 11, 13, 11, 17, 33)
	}
})
