package org.jellyfin.androidtv.ui.playback

import java.util.UUID

/**
 * Tracks how much of the current item was actually watched during this playback session.
 *
 * The server stores a resume point as soon as an item is reported with a position and refreshes
 * the "last played" time of that item at the same time. Opening an item and leaving again right
 * away would therefore move that item to the top of the continue watching list even though the user
 * did not watch it, so progress is only reported once the session actually moved forward.
 */
class PlaybackSessionProgress {
	companion object {
		/**
		 * The amount of playback a session needs before it counts as watched.
		 */
		const val MIN_WATCHED_MS = 5_000L
	}

	private var itemId: UUID? = null
	private var startPositionMs = 0L
	private var lastPositionMs = 0L
	private var watchedMs = 0L
	private var reportable = false

	/**
	 * The item that is played in this session.
	 */
	val currentItemId: UUID?
		get() = itemId

	/**
	 * The position the session started at.
	 */
	val startPosition: Long
		get() = startPositionMs

	/**
	 * The amount of media that was played during this session, in milliseconds.
	 */
	val watched: Long
		get() = watchedMs

	/**
	 * Whether the session watched enough to be reported to the server.
	 */
	val isReportable: Boolean
		get() = reportable

	/**
	 * Starts a new session for the given item.
	 *
	 * @param itemId The item that is played
	 * @param startPositionMs The position the playback starts at
	 */
	fun begin(itemId: UUID, startPositionMs: Long) {
		this.itemId = itemId
		this.startPositionMs = startPositionMs
		this.lastPositionMs = startPositionMs
		this.watchedMs = 0
		this.reportable = false
	}

	/**
	 * Records a new playback position of this session.
	 *
	 * Only forward movement counts as watched, a position that was inherited from the server when
	 * the session started is not part of it.
	 *
	 * @param positionMs The current playback position
	 * @return Whether the session watched enough to be reported to the server
	 */
	fun observe(positionMs: Long): Boolean {
		if (positionMs > lastPositionMs) watchedMs += positionMs - lastPositionMs
		lastPositionMs = positionMs
		if (!reportable && watchedMs >= MIN_WATCHED_MS) reportable = true
		return reportable
	}

	/**
	 * Ends this session.
	 */
	fun end() {
		itemId = null
		startPositionMs = 0
		lastPositionMs = 0
		watchedMs = 0
		reportable = false
	}
}
