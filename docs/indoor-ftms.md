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
Fehlt ein frischer Distanzzähler, wird seit dem Nachtrag26.10.2026 die beobachtete Trainergeschwindigkeit zeitlich integriert; Details unten. Die Geräte-Trainingszeit setzt die
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
Eine standardisierte Sport-Nachricht vor den Records erlaubt auch beim späteren
Detail-Einlesen die Indoor-Zuordnung ohne erneutes Dekodieren. So wird die
Bosch-Nullfortschreibung auch dort nicht auf FTMS-Datenlücken angewendet.
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

Die physische BLE-Verbindung mit dem Matrix wurde am 16.10.2026 vom Nutzer als
funktionierend bestätigt. Messwertgenauigkeit, längere Ausfälle und Langzeitstabilität
bleiben gesondert zu prüfen. Grundlage:
[erfolgreicher ANT+ Explorer-/Matrix-Referenztest](ble-trainer-matrix-icr50.md).

## Messwertkorrektur je Indoor-Profil

Im Bikes-Screen zwischen Sensoren und FIT-Aufzeichnung erscheint nur bei Indoor-Bikes
„Messwertkorrektur“. Zwei native Slider: **Leistung** und **Geschwindigkeit & Distanz**,
jeweils Faktor 0,0–3,0 in 0,1er-Schritten, Standard 1,0. Gültige Änderungen werden
sofort im ausgewählten Bike gespeichert. Ältere Profile ohne diese Angaben starten
mit 1,0; ungültige gespeicherte Faktoren fallen einzeln auf 1,0 zurück.

`IndoorCorrection` normalisiert die ursprünglichen FTMS-Werte in RideRuntime vor den
Fahrtverbrauchern. Leistung auf ganze Watt, Geschwindigkeit auf die vorhandenen
Hundertstel km/h und Distanzzähler auf ganze Meter runden. Danach verwenden Dashboard,
Graphen, Training/Zonen, Fahrtmittelwerte, Performance-Ereignisse und Journal/FIT dieselben
korrigierten Werte. Kein zweites Skalieren beim Export, Wiederherstellen oder Detail-Einlesen.
Der originale FTMS-Empfangspuffer bleibt für Frische/Zeitstempel unverändert. Fehlende
Werte bleiben fehlend, echte Nullen bleiben Null; Kadenz, Puls, Akkustand und Gerätezeit
werden nicht korrigiert. Geschwindigkeit und Distanz teilen den Faktor; die bestehende
START-Verankerung des Distanzzählers bleibt erhalten.

Während Aufnahme oder Speicherung sind Slider und Runtime-Setter gesperrt. Somit
gilt innerhalb einer Fahrt genau ein Faktorenpaar. Außerhalb der Aufnahme wirken
Änderungen sofort auf frische Livewerte, ohne deren Empfangszeit künstlich zu erneuern.
Fertige Fahrtzähler bleiben eingefroren; neue Fahrten beginnen mit den aktuellen Faktoren.
Vorhandene FIT-Dateien werden nicht geändert. Faktoren sind Teil des lokalen Bikeprofils;
das bisherige manuelle Backup-Schema bleibt unverändert.

Leistung und Geschwindigkeit sind nicht durch einen universellen Faktor miteinander
verknüpft. Vorgemerkt, noch keine zusätzliche Kalibrier-UI: Leistungsfaktor aus gleichzeitig
ermittelter Referenzleistung / Trainerleistung, nur bei gültigen Werten und positivem
Nenner. Mehrere stabile Belastungsstufen vergleichen; ein fester Faktor kann nicht jede
Nichtlinearität eines Trainers korrigieren. Keine Widerstandssteuerung daraus ableiten.

Am 16.10.2026 bestätigt der Nutzer die funktionierende Indoor-Anbindung am echten
Trainer. Die Genauigkeit der Matrix-Messwerte ist nach seinem Vergleich gering; dafür
ist die optionale Korrektur vorgesehen. Langzeit-/Ausfallprüfungen bleiben separat.

