# Aktueller Stand: APK 0.8.90 / Build 132 veröffentlicht

## Änderungen nach Build 132, noch unveröffentlicht

- Maximalzoom: nativen Overzoom-Gebäudeüberlauf vor signed-short-Packung reproduziert und gepufferte Zielkachel-Clipping-Stufe nachgeschaltet. Quellstufe/Kamera/Styles unverändert, beide Vektorpässe profitieren. Native Regression Stufen 15–19 grün.
- Hauptstraßennamen/-nummern: radialer Wiederholungsabstand mindestens 220 Canvas-dp, gleiche Kennung über Leerzeichen-/Stylevarianten. Neue Straßenkandidaten viewportweit statt Tile-Reihenfolge auswählen, größere Lücken bevorzugt auffüllen; bestehende Labels behalten Alter. Native Fälle plus regelmäßige Verteilung bei umgekehrter Reihenfolge/90° Drehung grün.
- Öffentliche Referenzen geprüft: Trekarta samt VTM-Fork, offizielle VTM-Beispiele und offene Cachebox-Integration. [Befund/Verfahren/Prüfung](vtm-maxzoom-and-road-labels.md). Fortsetzung als neue APK beauftragt: 0.8.91 / code 161 / erwarteter Build 133, ausschließlich GitHub Actions. Gerätevergleich offen.

## In Build 132 enthalten

- Fitness unter Statistiken als letzter Abschnitt (auch ohne lokale Fahrten): CTL/ATL von Intervals, absolute Form CTL−ATL, 7/30/90 Tage, horizontales Panning/Trägheit, Tagesauswahl, Heute/Aktualisieren. Bestehender Zugang/WLAN, Worker und atomarer Kontocache, keine Uploadänderung. [Details](intervals-fitness.md). Drei gezielte API-/Cache-/UI-Fälle grün; echte Nutzerkontoprüfung/Gerätebild offen.
- Wiederholte Straßenlabels konnten wegen grober Weltzellen dieselbe Fade-Geometrie überschreiben. Nun räumliche Eins-zu-eins-Zuordnung pro Runde mit aktueller Skala; zwei gleichnamige B16-/Donau-Labels bleiben trotz vertauschter Reihenfolge getrennt. Native Label-/Fadefälle grün. Orte beim Rauszoomen eine halbe Stufe länger behalten; neue Schwellen unverändert. [Details](vtm-label-stability.md).
- Ebenenbutton neben dem Auge auf der Dashboardkarte: natives Dropdown mit sofort wirksamen Profil-Schaltern, bleibt zum Vergleichen offen. Keine neue MapView; Pause/Profil/Editor/Raster berücksichtigen. Gezielter Popup-/Layerwechseltest grün. [Details](vtm-map-layers.md).

- Karteneditor im Ebenen-Dropdown: native Farbauswahl/vier Linienmuster plus Original, Objekt-/Gesamtreset und JSON-Dateiexport über Android-Picker. Stil je Fahrradprofil mit Sicherung/Kopie. Höhenlinien zwischen Gelände und Straßen/Gebäuden; zwei gefilterte Basispässe nur bei aktivierten Höhenlinien, ein Basispass sonst. Zusätzlicher Grenzschalter admin_level 7, initial an. [Editor/Reihenfolge/Prüfung](vtm-map-editor.md).
- Flat-Sticky-Header global PRIMARY RGB×0,7, automatische Anpassung an das Farbprofil; 2-dp-Linie/TERTIARY-Inhalte/Glow-Geometrie erhalten.

Neue APK ausdrücklich angefordert, über GitHub Actions gebaut und geprüft. Editor/Reihenfolge/Grenzen/Profil-/Backup-Rundreise/Flat-Farben sowie nativer Dokumentpicker/Dateiausgabe gezielt grün. [Änderungen](release-0.8.90.md), [Buildnachweis](build-132.md). Gerätebild bleibt offen.

## In Build 130 enthalten

Fehlende Glyphen: drittes Shaderattribut explizit aktiviert, danach alle Attribute sauber freigegeben. Straßennummer und Schild in gemeinsamer Atlaszelle/Fade. Drei gezielte Java-/native Fälle sowie echte GLES-Pixelprüfung mit vier erzwungenen Treiberbelegungen grün; siehe [Beschriftung](vtm-label-stability.md).

Wichtige Vektorlayer je Profil schaltbar, inklusive öffentlicher Mapzen/AWS-Terrain-Höhenlinien (default aus, ein Worker, DEM bis Zoom 13, Cache/Abbruch, Quellenhinweise). Profil-/Sicherungsumfang und strikte Typvalidierung erweitert. Drei gezielte Layer-/Sicherungs-/Terrain-/UI-Fälle grün, reale Terrainprobe plausibel. [Kartenebenen](vtm-map-layers.md). Original-Bright-Assets unverändert. Gerätebild/Zoom-Lesbarkeit weiter beurteilen.

