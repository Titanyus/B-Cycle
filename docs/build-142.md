# B|CYCLE 0.8.97 – Build142

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.97-release-142.apk)
- [SHA-256](../downloads/B-Cycle-0.8.97-release-142.apk.sha256)
- [Erfolgreicher Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/42109783572)
- Quellstand: `e88ca21d028e5e49c102132a911e276ea9771b54`
- Version0.8.97, versionCode167, Android13+.
- Größe: 4296260 Bytes (4,30MB).
- SHA-256: `aff217735e6dc2d26512ba253495b5b6ade56ebcf24b44af7e3f932aa16c7aac`.
- Zertifikat SHA-256: `94d34c46ede28d12e6d37efd8a7d953a75b621596ec2e5b68f61899b8ea7499b`.

Release ausschließlich aus GitHub Actions, dauerhafte Signatur, CI-Prüfsumme und Version geprüft. Manifest ohne Debug-/Test-Flags, ZIP-/16KB-Ausrichtung und ARM64-ELF-LOAD-Segmente geprüft; alle vier VTM-ABIs und28Shader vorhanden. Bright-JSON/XML,111Sprites und eigene Shader bytegleich zu Quellen, ohne Extrusionen. Terrain-/Cycling-Attribution erhalten. R8-Namen für GLMatrix/MVT und zentrale Karten-/Designerpfade geprüft; neue Import- und HRV-Texte im Artefakt vorhanden.

[Änderungen](release-0.8.97.md): Flat-Flächentausch/Sticky-Linien, HSV-Trainingshilfe, stationärer HRV-Tageswechsel mit Ease-in-out, HRV-Wiederholungsbestätigung mit zwei nebeneinanderstehenden Buttons, gemeinsame Importgruppen, Aufzeichnungsreihenfolge (Höhenkorrektur → Fahrt benennen → Trenner), Kartenatlas-/Kontur-/Strichkorrekturen, Vektorzoom21 und zentrale Quellen/gruppierte Kartenauswahl.

Gezielte Prüfungen grün: `flat-surfaces-group-rules-tests.log`, `flat-route-popup-final-test.log`, `flat-hsv-final-test.log`, bestehende Balken-/Glow-Fälle aus `flat-hsv-transition-tests.log`, `hrv-morph-final-tests.log` (inkl.17Dashboard-Paging-Fällen), `hrv-repeat-warning-tests.log`, `hrv-repeat-buttons-final-test.log`, `recording-order-final-test.log`, Importnavigation und zuvor dokumentierte native Karten-/Providerfälle. Typografie/UI-Katalog/Bright/Diff grün. Kein Gesamtlauf, keine lokale APK. Optischer Gerätevergleich offen.

Zwischenbuilds140/141 durch spätere Nutzerergänzungen ersetzt und nicht veröffentlicht.
