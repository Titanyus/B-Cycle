# B|CYCLE 0.8.36 – Build 75

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.36-release-75.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.36-release-75.apk.sha256)
- [GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/40091462695)
- Quellstand: `c710ad6cf804935efe2807d3df0d3a854b17d60c`
- Version 0.8.36, versionCode 106, Android 13+.
- Build vom 24.10.2026, Größe **2.392.733 Bytes (2,39 MB)**.
- APK-SHA-256: `edc7c62f3760e6c72335a9c770ea6694313d9502811d900d9e8876dcd3aa8881`
- Zertifikat-SHA-256: `94D34C46EDE28D12E6D37EFD8A7D953A75B621596EC2E5B68F61899B8EA7499B`

Optimierter Release über GitHub Actions mit R8 und Ressourcenverkleinerung. Prüfsumme mit CI abgeglichen; apksigner bestätigt denselben dauerhaften Schlüssel wie Build 74, aapt Version 0.8.36/106 ohne Debug-/Test-Flag, zipalign die Ausrichtung. Mapping separat im Actions-Artefakt, nicht in der APK. Direkter APK-Download ohne ZIP. Als Update über gleich signierte Vorgänger seit Build 53 installierbar.

Enthält alle seit Build 74 umgesetzten Appänderungen: Flat-/Glow-Design mit gemeinsamer Basis, PRIMARY-Hervorhebung, rechts angeordnete Auswahl-Schalter, Scroll-Glow-Korrektur, sechs Glättungspunkte für Zeitfenstergraphen, Dropdowns für alle Hauptquellen, korrigierte Dreeve-Namensrichtung und Kartenzoomstufe. Autostart weiterhin mit aktivem Profil, ohne automatischen Profilwechsel. Der separate HTML-Designer ist keine App-Konfigurationsseite. [Änderungen](release-0.8.36.md).

Gezielte Tests in den Umsetzungsschritten erfolgreich: Graphen inklusive unveränderter FIT-/Journal-Ausgabe, Quellen/Fallback/Sicherung, Dreeve-Namensrichtung und konkurrierende Bearbeitung, Modus-/PRIMARY-/ACCENT-/Sicherungsprüfung, Controls/HRV, Scroll-Glow sowie Kartenzoom/kurze Karten. Native Flat-/Glow- und Kartenvorschauen visuell geprüft; Typografie-/Atlas-/Designerprüfung erfolgreich. Kein Gesamtlauf, reales Dreeve-Konto, Hardwaretest oder lokaler APK-Build. [Vorheriger Build](build-74.md).
