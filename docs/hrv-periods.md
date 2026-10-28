# HRV: Tag, Woche und Monat

Umschaltung oberhalb der bestehenden Kennzahlenauswahl. Jede neue Auswertungsseite beginnt in Tag; kein neues dauerhaftes Setting. Bestehende HRV-Dateien, Tagesauswahl, Messungsqualität und Fragebögen unverändert.

## Berechnung

- HrvTrends berechnet unverändert zuerst die Tageswerte: genau eine zentral ausgewählte Messung pro lokalem Tag; abgelehnte oder fehlende Kennzahlen bleiben Lücken. Verschiedene Apps bleiben gemeinsamer Bestand bei bekannter Einheit.
- HrvTrendAggregation fasst nur diese Tageswerte zusammen: ISO-Woche Montag bis Sonntag oder Kalendermonat. Jeder vorhandene gültige Messtag hat dasselbe Gewicht. Kein Mittel der Wochenmittel für Monate, keine Nullfüllung und keine Summierung mehrerer Messungen desselben Tages. Alle Kennzahlen oben zeigen den entsprechenden Periodenmittelwert.
- Linie: arithmetisches Mittel der im Zeitraum vorhandenen **täglich berechneten** 7-Tage-Trendwerte (weiterhin mindestens fünf Messtage im jeweiligen täglichen Siebentagefenster).
- Korridor: untere und obere Grenze der vorhandenen **täglich berechneten** Normalbereiche jeweils mitteln, über dieselben Tage mit vorhandenem Grenzenpaar. Tägliche Basis bleibt die Stichprobenstreuung der vorherigen 60 Kalendertage, ab 14 Werten, ohne aktuellen Tag. Kein neuer Referenzbereich aus Wochen-/Monatsmitteln, kein Konfidenzintervall und keine 7-Wochen-/7-Monats-Glättung.
- Die Periodenmittel verwenden alle gültigen Tage, Trend und Korridor jeweils nur die Tage mit verfügbarer Statistik. Messtage, Trendtage und Referenztage stehen deshalb bei der Auswahl. Frühere Werte dürfen weiterhin nötig sein, um den täglichen Trend zu bestimmen.
- Laufende Woche/Monat: „bisher“, Zeitraum endet beim heutigen Tag; Nenner der Messtage zählt nur bereits vergangene Tage einschließlich heute. Ein einzelner Messtag ist darstellbar, aber über die Tagesanzahl als dünne Datenbasis erkennbar. Keine erfundene Mindestanzahl für Wochen-/Monatsmittel.
- Eigene feste Y-Achse je Kennzahl und Auflösung aus deren komplettem Bestand inklusive Trend und Grenzen. Woche/Monat nutzen ihre aggregierten Werte statt der Tagesextrema. Scrollen/Zoomen ändert sie nicht; beim Auflösungswechsel wird die passende Skala wiederhergestellt. Reserve wie bisher: Puls5bpm, sonst5%, Untergrenze mindestens0.
- Balken unter ihrer vorhandenen zusammengefassten Untergrenze bleiben weiß, sonst PRIMARY; rein beschreibend, keine neue Readiness-/Trainingsempfehlung. Fehlende Perioden werden nicht überbrückt. RMSSD wird in seiner angezeigten Einheit gemittelt; kein implizites Logarithmieren oder neue lnRMSSD-Kennzahl.

## Kalender und Darstellung

- Tagesansicht: wöchentlicher senkrechter Strich an der **linken sichtbaren Balkenkante des Montags**, von der oberen Rasterlinie bis unter die X-Achse. Datum rechts daneben, nicht mehr mittig über einem vom Text abhängigen Strich. Montage ohne Messung bleiben kalendarisch vorhanden.
- Wochenansicht: gleich breite Kalenderwochen, X-Achse `KW05`, bei Platzmangel nur `05`, ohne Jahr/Datum. ISO-Wochenjahr im vollständigen ausgewählten bzw. sichtbaren Zeitraum, einschließlich KW53/Jahreswechsel.
- Monatsansicht: gleich breite Kalendermonate, X-Achse Monatsabkürzung wie `Okt`, bei Platzmangel nur der Anfangsbuchstabe wie `O`, ohne Jahr und abschließenden Punkt. Vollständiger Monatsname und Jahr im Zeitraum.
- Wochen/Monate verwenden ebenfalls durchgehende Periodentrenner an der linken Balkenkante. Wochen-/Monatsbeschriftungen werden niemals ausgedünnt. Reicht der Platz nicht für das längste volle Kürzel samt Abstand, wechseln alle sichtbaren Labels einheitlich zum kurzen Format; Entscheidung nur anhand der Zoomstufe, nicht der Scrollposition. Auch bei noch engerem Zoom bleiben alle Labels gezeichnet. Alle Trennstriche, Balken und deren Wertbeschriftungen bleiben erhalten. Text wird an beiden Plotgrenzen abgeschnitten, niemals festgehalten. Die bisherige Kalenderdatums-Ausdünnung der Tagesansicht bleibt unverändert.
- Zusätzliche Zoombegrenzung ausdrücklich erst später nach Nutzeransicht entscheiden; bisherige7-/14-Bucket-Grenzen unverändert. Alle Auflösungen behalten echte Zweifinger-Gesten ohne Mindestabstandssperre, kinetisches Scrollen und Accessibility-Zoom. Navigation erfolgt in gleichen Kalender-Buckets, nicht mit 30-Tage-Näherungen. Tagesmondphasen bleiben in Tag sichtbar; in Monats-/Wochenbalken würden sich mehrere Mondphasen überlagern und werden deshalb dort nicht eingezeichnet.
- Ausgewählte Periode bleibt beim Kennzahlenwechsel erhalten, auch wenn der letzte gültige Messtag der Kennzahlen verschieden ist. Auflösungswechsel nimmt die zuletzt repräsentierte Messung als Kalenderanker. Einzelmessungsdetails bleiben in Tag verfügbar; Wochen-/Monatsbalken öffnen keine irreführende einzelne Messung.
- Header bleibt schlicht SECONDARY ohne Glow/Rahmen; nach Periodenauswahl z.B. „Auswertung: KW5 2026“ bzw. „Auswertung: Oktober 2026“. Frisch geöffnete Auswertung weiterhin Tagesansicht mit heutigem Headerdatum.

