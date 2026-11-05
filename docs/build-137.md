# B|CYCLE 0.8.94 – Build 137

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.94-release-137.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.94-release-137.apk.sha256)
- [GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/42038005305)
- Quellstand: `eb3cf0ad148af07bb36ccba004d81fe85a9634a7`
- Version 0.8.94, versionCode 164, Android 13+.
- Größe: 4278364 Bytes (4,28 MB).
- SHA-256: `0f23fa0ef0a53a656b95e72f94ae26230bb3f81c26ed7e80db19443d3845bcd4`.
- Signierzertifikat SHA-256: `94d34c46ede28d12e6d37efd8a7d953a75b621596ec2e5b68f61899b8ea7499b`.

Optimierter Release ausschließlich aus GitHub Actions. CI-Prüfsumme, dauerhafte Update-Signatur, Version und Manifest ohne Debug-/Test-Flags geprüft. ZIP-/16-KB-Ausrichtung und ARM64-ELF-LOAD-Segmente geprüft; vier VTM-ABIs, 28 Shader und erhaltene GLMatrix-/MVT-Protobuf-Namen nach R8.

Bright-JSON/XML, drei eigene Shader, 111 Spritevarianten und Atlas bytegenau geprüft; Referenzfarben/Dash-Regel und fehlende Extrusionen erhalten. Terrain-Hinweise bytegenau, bisherige Editor-/Ebenen-/Fitness-/Aufzeichnungs-Texte sowie RGBA-Bezeichnungen geprüft. Live-Farbthema, Editor-Popup, Clipping-Datenpfad und bisherige Label-/Editor-/Fitness-/Passklassen im R8-Mapping vorhanden.

[Änderungen](release-0.8.94.md): Linienbreite25–300 %, eigene Feldwegdarstellung, dünnere Radwege-Übersicht ab Kameraminimum. Designer-Dropdown für vier Ortsklassen, Gewicht100–900 und Größe50–200 %, Vorgabe town/cityBlack900/90 %.

Drei gezielte Fälle in MapStyleEditorTest/VtmVectorMapTest erfolgreich; darunter native Feldwegfarbe/Breiten über mehrere Zooms, JSON-/Profil-/Sicherungsrundreise, Editor/Wiederöffnen und Radwege-Übersicht/Schalter/Stil. Ortsnamen-Schriftanpassung gezielt kompiliert. Zusätzlich gezielter MapPlaceTextEditorTest erfolgreich: alle vier Schriftklassen unabhängig, native Gewichte/Größen/Metriken/Prioritäten, Profil/Sicherung/JSON/Wiederöffnen/Reset/Validierung. Typografie/UI-Katalog/Bright-/Diff-Prüfung erfolgreich. Kein Gesamtlauf, keine lokale APK. Optische Geräteprüfung offen.
