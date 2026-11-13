package com.tablettenchecker.app.ui

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.floor
import kotlin.math.roundToLong

private val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
private val DATE_SHORT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.")
private val DATE_SHORT_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yy")
private val TIME = Regex("""^(\d{1,2})(?:[:.](\d{2}))?$""")

/** Stückzahl ohne überflüssige Nachkommastellen, mit Komma: 3, 0,5, 1,25. */
fun formatAmount(value: Double): String {
    val rounded = (value * 100).roundToLong() / 100.0
    return if (rounded == floor(rounded)) rounded.toLong().toString() else rounded.toString().replace('.', ',')
}

/** Liest eine Stückzahl; Komma oder Punkt. Leer zählt als 0, Unlesbares und Negatives ergibt null. */
fun parseAmount(text: String): Double? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return 0.0
    val value = trimmed.replace(',', '.').toDoubleOrNull() ?: return null
    return if (value.isFinite() && value >= 0.0 && value <= 100000.0) value else null
}

/** Ganze Zahl zwischen [min] und [max]; sonst null (auch bei leerem Feld). */
fun parseWhole(text: String, min: Int, max: Int): Int? = text.trim().toIntOrNull()?.takeIf { it in min..max }

/** Uhrzeit als „08:00“. */
fun formatTime(minutes: Int): String = String.format(Locale.ROOT, "%02d:%02d", minutes / 60, minutes % 60)

/** Liest „8“, „8:30“, „08.30“ als Minuten nach Mitternacht; sonst null. */
fun parseTime(text: String): Int? {
    val match = TIME.matchEntire(text.trim()) ?: return null
    val hours = match.groupValues[1].toInt()
    val minutes = match.groupValues[2].ifEmpty { "0" }.toInt()
    return if (hours in 0..23 && minutes in 0..59) hours * 60 + minutes else null
}

fun formatDate(date: LocalDate): String = date.format(DATE)

/** Kurzes Datum für die kompakte Ansicht: 04.12. (mit Jahr, wenn es nicht das laufende ist: 04.01.27). */
fun formatDateShort(date: LocalDate, today: LocalDate = LocalDate.now()): String =
    date.format(if (date.year == today.year) DATE_SHORT else DATE_SHORT_YEAR)

fun localDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
