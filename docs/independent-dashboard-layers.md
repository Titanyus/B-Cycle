# Unabhängige Dashboard-Ebenen: Daten, Graph und Visualisierung

Der Feldauswahldialog besitzt drei zunächst geschlossene Accordions:

- **Daten:** „Keine Daten anzeigen“ über dem bisherigen Quellen-Dropdown;
  anschließend die vorhandenen Messwerte und Kategorien. Dropdown und Option
  bleiben außerhalb der eigenen, begrenzten Messwertliste.
- **Graph:** „Keine Graphen“ oder maximal zwei Grapharten mit ihren Farbpunkten.
  Das frühere Auswahlfeld „Graph-Darstellung“ ist vollständig entfernt.
- **Visualisierung:** unveränderte unabhängige Auswahl der Zonen-Glow-Quelle.

Graph und Visualisierung sind eigene Geschwister des Daten-Accordions und werden
beim Quellenwechsel nicht entfernt/neu angehängt. Ein äußerer Scrollbereich macht
alle drei Bereiche auch bei gleichzeitiger Öffnung erreichbar. Die Messwertliste
hat eine intern begrenzte Höhe, keine zusätzliche Benutzereinstellung.
Graphen bleiben wie bisher auf den geeigneten großen Kacheln verfügbar.
Kartenfelder besitzen das Daten-Accordion und die bisherige leere Feldbelegung.

## Zustand und Darstellung

DashboardDataVisibility speichert acht unabhängige Sichtbarkeiten je Seite in
dashboardDataVisible_0 bis _3. „Keine Daten“ blendet Hauptwert, Einheit und
Hauptbeschriftung aus, ohne Kachelgeometrie, Sensoren oder den gewählten Feld-ID
zu ändern. Bei erneuter Feldwahl wird Datenanzeige aktiviert. Graphen und Glow
werden durch diese beiden Aktionen nicht geändert. Ohne Daten gibt es auch
keinen gesprochenen versteckten Messwert in der Kachelbeschreibung.

Graphhelligkeit wird weiterhin aus der tatsächlichen Datenüberlagerung abgeleitet:
ohne Daten bleibt der Graph heller; mit Daten gelten die vorhandenen gedämpften
Linien-/Flächenwerte. Dies ist Rendering, kein auswählbarer Graph-Modus.

## Kompatibilität

Fehlt die neue Sichtbarkeit, wird eine alte „Nur Graph“-Einstellung nur dort als
ausgeblendete Daten übernommen, wo auf dem damaligen Preset tatsächlich ein
Graph ausgewählt und sichtbar war. Ohne Graph bleiben Daten bei der Migration
sichtbar. Ein erstes Ändern der Graph-Auswahl hält die Datenanzeige ausdrücklich
fest, damit eine alte globale Vorgabe sie nicht unbeabsichtigt umschaltet.

Explizite Sichtbarkeiten bleiben beim Presetwechsel je stabilem Slot erhalten.
BackupSection.DASHBOARD exportiert je Seite ein streng validiertes dataVisible-
Array mit acht booleschen Werten. Ältere Backups ohne diesen Eintrag verändern
vorhandene neue Sichtbarkeiten nicht. Die alten graphDisplayModes werden für
vorhandene Preferences/Backups weiterhin gelesen und als Kompatibilitätsmetadaten
bei Datenänderungen aktualisiert; sie sind nicht mehr die aktive UI-Steuerung.

## Prüfung

DashboardPresentationTest prüft echte Editor-Auswahl und erneutes Öffnen, alle
drei Ebenen, Scrollbarkeit, Quellenplatzierung, alte Modi, Presets sowie neue und
alte Backups. Bestehende FieldPicker-, LiveGraph-, Trainingszonen-, Stil- und
Backup-Prüfungen decken den Funktionserhalt ab. 44 gezielte Tests und Kotlin-Kompilierung bestanden. Keine Änderungen an FIT,
Sensorverarbeitung, Graphdaten oder Performance Engine.
