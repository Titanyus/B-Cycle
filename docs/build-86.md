# B|CYCLE 0.8.46 – Build 86

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.46-release-86.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.46-release-86.apk.sha256)
- [GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/40214439942)
- Quellstand: `39f328cc3e143bd1624a1aa57bd53ba51e953a08`
- Version: 0.8.46, versionCode116, Android13+.
- Größe: 2.268.321 Bytes (2,27 MB).
- SHA-256: `b9723c59c692f3ba26a12bf119c0c2f4f32b0baa4ef7f4dfcd1f7ac934e31570`
- Signierzertifikat SHA-256: `94d34c46ede28d12e6d37efd8a7d953a75b621596ec2e5b68f61899b8ea7499b`.

Optimierter Release aus GitHub Actions mit R8 und Ressourcenverkleinerung. CI-Prüfsumme, dauerhafte Signatur wie Build85, Version/Manifest ohne Debug-/Test-Flag und ZIP-Ausrichtung geprüft. Direkter APK-Download ohne ZIP; Update über gleich signierte Vorgänger seit Build53 möglich.

Neues Original-App-Logo aus BClogo3.zip. Glow-Sticky-Karten wieder nahtlos mit dynamisch passendem unteren Kartenbogen; keine deckende Maske mehr im8dp-Abstand unter Hauptheader. Radprofil-Kacheln mit32dp Abstand zur ersten Karte. HRV-Trendbeschriftungen immer mittig über allen vorhandenen Balkenwerten, ohne Unterdrückung bei engem Zoom; Überlappung möglich. [Alle Änderungen](release-0.8.46.md).

17 gezielte Tests für HRV-Trend/Zoom/Pan, Sticky-Push/Glow-/Flat-Karten, Pixelprüfung des unteren Bogens/transparenten Streifens und Headerschatten erfolgreich. Zusätzlich zwei gezielte Tests nach Profilabstand-Korrektur erfolgreich. Ressourcen-Kompilierung, intakte/byteidentische Original-PNGs, XML-Verweise sowie Typografie-/Diffprüfung erfolgreich. Kein vollständiger Gesamtlauf und kein Gerätetest.

[Vorheriger Download](build-85.md).
