# HRV-Balkenfarben nach Tageskontext

Unter der Zeitraumzeile des Diagramms stehen sechs gleich breite, nur mit Icons beschriftete Auswahlsegmente: Standard, Tagesbefinden, Training, Schlaf, Energie, Muskelkater. Symbole: Tagesbefinden `face-slightly-smiling`, Schlaf `bed`, Energie `biceps-flexed`, Muskelkater `target`; Standard und Training behalten ihre bisherigen Symbole. Der Name der ausgewählten Ansicht steht separat darunter. Keine Legende. Auswahl gilt temporär im geöffneten Screen, bleibt bei Kennzahl-/Zeitraumwechsel und Rückkehr aus Tagesbefinden erhalten. Kein neuer dauerhaft gespeicherter Einstellungswert.

Standard erhält bisherige HRV-Farbe (PRIMARY, unter persönlichem Bereich weiß). Kontextmodi ersetzen nur die Balkenfarbe; HRV-Werte, Kurve, Korridor, Achsen, Auswahlabdunklung und Zoom bleiben gleich. Diagrammhöhe454dp statt504dp (rund10% weniger), zentral für Tag/Woche/Monat. Alle Erklärungstexte und Tageswert-/Referenztextblöcke unter dem Diagramm entfernt. Tagestitel/Kennzahlenwerte oben und Balkenwerte bleiben. Fester Tagesfooter: Auswertung weiterhin deaktiviert, Messergebnis öffnet bisherige Ergebnisansicht, Tagesbefinden direkt die Tagesfragen, Löschen bestätigt die Messungslöschung. Kein zusätzlicher Inline-Ergebnisbutton.

## Farben und Rechenregel

Farben direkt aus TrainingZoneColors wie Leistungsdiagramm: Z3grün, Z4gelb, Z5orange, Z6rot, Z7violett. Ohne vorhandenen Wert Z1grau. Keine eigenen Farbkopien.

Schlaf und Energie verwenden jeweils den gespeicherten Sliderwert; Muskelkater den gespeicherten soreness-Wert. Zur gemeinsamen Skala0gut bis10belastet werden positive Schlaf-/Energiewerte invertiert, Muskelkater unverändert verwendet. Tagesbefinden mittelt gleichgewichtet die vorhandenen Werte Schlafqualität, Energie, Motivation (alle invertiert), Müdigkeit und Muskelkater. Fehlende Antworten niemals als0/Mitte ergänzen, keine Sliderdefaults aus aktuellen Einstellungen nachträglich erfinden. Es ist eine subjektive Darstellung, keine medizinische HRV-/Readinessberechnung.

Fünf gleich breite Belastungsintervalle0–<2,2–<4,4–<6,6–<8,8–10 entsprechen Z3–Z7. Training behält die vier gespeicherten Kategorien:0kein Training grau,1leicht grün,2mittel orange,3schwer rot. Fehlende Angabe ebenfalls grau, im Datenmodell weiterhin von explizitem0getrennt. Training aus einer Tagesfrage wird dem Vortag zugeordnet, auf den sich die Frage ausdrücklich bezieht.

Woche/Monat mitteln die tatsächlich verfügbaren Kontextwerte im echten Kalenderzeitraum; Training wird danach auf die nächste vorhandene Kategorie gerundet. Auch Antworttage ohne HRV-Messung werden einbezogen. Keine Nullfüllung, keine Gewichtung nach Anzahl der Messungen. Farben werden außerhalb von onDraw vorbereitet und je Auflösung/Modus gecacht.

HrvController.trendsWithContext lädt Tageswerte und kanonische Tagesantworten auf dem vorhandenen Speicherworker. HrvStore.trendQuestionnaires berücksichtigt Antworttage ohne Messung und vorhandene Legacyantwortmigration. Keine neue Datei, Datenbank, RR-Neuberechnung oder Backupformatänderung. Beim Zurückkehren aus dem Tagesfragebogen wird Kontext neu geladen und die bisherige Tagesauswahl erhalten.


Veröffentlichung28.10.2026: Diese Änderungen sind in **APK0.8.59/Build100** enthalten. [Buildnachweis und Download](build-100.md). Frühere Aussagen zu fehlender neuer APK sind Umsetzungshistorie.


Korrektur nach Build100: Ältere HRV4-Importe konnten Training, aber noch keine bzw. nur einzelne später unterstützte Sliderantworten enthalten. Der bisherige Kontextlader migrierte nur komplette Legacydateien, wenn noch keine kanonischen Tagesantworten existierten. Anders als der Fragebogen rief er die bestehende Ergänzung aus external.originalColumns nicht auf. trendQuestionnaires verwendet nun auch bei bereits vorhandenen Tagesantworten den fill-missing-only-Resolver questionnaireForEditing für importierte Datensätze. Damit profitieren Farbansicht und Tagesauswertung gemeinsam, ohne Fragebogenöffnung/Neuimport. Vorhandene Werte/Notizen/Zeitstempel und Original-CSV-Felder bleiben erhalten; explizite0nicht als fehlend behandeln.
