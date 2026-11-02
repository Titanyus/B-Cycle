# Online-Vektorkarte (02.11.2026)

Auftrag: Neben den bisherigen Rasterkarten eine echte Vektorkarte einbauen; normale OSM-Grundkarte und zuschaltbare Fahrradkennzeichnungen ermöglichen.

## Bedienung und Umfang

In Fahrradprofile → Karten ist zusätzlich **OpenFreeMap · Vektorkarte** auswählbar. Kein Konto/API-Schlüssel nötig. Bei dieser Quelle erscheint **Radwege hervorheben**, zunächst aktiv. Auswahl und Schalter werden je Fahrradprofil gespeichert und über den bestehenden Einstellungsbackupbereich Karten gesichert. Bisherige Rasterquellen und deren Schlüssel bleiben erhalten. Die bestehende Kartenauswahl wird nicht ungefragt umgestellt.

Die Grundkarte nutzt OSM-Daten im OpenMapTiles-Schema und das gebündelte VTM-OpenMapTiles-Theme. Das ist eine lokal gezeichnete Vektorkarte, keine Kopie der fertigen Standard-OSM-Rasterkacheln und kein MapLibre-JSON-Style. Hervorgehoben werden `transportation/subclass=cycleway` sowie Wege/Fußwege/Tracks mit `bicycle=designated|official`, ab Zoom12. Ohne Hervorhebung bleiben normale Wegesignaturen sichtbar. Nicht jeder für Fahrräder erlaubte Weg ist ein Radweg. Ausgeschilderte Radroutennetze sind damit noch keine eigene vollständige Routenebene; dafür wäre eine passende zusätzliche Datenquelle bzw. Schemaauswertung nötig.

## Technik

- VTM0.25.0 um `vtm-mvt` und `vtm-themes` ergänzt; MVT-Dekodierung durch VTM/Mapbox-Vector-Tile/JTS. Stille SLF4J-Anbindung für den Reader; eng begrenzte R8-Regel erhält die protobuf-Deskriptormethoden des MVT-Schemas.
- OpenFreeMap-TileJSON unter `https://tiles.openfreemap.org/planet`; datierte Kachel-URL wird auf VTM-Ladethreads ermittelt und stündlich erneuert. Keine Netzwerkabfrage auf dem UI-Thread, keine fest eingebaute Datensatzversion. HTTPS/Host/Pfad des gelieferten Templates validieren. Bei Netzausfall vorhandene Metadaten/HTTP-Cache nutzen; das ist kein vollständiges Offlinekartenpaket.
- Gemeinsamer begrenzter64-MB-HTTP-Cache, höchstens zwei Kachellader pro Karte. Quelle liefert bis Zoom14, VTM übernimmt Geometrie-Overzoom bis zum vorhandenen Kamera-Zoom19.
- Fahrradkennzeichnungen sind zusätzliche Stilregeln derselben Daten, keine zweite Kachelquelle oder zweite Kartenfläche. VTM-Beschriftungsebene; keine zusätzlichen3D-Gebäude.
- `VtmBaseMap` verwaltet Raster- oder Vektorkacheln plus optionale Beschriftungen, einschließlich Pause/Fortsetzen/Entfernen. Ebenen bleiben unter gespeicherten Strecken und Livefahrt. Dieselbe Quelle/Stilwahl auch in Streckenvorschau und Detail-Vollbild.
- Korrektur nach Build120 für native GL-Strecken bleibt erhalten. Kein Sensor-/Aufzeichnungs-/Analyseumbau, kein allgemeiner Akkumodus und keine Nachtkartenfunktion in diesem Auftrag.

## Gezielte Prüfung

Echte öffentliche OpenFreeMap-Kachel aus dem Raum Krumbach geladen und mit dem produktiven MVT-Decoder dekodiert: Geometrien, Namen und Theme-Zuordnung vorhanden. Stiltest prüft Radweg, ausgewiesenen gemeinsamen Weg und unveränderten normalen Fußweg. Metadatenauflösung/Cache/URL-Validierung geprüft. UI-Test prüft Vektor/Raster-Wechsel, Ebenenreihenfolge unter derselben Streckenebene, Pause/Fortsetzen, Fahrradprofilumfang und Backup-Roundtrip. Bestehende Rasteranbieter-/Schlüsseltests ebenfalls erfolgreich. Im ersten Lauf Zoomlimit-Konflikt der Standard-VTM-Beschriftung erkannt; Label-Limit explizit auf vorhandenen Kamera-Zoom19 gesetzt, betroffenen Fall erfolgreich wiederholt.

Logs: `/workspace/.tools/vtm-vector-tests.log`, `/workspace/.tools/vtm-vector-release-check.log`, `/workspace/.tools/vtm-vector-release-final.log`, `/workspace/.tools/vtm-vector-raster-final.log`. Release-R8 und Java-Ressourcenzusammenführung erfolgreich, ohne APK. Fehlende SLF4J-Anbindung im ersten R8-Lauf mit einer stillen Bindung behoben. VTM-OpenMapTiles-Theme im zusammengeführten Ressourcenarchiv nachgewiesen; 25 Bibliotheksshader plus eigener Streckenshader als App-Asset. Typografie/Atlas/Diff erfolgreich. Kein Gesamttestlauf. Tatsächliche Darstellung/Lesbarkeit beim Fahren und Gesten auf dem Smartphone noch prüfen; lokale Tests ersetzen keinen Android-GPU-/Akkutest.

Quelle/Schema: [OpenFreeMap](https://openfreemap.org/quick_start/), [TileJSON](https://tiles.openfreemap.org/planet), [OpenMapTiles](https://openmaptiles.org/). Attribution in der App: OpenFreeMap · © OpenMapTiles · © OpenStreetMap.
