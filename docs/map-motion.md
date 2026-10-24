# Weiche GPS-Kartenbewegung und Smartphone-Richtung

Die bestehende Mapsforge-Karte verwendet ausschließlich bereits empfangene GPS-Positionen. Die GNSS-Anfrage bleibt bei 1000 ms Mindestintervall. Die Darstellung spielt einen kleinen Puffer von höchstens acht Anzeigepositionen ab, anfangs um 1100 ms verzögert. Zwischen den tatsächlichen Empfangszeitpunkten wird linear interpoliert, statt nach einem festen 450-ms-Übergang anzuhalten. Die Pufferverzögerung folgt geglättet dem Empfangsintervall (600–2600 ms); Änderungen der Verzögerung werden langsam nachgeführt, damit die sichtbare Position nicht springt oder rückwärts läuft. Maximal alle 33 ms werden Kamera und Kartenebene aktualisiert, ungefähr 30 Bilder/s.

Es gibt keine Extrapolation: Ohne neue Positionen endet die Bewegung am letzten gültigen Punkt. Mehr als fünf Sekunden ohne neuen Fix sowie Sprünge über 200 m werden nicht überbrückt. Erster Fix, Stillstand und Wiederaufnahme nach App-Unterbrechung werden direkt übernommen. Longitude und GPS-Kurs verwenden den kürzesten Winkelweg, Latitude wird in Mercator-Koordinaten interpoliert. GPS-Zähler und alle Live-Messwerte bleiben unverändert; ausschließlich die Anzeige erhält interpolierte Koordinaten.

Der Positionspunkt wird auch in manueller Kartenansicht und während des Verschiebens weich bewegt. Die Kamera wird nur bei aktivem Folgen und außerhalb einer Berührung nachgeführt. Manuelles Verschieben erhält Mittelpunkt und Drehung. Nach manuellem Verschieben ist der Modusbutton inaktiv; Zentrieren stellt den letzten Modus wieder her. Bei verdeckter oder pausierter Karte laufen keine Animationsframes.

## Kleiner Smartphone-Richtungspfeil

Der blaue Positionspunkt erhält einen kleinen blau-weiß gezeichneten Pfeil. Er zeigt die Ausrichtung des Smartphones, auch im Stand. PhoneHeading nutzt den absoluten Rotation-Vector-Sensor, ersatzweise den geomagnetischen Rotation-Vector-Sensor, mit einem kleinen Winkelfilter und maximal zehn ausgewerteten Sensorereignissen je Sekunde. Es werden keine Bewegungsrichtung und keine unzuverlässigen Game-Rotation-Daten als Smartphone-Kompass ausgegeben. Die lokale magnetische Deklination wird zwischengespeichert berücksichtigt, damit die Richtung zum geografischen Kartennorden passt.

GPS-Kurs bleibt ausschließlich die Quelle für den bestehenden Fahrtrichtungsmodus der Kamera. Smartphone-Drehung verändert weder Kamerarichtung noch GPS-/FIT-Daten. Bei fehlendem oder unzuverlässigem Sensor, ungültigen/veralteten Messwerten oder nicht bestimmbarer horizontaler Richtung fehlt nur der kleine Pfeil. Der Positionspunkt bleibt erhalten. Der Sensor wird nur für die sichtbare Karte mit verfügbarer Position abonniert und bei Verdeckung/Pause wieder abgemeldet. Keine neue Nutzereinstellung oder Bibliothek.

Kartenframes rufen weder MainActivity.render noch Dashboard-/Graphverarbeitung auf. Die Trackliste wird weiterhin nur bei geänderter Revision kopiert; Kartenkacheln verwenden den bestehenden Cache. Keine häufigeren GPS-Abfragen, Änderungen an Aufzeichnung oder Performance Engine.

## Gezielte Prüfungen

MapMotionTest: kontinuierliches Fortschreiten bei 1-Hz-Daten, unregelmäßiger Empfang, begrenzte Verzögerung, Ende am letzten echten Punkt, Ausfälle, große Sprünge, Datumsgrenze, kürzester Kurswinkel und unveränderte Fahrtwerte.

MapPanelTest: tatsächliche Mapsforge-Kamera und unabhängiger Positionspunkt in manueller Ansicht, Pause/Resume, verdeckte Karte, Stillstand, manuelle Ausrichtungsaktion und Kompass-Lebenszyklus ohne Einfluss auf GPS-Kamerarichtung.

PhoneHeadingTest: Winkelglättung über Norden, alte/unzuverlässige/fehlende Messwerte, Richtung im Stand sowie Sensorregistrierung und Abmeldung. RouteTrackLayerTest prüft die tatsächlich gezeichneten Pfeilrichtungen und den Positionspunkt ohne Kompassdaten.

Subjektive Flüssigkeit, Kompassgenauigkeit und Energieverbrauch sind anschließend auf dem Zieltelefon zu prüfen. APK-Build nach Nutzerfreigabe über GitHub Actions, kein lokaler APK-Build.

## Gemeinsamer Folgemodus und variabler Vorausschauanker

Ein Modusbutton wechselt zwischen Fahrtrichtung oben und Norden oben. Beide folgen automatisch der GPS-Position. Manuelles Verschieben setzt Folgen aus und deaktiviert den Modusbutton; der GPS-Button reaktiviert den zuletzt verwendeten Modus. Icons benennen den aktuellen Zustand. Alte aktivierte Oval-Einstellungen werden zu Norden oben migriert; vorhandene Backupfelder bleiben lesbar.

In Fahrtrichtung liegt der GPS-Anker unterhalb der Mitte, in Nordausrichtung auf der Ellipse entgegen dem gefilterten GPS-Kurs. Abstand vom Rand: Zoom 6–11 = 50 % (Mitte), 12 = 40 %, 13 = 37 %, 14 = 34 %, 15 = 31 %, 16 = 28 %, 17–19 = 25 %. Zwischenstufen werden linear interpoliert; 11–12 verbindet Mitte und 40 %. Radiusänderungen werden mit 350-ms-Zeitkonstante weich nachgeführt, auch im Stand. Winkel bleiben bei Stillstand/unbrauchbarem Kurs stehen; 4°-Totbereich und höchstens 60°/s bleiben erhalten. Keine Messdatenänderung.

MapZoom begrenzt Dashboard-, Vollbild- und Detailkarten auf 6–19, OpenTopoMap weiterhin bis 17. Plus/Minus verwenden echte Mapsforge-Zoomschritte von 0,5. Fractional-Zoom ist aktiviert; Gesten dürfen weiterhin feinere Zwischenwerte liefern. Neue Float-Preference mapZoomFractional ergänzt die bisherige ganzzahlige mapZoom, inklusive profilabhängiger Konfiguration und Sicherung. Alte Sicherungen mit ganzzahligem Zoom bleiben lesbar und werden auf den neuen Bereich begrenzt. Die temporäre Zoomanzeige ist entfernt.

Gezielte Tests prüfen Zoom-/Profil-/Sicherungsrückkehr, Kardinal-/Diagonalgeometrie, Ellipsengrenzen, weiche Radiusänderungen, Winkelgeschwindigkeit, Norddurchgang, Stillstand, echte Mapsforge-Projektion, manuellen Pan, Moduswiederherstellung und Layoutkollisionen.
