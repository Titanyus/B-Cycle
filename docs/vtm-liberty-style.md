# Liberty als flacher VTM-Basisstil

Auftrag02.11.2026: Liberty als Basis, keine3D-Gebäude und grundsätzlich keine geneigte Kartendarstellung. Umsetzung gilt für Dashboard sowie Streckenvorschau/-vollbild.

Die Vektorquelle heißt in der Auswahl jetzt **OpenFreeMap · Liberty**; ihre gespeicherte Kennung `VECTOR` bleibt gleich. Keine zusätzliche Auswahloption/kein neuer Profil- oder Backupschlüssel. Rasterquellen bleiben unverändert.

`app/src/main/assets/styles/liberty-vtm.xml` ist eine VTM-Adaption: Liberty-Farbgebung für cremefarbenen Hintergrund, orange/gelbe große Straßen mit warmer Umrandung, weiße kleine Straßen, blaue Gewässer, grüne Landschaft und graue Gebäude; Geometrie-/Beschriftungsregeln aus dem passenden VTM0.25.0-OpenMapTiles-Theme. Die Darstellung ist keine direkte Ausführung der MapLibre-JSON-Datei und nicht pixelgleich: Zoomstaffelung, Linienbreiten, Schriftlayout und Symbole folgen weiter VTM. Nicht alle111 Liberty-MapLibre-Ebenen/-Ausdrücke werden nachgebildet. Der lokale XML-Stil kann später gezielt weiterentwickelt werden; kein Liveimport/Editor in diesem Auftrag.

Gebäude bleiben als flache Grundrisse ab Zoom13 sichtbar, auch bei höheren Zoomstufen und vorhandenen Höhenattributen. Extrusionsanweisungen vollständig entfernt; keine BuildingLayer. Native Kameraneigung zusätzlich auf min=max0 begrenzt, Tilt-Gesten bleiben deaktiviert und gesetzte Kamerapositionen weiterhin mit Tilt/Roll0 übernommen. Nord-/Fahrtrichtungsdrehung bleibt verfügbar. Optionale Radwegehervorhebung, Strecken und GPS behalten ihre bisherige Funktion.

Herkunft: Liberty-Referenz am02.11.2026 von `https://tiles.openfreemap.org/styles/liberty`, Original `https://github.com/maputnik/osm-liberty`; Hinweise/Lizenzen unter `Liberty-LICENSE.txt` und `Mapsforge-LICENSE.txt`, auch im Bibliothekslizenzdialog zugänglich. Keine neuen Kartenquellen oder Laufzeitabhängigkeiten.

Zwei gezielte Fälle erfolgreich: Liberty-Hintergrund/Wasser-/Autobahnfarbe, flache Gebäude einschließlich Höhe bei Zoom13/16/19, Kameraneigung/Roll0 bei erhaltener Richtung sowie echter MVT-Decoder mit optionaler Radwegehervorhebung. Direkte native Tilt-Sperre anschließend im betroffenen Fall geprüft. Logs `/workspace/.tools/vtm-liberty-tests.log`, `/workspace/.tools/vtm-liberty-flat-final.log`. Kompilation/Typografie/Atlas/Diff geprüft. Kein Gesamtlauf, keine APK; Lesbarkeit/Darstellung auf Android noch prüfen.

## Weitere vorgemerkte Stilwünsche

**Minimal:** Kleine Straßen und Feldwege ausblenden; große Straßen erhalten. Dazu Ortschaften, Gewässer, Waldflächen, eigene Position und ausgewählte Strecken. Noch nicht implementiert. OLED-dunkle Variante ebenfalls besprochen, keine Umsetzung in diesem Auftrag. Alle Varianten bleiben vorerst in Draufsicht.
