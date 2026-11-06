# B|CYCLE 0.8.99 – Build144

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.99-release-144.apk)
- [SHA-256](../downloads/B-Cycle-0.8.99-release-144.apk.sha256)
- [Erfolgreicher Actions-Build](https://github.com/Titanyus/B-Cycle/actions/runs/42232122452)
- Quellstand: `870a95a5f9c1631ee21b10c39296014343641e4a`
- Version0.8.99, versionCode169, Android13+.
- Größe: 4334347 Bytes (4,33MB).
- SHA-256: `5ae2e91a60e6b7257938f941eaaeffb58b3aac055d7e8696b401ed4557542c84`.
- Zertifikat SHA-256: `94d34c46ede28d12e6d37efd8a7d953a75b621596ec2e5b68f61899b8ea7499b`.

Release ausschließlich aus GitHub Actions. CI-Prüfsumme, dauerhafte Signatur, Version und Manifest ohne Debug-/Test-Flags geprüft. ZIP-/16KB-Ausrichtung und ARM64-ELF-LOAD-Segmente geprüft; vier VTM-ABIs und28Shader vorhanden. Original-Bright-JSON unverändert, generiertes XML einschließlich schmaler gerader Flugplatzlinien und113Sprites bytegleich zu Quellen. Terrain-/Cycling-Attribution erhalten. R8-Namen für GLMatrix/MVT, Trainingsoberfläche und zentrale Kartenpfade geprüft; alle drei Trainingsleser sowie Speicher-/Mergepfade im R8-Mapping nachgewiesen (Import-/Store-Klassen durch R8 zusammengeführt); Trainingsimporttexte im APK enthalten.

[Änderungen](release-0.8.99.md): eigene Trainingssammlung mit Intervals-Bibliotheksabruf und FIT/ERG/ZWO-Import, Karten/Stufendiagrammen/Sticky-Gruppen, Import-/Löschfooter und vorerst deaktivierter Sortierung/Filterung; gemeinsame Importframes; maßhaltige Startbahnflächen und schmale gerade Mittellinien.

Gezielte lokale Prüfungen grün: Trainingsformate, API-Testserver ohne Kalender-/Bibliothekswrites, Atomizität/Dubletten/Löschen/Abbruch, reale Menü-/Datei-/Footer-Navigation, native Diagramme und Sticky-Klickschutz; aktualisierte Einstellungsnavigation; vier Originalflugplatz-MVTs mit nativen Paintregeln13..21, dazu beide Overzoom-Geometriefälle. Logs training-collection-final-tests.log (Trainingsfälle grün, alter Menüindex danach angepasst), training-sticky-and-menu-final-tests.log und airfield-footprints-final-tests.log. Zentrale Typografie/UI-Katalog/Bright/Diff grün. Kein Gesamtlauf und keine lokale APK. Echter Intervals-Kontotest und optischer Gerätevergleich offen. Formatgrenzen in [training-library.md](training-library.md).
