# Schaltbare Vektorkartenebenen

Auftrag 04.11.2026 nach Build 129. Fahrradprofil → Karten → Kartenebenen: gemeinsames Rahmen-Accordion, darin rechte native Schalter für große Straßen, kleine Straßen, Feld-/Fuß-/Radwege, Gebäude, Wald/Wiesen/Flächennutzung, Gewässer, Ortsnamen, Straßennamen/-nummern, POIs/Einrichtungen, Gemeinde-/Ortsteilgrenzen und Höhenlinien. Die bisherige Radwegehervorhebung bleibt separat erhalten.

## Verhalten und Speicherung

Bisherige Defaults unverändert: Kartengrundlagen/Namen an, POIs und lokale Grenzen aus, neue Höhenlinien aus. Übergeordnete Grenzen bleiben erhalten. Straßenklassen betreffen gleichzeitig ihre Geometrie und zugehörigen Namen/Schilder; der Namensschalter wirkt zusätzlich unabhängig auf alle Straßen. Wasser inklusive Wasserbeschriftung; Naturflächen inklusive Landnutzung/Parks. Gleise und sonstige Verkehrselemente werden nicht versehentlich als kleine Straßen behandelt.

`MapVectorLayers` ist ein unveränderlicher Snapshot. `VtmBrightTheme` filtert vor nativer Stilzuordnung; keine Geometrie-/JSON-Dekodierung beim Zeichnen. Umschalten ersetzt die Basis-Layer über den vorhandenen Lebenszyklus, erhält MapView/Kamera, GPS und Track-Layer. Rasterquellen bleiben unverändert; das Accordion ist dort ausgeblendet. Streckendetailkarten verwenden dieselben Profileinstellungen.

Die Boolean-Keys `mapVectorLayer_ENUMNAME` gehören zum Fahrradprofil, zu Profilkopie/-wechsel und zur Einstellungssicherung. Öffentlicher MAPS-Export unter `vectorLayers`, strikte Boolean-Validierung dort und im Profilbank-Import. Alte Sicherungen ohne neue Felder bleiben lesbar.

## Höhenlinien

OpenFreeMap/OpenMapTiles enthält in unserer Quelle keine Höhenlinien. Zusätzliche öffentliche [AWS Terrain Tiles / Mapzen Terrarium](https://registry.opendata.aws/terrain-tiles/) werden deshalb nur bei aktiviertem Layer geladen. Kein Schlüssel erforderlich. RGB-Auswertung `R*256+G+B/256−32768` in Metern, transparente/ungültige Werte als Datenlücken. Nur Kartenzeichnung; keine Höhenkorrektur, Änderung von GPS-/Fahrdaten oder Sensordrosselung.

Native zusätzliche VTM-VectorTileLayer unter den Namen und Strecken, ein Tile-Worker. DEM-PNG-Dekodierung und Marching Squares auf diesem Worker, keine Berechnung auf UI-/GL-Thread. Zusammenhängende Linien statt einzelner Zellsegmente, definierte Sattelauflösung, gemeinsame Samples am Ost-/Südrand für passende Kachelanschlüsse. 100-m-Hauptlinien etwas stärker, übrige Linien dünner und transparent braun. Noch keine zusätzlichen Höhenzahlen an den Linien.

VTM-Zoom 10: 100-m-Abstand, 11: 50 m, 12: 20 m, ab 13: 10 m. Native DEM-Kacheln bis Zoom 13, darüber vorhandene Geometrie vergrößern statt immer feinere Downloads; darunter Layer unsichtbar. Der tatsächliche Abstand folgt der gerade geladenen nativen Tile-Stufe, auch während des Zoomübergangs. Unter Zoom 10 keine neuen Terrain-Anfragen. Die Linien sind eine Interpolation aus Rasterhöhen, kein Vermessungsprodukt.

Begrenzter gemeinsamer HTTP-Cache 64 MiB, maximal 16 dekodierte DEMs (4 MiB) pro aktivem Höhenlinien-Layer. Höchstens ein Request/Worker gleichzeitig; benachbarte Kacheln teilen ihre DEMs. Pause/Detach und Abbruch bleiben im nativen Lebenszyklus, laufende HTTP-Anfragen werden bei Dispose abgebrochen. Fehlerhafte/fehlende Terrain-Kacheln lassen die normale Karte bestehen. Quellen-/Lizenzhinweise lokal in `assets/Terrain-ATTRIBUTION.txt`, erreichbar im Karten-Accordion und den Bibliothekshinweisen; bei aktivem Layer zusätzliche Kartenattribution „Mapzen Terrain“.

## Gezielt geprüft

Drei Fälle: tatsächliches Bright-Theme für alle schaltbaren Quellgruppen/Defaults und keine verwaisten Straßennamen, Profilwechsel plus MAPS-Sicherungsrundreise/strikte Typprüfung, echte Einstellungsoberfläche mit Layeranordnung/Ein-/Aus/Rasterwechsel. Terrarium-/Marching-Squares-Prüfung darin mit zusammenhängenden Linien, exakten Nachbarkachel-Anschlüssen, Cachewiederverwendung und Datenlücken. Keine zusätzlichen Gesamt-Testläufe oder lokale APK.

Öffentliche Probe `13/4330/2826.png` erfolgreich geladen/dekodiert: 464,7–535,2 m, bei 10-m-Abstand 26 verbundene Linien mit 3676 Punkten. Lokaler JVM-Lauf rund 62 ms auf Cloud-Hardware, ausdrücklich keine Smartphone-Messung. Gerätebild, Netzausfallverhalten im realen Kartengebrauch und Zoom-Lesbarkeit noch beurteilen. Original-Bright-JSON/XML/Sprites unverändert, Generator-/Typografie-/UI-Katalog-Prüfungen grün.

## Direktzugriff nach Build 130

Auf der Dashboardkarte neben dem Auge ein Ebenenbutton mit Layers-Icon. Öffnet ein natives scrollbares Dropdown mit denselben Profil-Keys, einschließlich Radwegehervorhebung. Schalter wirken sofort, Dropdown bleibt zum Vergleichen offen; nur Basislayer wechseln, MapView/Kamera/GPS/Strecken bleiben bestehen. Bei Rasterquelle ausgeblendet. Editor/Profilwechsel/Pause schließen das Dropdown; automatische Bedienelement-Ausblendung hält bei offenem Dropdown an. Gemeinsame Kartenbutton-Geometrie und Footer-Inset-Logik erhalten. Gezielter tatsächlicher Popup-/Schaltertest mit Live-Map-Erhalt und Rasterwechsel grün.
