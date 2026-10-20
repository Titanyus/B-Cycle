# B|CYCLE 0.8.15 – Build 53

Build vom 20.10.2026, versionCode 85, Android 13+.

- [Erfolgreicher Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/39423155545)
- [Artefakt B-Cycle-release-53 herunterladen](https://github.com/Titanyus/B-Cycle/actions/runs/39423155545/artifacts/12264852640)
- Quellstand: `5a83cf2aeef2d5c8d6fa04204906a2b38f78e7bf`
- Datei: `B-Cycle-0.8.15-release-53.apk`
- APK-SHA-256: `6ec9c28d49d298d051423885c84c013fa52ecbf4df2225f984a8ce597a931cd5`
- Zertifikat-SHA-256: `94D34C46EDE28D12E6D37EFD8A7D953A75B621596EC2E5B68F61899B8EA7499B`

Das Actions-ZIP enthält die optimierte Release-APK, Prüfsumme und R8-Mapping.
Zum Artefakt-Download ist eine GitHub-Anmeldung erforderlich; Aufbewahrung 30 Tage.

## Dauerhafte Signierung

Das Actions-Secret `ANDROID_DEBUG_KEYSTORE_BASE64` funktioniert: Die heruntergeladene
APK wurde mit `apksigner verify --print-certs` geprüft und ihr Zertifikat stimmt
exakt mit dem vorbereiteten dauerhaften Schlüssel überein. Künftige Builds mit
diesem Secret können dieselbe Signatur verwenden.

Der neue Schlüssel ersetzt keinen verlorenen alten Schlüssel. Build 53 kann ältere,
anders signierte Installationen einschließlich Build 52 nicht als Update ersetzen.
Bestehende Appdaten vor einem Installationswechsel sichern; eine Deinstallation
löscht die privaten Appdaten.

## Funktionsstand und Prüfung

Die [Änderungen in 0.8.15](release-0.8.15.md) umfassen Trend-Schalter und einzelne
Feldauswahl, kleinere Chevrons, die Dreeve-Listenaktualisierung und die Streckenauswahl.
Die zu diesem Funktionsstand dokumentierten 73 gezielten Tests bestanden; Build 53
hat keine zusätzliche Testsuite ausgeführt. Die neue Fahrten-Verzeichnissicherung
im lokalen Entwicklungsstand ist in dieser APK noch nicht enthalten.
Physisches Gerät und echte Cloudkonten benötigen weiterhin praktische Prüfung.
