package com.tablettenchecker.app.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.tablettenchecker.app.data.AppSettings
import com.tablettenchecker.app.data.Medication
import com.tablettenchecker.app.data.MedicationFile
import com.tablettenchecker.app.data.MedicationInput
import com.tablettenchecker.app.data.MedicationStore
import com.tablettenchecker.app.data.StockCalculator
import com.tablettenchecker.app.notify.ReorderNotifier
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val store = MedicationStore(File(application.filesDir, "tabletten.json"))
    private val initial = store.load()

    var medications by mutableStateOf(initial.medications)
        private set

    var settings by mutableStateOf(initial.settings)
        private set

    val plan: StockCalculator.Plan get() = StockCalculator.Plan.of(settings)

    /** Zeitpunkt, für den die Übersicht rechnet; wird bei jedem Öffnen der App erneuert. */
    var now by mutableLongStateOf(System.currentTimeMillis())
        private set

    private val prefs = application.getSharedPreferences("einstellungen", Context.MODE_PRIVATE)

    /** Kompakte Ansicht (eine Zeile je Tablette) statt Karten; bleibt gemerkt. */
    var compact by mutableStateOf(prefs.getBoolean("kompakt", false))
        private set

    /** Auswahl Alle / Morgens / Mittags / Abends; bleibt gemerkt. */
    var filter by mutableStateOf(
        DoseFilter.entries.firstOrNull { it.name == prefs.getString("auswahl", null) } ?: DoseFilter.ALL
    )
        private set

    init {
        // Wecker für den Nachkauf-Hinweis bei jedem Start stellen (z. B. falls Android ihn entfernt hat).
        ReorderNotifier.schedule(application, settings)
    }

    fun toggleCompact() {
        compact = !compact
        prefs.edit().putBoolean("kompakt", compact).apply()
    }

    fun selectFilter(value: DoseFilter) {
        filter = value
        prefs.edit().putString("auswahl", value.name).apply()
    }

    fun refreshNow() {
        now = System.currentTimeMillis()
    }

    fun currentStock(med: Medication): Double = StockCalculator.currentStock(med, now, plan = plan)

    /** Legt eine Tablette an ([id] = null) oder ändert sie. Der eingegebene Bestand gilt ab jetzt. */
    fun save(id: String?, input: MedicationInput) {
        refreshNow()
        val today = localDate(now)
        val medication = Medication(
            id = id ?: UUID.randomUUID().toString(),
            name = input.name.trim(),
            note = input.note.trim(),
            morning = input.morning,
            noon = input.noon,
            evening = input.evening,
            everyDays = input.everyDays,
            anchorDay = if (input.everyDays > 1) today.plusDays(input.nextDoseInDays.toLong()).toEpochDay() else 0L,
            stock = input.stock,
            stockAt = now,
            packSize = input.packSize,
            leadDays = input.leadDays,
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
        val med = medications.firstOrNull { it.id == id } ?: return
        if (med.packSize > 0) addPurchased(mapOf(id to med.packSize.toDouble()))
    }

    /** Gekaufte Mengen (Stück je Tablette) zum Bestand zählen. */
    fun addPurchased(amounts: Map<String, Double>) {
        refreshNow()
        update(
            medications.map { med ->
                val amount = amounts[med.id] ?: return@map med
                med.copy(stock = currentStock(med) + amount, stockAt = now)
            }
        )
    }

    fun updateSettings(new: AppSettings) {
        refreshNow()
        // Andere Uhrzeiten dürfen den Verbrauch der Vergangenheit nicht verändern: Bestand vorher festschreiben.
        val rebased = if (StockCalculator.Plan.of(new) != plan) {
            medications.map { it.copy(stock = currentStock(it), stockAt = now) }
        } else {
            medications
        }
        medications = rebased
        settings = new
        persist()
        ReorderNotifier.schedule(getApplication(), new)
    }

    /** Tabletten und Einstellungen in die gewählte Datei schreiben. */
    fun exportTo(uri: Uri): Boolean = try {
        val text = MedicationStore.encode(MedicationFile(medications = medications, settings = settings))
        getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.use {
            it.write(text.toByteArray(Charsets.UTF_8))
        } != null
    } catch (e: Exception) {
        false
    }

    /** Liest eine Sicherung; null, wenn die Datei keine Sicherung dieser App ist. */
    fun readBackup(uri: Uri): MedicationFile? = try {
        val bytes = getApplication<Application>().contentResolver.openInputStream(uri)?.use { readLimited(it) }
        val text = bytes?.toString(Charsets.UTF_8)
        if (text == null || !text.contains("\"medications\"")) {
            null
        } else {
            MedicationStore.decode(text).takeIf { file -> file.medications.all { it.id.isNotBlank() } }
        }
    } catch (e: Exception) {
        null
    }

    fun restore(file: MedicationFile) {
        refreshNow()
        medications = file.medications.distinctBy { it.id }
        settings = file.settings
        persist()
        ReorderNotifier.schedule(getApplication(), file.settings)
    }

    private fun update(list: List<Medication>) {
        medications = list
        persist()
    }

    private fun persist() {
        store.save(MedicationFile(medications = medications, settings = settings))
        // Überholten Nachkauf-Hinweis entfernen, z. B. nachdem gekauft wurde.
        ReorderNotifier.check(getApplication(), post = false)
    }

    /** Liest höchstens 2 MB; größere Dateien sind keine Sicherung dieser App. */
    private fun readLimited(input: InputStream, limit: Int = 2 * 1024 * 1024): ByteArray? {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            total += n
            if (total > limit) return null
            out.write(buffer, 0, n)
        }
        return out.toByteArray()
    }
}
