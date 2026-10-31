# B|CYCLE 0.8.75

- Der TERTIARY-Detailkopf reicht bis an die Seitenränder und direkt unter den Hauptheader; unten zusätzlich8dp Innenabstand.
- Der Fahrtname im Detailheader ist durchgehend linksbündig, auch beim Scrollen und nach Größenwechseln.
- Diagrammreihenfolge: Tempo, Kadenz, Puls, Leistung, Höhe.
- Puls-Z1 bei Karvonen, Maximalpuls und Friel nach unten offen: alle gültigen Werte unterhalb der Z2-Grenze zählen zu Z1. Trainingshilfe, Zonenanteile, Histogramme, Profil und Hilfe verwenden dieselbe Regel. Ungültiger/fehlender Puls bleibt ausgeschlossen; Z2+ und obere Grenzen unverändert.
- Runde X-Raster in1/2/5×10ⁿ-Schritten je Zoom. Linien und Beschriftungen scrollen mit den Daten; aktuelle Anfangs-/Endwerte bleiben am Rand. Mitlaufende Werte fahren durch Clipping unter den Randbereichen sauber aus dem Bild.

Geprüft:10 Detailansicht- und7 Komponentenprüfungen für die Kopf-/Layoutänderung;57 gezielte Pulsmodell-/Titel-/Zonen-/Darstellungsfälle mit erfolgreichen Nachprüfungen der angepassten Profil-/Glow-Erwartungen. Abschließend19 Achsen-/Navigation-/Gruppen-/Detailprüfungen erfolgreich, darunter native Pixelprüfung teilweise sichtbarer Schrift am Cliprand. Typografie/Atlas/Diff geprüft. Kein physischer Gerätetest und keine vollständige Gesamtsuite. Rohdaten, FIT-Aufzeichnung und NP-Berechnung unverändert.

[Buildnachweis und Download](build-116.md).
