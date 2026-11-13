package com.tablettenchecker.app.ui

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.floor
import kotlin.math.roundToLong

private val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
private val DATE_SHORT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.")
private val DATE_SHORT_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yy")

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

fun formatDate(date: LocalDate): String = date.format(DATE)

/** Kurzes Datum für die kompakte Ansicht: 04.12. (mit Jahr, wenn es nicht das laufende ist: 04.01.27). */
fun formatDateShort(date: LocalDate, today: LocalDate = LocalDate.now()): String =
    date.format(if (date.year == today.year) DATE_SHORT else DATE_SHORT_YEAR)
