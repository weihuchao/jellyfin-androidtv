package org.jellyfin.androidtv.util

import android.app.Application
import android.provider.Settings
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.widget.TextClock
import org.jellyfin.androidtv.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalDateTime
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class TimeFormatTests {
	private val context get() = RuntimeEnvironment.getApplication()

	@Test
	fun timePatternIs24Hour() {
		assertEquals("HH:mm", context.getString(R.string.time_format_24h))
		assertEquals("HH:mm", context.getTimePattern())
	}

	@Test
	fun timeFormatterRenders24HourWithZeroPadding() {
		val formatter = context.getTimeFormatter()

		assertEquals("00:00", formatter.format(LocalDateTime.of(2026, 11, 12, 0, 0)))
		assertEquals("09:05", formatter.format(LocalDateTime.of(2026, 11, 12, 9, 5)))
		assertEquals("16:42", formatter.format(LocalDateTime.of(2026, 11, 12, 16, 42)))
		assertEquals("23:59", formatter.format(LocalDateTime.of(2026, 11, 12, 23, 59)))
	}

	@Test
	fun timeFormatterIgnoresSystem12HourSetting() {
		val formatter = context.getTimeFormatter()
		val dateTime = LocalDateTime.of(2026, 11, 12, 16, 42)

		for (setting in listOf("12", "24", null)) {
			Settings.System.putString(context.contentResolver, Settings.System.TIME_12_24, setting)

			assertEquals("16:42", formatter.format(dateTime))
		}
	}

	@Test
	@Config(qualifiers = "zh-rCN")
	fun dateFormatterRenders24HourTime() {
		val formatted = context.getDateFormatter().format(LocalDateTime.of(2026, 11, 12, 16, 42))

		assertEquals("2026/11/12 16:42", formatted)
	}

	@Test
	fun xmlTextClocksUse24HourPattern() {
		val inflater = LayoutInflater.from(context)

		val clock = inflater.inflate(R.layout.clock_user_bug, null).findViewById<TextClock>(R.id.clock)
		assertEquals("HH:mm", clock.format12Hour.toString())
		assertEquals("HH:mm", clock.format24Hour.toString())

		val guideClock = inflater.inflate(R.layout.overlay_tv_guide, null).findViewById<TextClock>(R.id.textClock2)
		assertEquals("HH:mm", guideClock.format12Hour.toString())
		assertEquals("HH:mm", guideClock.format24Hour.toString())
	}

	@Test
	fun androidTimeFormatRenders24HourWithZeroPadding() {
		val formatted = DateFormat.format(context.getTimePattern(), Date()).toString()

		assertTrue(formatted.matches(Regex("\\d{2}:\\d{2}")))
		assertTrue(formatted.substring(0, 2).toInt() in 0..23)
	}

	@Test
	fun durationFormatIsUnchanged() {
		assertEquals("21:43", TimeUtils.formatMillis(21 * 60 * 1000L + 43 * 1000L))
	}
}
