# Fahrradprofile: gemeinsame Aufzeichnung und kleine Dropdown-Abstände

Auf ausdrücklichen Nutzerwunsch ersetzen die bisherigen beiden Sticky-Abschnitte „FIT-Aufzeichnung“ und „Strecken-Aufzeichnung“ einen gemeinsamen Abschnitt **Aufzeichnung**. Bestehende Sticky-/Accordiongestaltung beibehalten.

Reihenfolge:

1. Bestehende Fahrt-Benennen-Funktion.
2. Höhenkorrektur nach Aufnahme, aus dem Kartenbereich hierher verschoben. Unveränderter Preference-Key `FitElevationCorrection.preference` und bestehende Profilspeicherung; keine doppelte Option im Kartenbereich.
3. Rahmen-Accordion **Sensor-Hauptwerte für die .fit-Datei**. Zuerst bisherige kurze Erklärung, danach die sechs bestehenden Quellenwahlen von Geschwindigkeit bis Höhe. Verfügbarkeit, Fallback, Auswahl und Aufnahmesperre unverändert.
4. Rahmen-Accordion **Zu speichernde Daten für die .fit-Datei**. Zuerst „Diese Daten werden in der .fit-Datei gespeichert.“, danach sämtliche bisherigen Gruppen für Metadaten und Sensor-/Streckendaten. Vorhandene Switches/Listener/Expansionseinstellungen der inneren Gruppen erhalten; keine Speicherfelder geändert. Profilabhängige Indoor-Auswahl bleibt erhalten.

Neue äußere Accordions sind zunächst geschlossen und verwenden die bestehende Sitzungsauswahl `expandedFitSources`. Keine neuen Konfigurations-/Backupfelder. Abschnittsbody `bike_recording_settings`; bisheriger Schaltertag `map_elevation_correction` bleibt trotz neuem Ort stabil.

Abstände: Gemeinsamer kleiner `BicycleDesign.controlGap` von 8 dp. Zusätzliche unsichtbare Trenner zwischen Sensorframes entfernt. Quellenzeilen besitzen statt jeweils 8 dp oben UND unten einen einzigen Zeilenabstand von 8 dp; Inline-Kinder bekommen weiterhin keinen Extraabstand. Trackdarstellung und bisherige Speicherdaten-Gruppen verwenden bereits denselben zentralen Abstand. Header-/Inhaltsabstände innerhalb der Frames bleiben nach bestehender Konvention.

Zwei betroffene bestehende Tests erfolgreich: Profilreihenfolge/Frames/echte Schalterpersistenz und gemessene 8-dp-Abstände in Flat/Glow und E-Bike/Indoor; Quellenwahl/Verfügbarkeit/Aufnahmesperre inklusive Sicherungsrundreise. Profiltest für tatsächlich angezeigte und angefügte Dialogansicht ausführen (Main-Looper nach show abarbeiten). Logs `/workspace/.tools/recording-section-tests.log` (Quellenfall erfolgreich) und `recording-section-final-test.log` (Profilfall erfolgreich). Nur offenen Profilfall nach Ergänzung wiederholt, kein Gesamtlauf. Kompilation/Typografie/Atlas/Diff erfolgreich. Noch keine neue APK beauftragt/gebaut.
