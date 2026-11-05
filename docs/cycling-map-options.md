# Radwege und Radrouten getrennt nach Build137

Nutzerentscheid: beide als eigene Kartenoptionen wie die drei User-Tracks, nicht als feste Basistil-Elemente. Im Fahrradprofil unter Karte zwei zunächst geschlossene Rahmen-Dropdowns „Radwege“ und „Radrouten“, jeweils eigener Schalter, eingefärbtes Icon, dieselbe Vorschau/Farbpalette/HEX/Deckkraft/Breite wie User-Tracks. Trackbedienung bleibt erhalten. Alte eigenständige Radwegeschalterzeile entfällt; Quick-Ebenenpopup bleibt synchron.

MapLineKind um cycleways/cycleRoutes ergänzt, bestehende IDs/Reihenfolge erhalten. Speicherung im vorhandenen mapRouteStylesV1, damit Profilkopie/Sicherung dieselben validierten Daten verwenden. Radwege behalten mapVectorCycleways/default an; Radrouten über MapVectorLayer.CYCLE_ROUTES/default aus. Bestehende Designer-CYCLE-Overrides beim Lesen übernehmen (Breite auf die Trackregler-Halbschritte runden). Im Designer keine doppelte Radweg-/Radrouten-Farbe mehr, Einstellungen sind zuständig.

Der JSON-Export enthält zusätzlich cyclingStyles mit beiden Farben/Breiten/Deckkräften; visibility/CYCLE_ROUTES und cycleways enthalten die Schaltzustände. Bestehende Objekt-/RGBA-/Muster-/relativen Breiten-/Schriftparameter bleiben im Export. Kein JSON-Import in die App beauftragt.

## Daten und Darstellung

Physische Radwege weiterhin aus den vorhandenen OpenFreeMap-Features. Kacheln niedriger Quellzoomstufen enthalten viele einzelne Wege nicht; die Hervorhebung kann das nicht beheben. Ausgeschilderte Radrouten als separate echte Vektorgeometrie von Waymarked Trails, mit eigener Darstellung auch in der Übersicht.

Öffentliche API geprüft: list/by_area mit EPSG:3857-bbox/limit100, danach list/segments mit den IDs und demselben Ausschnitt. Ergebnis bereits serverseitig zugeschnitten und vereinfacht. Ab Quellzoom12 vollständige tiles/12/x/y.json-Geometrien. Echter Testausschnitt: Übersichtsabruf100 Routen/ca1,4MB, Detailkachel73 Features/ca56KB, beideHTTP200. Logs/Antworten unter /workspace/.tools/wmt-area.json, wmt-segments.json und wmt-detail-tile.json. Keine Website-/Servercodes kopiert. API-Referenzen: https://github.com/waymarkedtrails/waymarkedtrails-api/blob/master/wmt_api/api/routes/listing.py und routes/tiles.py.

Ein VTM-Tileworker, Quellzoom6–12, darüber native Darstellung derselben Kacheln. Maximal2Requests pro Übersichtskachel,1 pro Detailkachel; keine Kaskade aus hunderten Detailkacheln für die Übersicht. HTTP über vorhandenen gemeinsamen64MiB-Cache, zusätzlich8MiB Geometrie-LRU; Antworten maximal8MiB, Geometrie maximal250000 Punkte, leere Cacheeinträge zählen mit Mindestgröße. HTTP-Cancel/Disposed-Generation und VTM-Pause/Resume/Detach beachten. EPSG:3857 explizit in lokale Kachelkoordinaten umrechnen; LineString/MultiLineString getrennt erhalten, exakte Linien-Duplikate unterdrücken, Punkte ignorieren.

Routenbreiten bei Zoom6–12 zunächst0,45/0,55/0,7/0,9/1,2/1,8/2,5, darauf der gewählte Breitenfaktor. Routen über Basiskarte, aber unter Beschriftungen/eigenen Tracks/GPS. Transparenter Hintergrund und kein eigenes globales Karten-Clear. Native Farb-/Alpha-Aktualisierung weiterhin live, Breitenwechsel über den bisherigen Aufbaupfad. Quellenhinweis im Kartencredit und assets/Cycling-ATTRIBUTION.txt.

Limit: In sehr großen Übersichtsausschnitten liefert die by_area-API höchstens100 übergeordnete Routen pro Kachel, mit Vorrang größerer Netze. Keine Vollständigkeit aller lokalen Routen in Kontinentalansichten behaupten. Optionaler Online-Dienst; vor Gerätefreigabe Darstellung/Antwortzeit prüfen.

## Feldwegfarbe

Der alte Designer-Eintrag „Feld-, Fuß- und Radwege“ war irreführend: er steuert native path-Geometrie, die neuen track-Geometrien besitzen einen anderen Stil. Designer nun eindeutig „Fußwege und Radwege“ versus „Feld- und Wirtschaftswege“. Layer-Schalter weiter gemeinsame Wegegruppe. Native track-Farbänderung einschließlich Dash-Stipple-Farbe und erhaltener Texturreferenz gezielt geprüft.

## Prüfungen

Vier gezielte Fälle in cycling-options-final-tests.log erfolgreich: unabhängige Controls/Profil/Backup/JSON und native Reihenfolge/Farbe/Transparenz, begrenzter Übersichtsdatenabruf/Cache/Dispose, Feldweg-Livefarbe, bisherige Trackpalette. Zusätzlicher echter MainActivity-/Karten-Einstellungstest in cycling-settings-integration-final-test.log erfolgreich, einschließlich beider Schalter und Raster-/Vektorwechsel. Die zentralen Formularbeobachter dürfen die eigenen Listener der Cycling-Frames nicht überschreiben; deshalb diese wie die bisherigen Trackframes erst nach observeSettings einfügen. Typografie/UI-Katalog/Bright-Generator/Diff grün. Kein Gesamtlauf, keine neue APK; optische Geräteprüfung offen.
