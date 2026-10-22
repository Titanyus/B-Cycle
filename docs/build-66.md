# B|CYCLE 0.8.27 – Build 66

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.27-release-66.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.27-release-66.apk.sha256)
- [GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/39821944070)
- Quellstand: `aea6b1ec24ba509ba1e85ffd954c84ec43917a2b`
- Version 0.8.27, versionCode 97, Android 13+.
- Build vom 22.10.2026, Größe **2.221.754 Bytes (2,22 MB)**.
- APK-SHA-256: `44843f32ca2fa8ead0379890629a59ddb2669ff25f90c891823ccc66a35f35a1`
- Zertifikat-SHA-256: `94D34C46EDE28D12E6D37EFD8A7D953A75B621596EC2E5B68F61899B8EA7499B`

Optimierter Release über GitHub Actions mit R8 und Ressourcenverkleinerung. Prüfsumme mit CI abgeglichen; apksigner bestätigt denselben dauerhaften Schlüssel wie Build 65, aapt Version 0.8.27/97 ohne Debug-/Test-Flag, zipalign die Ausrichtung. Mapping separat im Actions-Artefakt, nicht in der APK. Direkter APK-Download ohne ZIP. Als Update über gleich signierte Vorgänger seit Build 53 installierbar.

Enthalten: elf Tagesbefinden-Stufen 0–10, gerundete HRV4-Antworten, Ergänzung fehlender Antworten vorhandener Importe beim Öffnen und Skalenmigration mit erhaltenen Originalwerten. Normalbereich weiß mit 40 % Deckkraft und gepunkteten 1-dp-Grenzen. [Änderungen](release-0.8.27.md) · [Antwortdaten und Migration](hrv-questionnaire.md).

21 gezielte Tests erfolgreich; Korridor nativ visuell geprüft. Typografie und generierter UI-Atlas konsistent. Kein Gesamttestlauf, Hardwaretest oder lokaler APK-Build. HRV-Messalgorithmus und Ergebnisdateien unverändert; Fragebogen-Schema 2 mit Lesekompatibilität zu Schema 1. [Vorheriger Build](build-65.md).
