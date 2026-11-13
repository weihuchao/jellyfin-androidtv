package org.jellyfin.androidtv.preference

import android.app.Application
import androidx.preference.PreferenceManager
import io.kotest.matchers.shouldBe
import org.jellyfin.androidtv.preference.constant.NEXTUP_TIMER_DISABLED
import org.jellyfin.androidtv.preference.constant.NEXTUP_TIMER_NO_WAIT
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class UserPreferencesMigrationTests {
	@Test
	fun theStoredWaitForeverTimeoutIsMigratedToTheNewValue() {
		val preferences = defaultPreferences()
		preferences.edit()
			.putInt("store_version", 9)
			.putInt("next_up_timeout", 0)
			.apply()

		UserPreferences(RuntimeEnvironment.getApplication())

		preferences.getInt("next_up_timeout", NEXTUP_TIMER_NO_WAIT) shouldBe NEXTUP_TIMER_DISABLED
	}

	@Test
	fun aStoredCountdownIsNotChanged() {
		val preferences = defaultPreferences()
		preferences.edit()
			.putInt("store_version", 9)
			.putInt("next_up_timeout", 7000)
			.apply()

		UserPreferences(RuntimeEnvironment.getApplication())

		preferences.getInt("next_up_timeout", NEXTUP_TIMER_NO_WAIT) shouldBe 7000
	}

	@Test
	fun withoutAStoredTimeoutTheNewDefaultIsUsed() {
		defaultPreferences().edit()
			.putInt("store_version", 9)
			.apply()

		val userPreferences = UserPreferences(RuntimeEnvironment.getApplication())

		userPreferences[UserPreferences.nextUpTimeout] shouldBe NEXTUP_TIMER_NO_WAIT
	}

	private fun defaultPreferences() = PreferenceManager
		.getDefaultSharedPreferences(RuntimeEnvironment.getApplication())
		.apply { edit().clear().apply() }
}
