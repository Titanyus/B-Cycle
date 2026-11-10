# B|CYCLE 0.8.106 – Build151

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.106-release-151.apk)
- [SHA-256](../downloads/B-Cycle-0.8.106-release-151.apk.sha256)
- [Erfolgreicher Actions-Build](https://github.com/Titanyus/B-Cycle/actions/runs/42877730181)
- Quellstand: `631c960d173f0bde231dc5a531739a1f65ef6469`
- Version0.8.106, versionCode176, Android13+.
- Größe: 4445809 Bytes (4,45MB).
- SHA-256: `14c4737626c310f9afed8acab26462fae3fb05a8c6fb35bf88b03c0ca13f681b`.
- Zertifikat SHA-256: `94d34c46ede28d12e6d37efd8a7d953a75b621596ec2e5b68f61899b8ea7499b`.

Optimierter Release ausschließlich aus GitHub Actions. CI-Prüfsumme, dauerhafte Update-Signatur, Version, Manifest ohne Debug-/Test-Flags und ZIP-/16KB-Ausrichtung geprüft. ARM64-ELF-LOAD-Segmente passend, vier VTM-ABIs und28Shader vorhanden. Bright-JSON/XML/113Sprites bytegleich zum Quellstand; Terrain-/Cycling-Attribution erhalten. R8-Mapping enthält Stilbibliothek/Speicherbedienung und bisherige wesentliche Daten-/Kartenpfade; keine Headless-Testshadows.

[Änderungen](release-0.8.106.md): Kartenstil-Save/Cancel direkt sichtbar im Dialoginhalt statt ausgeblendeter nativer Standardbuttons. Bright als Vorlage/Überschreibziel, gespeicherte Bright-Werte in derselben Kartenquellenzeile laden. Alle bisherigen Stil-/Profil-/Backup-/JSON-Funktionen erhalten.

Fünf gezielte MapStyleLibraryTest-Fälle bestanden, einschließlich echter Sichtbarkeit und getGlobalVisibleRect-Fläche der Save-Aktion. Log map-style-save-visible-bright-tests.log. Typografie/UI-Katalog/Bright/Diff grün. Keine Gesamtsuite/Profiling oder lokaler APK-Build. [Ursache und Testkorrektur](vtm-map-editor.md); Bediengefühl am Gerät praktisch bestätigen. Frühere Gerätegrenzen unverändert.
