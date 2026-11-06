# Startbahnflächen und Mittellinien (06.11.2026, nach Build143)

Nutzerbefund: Start-/Landebahnen über alle Zoomstufen übermäßig lang/breit; weiße Pillenform mit grauem Rand bei Weißenhorn, Gundelfingen und München. OSM zeigt Weißenhorn als Rechteck.

## Originaldaten und Befund

Öffentliche OpenFreeMap-Tiles aus TileJSON-Version20261101_080013_pt geladen, für den reproduzierbaren Test ausschließlich die originale `aeroway`-Layer-Protobufsequenz unverändert übernommen. Keine synthetischen Startbahnkoordinaten und keine privaten Streckendaten.

- Weißenhorn13/4326/2837 und14/8653/5675: class=runway ausschließlich LineString (ref08/26), keine Startbahnfläche in diesen Vektordaten. Die separate aerodrome-Fläche bezeichnet das Flugplatzgelände. Eine in der OSM-Standardkarte sichtbare Rechteckfläche ist damit nicht automatisch als Fläche in dieser Vektorquelle vorhanden.
- Gundelfingen13/4331/2828: runway-Polygon und -Mittellinie (10/28).
- München13/4364/2835: zwei runway-Polygone plus Mittellinien, zahlreiche taxiway-Linien.

Die Bright-Vorlage gibt Startbahnlinien breite, runde Enden (bis50px Weiß/55px Umrandung vor VTM-Halbbbreitenumrechnung). Deren breite Casing-Filter schließen auch Polygongeometrien ein. In der App führte dies sowohl zu optisch verlängerten/aufgeblasenen Mittellinien als auch zu einer überbreiten Umrandung echter Flächen. Eine angenommene reale Bahnbreite aus der Mittellinie ist nicht ableitbar.

## Korrektur

Original-Bright-JSON unverändert. Beauftragte VTM-Adaption im Generator: runway-/taxiway-Linien schmale Marker mit geraden Enden (`butt`); maximale native logische Halbbbreiten für runway1,25 Umrandung/0,75 Weiß, taxiway0,75/0,5. Die native Mindestbreite wird weiterhin berücksichtigt. Das ist eine Darstellung der Mittellinie, keine erfundene Meter-Bahnbreite.

Echte Flugplatzpolygone behalten ihre Quellgeometrie, deckende ursprüngliche weiße Füllung und nur eine dünne graue0,35-Kontur. Keine ausgreifende Mittellinien-Casingbreite um die gesamte Fläche. VtmBrightTheme lässt die aeroway-Line-Stile ausschließlich auf LineString-Geometrien zu. Damit werden Rechteckflächen nicht durch zusätzliche breite Linien aufgebläht; auch niedrige Zoomstufen behalten vorhandene Flächen statt unsichtbarer Füllung mit dicker Umrandung. Keine Änderung von Positionskoordinaten, Quelle, Zoomgrenzen, übrigen Straßen-/Track-/Beschriftungsstilen oder Ebenenreihenfolge.

Generator/XML/Ebenenbericht zusammen aktualisiert,113Spritevarianten unverändert. Beide optionalen Basispässe verwenden dieselbe Darstellung.

## Gezielte Prüfung

AirfieldGeometryTest dekodiert die vier Originaldatenausschnitte mit dem produktiven MVT-Decoder: bei13..21 native Zeichenregeln, mindestens drei reale runway-Polygone und mehrere Mittellinien, deckende Flächen ohne LineStyle-Zugabe, `BUTT` und begrenzte Linienbreiten; Punkte vor/nach Theme-Zuordnung identisch. Zusammen mit beiden bisherigen VtmOverzoomGeometryTest-Fällen erfolgreich (`airfield-footprints-final-tests.log`). Typografie/UI-Katalog/Bright-Generator/Diff grün. Kein Gesamtlauf und keine APK beauftragt. Ein tatsächlicher Gerätevergleich bleibt offen.
