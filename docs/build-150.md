# B|CYCLE 0.8.105 – Build150

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.105-release-150.apk)
- [SHA-256](../downloads/B-Cycle-0.8.105-release-150.apk.sha256)
- [Erfolgreicher Actions-Build](https://github.com/Titanyus/B-Cycle/actions/runs/42801216387)
- Quellstand: `fe75f58d811834a576765532819e636c4751e5f6`
- Version0.8.105, versionCode175, Android13+.
- Größe: 4445809 Bytes (4,45MB).
- SHA-256: `e66e2a432d1d4898dc0a6b88bba97b806b3b5d753a3797f913809cc6fb53c163`.
- Zertifikat SHA-256: `94d34c46ede28d12e6d37efd8a7d953a75b621596ec2e5b68f61899b8ea7499b`.

Optimierter Release ausschließlich aus GitHub Actions. CI-Prüfsumme, dauerhafte Update-Signatur, Version, Manifest ohne Debug-/Test-Flags und ZIP-/16KB-Ausrichtung geprüft. ARM64-ELF-LOAD-Segmente passend, vier VTM-ABIs und28Shader vorhanden. Bright-JSON/XML/113Sprites bytegleich zum Quellstand; Terrain-/Cycling-Attribution erhalten. R8-Mapping enthält die neue Stilbibliothek/Speicherbedienung, GraphDemand und Gesamtachsenfelder sowie bisherige wesentliche Daten-/Kartenpfade; keine Headless-Testshadows.

[Änderungen](release-0.8.105.md): Erkennung abschließender Belastungen ohne letzte Erholung, Dashboard-Aufbereitung auf sichtbare/geänderte Inhalte begrenzt, interne benannte Kartenstile mit Überschreiben/Laden/Backup sowie synchroner Gesamtachsen-/Kurvenstand bei gemischten Zeitfenstern. Kurze Fenster1s normal/5s sparend, Gesamtverlauf10s in beiden Modi unverändert.

Gezielte Struktur-/Daten-/Status-/Dialog-/Cache-/Editor-/Backup-/native Graphprüfungen erfolgreich. Details und Testlogs in den Fachberichten der Release-Notizen. Zwei alte fachfremde Editor-Testannahmen beim ersten weiter gefassten Stil-Lauf dokumentiert; aktuelle Funktionsfälle gezielt grün. Zentrale Typografie/UI-Katalog/Bright/Diff-Prüfungen bestanden. Keine Gesamtsuite, kein Profiling und kein lokaler APK-Build. Gerätedarstellung/Gesten weiter praktisch bestätigen; vorherige Bosch-Ladeadressen benötigen weiterhin reale Bestätigung.
