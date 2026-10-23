# B|CYCLE 0.8.32 – Build 71

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.32-release-71.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.32-release-71.apk.sha256)
- [GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/39932771702)
- Quellstand: `fb87676a722869ec486902cff5141802d3bc17d3`
- Version 0.8.32, versionCode 102, Android 13+.
- Build vom 23.10.2026, Größe **2.294.624 Bytes (2,29 MB)**.
- APK-SHA-256: `26a99beaf37b046f95ae7a27e90362828f4a035739541ec2965b409e58cfa8e4`
- Zertifikat-SHA-256: `94D34C46EDE28D12E6D37EFD8A7D953A75B621596EC2E5B68F61899B8EA7499B`

Optimierter Release über GitHub Actions mit R8 und Ressourcenverkleinerung. Prüfsumme mit CI abgeglichen; apksigner bestätigt denselben dauerhaften Schlüssel wie Build 70, aapt Version 0.8.32/102 ohne Debug-/Test-Flag, zipalign die Ausrichtung. Mapping separat im Actions-Artefakt, nicht in der APK. Direkter APK-Download ohne ZIP. Als Update über gleich signierte Vorgänger seit Build 53 installierbar.

Enthalten: vollständige passwortgeschützte AES-256-Einstellungssicherung einschließlich Zugangsdaten/Fahrradbildern; behobene Lücken bei Ordner-/Kartenwerten und aktiver Profilprojektion; FIT-Metadaten als erstes Accordion unter Strecken-Aufzeichnung und Datenfeldauswahl dort als Schalter. [Änderungen](release-0.8.32.md).

36 gezielte Sicherungs-/Wiederherstellungstests plus drei gezielte Aufzeichnungs-UI-Tests in den Umsetzungsschritten erfolgreich. Typografie-/Atlasprüfung erfolgreich. Kein Gesamttestlauf, echter Geräte-/Cloudkontentest oder lokaler APK-Build. [Vorheriger Build](build-70.md).
