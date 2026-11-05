# Änderungen für APK0.8.97 / Build141

- Nachtrag vor Veröffentlichung: Hinweis vor erneuter HRV-Tagesmessung bei manuellen/automatischen Starts, Buttons Abbrechen/Verstanden nebeneinander. Neue Messung verwendet bestehende Tagesauswahl, Altwert vor Start unberührt. Build140 wird durch Build141 mit diesem Nachtrag ersetzt.

- Flat-Flächentausch: Sticky-Header TERTIARY, Inhalts-/Zwischenflächen SECONDARY, normale Dropdowns/Accordions/Buttons TERTIARY. Glow, Eingaben und aktive PRIMARY-Flächen erhalten. Streckengruppenköpfe in sämtlichen Sortierungen mit gemeinsamer oberer2dp-Rahmenlinie. Gezielt SectionCards/RouteStickyList grün; zentrale Prüfungen grün. Neue APK beauftragt, Veröffentlichung läuft.

- FLAT-Trainingshilfe: HSV-Zwischenfarben statt RGB-Braun, sowohl zeitlich als auch an Zonengrenzen (Puls/Leistung, aktuell/Mittel, Alle Zonen/Zone2). Normale SRC_OVER-Zeichnung, Endfarben, Dauer und Akkusparverhalten erhalten. Glow bleibt beim bisherigen Verfahren. [Details/Prüfung](dashboard-training-help.md). Keine APK beauftragt.

- HRV-Tageswechsel stationär statt seitlicher Seitenanimation: Werte/Bögen/Indexbalken/Glow synchron300ms Ease-in-out, gleiche Gestenerkennung und synchronisierte Tagesauswahl. Grenzwerttexte zusätzlich3dp oben/unten versetzt. Keine Messberechnung verändert; fehlende Werte ohne erfundene Null. [Details/Prüfung](hrv-daily-assessment.md). Keine APK beauftragt.

- Backup & Restore in „Sichern“, „Wiederherstellen“, „Importieren“ gegliedert. Reaktivierter Streckenimport als vier einzelne Menüzeilen statt Accordion; gemeinsame RouteImportAction-Liste, Handler und Dreeve-Freigabe mit dem Streckenmanager. Drei lokale Picker und Gruppierung sowie Dreeve-Verfügbarkeit gezielt grün, zentrale Typografie/UI-Katalog/Diff grün. [Details/Prüfung](backup-route-import-groups.md). Noch keine neue APK beauftragt.

- Ortsnamen-Flimmern/Barcode reproduziert (UV2108 bei zulässigen2048 am256px-Atlasrand) und durch vollständige Platzprüfung je Zelle behoben. Kleine/größere ein-/zweizeilige Labels sowie sehr große Glyphen als Pixel/UV/Seitenbereiche getestet; Halo-Padding berücksichtigt. [Befund/Prüfung](vtm-label-stability.md).
- Weiße Ortskonturen doppelt (Orte2,4, Länder/Kontinente4) und100 % Deckkraft. Globale gestrichelte Kartenlinien auf Nutzerwert12/8, reine Punkt-/Bahn-Querstrichsymbolik erhalten. JSON dashPattern/placeTextHalo ergänzt, alte Exporte kompatibel. [Strichregel](map-road-surfaces.md). Native Paint-/JSON-Prüfung grün.
- Quelleninformationen zentral Erweitert→Quellen, Terrain-Quelle aus Karten/Ebenen entfernt; OpenFreeMap/Waymarked Trails ergänzt. Höhe/Windrose erste Kartenoptionen. Kartenauswahl mit900er disabled Raster-/Vektorüberschriften und Trenner, vier Rasterquellen + Bright, Persistenz weiter stabile Quell-IDs. Echte MainActivity-/Provider-/Recycling-Prüfungen grün. [Details](map-settings-grouping.md).
- Vektor-Zoom bis21, Raster19/Topo17 erhalten. Kameragrenzen/Speicherwerte/Providerwechsel und Native-Clipping15–21 geprüft. Vollbild-Dashboard: „Android Statusleiste“ zentriert zwischen Links/Rechts, reine Beschriftung ohne Funktionsänderung.

