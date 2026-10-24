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

## Spätere Sensor-Erweiterungen

Auf Nutzerwunsch für später festgehalten, keine zusätzliche Implementierung beauftragt. Aufgaben in [TODO.md](../TODO.md).

- Bestehende Bluetooth-Herzfrequenz- und FTMS-Anbindung wiederverwenden. Für mehrere Fabrikate möglichst den gemeinsamen Standarddienst implementieren; proprietäre Besonderheiten erst bei belegtem Bedarf ergänzen.
- Nächste sinnvolle Protokolle: Bluetooth Cycling Power Service für Powermeter/Leistungspedale und Cycling Speed and Cadence Service für separate Rad-/Kurbelsensoren. Geräte als konkrete Quellen unterscheiden; Radumfang für aus Umdrehungen ermittelte Geschwindigkeit/Distanz verwenden. In vorhandene Quellenwahl, automatische Ersatzquellen, Aufnahmesnapshot und Sicherung integrieren.
- Wahoo KICKR CORE: vorhandenen FTMS-Empfang am konkreten Gerät samt Firmware und angebotenen Messwerten prüfen. Bei passender FTMS-Unterstützung ist für reine Messdaten keine eigene Wahoo-Anbindung nötig; noch kein Gerätetest oder zugesicherte Kompatibilität.
- Trainersteuerung bleibt ein eigenständiger Ausbau gegenüber dem heutigen Lesen: FTMS Control Point für ERG/Zielwatt, Widerstand und Steigung, jeweils nur bei bestätigten Gerätefähigkeiten.
- Shimano Di2 benötigt eine eigene Integration für Gangposition und Schaltungsakku. Zuerst Generation, Funkmodul und verfügbaren Übertragungsweg prüfen; weder generelle Bluetooth-Gangdaten noch Unterstützung aller Di2-Modelle voraussetzen.
- ANT+ ist ein separater Zugang mit eigener Geräte-/Adaptervoraussetzung; Bluetooth-Unterstützung ersetzt ihn nicht. Erst bei konkretem Bedarf planen.

Stand 23.10.2026: die beschriebenen neuen Änderungen sind in APK **0.8.34 / Build 73** enthalten. [Buildnachweis](build-73.md). Frühere Angaben zu fehlender APK sind Verlauf.

## Einheitliche Quellen-Dropdowns, 24.10.2026

Geschwindigkeit, Distanz, Leistung, Kadenz, Puls und Höhe zeigen jeweils ein eigenes Dropdown. Zwei oder mehr konfigurierte Quellen: Wahl wie bisher. Eine Quelle: ihr Name sichtbar, Dropdown deaktiviert. Keine Quelle: Keine Quelle sichtbar, deaktiviert. Aufnahmesperre bleibt zusätzlich wirksam. Keine Auswahl wird allein durch das Anzeigen einer deaktivierten Zeile gespeichert; automatische Ersatzquellen, FIT-IDs, Profil-/Sicherungsformat und Quellauflösung unverändert.
