# Tabletten-Checker – Übergabe (einzige Datei, wird laufend überschrieben)

Stand: 13.11.2026 · `main` = Version 0.1.0 (versionCode 1) · Regeln in `CLAUDE.md`, Verlauf in Git und `CHANGELOG.md`.

## Projekt in drei Sätzen
Tabletten-Checker: Android-App (Kotlin, Compose, `com.tablettenchecker.app`, ab Android 8), die Bestand, Reichweite und Nachkauf der täglichen Tabletten (morgens/abends) zeigt, lokal gespeichert, ohne Cloud. Repository `boerni328-bene/Tabletten-Checker` (öffentlich), Verteilung über GitHub Releases und Obtainium. Gebaut nur über GitHub Actions, Vorbild ist Moltobene (`D:\Claude\Projekte\Moltobene`).

## Stand 0.1.0
- Übersicht (dringendste Tablette oben, farbig bei „Jetzt nachkaufen“/aufgebraucht), Tablette hinzufügen/bearbeiten/löschen, „Packung hinzufügen“.
- Bestand zählt selbst herunter: feste Zeiten 8 Uhr und 20 Uhr, Vorlauf für den Nachkauf fest 14 Tage (`StockCalculator.MORNING/EVENING/LEAD_DAYS`).
- Daten: `tabletten.json` im App-Ordner (kotlinx.serialization, `MedicationFile.version = 1`). Android-Cloud-Sicherung aus (`allowBackup=false`).
- Build: `build.yml` mit Lint, Unit-Tests, signierter APK, Signatur- und Berechtigungsprüfung, Release. Noch **kein** Rundgang auf dem Emulator (keine Bildschirmfotos).
- Signatur: Schlüssel in `D:\Claude\Projekte\Tabletten-Checker-Signatur` (PKCS12, Alias `tabletten`) und als vier Secrets auf GitHub.

## Entscheidungen (Inhaber, 13.11.2026)
- Bestand automatisch herunterzählen, kein tägliches Abhaken.
- Benachrichtigungen nur für den Nachkauf.
- Repository öffentlich.

## Offen / nächste Schritte (Vorschlag, Reihenfolge mit dem Inhaber klären)
1. Inhaber installiert 0.1.0 über Obtainium und prüft auf dem Handy; Signaturordner zusätzlich sichern.
2. Nachkauf-Hinweis als Benachrichtigung (braucht Berechtigung `POST_NOTIFICATIONS` → Freigabe und Erlaubt-Liste in `build.yml`).
3. Einstellungen: Uhrzeiten morgens/abends, Vorlauf in Tagen.
4. Einkaufsliste „Was muss ich kaufen“ (wie viele Packungen bis zu einem Wunschvorrat), zum Teilen/Mitnehmen in die Apotheke.
5. Sichern und wiederherstellen (Datei), Umzug auf ein neues Handy.
6. Rundgang auf dem Emulator mit Bildschirmfotos (wie Moltobene).
Offene Fragen: Lizenz (Moltobene: GPL-3.0), Englisch als zweite Sprache nötig?

## Arbeitsweise
- Push auf `main` baut (ca. 5–10 Minuten ohne Emulator); neue `versionName` + CHANGELOG-Abschnitt ergibt ein Release.
- Fehler: `gh run view <id> --log-failed`; Lint-Bericht als Artefakt `lint-bericht-<version>`.
- Windows: Mehrzeiliges mit Umlauten per Write-Werkzeug; `gradlew` braucht das Ausführungsrecht in Git (`git update-index --chmod=+x gradlew`, ist gesetzt).
