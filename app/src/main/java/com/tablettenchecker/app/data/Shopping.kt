package com.tablettenchecker.app.data

import com.tablettenchecker.app.data.StockCalculator.Level
import com.tablettenchecker.app.data.StockCalculator.Status
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.max

/** Einkaufsliste und gemeinsamer Nachkauf-Termin. Reine Rechnung, ohne Android. */
object Shopping {

    private const val EPS = 1e-6

    /** Bis [date] einkaufen und dabei gleich [medications] mitnehmen. */
    data class Trip(val date: LocalDate, val medications: List<Medication>)

    data class Item(
        val medication: Medication,
        val status: Status,
        /** Fehlende Stück bis zum gewünschten Vorrat. */
        val needed: Double,
        /** Nötige Packungen; null ohne Packungsgröße. */
        val packs: Int?,
        /** Bis zu diesem Tag kaufen (nie vor heute). */
        val dueBy: LocalDate,
    )

    /**
     * Nächster Einkauf: der früheste Nachkauf-Termin. Alles, was innerhalb von [windowDays] danach ebenfalls
     * fällig wird, kommt gleich mit – das spart Wege.
     */
    fun nextTrip(rows: List<Pair<Medication, Status>>, today: LocalDate, windowDays: Int): Trip? {
        val dated = rows.mapNotNull { (med, status) -> status.reorderBy?.let { med to maxOf(it, today) } }
        if (dated.isEmpty()) return null
        val first = dated.minOf { it.second }
        val limit = first.plusDays(max(0, windowDays).toLong())
        val together = dated.filter { !it.second.isAfter(limit) }
            .sortedWith(compareBy({ it.second }, { it.first.name.lowercase() }))
            .map { it.first }
        return Trip(first, together)
    }

    /** Alle Tabletten, von denen für [supplyDays] Tage Vorrat etwas fehlt, die dringendste zuerst. */
    fun items(rows: List<Pair<Medication, Status>>, supplyDays: Int, today: LocalDate): List<Item> =
        rows.filter { it.second.level != Level.NO_PLAN }
            .map { (med, status) ->
                val needed = max(0.0, ceil(StockCalculator.dailyAverage(med) * supplyDays - status.current - EPS))
                val packs = if (med.packSize > 0) ceil(needed / med.packSize - EPS).toInt() else null
                val dueBy = maxOf(status.reorderBy ?: today.plusYears(10), today)
                Item(med, status, needed, packs, dueBy)
            }
            .filter { it.needed > 0.0 }
            .sortedWith(compareBy({ it.dueBy }, { it.medication.name.lowercase() }))

    /** Menge, die nach dem Kauf zum Bestand kommt. */
    fun purchaseAmount(item: Item): Double =
        item.packs?.let { it.toDouble() * item.medication.packSize } ?: item.needed
}
