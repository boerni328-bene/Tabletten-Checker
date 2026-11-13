#!/usr/bin/env bash
# Erzeugt die Beschreibung eines GitHub-Releases:
# Kernsatz, Abschnitt der Version aus CHANGELOG.md, Hinweis zur Installation und Angaben zur Echtheit.
# Aufruf: release-text.sh <versionName> <apk-datei>   (die APK darf fehlen, dann ohne Echtheitsangaben)
set -euo pipefail

VERSION="$1"
APK="$2"
KERNSATZ="Tabletten-Checker behält den Überblick über den Tablettenvorrat: Bestand, Reichweite und Nachkauf, lokal gespeichert, ohne Cloud."

# Abschnitt dieser Version aus CHANGELOG.md herausschneiden (bis zur nächsten Überschrift „## [“).
ABSCHNITT=$(awk -v v="$VERSION" '
  /^## \[/ { if (found) exit; if (index($0, "## [" v "]") == 1) { found = 1; next } }
  found { print }
' CHANGELOG.md | sed '/./,$!d')
if [ -z "${ABSCHNITT//[[:space:]]/}" ]; then ABSCHNITT="Version $VERSION"; fi

echo "_${KERNSATZ}_"
echo
echo "$ABSCHNITT"
echo
echo "### Installation"
echo
echo "Am einfachsten über Obtainium (Adresse des Repositorys hinzufügen), sonst die Datei \`tabletten-checker-$VERSION.apk\` herunterladen und auf dem Gerät öffnen (ab Android 8.0)."
echo "Ein Update wird einfach über die vorhandene App installiert, die Tablettenliste bleibt dabei erhalten."

if [ -f "$APK" ]; then
  PRUEFSUMME=$(sha256sum "$APK" | cut -d' ' -f1)
  APKSIGNER="$(ls -d "$ANDROID_HOME"/build-tools/*/ | sort -V | tail -n 1)apksigner"
  FINGERABDRUCK=$("$APKSIGNER" verify --print-certs "$APK" 2>&1 \
    | grep -oiE 'certificate SHA-256 digest: *[0-9a-f]{64}' | grep -oiE '[0-9a-f]{64}' | head -n 1 \
    | tr 'a-f' 'A-F' | sed 's/../&:/g; s/:$//')
  echo
  echo "### Echtheit prüfen"
  echo
  echo "- Prüfsumme (SHA-256) der APK: \`$PRUEFSUMME\`"
  echo "- Fingerabdruck des Signaturzertifikats (SHA-256): \`$FINGERABDRUCK\`"
fi
