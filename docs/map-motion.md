# Weiche GPS-Kartenbewegung

Die vorhandene Mapsforge-Karte verwendet eine reine Anzeigeanimation zwischen bereits
empfangenen GPS-Positionen. `MapMotion` interpoliert für 450 ms, `MapPanel` zeichnet
höchstens ungefähr alle 33 ms neu (rund 30 Bilder/s). Ein neuer GPS-Punkt beginnt an der
aktuell dargestellten Zwischenposition. Wiederholte gleiche Ziele starten den Übergang
nicht erneut. Die Animation endet am empfangenen Wert; es gibt keine Vorhersage.

Kamera, Drehung und Positionsmarker verwenden dieselbe dargestellte GPS-Kopie. Die
Latitude wird in Mercator-Koordinaten interpoliert, Longitude und Richtung verwenden den
kürzesten Winkelweg, auch beim Wechsel 359° → 1° und über die Datumsgrenze. Die vorhandene
gefilterte GPS-Richtung bleibt Datenquelle; keine neue Sensorfilterung.

Nur bei frischer Bewegung, aktivem Folgen und sichtbarer Karte wird animiert. Erster Fix,
Stillstand, Wiederaufnahme nach App-Unterbrechung und Sprünge über 200 m werden direkt
übernommen. Manuelles Verschieben oder Berühren pausiert die laufende Animation. In
manueller Ansicht bleibt die Kameraposition erhalten; erneutes Zentrieren nutzt wie bisher
den Zentrierungsbutton. Bei überdeckenden Einstellungsdialogen laufen keine Animationsframes.
North-up und Fahrtrichtungsansicht behalten die bisherigen Positionen im unteren Drittel.

Die Frames aktualisieren nur Kamera und vorhandene Kartenebene. Sie rufen weder
`MainActivity.render()` noch Dashboard-/Graphverarbeitung auf. Die bereits zwischengespeicherte
Trackliste wird nur bei geänderter Trackrevision kopiert; Kartenkacheln verwenden weiterhin
den bestehenden Cache. Es gibt keinen zusätzlichen Sensor, keine häufigeren GPS-Abfragen,
keinen FIT-Zugriff und keine neuen Nutzereinstellungen. Der dargestellte Standort folgt
bewusst mit höchstens einem kurzen Übergang den empfangenen Rohdaten.

Pause/Destroy entfernt ausstehende Frame-Callbacks. Kontrollbeschriftungen/Tints werden
nicht mit Frame-Frequenz gesetzt. GPS-Tripwerte, Aufzeichnung, FIT und Performance Engine
bekommen weiterhin ausschließlich die bisherigen unveränderten Daten.

## Prüfungen

`MapMotionTest`: erster Fix, Zwischenposition, synchroner kürzester Richtungsweg,
unveränderte Tripwerte, laufendes Retargeting, Abschluss, Stillstand, große Sprünge,
Reset, Datumsgrenze und wiederholte Ziele.

`MapPanelTest`: tatsächliche Zwischenposition der Mapsforge-Kamera, unveränderte
GPS-Daten, manueller Abbruch, Pause/Resume, Dialogüberdeckung, Stillstand, erhaltene Lower-third-/North-up-
Ausrichtung und alle bisherigen Kartenbedienungsprüfungen.

Tatsächliche Bildrate, Energieverbrauch und subjektive Flüssigkeit werden mit einer
realen Fahrt auf dem Zieltelefon geprüft; JVM-Tests ersetzen diese Messung nicht.

Abschlussprüfung zusammen mit dem Dashboard-Editor: **354 Tests, 0 Fehler**,
Release-Build einschließlich Lint/R8 erfolgreich; abschließend 1 Minute 57 Sekunden.
