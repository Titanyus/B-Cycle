# Flexible Hauptquellen je Profil

Stand 23.10.2026, nach Build 72. Nutzerentscheidung: bei Ausfällen automatisch geeignete Ersatzquelle verwenden; Barometer = Drucksensor des Smartphones, falls vorhanden.

## Umgesetzter Umfang

- `RecordingSources`: fachliche Zuordnung für Geschwindigkeit, Distanz, Fahrerleistung, Kadenz, Puls und Höhe. UI bietet ab zwei konfigurierten, aktivierten Quellen eine Auswahl; eine Quelle wird benannt, fehlende Quelle als solche ausgewiesen. Verbindungsunterbrechungen löschen keine Präferenz. Profilwechsel, Kopie und Einstellungssicherung erhalten die Auswahl einschließlich inaktiver Profile.
- Tatsächlich angebunden: Bosch und GPS outdoor, FTMS indoor, BLE-Pulsgurt und Smartphone-Drucksensor. Leistung/Kadenz haben derzeit je Profil nur Bosch oder FTMS. Separate BLE-Powermeter/Pedale und ANT+ sind weiter nicht angebunden und werden nicht als wählbar angeboten.
- Frische Werte der bevorzugten Quelle gewinnen; ansonsten erste geeignete frische Ersatzquelle, bei Rückkehr automatisch wieder bevorzugte Quelle. Ein Wert älter als fünf Sekunden ist kein frischer Primärwert. Auswahl bei START einfrieren; Änderungen der nächsten Aufnahme verändern laufende Aufnahmen nicht.
- Standard-FIT, private Analyse-FIT, Live-Aufzeichnungsgraphen und Performance-Geschwindigkeit nutzen aufgelöste Werte. Bestehende unabhängige Dashboard-Datenfelder und Rohkanäle bleiben erhalten. Fahrerleistung bleibt strikt von Motorleistung getrennt.
- Distanz als Summe beobachteter Zählerdifferenzen: beim Quellenwechsel neuer Ausgangspunkt, kein Sprung auf den schon angesammelten Zähler der Ersatzquelle. Nicht beobachtete Lücken werden nicht nachträglich als Strecke erfunden.
- Smartphone-Barometer nur bei unterstütztem Sensor; Referenz erst mit gültiger GPS-Höhe. Android-13-Ellipsoidreferenz bleibt als solche markiert, keine erfundene MSL-Höhe. Beim Höhenquellenwechsel Offset zur kontinuierlichen Höhe, gemeinsamer Auf-/Abstieg und Gradient. Ohne Referenz keine absolute Barometerhöhe. Druckänderungen durch Wetter werden nicht separat korrigiert.

## Kompatibilität und Grenzen

`recordSourcesV1` als optionales JSON je Profil; alte Geschwindigkeits-/Distanzpräferenzen bleiben lesbar und liefern Migrationsstandards. Indoor-Änderungen erhalten verborgene Outdoor-Präferenzen. Keine neue Abhängigkeit oder zweite Sensorverbindung.

BCY7 nutzt bereits eine deklarierte Spaltenliste. Neue `PRIMARY_*`-Spalten hinten ergänzen, bestehende Roh-IDs/Reihenfolge nicht ändern. `PRIMARY_SOURCES_V1` im Aufnahme-Snapshot aktiviert ihre Verwendung; Altfahrten ohne Marker behalten den bisherigen FIT-Pfad. Bestehende FITs werden nicht umgeschrieben. Aufgelöste Developer-Felder ergänzen die Standardwerte; Rohwerte bleiben separat erhalten. Nachträgliche DEM-Korrektur bleibt ein eigener Verarbeitungsschritt.

Geräteprüfung für Bosch/FTMS/Pulsgurt und tatsächlichen Smartphone-Drucksensor bleibt erforderlich. Externe Powermeter benötigen einen eigenen Cycling-Power-Sensoradapter, bevor mehrere Leistungsquellen sinnvoll angeboten werden können. Keine APK für diesen Auftrag erstellt.

## Prüfungen

40 gezielte Fälle erfolgreich, kein Gesamtlauf: sieben neue Quellen-/Barometer-/FIT-/Runtime-/UI-/Backup-Prüfungen, 21 bestehende Aufnahmefälle auf Android 13/15/16, drei Sicherungsfälle, zwei Indoor-UI-Fälle, drei Indoor-Laufzeitfälle, drei Graphauswahlfälle und ein Journal-/FIT-Unverändertheitsfall. Der erweiterte UI-/Backupfall prüft zusätzlich inaktive Profile, echte Dropdownänderung und Aufnahmesperre. Zwei alte sechssekündige Indoor-Testfahrten auf zwölf Sekunden verlängert, um trotz der bereits eingeführten Zehnsekundengrenze weiterhin FIT-Ausgaben zu testen. Native Ansicht geprüft; Typografie-/Katalogprüfung erfolgreich.