## Prüfung

Gezielte Modelltests prüfen Tagesauswahl/fehlende Werte, Tagesgewichtung statt Wochengewichtung, leere Monate, Schaltjahr, ISO-Jahresgrenzen, gleiche Kalenderbreiten und Zusammenfassung der unveränderten täglichen Trend-/Referenzwerte. Native UI-/Canvas-Prüfungen für Balkenkanten/volle Trenner bei mehreren Zoomstufen, kurze Wochen-/Monatslabels, vollständige Periodenwahl, feste Y-Skala, Kennzahlenwechsel im älteren Monat trotz unterschiedlicher Messtage, laufende Zeiträume, fehlende Frequenzwerte sowie Flat/Glow-Vorschauen. Bestehende HRV-Diagramm-/Pinch-/Fling-/Mondphasentests bleiben erhalten. Native Testvorschauen: [Glow](previews/hrv-period-month-glow.png) · [Flat](previews/hrv-period-month-flat.png). Aktueller genauer Prüfstand in CONTINUE-HERE.md. Kein erneuter Hardwaretest; noch keine neue APK beauftragt.


Veröffentlichung28.10.2026: Diese Änderungen sind in **APK0.8.56/Build97** enthalten. [Buildnachweis und Download](build-97.md). Frühere Aussagen zu fehlender neuer APK sind Umsetzungshistorie.


Nach Build97: adaptive Wochen-/Monatskürzel ohne Auslassungen umgesetzt; noch nicht in APK0.8.56 enthalten. Acht gezielte native UI-/Scrollfälle grün (HrvTrendPeriodUiTest5, HrvChartScrollingTest3). Prüfungen zählen pro Periodentrenner genau ein Label bei mehreren Zoomstufen/Pan, langes/kurzes Format, identischeY-Skala, Auswahl und bestehendes Scrollen. Keine neue Auflösung oder Zoombegrenzung; Tagesberechnungen unverändert.


Veröffentlichung28.10.2026: Auch die zuletzt nach Build97 ergänzten Änderungen sind in **APK0.8.57/Build98** enthalten. [Buildnachweis und Download](build-98.md). Frühere Aussagen zu fehlender neuer APK sind Umsetzungshistorie.


Veröffentlichung28.10.2026: Die zuletzt nach Build98 ergänzten Änderungen sind in **APK0.8.58/Build99** enthalten. [Buildnachweis und Download](build-99.md). Frühere Aussagen zu fehlender neuer APK sind Umsetzungshistorie.


Nach Build99: Ein bewusster Tipp in der Tagesansicht markiert den ausgewählten Balken mit halbierten RGB-Kanälen (auch bei zuvor weißen Balken) und zeigt den festen globalen Aktionsfooter: Auswertung vorerst deaktiviert, Tagesbefinden direkt zum vorhandenen Tageseingabedialog, Löschen zur bestätigten bestehenden Messungslöschung. Initiale automatische Kennzahlenwahl/Scrollen zeigen keinen Footer. Zeitraumwechsel blendet ihn aus und entfernt die Markierung, Kennzahlenwechsel erhält den gewählten Tag. Keine Aktionen für aggregierte Wochen/Monate. Bisheriger Inlinebutton Messung und Tagesbefinden entfällt zugunsten des Footers. Löschen lädt Tagesbestand/Aggregate/Achsen neu; Tagesantworten bleiben gemäß bestehender Tageslogik gespeichert, ältere Messung kann wieder sichtbar werden. Keine neue Tagesauswertung vorwegnehmen.


Aktueller kompakter Ausbau nach Build99: Diagrammhöhe454dp, unter Zeitraumzeile sechs Kontext-Farbsymbole mit separat benanntem Modus ohne Legende. Sämtliche unteren Erklärungs-/Tageswert-/Referenztextblöcke entfernt. Tagesfooter zusätzlich Messergebnis, vorhandene Ergebnisfunktion direkt erreichbar. [Farbregeln und Datenbezug](hrv-context-colors.md). Die vorherigen Textblockbeschreibungen sind Umsetzungshistorie.
