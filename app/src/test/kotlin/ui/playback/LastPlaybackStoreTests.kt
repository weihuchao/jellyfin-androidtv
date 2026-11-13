package org.jellyfin.androidtv.ui.playback

import android.app.Application
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class LastPlaybackStoreTests {
	@Test
	fun storeKeepsTheLastPlayback() {
		val store = LastPlaybackStore(RuntimeEnvironment.getApplication())
		val itemId = UUID.randomUUID()
		val userId = UUID.randomUUID()
		val timestamp = Instant.now().truncatedTo(ChronoUnit.MILLIS)

		store.save(LastPlayback(itemId, 600_000, timestamp, userId))

		val playback = store.load()
		playback?.itemId shouldBe itemId
		playback?.positionMs shouldBe 600_000L
		playback?.timestamp shouldBe timestamp
		playback?.userId shouldBe userId
	}

	@Test
	fun storeReturnsNothingWithoutAPlayback() {
		val store = LastPlaybackStore(RuntimeEnvironment.getApplication())

		store.load() shouldBe null
	}

	@Test
	fun storeClearsTheLastPlayback() {
		val store = LastPlaybackStore(RuntimeEnvironment.getApplication())

		store.save(LastPlayback(UUID.randomUUID(), 600_000, Instant.now()))
		store.clear()

		store.load() shouldBe null
	}
}
