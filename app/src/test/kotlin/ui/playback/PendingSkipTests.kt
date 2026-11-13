package org.jellyfin.androidtv.ui.playback

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PendingSkipTests : FunSpec({
	test("PendingSkip.request() sets the target position") {
		val pendingSkip = PendingSkip()

		pendingSkip.request(10_000, 30_000, 600_000) shouldBe true

		pendingSkip.hasPending() shouldBe true
		pendingSkip.targetPositionMs shouldBe 40_000
		pendingSkip.offsetMs shouldBe 30_000
	}

	test("PendingSkip.request() accumulates the offset of the gesture") {
		val pendingSkip = PendingSkip()

		pendingSkip.request(10_000, 30_000, 600_000) shouldBe true
		pendingSkip.request(10_000, 30_000, 600_000) shouldBe false
		pendingSkip.request(10_000, -10_000, 600_000) shouldBe false

		pendingSkip.targetPositionMs shouldBe 60_000
		pendingSkip.offsetMs shouldBe 50_000
	}

	test("PendingSkip.request() does not jump past the end of the media") {
		val pendingSkip = PendingSkip()

		pendingSkip.request(595_000, 30_000, 600_000) shouldBe true

		pendingSkip.targetPositionMs shouldBe 599_000
	}

	test("PendingSkip keeps a pending jump to the beginning of the media") {
		val pendingSkip = PendingSkip()

		pendingSkip.request(5_000, -10_000, 600_000) shouldBe true

		pendingSkip.hasPending() shouldBe true
		pendingSkip.targetPositionMs shouldBe 0
	}

	test("PendingSkip.clear() resets the gesture") {
		val pendingSkip = PendingSkip()

		pendingSkip.request(10_000, 30_000, 600_000) shouldBe true
		pendingSkip.clear()

		pendingSkip.hasPending() shouldBe false
		pendingSkip.targetPositionMs shouldBe 0
		pendingSkip.offsetMs shouldBe 0

		pendingSkip.request(50_000, 30_000, 600_000) shouldBe true

		pendingSkip.targetPositionMs shouldBe 80_000
		pendingSkip.offsetMs shouldBe 30_000
	}

	test("PendingSkip.markApplied() keeps the gesture open for the next key press") {
		val pendingSkip = PendingSkip()

		pendingSkip.request(10_000, 30_000, 600_000) shouldBe true
		pendingSkip.markApplied()

		// The jump was made right away, so it must not be made a second time
		pendingSkip.hasPending() shouldBe false

		// The key press that follows is merged into the same gesture
		pendingSkip.request(10_000, 30_000, 600_000) shouldBe false
		pendingSkip.hasPending() shouldBe true
		pendingSkip.targetPositionMs shouldBe 70_000
		pendingSkip.offsetMs shouldBe 60_000
	}
})
