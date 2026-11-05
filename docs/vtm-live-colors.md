# Direkte VTM-Farbvorschau

Auftrag 05.11.2026 nach dem separaten RGB-Editor: das Verfahren aus der offiziellen [ThemeStylerActivity](https://github.com/mapsforge/vtm/blob/master/vtm-android-example/src/org/oscim/android/test/ThemeStylerActivity.java) für vorhandene Renderstile übernehmen.

## Verhalten

Reine RGB-Änderungen an Hintergrund, Flächen und Linien aktualisieren vorhandene Kartenstile. Vektor-/Höhenlinien-Layer, Tilemanager, Datenquellen, Geometrie-/Vertexpuffer, Linienmaskentexturen und Beschriftungsplatzierung bleiben erhalten. Kartenposition/Zoom/Tracks/GPS und Profil-/JSON-Speicherung bleiben unverändert. Es wird nur ein neuer Frame angefordert; kein clearMap und kein Basis-Layerwechsel für diesen Farbpfad.

RGB-Regler veröffentlichen den neuesten Wert spätestens nach dem gebündelten 48-ms-Fenster, auch während fortlaufender Bewegung. Es wird nicht bei jedem Sliderereignis der Timer neu gestartet. Auf der zugehörigen GL-Queue höchstens ein ausstehendes Farbupdate, neuester gesamter Profil-Stilsnapshot gewinnt. Beim Detach ausstehende Updates verwerfen, damit kein veralteter Layer mehr verändert wird. Reine Objektfarben nach Möglichkeit nur für tatsächlich geänderte Ziele bearbeiten.

## Native Stilreferenzen

`VtmLiveTheme` indiziert beim Themeaufbau native LineStyle-/AreaStyle-Objekte über RuleVisitor, pro Objektidentität einmal. Vorhandene Buckets behalten genau diese Originalreferenzen. Auf der GL-Queue `RenderStyle.set(neuerPaint)`, `updateStyles()`, anschließend map.render. Die nativen Renderer lesen LineStyle.current()/AreaStyle.current(), wodurch vorhandene Geometrie unmittelbar die neuen Farben erhält.

Linien-/Flächenfarben beim XML-Kompilieren zunächst unverändert lassen; Dash-Auswahl und gebackene Textfarben bleiben im bisherigen XML-Pfad. Gespeicherte RGB-Werte vor dem ersten Einsatz des Themas direkt anwenden. Dadurch stehen exakte Originalfarben für jede Zoomvariante zur Verfügung, auch bei den zoomabhängigen Gebäudefarben und ursprünglichen Transparenzen. Zurücksetzen setzt den nativen Override wieder auf das Originalobjekt, nicht auf einen angenäherten einzelnen Standardfarbwert. Bis Build134 keine Schemaänderung; ab Build135 optionales ganzzahliges `alpha` 0–255 je Objekt, alte RGB-Deltas bleiben unverändert gültig.

Nur immutable Paint-Eigenschaften klonen; native Texturreferenzen übernehmen, keine Masken-/Atlasbitmaps oder Vertexpuffer neu anlegen. Bei Dash-Linien sowohl color als auch stippleColor aktualisieren, da der Shader die weiße Maske mit diesen Farben zeichnet. Ohne expliziten Alpha-Wert native Transparenz beibehalten, andernfalls absolute Deckkraft setzen (kein Multiplikator). Der Editor zeigt die bisherige Objekttransparenz an; Reset stellt die Originale je Variante wieder her. Straßenränder behalten wie zuvor ihre eigene Farbe. Wiederholte Farben erzeugen keine weiteren Paint-Updates.

Bei aktivierten Höhenlinien Grund-/Vordergrundthema und Höhenlinienthema in derselben GL-Aktion aktualisieren. Hintergrundfarbe sowohl im globalen Clear-Color als auch im Tile-Fade-Matte nachführen; Vordergrund-/Höhenlinienhintergrund bleibt transparent. Der feste Zeichenaufbau bleibt Gelände → Höhenlinien → Straßen/Gebäude → Namen → Tracks/GPS.

## Bewusster bestehender Neuaufbau

Andere Linienmuster benötigen neue native Masken/ggf. Buckettypen; Textfarben benötigen neu rasterisierte Glyphen. Für diese Änderungen sowie Quelle, Sichtbarkeit und Radwege-Schalter bleibt der vorhandene geprüfte Neuaufbau aktiv. `MapStyleOverrides.canUpdateColorsFrom` entscheidet anhand des gesamten alten/neuen Snapshots. Text-/Patternänderungen weiterhin 180 ms bündeln. Farben und Muster gemeinsam ändern oder Zurücksetzen mit Musterwechsel fällt ebenfalls in diesen Pfad.

## Gezielte Prüfung

- Bestehende native Stilreferenz und Dash-Textur bleiben über Rot/Cyan und Zurücksetzen identisch; beide Dash-Shaderfarben korrekt, Waldalpha und unterschiedliche Gebäude-Originalfarben je Zoom erhalten.
- Zwei schnelle Farbaufträge ergeben eine einzige GL-Aufgabe mit neuestem Snapshot; Grund-/Vordergrund-/Höhenlinieninstanzen und transparente Overlays erhalten, Detach verwirft alte Aufgaben.
- Tatsächlicher MapPanel behält seinen VtmBaseMap für Farbänderungen und baut bei Musterwechsel weiterhin neu auf. Vorherige Cycleway-/Editor-/Profil-/JSON-Rundreisefälle mit native current()-Farben geprüft.
- RGB-Reglerereignisse nach 0 und 24 ms lösen bereits nach 48 ms eine Vorschau aus; damit kein Warten bis zum Ende eines kontinuierlichen Ziehens.

Gezielte Prüfungen, Kompilation, Typografie, UI-Katalog, Bright-Generator und Diff grün; kein Gesamtlauf, keine lokale APK. Subjektive Vorschauflüssigkeit auf dem Gerät noch beurteilen. Keine neue APK angefordert.
