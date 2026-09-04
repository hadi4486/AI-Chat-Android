package com.aichat.assistant.utils

import androidx.annotation.StringRes
import com.aichat.assistant.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Drives the "صبح بخیر / عصر بخیر …" line at the top of Home. */
@StringRes
fun greetingStringRes(hourOfDay: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)): Int = when (hourOfDay) {
    in 5..11 -> R.string.home_greeting_morning
    in 12..16 -> R.string.home_greeting_noon
    in 17..20 -> R.string.home_greeting_afternoon
    else -> R.string.home_greeting_evening
}

/**
 * Short label for a conversation-list timestamp: clock time if it happened today, otherwise a
 * short date. Deliberately simple (Gregorian, device locale) — a Jalali calendar formatter would
 * be a natural follow-up for a Persian-first release, called out in README.md.
 */
fun Long.toShortTimeLabel(): String {
    val target = Calendar.getInstance().apply { timeInMillis = this@toShortTimeLabel }
    val today = Calendar.getInstance()
    val isSameDay = target.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
        target.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    val pattern = if (isSameDay) "HH:mm" else "yyyy/MM/dd"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(this))
}
