# Neue Menüanpassungen nach Build155 (noch unveröffentlicht)

Chevrons aus Einstellungsmenüpunkten entfernt, Gruppenüberschriften16dp eingerückt, Menüzeilen8dp links. Innen-/Außenkästen verwenden denselben zentralen6dp-Radius. HRV: heart-pulse für Messung, chart-line für Auswertung, Einstellungen mit wrench. Bisherige Zahnradrollen Advanced/Work systemweit wrench. Neue APK0.8.111/code181 beauftragt; Actions-Build156 folgt.

# Aktueller Stand: APK0.8.110 / Build155

Neue Einstellungsaufteilung, Glow-Sticky-Rahmen und zuvor vorbereitete Graphkanten umgesetzt und angeforderte APK veröffentlicht. Keine offenen Implementierungsaufträge. Tatsächlicher direkter Raw-Download geprüft, bytegleich zum Actions-Artefakt.

[APK](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.110-release-155.apk) · [Buildnachweis](build-155.md) · [Änderungen](release-0.8.110.md)

Actions43202153928 erfolgreich aus Quellstand8b821fb20bd84f1bff1da9735b558372cf3535ef, VersionCode180, Android13+. 4462661Bytes; SHA-256123d5e8f9c3d22a2cb28c1053e9896d0237793209360acd255637c5eea565c3e. Zertifikat SHA-25694d34c46ede28d12e6d37efd8a7d953a75b621596ec2e5b68f61899b8ea7499b. Artefakt und Raw-Direktdownload bytegleich geprüft; Veröffentlichung8957741.

## Letzte Änderungen

Einstellungen: drei gerahmte TERTIARY-Kästen Bike/Training/HRV, innen feste SECONDARY-Spalten mit Rahmen/Trennern. Links Aktionen, rechts Einstellungen, Training rechts vorerst leer. Erweitert separat, Markenbild/Build erhalten. Training systemweit biceps-flexed, funktionale Leistungssymbole unverändert. Glow-Sticky-Kästen mit normalem2dp-Rahmen, Flat unverändert. Zehn gezielte UI-/Navigationsmethoden erfolgreich; ältere Testannahmen und Geräteprüfgrenzen im [Bericht](settings-boxes.md). Kein Profiling.

Dashboard-Graphen ohne seitliche Begrenzungslinien; Kurve/Füllung horizontal bis zum Kachel-/Bildschirmrand. Zahlenpadding nur für Labels, vertikale Geometrie und Intervalle erhalten. Drei native Methoden erfolgreich, [Details](dashboard-graph-edges.md).

## Bisheriger Stand und Grenzen

Build154: Bright als normalen Stil einmalig migrieren, dauerhaftes Löschen ohne virtuelle Wiederergänzung; allgemeine Beschriftungsgrößen/-gewichte für alle Textarten, echte Hausnummern32 im Quellfixture bestätigt, Anzeigegrenze14 statt17. Ebene standardmäßig aus. [Details](map-bright-typography-house-numbers.md). Nutzer kann eigene Stile als JSON zur Einbettung übergeben; bisher keine übermittelt.

Bisherige Gerätegrenzen weiterhin: Hausnummernanzeige/Dichte, Bosch-Ladeerkennung am Rad, alte Dubletten unverändert. Keine Sensor-/Aufzeichnungs-/GL-Lebenszyklusänderung durch diese UI-Aufträge. APK nur auf Auftrag über Actions, kein lokaler APK-Build/zusätzlicher Dispatch neben Push. Veröffentlichung/Downloadnachweis mit[skip ci].
