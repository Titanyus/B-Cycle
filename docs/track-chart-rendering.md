# Verlaufsdiagramme in den Streckendetails

Die fünf vorhandenen Diagramme behalten Reihenfolge, semantische Farben, gemeinsame
Strecken-/Zeitachse, Höhen, Beschriftungen und die Skalierung aus Originalwerten.
Ihre Flächenfüllung verwendet jetzt die jeweilige vorhandene Diagrammfarbe mit
150/255 Deckkraft statt 32/255. Weiße Verteilungskurven und Zonenflächen bleiben
von dieser Änderung unberührt.

`RouteChartCurve` bereitet ausschließlich die bereits begrenzten Anzeigeserien auf:
pro Punkt höchstens neun benachbarte Werte in einem Radius von zwei Plot-Pixeln,
45 % Mediananteil und 55 % Originalanteil. Original-Nullwerte, Übergänge an Null,
Segmentenden und deutlich ausgeprägte Spitzen/Täler bleiben erhalten. Keine
Glättung über fehlende Werte, ungültige Achsenpositionen oder rückläufige Achsen.

Monotone kubische Hermite-Kurven verbinden die vorbereiteten Punkte. Begrenzte
Tangenten verhindern Überschwingen über benachbarte Werte und künstliche negative
Leistung/Kadenz. Doppelte Distanzpositionen behalten ihre vertikale Verbindung.
Pfade werden nur bei Größenwechsel neu aufgebaut; beim Zeichnen/Scrollen keine
erneute Datenaufbereitung. FIT, Statistik, Sensoren und Performance Engine bleiben
unverändert.

Validierung: gezielte Tests für Nullwerte, Lücken, Pixeldichte, unveränderte
Quelldaten und kubische Begrenzung sowie bestehende native Diagramm-/Details-Tests
bestanden. Release-Kotlin-Kompilierung erfolgreich.
