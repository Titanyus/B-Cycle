# Test-Build 28 · 17.10.2026

[**APK herunterladen**](https://raw.githubusercontent.com/Titanyus/B-Cycle/test-build-28-apk/downloads/B-Cycle-0.7.31-test-build-28.apk)

## Sensoren, Dropdowns und Abstände

- Sensorzeilen: Höhe eines einzeiligen Sticky Headers; Schalter, Titel/Icon und Chevron vertikal zentriert. Zusätzliche vertikale Formular-Margins innerhalb der Zeile entfallen.
- Sensorliste als gemeinsame Inhaltsgruppe. Zum nächsten Sticky Header 32 dp, auch bei geöffneten Details und vor dem verschachtelten FIT-Layout; interne Sensorabstände bleiben kleiner.
- Zentral vor nachfolgenden Sticky Headern 32 statt 16 dp. Abstand vom Header zum Inhalt weiterhin 16 dp, Seitenanfang unverändert.
- Native Dropdowns der Profilseite mit 14 sp wie normale Titel; Theme-Namen in geschlossenem Feld und Popup mit 28 sp / Bebas Neue wie die schmalen Streckendetailwerte. Die Farbfelder behalten die tatsächlichen fünf Themefarben.

[Sensorvorschau](previews/profile-sensors.png) · [Themes](previews/theme-dropdown.png) · [Design](previews/profile-long-design.png) · [FIT](previews/profile-long-fit.png)

**Vorgemerkt:** FIT-Aufzeichnung und Strecken-Aufzeichnung als gleichrangige Sticky-Oberpunkte in voller Seitenbreite auf die Hauptebene ziehen. Beide sind bereits sticky, liegen aber noch im gemeinsamen FIT-Inhaltslayout und wirken schmaler. Auf ausdrücklichen Nutzerwunsch nur dokumentiert, in diesem Build noch nicht umgesetzt. Siehe [Profil-Dokumentation](bicycle-profiles.md#vorgemerkt-aufzeichnungsabschnitte-auf-die-hauptebene).

## Herkunft und Prüfung

[Erfolgreicher GitHub-Actions-Build 28](https://github.com/Titanyus/B-Cycle/actions/runs/39051529940), Quellstand `09fdd2f9f0a8725f4ec934e152ffe579aa03ad1b`.

**34 gezielte Tests erfolgreich**, keine Fehler oder übersprungenen Tests: Sensorhöhen/Zentrierung, 32-dp-Abschluss bei geöffneten und geschlossenen Details, kleinere interne Listenabstände, wiederholbare zentrale Abstandsregeln, Dropdown-Schriften, Themewechsel, Profile, FIT und Indoor. Drei Komponententests anschließend für die zusätzliche Sensorvorschau nochmals erfolgreich ausgeführt. Native Vorschauen visuell geprüft. Keine vollständige Suite, kein zusätzlicher Gerätetest.

APK durch GitHub Actions gebaut, anschließend mit dem bisherigen Testschlüssel nachsigniert. Signatur und 16-KiB-ZIP-Ausrichtung geprüft. Kein lokaler APK-Build.

Version **0.7.31**, versionCode **69**; als Update über bisherige mit diesem Testschlüssel signierte APKs installierbar.

Größe: 8,226,735 Bytes.

SHA-256: `5bc5c5778fe3612d23e8dda9ba05b4de8f8c9be4e868810faf6144eefe80c196`

Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.
