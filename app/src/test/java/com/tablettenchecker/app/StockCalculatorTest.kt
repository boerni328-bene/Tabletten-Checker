package com.tablettenchecker.app

import com.tablettenchecker.app.data.Medication
import com.tablettenchecker.app.data.Shopping
import com.tablettenchecker.app.data.StockCalculator
import com.tablettenchecker.app.data.StockCalculator.Level
import com.tablettenchecker.app.ui.formatAmount
import com.tablettenchecker.app.ui.formatTime
import com.tablettenchecker.app.ui.parseAmount
import com.tablettenchecker.app.ui.parseTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

class StockCalculatorTest {

    private val zone = ZoneOffset.UTC

    private fun at(day: Int, hour: Int, minute: Int = 0): Long =
        LocalDateTime.of(2026, 11, day, hour, minute).toInstant(ZoneOffset.UTC).toEpochMilli()

    private fun med(
        morning: Double,
        evening: Double,
        stock: Double,
        stockAt: Long,
        packSize: Int = 0,
        noon: Double = 0.0,
        everyDays: Int = 1,
        anchorDay: Long = 0L,
        leadDays: Int? = null,
        id: String = "1",
    ) = Medication(
        id = id,
        name = "Test $id",
        morning = morning,
        noon = noon,
        evening = evening,
        everyDays = everyDays,
        anchorDay = anchorDay,
        stock = stock,
        stockAt = stockAt,
        packSize = packSize,
        leadDays = leadDays,
    )

    @Test
    fun bestandSinktMitJederEinnahme() {
        // Gezählt am 1.11. um 10 Uhr; bis 3.11. 9 Uhr: 1.11. abends, 2.11. morgens und abends, 3.11. morgens.
        val m = med(1.0, 1.0, 20.0, at(1, 10))
        assertEquals(16.0, StockCalculator.currentStock(m, at(3, 9), zone), 0.001)
    }

    @Test
    fun einnahmeZumZeitpunktDerZaehlungZaehltNicht() {
        val m = med(1.0, 1.0, 20.0, at(1, 8))
        assertEquals(20.0, StockCalculator.currentStock(m, at(1, 8), zone), 0.001)
        assertEquals(19.0, StockCalculator.currentStock(m, at(1, 20), zone), 0.001)
    }

    @Test
    fun bestandWirdNieNegativ() {
        val m = med(1.0, 1.0, 2.0, at(1, 10))
        assertEquals(0.0, StockCalculator.currentStock(m, at(20, 10), zone), 0.001)
    }

    @Test
    fun reichweiteUndNachkauf() {
        // 4 Stück am 1.11. um 10 Uhr: 1.11. abends, 2.11. morgens und abends, 3.11. morgens; am 3.11. abends fehlt eine.
        val status = StockCalculator.status(med(1.0, 1.0, 4.0, at(1, 10)), at(1, 10), zone)
        assertEquals(LocalDate.of(2026, 11, 3), status.emptyOn)
        assertEquals(2, status.daysLeft)
        assertEquals(Level.REORDER, status.level)
    }

    @Test
    fun langeReichweiteIstInOrdnung() {
        // 100 Stück, eine morgens: erste Einnahme am 2.11., die 100. am 9.2.2027, am 10.2.2027 fehlt eine.
        val status = StockCalculator.status(med(1.0, 0.0, 100.0, at(1, 10), packSize = 50), at(1, 10), zone)
        assertEquals(LocalDate.of(2027, 2, 10), status.emptyOn)
        assertEquals(101, status.daysLeft)
        assertEquals(LocalDate.of(2027, 1, 27), status.reorderBy)
        assertEquals(Level.OK, status.level)
        assertEquals(50, status.packDays)
    }

    @Test
    fun halbeTabletten() {
        val status = StockCalculator.status(med(0.5, 0.0, 10.0, at(1, 10), packSize = 20), at(1, 10), zone)
        assertEquals(20, status.daysLeft?.minus(1))
        assertEquals(40, status.packDays)
    }

    @Test
    fun leererBestand() {
        val status = StockCalculator.status(med(0.0, 1.0, 0.0, at(1, 10)), at(1, 10), zone)
        assertEquals(0, status.daysLeft)
        assertEquals(Level.EMPTY, status.level)
    }

    @Test
    fun leererBestandNachDerLetztenEinnahmeDesTages() {
        // Nur morgens, heute schon genommen, nichts mehr da: morgen fehlt eine.
        val status = StockCalculator.status(med(1.0, 0.0, 0.0, at(1, 10)), at(1, 10), zone)
        assertEquals(1, status.daysLeft)
        assertEquals(Level.EMPTY, status.level)
    }

