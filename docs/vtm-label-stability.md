# Ruhige Beschriftung bei Zoom und nachgeladenen Kacheln

Auftrag 04.11.2026: Große Orte und Straßen bevorzugen, bereits sichtbaren Namen bei gleicher Bedeutung Vorrang geben. Farben, Schriftgrößen, Zoomgrenzen und Original-Bright-JSON bleiben unverändert.

## Priorität

VtmBrightTheme ergänzt ausschließlich die TextStyle-Priorität: Kontinent, Land, Bundesland/Provinz, Stadt, Kleinstadt, Dorf, Weiler, Stadtteil und übrige Orte. Der OpenMapTiles-Rang sortiert nur neue Kandidaten; er darf innerhalb derselben Größenklasse keinen bereits sichtbaren Namen verdrängen. Fehlender Rang steht bei neuen Kandidaten hinter vorhandenen Rängen derselben Klasse. Das ist eine Einordnung anhand der Kartendaten, keine neue Abfrage von Einwohnerzahlen.

Straßen: Autobahn, Fernstraße, Hauptstraße, sekundäre Straße, tertiäre Straße, Wohn-/Nebenstraße, Zufahrt, Feld-/Fuß-/Radweg. Ortsbeschriftungen bleiben gegenüber Straßen bevorzugt. Kleinere Zahlen gewinnen. Höhere Bedeutung gewinnt vor Sichtbarkeitsalter; bei gleicher Bedeutung bleibt der früher eingeblendete Name. Neue gleichwertige Kandidaten werden bei gleicher Einblendegeneration deterministisch nach Name/Position aufgelöst, damit die Tile-Ladereihenfolge kein Zufallskriterium ist.

Native Stilvarianten liegen in einem synchronisierten, auf 256 Einträge begrenzten Cache für die zwei bestehenden Tile-Worker. Paint-/Layoutdaten werden übernommen; Symbolbitmaps werden nur referenziert und nicht doppelt freigegeben. Schrift-Skalierung und Dispose leeren den Varianten-Cache. Kein Prioritätsrechnen oder neuer Overlay-Renderer auf dem UI-Thread, keine Vergrößerung des generierten Bright-XML.

## Sichtbare Namen behalten

Die Standard-VTM-Platzierung verwirft Caption-Labels vor jeder Neuplatzierung. BCycleLabelPlacement behält sie dagegen, solange das Merkmal in den aktuell gezeichneten Tiles noch vorhanden und sichtbar ist. Ortsnamen werden über Namen, Stilfamilie und räumliche Nähe der Anker wiedererkannt, auch beim Tile-/Stilwechsel zur nächsten Zoomstufe. Größe, Stil, Anker und Priorität werden an das aktuelle Merkmal angepasst; das ursprüngliche Einblendealter bleibt bestehen. Die Suche ist auf aktuelle Tiles und 64 Pixel Ankerabweichung begrenzt.

Die bisherige Beibehaltung von Straßennamen bleibt, berücksichtigt nun ebenfalls Priorität und aktuelle Merkmalsverfügbarkeit. Bei Konflikten wird zuerst gegen alle sichtbaren Namen geprüft und erst dann verdrängt; ein später abgewiesener Kandidat darf zuvor keine anderen Namen löschen. Lokale doppelte Namen werden weiterhin unterdrückt. Fehlende Merkmale, entfernte Bereiche und große Zoomsprünge werden nicht als alte Beschriftungen festgehalten.

Kein Einfrieren der Karte, keine verzögerte Gestenreaktion oder globale Verringerung der Aktualisierung. Native GL-Texturen, TextRenderer-Matrizen, Tile-/Worker-/Layer-Lebenszyklus und der dauerhafte Dashboard-Kartenhost bleiben erhalten. Das Gerätebild während echtem Pinch-Zoom ist noch zu beurteilen; eine Gleichheit mit der Platzierung anderer Kartenengines wird nicht behauptet.

## Herkunft der lokalen VTM-Anpassung

Die drei Klassen unter `app/src/main/java/org/oscim/layers/tile/vector/labeling/` sind gezielt umbenannte lokale Ableitungen der VTM-Beschriftung:

- BCycleLabelPlacement aus [LabelPlacement.java](https://github.com/mapsforge/vtm/blob/0.25.0/vtm/src/org/oscim/layers/tile/vector/labeling/LabelPlacement.java): Kollisionsrangfolge und Beibehalten aktueller Caption-/Straßenmerkmale.
- BCycleLabelLayer aus [LabelLayer.java](https://github.com/mapsforge/vtm/blob/0.25.0/vtm/src/org/oscim/layers/tile/vector/labeling/LabelLayer.java): verbindet lokalen Placer und Renderer mit dem vorhandenen Worker und LabelTileLoaderHook.
- BCycleTextRenderer aus [TextRenderer.java](https://github.com/mapsforge/vtm/blob/0.25.0/vtm/src/org/oscim/layers/tile/vector/labeling/TextRenderer.java): unveränderte Renderinglogik, Worker-Typ der lokalen Klasse.

Copyright-/LGPL-Hinweise und Änderungsdatum stehen in den Quelldateien; vorhandene Mapsforge-Lizenz und Quellenhinweise bleiben erhalten. Die Namen sind eigenständig, es werden keine Klassen aus dem VTM-JAR überschrieben oder per Reflection zur Laufzeit ausgetauscht. Die gemeinsame Package-Zuordnung ermöglicht die Nutzung der vorhandenen Label-/Task-/Pooltypen ohne duplizierte Grafikengine.

## Gezielte Prüfung

Zwei native Platzierungstests verwenden reale VTM-Textstyles, Tile-Daten und LabelTasks: Ortsname bleibt trotz späterem gleichwertigem Kandidaten und Zoom-/Stilwechsel, wichtigerer Ort darf verdrängen, fehlendes Merkmal verschwindet; Straßenklasse gewinnt vor Alter, gleichwertiger späterer Name verdrängt nicht. Bestehende Vektortests prüfen Priorität im tatsächlich geladenen Theme und Wechsel Vektor/Raster mit weiter darüberliegenden Tracks. Kein Gesamtlauf und keine lokale APK.


## Verfeinerung nach Build 128

Nutzer beobachtet weiter kreisende Ortsnamen und überfüllte Übersicht. Die Platzierung behandelt deshalb die Größenklasse vor dem Einblendealter und erst neue Kandidaten nach ihrem Tile-Feinrang. Neue Caption-Kandidaten werden für den gesamten Viewport gesammelt und gemeinsam geordnet, statt je nach Tile-Reihenfolge einzeln um Plätze zu konkurrieren. Abstände um Ortsnamen wachsen auf mindestens drei Schrifthöhen horizontal und anderthalb vertikal; echte Kartenlinien und Schriftgrößen bleiben gleich.

Zoomkohorten (VTM-256-Pixel-Zoom): Länder ab Übersicht, Regionen ab 6,25, Städte 9,25, Kleinstädte 11,25, Dörfer 13,25, Weiler 15,25, Stadtteile/übrige kleine Orte 16,25. Neue Kandidaten kommen erst 0,2 Stufen oberhalb der Schwelle hinzu; vorhandene dürfen seit Verfeinerung nach Build 130 bis 0,8 Stufen darunter bleiben. Beim Verlassen/Hinzunehmen einer Größenklasse beginnen ihre Namen gemeinsam einen zeitlichen 240-ms-Übergang. Das gilt auch bei direkten Zoomsprüngen. Die Schwellen liegen zwischen den bedienbaren halben Zoomstufen; es gibt keine dauerhaft halbtransparenten Gruppen.

BCycleLabelFades führt auf dem vorhandenen Label-Worker eine auf 1024 Einträge begrenzte Übergangshistorie. Namen erhalten bei Ein-/Ausblendung 240 ms mit weicher Interpolation. Kehrt ein Name während des Ausblendens zurück, setzt er bei seiner aktuellen Deckkraft und seinem ursprünglichen Sichtbarkeitsalter fort. Quellenwechsel, nicht mehr passende/offscreen Anker und Cleanup verwerfen die Historie. Nach Fade-Ende werden keine zusätzlichen Animationsframes mehr angefordert.

BCycleLabelTask enthält den spezialisierten BCycleTextBucket. Native sechs-Short-Glyphvertices, UVs, Atlaspackung und Map-Matrizen bleiben erhalten. Ein separater kleiner Metadaten-VBO ergänzt pro Vertex Start-/Zielalpha und Startzeit/Dauer. `bcycle_labels.glsl` (jetzt insgesamt 28 Shader) multipliziert die premultiplizierte Textur mit der zeitlichen Deckkraft; die Größenklassenübergänge werden gemeinsam von der Platzierung ausgelöst. Das Alpha wird pro GL-Frame interpoliert; dafür werden keine Glyphbitmaps pro Animationsframe neu erzeugt. Zusätzliche Frames nur bis Fade-Ende oder durch die laufende Kamerabewegung. Systemweit deaktivierte Animationen schalten Zeit-Fades ab.

Shaderprogramme sind an den jeweiligen Renderer/GL-Kontext gebunden. VtmBaseMap erhält die Queue der zugehörigen MapView und gibt Label-VBOs/Programm beim Detach auf dieser GL-Queue frei; Detach ist idempotent. Keine neue MapView und kein Umhängen des dauerhaften Dashboard-Kartenhosts.

Prüfung: bestehende reale Tile-/Zoom-/Stiltests ergänzt um kurz verschwindende/wiederkehrende Namen; zusätzlicher Kohorten-/Hysterese-/Tile-Rang-/Fade-Umkehrfall. `python3 scripts/check-vtm-label-shader.py` prüft auf Linux mit surfaceless EGL echten GLES-Compile/Link und Alpha-Pixel für Einblenden, Ausblenden und fortgesetzte/umgekehrte Übergänge. Die Kohorten-/Hysterese-Logik ist durch den nativen Platzierungstest abgedeckt. Layerwechsel-Test ebenfalls grün. Reale Smartphone-GPU und subjektiver Zoom-Eindruck noch offen; keine APK aus lokalen Tests erzeugt.

Der zusätzliche BCycleTextBucket-Drawer basiert auf [TextureBucket.java](https://github.com/mapsforge/vtm/blob/0.25.0/vtm/src/org/oscim/renderer/bucket/TextureBucket.java); seine LGPL-/Copyright-Hinweise sind in der Datei erhalten. BCycleLabelTask, BCycleLabelPolicy und BCycleLabelFades ergänzen die umbenannten VTM-Klassen für Aufgabenübergabe, Auswahlpolitik und Übergangszustand.

## Korrektur nach Build 129: fehlende Glyphen und flimmernde Schilder

VTM GLState.enableVertexArrays verwaltet nur Slots 0/1. Der eigene Textshader hat drei Attribute; vergibt der Treiber UV-Slot 2, wurde dieser nicht aktiviert und die transparente Atlas-Ecke statt der Schrift gelesen. Der Drawer leert nun den nativen Slotcache, aktiviert alle drei tatsächlichen Attributpositionen und deaktiviert sie im finally-Block. Nachfolgende VTM-Layer erhalten einen konsistenten Zustand.

Straßennummern samt Hintergrund werden in derselben Atlaszelle rasterisiert. Dadurch teilen Schild und Zahl ein Quad und denselben Fade statt einer unabhängig sichtbaren weißen Symbolfläche. Übrige Texte behalten die native Atlasgeometrie.

Gezielte Prüfung: Java-GL-Zustandstest mit sieben Treiberbelegungen, nativer Atlas-/Platzierungstest mit weißen Schildpixeln und tatsächlicher schwarzer Zahl sowie bestehender Beibehaltungstest erfolgreich. Erweiterte echte GLES-Pixelprüfung erzwingt vier Attributbelegungen und eine transparente Atlas-Ecke; reproduziert den vorherigen Fehler und prüft Glyphen/Fades nach der Korrektur. Keine lokale APK; Geräteprüfung folgt mit nächstem angeforderten Build.

## Folgekorrektur nach Build 130: wechselnde Straßenplatzierungen

Nutzer präzisiert: Autobahn-/Bundesstraßenlabels, Straßennamen und Flussnamen flimmern zwischen zwei Positionen, teils scheinbar gespiegelt. Die Übergangshistorie hatte Name/Stil plus einen sehr groben festen Weltpositionsraster als Identität (32768 Zellen um die Erde). Bei Zoom 16 entspricht eine Zelle 512 Bildpunkten. Zwei gleichnamige, gleichzeitig zulässige Straßenlabels konnten dadurch denselben Zustand überschreiben und je nach Listenreihenfolge Position/Ausrichtung tauschen.

Statt Rasterkollision nun räumlich nächste gleiche Textfamilie innerhalb 48 tatsächlicher Kartenpixel, mit Antimeridian-Korrektur und echter aktueller Zoomskala. Pro Platzierungsrunde jeden Zustand höchstens einmal vergeben; neue Platzierungen erhalten eigene IDs. Die aktuelle Labelgeometrie, native Ausrichtung und separaten Fade-VBOs bleiben erhalten. Bereits sichtbare Ortsnamen erhalten zusätzlich 0,5 Zoomstufen mehr Hysterese (0,8 unter der Schwelle statt 0,3), neue Namen unverändert.

Regression für Straße und Fluss: jeweils zwei B16-/Donau-Labels innerhalb derselben alten Weltzelle mit verschiedenen Richtungen bleiben nach vertauschter Ankunftsreihenfolge zwei unabhängige Glyphenpositionen; kein Zusammenfallen mehr. Bestehende native Platzierungs-/Fadefälle ebenfalls grün. Geräteprüfung bei den gemeldeten Autobahn-/Bundesstraßenabschnitten folgt; kein pauschaler Beleg für sämtliche denkbaren Glyphenorientierungen.

## Dichte nach Build 132

Hauptstraßen-Schilder/-namen mit radialem, zoomunabhängigem Wiederholungsabstand und einheitlicher Kennung über Stil-/Leerzeichenvarianten. Neue Hauptstraßenkandidaten über den gesamten Viewport sammeln und größere Lücken bevorzugt auffüllen; aktuelle Namen/Alter behalten. [Befund, offene VTM-Referenzen und native Regression](vtm-maxzoom-and-road-labels.md).
