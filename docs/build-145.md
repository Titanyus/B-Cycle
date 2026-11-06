# B|CYCLE 0.8.100 – Build145

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.100-release-145.apk)
- [SHA-256](../downloads/B-Cycle-0.8.100-release-145.apk.sha256)
- [Erfolgreicher Actions-Build](https://github.com/Titanyus/B-Cycle/actions/runs/42270699855)
- Quellstand: `191aacfa51a384f3109fa9ff84dd44c382bde90d`
- Version0.8.100, versionCode170, Android13+.
- Größe: 4369907 Bytes (4,37MB).
- SHA-256: `7314fce2d8d7602a4a0969888e2c4139e010046d2f1ccb19320363e0ef071ebd`.
- Zertifikat SHA-256: `94d34c46ede28d12e6d37efd8a7d953a75b621596ec2e5b68f61899b8ea7499b`.

Release ausschließlich aus GitHub Actions. CI-Prüfsumme, dauerhafte Signatur, Version und Manifest ohne Debug-/Test-Flags geprüft. ZIP-/16KB-Ausrichtung und ARM64-ELF-LOAD-Segmente geprüft; vier VTM-ABIs und28Shader vorhanden. Original-Bright-JSON, generiertes XML und113Sprites bytegleich zu Quellen. Terrain-/Cycling-Attribution erhalten. R8-Mapping für GLMatrix/MVT, Trainingsleser/Speicher und neue Trainingsdetails/-kennzahlen/-sortierung sowie zugehörige Ressourcen geprüft.

[Änderungen](release-0.8.100.md): native Intervals-Rampen statt verlustbehaftetem FIT-Umweg, vollständige Originaldaten in Trainingsdetails, Volltondiagramme mit Detailachsen, automatische Leistungstags, berechnete Kennzahlen und Streckenmanager-artige Trainingsliste mit Sortierung/Gruppenzählung. Früher importierte Intervals-Vorlagen einmal erneut abrufen, damit Rampen und Originaldaten ergänzt werden. Varianten Q+/Q/S/E/E+ und nachträgliche Intervallerkennung nur vorgemerkt.

Gezielte lokale Tests grün: training-original-data-final-tests.log, training-intensity-tests.log, training-chart-axes-tests.log, training-metrics-tests.log, training-sort-tests.log. Import/Rampen/Diagrammpixel, Originaldaten/Pagination, Kennzahlen/analytische Rampen, vier Kriterien/beide Richtungen/fehlende Werte, fünf Kacheln in beiden Designs und Listen-/Pinned-/Auswahl-/Detailbedienung geprüft. Typografie/UI-Katalog/Bright/Diff grün. Kein Gesamtlauf und keine lokale APK. Optischer Gerätevergleich und erneuter Abruf im persönlichen Intervals-Konto offen. [Umfang/Formeln](training-library.md).