Gezielte Prüfungen erfolgreich: city-atlas-halo-and-dashes-tests.log, map-halo-global-dash-final-test.log, map-halo-dash-and-overzoom-final-tests.log (Overzoom grün, damalige Länder-Testmerkmale anschließend korrigiert), grouped-map-source-and-settings-tests.log und grouped-map-provider-final-test.log (veraltete Höhenkorrektur-Testannahme korrigiert). Typografie/UI-Katalog/Bright/Diff grün, kein Gesamtlauf. Keine neue APK beauftragt; Gerätevergleich offen.

## Letzte Veröffentlichung: APK 0.8.96 / Build 139

Downloadnachweis: Den veröffentlichten direkten Raw-Link abgerufen: 4279128 Bytes, bytegenau identisch zum geprüften Actions-Artefakt; SHA-256 ebenfalls identisch.

Straßen/Wege nach vorhandenen Belagsangaben: unbefestigt ausdrücklich gestrichelt6/4, befestigt/fehlend/unbekannt durchgezogen. Farbige Radwegehervorhebung selbst samt Grundlinie/Umrandung gleich gestrichelt; kein seitlicher Versatz (Nutzerpräzisierung). Radrouten ohne Belagsdaten bleiben durchgezogen/unbekannt. Designer-Straßenmusterwahl durch kompakten Hinweis ersetzt, Farbe/Alpha/Breite behalten. JSON feste Regel roadSurfaceStyle=unpaved-dashed, bestehende Exporte kompatibel. Sechs gezielte Fälle plus Typografie/UI-Katalog/Bright/Diff grün; [Umfang/Prüfung](map-road-surfaces.md). Neue APK über GitHub Actions erfolgreich gebaut, geprüft und veröffentlicht. [Nachweis](build-139.md), [Änderungen](release-0.8.96.md). Gerätebild offen.

## Letzte Veröffentlichung: APK 0.8.95 / Build 138

Downloadnachweis: Den veröffentlichten direkten Raw-Link abgerufen: 4279004 Bytes, bytegenau identisch zum geprüften Actions-Artefakt; SHA-256 ebenfalls identisch.

Lange mehrteilige Ortsnamen zweizeilig, ohne Wörter zu trennen: geografische Zusätze zusammen unten, sonst ausgeglichene Wortgruppen. Name/Zuordnung erhalten; native Atlaszeichnung und Kollisionsmaße gemeinsam erweitert. Drei gezielte Pixel-/Platzierungs-/Schilder-/JSON-Ebenen-/Profil-/Sicherungsfälle grün (place-wrap-and-layer-export-tests.log). Aktuelle Ebenenauswahl wird bereits als visibility/cycleways exportiert und gezielt bestätigt. Neue APK über GitHub Actions erfolgreich gebaut, geprüft und veröffentlicht. [Nachweis](build-138.md), [Änderungen](release-0.8.95.md).

Radwege/Radrouten auf Nutzerentscheidung getrennt als eigene Karten-Einstellungen mit Track-Farbe/HEX/Breite/Deckkraft und Schalter umgesetzt. Radrouten als optionale echte Waymarked-Trails-Vektorübersicht, default aus; Radwege behalten bisherigen Schalter und Quelldetailgrenzen. cyclingStyles plus Ebenenauswahl im JSON, Profil/Backup erhalten. Feldwegfarbe: irreführende PATH-Beschriftung getrennt, native Live-Farbe/Dash geprüft. Vier gezielte Optionen-/Vektor-/Profil-/Backup-/Export-/Farb-/Trackfälle plus echte MainActivity-Integration grün. [Umfang/Quelle/Begrenzungen](cycling-map-options.md). In APK0.8.95/Build138 enthalten. Restlicher Designer bleibt Arbeitsmittel für spätere feste Stile.

Belagsfrage während Build138: Braun gestrichelt zeigt Wegeart; surface=paved/unpaved aus der Quelle wird noch nicht als eigener Belagstil ausgewertet. Nutzer darüber informiert; keine gesonderte Belagimplementierung beauftragt.

