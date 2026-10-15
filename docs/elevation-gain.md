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
Die separate optionale DEM-Korrektur verändert die aktive FIT-Version ausdrücklich;
das unveränderte Original bleibt als Sicherung erhalten (siehe unten).

## Optionale Mapterhorn-Höhenkorrektur

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
öffentlichen Z/X/Y-Endpunkt. Direkter Anbieterzugriff ist in dieser Entwicklungsumgebung per Proxy-Domainfreigabe
blockiert; ein echter Dienstabruf konnte hier nicht geprüft werden.

## Prüfung

39 gezielte Tests aus RouteElevationTest, GpsRideTest, GpxManagerTest,
GpxLibraryTest, FitLibraryTest, FitWriterTest und RouteAnalysisTest bestanden.
Neue Regressionen: wiederholte kleine Hügel, ganze bestätigte Rampen,
Gegenschwankungen, exakte 3-m-Grenze, konstantes Niveau mit Rauschen in Fahrt,
Datenlücken, ungültige Werte und Reset. Bestehende Tests prüfen zusätzlich
Aufnahmegrenzen, Stillstand, Höhendatum und Einzelspitzen. Release-Kotlin-
Kompilierung erfolgreich. Kein vollständiger Suite-/APK-Build.

## Bedienung und Speicherung

Unter Karten aktiviert „Höhenkorrektur nach Aufnahme“ die Nachbearbeitung beim
Beenden der Aufnahme. Standardmäßig aus; die Option gehört zur Einstellungssicherung.
Unterstützt werden eigene FIT-Fahrten mit GPS-Positionen. In deren Streckendetails
steht bei aktivierter Option „Höhe korrigieren“ auch für ältere Fahrten bereit.
Schließen der Detailansicht bricht die laufende manuelle Korrektur ab.

Die Korrektur läuft im bestehenden Hintergrund-Worker vor lokalem Speichern und
Cloud-Upload. Scheitert sie, wird die vorhandene Datei weiterverwendet und die
Aufnahme bleibt erhalten; ein Hinweis verweist auf den erneuten manuellen Versuch.
Keine Sensor-, Aufzeichnungs- oder Performance-Berechnung wird dadurch verändert.

Die Originaldatei in fit-library bleibt unverändert. Erst nach erfolgreicher
FIT-Integritätsprüfung ersetzt eine atomare Umbenennung die korrigierte aktive
Version in fit-library/corrected. Streckenliste, Analyse, Speichern und Upload
verwenden diese Version. Ihre Änderung entwertet frühere Speicher-/Upload-Häkchen;
ältere Cloud-Dateien werden nicht automatisch erneut hochgeladen. Löschen einer
Fahrt entfernt Original und korrigierte Version. Eine erneute Korrektur beginnt
immer beim Original. Zugehörige GPS-Höhen-Developer-Felder, vorhandener Gradient,
Lap-/Session-Höhen und Auf-/Abstieg werden konsistent aktualisiert. Alle anderen
Nachrichten, Metadaten und Messwerte bleiben erhalten. Nicht auswertbare Positionen
werden nicht mit Nullhöhen oder alten GPS-Höhen vermischt. Datenlücken und
Aufnahmepausen erzeugen keinen künstlichen Höhengewinn.

## Abruf und Grenzen

Bilineare Abtastung aus 512-Pixel-Terrarium-WebP auf Zoom 14; bei fehlender Kachel
Rückfall auf Zoom 12. Maximal acht dekodierte Kacheln im Arbeitsspeicher, 64 MiB
wegwerfbarer Plattencache mit 30 Tagen Gültigkeit. Pro Korrektur maximal 256
Kachelanfragen und drei Minuten; Downloads erfolgen nacheinander mit begrenzten
Timeouts und maximal 2 MiB pro Antwort. Fehlende Daten/HTTP-Fehler werden nicht
als Höhe 0 ausgegeben. Kein Vorabladen ganzer Regionen, kein API-Schlüssel.

Attribution mit Quellen-/Lizenzlink steht an korrigierten Höhendaten im
Detailscreen und als standardkonforme FIT-Memo in der Datei. Das Bodenmodell kann
Brücken, Tunnel oder neu verändertes Gelände nicht zuverlässig repräsentieren.

Zusätzlich zur oben geprüften Höhenzählung bestanden 47 gezielte Tests aus
FitElevationCorrectionTest, MapterhornElevationsTest, FitLibraryTest,
FitWriterTest, RouteAnalysisTest, RouteActionStatusTest, RouteDetailActionsTest,
SettingsBackupTest und SettingsDesignMigrationTest. Geprüft: WebP-/Terrarium-Dekodierung,
Kachelränder, Cache, 404-Fallback, Fehler/Abbruch, unverändertes Original, korrigierte
Höhen/Summen, Garmin-Validator/CRC, erhaltene Rohfelder und Developer Fields,
Wiedereinlesen, aktive Version, entwertete Erfolgsstatus und Upload der neuen Datei.
Release-Build erfolgreich; realer Anbieterzugriff bleibt wegen der Netzwerkfreigabe
ungeprüft.
