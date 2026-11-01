# B|CYCLE0.8.79 – Build120

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.79-release-120.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.79-release-120.apk.sha256)
- [GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/41355159413)
- Quellstand: `66a487b5f490df3834644a21b96d97caac309a96`
- Version0.8.79, versionCode149, Android13+.
- Größe: 2870449Bytes (2,87MB).
- SHA-256: `b8e9daffb07e871c836a33d501943328a97170333d589c6b306a0d31aabe980e`.
- Signierzertifikat SHA-256: `94d34c46ede28d12e6d37efd8a7d953a75b621596ec2e5b68f61899b8ea7499b`.

Optimierter Release aus GitHub Actions. CI-Prüfsumme, Version, Manifest ohne Debug-/Test-Flags und dauerhafte Signatur geprüft. Update-Zertifikat wie Build119. APK-ZIP-Ausrichtung einschließlich16-KB-Ausrichtung nativer Bibliotheken geprüft. Alle vier VTM-ABIs und25 Shader in der tatsächlich gebauten APK vorhanden; native GLMatrix-Klassenbezeichnung nach R8 erhalten. Tatsächlichen veröffentlichten Raw-Direktdownload erneut geladen und binär identisch mit dem geprüften Actions-Artefakt bestätigt.

[Änderungen](release-0.8.79.md): Mapsforge-VTM mit bisherigen Rasterquellen und erhaltenen Dashboard-Kartenfunktionen; invertierte Nachtkarte zunächst deaktiviert. Keine Sensor-/Aufzeichnungsdrosselung, kein allgemeiner Sparmodus und noch keine Online-Vektorkarten.

[Umfang und19 gezielte Prüffälle](vtm-migration.md). Nach Korrekturen nur betroffene/offene Fälle wiederholt, kein Gesamtlauf. Debugkompilation, Native-/Java-Ressourcenzusammenführung, Typografie/Atlas/Diff erfolgreich. Tatsächliche OpenGL-Ausgabe, SurfaceView-Zusammensetzung/Clipping, PixelCopy, Wischgefühl und App-/Dialogrückkehr noch auf Androidgerät bestätigen. Rückfallstand [Meilenstein0.8.78/Build119](milestone-0.8.78.md).
