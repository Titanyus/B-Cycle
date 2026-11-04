# Aktueller Stand: APK 0.8.88 / Build 129 veröffentlicht

## Aktueller Auftrag abgeschlossen: neue APK

Version 0.8.88 / Build 129 über GitHub Actions gebaut und geprüft. [Änderungen](release-0.8.88.md), [Buildnachweis](build-129.md). Enthält alle folgenden Korrekturen. Smartphone-GPU/Zoom-Eindruck offen.

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

[APK 0.8.88 / Build 129](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.88-release-129.apk), code 158, Quelle `07b8d5b94a566d46e02ff793ec13fd488b50efd0`, 4189008 Bytes, SHA-256 `947936351d20fc760351fe11b88a7be328850470c0a3b926bcb828ac33421fbe`. Dauerhafte Signatur, Manifest, ZIP-/ARM64-16-KB-Ausrichtung, vier ABIs, 28 Shader, Bright-Assets, Texte und R8-Namen geprüft. [Nachweis](build-129.md). Vorige APK: [0.8.87 / Build 128](build-128.md).

## Weitere Planung

Keine automatische Stiländerung im Akkusparmodus. Minimalstil/OLED und Dashboardoption offen; Maputnik als Browsereditor, kein Live-JSON-Import beauftragt. Sensoren bleiben zuverlässig in voller Auflösung.

Nächste APK nur auf ausdrücklichen Auftrag über GitHub Actions, voraussichtlich 0.8.89 / code 159 / Build 130. Appcode nie mit [skip ci] überspringen. Dauerhafte Signatur/28 Shader/Bright-Ressourcen und tatsächlichen direkten Download prüfen.

Downloadnachweis: veröffentlichter direkter Raw-Link abgerufen, 4189008 Bytes, bytegenau identisch zum geprüften Actions-Artefakt; SHA-256 ebenfalls identisch.
