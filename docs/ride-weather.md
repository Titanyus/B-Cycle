# Wetter in den Streckendetails

Unter Datum/Uhrzeit steht eine kompakte Wetterzeile: Wetterzustand mit Icon und
Temperatur (°C), Luftfeuchtigkeit (%) sowie Windgeschwindigkeit (km/h) und
einen Windrichtungspfeil. Die gespeicherten Wetterdaten enthalten weiterhin die
meteorologische Herkunftsrichtung. Der Pfeil zeigt die Strömungsrichtung (wohin
der Wind weht): Südwestwind ergibt einen Pfeil nach Nordost. Texte und Beschreibungen
liegen in Android-Ressourcen; Icons und Texte folgen FOREGROUND.

## Zeitpunkt und Position

Darstellungsmetadaten werden beim bestehenden Hintergrund-Einlesen gesammelt.
Der Zielzeitpunkt ist Start plus halbe verstrichene Fahrtdauer, einschließlich
Pausen. Aus einem begrenzten Puffer wird die zeitlich nächste GPS-Position gewählt;
bei mehr als fünf Minuten Abstand fehlen verlässliche Positionsdaten und die
Zeile entfällt. Keine Entfernungshalbierung und kein bloßes mittleres Arrayelement.
Fehlendes Datum, fehlende Dauer oder keine GPS-Daten erzeugen keine Wetterwerte.

## Quelle und Genauigkeit

Open-Meteo, stündliche Daten für den betreffenden UTC-Tag. Innerhalb der letzten
fünf Tage Forecast-Endpunkt mit vergangenen Stunden, sonst Archive-Endpunkt.
Die zum Tourmittelpunkt nächste verfügbare Stunde wird genutzt, maximal eine
Stunde entfernt. Temperatur, relative Feuchtigkeit, WMO-Wettercode sowie Wind in
10 m Höhe. Das sind Modell-/historische Wetterdaten, keine Messung am Fahrrad.
Gesendet werden ein auf drei Nachkommastellen gerundeter Ort und der angefragte
Tag, niemals die FIT-Datei oder der ganze Track. HTTPS, feste Anbieteradressen,
begrenzte Antwortgröße/Timeouts; keine API-Schlüssel in der App erforderlich.

Die dokumentierten Endpunkte sind api.open-meteo.com/v1/forecast und
archive-api.open-meteo.com/v1/archive. Anbieterzugriff/-dokumentation sind in
dieser Entwicklungsumgebung durch die Domain-Netzwerkregel blockiert. Deshalb
kein echter Onlineabruf als bestanden ausweisen. Vertrag mit lokalen HTTP-
Testservern geprüft; Nutzungslimits/-bedingungen vor kommerzieller Veröffentlichung
aktuell prüfen. Kostenloser Standardzugang ist für nichtkommerzielle Nutzung.

## Dauerhafte Speicherung

Erfolgreich geladene Werte werden als kleine JSON-Metadatei pro Fahrt unter
filesDir/ride-weather-v1 atomar gespeichert. Enthalten sind Zeitpunkt, Position,
Abrufzeit, Quelle und Wetterwerte. Kein Cache-Verzeichnis, Ablaufdatum oder
Verdrängen älterer Fahrten. Nach erneutem Öffnen und normalen App-Updates bleiben
die Werte erhalten, auch ohne Netz oder bei später fehlender Anbieterhistorie.
Bei verändertem Bezugszeitpunkt/-ort werden alte Werte nicht als passend ausgegeben.
Ein neuer erfolgreicher Abruf kann dann die Metadaten ersetzen. Ohne Daten wird
nichts erfunden; fehlende Einzelwerte bleiben fehlend, echte Nullen bleiben gültig.

Löschen einer Fahrt entfernt ihre Wettermetadaten über denselben seriellen
Wetter-Worker, auch wenn ein vorheriger Abruf noch läuft. Die FIT-Datei und alle
Sensor-/Performance-Berechnungen bleiben unverändert. Wettermetadaten werden
aktuell nicht mit FIT exportiert oder über die reine Einstellungssicherung
übertragen. Deinstallation bzw. Löschen der App-Daten entfernt sie ebenso wie die
interne Streckensammlung.

Die Ansicht lädt ohne Blockieren des übrigen Screens. Veraltete Callbacks nach
Schließen/Neuladen werden ignoriert. Quellen und Lizenzen stehen zentral unter
Erweitert → Quellen & Dienste; kein zusätzlicher Text an der Wetterzeile.

## Windpfeil nach Build 72

Lucide `mouse-pointer-2` aus bestehender Version 0.468.0 statt sichtbarer
Himmelsrichtungs-Abkürzung. Pfeil in Textgröße (rowTitleSp), FOREGROUND und direkt
hinter der Windgeschwindigkeit. Grundausrichtung des Icons ist Nordwest/315°.
Rotation = (meteorologische Richtung + 180° − 315° + 360°) modulo 360°, also
(Richtung + 225°) modulo 360°. Beispiele: Nordwind → Süden, Westwind → Osten,
Südwestwind → Nordosten. Screenreader behalten die ausdrücklich als Herkunft
benannte Richtung. Fehlende Richtung zeigt keinen erfundenen Pfeil. Wetterdaten,
Abruf, Speicherung, FIT und Berechnungen bleiben unverändert.

Gezielte Prüfung: kompakte Hochformatzeile, Textgröße/Abstände, sichtbarer Ersatz
der Abkürzung, fehlende Richtung sowie Rotationen für Nord/Ost/Süd/Südwest/Nordwest
und 360° erfolgreich. Screenreader-Herkunft erhalten. Keine APK erstellt.

Stand 23.10.2026: die beschriebenen neuen Änderungen sind in APK **0.8.34 / Build 73** enthalten. [Buildnachweis](build-73.md). Frühere Angaben zu fehlender APK sind Verlauf.
