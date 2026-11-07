# Gemeinsame Höhenquelle und Schummerung

Auftrag07.11.2026: Höhenlinien und nachträgliche Fahrt-Höhenkorrektur auf Mapterhorn vereinheitlichen; geeignete Höhenkacheln und extern vorberechnete dezente Schummerung prüfen. Keine neue APK beauftragt. Anschließender Auftrag zur Online-Schummerung inzwischen umgesetzt: [Schummerung](vtm-hillshade.md).

## Umsetzung

`MapterhornTiles` lädt/validiert Terrarium-WebP512 und hält für beide Verbraucher einen gemeinsamen 64-MiB-Plattencache (30Tage) sowie 8MiB dekodierte Float-Höhenwerte. Ein gemeinsamer cancellierbarer Download-Lock verhindert doppelte parallele Downloads. Keine Bitmap bleibt dauerhaft erhalten. Eigene Endpunkte sind im Cache getrennt. Fehler/NoData werden nicht zu Höhe0; fehlende Kacheln werden kurz pro Verbraucher gemerkt. Bestehender Produktionscache `mapterhorn-v1` weiterverwendet.

- Fahrtkorrektur: unverändert bilinear Zoom14, bei404 Zoom12, maximal256Anfragen/3Minuten. Originaldatei/Atomizität/Höhenmeterzählung unverändert.
- Höhenlinien: bisherige AWS/Mapzen-PNG256-Quelle durch denselben Mapterhorn-Zugriff ersetzt. Nativ Zoom10–14; darüber Geometrie weiterzeichnen. Aus512Pixeln wird mit Schrittweite2 ein257×257-Zeichenraster einschließlich gemeinsamer Ost-/Südränder; keine vierfache Marching-Squares-Zellzahl. Regionale Detail-404 fallen auf globalen Zoom12 zurück. Fehlende/kaputte Kacheln betreffen nur das optionale Overlay.
- Linienabstände100/50/20/10m und Ebenenreihenfolge Gelände → Höhenlinien → Straßen/Gebäude → Namen → Tracks/GPS erhalten. Profiloption/Defaultaus/Backup unverändert. Gemeinsame Quelle bedeutet gemeinsame Eingangshöhen, keine Änderung aufgezeichneter Sensorwerte beim Kartenzeichnen.
- Zentrale Kartenattribution und lokales Quellenverzeichnis jetzt Mapterhorn; ursprüngliche Datenlizenzen aus dem offiziellen Katalog mit übernommen.

## Geeignete Höhenkacheln

Offizielle Dokumentation: https://mapterhorn.com/data-access/ ; TileJSON: https://tiles.mapterhorn.com/tilejson.json . Verifiziert am07.11.2026: XYZ, WebMercator,512Pixel, Terrarium, verlustfreies WebP. Endpunkt `https://tiles.mapterhorn.com/{z}/{x}/{y}.webp`.

Die Dokumentation nennt globales Archiv0–12 und regionale Detailarchive13–17. Das TileJSON nennt keine Zoomgrenzen;17 ist deshalb keine Zusage weltweiter Detailabdeckung. Hohe Zoomstufen verbessern weder unbekannte Quellauflösung noch Geländeaktualität automatisch.

Rasterweite bei48,3°N (berechnet, nicht vermessen):

| Datenzoom | Native512-Pixel-Rasterweite | Verwendung |
| --- | --- | --- |
|12|ca.12,7m|globaler Rückfall, Übersicht|
|13|ca.6,4m|mittlere Darstellung|
|14|ca.3,2m|Fahrtkorrektur, detaillierte Höhenlinien|

Linien-Zeichenraster bei14 mit Schrittweite2 ca.6,4m. Gröbere Darstellung spart Arbeit; Korrektur verwendet weiterhin die volle Rasterauflösung. Kein zusätzlicher Download auf15–17 allein wegen Karten-Überzoom. Das ist für unsere visuelle Orientierung angemessen; kein Vermessungsprodukt.

