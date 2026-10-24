# B|CYCLE 0.8.40 – Build 80

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.40-release-80.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.40-release-80.apk.sha256)
- [GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/40118877616)
- Quellstand: `95590eb554ab09a9b02183ccbf9ac3cf3bd0d113`
- Version 0.8.40, versionCode 110, Android 13+.
- Build vom 24.10.2026, Größe **2.421.725 Bytes (2,42 MB)**.
- APK-SHA-256: `802f97b571e585dd77826526048f1610b4f3cd3f1e8ed1bc7a2f5f93962fb197`
- Zertifikat-SHA-256: `94D34C46EDE28D12E6D37EFD8A7D953A75B621596EC2E5B68F61899B8EA7499B`

Optimierter Release über GitHub Actions mit R8 und Ressourcenverkleinerung. CI-Prüfsumme, dauerhafte Signatur wie Build 78, Version/Manifest ohne Debug-/Test-Flag und ZIP-Ausrichtung geprüft. Mapping separat im Actions-Artefakt, nicht in der APK. Direkter APK-Download ohne ZIP. Update über gleich signierte Vorgänger seit Build 53 möglich. Build 79 enthielt den ersten Zwischenstand vor der während des Builds gewünschten Zoom-Ergänzung und wurde nicht als Download veröffentlicht.

Enthält alle Nachträge seit Build 78: zentral geschützte Sticky-Schrift/Großbuchstaben, kartenloser Footer immer sichtbar, kompakte Daten-/Trendkategorien mit Trennern und Graph-Schalter-Accordion. Trink-Erinnerung am Ende der Trainingshilfe mit Dauer/Position, profilbezogen gesichert; Sensorlinien testweise unsichtbar bei erhaltener Geometrie. Kontinuierlicher Pinch-Zoom und kurzer Zoomnachlauf auf Dashboard/interaktiver Detailkarte; native Verschiebung/Fling unverändert. [Alle Änderungen](release-0.8.40.md).

Gezielte Tests in den Umsetzungsschritten erfolgreich: Sticky-/HRV-/Benachrichtigungsstil, FullData/Indoor-Footer, 26 Feldauswahl-/Graph-/Fensterprüfungen, Trinkintervall/Timeout/Position/Profilkopie/Backup und Sensorbedienung, anschließend Pinch-/Nachlauf-/Abbruch-/Grenz- und bestehende MapPanel/MapZoom-Prüfungen. Native Trink-/UI-Vorschauen visuell geprüft; Typografie-/Atlas-/XML-/Diff-Prüfungen erfolgreich. Kein Gesamtlauf, Hardwaretest oder lokaler APK-Build. [Vorheriger Download](build-78.md).
