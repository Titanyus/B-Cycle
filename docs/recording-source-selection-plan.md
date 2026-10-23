# Flexible Hauptquellen – Anforderung und kleiner Ausbauplan

Nutzerwunsch 23.10.2026: Hauptquelle je Messwert aus den im Profil zugeordneten geeigneten Sensoren wählen, sobald mindestens zwei Quellen existieren. Kein vollständiger Radcomputer-Umbau. Diese Datei beschreibt den nächsten Ausbau; noch keine Quellenwahl-/Sensorimplementierung vorgenommen.

## Tatsächlicher Stand

- RecordingConfig und sein Decoder akzeptieren für Geschwindigkeit nur SPEED/GPS_SPEED, für Distanz nur BIKE_DISTANCE/GPS_DISTANCE. UI und Sicherungsvalidierung bilden dieselben festen Alternativen ab.
- SPEED/RIDER_POWER werden im Indoor-Profil durch FTMS gespeist; outdoor Bosch. Primäre Leistungsquelle wird aus dem Profiltyp bestimmt, nicht frei gewählt.
- Höhe kommt aus GPS; spätere DEM-Korrektur ist eine Nachverarbeitung, kein Barometersensor.
- PowerSource benennt bereits Bosch/BLE/ANT/Pedal/FTMS, aber BikeSensor.POWER ist noch unsupported. Enum-Eintrag ist keine implementierte Sensoranbindung. Barometer fehlt ebenfalls.

## Zielverhalten

Quellen je Messgröße über Sensorfähigkeiten des Profils anbieten, nicht über einen festen Schalter zwischen zwei Feldnamen. Konkrete Geräte eindeutig unterscheiden (etwa Pedale gegenüber separatem Kurbel-Powermeter). Zwei oder mehr geeignete Quellen: Auswahl. Eine Quelle: eindeutige Zuordnung. Keine: Wert nicht verfügbar. Kurzfristiger Verbindungsverlust entfernt keine Profilwahl. Ausfall-/Fallbackverhalten vor Umsetzung festlegen; keine stillen Quellenwechsel annehmen.

## Begrenzte Schritte

1. Fachliche Zuordnung je Profil für Geschwindigkeit, Distanz, Fahrerleistung, Kadenz, Puls und Höhe ergänzen; Altwerte von speedSource/distanceSource übernehmen und Standardverhalten Bosch/FTMS/GPS erhalten. Betroffen: RecordingData, BikeProfiles, RecordingControls/Quellen-UI, SettingsBackup/Supplement. Vorhandene stabile Feld-IDs und Dateiformate kompatibel halten.
2. Vorhandene Bosch-/GPS-/FTMS-/Pulsgurt-Fähigkeiten an diese Auswahl anbinden. RideRuntime-Aufnahme-Snapshot, Performance-Ereignisse und LiveGraphProvider aus derselben effektiven Quelle versorgen. Auswahl während einer Aufnahme einfrieren; nicht nur FIT-Writer umstellen.
3. Zusätzliche echte Sensoradapter separat ergänzen: BLE Cycling Power für externe Powermeter/Pedale; Barometer nach Klärung Smartphone-Drucksensor gegenüber externem Gerät. Barometerhöhe braucht Referenz/Kalibrierung, bevor sie als absolute MSL-Höhe in FIT gelangt. Keine nicht implementierten Quellen als nutzbar anzeigen.
4. Gezielte Tests: alte Profile/Backups/FIT unverändert lesbar, Mehrquellenwahl/Profilwechsel, eingefrorene Aufzeichnung, Lücken/Ausfälle, identische Quelle für FIT/NP/Graphen, keine Verwechslung von Motor- und Fahrerleistung sowie keine Distanzsprünge oder unkorrigierte Druckhöhe. Hilfe/IN-OUT/Backups gemeinsam pflegen.
