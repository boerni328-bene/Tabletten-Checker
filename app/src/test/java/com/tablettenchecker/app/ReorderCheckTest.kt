package com.tablettenchecker.app

import com.tablettenchecker.app.data.Medication
import com.tablettenchecker.app.data.ReorderCheck
import com.tablettenchecker.app.data.StockCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class ReorderCheckTest {

    private val now = LocalDateTime.of(2026, 11, 1, 10, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
    private val today = LocalDate.of(2026, 11, 1)

    private fun med(id: String, stock: Double) =
        Medication(id = id, name = id, morning = 1.0, stock = stock, stockAt = now)

    private fun rows(vararg meds: Medication) = meds.map { it to StockCalculator.status(it, now, ZoneOffset.UTC) }

    @Test
    fun faelligUndGleichMit() {
        // a reicht 4 Tage (fällig), b 20 Tage (Nachkauf am 8.11., kommt mit), c 100 Tage (nicht dabei).
        val result = ReorderCheck.due(rows(med("a", 4.0), med("b", 20.0), med("c", 100.0)), today, 14)!!
        assertEquals(listOf("a"), result.due.map { it.id })
        assertEquals(listOf("b"), result.along.map { it.id })
    }

    @Test
    fun nichtsFaellig() {
        assertNull(ReorderCheck.due(rows(med("b", 20.0), med("c", 100.0)), today, 14))
    }

    @Test
    fun wiederholungAlleDreiTage() {
        val ids = setOf("a")
        assertTrue(ReorderCheck.shouldNotify(ids, today, null, emptySet()))
        assertFalse(ReorderCheck.shouldNotify(ids, today.plusDays(2), today, ids))
        assertTrue(ReorderCheck.shouldNotify(ids, today.plusDays(3), today, ids))
        // Neu fällig: sofort, auch am selben Tag.
        assertTrue(ReorderCheck.shouldNotify(setOf("a", "b"), today, today, ids))
        assertFalse(ReorderCheck.shouldNotify(emptySet(), today, null, emptySet()))
    }
}
