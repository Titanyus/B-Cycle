# Test-Build 29 · 17.10.2026

[**APK herunterladen**](https://raw.githubusercontent.com/Titanyus/B-Cycle/test-build-29-apk/downloads/B-Cycle-0.7.31-test-build-29.apk)

## Sticky-Accordions und neue Formulardarstellung

- Sticky Header verwenden TERTIARY, Schrift und obere/untere Linien weiterhin FOREGROUND.
- Profilabschnitte auf-/zuklappbar durch Antippen des Headers. Anfangs geöffnet, Chevron zeigt den Zustand; Titel durch gleichen Platz links und rechts exakt zentriert. Geöffnet bleibt das bisherige Sticky-Verhalten erhalten. Geschlossene Abschnitte rücken zusammen; ein angehefteter Header bleibt nach dem Einklappen erreichbar. Offen-Zustand bleibt innerhalb derselben Ansicht bei Profilwechsel erhalten.
- Fahrradname wieder eigener Sticky Header. Antippen benennt um, keine Klappfunktion.
- FIT-Aufzeichnung und Strecken-Aufzeichnung nun gleichrangige Oberpunkte direkt auf der Hauptebene, mit voller Seitenbreite und separaten Inhaltsgruppen. Vorhandene Auswahl und sofortige Speicherung erhalten; keine neue FIT-/Statistiklogik.
- Formularbuttons und Dropdowns mit SECONDARY, 2 dp FOREGROUND-Rahmen und vorhandenem 6-dp-Radius. Eigene Streckenfilter und Popup-Auswahlen entsprechend angepasst. Bestehende spezielle Eingabefeld- und fachliche Auswahlstile erhalten.
- Titel-/Dropdown-Zeilen ohne zusätzliche innere vertikale Formular-Margins; Rahmen werden nicht mehr unten abgeschnitten.

[Sticky-Accordions mit eingeklappten Sensoren](previews/profile-sticky-accordions.png) · [Sensoren](previews/profile-sensors.png) · [Design](previews/profile-long-design.png) · [FIT](previews/profile-long-fit.png)

## Nur vorgemerkt

- Dashboard im Vollbild als ersten Inhaltspunkt in Design & UI setzen.
- Benachrichtigungen: Header Position/Anzeigedauer entfernen. Beschriftung Anzeigedauer links, vertikal zur Sliderlinie zentriert; Slider rechts, gewählter Sekundenwert darüber.

Diese zusätzlichen Layoutänderungen sind auf ausdrücklichen Nutzerwunsch nur dokumentiert und noch nicht umgesetzt.

## Herkunft und Prüfung

[Erfolgreicher GitHub-Actions-Build 29](https://github.com/Titanyus/B-Cycle/actions/runs/39054021295), Quellstand `d10e7fe1335e2297078ba433f31002b4463554ac`.

**40 gezielte Tests erfolgreich**, keine Fehler oder übersprungenen Tests. Geprüft: unabhängiges Auf-/Zuklappen, Breite und Hierarchie der Aufzeichnungsheader, Erreichbarkeit nach Einklappen, Profilwechsel, Name/Umbenennen, FIT-/Indoor-Auswahl, Sensorzeilen, Dropdown-Rahmen ohne Clipping, 2-dp-Kontur und Themewechsel sowie unveränderte separate Eingabefeld-Darstellung. Native Vorschauen visuell geprüft. Keine vollständige Suite und kein zusätzlicher Gerätetest.

APK durch GitHub Actions gebaut, anschließend mit dem bisherigen Testschlüssel nachsigniert. Signatur und 16-KiB-ZIP-Ausrichtung geprüft. Kein lokaler APK-Build.

Version **0.7.31**, versionCode **69**; als Update über bisherige mit diesem Testschlüssel signierte APKs installierbar.

Größe: 8,239,023 Bytes.

SHA-256: `e43aaa85ef1b40c5cc9e0705d9a6da089645b4a1e316f168bb7698ea416c0ba7`

Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.
