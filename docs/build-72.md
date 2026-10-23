# B|CYCLE 0.8.33 – Build 72

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.33-release-72.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.33-release-72.apk.sha256)
- [GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/39945435222)
- Quellstand: `7ebcdd2d59053e681aaf1902f3aa9566a98ff792`
- Version 0.8.33, versionCode 103, Android 13+.
- Build vom 23.10.2026, Größe **2.294.768 Bytes (2,29 MB)**.
- APK-SHA-256: `4e141c619be489504c983f46d7b76eae3dc34989f3d53c5a7dbafe183e395db6`
- Zertifikat-SHA-256: `94D34C46EDE28D12E6D37EFD8A7D953A75B621596EC2E5B68F61899B8EA7499B`

Optimierter Release über GitHub Actions mit R8 und Ressourcenverkleinerung. Prüfsumme mit CI abgeglichen; apksigner bestätigt denselben dauerhaften Schlüssel wie Build 71, aapt Version 0.8.33/103 ohne Debug-/Test-Flag, zipalign die Ausrichtung. Mapping separat im Actions-Artefakt, nicht in der APK. Direkter APK-Download ohne ZIP. Als Update über gleich signierte Vorgänger seit Build 53 installierbar.

Enthalten: kalendergebundene Montagsbeschriftung mit Wochentrenner, sauberes Rand-Clipping in beiden HRV-Diagrammen und native Scrollträgheit in der HRV-Auswertung. Messdaten, feste Y-Achse und Statistik unverändert. [Änderungen](release-0.8.33.md).

Sieben gezielte native Tests erfolgreich, native Vorschau visuell geprüft und Typografie-/Atlasprüfung erfolgreich. Kein Gesamtlauf, Hardwaretest oder lokaler APK-Build. [Vorheriger Build](build-71.md).
