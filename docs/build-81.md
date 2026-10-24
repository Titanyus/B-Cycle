# B|CYCLE 0.8.41 – Build 81

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.41-release-81.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.41-release-81.apk.sha256)
- [GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/40119418776)
- Quellstand: `c120b63c45f1346e9cabc3c54844ce5853e84f4f`
- Version 0.8.41, versionCode 111, Android 13+.
- Build vom 24.10.2026, Größe **2.421.681 Bytes (2,42 MB)**.
- APK-SHA-256: `3e11ca2cd91cd8a35955920056cfb7406ea92bb80c8c2d44f1957f0f480c1cd3`
- Zertifikat-SHA-256: `94D34C46EDE28D12E6D37EFD8A7D953A75B621596EC2E5B68F61899B8EA7499B`

Optimierter Release über GitHub Actions mit R8 und Ressourcenverkleinerung. CI-Prüfsumme, dauerhafte Signatur wie Build 80, Version/Manifest ohne Debug-/Test-Flag und ZIP-Ausrichtung geprüft. Mapping separat im Actions-Artefakt, nicht in der APK. Direkter APK-Download ohne ZIP. Update über gleich signierte Vorgänger seit Build 53 möglich.

Texteingaben auch in Glow wieder mit 6-dp-Ecken, Rahmen und Glow folgen derselben Kontur. Special-Pillen und andere Controls unverändert. Sticky-Kategorieheader in Flat ohne sichtbare weiße obere/untere Linien, bei unveränderter Geometrie; Glow-Linien bleiben. Alle Funktionen und Änderungen aus Build 80 enthalten. [Alle Änderungen](release-0.8.41.md).

Gezielte native AppearanceMode-/InputFocusFrame-/BicycleStickySections-/GlowChrome-Prüfungen erfolgreich, native Eingabevorschau visuell geprüft; Typografie-/Atlas-/Diff-Prüfungen erfolgreich. Kein Gesamtlauf, Hardwaretest oder lokaler APK-Build. [Vorheriger Download](build-80.md).
