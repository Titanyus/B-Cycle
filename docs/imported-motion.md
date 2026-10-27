# Robuste Auswertung importierter Bewegungsdaten

Stand27.10.2026, nachBuild92. Nur gespeicherte Import-/Analysepfade, keine Änderung der Live-Sensoren, Aufnahme, Originaldateien oder Messwerte.

## GPX

GPX besitzt häufig keine native Geschwindigkeit. Unregelmäßige GPS-Updates und über mehrere Zeitpunkte wiederholte Koordinaten dürfen nicht abwechselnd als schneller Sprung und Stillstand ausgewertet werden. GpxMotion berechnet deshalb eine abgeleitete Geschwindigkeit aus Weg und Zeit eines maximal10Sekunden langen Fensters, begrenzt auf128 Einträge. Wiederholte Positionen behalten ihre Zeitanteile. Segmente, unbekannte/rückläufige Zeit und Lücken über10Sekunden setzen das Fenster zurück. Vorhandene gültige native GPX1.0-Geschwindigkeit in m/s hat Vorrang. Keine neuen Koordinaten oder Zeitstempel.

Dieselbe Geschwindigkeit verwenden GPX-Metadaten/Fahrtzeit, Detaildiagramme/Maxima/Durchschnitt und die vorhandene GPX→FIT-Aktivitätskopie. GpxActivityFit.VERSION erhöht, damit alte abgeleitete Uploadkopien nicht weiterverwendet werden. GPX-Original und unveränderte Leistung/Puls/Kadenz bleiben erhalten. Der native Rohintervallwert bleibt separat in GpxMeasurement.intervalSpeed für bestehende konservative Langpausenerkennung, damit die Glättung Pausen nicht verdeckt. Kein Rückschluss auf Pausen allein aus fehlenden Sensorwerten.

GPX-Geschwindigkeit und Fahrtzeit bleiben Schätzungen. Verlorene Zeitpräzision oder fehlende Radmessungen können damit nicht exakt rekonstruiert werden. Keine willkürliche nachträgliche Anpassung an ein gewünschtes Tempo oder eine fremde Fahrtsumme.

## FIT

FitRecordJoiner ergänzt die bereits implementierte Verbindung komplementärer Records gleichen Zeitstempels. Reine Sensorrecords zwischen langsameren GPS-Updates brechen die Kartenlinie nicht mehr; ausdrücklich ungültige Positionsfelder und echte zeitliche GPS-Ausfälle bleiben Grenzen. Reine Positions-/andere Telemetrierecords löschen keinen vorhandenen nativen Geschwindigkeitszeitpunkt. Nur tatsächlich beobachtete Geschwindigkeitsfelder aktualisieren MovingTime, Geschwindigkeitsdiagramm und Performance-Geschwindigkeitskanal. Die bestehende10Sekunden-Kontinuitätsgrenze verhindert unbegrenztes Fortschreiben.

Positionsbasierte Distanzfortschreibung in der Detailauswertung hält den letzten GPS-Punkt unabhängig vom letzten Sensorrecord; zeitlose geplante GPX-Strecken bleiben darstellbar. FIT-Höhenwerte werden auch aus eigenständigen Höhen-/Sensorrecords gelesen. Native FIT-Geschwindigkeit und Sessionwerte behalten Vorrang vor GPS-Ableitungen. Vorhandene Zeitberechnung verwendet explizite Dauern/Messrecords, keine neue Subtraktion unplausibler Session-/Activity-Abschlusszeitstempel.

## Bestehende Bestände

Privater GpxLibrary-Index motionVersion1 berechnet bisherige Fahrtzeitmetadaten einmal neu. FitLibrary detailsVersion3 liest ältere abgeleitete FIT-Metadaten einmal neu. Beides auf dem vorhandenen Bibliotheksworker, ohne Änderung der Originaldateien, IDs, Titel, Herkunft, Profil-/Dienstverknüpfungen oder des gespeicherten Datums. Keine automatische Cloud-Neuübertragung. Alte Formate weiter lesbar; keine neue Einstellung/Sicherungskategorie/Abhängigkeit.

## Prüfung

Synthetische Regressionen für wiederholte Positionen, native Geschwindigkeit, zeitliche/Segmentgrenzen, Metadatenreparatur und langsamere GPS- als Sensorupdates ergänzt. Bestehende Analyse-, GPX-Pausen-, Bibliotheks- und Teilrecordprüfungen erhalten. Hochgeladene Originaldateien ausschließlich lokal verifiziert; private Tracks und dateispezifischer Prüffall werden nicht eingecheckt. Logs unter /workspace/.tools/bosch-fit-review.
