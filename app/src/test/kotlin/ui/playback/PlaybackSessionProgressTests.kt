package org.jellyfin.androidtv.ui.playback

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.UUID

class PlaybackSessionProgressTests : FunSpec({
	test("PlaybackSessionProgress does not count the position the session started at") {
		val session = PlaybackSessionProgress()
		val itemId = UUID.randomUUID()

		session.begin(itemId, 600_000)

		session.currentItemId shouldBe itemId
		session.startPosition shouldBe 600_000L
		session.watched shouldBe 0L
		session.isReportable shouldBe false

		session.observe(600_000) shouldBe false
		session.watched shouldBe 0L
	}

	test("PlaybackSessionProgress only counts forward movement") {
		val session = PlaybackSessionProgress()

		session.begin(UUID.randomUUID(), 60_000)

		session.observe(64_000) shouldBe false
		session.watched shouldBe 4_000L

		// A rewind does not add to the watched time
		session.observe(30_000) shouldBe false
		session.watched shouldBe 4_000L

		// Four seconds before the rewind and six seconds after it
		session.observe(36_000) shouldBe true
		session.watched shouldBe 10_000L
	}

	test("PlaybackSessionProgress is reportable after the session watched enough") {
		val session = PlaybackSessionProgress()

		session.begin(UUID.randomUUID(), 0)

		session.observe(3_000) shouldBe false
		session.isReportable shouldBe false

		session.observe(6_000) shouldBe true
		session.isReportable shouldBe true
	}

	test("PlaybackSessionProgress resets the counters when a new session starts") {
		val session = PlaybackSessionProgress()

		session.begin(UUID.randomUUID(), 0)
		session.observe(10_000) shouldBe true

		session.begin(UUID.randomUUID(), 0)

		session.watched shouldBe 0L
		session.isReportable shouldBe false
		session.observe(1_000) shouldBe false
	}

	test("PlaybackSessionProgress.end() forgets the session") {
		val session = PlaybackSessionProgress()

		session.begin(UUID.randomUUID(), 0)
		session.observe(10_000) shouldBe true

		session.end()

		session.currentItemId shouldBe null
		session.watched shouldBe 0L
		session.isReportable shouldBe false
	}
})
