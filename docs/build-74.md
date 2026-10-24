# B|CYCLE 0.8.35 – Build 74

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.35-release-74.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.35-release-74.apk.sha256)
- [GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/40077771431)
- Quellstand: `71ad3d96028544cce03b8f7af8ef89825e930695`
- Version 0.8.35, versionCode 105, Android 13+.
- Build vom 24.10.2026, Größe **2.387.097 Bytes (2,39 MB)**.
- APK-SHA-256: `925e4442f36eb9d77d5aeeb03a465dbb51f22896a739e22ef020e88635d0e4a1`
- Zertifikat-SHA-256: `94D34C46EDE28D12E6D37EFD8A7D953A75B621596EC2E5B68F61899B8EA7499B`

Optimierter Release über GitHub Actions mit R8 und Ressourcenverkleinerung. Prüfsumme mit CI abgeglichen; apksigner bestätigt denselben dauerhaften Schlüssel wie Build 73, aapt Version 0.8.35/105 ohne Debug-/Test-Flag, zipalign die Ausrichtung. Mapping separat im Actions-Artefakt, nicht in der APK. Direkter APK-Download ohne ZIP. Als Update über gleich signierte Vorgänger seit Build 53 installierbar.

Enthält alle seit Build 73 umgesetzten Änderungen: tägliches HRV-Tagesbefinden, feste Zeitmasken und Einheiten im Feld, 04:00-Uhr-Angebotstag, globale Auswahl-Schalter, stabile HRV-Balkenbeschriftung, neue Kontur-Blurs und aktive Flächenverläufe. Separater HTML-Designer im Repository. [Änderungen](release-0.8.35.md).

Gezielte Tests in den Umsetzungsschritten erfolgreich; native Fahrerprofil-/Rahmenvorschau visuell geprüft. Designer in Chromium bei Desktop-/Mobilbreite und über öffentlichen Vorschau-Link geprüft; Typografie-/Atlas-/Designergenerierung konsistent. Kein Gesamtlauf, Hardwaretest oder lokaler APK-Build. [Vorheriger Build](build-73.md).
