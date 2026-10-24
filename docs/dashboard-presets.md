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

## Ergänzung: Dashboard 9

Dashboard 9 (`EIGHT`) enthält acht unabhängige Slots in drei gleich hohen Reihen:
2 / 3 / 3. Das Thumbnail verwendet dieselben Reihen und Gewichtungen; die dritte
Auswahlreihe ist damit vollständig. Die ersten acht Vorlagen behalten IDs und Positionen.

`DashboardLayout.FIELD_COUNT` erweitert die Belegung auf acht Slots. Die bisherigen
sieben Default-IDs bleiben unverändert; Slot 7 hat initial `HEART_RATE`. Gespeicherte
Sieben-Feld-Belegungen werden beim Lesen ergänzt, ohne ältere Entscheidungen zu ändern.
Das achte Feld ist wie alle anderen frei auswählbar und über die vorhandenen Präferenzschlüssel persistent.

Sicherungen enthalten nun acht IDs pro Seite. Alte Sicherungen mit sieben IDs werden
weiterhin akzeptiert; beim selektiven Wiederherstellen bleibt die bestehende achte
Belegung erhalten. Unbekannte Feld-IDs oder mehr als acht IDs werden abgewiesen.
Neue Acht-Feld-Sicherungen benötigen eine App-Version mit dieser Erweiterung.

44 gezielte Tests prüfen bestehende und neue Reihen, Thumbnails, Speicherung, Änderung
des achten Feldes einschließlich Activity-Neustart, Sieben-Feld-Migration, Backup-Roundtrip
und alte Backups sowie einheitliche Typografie/Farbe und Glow. Keine neue APK für diese
Ergänzung; die oben genannte APK 0.7.7 enthält die ersten acht Vorlagen.

## Einheitliche Zahlen in Dreier-Reihen

Alle Dreier-Kacheln verwenden zentral feste `tripleValueSp = 45 sp` und `tripleUnitSp = 10 sp`.
Automatische Größenänderungen nach Wertlänge sind dort deaktiviert. Drei dp seitlicher
Innenabstand und eine gemeinsame zentrierte Anordnung von Zahl, kleiner Einheit und
gegebenenfalls Chevron sichern Platz. Das Chevron erhält Messraum, wenn es sichtbar ist, und wird
bei Gleichstand ausgeblendet. Die numerische Schriftfamilie und das Einheitengrau bleiben erhalten.

Distanzen bis unter 100 km zeigen zwei Dezimalstellen, ab 100 km eine, nur in diesen Kacheln.
Dauerwerte behalten Sekunden; ihre redundante Formateinheit wird dort als vorhandenes `h`
angezeigt. Auswahlen und Rohwerte bleiben unverändert. Werte außerhalb der vorgesehenen
Breite werden sauber gekürzt statt die feste Schriftgröße aufzuheben.

Die Darstellungstests berücksichtigen `99,99 km` und `999,9 km` ab 320 dp Breite;
längere Zeit-/Geschwindigkeitstexte mit Einheit und Chevron auf breiteren Displays. Sie prüfen feste Schriftgröße, vollständige Darstellung dieser Beispiele,
mittige Einheiten und alle Dreier-Reihen. Feldbreiten unterscheiden sich höchstens
um einen physischen Pixel durch Rundung des verfügbaren Rasters, nicht um unterschiedliche Gewichte.

Die feste Zahlengröße wurde auf Nutzerwunsch von 32 auf 45 sp erhöht (+40,625 %).
Bezeichnungen und Einheiten behalten ihre Größen. Bei ausgeblendetem Chevron wird
kein Platz dafür abgezogen. Für diese letzte Größenänderung wurde ausdrücklich keine
Testsuite ausgeführt; die frühere Validierung mit 32 sp gilt nicht als neue Prüfung von 45 sp.

## Footer ohne Karte

Die untere Dashboard-Bedienleiste bleibt bei jedem tatsächlich aktiven Layout ohne Karte immer sichtbar, unabhängig vom Fahrradtyp. Indoor fällt über seine kartenlosen Layouts unter dieselbe Regel; keine gesonderte Radtyp-Ausnahme. Layouts mit Karte behalten die bestehende Auto-Hide-/Bewegungs-/Touchlogik. Wechsel zurück zu einem kartenlosen Layout blendet die Leiste über die vorhandene Reveal-Logik wieder ein.
