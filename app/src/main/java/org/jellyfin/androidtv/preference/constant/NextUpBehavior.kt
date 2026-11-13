package org.jellyfin.androidtv.preference.constant

import org.jellyfin.androidtv.R
import org.jellyfin.preference.PreferenceEnum

enum class NextUpBehavior(
	override val nameRes: Int,
) : PreferenceEnum {
	/**
	 * Enable the Next Up screen - show the item's thumbnail
	 */
	EXTENDED(R.string.lbl_next_up_extended),

	/**
	 * Enable the Next Up screen - hide the item's thumbnail
	 */
	MINIMAL(R.string.lbl_next_up_minimal),

	/**
	 * Disable the Next Up screen
	 */
	DISABLED(R.string.lbl_never),
}

/**
 * Start the next item right away, the next up screen is not shown at all.
 */
const val NEXTUP_TIMER_NO_WAIT: Int = 0

/**
 * The next up timer is disabled, the next up screen waits for the user to make a choice.
 */
const val NEXTUP_TIMER_DISABLED: Int = -1
