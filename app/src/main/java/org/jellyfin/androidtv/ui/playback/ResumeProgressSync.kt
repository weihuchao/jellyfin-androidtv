package org.jellyfin.androidtv.ui.playback

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.auth.repository.SessionRepository
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.UpdateUserItemDataDto
import timber.log.Timber
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID

/**
 * Decides whether the resume point of the last watched item has to be restored on the server.
 */
object ResumeProgressDecision {
	/**
	 * Whether the resume point of the last watched item is missing on the server.
	 *
	 * The server only keeps items that have a resume point and orders them by the time they were
	 * played last. The item that was watched last is therefore missing when it is not part of the
	 * list and when it is newer than everything the server has.
	 *
	 * @param lastPlayback The item that was reported to the server last
	 * @param serverItemIds The ids of the items the server returned
	 * @param serverNewestTimestamp The time the newest item of the server was played, in UTC
	 */
	fun needsRestore(
		lastPlayback: LastPlayback?,
		serverItemIds: Set<UUID>,
		serverNewestTimestamp: LocalDateTime?,
	): Boolean {
		val playback = lastPlayback ?: return false
		if (playback.itemId in serverItemIds) return false

		val newest = serverNewestTimestamp ?: return true
		return playback.timestamp.atOffset(ZoneOffset.UTC).toLocalDateTime().isAfter(newest)
	}
}

/**
 * Restores the resume point of the last watched item when the server does not have it.
 *
 * The position is written directly to the user data of the item, because a normal progress report
 * is discarded by the server when it is below the minimum resume percentage.
 */
class ResumeProgressSync(
	private val api: ApiClient,
	private val store: LastPlaybackStore,
	private val sessionRepository: SessionRepository,
) {
	/**
	 * Whether the resume point of the last watched item is missing on the server.
	 *
	 * @param serverItems The items the server returned for the continue watching list
	 */
	fun needsRestore(serverItems: List<BaseItemDto>): Boolean {
		val lastPlayback = store.load() ?: return false
		val serverNewestTimestamp = serverItems
			.mapNotNull { it.userData?.lastPlayedDate }
			.maxOrNull()

		return ResumeProgressDecision.needsRestore(
			lastPlayback = lastPlayback,
			serverItemIds = serverItems.map { it.id }.toSet(),
			serverNewestTimestamp = serverNewestTimestamp,
		)
	}

	/**
	 * Writes the position of the last watched item back to the server.
	 *
	 * @return Whether the resume point was restored
	 */
	suspend fun restore(): Boolean {
		val lastPlayback = store.load() ?: return false
		val userId = sessionRepository.currentSession.value?.userId ?: return false

		return withContext(Dispatchers.IO) {
			runCatching {
				val item = api.userLibraryApi.getItem(lastPlayback.itemId).content
				if (item.userData?.played == true) {
					Timber.i("Resume sync: %s was watched on the server, keeping its state", item.name)
					return@runCatching false
				}

				api.itemsApi.updateItemUserData(
					lastPlayback.itemId,
					userId,
					UpdateUserItemDataDto(
						playbackPositionTicks = lastPlayback.positionMs * TICKS_PER_MILLISECOND,
						// The server orders the continue watching list by the time an item was played last,
						// so the restored item has to carry that time as well. The server treats that time as
						// the local time of the server and turns it into UTC, so the local time of the device
						// is sent instead of the time in UTC.
						lastPlayedDate = lastPlayedDate(lastPlayback.timestamp),
					),
				)

				Timber.i(
					"Resume sync: restored the resume point of %s at %d ms",
					item.name,
					lastPlayback.positionMs,
				)
				true
			}.getOrElse { error ->
				Timber.w(error, "Resume sync: failed to restore the resume point")
				false
			}
		}
	}

	companion object {
		private const val TICKS_PER_MILLISECOND = 10_000L

		/**
		 * The time the server has to be told about for the item that was played last.
		 *
		 * The server treats the time it is given as the local time of the server and turns it into UTC,
		 * the same way it does with the times it writes itself, so the local time of the device is sent
		 * instead of the time in UTC.
		 *
		 * @param timestamp The time the item was reported to the server
		 * @param zone The time zone of the device
		 */
		fun lastPlayedDate(timestamp: Instant, zone: ZoneId = ZoneId.systemDefault()): LocalDateTime =
			timestamp.atZone(zone).toLocalDateTime()
	}
}
