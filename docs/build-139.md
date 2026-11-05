# B|CYCLE 0.8.96 – Build 139

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.96-release-139.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.96-release-139.apk.sha256)
- [GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/42050272725)
- Quellstand: `9fab848d747bd0dad3afca805db501429d970bc3`
- Version 0.8.96, versionCode 166, Android 13+.
- Größe: 4279128 Bytes (4,28 MB).
- SHA-256: `32393bd51071b2bd15730ae46ab2d58600caf2e52ce5bd15e4decd07d36833f6`.
- Signierzertifikat SHA-256: `94d34c46ede28d12e6d37efd8a7d953a75b621596ec2e5b68f61899b8ea7499b`.

Optimierter Release ausschließlich aus GitHub Actions. CI-Prüfsumme, dauerhafte Update-Signatur, Version und Manifest ohne Debug-/Test-Flags geprüft. ZIP-/16-KB-Ausrichtung und ARM64-ELF-LOAD-Segmente geprüft; vier VTM-ABIs, 28 Shader und erhaltene GLMatrix-/MVT-Protobuf-Namen nach R8.

Bright-JSON/XML, drei eigene Shader, 111 Spritevarianten und Atlas bytegenau geprüft. Die neue Belagsregel entsteht zur Laufzeit; Referenz-Assets unverändert, keine Extrusionen. Terrain-/Cycling-Quellenhinweise bytegenau. Karten-/Designer-/Cycling-Texte einschließlich Belagshinweis und R8-Namen einschließlich Radroutenlayer/-datenpfad und zweizeiligem Caption-Layout geprüft.

[Änderungen](release-0.8.96.md): Straßen/Wege mit explizit unbefestigtem Belag gestrichelt; befestigt/fehlend/unbekannt solid. Auch die farbige Radwegehervorhebung und Umrandung folgen demselben offenen Muster. Designer-Hinweis und feste JSON-Regel, bestehende Farben/Alpha/Breiten erhalten.

Sechs gezielte Fälle erfolgreich: mehrere Straßen-/Wegeklassen und normale/Tunnel-/Brückenlagen, bekannte/fehlende Beläge, native Dash-/Solid-Varianten, Overlaymuster, Live-Farben/Reset, Breiten, Editor, Profil/Backup/Export und JSON. Typografie/UI-Katalog/Bright-/Diff-Prüfung erfolgreich. Kein Gesamtlauf und keine lokale APK. Optische Geräteprüfung offen; separate Radroutengeometrien ohne Belagsdaten bleiben durchgezogen/unbekannt.
