package org.jellyfin.androidtv.util

import android.content.Context
import org.jellyfin.androidtv.R
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.util.Locale

@Suppress("DEPRECATION")
val Context.locale: Locale
	get() = when {
		AndroidVersion.isAtLeastN -> resources.configuration.getLocales().get(0)
		else -> resources.configuration.locale
	}

@JvmOverloads
fun Context.getDateFormatter(
	style: FormatStyle = FormatStyle.SHORT
): DateTimeFormatter = DateTimeFormatterBuilder()
	.append(DateTimeFormatter.ofLocalizedDate(style))
	.appendLiteral(' ')
	.appendPattern(getTimePattern())
	.toFormatter(locale)

/**
 * The time format used everywhere in the app, always 24-hour with zero padding.
 */
fun Context.getTimePattern(): String = getString(R.string.time_format_24h)

fun Context.getTimeFormatter(): DateTimeFormatter = DateTimeFormatter
	.ofPattern(getTimePattern())
	.withLocale(locale)
