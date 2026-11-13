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

	test("PlaybackSessionProgress ignores a first position behind the start position") {
		val session = PlaybackSessionProgress()

		// The session starts at a resume point the server already knows
		session.begin(UUID.randomUUID(), 124_617)

		// The player reports the position it is still preparing, which is behind the start position
		session.observe(0) shouldBe false
		session.watched shouldBe 0L

		// The player reached the resume point, that position is not watched media either
		session.observe(124_617) shouldBe false
		session.watched shouldBe 0L

		// Only the media that was played during this session counts
		session.observe(125_739) shouldBe false
		session.watched shouldBe 1_122L
	}

	test("PlaybackSessionProgress counts the first position when the session starts at zero") {
		val session = PlaybackSessionProgress()

		session.begin(UUID.randomUUID(), 0)

		session.observe(3_000) shouldBe false
		session.watched shouldBe 3_000L

		session.observe(6_000) shouldBe true
		session.watched shouldBe 6_000L
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
