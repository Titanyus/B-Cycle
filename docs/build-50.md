# B|CYCLE 0.8.13 – optimierte APK

Build 50 vom 19.10.2026, versionCode 83.

**[APK herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/v0.8.13/downloads/B-Cycle-0.8.13.apk)**

## Änderungen

- Streckenmanager → Strecke importieren → Aus Dreeve importieren öffnet eine einfache eigene Fahrtenliste mit Mehrfachauswahl, Nachladen weiterer Seiten und Konfigurationszugang.
- Bestehende Dreeve-Zugangsdaten wiederverwenden; manuelle Abrufe unabhängig vom automatischen Upload-Schalter. Namen, Datum, Distanz, Sportart und GPX-Verfügbarkeit anzeigen.
- Verfügbare GPX-Sensorerweiterungen übernehmen: Leistung, Puls und Kadenz in der Analyse, Temperatur im unveränderten Original. Nach dem Import verfügbare Datenfelder auflisten. Kein Original-FIT-Download, keine vollständigen E-Bike-Kanäle; ohne GPX derzeit kein Import.
- Stabile lokale/externe IDs verhindern bekannte Duplikate, einschließlich bereits zu Dreeve hochgeladener lokaler FIT-Fahrten. Einzelne Fehler blockieren übrige Importe nicht; kein automatischer Upload von Importen.
- Bestehende Karten-/Streckenauswahl erhalten. Hilfe und [Dokumentation](dreeve-import.md) ergänzt.

## Prüfung

41 unterschiedliche gezielte Tests über abgeschlossene Läufe bestanden: HTTP/Pagination/Authentifizierung/URL-Kodierung, Größenbegrenzung und Abbruch, Sensorerweiterungen und fehlende Werte, Originaldatei/Datum/Identität, bekannte FIT-Zuordnung und wiederholter Import ohne Duplikat, Teilfehler, Mehrfachauswahl/Nachladen/Navigation sowie vorhandene Upload-/GPX-/Archiv-/Analyse-/Hilferegressionen. Kein vollständiger Testlauf. Kein echter Dreeve-Kontoabruf und kein physischer Gerätetest; der reale Exportumfang bleibt in der App auszuprobieren. Versionserhöhung danach ohne funktionale Änderungen.

[GitHub Actions erfolgreich](https://github.com/Titanyus/B-Cycle/actions/runs/39310337422), Quellstand c047f391b0f910961df71f22d83d3e5a8cefb6a7. Optimierter Release mit R8 und Ressourcenverkleinerung, kein lokaler APK-Build. Artefakt-Prüfsumme geprüft, mit bisherigem Testschlüssel nachsigniert. Zertifikat, Version 0.8.13/83, nicht-debuggable Manifest und 16-KiB-Zip-Ausrichtung geprüft.

- Größe: 1,899,740 Bytes.
- SHA-256: `31a248193a9f377bcb3c66e3c6ac845bbaac91bc17c818056bab57dc59775003`.
- Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.
- [Prüfsumme](../downloads/B-Cycle-0.8.13.apk.sha256) · [R8-Mapping](../downloads/B-Cycle-0.8.13-mapping.txt.gz).
