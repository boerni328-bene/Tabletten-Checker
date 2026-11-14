package com.tablettenchecker.app.data

import com.tablettenchecker.app.data.StockCalculator.Status
import java.time.LocalDate

/** Entscheidet, ob ein Nachkauf-Hinweis kommt. Reine Rechnung, ohne Android. */
object ReorderCheck {

    /** Erinnerung, solange nichts Neues dazukommt. */
    const val REPEAT_DAYS = 3L

    /** [due]: Nachkauf-Termin heute oder vorbei; [along]: kommt beim gemeinsamen Einkauf gleich mit. */
    data class Result(val due: List<Medication>, val along: List<Medication>)

    fun due(rows: List<Pair<Medication, Status>>, today: LocalDate, windowDays: Int): Result? {
        val due = rows.filter { (_, status) -> status.reorderBy?.isAfter(today) == false }
            .map { it.first }
            .sortedBy { it.name.lowercase() }
        if (due.isEmpty()) return null
        val dueIds = due.map { it.id }.toSet()
        val along = Shopping.nextTrip(rows, today, windowDays)?.medications.orEmpty().filter { it.id !in dueIds }
        return Result(due, along)
    }

    /** Hinweis, wenn etwas neu fällig ist, sonst alle [REPEAT_DAYS] Tage. */
    fun shouldNotify(dueIds: Set<String>, today: LocalDate, lastDay: LocalDate?, lastIds: Set<String>): Boolean =
        dueIds.isNotEmpty() &&
            (lastDay == null || !lastIds.containsAll(dueIds) || !today.isBefore(lastDay.plusDays(REPEAT_DAYS)))
}
