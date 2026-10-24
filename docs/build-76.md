# B|CYCLE 0.8.37 – Build 76

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.37-release-76.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.37-release-76.apk.sha256)
- [GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/40102129775)
- Quellstand: `3dd2c62e509403426bce817c51ad7d4e10374d1d`
- Version 0.8.37, versionCode 107, Android 13+.
- Build vom 24.10.2026, Größe **2.407.205 Bytes (2,41 MB)**.
- APK-SHA-256: `66d3f715a3232d4e98349b9bc0893b0d4a223b9c54d66be39dd0059b692d1e58`
- Zertifikat-SHA-256: `94D34C46EDE28D12E6D37EFD8A7D953A75B621596EC2E5B68F61899B8EA7499B`

Optimierter Release über GitHub Actions mit R8 und Ressourcenverkleinerung. Prüfsumme mit CI abgeglichen; apksigner bestätigt denselben dauerhaften Schlüssel wie Build 75, aapt Version 0.8.37/107 ohne Debug-/Test-Flag, zipalign die Ausrichtung. Mapping separat im Actions-Artefakt, nicht in der APK. Direkter APK-Download ohne ZIP. Als Update über gleich signierte Vorgänger seit Build 53 installierbar.

Enthält alle Appänderungen seit Build 75: Kotlin-Chromatic-Engine analog Designer, freigegebene Theme-/Kontur-/Flächen-/Glow-Werte aus der Nutzer-JSON (zentral im Code, keine Laufzeitdatei), Kontrastschutz und Flat-Modus erhalten. Gemeinsame Kartenmodi, weiche zoomabhängige Vorausschau, Zoom 6–19 in halben Tastenschritten (OpenTopoMap max. 17), Zoomzahl entfernt. Gemeinsames Daten-Schalter-Accordion im Dashboard sowie konkrete Dienstfehler bei Dreeve/intervals/Strava/WebDAV. [Änderungen](release-0.8.37.md).

Gezielte Tests in den Umsetzungsschritten erfolgreich: 104 eingefrorene Designerreferenzen, Theme-/Fokus-/Glow-/Flat-/Scroll-/Eingabe-/Konfigurationsprüfungen, Kartensteuerung/Projektion/Zoom/Profile/Sicherung, Feldauswahl/unabhängige Graphen, echte HTTP-Testserver und dauerhafte Upload-/Namens-/Backupzustände. Native Vorschauen visuell geprüft; Typografie-/Atlas-/Designerprüfung erfolgreich. Kein Gesamtlauf, echter Cloudkonten-/Hardwaretest oder lokaler APK-Build. [Vorheriger Build](build-75.md).
