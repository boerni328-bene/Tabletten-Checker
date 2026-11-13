package com.tablettenchecker.app

import com.tablettenchecker.app.data.Medication
import com.tablettenchecker.app.data.StockCalculator
import com.tablettenchecker.app.data.StockCalculator.Level
import com.tablettenchecker.app.ui.formatAmount
import com.tablettenchecker.app.ui.parseAmount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class StockCalculatorTest {

    private val zone = ZoneOffset.UTC

    private fun at(day: Int, hour: Int, minute: Int = 0): Long =
        LocalDateTime.of(2026, 11, day, hour, minute).toInstant(ZoneOffset.UTC).toEpochMilli()

    private fun med(morning: Double, evening: Double, stock: Double, stockAt: Long, packSize: Int = 0) =
        Medication(
            id = "1",
            name = "Test",
            morning = morning,
            evening = evening,
            stock = stock,
            stockAt = stockAt,
            packSize = packSize,
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
    fun zahlenLesenUndSchreiben() {
        assertEquals("3", formatAmount(3.0))
        assertEquals("0,5", formatAmount(0.5))
        assertEquals("1,25", formatAmount(1.25))
        assertEquals(0.5, parseAmount("0,5")!!, 0.001)
        assertEquals(2.0, parseAmount(" 2 ")!!, 0.001)
        assertEquals(0.0, parseAmount("")!!, 0.001)
        assertNull(parseAmount("abc"))
        assertNull(parseAmount("-1"))
    }
}
