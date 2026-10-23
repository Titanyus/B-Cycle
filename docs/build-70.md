# B|CYCLE 0.8.31 – Build 70

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.31-release-70.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.31-release-70.apk.sha256)
- [GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/39925371710)
- Quellstand: `7d7c03987b1dacb8008f19ec6624c1c7efe90bf8`
- Version 0.8.31, versionCode 101, Android 13+.
- Build vom 23.10.2026, Größe **2.241.834 Bytes (2,24 MB)**.
- APK-SHA-256: `2e3f92dc2c887d4bb2c2910e1b8371f6b620c709c4e47845780175cbcf1cdb02`
- Zertifikat-SHA-256: `94D34C46EDE28D12E6D37EFD8A7D953A75B621596EC2E5B68F61899B8EA7499B`

Optimierter Release über GitHub Actions mit R8 und Ressourcenverkleinerung. Prüfsumme mit CI abgeglichen; apksigner bestätigt denselben dauerhaften Schlüssel wie Build 69, aapt Version 0.8.31/101 ohne Debug-/Test-Flag, zipalign die Ausrichtung. Mapping separat im Actions-Artefakt, nicht in der APK. Direkter APK-Download ohne ZIP. Als Update über gleich signierte Vorgänger seit Build 53 installierbar.

Enthalten: neue Fahrtenzusammenfassung mit aktuellen Upload-Belegen; optionale Garmin-FIT-Metadaten mit Profilwerten an den Schaltern; wiederkehrendes HRV-Tagesangebot bis zum Tap; Starten-Button und tieferer Kreisinhalt; Linien ohne Glow in Dashboard und Zonenverteilungen. [Änderungen](release-0.8.31.md).

31 gezielte Tests in den Umsetzungsschritten erfolgreich: elf für Glow/HRV-Tagesangebot, drei für Zusammenfassung/HRV-Rendering und 17 für FIT-Metadaten inklusive Garmin-Decoder/Aktivitätsvalidator, Sicherung und Profile. Native Vorschauen und Typografie-/Atlasprüfung erfolgreich. Kein Gesamttestlauf, Hardwaretest, Dreeve-Importnachweis oder lokaler APK-Build. Binäres Aufzeichnungsformat unverändert. [Vorheriger Build](build-69.md).
