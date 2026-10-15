# Höhenmeterberechnung: bestätigte Hoch- und Tiefpunkte

## Korrektur

RouteElevation ist die gemeinsame, speicherkonstante Zählung für Live-GPS und
streckenbasierte Neuberechnungen/Fallbacks. Bisher wurde der Zählanker nach jeder
Änderung um mindestens 3 m versetzt. Reststücke an jeder Kuppe und Senke konnten
so wiederholt verloren gehen. Live stieg diese Schwelle zusätzlich bis auf 6 m,
abhängig von der geschätzten vertikalen GPS-Genauigkeit.

Jetzt bestätigen 3 m eine erste Bewegungsrichtung beziehungsweise deren Umkehr.
Nach Bestätigung werden neue Hoch-/Tiefpunkte vollständig mitgezählt, auch wenn
der einzelne Fortschritt kleiner als 3 m ist. Kleine Gegenbewegungen werden
nicht doppelt gezählt. Es gibt keine Multiplikation, keinen Zielsummen-Faktor
und keine auf eine konkrete Fahrt abgestimmte Schwellenänderung.

GPS-Qualitätsprüfung, Median über fünf Fixes und zeitliche Höhenglättung bleiben
erhalten. Im Stillstand und außerhalb der Aufnahme werden die Summen nicht
verändert. Datenlücken, unbrauchbare Höhen und Höhenbezugwechsel starten einen
neuen Abschnitt, ohne den Sprung dazwischen zu zählen. Reset entfernt sämtliche
Summen. Anzeige, GPS_ALTITUDE, Gradient und Distanz werden nicht neu skaliert.
Aufstieg und Abstieg verwenden denselben Algorithmus.

## Vergleich und Grenzen

Der Vergleich der vom Nutzer bereitgestellten BICYCLE-/Flow-Dateien bleibt lokal;
keine Originaldatei, Koordinaten oder Trackbilder werden veröffentlicht.
Mit der tatsächlich kompilierten RouteElevation-Klasse ergeben die bereits
aufgezeichneten BICYCLE-Höhen 435,2 m Aufstieg und 437,6 m Abstieg; die bisherige
3-m-Ankerzählung ergab 389,0/393,6 m. In der Datei gespeichert sind 386/386 m
(Livezählung mit zusätzlichen Bewegungs-/Qualitätsbedingungen).

Die Referenzen Strava/GPX-Studio wurden zur Plausibilitätsprüfung verwendet,
nicht als Kalibrierungsziel. Dieselbe neue Zählung ergibt auf den ungeänderten
Flow-Höhen 515,6/520,6 m: Die Höhenquelle und vorherige Glättung bleiben relevant.
Ein Ergebnis von 435,2 m bei Auswertung gespeicherter BICYCLE-Höhen ist keine
Garantie für einen identischen Wert einer erneuten Live-Aufzeichnung.

Vorhandene FIT-Dateien werden durch diese Berechnungsänderung nicht automatisch
überschrieben. FIT-Sessionwerte bleiben beim normalen Einlesen maßgeblich.
Eine optionale nachträgliche Höhenkorrektur mit DEM und konsistenter Änderung der
FIT ist der separate nächste Schritt; Original als Sicherung behalten.

## Mapterhorn als vorgesehene DEM-Quelle

Offizielle Dokumentation:
https://mapterhorn.com/data-access/
https://github.com/mapterhorn/mapterhorn/blob/main/website/data-access/index.html
https://github.com/mapterhorn/mapterhorn/blob/main/distribution/README.md

Einzelne Höhenkacheln können über
https://tiles.mapterhorn.com/{z}/{x}/{y}.webp
abgerufen werden. 512-Pixel-WebP mit Terrarium-Encoding
(Höhe = R * 256 + G + B / 256 - 32768 Meter). Keine Abhängigkeit vom
Mapsforge-Kartenrenderer notwendig. Nur die benötigten Kacheln entlang der Fahrt
laden und wiederverwenden; kein kompletter Offlinekarten-Download erforderlich.
Die gesamte FIT-Datei muss dafür nicht an einen Dienst übertragen werden;
Kachelanfragen verraten dem Anbieter allerdings die angefragten Gebiete.
PMTiles/Range-Zugriff wäre ebenfalls möglich, ist für den ersten Ausbau aber
nicht nötig. Online-Kachelcache ermöglicht später Offline-Wiederverwendung.

Der offizielle Quellenkatalog enthält für Bayern das amtliche DGM1 (1 m,
Bayerische Vermessungsverwaltung, CC BY 4.0):
https://github.com/mapterhorn/mapterhorn/blob/main/source-catalog/debayern/metadata.json

Die Quelldatenauflösung ist nicht automatisch die effektiv abgerufene
Kachelauflösung. Geeignete Zoomstufe/TileJSON, Interpolation, NoData, Rasterränder,
Attribution und Lizenz je verwendeter Region berücksichtigen. Bodenmodellwerte
können auf Brücken oder in Tunneln von der Fahrhöhe abweichen. Gleiche Quelle wie
GPX-Studio garantiert ohne gleiche Verarbeitung keine identische Höhenmetersumme.
Mapterhorn-Code BSD-3; Höhendaten haben unterschiedliche offene Quelllizenzen,
siehe https://mapterhorn.com/attribution . Keine API-Key-Angabe am dokumentierten
öffentlichen Z/X/Y-Endpunkt. Dienstverfügbarkeit/Nutzungsbedingungen vor einer
produktiven Integration prüfen; direkter Anbieterzugriff ist in dieser
Entwicklungsumgebung derzeit per Proxy-Domainfreigabe blockiert.

## Prüfung

39 gezielte Tests aus RouteElevationTest, GpsRideTest, GpxManagerTest,
GpxLibraryTest, FitLibraryTest, FitWriterTest und RouteAnalysisTest bestanden.
Neue Regressionen: wiederholte kleine Hügel, ganze bestätigte Rampen,
Gegenschwankungen, exakte 3-m-Grenze, konstantes Niveau mit Rauschen in Fahrt,
Datenlücken, ungültige Werte und Reset. Bestehende Tests prüfen zusätzlich
Aufnahmegrenzen, Stillstand, Höhendatum und Einzelspitzen. Release-Kotlin-
Kompilierung erfolgreich. Kein vollständiger Suite-/APK-Build.
