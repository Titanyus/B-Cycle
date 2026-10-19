# B|CYCLE 0.8.11 – optimierte APK

Build 48 vom 19.10.2026, versionCode 81.

**[APK herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/v0.8.11/downloads/B-Cycle-0.8.11.apk)**

## Änderungen

- Streckenliste: Anfangsabstand unter dem Header in die ListView verschoben. Der feste Seitencontainer hat kein oberes Padding mehr; Einträge können beim Scrollen direkt unter dem Header verschwinden.
- Native Android-Schnellnavigation am rechten Listenrand. Bei längeren Listen erscheint der ziehbare Fast-Scroller beim Scrollen.
- Sprungabschnitte entsprechend der aktiven Sortierung: Monat/Jahr, 10-km-Bereiche, Stundenbereiche oder 5-km/h-Bereiche. Reihenfolge und Sprungziele folgen dem gefilterten Bestand, auf- wie absteigend. Fehlende Werte erhalten einen eigenen Abschnitt.
- Vorhandene Filter, Importstatus, Kartenaugen und Fußaktionen bleiben erhalten. Keine Namenssortierung ergänzt; A–Z wäre eine zusätzliche spätere Funktion.
- [Scrollkonventionen](route-list-scrolling.md). Die [vorgemerkte Streckendetail-Optik](pending-ui-changes.md) ist weiterhin nicht umgesetzt.

## Prüfung

24 gezielte Tests bestanden: Streckenmanager, Inhaltsabstände, Vollbild-Layout sowie Abschnitte/Sprungpositionen der Schnellnavigation einschließlich Sortierrichtung, Teilmengen und fehlender Werte. Kein vollständiger Testlauf. Physische Bedienung und Darstellung des nativen Fast-Scrollers auf dem Gerät stehen noch aus. Versionserhöhung danach ohne funktionale Änderungen.

[GitHub Actions erfolgreich](https://github.com/Titanyus/B-Cycle/actions/runs/39251589788), Quellstand 3c241d92550a233fdb0e128ff617ee2e4d0d896d. Optimierter Release mit R8 und Ressourcenverkleinerung, kein lokaler APK-Build. Artefakt-Prüfsumme geprüft, mit bisherigem Testschlüssel nachsigniert. Zertifikat, Version 0.8.11/81, nicht-debuggable Manifest und 16-KiB-Zip-Ausrichtung geprüft.

- Größe: 1,866,916 Bytes.
- SHA-256: `e5fa411caf3ce7c595b695b0ac8baa2bbc3e1a1a29f92e915895ca3711434acf`.
- Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.
- [Prüfsumme](../downloads/B-Cycle-0.8.11.apk.sha256) · [R8-Mapping](../downloads/B-Cycle-0.8.11-mapping.txt.gz).