Neue APK ausdrücklich angefordert, ausschließlich über GitHub Actions gebaut und geprüft. [Änderungen](release-0.8.89.md), [Buildnachweis](build-130.md).

## Abgeschlossene Folgeaufträge nach Build 128

- Ortsnamen: weniger Dichte, gemeinsame Zoomkohorten, Hysterese und 240-ms-Ein-/Ausblenden über eigenen GL-Shader. Feinränge neuer Tiles verdrängen keine sichtbaren Namen derselben Größenklasse. Viewportweit sortieren, kurzzeitig fehlende Namen während Fade mit ihrem alten Sichtbarkeitsalter wiederaufnehmen. Native Atlas-/Glyphvertices/Matrizen erhalten; separater Metadata-VBO, begrenzte Historie und GL-Freigabe über zugehörige MapView-Queue. [Details](vtm-label-stability.md). Jetzt 28 Shader einschließlich bcycle_labels.glsl; neue APK-Prüfung anpassen.
- Gemeinde-/Ortsteilgrenzen (admin_level ≥8) ausblenden, übrige Boundary-Ebenen erhalten. POI-Sperre bleibt. Bright-JSON/Generatorassets unverändert.
- Feldauswahl korrigiert: globalen pageContentGap 16 dp behalten, nur früheres zusätzliches Außenpadding entfernen. FieldPicker und tatsächlicher Vollbildhost TERTIARY; der Host hatte die Fläche bisher wieder mit SECONDARY übermalt. Überflüssige leadingContentGap=0-Ausnahme entfernt; lokaler sectionBackdrop bleibt TERTIARY.
- Glow-Sticky-Kästen global außen links/rechts 8 statt 16 dp; Abstand zwischen Kästen weiterhin 24 dp. Inhaltseinstellungen und Flat-Geometrie erhalten.

Acht gezielte native Fälle grün: drei Label-/Gruppen-/Fadefälle (inkl. Nachladen/Umkehr), Gemeindegrenzen/Theme, echter Dashboardeditor mit Vollbildfarbe/16-dp-Abstand, Boxgeometrie/Pinning, Layerwechsel und Feldbelegungsnavigation. Logs /workspace/.tools/label-fades-and-ui-tests.log, label-resource-and-field-tests.log, label-fades-final-placement-tests.log. GLES-Pixelprüfung über scripts/check-vtm-label-shader.py grün; Kompilation/Typografie/Atlas/Bright/Diff grün. Kein Gesamtlauf; in Build 129 enthalten. Smartphone-GPU/Zoom-Eindruck noch beurteilen.

## Erhaltener Stand aus Build 128

Neue APK ausdrücklich angefordert, ausschließlich über GitHub Actions gebaut und geprüft. [Änderungen](release-0.8.87.md), [Buildnachweis](build-128.md).

- Kartenbeschriftung: Orte nach Größenklasse/OpenMapTiles-Rang, Straßen nach Straßenklasse. Höhere Bedeutung gewinnt; bei gleicher Bedeutung bleiben sichtbare Namen. Aktuelle Merkmale über Tile-/Zoom-/Stilwechsel binden, entfallene entfernen. Drei lokale umbenannte LGPL-VTM-Klassen, native GL-/Worker-/Tilelebenszyklen erhalten. Stilvarianten-Cache 256, keine Änderung der Original-Bright-JSON oder generierten Assets. [Verfahren/Quellen](vtm-label-stability.md).
- HRV-Einstellungen: gemeinsamer Sticky-Scrollhost mit „HRV“ (Messoptionen, offen) und „Tagesbefinden“ (bisherige Frageauswahl). Gemeinsame TERTIARY-Karten ohne abweichende SECONDARY-Ränder; Werte/Listener/Sperren erhalten.
- „Feld belegen“: führender Abstand 0, auch bei Seitennormalisierung. Scroll-/Zwischen-/Restflächen nach Trinkerinnerung TERTIARY. Lokale leadingContentGap/sectionBackdrop-Optionen; andere Seiten unverändert.
- Vektorkarte blendet poi/aerodrome_label aus; Orts-/Straßennamen bleiben. Haupttitel „Gefahrene Strecke“ mit nativer Größenanpassung, Menü weiterhin Plural. Vier Track-Icons in deckender Trackfarbe, Aktualisierung per HEX/Palette.
- Controls in Glow wie Flat: einfarbige Flächen/Konturen/Rundungen, keine Auswahl-Elevation oder dekorative Chromatic-/Blur-Engine im Appcode. Unterschiede nur Header-/Footer-Schatten, Trainingshilfe-Glow und abgerundete Sticky-Kästen/Geometrie; funktionale HRV/Bosch-Effekte erhalten. Nachrichtenanzeige ohne zusätzlichen Glow-Schatten. Einfache Spinner SECONDARY in beiden Designs.