## Letzte Veröffentlichung: APK 0.8.94 / Build 137

Downloadnachweis: Den veröffentlichten direkten Raw-Link abgerufen: 4278364 Bytes, bytegenau identisch zum geprüften Actions-Artefakt; SHA-256 ebenfalls identisch.

Linienbreite im Karteneditor (25–300 % je Zoom-Basis, optionales validiertes widthPercent im Objekt-JSON). Eigenständige Feld-/Wirtschaftswege: class track aus gemeinsamen Straßenstilen abspalten, zunächst halb so breit/braun/gestrichelt ohne breite Umrandung; unabhängig Farbe/Alpha/Muster/Breite bearbeiten. Radwegehervorhebung bis Kameraminimum6 statt9 mit abgestuften Übersichtslinien, soweit Quelldaten vorhanden. Drei gezielte Native-/Editor-/Export-/Profil-/Sicherungsfälle grün; Typografie/UI-Katalog/Bright-Generator/Diff grün. Nachweis: /workspace/.tools/map-width-and-tracks-tests.log. Ortsnamen-Folgeauftrag: town/city Black900 und 10 % kleiner, kleine Orte unverändert. Echte lokale Kachelprobe bestätigt Günzburg/Krumbach als town; keine eigenen Einwohnergrenzen. Gezielte Kotlin-Kompilation, Typografie und Diff grün. Neue APK über GitHub Actions erfolgreich gebaut, geprüft und veröffentlicht. [Buildnachweis](build-137.md), [Änderungen](release-0.8.94.md). Gerätebild offen.

Designer-Ergänzung enthalten: geschlossenes Dropdown Ortsbeschriftung für CITY/TOWN/VILLAGE/HAMLET; unabhängige Gewichte100–900 und Größen50–200 %. MapPlaceTextEditorTest für native Gewichte/Metriken, UI, Profil/Backup/JSON/Reset grün.

## Letzte Veröffentlichung: APK 0.8.93 / Build 135

Downloadnachweis: Den veröffentlichten direkten Raw-Link abgerufen: 4261284 Bytes, bytegenau identisch zum geprüften Actions-Artefakt; SHA-256 ebenfalls identisch.

FLAT-Sticky-Header SECONDARY. Kompakter RGBA-Karteneditor ohne Überschrift/Erklärung, Farbfeld in der Objekt-Dropdownzeile, vier Regler enger (48dp), vier Iconaktionen in einer Zeile. Alpha zeigt bestehende Originaltransparenz und setzt absolute Deckkraft; optionales JSON-alpha bleibt kompatibel mit alten RGB-Deltas, Profilen und Sicherung. Letztes Objekt nach Schließen wiederherstellen. Gezielte Editor-/Native-Farb-/Sticky-Tests grün; Typografie/UI-Katalog/Bright/Diff grün. Neue APK ausdrücklich beauftragt und über GitHub Actions erfolgreich gebaut/geprüft/veröffentlicht. [Nachweis](build-135.md), [Änderungen](release-0.8.93.md). Optische Geräteprüfung offen.

## Letzte Veröffentlichung: APK 0.8.92 / Build 134

Downloadnachweis: Den veröffentlichten direkten Raw-Link abgerufen: 4261416 Bytes, bytegenau identisch zum geprüften Actions-Artefakt; SHA-256 ebenfalls identisch.

## In Build 134 enthalten (05.11.2026)

