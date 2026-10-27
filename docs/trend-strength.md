# Trendstärke und Aufnahmeindikator – 19.10.2026

Seit 27.10.2026 berechnet RideRuntime genau einen gemeinsamen Trendstand für Tempo, Leistung, Kadenz und Puls, höchstens einmal pro Sekunde. DashboardView und MapDataFieldsView lesen nur RideTrendSnapshot, ohne eigene Glättungsfenster. Alle Tempo-Feldvarianten (Fahrrad/GPS, aktuell/Bewegungs-/Gesamtdurchschnitt) verwenden denselben Tempo-Trend. Die angezeigte Messgröße oder Quelle beeinflusst dessen Berechnung nicht.

Quelle ist jeweils die tatsächlich aufgelöste FIT-Primärquelle einschließlich des vorhandenen automatischen Fallbacks. Tempo nutzt den Durchschnitt dieser Quelle; Kadenz und Puls die bestehenden ausgewählten Fahrtdurchschnitte. Bei laufender/abgeschlossener Aufnahme wird für Leistung der bereits integrierte Mittelwert der aufgelösten Aufnahmeleistung nur lesend verwendet; vor Aufnahme vorhandene Durchschnittswerte der Quelle. Rohdaten, FIT-Aufzeichnung und gespeicherte Kennzahlen bleiben unverändert.

Die bestehenden Sichtbarkeitseinstellungen bleiben ausdrücklich pro Feldtyp erhalten: gemeinsamer globaler Schalter, unabhängige Feld-Auswahl und Bindung an aktivierte Datenanzeige. Ausblenden stoppt oder löscht keine Trendhistorie. Dreisekündiges Zeitfenster, Stärkeschwellen, Hysterese und Animation bleiben; Profilwechsel/Aufnahmestart löschen den zentralen Zustand, Quellenwechsel/fehlende oder überalterte Werte neutralisieren den betroffenen Kanal.

Der zeitgewichtete aktuelle 3-Sekunden-Wert wird mit dem jeweiligen Fahrtdurchschnitt verglichen. Aktuelle Felder und Durchschnittsfelder teilen denselben Trendstand.

| Messgröße | Ruhebereich | 1 Chevron | 2 Chevrons | 3 Chevrons |
| --- | --- | --- | --- | --- |
| Leistung | bis 2 % | bis 20 % | über 20 bis 40 % | über 40 % |
| Tempo / Kadenz | bis 1 % | bis 5 % | über 5 bis 15 % | über 15 % |
| Puls | bis 1 % | bis 3 % | über 3 bis 7 % | über 7 % |

Gleiche Grenzen oberhalb und unterhalb des Durchschnitts. Richtung nach oben/unten. Hysterese gegen Flackern: Leistung 1 Prozentpunkt, Tempo/Kadenz 0,5, Puls 0,25. Nach Beginn/Zurücksetzen drei Sekunden sammeln. Fehlende, veraltete oder ungültige Livewerte und fehlender/nichtpositiver Durchschnitt ergeben keinen Trend. Profil-/Fahrtwechsel setzt die Darstellung zurück.

TrendChevrons zeichnet bis zu drei getrennte Glyphen mit 500-ms-Überblendung. Chevron 1 bleibt am Ausgangspunkt; bei positivem Trend werden Chevron 2 und 3 nacheinander darüber ergänzt, bei negativem Trend darunter. Während der Animation nur neu zeichnen; keine Layoutänderungen pro Frame. Androids Animationseinstellung respektieren und beim Abhängen des Views abbrechen. Dashboard und Kartenfelder verwenden dieselbe Darstellung und dieselbe Berechnung. Die Glyphen sind gegenüber der bisherigen Darstellung um 25 % kleiner; der Abstand ihrer Positionen ist zusätzlich um 35 % reduziert. Chevron 1 hat 100 %, Chevron 2 85 %, Chevron 3 70 % Deckkraft. Die bestehende 500-ms-Überblendung multipliziert diese Zieldeckkraft.

In der Feldauswahl gibt es ein gemeinsames Schalter-Accordion unabhängig vom aktuell gewählten Datenfeld. Der Hauptschalter gilt global für alle Dashboard-Seiten und Kartenwerte. Öffnen/Schließen des Accordions verändert die Aktivierung nicht. Darunter stehen alle elf unterstützten Felder als Checkboxen mit kleinen Überschriften „Aktuelle Werte“ und „Durchschnittswerte“. Die Einzelauswahl bleibt beim Abschalten erhalten. Änderungen werden sofort gespeichert und verändern weder Trendhistorie noch Berechnung.

TrendIndicatorSettings verwendet trendIndicatorsEnabled und trendIndicatorsDisabledFields. Dashboard-Sicherungen und der künftige BikeUiConfigurationScope enthalten beide Werte. Fehlt die neue Feldauswahl, werden alte Kachelflags nach Feld zusammengefasst: Ein Feld wird nur deaktiviert, wenn alle gespeicherten Vorkommen deaktiviert waren; bei widersprüchlichen Vorkommen gewinnt aktiviert. Ohne Altwerte bleiben alle unterstützten Felder aktiviert. Alte Kachelflags bleiben zur Rückwärtskompatibilität lesbar.

Der weiche Stop-/Aufnahmepunkt-Wechsel gilt ab dieser Version während jeder Aufnahme für alle Radprofile. Bisheriges Intervall und STOP-Aktion bleiben erhalten; siehe indoor-dashboard-and-switch-accordions.md.

Dashboard-Zeiteinheiten werden in allen Kachelbreiten als „h“ angezeigt; das Format der Zeitwerte bleibt h:mm:ss.

Filter-/Sortier-Accordions im Streckenmanager erhalten einheitlich controlGap (8 dp) Innenabstand über und unter ihrem ausgeklappten Inhalt. Damit bleiben Bereichseingaben vom öffnenden Button und alle Filter-/Sortierinhalte vom folgenden Button getrennt.

Filter-/Sortierfenster und geschlossene Gruppenbuttons verwenden TERTIARY. Ausgeklappte Gruppen haben eine zusammenhängende abgerundete SECONDARY-Fläche mit zentraler Kontur und 8-dp-Innenabstand. Normale Buttons und Texteingaben im Inhalt verwenden TERTIARY; Auswahl-/Akzentzustände bleiben erhalten. Diese Flächenrollen sind auf Filter/Sortierung begrenzt.

## Trink-Erinnerung

Die profilbezogene Intervallerinnerung ist inzwischen umgesetzt; aktuelle Regeln in [drink-reminder.md](drink-reminder.md). Eine ausdrückliche Aktion „Getrunken“ zum Zurücksetzen des Timers bleibt eine mögliche spätere Erweiterung. Bloßes Schließen einer Erinnerung gilt nicht als Trinkbestätigung.
