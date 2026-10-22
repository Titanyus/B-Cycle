# B|CYCLE 0.8.30 – Build 69

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.30-release-69.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.30-release-69.apk.sha256)
- [GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/39834989340)
- Quellstand: `def820992a58bf09485dadcad38d31cbe695c8d7`
- Version 0.8.30, versionCode 100, Android 13+.
- Build vom 22.10.2026, Größe **2.228.390 Bytes (2,23 MB)**.
- APK-SHA-256: `a2471bba1429267794ad11b68eb1a870d0f835b9565744472b1909fdf9b957d2`
- Zertifikat-SHA-256: `94D34C46EDE28D12E6D37EFD8A7D953A75B621596EC2E5B68F61899B8EA7499B`

Optimierter Release über GitHub Actions mit R8 und Ressourcenverkleinerung. Prüfsumme mit CI abgeglichen; apksigner bestätigt denselben dauerhaften Schlüssel wie Build 68, aapt Version 0.8.30/100 ohne Debug-/Test-Flag, zipalign die Ausrichtung. Mapping separat im Actions-Artefakt, nicht in der APK. Direkter APK-Download ohne ZIP. Als Update über gleich signierte Vorgänger seit Build 53 installierbar.

Enthalten: bestätigtes Löschen von HRV-Messungen mit erneutem CSV-Import, überarbeiteter Messbildschirm sowie kleine Tageswertbeschriftungen und kontinuierliches Rand-Clipping im HRV-Diagramm. [Änderungen](release-0.8.30.md).

Sechs gezielte Tests erfolgreich: drei für Löschen/Wiederimport/Messbildschirm sowie zuvor drei für Diagrammbeschriftung/Gesten/Rand-Clipping. Native Vorschauen visuell geprüft, Typografie-/Atlasprüfung erfolgreich. Kein Gesamttestlauf, Hardwaretest oder lokaler APK-Build. Messalgorithmus und bestehende Datenformate unverändert. [Vorheriger Build](build-68.md).