- Kartenobjekt-Editor mit eigenem Stiftbutton neben Ebenen/Auge statt verschachteltem Dropdown; drei zentrale RGB-Regler 0–255, HEX/Farbvorschau, exakte Werte. JSON/Reset/Stil-/Profilfunktionen erhalten, Trackfarbpalette unverändert. Karten-/Footer-/Popup-Lebenszyklus ergänzt.
- Neue und vorhandene Ortsnamen nutzen dieselbe Ein-/Ausblendschwelle. Städte/towns/Dörfer deutlich früher, dadurch mehr Orte in der Übersicht. Native Größenpriorität/Fades/Beibehaltung erhalten.
- Radwegehervorhebung ab VTM 9 statt 12, dünne Breiten 0,6/0,9/1,4 bis 12, ab13 bisher2,5. Quellenprobe belegt frühe Orts-/Radwegdaten, nicht jedes Detail ist in der Übersicht vorhanden.
- Direkte Farbaktualisierung eingebaut: vorhandene native LineStyle/AreaStyle-Handles und Texturen beibehalten, auf GL-Queue set/updateStyles/render, RGB-Vorschau alle48ms auch während Bewegung. Beide Basispässe/Höhenlinien/Hintergrund und exaktes Zurücksetzen berücksichtigt. Textfarben/Muster weiter alter Aufbaupfad. Gezielte native Referenz-/Reset-/Queue-/MapPanel-/Dragfälle grün. [Verfahren](vtm-live-colors.md).
- Offizielle VTM-Beispiele gelesen: direkte Styleaktualisierung aus ThemeStylerActivity für Farben übernommen, Stilgruppen und MVT-/Cache-/LabelLayer-Integration ebenfalls. [Umfang/Daten/Prüfung](vtm-rgb-and-overview.md). Gezielte RGB-/Popup-/Label-/Cyclewayfälle grün, kein Gesamtlauf. Neue APK ausdrücklich angefordert, über GitHub Actions gebaut und geprüft. [Änderungen](release-0.8.92.md), [Buildnachweis](build-134.md). Gerätevergleich offen.

## In Build 133 enthalten

- Maximalzoom: nativen Overzoom-Gebäudeüberlauf vor signed-short-Packung reproduziert und gepufferte Zielkachel-Clipping-Stufe nachgeschaltet. Quellstufe/Kamera/Styles unverändert, beide Vektorpässe profitieren. Native Regression Stufen 15–19 grün.
- Hauptstraßennamen/-nummern: radialer Wiederholungsabstand mindestens 220 Canvas-dp, gleiche Kennung über Leerzeichen-/Stylevarianten. Neue Straßenkandidaten viewportweit statt Tile-Reihenfolge auswählen, größere Lücken bevorzugt auffüllen; bestehende Labels behalten Alter. Native Fälle plus regelmäßige Verteilung bei umgekehrter Reihenfolge/90° Drehung grün.
- Öffentliche Referenzen geprüft: Trekarta samt VTM-Fork, offizielle VTM-Beispiele und offene Cachebox-Integration. [Befund/Verfahren/Prüfung](vtm-maxzoom-and-road-labels.md). Fortsetzung als neue APK beauftragt, über GitHub Actions gebaut und geprüft. [Änderungen](release-0.8.91.md), [Buildnachweis](build-133.md). Gerätevergleich offen.

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

[APK 0.8.96 / Build 139](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.96-release-139.apk), code166, Quelle `9fab848d747bd0dad3afca805db501429d970bc3`, 4279128 Bytes, SHA-256 `32393bd51071b2bd15730ae46ab2d58600caf2e52ce5bd15e4decd07d36833f6`. Dauerhafte Signatur, Manifest, ZIP-/ARM64-16-KB-Ausrichtung, vier ABIs, 28 Shader, Bright-/Terrain-/Cycling-Assets, Texte und R8-Namen geprüft. [Nachweis](build-139.md). Vorige APK: [0.8.95 / Build 138](build-138.md).

## Weitere Planung

Keine automatische Stiländerung im Akkusparmodus. Minimalstil/OLED und Dashboardoption offen; Maputnik als Browsereditor, kein Live-JSON-Import beauftragt. Sensoren bleiben zuverlässig in voller Auflösung.

Nächste APK nur auf ausdrücklichen Auftrag über GitHub Actions, voraussichtlich 0.8.97 / code 167 / Build 140. Appcode nie mit [skip ci] überspringen. Dauerhafte Signatur/28 Shader/Bright-Ressourcen und tatsächlichen direkten Download prüfen.
