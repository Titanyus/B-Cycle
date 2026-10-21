# B|CYCLE 0.8.18 – Build 57

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.18-release-57.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.18-release-57.apk.sha256)
- [GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/39560711776)
- Quellstand: `9e3a29cc5c0475e2bee8046ba5df2c75368f858d`
- Version 0.8.18, versionCode 88, Android 13+, Build vom 21.10.2026.

- Größe: **2.098.575 Bytes (2,10 MB)**.
- APK-SHA-256: `a3f4772a63f067248f2554b508db54870a7989c73e6e8a23b730b7b64dca63b9`
- Zertifikat-SHA-256: `94D34C46EDE28D12E6D37EFD8A7D953A75B621596EC2E5B68F61899B8EA7499B`

Optimierter Release mit R8 und Ressourcenverkleinerung, über GitHub Actions gebaut. Download-Prüfsumme stimmt mit CI überein, apksigner bestätigt den dauerhaften Schlüssel, aapt bestätigt Version 0.8.18/88 ohne Debug-/Test-Flag, zipalign bestätigt die Ausrichtung. Das separate R8-Mapping ist im Actions-Artefakt enthalten, nicht in der APK. Update über gleich signierte Builds 53/55/56 möglich. APK direkt veröffentlicht, kein ZIP-Download erforderlich.

## Prüfungen

Vor dem Release 67 unterschiedliche gezielte Tests zu Graphen, Trend-Chevrons, Fahrtnamen, Profilen und Backup erfolgreich. Dazu 9 zuvor erfolgreiche gezielte Tests für kompakte Dreeve-Zeilen, Fahrradkacheln und Pulsmesser-Wiederverwendung sowie der erfolgreiche UI-Test zur erkennbaren Sensorsuch-Sperre während Aufnahme/Speichern. Kein vollständiger Gesamttestlauf.

Drei bekannte ältere UI-Testfehler sind im [vorherigen Buildnachweis](build-56.md) dokumentiert; nicht im Rahmen dieses Releases korrigiert. Echte Sensor-/Telefonprüfungen, insbesondere Pulsmesser-Wiederverwendung am H10, bleiben offen.

[Änderungen](release-0.8.18.md) · [Übergabe](CONTINUE-HERE.md).
