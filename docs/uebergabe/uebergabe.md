# Tabletten-Checker – Übergabe (einzige Datei, wird laufend überschrieben)

Stand: 13.11.2026 · `main` = Version 0.3.0 (versionCode 3) · Regeln in `CLAUDE.md`, Verlauf in Git und `CHANGELOG.md`.

## Projekt in drei Sätzen
Tabletten-Checker: Android-App (Kotlin, Compose, `com.tablettenchecker.app`, ab Android 8), die Bestand, Reichweite und Nachkauf der täglichen Tabletten zeigt, lokal gespeichert, ohne Cloud. Repository `boerni328-bene/Tabletten-Checker` (öffentlich), Verteilung über GitHub Releases und Obtainium. Gebaut nur über GitHub Actions, Vorbild ist Moltobene (`D:\Claude\Projekte\Moltobene`).

## Stand 0.3.0
- Inhaber nutzt die App mit 7 Tabletten (0.2.0 auf dem Handy geprüft, 13.11.2026).
- Übersicht: Auswahl Alle/Morgens/(Mittags)/Abends (gemerkt), Umschalter Kompakt/Karten (gemerkt), Hinweis „Nächster Einkauf bis …“ oben. Kompakt bei „Alle“ in Abschnitten nach erster Tageszeit, darin nach Namen; Karten nach Dringlichkeit.
- Einnahmemengen als farbige Marken (`DoseChips` in `ui/DoseFilter.kt`, Farben `doseColors` in `ui/Theme.kt`).
- Einkaufsliste (`ui/ShoppingScreen.kt`, Rechnung `data/Shopping.kt`): Vorrat `supplyDays` (Standard 90), gemeinsamer Termin = frühester Nachkauf + Tabletten, die bis `leadDays` danach fällig werden. Abhaken, Teilen als Text, „Als gekauft eintragen“.
- Einstellungen (`ui/SettingsScreen.kt`): Uhrzeiten, Mittags an/aus, Vorlauf, Vorrat; Sichern/Wiederherstellen über den Android-Dateidialog (keine Berechtigung). Einstellungen liegen in `tabletten.json` (`MedicationFile.settings`) und kommen mit in die Sicherung. Uhrzeiten ändern schreibt vorher den Bestand fest.
- Je Tablette optional: `everyDays`/`anchorDay` (alle n Tage), `leadDays` (eigener Vorlauf).
- Daten: `MedicationFile.version = 1` (nur Felder mit Standardwerten ergänzt, alte Dateien bleiben lesbar).
- Auf dem Handy noch nicht geprüft: alles aus 0.3.0.

## Entscheidungen (Inhaber, 13.11.2026)
- Bestand automatisch herunterzählen, kein tägliches Abhaken.
- Benachrichtigungen nur für den Nachkauf.
- Repository öffentlich.
- Vorschläge 1–5 umsetzen (Einkaufsliste, eigener Vorlauf nur optional, gemeinsamer Einkauf, Sichern, Einstellungen); kompakte Ansicht nach Tageszeit mit Auswahl und farbigen Mengen.

## Offen / nächste Schritte
1. Inhaber prüft 0.3.0 auf dem Handy (besonders: Lesbarkeit der farbigen Marken, Einkaufsliste, Sichern).
2. Nachkauf-Hinweis als Benachrichtigung: braucht `POST_NOTIFICATIONS` (und für tägliche Prüfung z. B. WorkManager, der weitere Berechtigungen mitbringt) → vorher Freigabe einholen, Erlaubt-Liste in `build.yml` ergänzen.
3. Rundgang auf dem Emulator mit Bildschirmfotos (wie Moltobene).
Offene Fragen: Lizenz (Moltobene: GPL-3.0), Englisch als zweite Sprache nötig?

## Arbeitsweise
- Push auf `main` baut (ca. 4 Minuten ohne Emulator); neue `versionName` + CHANGELOG-Abschnitt ergibt ein Release.
- Fehler: `gh run view <id> --log-failed`; Lint-Bericht als Artefakt `lint-bericht-<version>`, Testbericht `testbericht-<version>` bei Fehlern.
- Windows: Mehrzeiliges mit Umlauten per Write-Werkzeug; Git-Autor im Repository gesetzt (wie Moltobene).
