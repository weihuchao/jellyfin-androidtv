package org.jellyfin.androidtv.ui.playback.nextup

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.androidtv.preference.constant.NEXTUP_TIMER_DISABLED
import org.jellyfin.androidtv.preference.constant.NEXTUP_TIMER_NO_WAIT

class NextUpDecisionTests : FunSpec({
	test("NextUpDecision waits for the user when the timer is disabled") {
		NextUpDecision.timeoutAction(NEXTUP_TIMER_DISABLED) shouldBe NextUpTimeoutAction.WAIT_FOREVER
	}

	test("NextUpDecision plays the next item right away when there is no wait") {
		NextUpDecision.timeoutAction(NEXTUP_TIMER_NO_WAIT) shouldBe NextUpTimeoutAction.PLAY_NOW
	}

	test("NextUpDecision counts down when a duration is configured") {
		NextUpDecision.timeoutAction(7000) shouldBe NextUpTimeoutAction.COUNTDOWN
	}

	test("NextUpDecision shows the still watching screen first") {
		NextUpDecision.nextItemAction(
			stillWatchingEnabled = true,
			showStillWatching = true,
			nextUpEnabled = true,
			nextUpTimeoutMillis = 7000,
		) shouldBe NextItemAction.SHOW_STILL_WATCHING
	}

	test("NextUpDecision shows the next up screen when there is a countdown") {
		NextUpDecision.nextItemAction(
			stillWatchingEnabled = false,
			showStillWatching = false,
			nextUpEnabled = true,
			nextUpTimeoutMillis = 7000,
		) shouldBe NextItemAction.SHOW_NEXT_UP
	}

	test("NextUpDecision shows the next up screen when the user has to wait forever") {
		NextUpDecision.nextItemAction(
			stillWatchingEnabled = false,
			showStillWatching = false,
			nextUpEnabled = true,
			nextUpTimeoutMillis = NEXTUP_TIMER_DISABLED,
		) shouldBe NextItemAction.SHOW_NEXT_UP
	}

	test("NextUpDecision plays the next item right away without a wait") {
		NextUpDecision.nextItemAction(
			stillWatchingEnabled = false,
			showStillWatching = false,
			nextUpEnabled = true,
			nextUpTimeoutMillis = NEXTUP_TIMER_NO_WAIT,
		) shouldBe NextItemAction.PLAY_NEXT
	}

	test("NextUpDecision plays the next item right away when the next up screen is disabled") {
		NextUpDecision.nextItemAction(
			stillWatchingEnabled = false,
			showStillWatching = false,
			nextUpEnabled = false,
			nextUpTimeoutMillis = 7000,
		) shouldBe NextItemAction.PLAY_NEXT
	}

	test("NextUpDecision plays the next item right away when the still watching screen is not shown now") {
		NextUpDecision.nextItemAction(
			stillWatchingEnabled = true,
			showStillWatching = false,
			nextUpEnabled = false,
			nextUpTimeoutMillis = 7000,
		) shouldBe NextItemAction.PLAY_NEXT
	}
})
