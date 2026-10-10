# Lokale FIT-Aufzeichnung ab 0.4.0

START startet eine Fahrt, STOPP beendet sie und speichert in `Download/B-Cycle/`.
Die Dashboard-Belegung und die Speicherfeld-Auswahl sind unabhängig. Die Auswahl
wird bei START eingefroren; spätere Änderungen gelten für die nächste Fahrt.
WebDAV wird noch nicht unterstützt. Es gibt keinen automatischen Upload.

## Offizielle Spezifikation

Der schlanke, eigenständige Encoder schreibt Garmin FIT 2.0 mit Header-, Datei-CRC
und Profil 21.218. Die Referenz sind das
[offizielle Garmin FIT Java SDK](https://github.com/garmin/fit-java-sdk),
Profil 21.218.0 (Referenz-Commit `dd12be0bf9757df80e2308f4b8c808553d09b42c`),
die [FIT-Protokollspezifikation](https://developer.garmin.com/fit/fit-protocol/)
und die [Activity-Datei-Anleitung](https://developer.garmin.com/fit/cookbook/encoding-activity-files/).
Garmins ActivityFileValidationPlugin wird nur in Tests verwendet. Im aktuellen Code
nach APK 0.6.6 wird der offizielle Decoder auch auf dem Datei-Worker für Metadaten
und Kartenanzeige gespeicherter FIT-Dateien verwendet. Die Tests prüfen auch Skalierungen und Einheiten,
Quellwahl, fehlende Werte und eine abgebrochene letzte Journalzeile.

Enthalten sind `file_id`, `device_info`, Timer-Start/Stop-Events, Sekunden-Records,
eine Lap, Session und Activity sowie die Beschreibungen ausgewählter Developer-
Felder. Sport ist Cycling / E-Bike Fitness. B-Cycle wird als entwickelndes Gerät
identifiziert, nicht als Garmin-Gerät. Zeitstempel sind UTC mit FIT-Epoche;
Fahrtdauer basiert auf monotoner Android-Zeit. Keine automatische Timer-Pause:
Die FIT-Timerzeit umfasst START bis STOPP, einschließlich Standzeiten.

## Daten und Zuordnung

| Messung | FIT-Feld |
| --- | --- |
| GPS-Route | `position_lat`, `position_long`, Semicircles |
| Gewählte Hauptgeschwindigkeit | `speed`, `enhanced_speed`, m/s |
| Gewählte Hauptdistanz | `distance`, m |
| Eigene Fahrerleistung | `power`, W |
| Motorleistung | `motor_power`, W |
| Kadenz | `cadence`, `fractional_cadence`, rpm |
| Puls | `heart_rate`, bpm |
| E-Bike-Akkustand | `battery_soc`, % |
| GPS-Steigung | `grade`, % |
| GPS-Meereshöhe (MSL) | `altitude`, `enhanced_altitude`, m |
| Fahrtsummen | Dauer, Strecke, Durchschnitt/Maximum, Auf-/Abstieg in Lap/Session |

Zusätzlich erhalten alle ausgewählten numerischen Quellwerte benannte FIT-
Developer-Felder. Dazu gehören beide Geschwindigkeiten/Distanzen, Kilometerstand,
Unterstützungsmodus als Index, Drehmomente, Akkuenergie und Akkutemperatur,
aktuelle Reichweite, Bosch-Aktivitätswerte, GPS-Bewegungs-/Standzeit,
GPS-Durchschnitte/Höhenwerte sowie Puls-Durchschnitt/Maximum und Sensorakku.
Eine variable Reichweitenliste und die Modusnamen werden nicht als Textliste
aufgezeichnet; die aktuelle Reichweite ist auswählbar. RR-Intervalle und
HRV-Analyse werden in dieser Version nicht exportiert.

Die FIT-Hauptquellen für Geschwindigkeit und Distanz sind getrennt wählbar.
Die jeweils andere Quelle bleibt nur erhalten, wenn ihr Speicherfeld angehakt
ist. Es gibt keinen stillen Quellenwechsel bei Empfangsausfällen. Unbekannte
oder veraltete momentane Werte bleiben ungültig statt 0; echte Nullwerte bleiben
0. Die GPS-Filter sind unverändert. Bosch-Momentanwerte verfallen für die
Aufzeichnung nach 5 s, Akku/Modus/Kilometerstand nach 60 s, Bosch-Aktivität nach
120 s; die Verbindung muss live sein. Puls verfällt nach 5 s ohne neue Messung.

Android liefert nicht auf jedem Handy eine Meereshöhe. Ellipsoidhöhe wird als
zusätzliches `GPS_ALTITUDE`-Feld samt `GPS_ALTITUDE_MSL`-Kennzeichen gespeichert;
sie wird nicht fälschlich als Meereshöhe ins native FIT-Höhenfeld geschrieben.
Andere Programme zeigen häufig nur native FIT-Felder an: Die Zusatzfelder sind
standardskonform gespeichert, ihre Anzeige hängt vom jeweiligen FIT-Import ab.

## Hintergrund, Speicherung und Fehler

Activity und RecordingService teilen sich GPS und die vorhandenen BLE-Clients.
Nur eine ausdrücklich gestartete Fahrt hält einen Foreground-Service mit
Benachrichtigung und partiellem Wake-Lock aktiv. Der Dienst bleibt beim STOPP
bis zum Abschluss des Speicherns aktiv. Standort-/BLE-Berechtigungen und
Standort/Bluetooth am Handy müssen aktiviert sein. Androids erzwungenes Beenden,
Neustart oder herstellerspezifische Akkuverwaltung können trotzdem unterbrechen;
ein Android-13/15-Test mit simulierten Sensoren ersetzt keinen Poco-Hardwaretest.

Ein Worker schreibt gewählte Messwerte ungefähr einmal pro Sekunde in ein
privates Journal. Die Arbeitswarteschlange ist begrenzt; Synchronisation erfolgt
am Anfang, alle fünf Messpunkte und am Ende. RAM wächst nicht mit Fahrtdauer.
Bei STOPP wird dieses Journal zum FIT-Stream. Im aktuellen Code bleibt eine atomar
gespeicherte originale FIT-Kopie in der appinternen Sammlung, anschließend wird
über MediaStore in Downloads veröffentlicht. Der gemeinsame Streckenmanager
registriert die private Kopie; diese ist auch nach einem Downloadfehler wieder auffindbar. Eine allgemeine Speicherberechtigung ist nicht nötig.
Private Zwischendateien werden erst nach erfolgreichem Export entfernt. Bei
Fehlern bleibt das Journal für „Ungespeicherte Fahrten speichern“ erhalten.
Nach Prozessabbruch sind vollständige gespeicherte Messpunkte exportierbar;
ein unvollständiger letzter Messpunkt wird verworfen. Die Fahrt wird nach
Prozessende nicht automatisch weitergeführt. Eine teilweise erhaltene Fahrt
endet am letzten vollständigen Messpunkt.

GPS-Route und Sensorwerte verlassen das Handy nur durch selbst gewähltes Teilen
der FIT-Datei. Die Internetberechtigung dient den Online-Karten; kein FIT-Upload und keine
Standortkoordinaten im Diagnosebericht. Speicherbedarf hängt von der Auswahl ab; standardmäßig alle
numerischen Felder bedeuten grob 1–2 MB pro Stunde einschließlich FIT-Zusatzfeldern.

Der Menüpunkt heißt im aktuellen Code **FIT-Aufzeichnung**. Die gespeicherten
Fahrten werden ausschließlich im gemeinsamen [Streckenmanager](routes.md) verwaltet.
Garmins Lizenztext ist unter Erweitert → Bibliotheken & Lizenzen und in
`app/src/main/assets/Garmin-FIT-LICENSE.txt` enthalten.
