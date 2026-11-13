package org.jellyfin.androidtv.ui.playback.nextup

import org.jellyfin.androidtv.preference.constant.NEXTUP_TIMER_DISABLED
import org.jellyfin.androidtv.preference.constant.NEXTUP_TIMER_NO_WAIT

/**
 * What happens to the next item when the item that is playing ends.
 */
enum class NextItemAction {
	/** Show the still watching screen. */
	SHOW_STILL_WATCHING,

	/** Show the next up screen, with the countdown. */
	SHOW_NEXT_UP,

	/** Start the next item right away. */
	PLAY_NEXT,
}

/**
 * What the next up screen does with the configured timeout.
 */
enum class NextUpTimeoutAction {
	/** Start the next item right away. */
	PLAY_NOW,

	/** Count down and then start the next item. */
	COUNTDOWN,

	/** Wait until the user makes a choice. */
	WAIT_FOREVER,
}

/**
 * Decides what happens when an item ends and the next item is queued.
 */
object NextUpDecision {
	/**
	 * The action of the next up screen for the given timeout.
	 *
	 * @param timeoutMillis The configured timeout, in milliseconds
	 */
	fun timeoutAction(timeoutMillis: Int): NextUpTimeoutAction = when {
		timeoutMillis == NEXTUP_TIMER_DISABLED -> NextUpTimeoutAction.WAIT_FOREVER
		timeoutMillis <= NEXTUP_TIMER_NO_WAIT -> NextUpTimeoutAction.PLAY_NOW
		else -> NextUpTimeoutAction.COUNTDOWN
	}

	/**
	 * What to do when the item that is playing ends and a next item is queued.
	 *
	 * The still watching screen takes precedence, it is a different feature and asks the user
	 * whether the watching should continue. The next up screen is only shown when there is
	 * something to wait for.
	 *
	 * @param stillWatchingEnabled Whether the still watching screen is enabled
	 * @param showStillWatching Whether the still watching screen should be shown now
	 * @param nextUpEnabled Whether the next up screen is enabled
	 * @param nextUpTimeoutMillis The configured next up timeout, in milliseconds
	 */
	fun nextItemAction(
		stillWatchingEnabled: Boolean,
		showStillWatching: Boolean,
		nextUpEnabled: Boolean,
		nextUpTimeoutMillis: Int,
	): NextItemAction = when {
		stillWatchingEnabled && showStillWatching -> NextItemAction.SHOW_STILL_WATCHING
		nextUpEnabled && timeoutAction(nextUpTimeoutMillis) != NextUpTimeoutAction.PLAY_NOW ->
			NextItemAction.SHOW_NEXT_UP

		else -> NextItemAction.PLAY_NEXT
	}
}
