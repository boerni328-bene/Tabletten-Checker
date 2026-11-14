# Tabletten-Checker – Übergabe (einzige Datei, wird laufend überschrieben)

Stand: 14.11.2026 · `main` = Version 0.4.0 (versionCode 4) · Regeln in `CLAUDE.md`, Verlauf in Git und `CHANGELOG.md`.

## Projekt in drei Sätzen
Tabletten-Checker: Android-App (Kotlin, Compose, `com.tablettenchecker.app`, ab Android 8), die Bestand, Reichweite und Nachkauf der täglichen Tabletten zeigt, lokal gespeichert, ohne Cloud. Repository `boerni328-bene/Tabletten-Checker` (öffentlich), Verteilung über GitHub Releases und Obtainium. Gebaut nur über GitHub Actions, Vorbild ist Moltobene (`D:\Claude\Projekte\Moltobene`).

## Stand 0.4.0
- Neu in 0.4.0: Nachkauf-Hinweis (`notify/ReorderNotifier.kt`, Entscheidung `data/ReorderCheck.kt`): ungefährer Tageswecker (AlarmManager, keine Berechtigung für genaue Wecker) zur Uhrzeit `notifyMinutes` (Standard 9:00); Hinweis bei neu Fälligem, sonst alle 3 Tage; gemerkt in SharedPreferences `hinweis_tag`/`hinweis_ids`. `StartReceiver` stellt den Wecker nach Neustart und Update neu. Berechtigungen POST_NOTIFICATIONS und RECEIVE_BOOT_COMPLETED (Freigabe 14.11.2026, Erlaubt-Liste in `build.yml`).
- Inhaber nutzt die App mit 7 Tabletten (0.2.0 auf dem Handy geprüft, 13.11.2026).
- Übersicht: Auswahl Alle/Morgens/(Mittags)/Abends (gemerkt), Umschalter Kompakt/Karten (gemerkt), Hinweis „Nächster Einkauf bis …“ oben. Kompakt bei „Alle“ in Abschnitten nach erster Tageszeit, darin nach Namen; Karten nach Dringlichkeit.
- Einnahmemengen als farbige Marken (`DoseChips` in `ui/DoseFilter.kt`, Farben `doseColors` in `ui/Theme.kt`).
- Einkaufsliste (`ui/ShoppingScreen.kt`, Rechnung `data/Shopping.kt`): Vorrat `supplyDays` (Standard 90), gemeinsamer Termin = frühester Nachkauf + Tabletten, die bis `leadDays` danach fällig werden. Abhaken, Teilen als Text, „Als gekauft eintragen“.
- Einstellungen (`ui/SettingsScreen.kt`): Uhrzeiten, Mittags an/aus, Vorlauf, Vorrat; Sichern/Wiederherstellen über den Android-Dateidialog (keine Berechtigung). Einstellungen liegen in `tabletten.json` (`MedicationFile.settings`) und kommen mit in die Sicherung. Uhrzeiten ändern schreibt vorher den Bestand fest.
- Je Tablette optional: `everyDays`/`anchorDay` (alle n Tage), `leadDays` (eigener Vorlauf).
- Daten: `MedicationFile.version = 1` (nur Felder mit Standardwerten ergänzt, alte Dateien bleiben lesbar).
- Auf dem Handy noch nicht geprüft: alles aus 0.3.0 und 0.4.0.

## Entscheidungen (Inhaber, 13.11.2026)
- Bestand automatisch herunterzählen, kein tägliches Abhaken.
- Benachrichtigungen nur für den Nachkauf.
- Repository öffentlich.
- Nachkauf-Hinweis mit Berechtigungen Benachrichtigung und „Beim Start ausführen“; bei Neuem sofort, sonst alle 3 Tage (14.11.2026).
- Vorschläge 1–5 umsetzen (Einkaufsliste, eigener Vorlauf nur optional, gemeinsamer Einkauf, Sichern, Einstellungen); kompakte Ansicht nach Tageszeit mit Auswahl und farbigen Mengen.

## Offen / nächste Schritte
1. Inhaber prüft 0.3.0/0.4.0 auf dem Handy (Lesbarkeit der Marken, Einkaufsliste, Sichern, ob der Hinweis kommt).
2. Rundgang auf dem Emulator mit Bildschirmfotos (wie Moltobene).
Offene Fragen: Lizenz (Moltobene: GPL-3.0), Englisch als zweite Sprache nötig?

## Arbeitsweise
- Push auf `main` baut (ca. 4 Minuten ohne Emulator); neue `versionName` + CHANGELOG-Abschnitt ergibt ein Release.
- Fehler: `gh run view <id> --log-failed`; Lint-Bericht als Artefakt `lint-bericht-<version>`, Testbericht `testbericht-<version>` bei Fehlern.
- Windows: Mehrzeiliges mit Umlauten per Write-Werkzeug; Git-Autor im Repository gesetzt (wie Moltobene).
