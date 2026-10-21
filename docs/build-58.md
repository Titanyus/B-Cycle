# B|CYCLE 0.8.19 – Build 58

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.19-release-58.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.19-release-58.apk.sha256)
- [GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/39589515288)
- Quellstand: `265f3f0a1d16bd0baf0459c2b52d7e16173a790e`
- Version 0.8.19, versionCode 89, Android 13+.

- Build vom 21.10.2026, Größe: **2.104.847 Bytes (2,10 MB)**.
- APK-SHA-256: `b96af0ac19d47e2865bca89745adf29a949b7286d26a9db76df452928bd146b8`
- Zertifikat-SHA-256: `94D34C46EDE28D12E6D37EFD8A7D953A75B621596EC2E5B68F61899B8EA7499B`

Optimierter Release über GitHub Actions mit R8 und Ressourcenverkleinerung. apksigner bestätigt den dauerhaften Schlüssel wie Builds 53/55/56/57, SHA-256 stimmt mit CI überein, aapt bestätigt Version 0.8.19/89 ohne Debug-/Test-Flag und zipalign die Ausrichtung. R8-Mapping separat im Actions-Artefakt, nicht in der APK. Update über gleich signierte Vorgänger möglich; direkter APK-Download ohne ZIP.

## Prüfungen

Schritt 3 vor Release mit 62 gezielten Testausführungen erfolgreich geprüft: neue Runtime-/Activity-Tests für Speichersperren, Profilwechsel, Bibliotheksaktualisierung, lange Rückmeldungen und Unterdrückung identischer Toasts; bestehende RecordingFlow-Tests auf SDK 33/35/36 einschließlich Foreground-Service/Hintergrundaufnahme; Referenzkatalog/Altjournal, Speicherfehler, Teilerfolg/Retry, typisierte Ergebnisse, Fahrtnamen, intervals.icu, Indoor-Runtime und Sensorsuch-Sperren.

Die vorangehenden Architektur-Schritte 1/2 wurden vor Produktionsänderung mit 36 Referenz-/Flow-Tests und nach Umstellung mit 72 gezielten Tests geprüft. Diese Version enthält alle drei Schritte. Kein vollständiger Gesamttestlauf; bekannte ältere UI-Testfehler siehe [Build 56](build-56.md). Physische Telefon-/Sensorprüfungen bleiben zusätzlich zu Hosttests erforderlich.

Feldkatalog- und weitere Messdaten-/Dashboard-Entkopplung sind noch offen. Journal-/FIT-Formate, Aufzeichnungsfelder, Profile und Dienstwarteschlangen wurden nicht verändert.

[Änderungen](release-0.8.19.md) · [Technische Beschreibung](recording-results.md) · [Übergabe](CONTINUE-HERE.md).
