# Maximalzoom und regelmäßigere Straßennummern

Aufträge nach Build 132 (04.11.2026): Gebäude/Flächen zeigen bei Zoom 19 invertierte/rechteckige Artefakte; Hauptstraßen sind mit Nummern gepflastert, teilweise gehäuft mit langen Lücken dazwischen.

## Geometriefehler

VTM OpenMapTiles overzoomt aus Quellstufe 14 bis Kamerastufe 19. Sein OverzoomDataSink beschneidet normale Linien/Polygone, verwendet für Gebäude/Gebäudeteile hingegen nur TileSeparator und skaliert anschließend das vollständige Polygon. Bei Faktor 32 können einzelne Koordinaten außerhalb der darstellbaren signed-short-Vertexkoordinaten landen (COORD_SCALE 8). Daraus können umschlagende/flächig falsche Geometrien entstehen.

`VtmClippedVectorSource` behält native HTTP-/MVT-/Overzoom-Funktionen. Der nachgeschaltete `VtmClippedVectorDataSource` beschneidet bei Overzoom nur Linien/Polygone, die nach der nativen Transformation den gepufferten Zielkachelbereich verlassen. Nativer TileClipper, gleicher 32×CanvasAdapter.getScale-Puffer. Bereits passend beschnittene Geometrien bleiben unverändert; Zoom ≤14 durchreichen. Tags/Typ/Layer und native completion/cancel/dispose erhalten. Es werden keine Quelle, Kamerastufe, Farben oder Styles ausgetauscht; beide optionalen Basispässe verwenden dieselbe Korrektur. App bleibt flache Draufsicht.

Native Regression reproduziert vor der Korrektur einen Gebäude-Überlauf und prüft danach für Gebäude und Landflächen die Stufen 15–19: endliche und packbare Koordinaten, ganzer sichtbarer Tilebereich weiter gefüllt, kein invertierendes Umschlagen; Quellstufe unverändert. Das beweist einen konkreten Fehlermechanismus, ersetzt aber noch nicht den Abgleich mit den tatsächlich betroffenen Straßenzügen am Gerät.

## Beschriftungsdichte und Verteilung

Für highway-shield-Nummern sowie Autobahn-/Fern-/Hauptstraßennamen (native Straßenpriorität 100–129) denselben Straßenschlüssel unabhängig von Stylevariante und Leerzeichen verwenden, etwa B16/B 16. Keine Zusammenlegung unterschiedlicher Straßennummern. Mindestabstand gleicher Kennung radial im Kartenbild, mindestens 220 Canvas-dp oder ausreichend Platz für sehr breite Texte. Damit gilt derselbe Abstand für waagrechte, senkrechte und schräge Straßen; der frühere schmale vertikale Duplikatkorridor entfällt für diese Fälle. Bildschirmabstand unabhängig von Zoom, statt mehr Schilder pro sichtbarem Bereich mit jeder feineren Tile-Stufe zuzulassen.

Neue Kandidaten (Schilder und Wegtext) gemeinsam über den Viewport sammeln. Native Priorität zuerst, für noch nicht vertretene Straßen Kandidat nahe der Kartenmitte. Danach jeweils die größte verfügbare Lücke derselben Kennung bevorzugen. Deterministische Tie-Breaks über Kennung/Koordinate/Stil; bei gleichaltrigen gleichen Straßen behält der bereits ausgewählte Kandidat Vorrang. So setzen Tile-Lade- und Segmentreihenfolge keine zufälligen Häufungen. Vorhandene sichtbare Namen und deren Alter erhalten, Orts-/Gewässerbeschriftungen behalten eigene Regeln. Begrenzter Namenscache pro Platzierungsrunde; keine Änderungen an der originalen Bright-Vorlage oder Font-Atlas-Rasterung.

Ein gleichmäßiges wiederholtes Label an einer beliebigen Stelle lässt sich aus fragmentierten Quelldaten nicht erzwingen: Es werden weiterhin nur vorhandene geeignete Straßenkandidaten verwendet, Kollisionen mit Ortsnamen und anderen Beschriftungen bleiben berücksichtigt. Die Auswahl verbessert die Verteilung, ohne die Straße oder deren Verlauf aus einer Nummer zu erfinden.

## Referenzen

Öffentliche Projekte und Codequellen am 04.11.2026 geprüft:

