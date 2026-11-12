package org.jellyfin.androidtv.ui.playback.overlay

import android.app.Application
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class SkipIndicatorViewTests {
	@Test
	fun showDoesNotCrash() {
		val context = RuntimeEnvironment.getApplication()
		val indicator = SkipIndicatorView(context)

		indicator.show(60_000, true)
		indicator.show(-30_000, false)
	}
}
