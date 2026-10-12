# Aufnahmezeit und Sekundentakt

Das Feld `GPS_ELAPSED_TIME` (Dauer) verwendet nach START die monotone Aufnahmezeit,
auch ohne GPS-Fix. STOPP hält exakt die Dauer des letzten aufgezeichneten Samples fest.
Weiter eintreffendes GPS, Live-Sensoren oder Speichern/Upload verlängern sie nicht.
Erst ein neuer START setzt die Zeit zurück. Vor der ersten Aufnahme bleibt die
bisherige GPS-Vorschau erhalten. GPS-Geschwindigkeit, Distanz und die Berechnung
von Bewegungszeit/GPS-Durchschnitten werden dadurch nicht verändert.

Der bestehende gemeinsame Runtime-Tick plant den nächsten Lauf an einer festen
Sekundengrenze relativ zum Aufnahmestart, statt eine Sekunde an das Ende der Arbeit
anzuhängen. Versäumte Grenzen werden übersprungen: keine künstlichen Nachholsamples,
keine zusätzlichen Threads oder Timer. Tatsächliche Sensortimestamps bleiben erhalten.

Journalschreiben, Flush/fsync, FIT-Export und Dateiexport laufen bereits auf dem
vorhandenen FIT-Worker. Rohwerte werden nicht gedrosselt. Weniger ausgewählte Felder
verringern Dateigröße/Schreibarbeit, sind aber keine Voraussetzung für korrekte Zeit.

Für die Diagnose auf echter Hardware meldet `RUNTIME_TICK_SLOW` Verspätung (`lateMs`),
Sample-/Berechnungsarbeit (`captureMs`) und synchrone Observer-/UI-Arbeit (`uiMs`).
Ausgabe erst ab 150 ms und höchstens alle fünf Sekunden. Sie misst keine GPU-Frametimes
und ist kein Beweis für eine Ursache auf dem Poco ohne entsprechende Diagnosedaten.

Regressionstests auf Android 13 und 15 prüfen Lauf ohne GPS, Einfrieren trotz weiterer
GPS-Fixes, identische FIT-Abschlussdauer, Reset bei START und stabile Sekundengrenzen
bei simulierten 350 ms UI-Arbeit pro Tick.
