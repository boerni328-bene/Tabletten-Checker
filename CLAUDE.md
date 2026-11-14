# CLAUDE.md – Arbeitsregeln für das Projekt Tabletten-Checker

## Beim Start jeder Sitzung

Zuerst `docs/uebergabe/uebergabe.md` lesen (einzige Übergabedatei, wird überschrieben, etwa 60 Zeilen).

## Projektbeschreibung

**Kernsatz** (wörtlich gleich in README und Releases): _Tabletten-Checker behält den Überblick über den Tablettenvorrat: Bestand, Reichweite und Nachkauf, lokal gespeichert, ohne Cloud._

**Für wen:** zuerst für den Projektinhaber selbst (Tabletten täglich morgens und abends). Repository öffentlich, Verteilung über GitHub Releases und Obtainium.

**Entscheidungen des Projektinhabers (13.11.2026):**
- Bestand zählt **automatisch** nach dem Einnahmeplan herunter (kein tägliches Abhaken); von Hand korrigierbar.
- Benachrichtigungen **nur für den Nachkauf**, keine Einnahme-Erinnerung.
- Repository **öffentlich**.

**Bewusst nicht:** kein Internet, kein Konto, keine Werbung, kein Tracking, keine Cloud. Neue Berechtigungen nur nach Freigabe (der Build prüft die APK gegen eine Erlaubt-Liste).

## Arbeitsweise

- Vorbild ist das Projekt Moltobene (`D:\Claude\Projekte\Moltobene`): gleiche Werkzeuge und Versionen, gleicher Build.
- Gebaut wird nur über GitHub Actions (kein Android Studio/Java auf dem PC). Jeder Push auf `main` baut; eine neue `versionName` ergibt ein Release.
- Jede Version: `versionCode` und `versionName` in `app/build.gradle.kts` erhöhen und Abschnitt `## [x.y.z] – Datum (versionCode n)` in `CHANGELOG.md` ergänzen, sonst bricht der Build ab.
- Schritt für Schritt: kleine Versionen, der Projektinhaber prüft jede auf dem Handy.
- Projektinhaber ist kein Entwickler: Deutsch, kurz, vor größeren Entscheidungen fragen.
- Texte der App: ohne direkte Anrede, neutral und knapp. Feste Begriffe: Tablette, Bestand, Packung, Nachkauf/nachkaufen, Übersicht, „Reicht noch … Tage“.
- Signaturschlüssel: liegt außerhalb des Repositorys (`D:\Claude\Projekte\Tabletten-Checker-Signatur`) und als Secrets auf GitHub. Nie ins Repository, nie ersetzen (sonst lassen sich Updates nicht mehr installieren).

## Aufbau des Codes

- `data/Medication.kt` – Tablette, Einstellungen und Dateiformat; `data/MedicationStore.kt` – speichert `tabletten.json` im App-Ordner.
- `data/StockCalculator.kt` – Rechnung für Bestand, Reichweite, Nachkauf; `data/Shopping.kt` – Einkaufsliste und gemeinsamer Einkauf (beide ohne Android, Tests in `app/src/test`).
- `ui/OverviewScreen.kt`, `ui/EditScreen.kt`, `ui/SettingsScreen.kt`, `ui/ShoppingScreen.kt`, `ui/DoseFilter.kt` (Tageszeit-Auswahl, farbige Einnahme-Marken), `ui/MainViewModel.kt`, `MainActivity.kt` (einfache Seitenwahl ohne Navigationsbibliothek).
- `notify/ReorderNotifier.kt` – täglicher Wecker und Nachkauf-Benachrichtigung; `data/ReorderCheck.kt` – wann ein Hinweis kommt (getestet).
