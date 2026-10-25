# B|CYCLE 0.8.44 – Build 84

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.44-release-84.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.44-release-84.apk.sha256)
- [GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/40178603298)
- Quellstand: `c707ad03c405641586483cdefacea2ef6dd46d92`
- Version: 0.8.44, versionCode114, Android13+.
- Größe: 2.428.761 Bytes (2,43 MB).
- SHA-256: `9a45b986476ec63e714a26765e53d87eb2e7740a7a46af1b4e4fae59d8380336`
- Signierzertifikat SHA-256: `94d34c46ede28d12e6d37efd8a7d953a75b621596ec2e5b68f61899b8ea7499b`.

Optimierter Release aus GitHub Actions mit R8 und Ressourcenverkleinerung. CI-Prüfsumme, dauerhafte Signatur wie Build83, Version/Manifest ohne Debug-/Test-Flag und ZIP-Ausrichtung geprüft. Direkter APK-Download ohne ZIP; Update über gleich signierte Vorgänger seit Build53 möglich.

Alle Nachträge seit Build83 enthalten: Glow-Kartenabstände16/16/24 mit direktem Sticky-Push, regulärer editierbarer Profilnamenabschnitt, geschützte Bildauswahl nach vorherigem Löschen, rechtsbündige gleich große Fahrer-Zonenwerte/-einheiten, gemeinsame Karten-/Design-Menüreihenfolge, Inline-Dashboard-Layoutaccordion mit Fahrradprofil-Kachelstil sowie Trainingshilfe-Erklärung im Detailaccordion. [Alle Änderungen](release-0.8.44.md).

30 gezielte Profil-/Foto-/Eingabe-/Geometrie-/Zonen-/Karten-/Menüprüfungen erfolgreich, danach31 gezielte Dashboard-/Pixel-/Flat/Glow-/Indoor-/Sortier-/Sicherungs-/Training-/Trinkprüfungen erfolgreich. Abschließende nativePixelprüfung1/1 erfolgreich. Bereits geprüfte fachliche Zahlen-/Speicherpfade unverändert; ausschließlich angeforderte UI-Interaktionen angepasst. Native Vorschauen visuell geprüft, Typografie-/Katalog-/Diffchecks erfolgreich. Kein vollständiger Gesamtlauf oder Hardwaretest.

Vorhandene Tests, die einen nicht aufklappbaren Profilnamenheader bzw. kleinere Einheiten voraussetzten, gezielt an die ausdrücklichen neuen Anforderungen angepasst. Popup-spezifische Testhilfen auf Inline-Auswahl umgestellt; keine Tests deaktiviert. Drei früher dokumentierte alte UI-Testfehler aus Build82 nicht Teil dieser gezielten Läufe. [Vorheriger Download](build-83.md).
