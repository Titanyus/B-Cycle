# HRV-Balkenfarben nach Tageskontext

Unter der Zeitraumzeile des Diagramms stehen sechs gleich breite, nur mit Icons beschriftete Auswahlsegmente: Standard, Tagesbefinden, Training, Schlaf, Energie, Muskelkater. Symbole: Tagesbefinden `face-slightly-smiling`, Schlaf `bed`, Energie `biceps-flexed`, Muskelkater `target`; Standard und Training behalten ihre bisherigen Symbole. Der Name der ausgewählten Ansicht steht separat darunter. Bei Standard bleibt es beim Namen; alle anderen Modi zeigen inline „Name · Extremwert [farbige Quadrate] Extremwert“. Auswahl gilt temporär im geöffneten Screen, bleibt bei Kennzahl-/Zeitraumwechsel und Rückkehr aus Tagesbefinden erhalten. Kein neuer dauerhaft gespeicherter Einstellungswert.

Standard erhält bisherige HRV-Farbe (PRIMARY, unter persönlichem Bereich weiß). Kontextmodi ersetzen nur die Balkenfarbe; HRV-Werte, Kurve, Korridor, Achsen, Auswahlabdunklung und Zoom bleiben gleich. Diagrammhöhe454dp statt504dp (rund10% weniger), zentral für Tag/Woche/Monat. Alle Erklärungstexte und Tageswert-/Referenztextblöcke unter dem Diagramm entfernt. Tagestitel/Kennzahlenwerte oben und Balkenwerte bleiben. Fester Tagesfooter mit drei Aktionen: Messergebnis öffnet die Ergebnisansicht, Tagesbefinden direkt die Tagesfragen, Löschen bestätigt die Messungslöschung. Redundante Aktion Auswertung entfernt. Zentrale Sichtbarkeitssteuerung mit Schalter im Hauptkopf, initial verborgen; Tagesklick kann temporär einblenden. Tagesansicht, Datum und Tagesaktionen verwenden denselben Tag, bei frischem Einstieg heute. Ohne heutige Messung echte fehlende Werte anzeigen. Kein zusätzlicher Inline-Ergebnisbutton.

## Farblegende – 01.11.2026

Inline-Quadrate aus derselben `HrvContextColors.color`-Zuordnung wie die Balken, keine kopierten Farbwerte. Fünf Felder grün/gelb/orange/rot/violett bei Tagesbefinden, Schlaf, Energie und Muskelkater; vier Felder grau/grün/orange/rot bei Training. Endpunkte gemäß tatsächlicher Zuordnung:

- Tagesbefinden und Schlaf: Gut → Schlecht.
- Energie: Viel → Wenig (positive gespeicherte Antworten sind invertiert).
- Muskelkater: Gar nicht → Sehr.
- Training: Keins → Intensiv.

Schrift und Farben im bestehenden Beschreibungstext; Mittelpunkt als Trenner. ReplacementSpan zeichnet echte Quadrate statt geräteabhängiger Emoji, proportional zur Textgröße und vertikal mittig. Bei größerer Systemschrift darf der Text umbrechen, keine abgeschnittenen Beschriftungen. Screenreader erhält Beschriftung und Endpunkte ohne Objektzeichen. Standard bleibt ohne Legende. Kennzahl, Achsen, Zoom, Auswahltag und Kontextwerte ändern sich durch die Legende nicht.

## Farben und Rechenregel

Farben direkt aus TrainingZoneColors wie Leistungsdiagramm: Z3grün, Z4gelb, Z5orange, Z6rot, Z7violett. Ohne vorhandenen Wert Z1grau. Keine eigenen Farbkopien.

Schlaf und Energie verwenden jeweils den gespeicherten Sliderwert; Muskelkater den gespeicherten soreness-Wert. Zur gemeinsamen Skala0gut bis10belastet werden positive Schlaf-/Energiewerte invertiert, Muskelkater unverändert verwendet. Tagesbefinden mittelt gleichgewichtet die vorhandenen Werte Schlafqualität, Energie, Motivation (alle invertiert), Müdigkeit und Muskelkater. Fehlende Antworten niemals als0/Mitte ergänzen, keine Sliderdefaults aus aktuellen Einstellungen nachträglich erfinden. Es ist eine subjektive Darstellung, keine medizinische HRV-/Readinessberechnung.

Fünf gleich breite Belastungsintervalle0–<2,2–<4,4–<6,6–<8,8–10 entsprechen Z3–Z7. Training behält die vier gespeicherten Kategorien:0kein Training grau,1leicht grün,2mittel orange,3schwer rot. Fehlende Angabe ebenfalls grau, im Datenmodell weiterhin von explizitem0getrennt. Training aus einer Tagesfrage wird dem Vortag zugeordnet, auf den sich die Frage ausdrücklich bezieht.

Woche/Monat mitteln die tatsächlich verfügbaren Kontextwerte im echten Kalenderzeitraum; Training wird danach auf die nächste vorhandene Kategorie gerundet. Auch Antworttage ohne HRV-Messung werden einbezogen. Keine Nullfüllung, keine Gewichtung nach Anzahl der Messungen. Farben werden außerhalb von onDraw vorbereitet und je Auflösung/Modus gecacht.

HrvController.trendsWithContext lädt Tageswerte und kanonische Tagesantworten auf dem vorhandenen Speicherworker. HrvStore.trendQuestionnaires berücksichtigt Antworttage ohne Messung und vorhandene Legacyantwortmigration. Keine neue Datei, Datenbank, RR-Neuberechnung oder Backupformatänderung. Beim Zurückkehren aus dem Tagesfragebogen wird Kontext neu geladen und die bisherige Tagesauswahl erhalten.


Veröffentlichung28.10.2026: Diese Änderungen sind in **APK0.8.59/Build100** enthalten. [Buildnachweis und Download](build-100.md). Frühere Aussagen zu fehlender neuer APK sind Umsetzungshistorie.


Korrektur nach Build100: Ältere HRV4-Importe konnten Training, aber noch keine bzw. nur einzelne später unterstützte Sliderantworten enthalten. Der bisherige Kontextlader migrierte nur komplette Legacydateien, wenn noch keine kanonischen Tagesantworten existierten. Anders als der Fragebogen rief er die bestehende Ergänzung aus external.originalColumns nicht auf. trendQuestionnaires verwendet nun auch bei bereits vorhandenen Tagesantworten den fill-missing-only-Resolver questionnaireForEditing für importierte Datensätze. Damit profitieren Farbansicht und Tagesauswertung gemeinsam, ohne Fragebogenöffnung/Neuimport. Vorhandene Werte/Notizen/Zeitstempel und Original-CSV-Felder bleiben erhalten; explizite0nicht als fehlend behandeln.


Veröffentlichung28.10.2026: HRV4-Kontextkorrektur und aktualisierte Symbole/Reihenfolge in **APK0.8.60/Build101** enthalten. [Download und Buildnachweis](build-101.md).
