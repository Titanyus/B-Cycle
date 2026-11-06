# B|CYCLE 0.8.98 – Build143

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.98-release-143.apk)
- [SHA-256](../downloads/B-Cycle-0.8.98-release-143.apk.sha256)
- [Erfolgreicher Actions-Build](https://github.com/Titanyus/B-Cycle/actions/runs/42210679835)
- Quellstand: `3219288a13295edd9d84332abb5f50ad45234f22`
- Version0.8.98, versionCode168, Android13+.
- Größe: 4298351 Bytes (4,30MB).
- SHA-256: `fd01c7837b133adfe24f22e4a67cc74f437654e80ee237000bf13aa8c55be46e`.
- Zertifikat SHA-256: `94d34c46ede28d12e6d37efd8a7d953a75b621596ec2e5b68f61899b8ea7499b`.

Release ausschließlich aus GitHub Actions. CI-Prüfsumme, dauerhafte Signatur, Version und Manifest ohne Debug-/Test-Flags geprüft. ZIP-/16KB-Ausrichtung und ARM64-ELF-LOAD-Segmente geprüft; alle vier VTM-ABIs und28Shader vorhanden. Original-Bright-JSON unverändert; generiertes XML bis21,113Sprites und eigene Shader bytegenau zu Quellen. Terrain-/Cycling-Attribution erhalten. R8-Namen für GLMatrix/MVT und zentrale Kartenpfade erhalten; kompakte Quellenanzeige und MapCredits im Artefakt geprüft.

[Änderungen](release-0.8.98.md): Basiskarte in höchsten Zoomstufen wieder sichtbar und kompakte, direkt aufrufbare Quellenanzeige mit zentraler vollständiger Quellenliste.

Gezielte Prüfungen: native Theme-Abdeckung für sechs Klassen19..21 und Geometrie15..21 (`bright-maxzoom-fix-tests.log`); Quellenanzeige bei120/200/600px, fünf aktive Quellen, Ebenenwechsel, vollständige Anbieter-/Quellenliste und bestehende Kartengeometrie (`compact-map-credits-tests.log`). Beide Umfänge erfolgreich; Typografie/UI-Katalog/Bright/Diff grün. Keine unnötigen Wiederholungsläufe und keine lokale APK. Optischer Gerätevergleich offen.

## Direkter Downloadnachweis

Direkten veröffentlichten Raw-Download tatsächlich abgerufen: 4298351 Bytes, bytegenau identisch mit dem geprüften Actions-Artefakt; SHA-256 fd01c7837b133adfe24f22e4a67cc74f437654e80ee237000bf13aa8c55be46e ebenfalls identisch.
