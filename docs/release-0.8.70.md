# B|CYCLE 0.8.70

Änderungen seit Build110:

- Kachelgröße nach Drag-and-drop zuverlässig wiederherstellen, auch bei Abbruch oder erneutem Ziehen.
- Statussymbole links jetzt25dp wie rechts (rechts weiterhin4dp Reserve bei gemeldetem Datenschutzbereich). Fahrradlicht bei sichtbarer Android-Statusleiste direkt darunter ohne zusätzlichen oberen Abstand, rechts16dp.
- Zusammenfassung mit Schaltwerk-Akku bei Fahrtende links neben dem E-Bike-Akku. Bis einschließlich20% gelb/orange, bis einschließlich10% rot in bestehenden Dashboardfarben.
- Untere mittlere Dreierkachel bei sichtbarem Footer ohne Karte12dp höher. Kompaktere Aufzeichnungs-/Schaltwerk-Accordions, Schaltwerk wie übrige Sensoren mit Trennen/Löschen. Nur Hauptmenü: mehr Abstand um Gruppentrenner und27dp zwischen „Erweitert“ und Bild.
- Intervals-HRV-Sammelabruf auf korrekten JSON-Zeitraum-Endpunkt umgestellt, fehlende Null-Tage unterstützt. Fehler mit Datum/HTTP-Code statt pauschalem Unterbrechungstext. Vorhandene fremde HRV-Werte weiterhin schützen. [Details](intervals-hrv.md).
- Optionale Windrose in Kartenoptionen pro Fahrradprofil, SVG-Geometrie in Schwarz/70%, echter Kreismittelpunkt als Drehpunkt. Am Vergrößern-Symbol nur bei Folgen und ausgeblendeten Kartenbuttons, rotiert mit der Karte. Mitgesichert.
- Tempo-/Kadenz-/Leistungs-/Puls-/Höhendiagramme gemeinsam in X zoomen und danach verschieben, mit HRV-Fingerabstand und nativer Trägheit. Stabile Y-Skalen, kein erneutes Datei-Lesen beim Scrollen. [Details](route-analysis-navigation.md).
- Fehlerhafte Verdichtung fragmentierte Kadenzkurven künstlich. Tatsächlich zusammenhängende Tretabschnitte bleiben verbunden; echte Pausen, Rohdaten und Kennzahlen unverändert.
- „Auf Karte angezeigt“ erster Streckenfilter: alle drei zugewiesenen Strecken finden, auch ausgeblendete.

Gezielte native UI-/Gesten-/Pixel-, HRV-HTTP-, Sicherungs-, Aufzeichnungsabschluss- und Filtertests erfolgreich; Typografie/Atlas/Diff geprüft. Keine vollständige Gesamtsuite, kein physischer Gerätetest und kein privater Live-HRV-Upload zu intervals. Der Zugang selbst funktioniert laut Nutzer (400 Fahrten hochgeladen).

Buildnachweis folgt nach Veröffentlichung.
