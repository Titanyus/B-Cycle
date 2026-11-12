# Nach Build156 noch unveröffentlicht

heart-pulse erneut aus originalem Lucide0.468.0 heruntergeladen und unverändert als Android-Vector übernommen. Vorherige handgezeichnete Pulslinie war zu groß. Beide Originalpfade und Stroke2/runde Enden geprüft. Keine neue APK beauftragt.

# Aktueller Stand: APK0.8.111 / Build156

Menüanpassungen einschließlich Einzüge/Icons umgesetzt und angeforderte APK veröffentlicht. Keine offenen Implementierungsaufträge. Tatsächlicher Raw-Direktdownload bytegleich zum Actions-Artefakt geprüft.

[APK](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.111-release-156.apk) · [Buildnachweis](build-156.md) · [Änderungen](release-0.8.111.md)

Actions43205213885 erfolgreich aus Quellstand4d2e5691baf5442e0dae411fda4216c9bd290b19, VersionCode181, Android13+. 4463689Bytes; SHA-2566816bf5541e899617c30a8cb0c55d495a98d2c8a3322a4c84496237facf4d81e. Dauerhafte Signatur bestätigt. Direktdownload bytegleich geprüft; Veröffentlichung518982c.

## Letzte Änderungen

Menü ohne Chevrons, Überschriften16dp/Menüzeilen8dp eingerückt, Innen-/Außenradius6dp. HRV: heart-pulse/chart-line/wrench, „Einstellungen“ verkürzt. Bisherige Zahnradrollen systemweit wrench. Acht gezielte Menü-/Navigationsmethoden grün, settings-menu-refine-tests.log.

Einstellungen: drei gerahmte TERTIARY-Kästen Bike/Training/HRV, innen feste SECONDARY-Spalten mit Rahmen/Trennern. Links Aktionen, rechts Einstellungen, Training rechts vorerst leer. Erweitert separat, Markenbild/Build erhalten. Training systemweit biceps-flexed, funktionale Leistungssymbole unverändert. Glow-Sticky-Kästen mit normalem2dp-Rahmen, Flat unverändert. Zehn gezielte UI-/Navigationsmethoden erfolgreich; ältere Testannahmen und Geräteprüfgrenzen im [Bericht](settings-boxes.md). Kein Profiling.

Dashboard-Graphen ohne seitliche Begrenzungslinien; Kurve/Füllung horizontal bis zum Kachel-/Bildschirmrand. Zahlenpadding nur für Labels, vertikale Geometrie und Intervalle erhalten. Drei native Methoden erfolgreich, [Details](dashboard-graph-edges.md).

## Bisheriger Stand und Grenzen

Build154: Bright als normalen Stil einmalig migrieren, dauerhaftes Löschen ohne virtuelle Wiederergänzung; allgemeine Beschriftungsgrößen/-gewichte für alle Textarten, echte Hausnummern32 im Quellfixture bestätigt, Anzeigegrenze14 statt17. Ebene standardmäßig aus. [Details](map-bright-typography-house-numbers.md). Nutzer kann eigene Stile als JSON zur Einbettung übergeben; bisher keine übermittelt.

Bisherige Gerätegrenzen weiterhin: Hausnummernanzeige/Dichte, Bosch-Ladeerkennung am Rad, alte Dubletten unverändert. Keine Sensor-/Aufzeichnungs-/GL-Lebenszyklusänderung durch diese UI-Aufträge. APK nur auf Auftrag über Actions, kein lokaler APK-Build/zusätzlicher Dispatch neben Push. Veröffentlichung/Downloadnachweis mit[skip ci].