Automatisierte Absicherung der Korrektur: 22 gezielte Tests aus IndoorCorrectionTest,
IndoorRuntimeTest, IndoorUiTest, IndoorFitTest, BikeProfilesTest, BikeProfileRuntimeTest
und FtmsMeasurementsTest bestanden. Einschließlich pro-Profil-Persistenz/Migration,
Faktorschritten/Nullen/fehlenden Werten, UI-Auswahl, korrigierter Live-Anzeige und
Performance, Distanz, FIT-Roundtrip, Aufnahmesperre und unveränderten alten FIT-Bytes.
Release-Build einschließlich R8, Resource-Shrinking und Lint Vital erfolgreich.
Diese Korrektur ist seit APK 0.7.27 enthalten.


## Rollentrainer und Messwertdiagnose, 26.10.2026

Die vorhandene Indoor-Typwahl bietet jetzt Hometrainer und Rollentrainer. `BikeProfile.indoorType` speichert die Auswahl pro Profil, einschließlich Profilkopie und Einstellungssicherung. Alte Profile/Backups ohne Feld verwenden Hometrainer. Bei Aufnahme/Speichern/HRV ist die Änderung gesperrt. Der Radtyp und FIT-Untertyp bleiben INDOOR/INDOOR_CYCLING; es entstehen keine neuen FIT- oder Journalformate.

Ein Rollentrainer-Profil deckt sowohl ein eingespanntes Fahrrad als auch einen Zwift-Ride-Rahmen ab: dieselbe Trainerverbindung und Aufzeichnung. Virtuelle Schaltung/Click sind separat für später geplant, siehe TODO. Es gibt weder eine neue Controllerverbindung noch neue Widerstands-/ERG-Befehle. KICKR CORE mit FTMS-fähiger Firmware ist die Zielhardware; CORE2 und tatsächlich angebotene Dienste/Felder müssen am Gerät geprüft werden. Ohne Gerätetest keine zugesicherte Kompatibilität.

`TrainerDistance` bevorzugt einen frischen Zähler. Fehlt dieser, werden aufeinanderfolgende tatsächliche Geschwindigkeitsmessungen mit Trapezregel integriert (km/h → m/s, maximal5s Abstand). Wiederholte Verarbeitung desselben Zeitstempels, fehlende/ungültige/stale Geschwindigkeit und Verbindungsabbruch addieren keine Strecke. Quellenwechsel setzen einen neuen Anker, ohne fremde Zählerstände aufzuschlagen; ein Zählerrücksprung addiert nichts Negatives. Start setzt die Fahrtstrecke zurück und verhindert das Einbeziehen der Zeit vor START. Der bestehende Geschwindigkeitskorrekturfaktor gilt einmalig auch für die daraus berechnete Distanz. Dashboard und aufgezeichnete Hauptdistanz verwenden dieselbe Fahrtstrecke; kein synthetischer Roh-Distanzzähler in den Sensordaten.

Die Statuszeile mit Messwertanzahl ist über ein kleines Chevron aufklappbar und listet die tatsächlich empfangenen Arten samt Einheiten. Bosch: bisherige dekodierte Messfelder/Reichweiten; Pulsgurt: Puls, RR, Energie, Akku; FTMS: tatsächlich empfangene Trainerfelder und optional Akku. Abgeleitete Strecke oder nicht empfangene optionale Felder erscheinen dort nicht. Historie gilt nur für die aktuelle Verbindung; Trennung schließt/leert die Liste. Status-/Sensorverbindung und GATT-Schreibverhalten unverändert.

Prüfung26.10.2026:18unterschiedliche gezielte Tests erfolgreich zu nativer Trainerdistanz und neuem Fallback, einmaliger Korrektur, Datenlücken/Quellenwechsel/Nullen/Zählerrücksprung/duplizierten und älteren Zeitstempeln, echter synthetischer FIT-Ausgabe, Profilwahl/Migration/Sicherung, aufklappbarer Messwertliste und erhaltenem Pulsgurt-/Zuordnungsdialogverhalten. Typografie-/Atlas-/Diffprüfung erfolgreich. Kein Gerätetest oder neue APK; CORE/CORE2 bleiben hardwareseitig zu prüfen.
