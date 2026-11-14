package com.tablettenchecker.app.data

import kotlinx.serialization.Serializable

/**
 * Eine Tablette mit Einnahmeplan und Bestand.
 *
 * Der Bestand wird nicht täglich umgeschrieben: [stock] gilt für den Zeitpunkt [stockAt], der aktuelle
 * Bestand wird daraus mit dem Einnahmeplan berechnet (siehe [StockCalculator]).
 */
@Serializable
data class Medication(
    val id: String,
    val name: String,
    /** Stärke oder Hinweis, z. B. „5 mg“. */
    val note: String = "",
    /** Stück pro Einnahme morgens (auch halbe, z. B. 0.5). */
    val morning: Double = 0.0,
    /** Stück pro Einnahme mittags. */
    val noon: Double = 0.0,
    /** Stück pro Einnahme abends. */
    val evening: Double = 0.0,
    /** Einnahme alle so viele Tage; 1 = täglich. */
    val everyDays: Int = 1,
    /** Ein Einnahmetag (Tage seit 1970), nur bei [everyDays] > 1 von Bedeutung. */
    val anchorDay: Long = 0L,
    /** Gezählter Bestand zum Zeitpunkt [stockAt]. */
    val stock: Double = 0.0,
    /** Zeitpunkt der Zählung in Millisekunden seit 1970. */
    val stockAt: Long = 0L,
    /** Stück pro Packung; 0 = nicht angegeben. */
    val packSize: Int = 0,
    /** Eigener Vorlauf für den Nachkauf in Tagen; null = Standard aus den Einstellungen. */
    val leadDays: Int? = null,
)

/** Eingaben der Seite „Tablette bearbeiten“. */
data class MedicationInput(
    val name: String,
    val note: String,
    val morning: Double,
    val noon: Double,
    val evening: Double,
    val everyDays: Int,
    /** Nächster Einnahmetag in so vielen Tagen (0 = heute), nur bei [everyDays] > 1. */
    val nextDoseInDays: Int,
    val stock: Double,
    val packSize: Int,
    val leadDays: Int?,
)

/** Einstellungen; liegen mit den Tabletten in derselben Datei und kommen so mit in die Sicherung. */
@Serializable
data class AppSettings(
    /** Uhrzeiten der Einnahme in Minuten nach Mitternacht. */
    val morningMinutes: Int = 8 * 60,
    val noonMinutes: Int = 12 * 60,
    val eveningMinutes: Int = 20 * 60,
    /** Mittags als dritte Einnahmezeit anbieten. */
    val showNoon: Boolean = false,
    /** Standard-Vorlauf: so viele Tage vor dem Aufbrauchen nachkaufen. */
    val leadDays: Int = 14,
    /** Einkaufsliste: Vorrat nach dem Einkauf in Tagen. */
    val supplyDays: Int = 90,
    /** Nachkauf-Hinweis als Benachrichtigung. */
    val notify: Boolean = true,
    /** Uhrzeit des täglichen Hinweises in Minuten nach Mitternacht. */
    val notifyMinutes: Int = 9 * 60,
)

/** Aufbau der gespeicherten Datei und der Sicherung; [version] erlaubt spätere Umbauten. */
@Serializable
data class MedicationFile(
    val version: Int = 1,
    val medications: List<Medication> = emptyList(),
    val settings: AppSettings = AppSettings(),
)
