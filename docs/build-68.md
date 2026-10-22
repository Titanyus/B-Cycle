# B|CYCLE 0.8.29 – Build 68

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.29-release-68.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.29-release-68.apk.sha256)
- [GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/39828449690)
- Quellstand: `8fa9e77799b35e54ea4391e0672f69793c602072`
- Version 0.8.29, versionCode 99, Android 13+.
- Build vom 22.10.2026, Größe **2.224.554 Bytes (2,22 MB)**.
- APK-SHA-256: `b4e92d17c61b302cd06e6eeff663b4785f26482059a3c692bf807cd01ad7f8c9`
- Zertifikat-SHA-256: `94D34C46EDE28D12E6D37EFD8A7D953A75B621596EC2E5B68F61899B8EA7499B`

Optimierter Release über GitHub Actions mit R8 und Ressourcenverkleinerung. Prüfsumme mit CI abgeglichen; apksigner bestätigt denselben dauerhaften Schlüssel wie Build 66, aapt Version 0.8.29/99 ohne Debug-/Test-Flag, zipalign die Ausrichtung. Mapping separat im Actions-Artefakt, nicht in der APK. Direkter APK-Download ohne ZIP. Als Update über gleich signierte Vorgänger seit Build 53 installierbar.

Enthalten: kombinierte Aufsteh-/Schlafenszeit, neue optionale Schlafdauer und global engerer Abstand zwischen Auswahlpunkt und Text. Außerdem größeres HRV-Diagramm mit festen Y-Achsen, besser genutztem linken Rand und neuen Farben. Korridor weiß 30 %, Mittel weiß, Balken ACCENT und nur unter dem Normalbereich weiß. Gemeinsame persönliche Historie über App-/Protokollwechsel, Tagesregel erhalten. [Änderungen](release-0.8.29.md) · [Berechnung](hrv-trends.md).

15 gezielte Tests der letzten Schlaf-/Formularergänzungen erfolgreich; zuvor zehn Auswertungstests und native Darstellung geprüft. Typografie-/Atlasprüfung erfolgreich. Kein Gesamttestlauf, Hardwaretest oder lokaler APK-Build. Keine Extrapolation oder Wiederherstellung des konkret fehlenden Messwerts; HRV-Messalgorithmus und archivierte Originaldateien unverändert. [Vorheriger Build](build-66.md).
