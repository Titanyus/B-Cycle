# B|CYCLE 0.8.21 – Build 60

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.21-release-60.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.21-release-60.apk.sha256)
- [GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/39603707593)
- Quellstand: `8b29f37ef3a3ac36a9601dbe87498e6177d692bd`
- Version 0.8.21, versionCode 91, Android 13+.

- Build vom 21.10.2026, Größe: **2.108.343 Bytes (2,11 MB)**.
- APK-SHA-256: `ee4514500142449e4ebd80ecbe9cb491bde8fdf370224232f88bd9d7b270ba8c`
- Zertifikat-SHA-256: `94D34C46EDE28D12E6D37EFD8A7D953A75B621596EC2E5B68F61899B8EA7499B`

Optimierter Release über GitHub Actions mit R8 und Ressourcenverkleinerung. Prüfsumme stimmt mit CI überein, apksigner bestätigt den dauerhaften Schlüssel wie Build 59, aapt die Version 0.8.21/91 ohne Debug-/Test-Flag, zipalign die Ausrichtung. Mapping separat im Actions-Artefakt, nicht in der APK. Als Update über gleich signierte Vorgänger installierbar. Direkt als APK veröffentlicht, kein ZIP-Download erforderlich.

## Prüfungen

Optikkorrekturen vor Release mit 11 gezielten UI-Tests erfolgreich geprüft: vier SelectionSpacing-Tests, sechs bestehende ContentSpacing-Tests und ein bestehender RideNaming-Profil-/Mehrfachauswahltest. Native Pixelprüfung für 2-dp-Iconabstand und Rahmen, sieben tatsächliche Einstellungsseiten, nachträglich eingefügte Formulare und bündige Sticky-Header, bestehende Profil-/Strecken-/Detailabstände. Fahrtnamensansicht nativ gerendert und visuell kontrolliert.

Keine Fachlogik-, Datenformat- oder Abhängigkeitsänderungen. Kein vollständiger Gesamttestlauf; bekannte ältere UI-Testfehler und offene Geräteprüfungen sind in früheren Buildnachweisen dokumentiert. [Vorheriger Build](build-59.md).

[Änderungen](release-0.8.21.md) · [UI-Konventionen](ui-conventions-audit.md).
