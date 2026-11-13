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
    /** Stück pro Einnahme abends. */
    val evening: Double = 0.0,
    /** Gezählter Bestand zum Zeitpunkt [stockAt]. */
    val stock: Double = 0.0,
    /** Zeitpunkt der Zählung in Millisekunden seit 1970. */
    val stockAt: Long = 0L,
    /** Stück pro Packung; 0 = nicht angegeben. */
    val packSize: Int = 0,
)

/** Eingaben der Seite „Tablette bearbeiten“. */
data class MedicationInput(
    val name: String,
    val note: String,
    val morning: Double,
    val evening: Double,
    val stock: Double,
    val packSize: Int,
)

/** Aufbau der gespeicherten Datei; [version] erlaubt spätere Umbauten. */
@Serializable
data class MedicationFile(
    val version: Int = 1,
    val medications: List<Medication> = emptyList(),
)
