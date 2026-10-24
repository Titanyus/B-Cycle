# B|CYCLE 0.8.40

- Sticky-Header zentral League Gothic / 30 sp / nur Großbuchstaben. Allgemeines Dialogstyling überschreibt diese Rolle nicht mehr. Gespeicherte Profilnamen und Titel behalten ihre Schreibweise.
- Dashboard-Footer bei allen Fahrradtypen dauerhaft sichtbar, sobald das aktive Layout keine Karte enthält. Dashboards mit Karte behalten ihr bisheriges Ausblendverhalten.
- Datenlisten mit kleinen zentralen Kategorieüberschriften und horizontalen Trennern; Trendgruppen ebenfalls getrennt.
- Graphauswahl als Schalter-Accordion wie Daten/Trendindikator. Ausschalten ersetzt „Kein Graph“, Auswahl aktiviert automatisch; maximal zwei Graphen und Zeitfenster bleiben erhalten.
- Trink-Erinnerung als letzter Punkt der Trainingshilfe mit Trenner, ohne eigenen Sticky-Header. Intervall wie bisher, Anzeigedauer 5–60 Sekunden in 5-Sekunden-Schritten, Positionskacheln Unten/Mitte/Oben wie bei Benachrichtigungen. Standards 15 Sekunden/Mitte. Neue Werte profilbezogen, kopiert und gesichert; Altwerte kompatibel. Änderungen an Dauer/Position starten das Erinnerungsintervall nicht neu.
- Karten-Zoom: kontinuierliche Pinch-Verarbeitung auch bei geringem Fingerabstand, mit kurzem weichem Nachlaufen. Neue Berührung stoppt sofort. Native Verschiebung/Fling unverändert; Zoomgrenzen und Kameramodi erhalten.
- Sensor-Einstellungen testweise ohne sichtbare horizontale Zeilentrenner, bei unveränderten Abständen und Funktionen.

Gezielte Tests während der Umsetzung erfolgreich: Sticky-/HRV-/Benachrichtigungsstil, kartenloser Footer und Kartenwechsel, Feldauswahl/Graphlimit/Zeitskalen/Speicherung, Trinkintervall/Timeout/Positionen/Profilkopie/Backup sowie Sensorbedienung. Zusätzliche Karten-Zoom-/Pinch-/Nachlauf-/Abbruch-/Grenzprüfungen erfolgreich. Native Vorschauen visuell geprüft; Typografie-/Atlas-/XML-/Diff-Prüfung erfolgreich. Kein Gesamtlauf oder Hardwaretest.
