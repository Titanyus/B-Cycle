# B|CYCLE 0.8.20 – Build 59

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.20-release-59.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.20-release-59.apk.sha256)
- [GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/39598197014)
- Quellstand: `4b696c209d888ea8552e3758b6718c2e99f7a636`
- Version 0.8.20, versionCode 90, Android 13+.

- Build vom 21.10.2026, Größe: **2.107.835 Bytes (2,11 MB)**.
- APK-SHA-256: `52204fb6cb6b23470656ac8868710abf83ac03d38cf02bad7782d180f968bafc`
- Zertifikat-SHA-256: `94D34C46EDE28D12E6D37EFD8A7D953A75B621596EC2E5B68F61899B8EA7499B`

Optimierter Release mit R8/Ressourcenverkleinerung über GitHub Actions. APK-Prüfsumme mit CI abgeglichen, apksigner bestätigt dieselbe dauerhafte Signatur wie Build 58, aapt bestätigt 0.8.20/90 ohne Debug-/Test-Flag und zipalign die Ausrichtung. Mapping separat im Actions-Artefakt, nicht in der APK. Update über gleich signierte Vorgänger möglich; direkter APK-Download ohne ZIP.

## Prüfungen

Schritt 4 separat in `069f840` geprüft und festgehalten: 48 gezielte Testausführungen; nach Entfernung der letzten Backup-Adapter-Abhängigkeit Katalog-/Supplement-Tests nochmals erfolgreich. Schritt 5 separat in `4b696c2`.

Abschließend 105 gezielte Testausführungen erfolgreich auf Version 0.8.20: Feldkatalog-/Labelreferenz, bytegleiche Altjournal-Wiedererzeugung, FIT-Records/Summaries/Developer-Felddefinitionen gegen synthetische Referenz, Messwertverarbeitung ohne Android/Dashboard und unter verschiedenen Zahlensprachen, LiveData-/Dashboard-Darstellung, Indoor, FIT-/Performance-Journal, Backup/Profil, Fahrtnamen sowie Aufnahme und Foreground-Service auf SDK 33/35/36. Frühere Prüfungen der Schritte 1–3 siehe [Build 58](build-58.md).

Kein vollständiger Gesamttestlauf. Bekannte ältere UI-Testfehler siehe [Build 56](build-56.md). Physische Sensor-/Telefonprüfung bleibt ergänzend offen. Keine neuen Abhängigkeiten oder Änderungen an Datenformaten, Sensorverbindungen, Quellenwahl und Leistungsalgorithmen.

[Änderungen](release-0.8.20.md) · [Technische Beschreibung](recording-fields.md) · [Übergabe](CONTINUE-HERE.md).
