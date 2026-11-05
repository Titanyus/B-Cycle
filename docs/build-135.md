# B|CYCLE 0.8.93 – Build 135

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.93-release-135.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.93-release-135.apk.sha256)
- [GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/41976148282)
- Quellstand: `af4191a9982b39a57fbc0703d281eee812615037`
- Version 0.8.93, versionCode 163, Android 13+.
- Größe: 4261284 Bytes (4,26 MB).
- SHA-256: `dac5119f9abd1a3b795f85d2f588c36ae46f9b3d242dd2acff3eecb656ec43e1`.
- Signierzertifikat SHA-256: `94d34c46ede28d12e6d37efd8a7d953a75b621596ec2e5b68f61899b8ea7499b`.

Optimierter Release ausschließlich aus GitHub Actions. CI-Prüfsumme, dauerhafte Update-Signatur, Version und Manifest ohne Debug-/Test-Flags geprüft. ZIP-/16-KB-Ausrichtung und ARM64-ELF-LOAD-Segmente geprüft; vier VTM-ABIs, 28 Shader und erhaltene GLMatrix-/MVT-Protobuf-Namen nach R8.

Bright-JSON/XML, drei eigene Shader, 111 Spritevarianten und Atlas bytegenau geprüft; Referenzfarben/Dash-Regel und fehlende Extrusionen erhalten. Terrain-Hinweise bytegenau, bisherige Editor-/Ebenen-/Fitness-/Aufzeichnungs-Texte sowie RGBA-Bezeichnungen geprüft. Live-Farbthema, Editor-Popup, Clipping-Datenpfad und bisherige Label-/Editor-/Fitness-/Passklassen im R8-Mapping vorhanden.

[Änderungen](release-0.8.93.md): FLAT-Sticky-Header SECONDARY; kompakter Editor mit Alpha aus Originalstil, Farbfeld in der Objektauswahlzeile, vier Iconaktionen nebeneinander, zuletzt gewähltes Objekt wiederherstellen.

Acht gezielte Fälle in MapStyleEditorTest/VtmLiveColorsTest/BicycleSectionCardsTest erfolgreich; darunter native Alpha-/Reset-/Textur-/Queuefälle, JSON-/Profil-/Sicherungsrundreise, Objektwiederherstellung und SECONDARY über alle Farbprofile. Typografie/UI-Katalog/Bright-/Diff-Prüfung erfolgreich. Kein Gesamtlauf, keine lokale APK. Optische Geräteprüfung offen.