Reale Bayern-Stichproben erfolgreich:14/8660/5674.webp (47018Bytes),12/2165/1418.webp (66636Bytes), jeweils512×512RGB. Der offizielle Quellenkatalog enthält für Bayern DGM1 der Bayerischen Vermessungsverwaltung, CC BY4.0, Quellraster1m. Daraus folgt **keine** pauschale1-m-Auflösung aller ausgelieferten Kacheln. Quellkatalog: https://download.mapterhorn.com/attribution.json ; Attribution: https://mapterhorn.com/attribution . Mapterhorn-Softwarelizenz ersetzt die regionalen Datenlizenzen nicht.

## Extern vorberechnete Schummerung

Mapterhorn liefert hier Höhenwerte, keine fertig schattierten Bildkacheln. Gemeinsame DEM-Quelle ist umgesetzt; fertig bezogene Schummerung darf unabhängig davon bleiben. Sie verändert keine Fahrt-Höhenwerte.

Technisch geprüfter Kandidat: **Esri World Hillshade**, ein fertiger globaler Geländehintergrund ohne Straßen/Ortsnamen. WebMercator,256×256JPEG, URL mit **Z/Y/X** statt Z/X/Y:

`https://server.arcgisonline.com/arcgis/rest/services/Elevation/World_Hillshade/MapServer/tile/{z}/{y}/{x}`

Metadaten: https://server.arcgisonline.com/arcgis/rest/services/Elevation/World_Hillshade/MapServer?f=pjson . Reale Probe12/1418/2165 (7626Bytes) erfolgreich,256×256RGB, visuell dezentes Relief. Metadaten nennen23Kachelstufen, aber tatsächliche Abdeckung etwa1:72000 global und bis etwa1:9000 in Teilen Europas/USA. Das ist keine weltweit gleichmäßige Detailauflösung.

Für Bright geeignet als schwache Schattenebene oberhalb der Grundflächen und unterhalb von Straßen/Gebäuden/Beschriftungen. JPEG ist deckend: niedrige normale Deckkraft würde Flächen auch aufhellen. Für saubere Farben wäre eine einmalige Weiß→transparent-/Schattenalpha-Aufbereitung auf dem Bildworker oder ein geeigneter Multiply-Pass sinnvoll; keine Höhen-/Hangberechnung im laufenden Kartenbetrieb. Implementierung noch offen.

**Nutzungsgrenze vor Einbau:** öffentliche Erreichbarkeit ist keine pauschale freie Offline-/Weiterverteilungserlaubnis. Das offizielle Element verweist auf Esri Master License Agreement und schließt Kachelexport für Offline für diesen Dienst aus; dafür gibt es ein getrenntes forExport-Angebot. Deshalb aktuell kein neuer Anbieter, keine Cache-/Offline-Zusage und kein Esri-Layer eingebaut. Zentrale Attribution müsste vor Aktivierung sämtliche Dienst-Credits ergänzen.

Offizielles Element mit Nutzungsbedingungen:
https://www.arcgis.com/sharing/rest/content/items/1b243539f4514b6ba35e7d995890db1d?f=json

Ergebnis: Mapterhorn512/Zoom14 mit Rückfall12 für gemeinsame Höhenwerte verwenden. Esri ist optisch/technisch für fertige Schummerung geeignet, aber vor Produktintegration Lizenz-/Cache-Passung klären. Kein unbestätigter MapTiler-Hillshade-Endpunkt zugesagt.

## Prüfung

Gezielte lokale Tests: `MapterhornElevationsTest`, `MapVectorLayersTest`, `MapPresentationSettingsTest`, `FitElevationCorrectionTest`. Echte verlustfreie512-WebP-Fixtures, Terrarium/Bilinear/Kachelränder, zusammenhängende Linien und identische Nachbarränder, wiederholter Cachezugriff, gemeinsame Nutzung durch beide Verbraucher, regionaler404→12-Rückfall, Fehler/Abbruch/Dispose, vorhandene FIT-Korrektur und Quellenanzeige. Kein Gesamtlauf, kein lokaler APK-Build. Reale Anbieter-Stichproben wie oben; Gerätetest der Linien über Zoomwechsel offen.

Aktualisierung nach anschließendem Nutzerauftrag07.11.2026: Esri als optionale Online-Schummerung eingebaut, ohne Diskcache/Offline-Export; vollständige Quelle/Lizenz in Erweitert. Die obige Prüfung beschreibt die damalige Auswahl, aktuelle Implementierung unter [Schummerung](vtm-hillshade.md).
