# Aktueller Stand: APK0.8.115 / Build160

Angeforderte APK veröffentlicht. Keine offenen Implementierungsaufträge. Direktdownload nach Veröffentlichung noch prüfen.

[APK](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.115-release-160.apk) · [Buildnachweis](build-160.md) · [Änderungen](release-0.8.115.md)

Actions43301379105 aus Quellstand31e6f2db5dab263d886a74018591b633b0426af1, VersionCode185, Android13+. 7129785Bytes; SHA-25670d1473f5934fdf9852eb420062f1bf6dca315a2776d787c71d1c405b928db6f. Dauerhafte Signatur bestätigt.

## Letzte Änderungen

- Zentrale Footer-Gestaltung auch im Vollbildhost: alle Footer PRIMARY, ausschließlich Dashboard TERTIARY.
- Metadaten und Datenquellen unter der FIT-Speicherauswahl in gemeinsamer Kombibox, vorhandene Feldschalter erhalten.
- Trainingsfooter über Header-Button ein-/ausblendbar, initial verborgen.
- Sortierung Stärke als erste Option mit Leicht/Sweet Spot/Hart; Abschnitte nur noch als Kennzahl.
- Variantenpille bei Trainings ohne umschaltbare Varianten ausgeblendet.

Gezielte Menü-/Navigations-/Trackmethoden erfolgreich, central-footer-route-tests.log, fit-stored-group-final-tests.log, training-strength-footer-final-tests.log und training-variant-visibility-tests.log. Regression prüft globale page-Normalisierung ohne Löschen des ersten Kastenpaddings, vertikale Zentrierung sowie kurzen HRV-Kasten. Flat/Glow bei288/360/480dp. Zentrale Prüfungen grün. BicycleGroupedBox jetzt auch für Sensoren, Radwege/Radrouten und Backup-Gruppen. Native Bildvorschau geprüft; Original-PNGs aus ZIP unverändert übernommen. Ein zusätzlich gestarteter CyclingOptionsTest scheiterte vor UI-Prüfung an nativer Zoominitialisierung; Details settings-boxes.md.

Vorherige Änderungen aus Build155/156 weiter enthalten: drei zweispaltige TERTIARY/SECONDARY-Kästen, Markenbild/Build, biceps-flexed, wrench, keine Chevrons, Einzüge16/8dp; Glow-Sticky-Kontur und Graphen bis zum Rand. [Details](settings-boxes.md).

## Bisheriger Stand und Grenzen

Build154: Bright als normalen Stil einmalig migrieren, dauerhaftes Löschen ohne virtuelle Wiederergänzung; allgemeine Beschriftungsgrößen/-gewichte für alle Textarten, echte Hausnummern32 im Quellfixture bestätigt, Anzeigegrenze14 statt17. Ebene standardmäßig aus. [Details](map-bright-typography-house-numbers.md). Nutzer kann eigene Stile als JSON zur Einbettung übergeben; bisher keine übermittelt.

Bisherige Gerätegrenzen weiterhin: Hausnummernanzeige/Dichte, Bosch-Ladeerkennung am Rad, alte Dubletten unverändert. Keine Sensor-/Aufzeichnungs-/GL-Lebenszyklusänderung durch diese UI-Aufträge. APK nur auf Auftrag über Actions, kein lokaler APK-Build/zusätzlicher Dispatch neben Push. Veröffentlichung/Downloadnachweis mit[skip ci].
