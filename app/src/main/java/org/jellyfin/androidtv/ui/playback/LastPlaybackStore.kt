package org.jellyfin.androidtv.ui.playback

import android.content.Context
import androidx.core.content.edit
import java.time.Instant
import java.util.UUID

/**
 * The item that was reported to the server last.
 *
 * @param itemId The id of the item
 * @param positionMs The position that was reported, in milliseconds
 * @param timestamp The time of the report
 * @param userId The account the item was reported for
 */
data class LastPlayback(
	val itemId: UUID,
	val positionMs: Long,
	val timestamp: Instant,
	val userId: UUID? = null,
)

/**
 * Remembers the item that was reported to the server last.
 *
 * The server keeps the continue watching list on its own, but the resume point of an item can be
 * lost, for example when the device is powered off while the item is playing. This store keeps the
 * last report around so it can be restored on the next start.
 */
class LastPlaybackStore(context: Context) {
	private val sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)

	/**
	 * Stores the given playback as the last one that was reported.
	 */
	fun save(playback: LastPlayback) {
		sharedPreferences.edit {
			putString(KEY_ITEM_ID, playback.itemId.toString())
			putLong(KEY_POSITION_MS, playback.positionMs)
			putLong(KEY_TIMESTAMP, playback.timestamp.toEpochMilli())
			putString(KEY_USER_ID, playback.userId?.toString())
		}
	}

	/**
	 * The last playback that was reported, or null when nothing was reported yet.
	 */
	fun load(): LastPlayback? {
		val itemId = sharedPreferences.getString(KEY_ITEM_ID, null)
			?.let { runCatching { UUID.fromString(it) }.getOrNull() }
			?: return null
		val timestamp = sharedPreferences.getLong(KEY_TIMESTAMP, 0L)
		if (timestamp <= 0L) return null

		return LastPlayback(
			itemId = itemId,
			positionMs = sharedPreferences.getLong(KEY_POSITION_MS, 0L),
			timestamp = Instant.ofEpochMilli(timestamp),
			userId = sharedPreferences.getString(KEY_USER_ID, null)
				?.let { runCatching { UUID.fromString(it) }.getOrNull() },
		)
	}

	/**
	 * Forgets the last playback that was reported.
	 */
	fun clear() {
		sharedPreferences.edit { clear() }
	}

	companion object {
		private const val SHARED_PREFERENCES_NAME = "lastplayback"
		private const val KEY_ITEM_ID = "item_id"
		private const val KEY_POSITION_MS = "position_ms"
		private const val KEY_TIMESTAMP = "timestamp"
		private const val KEY_USER_ID = "user_id"
	}
}
