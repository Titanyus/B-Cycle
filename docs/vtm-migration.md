# Mapsforge-VTM: erste Umstellungsstufe mit bisherigen Raster-Tiles

Nutzerauftrag01.11.2026: bisherige Mapsforge-Engine gegen VTM austauschen, alle Kartenfunktionen auf dem Dashboard erhalten und zunächst dieselben Raster-Tiles verwenden. Invertierte Nachtkarte darf zurückgestellt werden. Keine Online-Vektorkarten, kein allgemeiner Akkuschonmodus; APK0.8.79/Build120 danach ausdrücklich beauftragt und über GitHub Actions veröffentlicht. Rückfallstand: [Meilenstein0.8.78/Build119](milestone-0.8.78.md), Git-Tag`v0.8.78`.

## Implementierung

- `mapsforge-map-android` entfernt; Produktionscode importiert keine alte Mapsforge-Map-/Core-API mehr. Stabile Bibliotheken `vtm-android`/`vtm-http`0.25.0, native Varianten für arm64-v8a, armeabi-v7a, x86 und x86_64. OpenGL ES2 als Geräteanforderung. LGPL-Hinweis aktualisiert.
- `BicycleMapView` kapselt die echte VTM-MapView, Kamera, Anker, Lebenszyklus und vorübergehende GL-Aufnahme. Tilegröße256 erhält die bisherige Zoomdarstellung. VTM-Shader werden aus Android-Assets bzw. den unveränderten Bibliotheksressourcen geladen.
- CyclOSM, OSM, OpenTopoMap und OpenCycleMap bleiben erhalten: dieselben HTTPS-Hosts/Pfade, Attributionen, User-Agent, Thunderforest-Key und Zoomgrenzen. VTM-Rasterlayer mit zwei Lade-Threads pro Ansicht; gemeinsamer begrenzter64-MB-HTTP-Cache statt altem Mapsforge-Tilecache. Kein Regionsdownload/Prefetch. HTTP-Cache beachtet Serverantworten; alter Diskcache wird nicht als VTM-Cache interpretiert.
- Kamera: unveränderte gespeicherte Zoom-/Positionsschlüssel, Zoom6–19 in0,5-Schritten und kontinuierlicher Pinch mit Pan/Nachlauf, OpenTopoMap bis17. VTM übernimmt native Einfingerbewegung/Nachlauf und Doppeltipp. Eigener Pinch bleibt zuständig für schmale Spannweiten, Fokusverschiebung und Grenzen; neue Berührung/Buttons stoppen auch VTM-Nachlauf.
- GPS-Folgen, Fahrtrichtung/Norden oben, zoomabhängiger Vorausschauanker und bestehende zeitversetzte33-ms-Interpolation erhalten. Manuelles Verschieben löst Folgen, GPS-Button stellt es mit dem letzten Modus wieder her. Die Bildschirmprojektion wird direkt gegen VTM geprüft; bei Fahrt nach Osten ist Osten oben und Norden links. Keine aus der alten API übernommene ungeprüfte Rotationsmatrix.
- Drei gespeicherte Streckenplätze mit Sichtbarkeit/Farben/Breite/Deckkraft, Endpunkten und separatem Live-Track bleiben erhalten. Strecken-/Positionsdarstellung nun als transparenter nativer View über der GL-Karte, anhand derselben VTM-Projektion. Pfade werden auf einem begrenzten Worker bei Änderung der Punktlisten vorbereitet, danach pro Bild transformiert. Keine Datei-/FIT-Verarbeitung im Zeichenpfad; Profilauswahl und Generationsprüfung entfernen alte Geometrie. Segmentanfänge bleiben echte Lücken, Datumsgrenze wird berücksichtigt. Das ist noch keine Umstellung dieser Overlays auf VTM-Vektorlayer.
- GPS-Punkt, Genauigkeitskreis mit bisherigen15/10-m-Schwellwerten, Smartphone-Kompasspfeil und aufrechte Höhenbeschriftung erhalten. Marker-/Schriftgrößen sind unabhängig vom Zwischenzoom. Windrose, Kartenwerte, Status, Quellenhinweis, Streckenpopup, Editor und Vollbild bleiben native Bedienelemente über der Karte.
- Dashboardwechsel behalten eine einzige lebende Dashboardkarte. Vor dem Seitenbild wird die separate GL-Fläche asynchron über PixelCopy aufgenommen (höchstens1Mio.Pixel/4MB,200-ms-Zeitgrenze). Das bestehende Seitenbild bleibt auf4Mio.Pixel/16MB begrenzt; GL-Bild direkt danach freigeben. Während Vorbereitung alte Seite behalten; nach Abbruch/Fokuswechsel verspätete Ergebnisse ignorieren. Ohne verfügbare Aufnahme sofortiger Wechsel statt Übergang mit fehlender Karte. Keine zweite MapView für den Übergang.
- Detailvorschau und Detailvollbild verwenden ebenfalls VTM; Einpassen, Attribution, Zoom und Wiederanfügen im Dialoghost erhalten. Abhängen pausiert; endgültiges Schließen gibt Layer/Geometrie/Engine frei.
- Invertierte Rasterdarstellung und Nachtbutton zunächst deaktiviert. Gespeicherte Nachtpräferenz und Liniengestaltung bleiben kompatibel; der bisherige Raster-Invertierungs-/Tilecachepfad ist entfernt. Palette für bestehende Stilvorschauen bleibt isoliert erhalten.

