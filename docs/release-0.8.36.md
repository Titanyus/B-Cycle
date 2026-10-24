# Änderungen in 0.8.36

- Design & UI: globaler Schalter Flat-Design. Aus = Glow-Design (Standard); ein = einfarbige Gestaltung ohne dekorative Verläufe, Glows und Schatten. Beide verwenden dieselben Controls und Funktionen. Bosch-Fahrmodus-Glow bleibt unabhängig. Modus in Einstellungssicherung enthalten.
- Hervorhebungen verwenden PRIMARY. ACCENT bleibt als separate Themefarbe für spätere Highlights erhalten und wird weiterhin unabhängig gespeichert.
- Auswahl-Schalter rechts, Beschriftung links, auch bei bisherigen Checkbox-/Radio-Zeilen und Dreeve-/ZIP-Importlisten. Auswahlverhalten und Speicherung erhalten.
- Textfeld-Glow wandert beim Scrollen korrekt mit dem Feld, ohne doppelten Versatz; Sticky-Header bleiben geschützt.
- Dashboard-Graphen über 30/60/120 Sekunden beziehen bis zu sechs statt drei Punkte in die Glättung ein. Vollgraph, Null-/Datenlücken, Originalmesswerte und FIT bleiben erhalten.
- FIT-Aufzeichnung: eigene Quellen-Dropdowns für Geschwindigkeit, Distanz, Leistung, Kadenz, Puls und Höhe. Eine/keine Quelle wird sichtbar, aber deaktiviert angezeigt. Automatische Ersatzquellen unverändert.
- Dreeve: nach bestätigter Verarbeitung wird der Dreeve-Name abgeholt. Automatische Profilnamen werden nicht mehr als manuelle Umbenennung zurückgeschrieben. Schutz neuerer bewusster lokaler Änderungen bleibt erhalten. Bereits überschriebene Dreeve-Namen lassen sich nicht rekonstruieren.
- Kleine aktuelle Zoomstufe unten rechts auf der Dashboard-Karte.
- Separater HTML-Designer: Glow-Intensität und aktive Auswahl mit gemeinsamer Kontur, Basisfarbe PRIMARY. Keine zusätzlichen Intensitätsregler in der App.

Gezielte native Tests in den Umsetzungsschritten erfolgreich: Graphen und FIT-/Journal-Unverändertheit, Quellen und Sicherung, Dreeve-Namensrichtung/Konkurrenz, Flat-/Glow-/PRIMARY-Darstellung, Auswahl und Scroll-Glow, HRV und Kartendarstellung. Native Vorschauen visuell geprüft, Browser-Designer einschließlich Export/Import geprüft. Kein Gesamtlauf, reales Dreeve-Konto oder Gerätetest.
