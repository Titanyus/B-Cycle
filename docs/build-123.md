# B|CYCLE 0.8.82 – Build 123

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.82-release-123.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.82-release-123.apk.sha256)
- [Erfolgreicher GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/41472548976)
- Quellstand: `1730b8182c22e6931c919f6f1c97a7ad72eb9eea`
- Version 0.8.82, versionCode 152, Android 13+.
- Größe: 4075100 Bytes (4,08 MB).
- SHA-256: `298c19b2bd27f7a1c49f9755415a875908cb2320f5936d1b7b51d87f6a207a10`.
- Signierzertifikat SHA-256: `94d34c46ede28d12e6d37efd8a7d953a75b621596ec2e5b68f61899b8ea7499b`.

Optimierter Release ausschließlich aus GitHub Actions. CI-Prüfsumme, dauerhafte Update-Signatur, Version und Manifest ohne Debug-/Test-Flags geprüft. ZIP-/16-KB-Ausrichtung und ARM64-ELF-LOAD-Segmente geprüft. Vier VTM-ABIs, 27 Shader inklusive eigener GPS-/Streckenshader, Bright-Lizenz und OpenMapTiles-Ressourcen enthalten. Bright-XML und eigene Shader binär gegen Quelle verglichen; neue deckende Flächenfarben, flacher Stil ohne Extrusionen und erhaltene GLMatrix-/MVT-Protobuf-Namen nach R8 geprüft.

[Änderungen](release-0.8.82.md): erkennbare ländliche Flächenfarben, alte Karte im ausgehenden Dashboardbild, Trenner im Einstellungsmenü mit jeweils 12 dp Abstand.

Zwei betroffene Vektorkartentests, drei Karten-/Dashboard-Übergangsfälle und ein bestehender Menütest erfolgreich. Der neue Bildtest reproduzierte den fehlenden Karteninhalt vor der Korrektur. Typografie/Atlas/Diff erfolgreich; kein Gesamtlauf und keine lokale APK. Tatsächlichen PixelCopy-/Compositor-Übergang und Farben am Smartphone noch bestätigen.

Downloadnachweis: veröffentlichter direkter Raw-Link abgerufen, 4075100 Bytes, bytegenau identisch zum geprüften Actions-Artefakt; SHA-256 ebenfalls identisch.