## Gezielt geprüft

Keine Gesamtsuite und kein lokaler APK-Build.19 betroffene Fälle wurden einschließlich der nach Korrekturen einzeln wiederholten Fälle geprüft:

- Ursprüngliche Kartenauswahl/URLs, API-Key und Quellwechsel mit fehlendem/ungültigem Schlüssel.
- Reale native VTM-Kamera: Zwischenzoom, Rotation, Bildschirmanker und tatsächliche Himmelsrichtung.
- Drei unabhängige Linienfarben/Deckkraft, Livefahrt und sichtbare GPS-Lücke im Pixeltest.
- GPS-/Kompass-/Höhenpixel über Zwischenzoom und Rotation; Höhe aufrecht und unverändert groß.
- Drei Pinch-Fälle: gemeinsamer Fokus/Pan, kleine Spannweiten/Nachlauf, Grenzen/Abbruch/Ein-Finger-Zuständigkeit.
- GPS-Interpolation/Standstill/Pause, Zoom-Vorausschau, Windrose, automatisches Ausblenden/Vollbild, vier Dashboardseiten mit derselben MapView, Nord-/Fahrtrichtungswiederherstellung und Kompasslebenszyklus.
- Map-freies Dashboard pausiert Karte und verändert die GPS-Einstellung nicht; Detailkarte überlebt Dialog-Wiederanfügen.
- Asynchrone GL-Aufnahme hält alte Seite bis zum Ergebnis, verwirft verspätete Resultate nach Abbruch und wechselt bei fehlender Aufnahme genau einmal ohne defekten Übergang.

Logs unter `/workspace/.tools/`: `vtm-smoke-test.log`, `vtm-targeted-tests.log`, `vtm-pinch-page-tests.log`, `vtm-orientation-page-tests.log`, `vtm-snapshot-assets-tests.log`, `vtm-provider-compass-packaging-tests.log`, `vtm-provider-packaging-final.log`, `vtm-marker-label-test.log`. Erste Sammelläufe enthielten offene Fälle; entscheidend sind die später erfolgreichen gezielten Wiederholungen. JNI-Ladepfad je Robolectric-Sandbox isoliert über einen reinen Test-Shadow; echte VTM-Projektionsfunktionen bleiben aktiv. Alte Engine-interne Cache-/Framebufferprüfungen durch passende VTM-/Projektions-/Overlayfälle ersetzt. Gestenhelfer dispatcht an den echten Dashboard-Viewport, nicht direkt am Eltern-Gestenpfad vorbei an eine Kachel.

`compileDebugKotlin`, Zusammenführung nativer Release-Bibliotheken/Java-Ressourcen, Typografie/Atlas und `git diff --check` erfolgreich. Im zusammengeführten Stand alle vier ABI-Bibliotheken und25 Shader vorhanden. ARM64-ELF mit16-KB-Alignment geprüft. Zusammenführung ist kein APK-Build.

## Noch auf dem Gerät zu bestätigen

OpenGL-Tilezeichnung, SurfaceView-Zusammensetzung/Clipping während Drag/Vollbild, PixelCopy und das tatsächliche Wischgefühl brauchen einen Android-Gerätetest. Die lokalen Prüfungen belegen numerische Kamera-/Overlay-/Bedienungslogik und verfügbare Ressourcen, keine tatsächliche GPU-Ausgabe oder Verbrauchsersparnis. Mehrere Kartenansichten und Pause/Rückkehr ebenfalls im echten Dialogablauf bestätigen. [APK0.8.79/Build120](build-120.md) enthält die Migration. Der gesicherte Rückfallstand0.8.78/Build119 verwendet weiterhin die alte Engine.

Danach können Vektorquelle/Themes und der getrennt geplante Akkuschonmodus folgen. Sensorerfassung, Aufzeichnung und Analyse bleiben in diesem Umbau unverändert vollständig.

## Korrektur nach Gerätefeedback zu Build120

Strecken hinkten auf der getrennten Android-Zeichenfläche beim schnellen Ziehen hinterher. Lokal anschließend in den gemeinsamen VTM-GL-Frame verlagert, einschließlich Start-/Endmarkern; GPU-Geometriecache, einteilige Livefahrt-Umrandung und gesicherte Raster-Layerreihenfolge. GPS-Informations-View bleibt separat. [Aktuelle Änderung und gezielte Nachweise](vtm-route-frame-sync.md). Noch nicht in Build120 enthalten.
