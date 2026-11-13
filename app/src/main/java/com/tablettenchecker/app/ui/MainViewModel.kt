package com.tablettenchecker.app.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.tablettenchecker.app.data.Medication
import com.tablettenchecker.app.data.MedicationInput
import com.tablettenchecker.app.data.MedicationStore
import com.tablettenchecker.app.data.StockCalculator
import java.io.File
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val store = MedicationStore(File(application.filesDir, "tabletten.json"))

    var medications by mutableStateOf(store.load())
        private set

    /** Zeitpunkt, für den die Übersicht rechnet; wird bei jedem Öffnen der App erneuert. */
    var now by mutableLongStateOf(System.currentTimeMillis())
        private set

    private val settings = application.getSharedPreferences("einstellungen", Context.MODE_PRIVATE)

    /** Kompakte Ansicht (eine Zeile je Tablette) statt Karten; bleibt gemerkt. */
    var compact by mutableStateOf(settings.getBoolean("kompakt", false))
        private set

    fun toggleCompact() {
        compact = !compact
        settings.edit().putBoolean("kompakt", compact).apply()
    }

    fun refreshNow() {
        now = System.currentTimeMillis()
    }

    /** Legt eine Tablette an ([id] = null) oder ändert sie. Der eingegebene Bestand gilt ab jetzt. */
    fun save(id: String?, input: MedicationInput) {
        refreshNow()
        val medication = Medication(
            id = id ?: UUID.randomUUID().toString(),
            name = input.name.trim(),
            note = input.note.trim(),
            morning = input.morning,
            evening = input.evening,
            stock = input.stock,
            stockAt = now,
            packSize = input.packSize,
        )
        update(
            if (medications.any { it.id == medication.id }) {
                medications.map { if (it.id == medication.id) medication else it }
            } else {
                medications + medication
            }
        )
    }

    fun delete(id: String) {
        update(medications.filterNot { it.id == id })
    }

    /** Eine gekaufte Packung zum Bestand zählen. */
    fun addPack(id: String) {
        refreshNow()
        update(
            medications.map {
                if (it.id == id && it.packSize > 0) {
                    it.copy(stock = StockCalculator.currentStock(it, now) + it.packSize, stockAt = now)
                } else {
                    it
                }
            }
        )
    }

    private fun update(list: List<Medication>) {
        medications = list
        store.save(list)
    }
}
