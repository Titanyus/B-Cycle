# Aufzeichnungsfelder und Messwerte

`RecordingFields` ist der fachliche Katalog für stabile Feld-IDs, bestehende Speichereinheiten, RECORD/SUMMARY und Standardauswahl. Die Reihenfolge einschließlich Altdefinitionen wird beibehalten. Der Katalog enthält keine Dashboardobjekte, Android-Ressourcen oder formatierten Werte.

`RecordingFieldLabels` stellt mit dem bestehenden Typ `RecordField` ausschließlich Beschriftungen und Gruppierungen für die FIT-Einstellungen bereit. Der Adapter verwendet die fachlichen Definitionen; die Anzeige legt nicht mehr fest, welche Felder Journal, FIT oder Backup kennen. Die bisherigen Texte, Gruppen und Auswahlmöglichkeiten bleiben erhalten.

RecordingConfig, RideJournal, FitWriter, IndoorFields und Einstellungssicherungen verwenden den fachlichen Katalog. Feldnamen, Journalversion, FIT-Einheiten/-Skalierungen/-Developer-Feldzuordnungen, Standardauswahl und Profile werden nicht migriert oder bereinigt. Insbesondere vorhandene Smartphone-Altdefinitionen bleiben erhalten.

## Kompatibilitätsreferenzen

`recording-fields.txt` und `legacy-recording.ride` stammen aus dem unveränderten Stand vor den Architekturarbeiten. `legacy-recording-full.fit` wurde aus diesem synthetischen Journal vor Schritt 4 mit Stand `ae5bf89` erzeugt. Keine persönlichen Sensor-/GPS-Daten.

Tests vergleichen den vollständigen Katalog einschließlich UI-Metadaten, schreiben das alte Journal bytegleich neu und vergleichen FIT-Records, Zusammenfassungen und Developer-Felddefinitionen mit der Referenz. Versionsabhängige App-/Geräteversionsfelder dürfen sich bei einem Release ändern und werden bereits separat in FitWriterTest geprüft.

## Messdaten und Darstellung

`RideMeasurements` enthält die numerische Reichweitenzuordnung einschließlich strenger Zuordnung der vom Rad gelieferten Modusnamen und optionalem OFF-Versatz. Aufzeichnung und Dashboard verwenden dieselbe Funktion. Null bleibt unbekannt, eine explizite Null bleibt Messwert; keine Zahl wird aus einem Dashboardtext zurückgelesen.

`LiveDataPresentation` enthält ausschließlich die bisherigen Formatierungsfunktionen als Kotlin-Erweiterungen. LiveData/LiveMetric enthalten keine Formatierungslogik mehr; Aufrufnamen, Texte, Einheiten, Rundung und Fallbacks bleiben erhalten. `recordingValues()` bleibt der numerische Einstieg. Rohwertskalierungen, Quellenwahl, Frischegrenzen, Leistungsberechnung, Messereignisse und Sensorverbindungen bleiben unverändert.

Ein zusätzlicher JVM-Test verwendet den Katalog und numerische Messwertverarbeitung ohne Android/Robolectric oder Dashboardinitialisierung und prüft Werte unter deutscher, US- und arabischer Zahlensprache. Bestehende UI-/Protokoll-/Aufzeichnungsprüfungen bleiben erhalten.
