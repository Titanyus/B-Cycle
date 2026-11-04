# Ruhige Beschriftung bei Zoom und nachgeladenen Kacheln

Auftrag 04.11.2026: Große Orte und Straßen bevorzugen, bereits sichtbaren Namen bei gleicher Bedeutung Vorrang geben. Farben, Schriftgrößen, Zoomgrenzen und Original-Bright-JSON bleiben unverändert.

## Priorität

VtmBrightTheme ergänzt ausschließlich die TextStyle-Priorität: Kontinent, Land, Bundesland/Provinz, Stadt, Kleinstadt, Dorf, Weiler, Stadtteil und übrige Orte. Innerhalb einer Ortsklasse zählt der OpenMapTiles-Rang, sofern vorhanden; fehlender Rang steht hinter vorhandenen Rängen derselben Klasse. Das ist eine Einordnung anhand der Kartendaten, keine neue Abfrage von Einwohnerzahlen.

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
