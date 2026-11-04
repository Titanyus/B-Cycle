# Aktueller Stand: APK 0.8.87 / Build 128 veröffentlicht

## Aktueller Auftrag abgeschlossen

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

[APK 0.8.87 / Build 128](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.87-release-128.apk), code 157, Quelle `d60be354990fd63f1de57f6666ab9b74751f2f80`, 4188317 Bytes, SHA-256 `1ac96e326420a15ea4980253743ae6dba8b31b87ac92681555c1ea24aacdd9f8`. Dauerhafte Signatur, Manifest, ZIP-/ARM64-16-KB-Ausrichtung, vier ABIs, 27 Shader, Bright-Assets, Texte und R8-Namen geprüft. [Nachweis](build-128.md). Vorige APK: [0.8.86 / Build 127](build-127.md).

## Weitere Planung

Keine automatische Stiländerung im Akkusparmodus. Minimalstil/OLED und Dashboardoption offen; Maputnik als Browsereditor, kein Live-JSON-Import beauftragt. Sensoren bleiben zuverlässig in voller Auflösung.

Nächste APK nur auf ausdrücklichen Auftrag über GitHub Actions, voraussichtlich 0.8.88 / code 158 / Build 129. Appcode nie mit [skip ci] überspringen. Dauerhafte Signatur/27 Shader/Bright-Ressourcen und tatsächlichen direkten Download prüfen.

Downloadnachweis: veröffentlichter direkter Raw-Link abgerufen, 4188317 Bytes, bytegenau identisch zum geprüften Actions-Artefakt; SHA-256 ebenfalls identisch.
