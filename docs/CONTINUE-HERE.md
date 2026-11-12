# Nach Build157 noch unveröffentlicht

Menü: Strecken/Nachrichten, bodyTypeface14sp und Icons24dp wie Rahmen-Dropdowns; Gruppenüberschriften erhalten. Flat: Hauptgruppen4dp seitlich/16dp innen, Standard-Sticky-Köpfe4dp seitlich und Inhalt16dp eingerückt; Glow unverändert. Trackdarstellung: vier originale Rahmen-Accordions in gemeinsamem SECONDARY-Kasten mit3Trennern, ohne Einzelrahmen/Zwischenmargins; Schalter/IDs/Callbacks erhalten. Radwege/Radrouten separat. [Details](settings-boxes.md). Keine neue APK beauftragt.

# Aktueller Stand: APK0.8.112 / Build157

Angeforderte APK veröffentlicht. Keine offenen Implementierungsaufträge. Tatsächlichen Raw-Direktdownload geprüft, bytegleich zum Actions-Artefakt. Veröffentlichung29dd0ab.

[APK](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.112-release-157.apk) · [Buildnachweis](build-157.md) · [Änderungen](release-0.8.112.md)

Actions43210505910 aus Quellstandd5aac98deb695430b465de66fe469e0252fee042, VersionCode182, Android13+. 4463721Bytes; SHA-25619bc2a34d4a90a8a959b845099efc6b817943cdf3b820288c1df1a4c90410d00. Dauerhafte Signatur bestätigt.

## Letzte Änderungen

- Originales Lucide-heart-pulse statt zu großer Pulslinie.
- Gleicher oberer Innenabstand aller drei Einstellungsgruppen, einschließlich Bike.
- Gruppen-Icons20% größer, Titel in Hauptheadergröße32sp und zwischen Außenrahmen/Innenkasten vertikal zentriert.
- Abgerundete Füllung der inneren Kästen, Menüzeilen-Icons20% kleiner.
- HRV-Einstellungen rechts als einzeiliger, oben ausgerichteter Kasten.

Acht gezielte Menü-/Navigationsmethoden erfolgreich, settings-box-final-layout-tests.log. Regression prüft globale page-Normalisierung ohne Löschen des ersten Kastenpaddings, vertikale Zentrierung sowie kurzen HRV-Kasten. Flat/Glow bei288/360/480dp. Zentrale Prüfungen grün. Originales heart-pulse von Lucide0.468.0, beide SVG-Pfade unverändert übernommen.

Vorherige Änderungen aus Build155/156 weiter enthalten: drei zweispaltige TERTIARY/SECONDARY-Kästen, Markenbild/Build, biceps-flexed, wrench, keine Chevrons, Einzüge16/8dp; Glow-Sticky-Kontur und Graphen bis zum Rand. [Details](settings-boxes.md).

## Bisheriger Stand und Grenzen

Build154: Bright als normalen Stil einmalig migrieren, dauerhaftes Löschen ohne virtuelle Wiederergänzung; allgemeine Beschriftungsgrößen/-gewichte für alle Textarten, echte Hausnummern32 im Quellfixture bestätigt, Anzeigegrenze14 statt17. Ebene standardmäßig aus. [Details](map-bright-typography-house-numbers.md). Nutzer kann eigene Stile als JSON zur Einbettung übergeben; bisher keine übermittelt.

Bisherige Gerätegrenzen weiterhin: Hausnummernanzeige/Dichte, Bosch-Ladeerkennung am Rad, alte Dubletten unverändert. Keine Sensor-/Aufzeichnungs-/GL-Lebenszyklusänderung durch diese UI-Aufträge. APK nur auf Auftrag über Actions, kein lokaler APK-Build/zusätzlicher Dispatch neben Push. Veröffentlichung/Downloadnachweis mit[skip ci].
