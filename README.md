# Tabletten-Checker

_Tabletten-Checker behält den Überblick über den Tablettenvorrat: Bestand, Reichweite und Nachkauf, lokal gespeichert, ohne Cloud._

Android-App (ab Android 8.0) für alle, die täglich morgens und abends Tabletten nehmen:

- **Übersicht:** von welcher Tablette noch wie viel da ist und wie lange es reicht.
- **Nachkauf:** bis wann nachgekauft werden sollte und wie lange eine Packung reicht.
- **Ohne tägliche Arbeit:** Die App zieht die Einnahme nach dem Plan selbst vom Bestand ab.
- **Nachkauf-Hinweis:** Benachrichtigung, sobald eine Tablette nachzukaufen ist.
- **Privat:** kein Internet, kein Konto, keine Werbung. Die Tablettenliste bleibt auf dem Gerät; Berechtigungen nur für den Nachkauf-Hinweis.

Die App ist eine Vorratshilfe. Sie ersetzt keinen ärztlichen Rat und keinen Einnahmeplan der Praxis oder Apotheke.

## Installation über Obtainium

1. In [Obtainium](https://github.com/ImranR98/Obtainium) „App hinzufügen“ wählen.
2. Als Quelle die Adresse dieses Repositorys eintragen: `https://github.com/boerni328-bene/Tabletten-Checker`
3. „Hinzufügen“ und danach „Installieren“. Neue Versionen meldet Obtainium von selbst.

Ohne Obtainium: unter [Releases](https://github.com/boerni328-bene/Tabletten-Checker/releases) die neueste Datei `tabletten-checker-<Version>.apk` herunterladen und öffnen.

## Echtheit prüfen

Jedes Release nennt die Prüfsumme (SHA-256) der APK und den Fingerabdruck des Signaturzertifikats. Der Fingerabdruck ist bei jeder Version derselbe:

`0A:B2:69:3A:87:FF:8B:5D:A9:86:F5:12:FB:9A:D6:17:F2:A0:91:12:46:04:E1:67:32:2F:60:63:7D:0C:2C:E1`

## Entwicklung

Gebaut wird ausschließlich über GitHub Actions (`.github/workflows/build.yml`): Prüfung (Lint), Tests, signierte APK, Release. Änderungen stehen in [CHANGELOG.md](CHANGELOG.md).
