package com.tablettenchecker.app.data

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToLong

/** Rechnet Bestand, Reichweite und Nachkauf aus dem Einnahmeplan aus. Reine Rechnung, ohne Android. */
object StockCalculator {

    /** Ab dieser Uhrzeit gilt die Einnahme morgens als genommen. */
    val MORNING: LocalTime = LocalTime.of(8, 0)

    /** Ab dieser Uhrzeit gilt die Einnahme abends als genommen. */
    val EVENING: LocalTime = LocalTime.of(20, 0)

    /** So viele Tage vor dem Aufbrauchen soll nachgekauft sein. */
    const val LEAD_DAYS = 14L

    private const val MAX_DAYS = 3660
    private const val EPS = 1e-6

    enum class Level { OK, REORDER, EMPTY, NO_PLAN }

    data class Status(
        /** Bestand jetzt. */
        val current: Double,
        /** Tage bis zu dem Tag, an dem zum ersten Mal etwas fehlt (0 = heute fehlt schon etwas). */
        val daysLeft: Int?,
        /** Tag, an dem zum ersten Mal eine Einnahme nicht mehr vollständig möglich ist. */
        val emptyOn: LocalDate?,
        /** Bis zu diesem Tag sollte nachgekauft sein. */
        val reorderBy: LocalDate?,
        val level: Level,
        /** So viele Tage reicht eine Packung; null ohne Packungsgröße oder Einnahme. */
        val packDays: Int?,
    )

    /** Bestand jetzt: gezählter Bestand minus alle Einnahmen seit der Zählung. */
    fun currentStock(med: Medication, nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Double {
        val from = toLocal(med.stockAt, zone)
        val to = toLocal(nowMillis, zone)
        val used = doses(from, to, MORNING) * med.morning + doses(from, to, EVENING) * med.evening
        return round2(max(0.0, med.stock - used))
    }

    fun status(med: Medication, nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Status {
        val current = currentStock(med, nowMillis, zone)
        val daily = med.morning + med.evening
        if (daily <= 0.0) {
            return Status(current, null, null, null, Level.NO_PLAN, null)
        }
        val packDays = if (med.packSize > 0) floor(med.packSize / daily + EPS).toInt() else null

        val now = toLocal(nowMillis, zone)
        val today = now.toLocalDate()
        val plan = listOf(MORNING to med.morning, EVENING to med.evening).filter { it.second > 0.0 }
        var remaining = current
        var emptyOn: LocalDate? = null
        search@ for (offset in 0..MAX_DAYS) {
            val day = today.plusDays(offset.toLong())
            for ((time, dose) in plan) {
                if (!day.atTime(time).isAfter(now)) continue
                if (remaining + EPS < dose) {
                    emptyOn = day
                    break@search
                }
                remaining -= dose
            }
        }
        if (emptyOn == null) {
            return Status(current, null, null, null, Level.OK, packDays)
        }

        val daysLeft = ChronoUnit.DAYS.between(today, emptyOn).toInt()
        val smallestDose = plan.minOf { it.second }
        val level = when {
            daysLeft <= 0 || current + EPS < smallestDose -> Level.EMPTY
            daysLeft <= LEAD_DAYS -> Level.REORDER
            else -> Level.OK
        }
        return Status(current, daysLeft, emptyOn, emptyOn.minusDays(LEAD_DAYS), level, packDays)
    }

    /** Zahl der Einnahmen zur Uhrzeit [time] nach [from] und bis einschließlich [to]. */
    internal fun doses(from: LocalDateTime, to: LocalDateTime, time: LocalTime): Long {
        if (!to.isAfter(from)) return 0
        val first = if (from.toLocalTime() < time) from.toLocalDate() else from.toLocalDate().plusDays(1)
        val last = if (to.toLocalTime() >= time) to.toLocalDate() else to.toLocalDate().minusDays(1)
        return max(0L, ChronoUnit.DAYS.between(first, last) + 1)
    }

    private fun toLocal(millis: Long, zone: ZoneId): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), zone)

    private fun round2(value: Double): Double = (value * 100).roundToLong() / 100.0
}
