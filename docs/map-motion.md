# Weiche GPS-Kartenbewegung und Smartphone-Richtung

Die bestehende Mapsforge-Karte verwendet ausschließlich bereits empfangene GPS-Positionen. Die GNSS-Anfrage bleibt bei 1000 ms Mindestintervall. Die Darstellung spielt einen kleinen Puffer von höchstens acht Anzeigepositionen ab, anfangs um 1100 ms verzögert. Zwischen den tatsächlichen Empfangszeitpunkten wird linear interpoliert, statt nach einem festen 450-ms-Übergang anzuhalten. Die Pufferverzögerung folgt geglättet dem Empfangsintervall (600–2600 ms); Änderungen der Verzögerung werden langsam nachgeführt, damit die sichtbare Position nicht springt oder rückwärts läuft. Maximal alle 33 ms werden Kamera und Kartenebene aktualisiert, ungefähr 30 Bilder/s.

Es gibt keine Extrapolation: Ohne neue Positionen endet die Bewegung am letzten gültigen Punkt. Mehr als fünf Sekunden ohne neuen Fix sowie Sprünge über 200 m werden nicht überbrückt. Erster Fix, Stillstand und Wiederaufnahme nach App-Unterbrechung werden direkt übernommen. Longitude und GPS-Kurs verwenden den kürzesten Winkelweg, Latitude wird in Mercator-Koordinaten interpoliert. GPS-Zähler und alle Live-Messwerte bleiben unverändert; ausschließlich die Anzeige erhält interpolierte Koordinaten.

Der Positionspunkt wird auch in manueller Kartenansicht und während des Verschiebens weich bewegt. Die Kamera wird nur bei aktivem Folgen und außerhalb einer Berührung nachgeführt. Manuelles Verschieben erhält Mittelpunkt und Drehung. Der bestehende Ausrichtungsbutton kann die Drehung weiterhin ausdrücklich ändern, ohne die Karte ungefragt auf die Position zurückzusetzen. Zentrieren bleibt eine bewusste Aktion. Bei verdeckter oder pausierter Karte laufen keine Animationsframes.

## Kleiner Smartphone-Richtungspfeil

Der blaue Positionspunkt erhält einen kleinen blau-weiß gezeichneten Pfeil. Er zeigt die Ausrichtung des Smartphones, auch im Stand. PhoneHeading nutzt den absoluten Rotation-Vector-Sensor, ersatzweise den geomagnetischen Rotation-Vector-Sensor, mit einem kleinen Winkelfilter und maximal zehn ausgewerteten Sensorereignissen je Sekunde. Es werden keine Bewegungsrichtung und keine unzuverlässigen Game-Rotation-Daten als Smartphone-Kompass ausgegeben. Die lokale magnetische Deklination wird zwischengespeichert berücksichtigt, damit die Richtung zum geografischen Kartennorden passt.

GPS-Kurs bleibt ausschließlich die Quelle für den bestehenden Fahrtrichtungsmodus der Kamera. Smartphone-Drehung verändert weder Kamerarichtung noch GPS-/FIT-Daten. Bei fehlendem oder unzuverlässigem Sensor, ungültigen/veralteten Messwerten oder nicht bestimmbarer horizontaler Richtung fehlt nur der kleine Pfeil. Der Positionspunkt bleibt erhalten. Der Sensor wird nur für die sichtbare Karte mit verfügbarer Position abonniert und bei Verdeckung/Pause wieder abgemeldet. Keine neue Nutzereinstellung oder Bibliothek.

Kartenframes rufen weder MainActivity.render noch Dashboard-/Graphverarbeitung auf. Die Trackliste wird weiterhin nur bei geänderter Revision kopiert; Kartenkacheln verwenden den bestehenden Cache. Keine häufigeren GPS-Abfragen, Änderungen an Aufzeichnung oder Performance Engine.

## Gezielte Prüfungen

MapMotionTest: kontinuierliches Fortschreiten bei 1-Hz-Daten, unregelmäßiger Empfang, begrenzte Verzögerung, Ende am letzten echten Punkt, Ausfälle, große Sprünge, Datumsgrenze, kürzester Kurswinkel und unveränderte Fahrtwerte.

MapPanelTest: tatsächliche Mapsforge-Kamera und unabhängiger Positionspunkt in manueller Ansicht, Pause/Resume, verdeckte Karte, Stillstand, manuelle Ausrichtungsaktion und Kompass-Lebenszyklus ohne Einfluss auf GPS-Kamerarichtung.

PhoneHeadingTest: Winkelglättung über Norden, alte/unzuverlässige/fehlende Messwerte, Richtung im Stand sowie Sensorregistrierung und Abmeldung. RouteTrackLayerTest prüft die tatsächlich gezeichneten Pfeilrichtungen und den Positionspunkt ohne Kompassdaten.

Subjektive Flüssigkeit, Kompassgenauigkeit und Energieverbrauch sind anschließend auf dem Zieltelefon zu prüfen. APK-Build nach Nutzerfreigabe über GitHub Actions, kein lokaler APK-Build.

## Optionaler Vorausschau-Ovalmodus

Ein eigener Button in der rechten Karten-Controlgruppe schaltet den zusätzlichen Modus ein/aus. Das Icon zeigt ein Oval; im ausgeschalteten Zustand ist es durchgestrichen. Der Modus startet ausgeschaltet. Die Einstellung mapOvalFollow bleibt erhalten und wird im Kartenbackup sowie im vorbereiteten Bike-Konfigurationsumfang berücksichtigt.

Die Karte bleibt nach Norden ausgerichtet. Der GPS-Anker wandert auf einer Ellipse im mittleren Kartendrittel entgegen der GPS-Fahrtrichtung: Norden → unten, Süden → oben, Osten → links, Westen → rechts. Die Ellipse hat Halbachsen Kartenbreite/6 und Kartenhöhe/6. Für diagonale Richtungen wird der Schnitt des entgegengesetzten Kursstrahls mit der Ellipse verwendet; dadurch zeigt der Kurs geometrisch in die Kartenmitte, auch auf hohen/schmalen Karten.

Der Ankerwinkel verwendet die bestehende angezeigte GPS-Richtung, einen 4°-Totbereich, sanftes Nachführen und höchstens 60° Winkeländerung pro Sekunde. Im Stand, bei unbrauchbarem Kurs oder manuell ausgesetztem Folgen wird er gehalten. Der Smartphone-Kompass bleibt davon unabhängig. Keine Änderungen an Positionsmessung, FIT oder Statistik.

Zentrieren reaktiviert den gewählten Folgemodus. Ausschalten des Ovalmodus erhält den vorherigen normalen North-up-/Heading-up-Modus; der explizite Ausrichtungsbutton kann Oval verlassen und in Fahrtrichtungsansicht wechseln. Bei manueller Karte bleibt der Ausschnitt erhalten. Enge Kartenflächen verkleinern die rechte Button-Gruppe bei Bedarf innerhalb der zentralen kompakten Maße, ohne linke Buttons oder Zoom zu überdecken.

Gezielte Tests prüfen Kardinal-/Diagonalgeometrie, Ellipsengrenzen, Winkelgeschwindigkeit, Norddurchgang, Rauschen, Stillstand, echte Mapsforge-Projektion, manuellen Pan, bisherigen Richtungsmodus, Layoutkollisionen und Backup-Kompatibilität.
