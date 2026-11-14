# Änderungsprotokoll

Alle wichtigen Änderungen an der App werden hier festgehalten.
Die neueste Version steht oben. Versionsschema: Major.Minor.Patch.

## [0.4.0] – 2026-11-14 (versionCode 4)

### Neu
- **Nachkauf-Hinweis:** Einmal am Tag (Standard 9 Uhr, ungefähr) prüft die App den Vorrat. Ist eine Tablette nachzukaufen, kommt die Benachrichtigung „Tabletten nachkaufen“ mit den fälligen Tabletten und dem, was beim selben Einkauf gleich mitkommt. Danach alle 3 Tage eine Erinnerung, bis „Als gekauft eintragen“ oder „Packung hinzufügen“ den Bestand erhöht hat. Antippen öffnet die Einkaufsliste.
- In den Einstellungen: „Hinweis zum Nachkauf“ an/aus und die Uhrzeit. Ab Android 13 fragt die App einmal, ob Benachrichtigungen erlaubt sind.

### Berechtigungen
- Benachrichtigungen und „Beim Start ausführen“ (stellt den täglichen Wecker nach einem Neustart des Handys neu). Beide nur für den Nachkauf-Hinweis, freigegeben vom Projektinhaber am 14.11.2026. Weiterhin kein Internet.

## [0.3.0] – 2026-11-13 (versionCode 3)

### Neu
- **Auswahl nach Tageszeit:** Oben „Alle“, „Morgens“, „Abends“ (und „Mittags“, wenn eingeschaltet). „Morgens“ zeigt nur die Tabletten für morgens mit ihrer Menge – praktisch beim Richten der Tabletten. Die Auswahl bleibt gemerkt.
- **Kompakte Ansicht nach Tageszeit geordnet:** Bei „Alle“ in den Abschnitten Morgens, Mittags, Abends, darin nach Namen.
- **Einnahmemengen farbig:** „morgens 1“ (blau), „mittags 1“ (grün), „abends 1“ (violett) als Marken, klar getrennt vom Bestand.
- **Einkaufsliste** (Einkaufswagen oben): wie viele Packungen von welcher Tablette für einen Vorrat von 90 Tagen nötig sind, zum Abhaken, Teilen (z. B. an die Apotheke) und „Als gekauft eintragen“, das den Bestand erhöht.
- **Gemeinsamer Einkauf:** Oben in der Übersicht steht „Nächster Einkauf bis …“ mit allen Tabletten, die bis 14 Tage danach ebenfalls fällig werden – ein Weg statt mehrerer.
- **Einstellungen** (Zahnrad oben): Uhrzeiten morgens, mittags und abends, Mittags als dritte Einnahmezeit, Vorlauf für den Nachkauf, Vorrat nach dem Einkauf.
- **Sichern und wiederherstellen** in den Einstellungen: Tabletten und Einstellungen als Datei, z. B. für ein neues Handy.
- **Weitere Angaben je Tablette** (beim Bearbeiten aufklappen): Einnahme nur alle paar Tage und ein eigener Vorlauf, z. B. für ein Rezept, das länger dauert.

## [0.2.0] – 2026-11-13 (versionCode 2)

### Neu
- **Kompakte Ansicht:** Oben rechts wechselt „Kompakt“ zu einer Tabelle mit einer Zeile je Tablette: Name mit Einnahme (morgens–abends) und Stärke, Bestand, Tage Reichweite und Nachkauf-Datum. So passen alle Tabletten auf einen Bildschirm. „Karten“ wechselt zurück; die gewählte Ansicht bleibt gemerkt. Tippen auf eine Zeile öffnet die Tablette zum Bearbeiten.

## [0.1.0] – 2026-11-13 (versionCode 1)

### Neu
- **Erste Version.** Übersicht aller Tabletten mit Bestand, Reichweite („Reicht noch … Tage“, „Aufgebraucht am …“) und Nachkauf („Nachkaufen bis …“, 14 Tage vor dem Aufbrauchen). Was am frühesten aufgebraucht ist, steht oben; bald Aufgebrauchtes ist farbig hervorgehoben.
- **Tablette hinzufügen und bearbeiten:** Name, Stärke oder Hinweis, Stück morgens und abends (auch halbe, z. B. 0,5), Bestand und Stück pro Packung.
- **Bestand zählt selbst herunter:** Die App zieht die Einnahme morgens um 8 Uhr und abends um 20 Uhr vom Bestand ab. Der Bestand lässt sich jederzeit korrigieren.
- **Packung hinzufügen:** Eine gekaufte Packung mit einem Tippen zum Bestand zählen.
- Alles bleibt auf dem Gerät: kein Internet, kein Konto, keine Berechtigung.
