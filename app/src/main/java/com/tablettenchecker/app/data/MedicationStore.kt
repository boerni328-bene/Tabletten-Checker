package com.tablettenchecker.app.data

import kotlinx.serialization.json.Json
import java.io.File

/** Speichert Tabletten und Einstellungen als kleine Datei im privaten Ordner der App. */
class MedicationStore(private val file: File) {

    fun load(): MedicationFile {
        if (!file.exists()) return MedicationFile()
        return try {
            decode(file.readText())
        } catch (e: Exception) {
            // Unlesbare Datei aufheben statt überschreiben, damit nichts verloren geht.
            file.copyTo(File(file.parentFile, file.name + ".defekt"), overwrite = true)
            MedicationFile()
        }
    }

    fun save(data: MedicationFile) {
        val text = encode(data)
        // Erst daneben schreiben, dann austauschen: Die alte Datei bleibt ganz, falls das Schreiben abbricht.
        val temp = File(file.parentFile, file.name + ".neu")
        temp.writeText(text)
        if (!temp.renameTo(file)) {
            file.writeText(text)
            temp.delete()
        }
    }

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        fun encode(data: MedicationFile): String = json.encodeToString(MedicationFile.serializer(), data)

        fun decode(text: String): MedicationFile = json.decodeFromString(MedicationFile.serializer(), text)
    }
}
