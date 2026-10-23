# B|CYCLE 0.8.34 – Build 73

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.34-release-73.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.34-release-73.apk.sha256)
- [GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/40027143982)
- Quellstand: `ca2d5ea2e67baa6291653f35434937a9d4429fb6`
- Version 0.8.34, versionCode 104, Android 13+.
- Build vom 23.10.2026, Größe **2.369.837 Bytes (2,37 MB)**.
- APK-SHA-256: `68cf3311fd7c9706055add289bf301a8a6bb54fe02d014712f774c205b2e0d13`
- Zertifikat-SHA-256: `94D34C46EDE28D12E6D37EFD8A7D953A75B621596EC2E5B68F61899B8EA7499B`

Optimierter Release über GitHub Actions mit R8 und Ressourcenverkleinerung. Prüfsumme mit CI abgeglichen; apksigner bestätigt denselben dauerhaften Schlüssel wie Build 72, aapt Version 0.8.34/104 ohne Debug-/Test-Flag, zipalign die Ausrichtung. Mapping separat im Actions-Artefakt, nicht in der APK. Direkter APK-Download ohne ZIP. Als Update über gleich signierte Vorgänger seit Build 53 installierbar.

Enthält alle seit Build 72 umgesetzten Änderungen: HRV-Einstiegsdialog und manueller Langdruck-Einstieg, Dreeve-Bestätigung/Namensabruf und lang gedrückte Upload-Neuprüfung, flexible Aufzeichnungsquellen/Smartphone-Barometer, Aufnahme-Autostart, Graphglättung/weiche Rückskalierung, Abschluss-Detailsbutton/Zehnsekundengrenze, dauerhafte Akkuanzeige und korrigierte Bosch-Akkufrische sowie Windpfeile. [Änderungen](release-0.8.34.md).

Gezielte Tests in den Umsetzungsschritten erfolgreich; native HRV-Vorschau visuell geprüft, Typografie-/Atlasprüfung erfolgreich. Kein Gesamtlauf, Hardware-/Cloudkontentest oder lokaler APK-Build. [Vorheriger Build](build-72.md).
