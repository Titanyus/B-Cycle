# Test-Build 24 · 17.10.2026

[**APK herunterladen**](https://raw.githubusercontent.com/Titanyus/B-Cycle/test-build-24-apk/downloads/B-Cycle-0.7.31-test-build-24.apk)

## Hierarchie-Prototyp auf der Profilseite

- Fünf Haupt-Accordions mit kräftigem 23-sp-/900-Black-Titel, Kategorie-Icon links und Zustands-Chevron rechts. Abgerundeter TERTIARY-Kopf, FOREGROUND-Kontur und betonte, gerundete linke Kante.
- Geöffneter Inhalt bleibt in einem verbundenen SECONDARY-Rahmen, links um 4 dp eingerückt. Hauptbereich und Unterpunkte sind klar getrennt.
- Unterüberschriften zentriert zwischen horizontalen Linien auf Texthöhe. Zusätzlich Sticky: **Radtyp**, **Sensoren**, **FIT-Hauptquelle**, **Aufzuzeichnende Werte**. Bestehende Design-Kategorien übernehmen dieselbe Darstellung.
- Sticky Header enden immer mit ihrem eigenen Accordion, auch vor geschlossenen Hauptbereichen. Lange Titel dürfen bei gleicher Schriftgröße umbrechen.
- Ausschließlich die Profilseite wird umgestellt. Verschachtelte FIT-Accordions, andere Screens, Profileinstellungen und Footer-Funktionen bleiben erhalten.

[Design-Vorschau](previews/profile-accordions.png) · [FIT-Vorschau](previews/profile-fit-accordion.png)

## Herkunft und Prüfung

[Erfolgreicher GitHub-Actions-Build 24](https://github.com/Titanyus/B-Cycle/actions/runs/39032822895), Quellstand `56b86347e0e0fe412dbe80cbcf8c77ca188ea044`.

**26 gezielte Tests erfolgreich.** Geprüft wurden verbundene Rahmen, Einrückung, Theme-Farben, lange Überschriften auf schmalen Hochformaten, Sticky-Begrenzung vor geschlossenen Folge-Accordions, Auf-/Zuklappen, einzelne feste Profilaktionen, Profilwechsel, FIT-Auswahl sowie bestehende Abschnittsdarstellung auf anderen Screens. Die native Profil- und FIT-Ansicht wurde zusätzlich visuell geprüft. Nach Ergänzung der FIT-Vorschau wurden die drei neuen Komponententests nochmals erfolgreich ausgeführt. Keine erneute vollständige Suite für diese UI-Änderung; die 595 Tests des vorherigen Builds 23 waren erfolgreich.

Die APK wurde von GitHub Actions gebaut und anschließend ausschließlich mit dem bisherigen Testschlüssel nachsigniert. Kein lokaler APK-Build.
App-Version weiterhin **0.7.31**, versionCode **69**. Als Update über bisherige mit diesem Testschlüssel signierte APKs installierbar. Kein zusätzlicher Gerätetest in dieser Umgebung.

Signatur und 16-KiB-ZIP-Ausrichtung geprüft.

Größe: 8,222,639 Bytes.

SHA-256: `dd67adb31bccc6b5b12b005dff5d204738170d609ee97b758bb02bb54f3fd7dd`

Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.
