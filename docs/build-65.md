# B|CYCLE 0.8.26 – Build 65

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.26-release-65.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.26-release-65.apk.sha256)
- [GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/39783094587)
- Quellstand: `2275327ce05806d5c44875d13efad4547af8dcd5`
- Version 0.8.26, versionCode 96, Android 13+.
- Build vom 22.10.2026, Größe **2.219.870 Bytes (2,22 MB)**.
- APK-SHA-256: `40e220d8c4e08d8f362fbac3113144d6da53cb22fb748fc904c737b756540047`
- Zertifikat-SHA-256: `94D34C46EDE28D12E6D37EFD8A7D953A75B621596EC2E5B68F61899B8EA7499B`

Optimierter Release über GitHub Actions mit R8 und Ressourcenverkleinerung. Prüfsumme mit CI abgeglichen; apksigner bestätigt denselben dauerhaften Schlüssel wie Build 64, aapt Version 0.8.26/96 ohne Debug-/Test-Flag, zipalign die Ausrichtung. Mapping separat im Actions-Artefakt, nicht in der APK. Direkter APK-Download ohne ZIP. Als Update über gleich signierte Vorgänger seit Build 53 installierbar.

Enthalten: neuer HRV-Auswertungsbildschirm mit Kennzahlwahl, Tagesbalken, 7-Tage-Mittel und eigenem Normalbereich aus vorherigen 60 Tagen; Zweifinger-Zoom, horizontales Blättern und Tagesauswahl. Einstiege über HRV-Einstellungen und nach erfolgreichem Speichern des Tagesbefindens. CSV-Lebenszyklusfehler mit endloser Prüfanzeige korrigiert. [Änderungen](release-0.8.26.md) · [Berechnung](hrv-trends.md).

27 unterschiedliche gezielte Tests erfolgreich. Native Vorschau visuell geprüft, Typografie und generierter UI-Atlas konsistent. Kein vollständiger Gesamttestlauf, Hardwaretest oder lokaler APK-Build. Keine neue Score-/Altersformel oder Änderung der HRV-Messung/Archivformate. [Vorheriger Build](build-64.md).
