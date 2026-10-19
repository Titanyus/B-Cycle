# Trendstärke und Aufnahmeindikator – 19.10.2026

Die bestehenden Trendindikatoren vergleichen den zeitgewichteten aktuellen 3-Sekunden-Anzeigewert mit dem vorhandenen Fahrtdurchschnitt derselben Quelle. Direkte Messwerte und ihre Durchschnittsfelder zeigen dieselbe Abweichung. Aufzeichnung, FIT-Werte und Durchschnittsberechnung bleiben unverändert.

| Messgröße | Ruhebereich | 1 Chevron | 2 Chevrons | 3 Chevrons |
| --- | --- | --- | --- | --- |
| Leistung | bis 2 % | bis 20 % | über 20 bis 40 % | über 40 % |
| Tempo / Kadenz | bis 1 % | bis 5 % | über 5 bis 15 % | über 15 % |
| Puls | bis 1 % | bis 3 % | über 3 bis 7 % | über 7 % |

Gleiche Grenzen oberhalb und unterhalb des Durchschnitts. Richtung nach oben/unten. Hysterese gegen Flackern: Leistung 1 Prozentpunkt, Tempo/Kadenz 0,5, Puls 0,25. Nach Beginn/Zurücksetzen drei Sekunden sammeln. Fehlende, veraltete oder ungültige Livewerte und fehlender/nichtpositiver Durchschnitt ergeben keinen Trend. Profil-/Fahrtwechsel setzt die Darstellung zurück.

TrendChevrons zeichnet bis zu drei getrennte Glyphen auf festen Positionen mit 500-ms-Überblendung. Während der Animation nur neu zeichnen; keine Layoutänderungen pro Frame. Androids Animationseinstellung respektieren und beim Abhängen des Views abbrechen. Dashboard und Kartenfelder verwenden dieselbe Darstellung und dieselbe Berechnung. Der vorhandene Schalter Trendindikator bleibt maßgeblich.

Der weiche Stop-/Aufnahmepunkt-Wechsel gilt ab dieser Version während jeder Aufnahme für alle Radprofile. Bisheriges Intervall und STOP-Aktion bleiben erhalten; siehe indoor-dashboard-and-switch-accordions.md.

Dashboard-Zeiteinheiten werden in allen Kachelbreiten als „h“ angezeigt; das Format der Zeitwerte bleibt h:mm:ss.

## Vorgemerkt, nicht umgesetzt

Trinkerinnerung: einstellbares Zeitintervall während der Aufnahme. Optional eine ausdrückliche Aktion „Getrunken“ zum Zurücksetzen des Timers; bloßes Schließen einer Erinnerung gilt nicht als Trinkbestätigung. Details vor Umsetzung abstimmen.
