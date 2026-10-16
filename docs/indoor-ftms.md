# Indoor-Bike über BLE/FTMS

Ab 0.7.25 ist Indoor-Bike aktiv. Normales Bike bleibt vorbereitet und deaktiviert.
Die Profile, Fotos und Reihenfolge verwenden weiterhin die dynamische Verwaltung
mit maximal sechs Rädern. E-Bike und Indoor haben getrennte gespeicherte Geräte.

## Einrichtung und Umfang

1. Unter Bikes ein Rad anlegen/auswählen und Indoor-Bike aktivieren.
2. Den Matrix von anderen Trainer-Apps trennen und einschalten.
3. Sensor suchen / verbinden: den FTMS-Trainer bewusst auswählen, im Referenztest
   heißt er JFICcycle. Optional einen BLE-Pulsmesser auswählen.
4. Gewünschte Felder unter FIT-Aufzeichnung auswählen; Dashboard im vorhandenen
   Editor konfigurieren. Die Datenquelle heißt Indoor-Bike.
5. START/STOPP steuern ausschließlich die BICYCLE-Aufnahme.

Nur TRAINER und HEART werden als Sensorrollen angeboten. GPS bleibt aus; es gibt
keine Standortabfrage, GPS-Positionen, Bosch-Daten oder separate Powermeter-Rolle.
Kein Android-Pairing erforderlich. Der Widerstand des Matrix wird weiterhin
manuell am Hebel eingestellt. Die App sendet keine Trainings-, Widerstands-,
Leistungs-, Start-/Stop- oder Kalibrierbefehle an den Trainer.

## Datenpfad

TrainerMeasurement dekodiert FTMS Indoor Bike Data 2AD2 anhand der Little-Endian-
Flags. Weitere bekannte Felder werden korrekt übersprungen, unvollständige Frames
oder unbekannte Flags verworfen. Echte Nullen bleiben gültig, reservierte ungültige
Werte werden fehlend. Unterschiedliche Feldkombinationen behalten ältere Felder
nur innerhalb ihrer jeweiligen 5-Sekunden-Frischegrenze.

TrainerConnection abonniert ausschließlich 2AD2: lokale Freigabe und ein
CCCD-Schreibzugriff. Ein optionaler Battery-Service-Read folgt erst nach dessen
Bestätigung. Alle Callbacks werden auf dem Main-Thread geprüft; alte GATT-
Generationen nach Stop/Profilwechsel werden ignoriert. Verbindung, freigegebenes
Datenabo und tatsächlicher Datenempfang sind getrennte Zustände. Nach mehr als
fünf Sekunden ohne Messwerte zeigt der Status Daten veraltet; einzelne Felder
werden unabhängig davon nicht durch andere Messwertarten frisch gehalten.
Die Statuszeile zählt unterschiedliche tatsächlich empfangene Messwertarten.

AutoReconnect wird wie bei den anderen Sensoren verwendet. Disconnect behält die
Zuordnung und verhindert automatisches Wiederverbinden bis zur bewussten Aktion.
Zuordnung entfernen löscht nur den Trainer des gewählten Profils. Bei Aufnahme
bleibt die Verbindung im Hintergrund aktiv, sonst wird sie bei Inaktivität beendet.

Tempo, Kadenz und Fahrerleistung werden auf die bestehenden gemeinsamen
LiveData-/Dashboard-/Record-IDs und Einheiten normalisiert. Keine MCSP-Nachrichten
werden simuliert. Der vom Trainer gesendete Distanzzähler dient als Anker für die
bestehende BikeRide-Fahrtstrecke: vor START keine App-Fahrtstrecke, bei START null,
danach Geräte-Differenzen; Neustarts/Rücksprünge erzeugen keine negative Strecke.
Fehlt die Distanz, wird keine Strecke erfunden. Die Geräte-Trainingszeit setzt die
BICYCLE-Aufnahmezeit nicht vor und ersetzt sie nicht.

IndoorRide verwendet MovingTime und RideAnalysisMeans für bestehende Bewegungs-
und Mittelwertregeln. Aktualisierung nur im vorhandenen Aufzeichnungstick.
Kadenzmittel ohne Tretpausen über den bestehenden PedalingAverage; Roh-Kadenz null
bleibt in FIT enthalten. Kein zweiter Statistik- oder Aufzeichnungsdienst.
Ein eingebauter Trainerpuls wird nur verwendet, wenn kein aktivierter separater
Pulsmesser zugeordnet ist. Zugeordnete externe Pulsmesser behalten die Priorität;
kein stiller Quellenwechsel bei deren Datenlücke.

Historische GPS_-IDs der allgemeinen Fahrtzeit-/Bewegungs-/Spitzenwerte bleiben
als Kompatibilitätskennung erhalten. Im Indoor-Profil sind dies App-Fahrtwerte,
keine GPS-Messungen; Auswahl, Beschriftung und Quellenicon zeigen Indoor-Bike.
GPS-Geschwindigkeit, GPS-Distanz, Höhe und Wetter werden dort nicht angeboten.

## Performance und FIT

FTMS ist eine eigene PowerSource. Derselbe PerformanceEngine akzeptiert die zum
Profil gehörende Hauptquelle; weitere Vergleichsquellen überschreiben sie nicht.
Die Bosch-spezifische Verlängerung eines zuletzt expliziten Nullzustands wird für
FTMS nicht angewendet: fehlende Leistung bleibt nach der Frischegrenze fehlend.

RideJournal V6 speichert das Indoor-Merkmal im Header. V4 und V5 bleiben lesbar,
einschließlich vorhandener ungespeicherter Fahrten. Das Merkmal wird auch bei einer
späteren Wiederherstellung verwendet, unabhängig vom dann aktiven Bike-Profil.
Garmin-FIT verwendet die vorhandenen Standardfelder für speed, distance, cadence,
power und heart_rate; Sport Cycling, Sub-Sport Indoor Cycling in Session und Lap.
Keine künstlichen Koordinaten oder Motorleistung. Bestehende optionale
Performance-Sessionfelder bleiben unabhängig auswählbar. Die zentrale
Höhenkorrektur wird für Indoor-Aufzeichnungen übersprungen.

## Prüfung

Automatisierte Tests decken Parserflags/Skalen, Nullen, ungültige Werte, Lücken,
alternierende Frames, CCCD-/Read-Reihenfolge, kein Steuer-Schreiben, Frische,
Callback-Generationen, Suche, Wiederverbindung, Profilwechsel, Dashboard-/FIT-
Auswahl, Hintergrundaufnahme, Fahrtzählergrenzen und persistente Trainerzuordnung ab.
Eine 20-Minuten-FIT mit einer echten Nullleistungsphase wird mit Garmins Decoder
und ActivityFileValidationPlugin geprüft und wieder in die Detailanalyse eingelesen;
NP, Arbeit und Estimated FTP werden geprüft. Alte V5-Journale bleiben lesbar.

Die physische BLE-Verbindung mit dem Matrix, Messwertgenauigkeit, längere Ausfälle
und Langzeitstabilität müssen mit der Test-APK am Gerät geprüft werden. Grundlage:
[erfolgreicher ANT+ Explorer-/Matrix-Referenztest](ble-trainer-matrix-icr50.md).