Sechs gezielte letzte Fälle grün: zwei echte VTM-Platzierungsfälle (Priorität, Alter, Tile-/Zoom-/Stilwechsel, Entfallen), tatsächliche Theme-Prioritäten, Vektor-/Raster-Lifecycle mit Tracklayer, HRV-Werte/Sicherungsrundreise und beide TERTIARY-Karten, Feldbelegungsnavigation/Abstand/Restflächenpixel. Logs /workspace/.tools/stable-labels-final-placement-tests.log und stable-labels-and-surfaces-tests.log. Frühere relevante Control-/Fokus-/Icon-/POI-Tests ebenfalls grün. Kompilation/Typografie/Atlas/Bright-Generator/Diff erfolgreich; kein Gesamtlauf, keine lokale APK. Gerätebild, besonders Beschriftungsruhe bei Pinch-Zoom, noch beurteilen.

## Gesicherte Stände und Archive

- Vor Flat-Sticky-Designversuch: Tag `design-before-flat-sticky-lines-2026-11-04` bei `10453de2d9119db0a878f899e07b51ba9b695cd8`, auch remote gesichert. Spätere fachliche Änderungen bei Rückkehr nicht pauschal zurücksetzen.
- [Chromatic-Archiv](archive/chromatic-glow-2026-11-04/README.md): 25 Originaldateien vom Stand 87d0958 samt SHA-Manifest, Berechnung, Kotlin/JS, Blur/Shader, Parameter/Tests und eigenständigem Offline-Designer. Originale bytegenau gegen Herkunftscommit geprüft. Nur dort verbleibt die alte Android-Engine, nicht im aktiven Appcode; separater Designer weiterhin Werkzeug.
- [Meilenstein vor VTM](milestone-0.8.78.md), Build 119, unverändert erhalten.

## Weiter gültige Kartenbasis

Eine dauerhafte MapPanel/GLSurfaceView im DashboardMapHost; Seiten nur Slots. Bei Wechsel/Abbruch niemals reparenten/detachen. Host folgt Slot pro Layout/Drag-/Animationsframe; echte Footer-/Zielgröße, auslaufende Aufnahme und Bitmapfreigabe erhalten. [Details](dashboard-page-transition.md). Physische Geräte-GPU/Zoom-/Tileausgabe weiter beurteilen.

VTM 0.25.0 mit Rasterquellen und optional OpenFreeMap-Vektor; GPS/Höhe/Strecken gemeinsam im GL-Frame, Live-Strecke während Geometriewechsel erhalten. Native Draufsicht ohne Extrusionen. Bright-JSON SHA-256 cda8931095c5101cbad5d2a60fa129600a1a0ad3dea27d802be341fb5eae071f; Generator --check für XML/111 Sprites. MapLibre-Zoom = VTM-Zoom − 1. GL-Hintergrund beim Initialisieren/Resume an MapRenderer übergeben. [Stil](vtm-bright-style.md).

Akkusparmodus nur Darstellung: Karte/Dashboard 1 Hz, kurze Diagramme 5 s, Gesamtverlauf 10 s, Gesten direkt. Keine GPS-/BLE-/Rohdaten-/Analyse-Drosselung und kein automatischer Stilwechsel. Weitere Regeln in AGENTS.md und verlinkten Fachdokumenten.

## Letzte veröffentlichte APK

[APK 0.8.90 / Build 132](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.90-release-132.apk), code 160, Quelle `e52738ee1ca03596786d5ed53ef8645e41205347`, 4244912 Bytes, SHA-256 `6187fe34e965f1e0644ffd4b43b9df78fbe4a1f33a6f92645b8429966009388c`. Dauerhafte Signatur, Manifest, ZIP-/ARM64-16-KB-Ausrichtung, vier ABIs, 28 Shader, Bright-/Terrain-Assets, Texte und R8-Namen geprüft. [Nachweis](build-132.md). Vorige APK: [0.8.89 / Build 130](build-130.md).

## Weitere Planung

Keine automatische Stiländerung im Akkusparmodus. Minimalstil/OLED und Dashboardoption offen; Maputnik als Browsereditor, kein Live-JSON-Import beauftragt. Sensoren bleiben zuverlässig in voller Auflösung.

Nächste APK nur auf ausdrücklichen Auftrag über GitHub Actions, voraussichtlich 0.8.91 / code 161 / Build 133. Appcode nie mit [skip ci] überspringen. Dauerhafte Signatur/28 Shader/Bright-Ressourcen und tatsächlichen direkten Download prüfen.

Downloadnachweis: veröffentlichter direkter Raw-Link abgerufen, 4244912 Bytes, bytegenau identisch zum geprüften Actions-Artefakt; SHA-256 ebenfalls identisch.
