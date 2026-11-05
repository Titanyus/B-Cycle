# Kleiner Karteneditor und Reihenfolge

Auftrag 04.11.2026 nach Build 130. Seit 05.11.2026 auf der Dashboardkarte eigener Stiftbutton neben Auge/Ebenen → Karteneditor. Native Objektauswahl, vier RGBA-Regler 0–255 mit sichtbarem HEX-Wert und Farbvorschau, Linienstile Original / Durchgezogen / Gestrichelt / Gepunktet / Strich-Punkt. Farbe und Linienstil unabhängig, einzelne oder alle Stiländerungen zurücksetzen, JSON exportieren. Keine freie Auswahl der Zeichenreihenfolge beauftragt; die gewünschte feste Reihenfolge ist unten beschrieben.

## Stiländerungen

Kuratiertes Objektverzeichnis: Hintergrund, Wald, Gras/Wiesen/Parks, Wohn-/Gewerbe-/Industrie-/Friedhof-/Krankenhaus-/Schulflächen, Sand/Eis, Wasserflächen/Flüsse/Bäche, Autobahnen/Bundes- und Fernstraßen/weitere übergeordnete Straßen/kleine Straßen/Wege, Radwegehervorhebung/Eisenbahn, Gebäude/Grenzen, Orts-/Straßen-/Gewässernamen und Höhenlinien. Gruppierung folgt den tatsächlichen Bright-Stilen: z. B. sekundäre und tertiäre Straßen teilen dort einen Stil, ebenso Haupt-/Fernstraßen auf Brücken/in Tunneln. Keine Bearbeitung von POI-Icongrafiken oder nicht in Bright gezeichneten Objektklassen.

`MapStyleOverrides` je Fahrradprofil unter `mapStyleOverridesV1`, mit Kopie/Wechsel und strikter Validierung in MAPS-/Profilbanksicherung. Nur bekannte Objekte/Enums, #RRGGBB-Farben, Schema 1 und aktuelle Bright-Basis zulässig. Native Render-XML-Instruktionen vor ThemeLoader anpassen; Originaldateien bright.json/bright-vtm.xml/Sprites unverändert. Eigene Änderungen als Delta, keine direkte Änderung von MapLibre-JSON-Ausdrücken.

Alle Zoomvarianten der zugeordneten nativen Stile anpassen. Originale Transparenz, Breiten, Zoomregeln, Beschriftungspriorität und Textkonturen bleiben. Straßenfarben verändern die Fahrbahnfarbe; die vorhandenen Ränder behalten ihre Farbe, Linienmuster wirken auch auf zugeordnete Ränder. Native VTM-Dash-Texturen über denselben XML-Parser erzeugen, keine manuell gemeinsam genutzten Texturkopien. Linienmuster nur bei Linienobjekten, RGB-Farbe auch für Flächen/Text. Seit Folgeauftrag 05.11.2026 reine RGB-Farben im 48-ms-Fenster direkt in vorhandenen VTM-Stilen aktualisieren. Textfarben/Muster weiterhin 180 ms bündeln und neu aufbauen; beim Schließen letzte Änderung anwenden. [Live-Farben](vtm-live-colors.md). Schalter bleiben davon unabhängig.

## JSON-Datei

Export über Android ACTION_CREATE_DOCUMENT, MIME application/json, Dateiname B-Cycle-map-style-DATUM.json. UTF-8, außerhalb des UI-Threads schreiben. Export-Snapshot beim Start des Pickers erfassen und bei Activity-Neuerstellung erhalten; Abbruch verwirft den Exportauftrag, Stil bleibt gespeichert. Erfolg/Fehler über bestehende Meldungen.

Datei enthält Format bcycle-map-style, version 1, base bright, baseSha256 der Originalvorlage, objects mit optional color/lineStyle sowie die aktuelle visibility und cycleways. Damit lässt sich die Darstellung nachvollziehbar übergeben. Kein vollständiger Maputnik-/MapLibre-Stil und noch kein Dateiimport in der App beauftragt.

## Zeichnen

Mit Höhenlinien:

1. GROUND: Bright-Hintergrund sowie landcover/landuse/park/water.
2. Höhenlinien als native Vektorlinien.
3. FOREGROUND: übrige Kartengeometrie, insbesondere Straßen/Wege und Gebäude, Wasserläufe/Grenzen/sonstige Objekte.
4. Gemeinsame Beschriftung.
5. Gespeicherte/aktive Tracks und eigener Standort, wie bisher.

