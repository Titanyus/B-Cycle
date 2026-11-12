# Nach Build159 noch unveröffentlicht

Nutzerkorrektur: Dashboard-Footer samt Seitenbuttons wieder TERTIARY wie vor PRIMARY-Änderung. Ausdrücklich alle anderen Footer, einschließlich Streckenmanager/-details, bleiben PRIMARY in Flat/Glow; zentraler dashboard-Parameter trennt Rollen. Dashboard-Schattenlogik und Aufnahmebutton unverändert. Keine neue APK beauftragt.

# Aktueller Stand: APK0.8.114 / Build159

Angeforderte APK veröffentlicht. Keine offenen Implementierungsaufträge. Tatsächlicher Raw-Direktdownload bytegleich zum Actions-Artefakt geprüft; Veröffentlichung32da5eaa. Beide Originalbilder im APK auf identische Pixel geprüft.

[APK](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.114-release-159.apk) · [Buildnachweis](build-159.md) · [Änderungen](release-0.8.114.md)

Actions43293374243 aus Quellstanda058ed2429c6fc7da1b65ea08c63f5f20b407d6f, VersionCode184, Android13+. 7129637Bytes; SHA-256c532bef15f069fb1e1bf32903365cf700f40f896585295d410aef16ad52d3404. Dauerhafte Signatur bestätigt.

## Letzte Änderungen

- Bike und Training mit den gelieferten Originalbildern als vollbreiten, oben ausgerichteten Kastenhintergründen.
- Überschriftenbereiche aller drei Einstellungskästen35% höher; HRV weiterhin ohne Bild.
- Sensoren, Radwege/Radrouten und Backup-Gruppen Sichern/Wiederherstellen/Importieren/Cloud in gemeinsamen Boxen.
- Glow-Sticky-Innenabstand16dp und Standardabstand unter Trackdarstellung.
- Footer in Flat und Glow wieder durchgehend PRIMARY.

Gezielte Menü-/Navigations-/Trackmethoden erfolgreich, settings-group-image-tests.log sowie grouped-settings-expansion-final-tests.log, glow-box-spacing-tests.log und footer-primary-tests.log. Regression prüft globale page-Normalisierung ohne Löschen des ersten Kastenpaddings, vertikale Zentrierung sowie kurzen HRV-Kasten. Flat/Glow bei288/360/480dp. Zentrale Prüfungen grün. BicycleGroupedBox jetzt auch für Sensoren, Radwege/Radrouten und Backup-Gruppen. Native Bildvorschau geprüft; Original-PNGs aus ZIP unverändert übernommen. Ein zusätzlich gestarteter CyclingOptionsTest scheiterte vor UI-Prüfung an nativer Zoominitialisierung; Details settings-boxes.md.

Vorherige Änderungen aus Build155/156 weiter enthalten: drei zweispaltige TERTIARY/SECONDARY-Kästen, Markenbild/Build, biceps-flexed, wrench, keine Chevrons, Einzüge16/8dp; Glow-Sticky-Kontur und Graphen bis zum Rand. [Details](settings-boxes.md).

## Bisheriger Stand und Grenzen

Build154: Bright als normalen Stil einmalig migrieren, dauerhaftes Löschen ohne virtuelle Wiederergänzung; allgemeine Beschriftungsgrößen/-gewichte für alle Textarten, echte Hausnummern32 im Quellfixture bestätigt, Anzeigegrenze14 statt17. Ebene standardmäßig aus. [Details](map-bright-typography-house-numbers.md). Nutzer kann eigene Stile als JSON zur Einbettung übergeben; bisher keine übermittelt.

Bisherige Gerätegrenzen weiterhin: Hausnummernanzeige/Dichte, Bosch-Ladeerkennung am Rad, alte Dubletten unverändert. Keine Sensor-/Aufzeichnungs-/GL-Lebenszyklusänderung durch diese UI-Aufträge. APK nur auf Auftrag über Actions, kein lokaler APK-Build/zusätzlicher Dispatch neben Push. Veröffentlichung/Downloadnachweis mit[skip ci].
