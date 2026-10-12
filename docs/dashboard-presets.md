# Dashboard-Vorlagen und Bezeichnungen

Stand: 12.10.2026. Vier gespeicherte Dashboard-Seiten können unabhängig aus acht Vorlagen wählen.

Die bisherigen sechs IDs, ihre Reihenfolge und die Standardbelegung bleiben erhalten.
Die neuen IDs `HERO_SEVEN` (Dashboard 7) und `HERO_FIVE` (Dashboard 8) werden angehängt:

- Dashboard 7: ein großes Feld, darunter drei Felder, darunter drei Felder.
- Dashboard 8: ein großes Feld, darunter zwei Felder, darunter zwei Felder.

Das große Feld nutzt wie die bestehende Hero-Vorlage Slot 2. Alle sichtbaren Felder bleiben
frei belegbar. Die vorhandene Speicherung und selektive Dashboard-Sicherung akzeptieren
beide neuen Vorlagen. Die vier Dashboard-Seiten werden dadurch nicht auf acht erweitert.

Die Vorschauen zeichnen dieselben Reihen und Gewichtungen wie das Dashboard. Die Auswahl
behält ihre drei Spalten; eine dritte Reihe enthält die beiden neuen Vorschaubilder.
Der bestehende Design-&-UI-Scrollbereich nimmt die zusätzliche Reihe auf. Abbrechen verwirft
weiterhin die Auswahl; Speichern übernimmt sie für jede Seite separat.

`BicycleDesign.dashboardCaptionSp` definiert für alle Feldbezeichnungen feste 13,5 sp,
25 Prozent unter der bisherigen maximalen Größe von 18 sp. Die frühere Verkleinerung nach
Wortlänge oder Vorlage entfällt. Bezeichnungen verwenden `dashboardUnitColor`, exakt wie
Einheiten. Bei Bedarf erhält die Bezeichnung ausreichend Texthöhe innerhalb des Feldes,
ohne die Schrift zu verkleinern. Zahlen und Einheiten behalten ihre bestehende Größenlogik.
Neue Vorlagen nutzen die vorhandene Größenkalibrierung; im Querformat bleibt Kartenfläche frei.

## Prüfung

40 gezielte Tests bestanden: DashboardViewTest, DashboardLayoutTest, SettingsMenuTest,
BoschModeGlowTest und SettingsBackupTest. Geprüft wurden Reihenstruktur, Feldklicks,
Vorschauen, getrennte Seitenauswahl und Speicherung, feste Caption-Größe/Farbe bei langen
Bezeichnungen, Hoch-/Querformat sowie bestehende Glow- und Backup-Funktionen.
Die Android-Debug-Kompilierung ist Bestandteil dieses Testlaufs. Keine neue APK erzeugt;
visuelle Prüfung auf einem echten Gerät steht noch aus.

Der Stand ist inzwischen in [APK 0.7.7](milestone-077.md) enthalten.
