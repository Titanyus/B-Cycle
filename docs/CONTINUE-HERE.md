# Aktueller Stand: APK0.8.116 / Build161

Angeforderte APK veröffentlicht. Keine offenen Implementierungsaufträge. Tatsächlicher Raw-Direktdownload bytegleich zum Actions-Artefakt geprüft; Veröffentlichungfe1c85ed.

[APK](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.116-release-161.apk) · [Buildnachweis](build-161.md) · [Änderungen](release-0.8.116.md)

Actions43408245147 erfolgreich aus Quellstand5ff04778ea77ed1351b66fa9f5cb997acba35b71, VersionCode186, Android13+. 4645828Bytes; SHA-256ab8ff0e82335ccb3604f6a89a35fc8309af5e6f7cb59257000cc0b4a559c7286. Dauerhafte Signatur bestätigt, drei neue WebPs unverändert eingebettet.

## Letzte Änderungen

- Trainingshilfe in Dashboards & UI als Kombibox mit Zonenindikator und Trinkerinnerung.
- Zonenindikator mit unabhängigem Ein/Aus-Schalter; innen Aus/Alle Zonen/Zone2, „Trainingshilfe für“, Aktuelle Werte/Durchschnittswerte als Mehrfachbuttons, Erklärung zuletzt.
- Dieselben Bedienelemente in der Dashboard-Feldauswahl; bestehende Dashboard-/Profilzuordnung und Speicherung erhalten.
- Neue unveränderte WebP-Bilder für Bike, Training und HRV aus BC.zip.

Gezielte Trainingshilfe-/Trinkerinnerungs-/Speicherprüfungen erfolgreich (training-help-combobox-final-tests.log); Bilder und Kastenlayout in Flat/Glow bei288/360/480dp geprüft (settings-bc-artwork-tests.log), native Vorschauen angesehen. Zentrale Typografie-/Atlas-/Bright-/Diffprüfungen grün. Kein Profiling oder lokaler APK-Build. Ein zusätzlicher alter Routingtest im unveränderten DashboardTrainingHelp-Modell scheiterte im breiteren Erstlauf; keine grüne Gesamtsuite behauptet.

TrainingHelpView von Profil und Feldauswahl gemeinsam genutzt. Kein eigener Profil-Sticky-Abschnitt mehr. Neue Header-Switch/Segmentbuttons synchronisiert, Dropdown-Aufklappen aktiviert nichts. Letzten aktiven Modus nur innerhalb derselben Ansicht merken, beim Dashboardwechsel dessen gespeicherte Werte laden. Trinkerinnerung weiter profilbezogen. Ursprüngliche zwei großen PNGs durch drei Original-WEBPs aus BC.zip ersetzt; bei künftiger APK-Verifikation drei WEBPs berücksichtigen.

Vorheriger Build160 enthält zentrale PRIMARY-Footer über BicycleFooter.kt einschließlich Vollbildhost, ausschließlich Dashboard TERTIARY; FIT-Kombibox, Trainingsfooter-Schalter, Sortierung Stärke/Leicht/Sweet Spot/Hart und verborgene Variantenpille ohne Alternativen. Alles erhalten.

## Bisheriger Stand und Grenzen

Build154: Bright als normalen Stil einmalig migrieren, dauerhaftes Löschen ohne virtuelle Wiederergänzung; allgemeine Beschriftungsgrößen/-gewichte für alle Textarten, echte Hausnummern32 im Quellfixture bestätigt, Anzeigegrenze14 statt17. Ebene standardmäßig aus. [Details](map-bright-typography-house-numbers.md). Nutzer kann eigene Stile als JSON zur Einbettung übergeben; bisher keine übermittelt.

Bisherige Gerätegrenzen weiterhin: Hausnummernanzeige/Dichte, Bosch-Ladeerkennung am Rad, alte Dubletten unverändert. Keine Sensor-/Aufzeichnungs-/GL-Lebenszyklusänderung durch diese UI-Aufträge. APK nur auf Auftrag über Actions, kein lokaler APK-Build/zusätzlicher Dispatch neben Push. Veröffentlichung/Downloadnachweis mit[skip ci].
