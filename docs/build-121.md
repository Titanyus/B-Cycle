# B|CYCLE0.8.80 – Build121

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.80-release-121.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.80-release-121.apk.sha256)
- [GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/41420841590)
- Quellstand: `6061cc6379cdce87c5125463bf56cd377b7f8327`
- Version0.8.80, versionCode150, Android13+.
- Größe: 4074378Bytes (4,07MB).
- SHA-256: `44daf0c56e46b407459e18df420757376e022e3d3e24b702ee22aa458096b554`.
- Signierzertifikat SHA-256: `94d34c46ede28d12e6d37efd8a7d953a75b621596ec2e5b68f61899b8ea7499b`.

Optimierter Release aus GitHub Actions. CI-Prüfsumme, Version, Manifest ohne Debug-/Test-Flags und dauerhafte Signatur geprüft. Update-Zertifikat wie Build120. ZIP-Ausrichtung einschließlich16-KB-Ausrichtung nativer Bibliotheken geprüft; ARM64-ELF-LOAD-Segmente mindestens16-KB-ausgerichtet. Alle vier VTM-ABIs und26 Shader vorhanden, einschließlich neuem Streckenshader. Liberty-XML und Shader binär gegen Quelle verglichen, Liberty-Hintergrund/fehlende Extrusionen in der APK geprüft. OpenMapTiles-Ressourcen und Liberty-Lizenz vorhanden. Native GLMatrix-Bezeichnung und protobuf-MVT-Namen nach R8 erhalten. Tatsächlichen veröffentlichten Raw-Direktdownload erneut geladen und binär identisch mit dem geprüften Actions-Artefakt bestätigt.

[Änderungen](release-0.8.80.md): zusätzliche OpenFreeMap-Vektorkarte mit flacher Liberty-Adaption, optionale Radwegehervorhebung, native GL-Strecken ohne separate Android-Zeichenfläche und Seitenwechsel erst beim Loslassen nach Weg/Geschwindigkeit.

Gezielte Komponenten-/Integrations-/Decoder-/Native-/Shaderprüfungen im Vorfeld erfolgreich, kein Gesamtlauf und keine lokale APK gebaut. Siehe [Vektorquelle](vtm-vector-map.md), [Liberty/Draufsicht](vtm-liberty-style.md), [Strecken](vtm-route-frame-sync.md), [Seitenwechsel](dashboard-page-transition.md). Tatsächliche Android-GPU-Ausgabe, schnelle Kartengesten, Wischgefühl und App-/Dialogrückkehr am Smartphone noch bestätigen. Kein allgemeiner Akkuschonmodus/Minimal-Stil/Live-Stilimport.