    @Test
    fun ohneEinnahmeKeineReichweite() {
        val status = StockCalculator.status(med(0.0, 0.0, 30.0, at(1, 10)), at(5, 10), zone)
        assertEquals(30.0, status.current, 0.001)
        assertNull(status.daysLeft)
        assertEquals(Level.NO_PLAN, status.level)
    }

    @Test
    fun mittagsZaehltMit() {
        // Gezählt am 1.11. um 10 Uhr; bis 2.11. 13 Uhr: 1.11. mittags, 2.11. morgens und mittags.
        val m = med(1.0, 0.0, 10.0, at(1, 10), noon = 1.0)
        assertEquals(7.0, StockCalculator.currentStock(m, at(2, 13), zone), 0.001)
    }

    @Test
    fun einnahmeAlleZweiTage() {
        // Einnahmetage 1., 3., 5., 7., … November; am 1.11. um 10 Uhr ist die Einnahme schon vorbei.
        val anchor = LocalDate.of(2026, 11, 1).toEpochDay()
        val m = med(1.0, 0.0, 3.0, at(1, 10), everyDays = 2, anchorDay = anchor, packSize = 10)
        assertEquals(2.0, StockCalculator.currentStock(m, at(4, 10), zone), 0.001)
        val status = StockCalculator.status(m, at(1, 10), zone)
        // Einnahmen am 3., 5. und 7.; am 9. fehlt eine.
        assertEquals(LocalDate.of(2026, 11, 9), status.emptyOn)
        assertEquals(20, status.packDays)
    }

    @Test
    fun eigenerVorlauf() {
        val status = StockCalculator.status(med(1.0, 0.0, 100.0, at(1, 10), leadDays = 30), at(1, 10), zone)
        assertEquals(LocalDate.of(2027, 1, 11), status.reorderBy)
        assertEquals(30, status.leadDays)
    }

    @Test
    fun andereUhrzeiten() {
        // Morgens erst um 11 Uhr: Um 10 Uhr ist die Einnahme des Tages noch nicht genommen.
        val plan = StockCalculator.Plan(morning = LocalTime.of(11, 0))
        val m = med(1.0, 0.0, 10.0, at(1, 6))
        assertEquals(10.0, StockCalculator.currentStock(m, at(1, 10), zone, plan), 0.001)
        assertEquals(9.0, StockCalculator.currentStock(m, at(1, 11), zone, plan), 0.001)
    }

    @Test
    fun einkaufGemeinsamUndMenge() {
        val today = LocalDate.of(2026, 11, 1)
        val a = med(1.0, 1.0, 4.0, at(1, 10), packSize = 28, id = "a")
        val b = med(1.0, 0.0, 100.0, at(1, 10), packSize = 30, id = "b")
        val c = med(1.0, 0.0, 20.0, at(1, 10), id = "c")
        val rows = listOf(a, b, c).map { it to StockCalculator.status(it, at(1, 10), zone) }

        // a ist sofort fällig, c bis 7.11., b erst im Januar: a und c gemeinsam.
        val trip = Shopping.nextTrip(rows, today, 14)!!
        assertEquals(today, trip.date)
        assertEquals(listOf("a", "c"), trip.medications.map { it.id })

        val items = Shopping.items(rows, 90, today).associateBy { it.medication.id }
        // a: 2 am Tag × 90 = 180, 4 da, fehlen 176 → 7 Packungen à 28.
        assertEquals(176.0, items.getValue("a").needed, 0.001)
        assertEquals(7, items.getValue("a").packs)
        assertEquals(196.0, Shopping.purchaseAmount(items.getValue("a")), 0.001)
        // c ohne Packungsgröße: 70 Stück.
        assertNull(items.getValue("c").packs)
        assertEquals(70.0, Shopping.purchaseAmount(items.getValue("c")), 0.001)
        // b hat 100 Stück für 90 Tage: nichts nötig.
        assertNull(items["b"])
    }

    @Test
    fun zahlenUndUhrzeitenLesenUndSchreiben() {
        assertEquals("3", formatAmount(3.0))
        assertEquals("0,5", formatAmount(0.5))
        assertEquals("1,25", formatAmount(1.25))
        assertEquals(0.5, parseAmount("0,5")!!, 0.001)
        assertEquals(2.0, parseAmount(" 2 ")!!, 0.001)
        assertEquals(0.0, parseAmount("")!!, 0.001)
        assertNull(parseAmount("abc"))
        assertNull(parseAmount("-1"))
        assertEquals(480, parseTime("8"))
        assertEquals(510, parseTime("8:30"))
        assertEquals(1215, parseTime("20.15"))
        assertNull(parseTime("25:00"))
        assertEquals("08:00", formatTime(480))
    }
}
