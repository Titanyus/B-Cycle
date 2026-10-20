# B|CYCLE 0.8.14 – optimierte APK

Build 51 vom 20.10.2026, versionCode 84.

**[APK herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/v0.8.14/downloads/B-Cycle-0.8.14.apk)**

## Änderungen

- Dreeve-Import als eigene Seite mit automatischem Laden aller API-Seiten, Fortsetzung nach Abruffehlern, Filter/Sortierung aus der gemeinsamen Implementierung des Streckenmanagers und Auswahl aller gefilterten Fahrten.
- Fahrradnamen/Dauer in der Liste und bestätigte, für spätere Importe gespeicherte Zuordnung externer Fahrräder zu lokalen Profilen. Ohne GPX Import als JSON-Zusammenfassung; keine erfundenen Messwertreihen.
- Importierte Dreeve-Fahrten zeigen einen Verknüpfungshaken, auch bei deaktiviertem automatischem Upload. Zeitnahe Bestätigungsabfragen für Dreeve-Uploads.
- Nach bestätigtem Upload einmalig Dreeves Automationsnamen nach drei Sekunden abrufen und lokal sowie bei verknüpften Diensten übernehmen. Manuelle Namen haben Vorrang. Dauerhafte, getrennte Namensaufträge für Dreeve, intervals.icu und Strava; keine erneuten FIT-Uploads für Namensänderungen.
- Neue Aufnahmen bewahren verfügbare Messwerte privat für die vollständige Auswertung. FIT-Standard-/Entwickler-/GPS-Felder, Ordnerexport und Cloud-Uploads bleiben gemäß Nutzerauswahl reduziert. Ältere bereits verworfene Werte sind nicht wiederherstellbar.
- Mehrfachauswahl im Streckenmanager per Langdruck mit bestätigtem gemeinsamem Löschen; ungewählte Karte und externe Dateien bleiben erhalten.
- Feste Detailtitel mit langsamer Laufschrift und Fade, 10 dp nach Rad/Datum/Wetter, keine vertikalen Kennzahlentrenner. Hilfe und [Architekturdokumentation](dreeve-workflow-and-private-analysis.md) ergänzt.

## Prüfung

136 unterschiedliche gezielte Tests über abgeschlossene Läufe bestanden; keine Fehler oder übersprungenen Fälle. API-/Namensabgleich-Verträge, Teilfehler/Kontowechsel/Manualpriorität, automatische Pagination und Fortsetzung, gemeinsame Filter, fester Footer, Fahrradzuordnung, Journal-/FIT-/Privatdaten-/Exportpfade, tatsächliche GPX-Performancewerte, Mehrfachlöschen, Titeltypografie/Animatorabbau und bestehende Aufnahme-/Archiv-/Streckenregressionen. Kein vollständiger Testlauf. Echte Cloudkonten, reale Dreeve-Automationslaufzeit und physisches Gerät noch zu prüfen. Versionserhöhung danach ohne funktionale Änderung.

[GitHub Actions erfolgreich](https://github.com/Titanyus/B-Cycle/actions/runs/39382081391), Quellstand 3184aa07eb5fcbce47edd9f8dadc5c0d81bcde79. Optimierter Release mit R8 und Ressourcenverkleinerung, kein lokaler APK-Build. Artefakt-Prüfsumme geprüft und mit bisherigem Testschlüssel nachsigniert. Zertifikat, Version 0.8.14/84, nicht-debuggable Manifest und 16-KiB-Zip-Ausrichtung geprüft.

- Größe: 1,936,604 Bytes.
- SHA-256: `a2d8a2aa4ad5160c5fa7a13ddd2835d0db638abab5580542050b04932eabeb68`.
- Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.
- [Prüfsumme](../downloads/B-Cycle-0.8.14.apk.sha256) · [R8-Mapping](../downloads/B-Cycle-0.8.14-mapping.txt.gz).
