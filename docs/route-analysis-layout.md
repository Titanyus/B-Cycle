# Streckendetails – seitliche Gruppen und feste Diagrammskalen

Datum/Uhrzeit im Inhaltsbereich verwenden nun 14 sp (BicycleDesign.bodySp)
statt 12 sp. Die Kennzahlengruppen behalten ihre Reihenfolge und ihre Kacheln.
Ihre bisherigen Überschriften oberhalb entfallen. Stattdessen zeigt eine
24-dp-Seitenspalte links die Gruppenbezeichnung um 90° gedreht, in FOREGROUND
und Systemfont 900 Black. Wie bei den Diagrammbezeichnungen sind 12 sp die
Ausgangsgröße; lange Titel werden innerhalb der verfügbaren Höhe angepasst.
Kennzahlenzeilen verwenden mindestens 120 dp statt 140 dp; Schrift-/Icongrößen
bleiben erhalten. Zwischen den tatsächlich vorhandenen Gruppen liegen die zentralen feinen,
teiltransparenten FOREGROUND-Trenner mit nur 2 dp Abstand oben und unten
(statt 12/8 dp). Fehlende Gruppen erzeugen keine Leerflächen.
Über Karte und Diagrammen stehen keine zusätzlichen Überschriften mehr.

RouteChartScale berechnet ausschließlich Darstellungsgrenzen:

- Tempo, Kadenz und Leistung beginnen bei null.
- Puls und Höhe beginnen beim kleinsten gültigen Messwert.
- Die obere Grenze wird auf das nächste Vielfache von fünf aufgerundet.
  Konstante Daten bekommen bei Bedarf fünf Einheiten zusätzlichen Raum.
- Höchstens drei, möglichst zwei oder drei runde Zwischenmarken. Tempo verwendet
  Vielfache von zehn km/h; die anderen Skalen gerade ganzzahlige Marken.
- Marken direkt neben den Endwerten werden vermieden. Sehr kleine Wertebereiche
  dürfen weniger Zwischenmarken haben, wenn keine passende runde Marke hineinpasst.
- Kein prozentualer zusätzlicher Randaufschlag. Fehlende/nicht endliche Werte
  liefern keine Skalenextrema und verbinden keine Kurvenabschnitte.

Nur das Leistungsdiagramm der Streckendetails heißt „Leistung“ (detailLabel).
Die vorhandene Bezeichnung „Fahrerleistung“ in Dashboard-/Graph-Auswahlen bleibt
erhalten. Diagrammreihenfolge, gemeinsame Distanzachse, semantische Farben,
Kennzahlen, Aktionen, FIT-Dateien und Live-Dashboard-Diagramme bleiben erhalten.

Prüfung: RouteChartScaleTest, RouteAnalysisViewTest und RouteAnalysisUpdateTest.
Sie prüfen Grenzen, Anzahl/Abstände der Zwischenmarken, konstante und fehlende
Daten, seitliche Gruppen/Trenner, größere Datumszeile, Hochformat-Geometrie,
Themenwechsel, Einheiten, gemeinsame Achse und bisherige Interaktionen.

## Umgesetzt in 0.8: Kürzere Leistungsbeschriftung

Nutzerwunsch vom 17.10.2026 nach Build 33: In den Streckendetails „Normalisierte Leistung“ durch „Norm. Leistung“ ersetzen, damit die Beschriftung nicht zweizeilig wird. In Version 0.8 umgesetzt; Berechnung und übrige Verwendungen bleiben erhalten.

## Anpassungen nach Build 109

Bei Fahrten ohne GPS-Geometrie entfällt die Höhen-Kennzahlenzeile vollständig, auch wenn Metadaten Nullwerte für Auf-/Abstieg enthalten. Andere Kennzahlen und die gespeicherten Werte bleiben erhalten.

Die Zahlen der normalen AnalysisMetricTile-Kacheln stehen mit ihrer Mitte unter dem Icon. Die Einheit wird unabhängig rechts daneben angeordnet; Platz für sie wird beim Messen symmetrisch reserviert. Option nur für diese Streckendetailkacheln, keine Änderung der rechtsbündigen Fahrerprofilwerte, Streckenlisten oder übrigen AnalysisValueRow-Nutzer. Beschriftung bleibt zentriert. Gezielte Native-Tests für mehrere Kachelbreiten/Einheiten und GPS-lose Fahrten erfolgreich.
