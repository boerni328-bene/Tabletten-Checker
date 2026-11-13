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

    /** Uhrzeiten der Einnahme und Standard-Vorlauf (aus den Einstellungen). */
    data class Plan(
        val morning: LocalTime = LocalTime.of(8, 0),
        val noon: LocalTime = LocalTime.of(12, 0),
        val evening: LocalTime = LocalTime.of(20, 0),
        val leadDays: Int = 14,
    ) {
        companion object {
            fun of(settings: AppSettings) = Plan(
                morning = time(settings.morningMinutes),
                noon = time(settings.noonMinutes),
                evening = time(settings.eveningMinutes),
                leadDays = settings.leadDays,
            )

            private fun time(minutes: Int): LocalTime =
                LocalTime.of((minutes / 60).coerceIn(0, 23), (minutes % 60).coerceIn(0, 59))
        }
    }

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
        /** Vorlauf, der für diese Tablette gilt. */
        val leadDays: Int,
    )

    /** Durchschnittlicher Verbrauch pro Tag (bei „alle 2 Tage“ die Hälfte einer Tagesmenge). */
    fun dailyAverage(med: Medication): Double =
        (med.morning + med.noon + med.evening) / max(1, med.everyDays)

    fun isDoseDay(med: Medication, day: LocalDate): Boolean =
        med.everyDays <= 1 || Math.floorMod(day.toEpochDay() - med.anchorDay, med.everyDays.toLong()) == 0L

    /** Bestand jetzt: gezählter Bestand minus alle Einnahmen seit der Zählung. */
    fun currentStock(
        med: Medication,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
        plan: Plan = Plan(),
    ): Double {
        val used = used(med, toLocal(med.stockAt, zone), toLocal(nowMillis, zone), plan)
        return round2(max(0.0, med.stock - used))
    }

    fun status(
        med: Medication,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
        plan: Plan = Plan(),
    ): Status {
        val current = currentStock(med, nowMillis, zone, plan)
        val lead = (med.leadDays ?: plan.leadDays).coerceAtLeast(0)
        val slots = slots(med, plan)
        if (slots.isEmpty()) {
            return Status(current, null, null, null, Level.NO_PLAN, null, lead)
        }
        val packDays = if (med.packSize > 0) floor(med.packSize / dailyAverage(med) + EPS).toInt() else null

        val now = toLocal(nowMillis, zone)
        val today = now.toLocalDate()
        var remaining = current
        var emptyOn: LocalDate? = null
        search@ for (offset in 0..MAX_DAYS) {
            val day = today.plusDays(offset.toLong())
            if (!isDoseDay(med, day)) continue
            for ((time, dose) in slots) {
                if (!day.atTime(time).isAfter(now)) continue
                if (remaining + EPS < dose) {
                    emptyOn = day
                    break@search
                }
                remaining -= dose
            }
        }
        if (emptyOn == null) {
            return Status(current, null, null, null, Level.OK, packDays, lead)
        }

        val daysLeft = ChronoUnit.DAYS.between(today, emptyOn).toInt()
        val smallestDose = slots.minOf { it.second }
        val level = when {
            daysLeft <= 0 || current + EPS < smallestDose -> Level.EMPTY
            daysLeft <= lead -> Level.REORDER
            else -> Level.OK
        }
        return Status(current, daysLeft, emptyOn, emptyOn.minusDays(lead.toLong()), level, packDays, lead)
    }

    /** Einnahmen des Tages mit Uhrzeit und Menge, nach Uhrzeit sortiert. */
    private fun slots(med: Medication, plan: Plan): List<Pair<LocalTime, Double>> =
        listOf(plan.morning to med.morning, plan.noon to med.noon, plan.evening to med.evening)
            .filter { it.second > 0.0 }
            .sortedBy { it.first }

    /** Verbrauch nach [from] und bis einschließlich [to]. */
    internal fun used(med: Medication, from: LocalDateTime, to: LocalDateTime, plan: Plan): Double {
        if (!to.isAfter(from)) return 0.0
        val slots = slots(med, plan)
        if (slots.isEmpty()) return 0.0
        // Nach über hundert Jahren ist sicher alles aufgebraucht.
        if (ChronoUnit.DAYS.between(from.toLocalDate(), to.toLocalDate()) > 36600) return Double.MAX_VALUE
        var sum = 0.0
        var day = from.toLocalDate()
        val last = to.toLocalDate()
        while (!day.isAfter(last)) {
            if (isDoseDay(med, day)) {
                for ((time, dose) in slots) {
                    val at = day.atTime(time)
                    if (at.isAfter(from) && !at.isAfter(to)) sum += dose
                }
            }
            day = day.plusDays(1)
        }
        return sum
    }

    private fun toLocal(millis: Long, zone: ZoneId): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), zone)

    private fun round2(value: Double): Double = (value * 100).roundToLong() / 100.0
}
