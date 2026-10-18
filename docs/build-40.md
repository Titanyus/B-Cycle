# B|CYCLE 0.8.5 – optimierte APK

Build 40 vom 18.10.2026, versionCode 75.

**[APK herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/v0.8.5/downloads/B-Cycle-0.8.5.apk)**

## Änderungen

- Alle Sticky-Abschnitte aufklappbar, außer dem antippbaren Fahrradtitel zum Umbenennen.
- Überarbeitete Benachrichtigungseinstellungen mit Schalter/Accordion, Positionsüberschrift und Sekundenwert näher am Slider.
- Erweitert ohne die Menüeinträge für ungespeicherte Fahrten und GPS-Erklärung; zugrunde liegende Funktionen bleiben erhalten.
- Streckenfilter nach Herkunft/Radtyp, Fahrrad, Datum, Distanz, Bewegungszeit und Durchschnittsgeschwindigkeit, mit Datums-Schnellwahl. Sortierung nach vier Kriterien in beiden Richtungen.
- Streckendetails mit Herkunfts-/Radtyp-Icon und FIT/GPX neben dem zweizeiligen Kopf; ohne GPS kein Auge in der Liste.
- Dropdown-Fenster in TERTIARY, größere Abstände an allgemeinen Abschnittstrennern; kompakte Fahrdatenblöcke bleiben erhalten.
- Sieben Themes ohne DARK mit SECONDARY #161522 und TERTIARY #282735; ACCENT wie bei DARK gleich PRIMARY. DARK und helle Einstellungen unverändert.
- [Theme-Testeditor](previews/theme-surfaces-proposal.html) dauerhaft als eigenständige Offline-HTML-Datei gespeichert.

## Prüfung

Gezielte Filter-, UI-, Theme-, Benachrichtigungs-, Abstands- und Navigationsprüfungen bestanden. Veraltete Erwartungen für weiße Auswahlmarkierungen auf die bestätigte Accent-Farbe aktualisiert und die betroffenen Komponententests erneut erfolgreich ausgeführt. Kein vollständiger Testlauf und kein physischer Gerätetest.

[GitHub Actions erfolgreich](https://github.com/Titanyus/B-Cycle/actions/runs/39165261572), Quellstand c30ac36afff40111041eaf86c861c4756cf56073. Release mit R8 und Ressourcenverkleinerung; kein lokaler APK-Build. Artefakt-Prüfsumme geprüft, mit bisherigem Testschlüssel nachsigniert; Zertifikat, Version 0.8.5/75, nicht-debuggable Manifest und 16-KiB-Zip-Ausrichtung geprüft.

- Größe: 1,842,340 Bytes.
- SHA-256: `5295949d613fb6e920a534e07c4072d2520cf0083734102c41782ff1fa187d8c`.
- Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.
- [Prüfsumme](../downloads/B-Cycle-0.8.5.apk.sha256) · [R8-Mapping](../downloads/B-Cycle-0.8.5-mapping.txt.gz).

Vollständiger Dreeve-Import, Dreeve-Bestätigungsoptimierung und zentrale Strava-Anmeldung sind nicht implementiert.
