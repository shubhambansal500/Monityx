package com.bansalcoders.monityx.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Date formatting and calculation helpers.
 * Uses java.time (API 26+) – no deprecated Calendar or SimpleDateFormat.
 */
object DateUtils {

    private val shortFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    private val isoFormatter   = DateTimeFormatter.ISO_LOCAL_DATE

    /** Format a [LocalDate] for display (e.g. "Jan 15, 2025"). */
    fun format(date: LocalDate): String = date.format(shortFormatter)

    /** Parse an ISO date string ("yyyy-MM-dd") to [LocalDate]. */
    fun parseIso(dateString: String): LocalDate = LocalDate.parse(dateString, isoFormatter)

    /** Format a [LocalDate] to ISO string. */
    fun toIso(date: LocalDate): String = date.format(isoFormatter)

    /** Returns a human-readable "days until" string. */
    fun daysUntilText(target: LocalDate): String {
        val days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), target)
        return when {
            days < 0  -> "Overdue"
            days == 0L -> "Today"
            days == 1L -> "Tomorrow"
            days <= 7  -> "In $days days"
            else       -> format(target)
        }
    }

    /** Formats epoch day (Long) to display string. */
    fun epochDayToDisplay(epochDay: Long): String =
        format(LocalDate.ofEpochDay(epochDay))
}