- [Trekarta](https://github.com/andreynovikov/trekarta): Open-Source-Outdoor-App (GPL-3.0) mit eigenem [VTM-Fork](https://github.com/andreynovikov/vtm) (LGPL-3.0). Dessen LabelPlacement verwendet ebenfalls die grundlegende VTM-Logik; keine fertig nachgewiesene regelmäßige Straßenlabel-Lösung behaupten.
- [Offizielle VTM Android-Beispiele](https://github.com/mapsforge/vtm/tree/master/vtm-android-example): native Layer-/Kartenintegration, passend zur verwendeten Bibliotheksfamilie.
- [Cachebox 3.0](https://github.com/Longri/cachebox3.0): offene Geocaching-Anwendung, laut [offizieller VTM-Anwendungsliste](https://github.com/mapsforge/vtm/blob/master/docs/Applications.md) Apache 2.0. Interessante weitere Integrationsreferenz; dessen Labelqualität nicht als gemessen behaupten.

Keine fremde App kopiert oder Kartenengine gewechselt. Ergänzung des eigenen Platzierungsverfahrens; native Clipping-Klassen aus bereits eingebundenem LGPL-VTM verwenden.

## Gezielte Prüfung

Overzoom-/signed-short-Regression plus vorhandene native Platzierungs-/Fadefälle grün. Zusätzlicher Fall mit dichtem Anfang und weiteren Kandidaten entlang derselben Straße ergibt gleichmäßige 220-Pixel-Abstände; Auswahl bleibt bei umgekehrter Eingabereihenfolge und 90° Kartenrotation identisch. Typografie/Bright-Generator/Diff grün. Kein Gesamtlauf, keine lokale APK, kein neuer APK-Auftrag. Gerätevergleich für die gemeldete Maximalzoom-Szene und reale Straßendichte noch offen.

## Vektor-Zoom21 nach Build139

Expliziter Folgeauftrag: Vektorkarte zwei volle Zoomstufen näher, Rastergrenzen erhalten. MapZoom.maximum21, MapSource.VECTOR.maximumZoom21, native Vektorquelle zoomMax aus dieser Quelle; tatsächliche Datenweiterverwendung ab Quellzoom14 unverändert. Kamera, Pinch, Buttons, gespeicherte Werte und Streckenvorschau verwenden weiterhin MapZoom.maximum(source). Raster19, OpenTopoMap17. Native Clipping-Regression auf15–21 erweitert und erfolgreich. MapPresentationSettingsTest prüft reale Kameragrenzen21/19/17 und gespeicherten20,5-Wert je Quelle.

## Basiskarten-Ausfall bei Zoom20/21 nach Build142

Gemeldetes Verhalten: Basiskarte verschwindet in den höchsten Stufen, separate Radrouten bleiben sichtbar; beim Herauszoomen kommt die Karte zurück. Ursache konkret reproduziert: Die Kamera und Vektorquelle erlauben21, aber `build-bright-vtm.py` erzeugte Bright-Zeichenregeln nur für0..19 (`range(0,20)`). Native `matchElement` liefert für20/21 keine Flächen-/Straßen-/Gebäudestile, während der unabhängig konfigurierte Radroutenlayer weiterzeichnet. Der vorherige reine Geometrie-Clipping-Test konnte diese fehlende Theme-Abdeckung nicht erkennen.

Korrektur: Der Generator liest die maximale Stufe aus `MapZoom.maximum` und wertet die originalen Bright-Funktionen samt Min-/Maxzoom bis einschließlich dieser Stufe aus. Kein pauschales Strecken alter Regeln, keine neue Kamera-/Rastergrenze und kein Wechsel der Datenquelle. Erzeugtes XML und Mapping neu generiert; die Original-JSON bleibt unverändert. Die zusätzlichen Stufen benötigen zwei weitere vorhandene Einbahnstraßen-Spritevarianten (113 statt111).

Regression `VtmOverzoomGeometryTest.brightBaseLayersKeepTheirNativePaintsThroughTheMaximumCameraZoom`: sechs repräsentative Flächen-/Linienklassen (Wohnfläche, Gras, Wasser, Gebäude, Hauptstraße, Feldweg) im echten nativen Bright-Theme auf19..21. Vorher gezielt fehlgeschlagen bei landuse/residential, Zoom20 (`bright-maxzoom-before-test.log`). Nachher beide Tests der Klasse grün (`bright-maxzoom-fix-tests.log`), inklusive vorhandener nativer Overzoom-/Short-Clipping-Prüfung15..21. Bright-Generator/Typografie/UI-Katalog/Diff grün. Kein Gesamtlauf, keine neue APK beauftragt; optischer Gerätevergleich offen.