VtmBaseMap verwendet nur bei eingeschalteten Höhenlinien zwei gefilterte VTM-Basispässe. Vordergrund hat transparenten Overdraw-Hintergrund, damit Gelände/Höhenlinien darunter sichtbar bleiben. Namen nur einmal am Vordergrund-Tilemanager sammeln. Ohne Höhenlinien weiterhin ein Basispass, kein zweiter Tile-Decoder oder zusätzlicher Puffer. Beide Pässe je ein Loader statt zwei im Einzelpass; gleicher begrenzter HTTP-Diskcache. Kalt-Anfragen gleicher Tile-URL über kurze gestreifte Sperren serialisieren und Antwort vor Freigabe konsumieren, damit der Cache vor dem zweiten Abruf bereit ist. In beiden Pässen wird MVT separat dekodiert; keine zusätzliche MapView/GLSurfaceView, kein Reparenting.

Profil-/Stil-/Layerwechsel behalten Kamera, Footer-/Kartengeometrie, GPS und Track-Layer. Rasterquellen bleiben unverändert. Die Karten-Stiländerungen gelten auch für Streckendetailkarten.

## Verwaltungsgrenzen

Zusätzlicher Schalter „Verwaltungsgebiete zwischen Gemeinden und Landkreisen“ für admin_level 7, beispielsweise Verwaltungsgemeinschaften. Gemeinde-/Ortsteilgrenzen bleiben separat bei admin_level ≥8. Landkreise (typisch Ebene 6) und größere Grenzen unabhängig erhalten. Level 7 zunächst an, um den bisherigen Zustand nicht beim Update zu verändern. In Deutschland ist diese Einordnung plausibel; die genaue Verwaltungsbedeutung stammt aus den jeweiligen OSM-Daten und ist international nicht einheitlich.

## Flat-Sticky-Header

Global im Flat-Design Hintergrund aus PRIMARY berechnen: R/G/B × 0,7, Alpha erhalten. Alle vordefinierten und benutzerdefinierten Farbprofile verwenden dieselbe Formel, auch nach Themenwechsel. Bisherige obere 2-dp-Linie, TERTIARY-Inhalte, Geometrie und Glow-Header unverändert.

## Prüfung

Gezielte native Fälle: tatsächliche VTM-Linientexturen/Farben und ursprüngliche Waldtransparenz nach JSON-/Profil-/Backup-Rundreise, Reihenfolge und Passfilter mit Straße/Gebäude/Gelände/Wasser, kein zusätzlicher Pass ohne Höhenlinien, echte Editorsteuerung/Reset/Export-Snapshot, nativer Dokumentpicker samt UTF-8-Dateiausgabe, berechnete Flat-Farbe für alle Presets. Bestehender Layer-/Popup-/Rasterwechsel und admin_level-7-Filter/Sicherungsrundreise mitgeprüft. Bright-Generator, Typografie, UI-Katalog und Diff-Prüfung erhalten. Kein Gesamtlauf, keine lokale APK. Echtgerät-Lesbarkeit, Dash-Bild und Überdeckung während Zoom weiter beurteilen.

## Folgeänderungen 05.11.2026

[Eigenständiger RGB-Editor, frühere Ortsübersicht und länger sichtbare Radwege](vtm-rgb-and-overview.md). Frühere Wabenpalette nur bei Kartenobjekten ersetzt.

## Kompakter Editor ab 05.11.2026 nach Build 134

Keine Überschrift/Erklärung, Farbfeld rechts neben der Objektauswahl, HEX in deren Beschriftungszeile. Vier Regler mit 48-dp-Touchhöhe ohne bisheriges 8-dp-Padding oben/unten. Objektreset/Gesamtreset/JSON/Schließen als vier zugängliche Iconbuttons mit Tooltips in einer Zeile. Popup belegt nur die benötigte Inhaltshöhe (bei kurzen Screens begrenzt/scrollbar). Letztes ausgewähltes Objekt lokal unter `mapStyleEditorLastTarget` merken, auch nach Schließen/Neustart; kein Bestandteil des Kartenstils.

JSON-Version 1 bleibt kompatibel: optionales ganzzahliges `alpha` 0–255 je Objekt; alte reine RGB-Deltas behalten die nativen Transparenzen. Gleiches Schema in Profilen/Sicherung/Export. Alpha wird bei Flächen/Linien/Hintergrund direkt über native Live-Stile gesetzt, bei Text über den bisherigen Atlas-Neuaufbau. Ohne Alpha-Override genaue Transparenz je Stilvariante/Zoom erhalten. Die eine Objektauswahl zeigt den ersten passenden Basisstil (Wald 26, kleine Höhenlinie 102); eine explizite Änderung setzt den gewählten Wert für alle Varianten dieser Objektgruppe.
