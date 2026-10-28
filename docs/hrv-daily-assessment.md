# Eigene HRV-Tagesauswertung – erster funktionaler Entwurf

Unter HRV-Auswertung einen Tagesbalken auswählen und im festen Footer Auswertung öffnen. Eigener datierter HRV-Screen im bestehenden Design: schlichter SECONDARY-Header ohne Rahmen/Glow, ausgeblendete Android-Statusleiste, große logarithmische HRV-Zahl und Bereichsbalken, Tageshinweis im normalen TERTIARY-Kasten, darunter Ruhepuls mit eigenem Korridor und Punkt. Keine Trendpfeile und keine Baseline-/Trendzeile. Standardnavigation zurück erhält die bestehende Tagesauswahl. Info erklärt die eigenen Rechenregeln.

## Wert und persönlicher Korridor

Vorläufiger eigener Wert: **2 × ln(RMSSD in Millisekunden)**. Positive endliche RMSSD erforderlich, keine Begrenzung auf0–10, keine Alterskorrektur und keine Behauptung einer identischen HRV4Training-Formel. Nicht als validierter proprietärer Recovery-/Fitnessscore bezeichnen. Keine neuen Resultatfelder persistieren, vorhandene Messwerte und Annahmekriterien unverändert.

Tagesauswahl genau wie bestehendes HRV-Diagramm: letzte Messung pro gespeichertem lokalem Tag, gemeinsamer Bestand App/Import, abgelehnte bzw. fehlende Werte nicht künstlich ersetzen. Normalbereich unverändert aus HrvTrends: Mittelwert±1Stichproben-SD der verfügbaren vorherigen60Kalendertage, mindestens14gültige Tageswerte. Die Grenzen werden für die Darstellung mit derselben2ln-Transformation umgerechnet. Es wird ausdrücklich kein anderer Normalbereich im Lograum neu geschätzt. Bereichslage in Rohwerten vergleichen, dadurch identisch mit dem Tagesdiagramm. Gleichheit mit einer Grenze zählt innerhalb. Nichtpositive Scoregrenzen bleiben ohne erfundenen Zahlenersatz; ein komplett nichtpositiver Referenzbereich gibt keine Trainingsempfehlung frei.

Ruhepuls und dessen Referenzbereich kommen ebenfalls aus bestehender HrvTrends-Serie. Kein zusätzlicher Puls-Auf-/Abschlag auf den HRV-Wert oder die Tagesempfehlung.

## Subjektives Befinden und Hinweise

Ausgewählter Tag plus drei vorherige Kalendertage, keine vier letzten beantworteten Tage. HrvContextColors liefert Tagesmittel aus vorhandenen Schlafqualität/Energie/Motivation(invertiert), Müdigkeit/Muskelkater. Diese verfügbaren Tagesmittel gleichgewichtet mitteln; fehlende Angaben/Tage nicht nullfüllen. Erfasste Tage als x von4 und konkretes Datumsfenster anzeigen. Zustände auf Skala0gut–10belastet: ≤4gut, ≥6belastet, dazwischen gemischt, ohne Wert unbekannt. Keine Trendbehauptung aus einem bloßen Mittelwert.

Vorläufige eigene Entscheidungsregel:

| HRV-Lage | Befinden | Hinweis |
|---|---|---|
| fehlt/abgelehnt/nicht logarithmierbar | beliebig | Keine Einordnung möglich |
| Normalbereich fehlt/noch zu wenig Historie | beliebig | Normalbereich aufbauen |
| unterhalb | belastet | Heute ruhig angehen |
| unterhalb | gut/gemischt/unbekannt | Intensität heute begrenzen |
| innerhalb/oberhalb | belastet | Intensität heute begrenzen |
| innerhalb/oberhalb | gut/gemischt/unbekannt | Wie geplant trainieren |

Semantische Farben direkt TrainingZoneColors: Grün/Yellow/Rot/Grau. Text nennt HRV-Lage und Befinden getrennt, auch wenn sich beides unterscheidet. Oberhalb wird ohne pauschalen Zusatzbonus wie innerhalb behandelt. Diese erste Regel ist für spätere gemeinsame Anpassung gedacht; keine Replikation der unveröffentlichten HRV4Training-Gesamtlogik.

## Umsetzung

HrvController.dailyAssessment liest vorhandene kleine Verlaufssummaries und Tagesantworten im bestehenden Speicherworker. HrvDailyAssessment berechnet das Modell; HrvDailyAssessmentView zeigt es in beiden Designs mit zentralen Schrift-/Abstands-/Kartenmaßen. Zwei leichte Canvas-Bereichsanzeigen, kein Polling/keine zusätzliche Bibliothek. Eigene große Werttypografie bleibt bei erneutem Dialogstyling erhalten. Screenhilfe SettingsHelpPage.HRV_DAY. Alle anderen HRV-/FIT-/Import-/Sicherungswege bleiben erhalten.
