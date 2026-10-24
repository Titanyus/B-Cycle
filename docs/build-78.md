# B|CYCLE 0.8.39 – Build 78

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.39-release-78.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.39-release-78.apk.sha256)
- [GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/40115524407)
- Quellstand: `7c1d2aa61bbe26c04b62df782ee7905a3d434aab`
- Version 0.8.39, versionCode 109, Android 13+.
- Build vom 24.10.2026, Größe **2.416.073 Bytes (2,42 MB)**.
- APK-SHA-256: `47d6004fb884296e444523af1ca041e2cf5697908dcb8d6a5bc94cf03467571b`
- Zertifikat-SHA-256: `94D34C46EDE28D12E6D37EFD8A7D953A75B621596EC2E5B68F61899B8EA7499B`

Optimierter Release über GitHub Actions mit R8 und Ressourcenverkleinerung. CI-Prüfsumme, dauerhafte Signatur wie Build 77, Version/Manifest ohne Debug-/Test-Flag und ZIP-Ausrichtung geprüft. Mapping separat im Actions-Artefakt, nicht in der APK. Direkter APK-Download ohne ZIP. Update über gleich signierte Vorgänger seit Build 53 möglich.

Enthält HRV-Screens ohne Statusleiste, schlichten Messheader in SECONDARY, Starten/Erneut messen im Stil Special-Rahmen und Neon-Herz auch in Flat. Fahrtabschlussheader passend zum Hintergrund, Sticky-Kategorieheader League Gothic mit Groß-/Kleinschreibung/30 sp. Neue symmetrische Karten-Farbpalette mit sechs Feldern pro Kante/91 Farben und zehn Helligkeitsstufen statt Graureihe. [Alle Änderungen](release-0.8.39.md).

Gezielte HRV-, Angebots-, Sticky-, Fahrtabschluss-, Glow- und Statusleistenprüfungen erfolgreich. Vier Karten-Stiltests einschließlich regelmäßiger Geometrie, schmaler Breiten, zehn Graustufen, Endpunkten, Übernahme und Profil-/Sicherungsregression erfolgreich. Native Vorschauen visuell geprüft, Typografie-/Atlas-/Designer-, XML- und Diff-Prüfung erfolgreich. Kein Gesamtlauf, Hardwaretest oder lokaler APK-Build. [Vorheriger Build](build-77.md).
