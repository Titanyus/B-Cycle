# B|CYCLE 0.8.25 – Build 64

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.25-release-64.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.25-release-64.apk.sha256)
- [GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/39763256170)
- Quellstand: `f9e8e5ab67f12e86eb755f048909c52fd2055026`
- Version 0.8.25, versionCode 95, Android 13+.
- Build vom 22.10.2026, Größe **2.201.366 Bytes (2,20 MB)**.
- APK-SHA-256: `3419ada2ea11cf1158cf57357ff674e7141e0b702404edbd11b63da36e8a0dbc`
- Zertifikat-SHA-256: `94D34C46EDE28D12E6D37EFD8A7D953A75B621596EC2E5B68F61899B8EA7499B`

Optimierter Release über GitHub Actions mit R8 und Ressourcenverkleinerung. Prüfsumme mit CI abgeglichen; apksigner bestätigt denselben dauerhaften Schlüssel wie Build 63, aapt Version 0.8.25/95 ohne Debug-/Test-Flag, zipalign die Ausrichtung. Mapping separat im Actions-Artefakt, nicht in der APK. Direkter APK-Download ohne ZIP. Als Update über gleich signierte Vorgänger seit Build 53 installierbar.

Enthalten: HRV4Training-CSV-Import mit Vorschau und Dublettenschutz, letzte HRV-Messung pro lokalem Tag ohne Archivverlust und ergänzte Tagesbefinden-Abstände/Platzhalter. Kein neuer Score und keine allgemeine Altersformel. [Änderungen](release-0.8.25.md).

16 gezielte HRV-Regressionstests erfolgreich. Import und Tagesbefinden-Abstände zuvor separat gezielt geprüft; Typografie und generierter UI-Atlas konsistent. Kein vollständiger Gesamttestlauf, Hardwaretest oder lokaler APK-Build. [Vorheriger Build](build-63.md).
