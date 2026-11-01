# B|CYCLE0.8.79 – Mapsforge-VTM

- Kartenengine auf Mapsforge-VTM0.25.0 umgestellt; zunächst dieselben Raster-Tiles und Anbieter einschließlich Thunderforest-Schlüssel.
- GPS-Folgen, Norden/Fahrtrichtung, Positionsanker, kontinuierlicher Pinch/Pan/Nachlauf,0,5-Zoomschritte, Windrose, Kartenwerte, Editor und Vollbild übernommen.
- Drei gespeicherte Strecken und Livefahrt mit bisherigen Farben/Breiten/Deckkraft sowie echten GPS-Lücken. GPS-Punkt, Genauigkeitskreis, Kompass und aufrechte Höhenangabe erhalten; Geometrievorbereitung auf Worker bei Datenänderung.
- Dashboardwechsel nimmt die GL-Fläche asynchron auf und verwendet weiterhin eine lebende Dashboardkarte. Detailkarten ebenfalls VTM.
- Invertierte Nacht-Tiles und Nachtbutton zunächst deaktiviert; gespeicherte Einstellung/Stile kompatibel erhalten. Sensorerfassung, Aufzeichnung und Analyse unverändert.

[Umstellung und19 gezielte Prüffälle](vtm-migration.md). Debugkompilation, Native-/Java-Ressourcen-Zusammenführung mit vier ABIs/25 Shadern, ARM64-16-KB-Alignment, Typografie/Atlas/Diff geprüft. Kein Gesamtlauf. Tatsächliche OpenGL-Ausgabe, Zusammensetzung/Clipping, PixelCopy und Wischgefühl sowie App-/Dialogrückkehr sind am Smartphone noch zu bestätigen.

Kein allgemeiner Akkuschonmodus und keine Online-Vektorkartenquelle eingeführt. [Gesicherter Rückfallstand0.8.78/Build119](milestone-0.8.78.md).

Buildnachweis und Download folgen nach erfolgreichem GitHub-Actions-Lauf.
