package org.jellyfin.androidtv.ui.playback

import org.jellyfin.androidtv.util.Utils

/**
 * Keeps track of the target position of a pending fast forward or rewind.
 *
 * The target is accumulated instead of being applied right away, so a request that arrives
 * while the player is still buffering is kept until the player is ready again instead of
 * being dropped.
 */
class PendingSkip {
	private var basePositionMs = 0L
	private var gestureOpen = false
	private var pending = false

	/**
	 * The absolute position the player should jump to.
	 */
	var targetPositionMs: Long = 0L
		private set

	/**
	 * The accumulated offset of the current gesture, positive when fast forwarding.
	 */
	var offsetMs: Long = 0L
		private set

	fun hasPending(): Boolean = pending

	/**
	 * Adds a fast forward or rewind request to the pending jump.
	 *
	 * @param basePositionMs The playback position the current gesture started at
	 * @param deltaMs The amount to skip, positive when fast forwarding
	 * @param durationMs The duration of the media
	 * @return True when the request starts a new gesture, so the jump can be made right away
	 */
	fun request(basePositionMs: Long, deltaMs: Long, durationMs: Long): Boolean {
		val startsGesture = !gestureOpen
		if (startsGesture) {
			this.basePositionMs = basePositionMs
			offsetMs = 0
			gestureOpen = true
		}

		offsetMs += deltaMs
		targetPositionMs = Utils.getSafeSeekPosition(this.basePositionMs + offsetMs, durationMs)
		pending = true

		return startsGesture
	}

	/**
	 * Marks the current target as applied to the player, so it is not jumped to a second time.
	 *
	 * The gesture stays open, the key presses that follow are merged into the same jump.
	 */
	fun markApplied() {
		pending = false
	}

	fun clear() {
		gestureOpen = false
		pending = false
		basePositionMs = 0
		targetPositionMs = 0
		offsetMs = 0
	}
}
