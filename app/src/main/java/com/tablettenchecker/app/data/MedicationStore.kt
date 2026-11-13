package com.tablettenchecker.app.data

import kotlinx.serialization.json.Json
import java.io.File

/** Speichert die Tablettenliste als kleine Datei im privaten Ordner der App. */
class MedicationStore(private val file: File) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun load(): List<Medication> {
        if (!file.exists()) return emptyList()
        return try {
            json.decodeFromString(MedicationFile.serializer(), file.readText()).medications
        } catch (e: Exception) {
            // Unlesbare Datei aufheben statt überschreiben, damit nichts verloren geht.
            file.copyTo(File(file.parentFile, file.name + ".defekt"), overwrite = true)
            emptyList()
        }
    }

    fun save(medications: List<Medication>) {
        val text = json.encodeToString(MedicationFile.serializer(), MedicationFile(medications = medications))
        // Erst daneben schreiben, dann austauschen: Die alte Datei bleibt ganz, falls das Schreiben abbricht.
        val temp = File(file.parentFile, file.name + ".neu")
        temp.writeText(text)
        if (!temp.renameTo(file)) {
            file.writeText(text)
            temp.delete()
        }
    }
}
