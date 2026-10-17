# Test-Build 25 · 17.10.2026

[**APK herunterladen**](https://raw.githubusercontent.com/Titanyus/B-Cycle/test-build-25-apk/downloads/B-Cycle-0.7.31-test-build-25.apk)

## Überarbeitete Profil-Accordions

- Rundungen auf 10 dp vergrößert. Kein umlaufender Rahmen am Header oder Inhalt.
- Geschwungene linke FOREGROUND-Kante, 3 dp breit. Die dünne vertikale Inhaltslinie schließt durchgehend an die Kurve des nächsten Accordion-Headers an.
- Sticky-Unterüberschriften, Bedienung und Profilkonfiguration bleiben erhalten.
- Sichtbare Radtypbezeichnung **Indoor** statt Indoor-Bike; stabile interne Quellenschlüssel bleiben unverändert.
- Überschrift **Kartenquelle** entfernt, Auswahl und Accessibility-Beschreibung erhalten.

[Design-Vorschau](previews/profile-accordions.png) · [FIT-Vorschau](previews/profile-fit-accordion.png)

## Herkunft und Prüfung

[Erfolgreicher GitHub-Actions-Build 25](https://github.com/Titanyus/B-Cycle/actions/runs/39044145548), Quellstand `8dd0388a346808b6cf0a430e2e0172a7aa035646`.

**19 gezielte Tests erfolgreich**, keine Fehler oder übersprungenen Tests: Accordion-Geometrie ohne Außenkontur, durchgehende Verbindung zum nächsten Header, Themewechsel, Sticky-Begrenzung, Profilwechsel und Übernahme, Bike-/FIT-Ansicht sowie Indoor-Oberfläche. Native Design-/FIT-Vorschauen visuell geprüft. Keine erneute vollständige Suite für diese UI-Anpassung.

APK ausschließlich durch GitHub Actions gebaut, anschließend mit dem bisherigen Testschlüssel nachsigniert. Signatur und 16-KiB-ZIP-Ausrichtung geprüft. Kein lokaler APK-Build und kein zusätzlicher Gerätetest.

Version **0.7.31**, versionCode **69**; als Update über bisherige mit diesem Testschlüssel signierte APKs installierbar.

Größe: 8,222,639 Bytes.

SHA-256: `4a6360e83e40c1a7486a90c2192734fc487bb5c4c5e49de4f5681eec2ff51a23`

Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.
