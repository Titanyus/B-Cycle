# Persönliche PNS- und Aktivierungsindikatoren

Am01.11.2026 vom Nutzer bestätigt und umgesetzt. Eigene beschreibende Indizes, keine Kubios-Kopie. Bestehende HRV-Tagesempfehlung und Annahmeregeln bleiben unverändert.

## Berechnung

- PNS: `(ln(RMSSD heute) − Mittelwert ln(RMSSD Referenz)) / Stichproben-Standardabweichung ln(RMSSD Referenz)`.
- Aktivierung: dieselbe Formel mit Ruhepuls in bpm als Eingangsgröße. Bewusst „Aktivierungsindex“, kein isolierter SNS-Messwert.
- Referenz: vorherige60 Kalendertage, mindestens14 gültige verfügbare Tageswerte je Messgröße. Heute und Zukunft ausgeschlossen. Tagesauswahl vor Qualitätsprüfung wie bisher; eine spätere fehlgeschlagene Messung lässt keinen früheren guten Wert wiederaufleben.
- Ausdrückliche Nutzerpräzisierung01.11.2026: genau eine persönliche Messhistorie. Eigene und passend importierte Werte zählen gemeinsam, wie beim bestehenden HRV-Korridor; keine neue Referenz bei Quellen-/Protokollwechsel. Importzuordnung/Validierung erfolgt an der Eingangsgrenze. Vergleichbare Messbedingungen bleiben für die Interpretation wichtig. Bestehende RMSSD-/Puls-Korridore für Empfehlung/Kreisbögen unverändert.
- Fehlende/nichtpositive/nichtendliche Messwerte, zu kurze Referenz oder praktisch fehlende Streuung ergeben keinen Index. Statt Strich erscheint rechts vom jeweiligen Indexnamen ein kompakter Grund: „noch XX Tage bis zum ersten Wert“, „Messwert fehlt“ oder „Referenz ohne Streuung“. Die Resttage zählen noch benötigte gültige Messtage, keine garantierten Kalendertage. Beide Balken bleiben erhalten. Beide Messgrößen unabhängig prüfen; fehlender Puls verhindert keinen gültigen PNS-Wert.
- Keine Gewichtung, künstliche Gegenrechnung oder Alterskorrektur. Kein Baevsky-Index und keine LF/HF-Balance. Aggregierte Importwerte benötigen keine erfundenen RR-Rohdaten.

0 bedeutet persönliche Basis; +1/−1 eine Standardabweichung darüber/darunter. Das Referenzfenster und die Mindestanzahl sind Produktentscheidungen, keine wissenschaftlich validierten Normgrenzen. ±1SD beschreibt persönliche Streuung, keine klinischen Grenzwerte. Werte bleiben rechnerisch unbegrenzt; nur die Balkenzeichnung wird auf−2…+2 begrenzt. Mittlere50% zeigen−1…+1, beidseits25% Puffer. Weiß/FOREGROUND ohne neue diagnostische Warnschwellen; die bestehende Empfehlung behält ihre bisherigen funktionalen Farben.

## Aussage und Grenzen

lnRMSSD eignet sich als Indikator kurzfristiger vagaler/kardialer parasympathischer Modulation, nicht als direkte Messung der Nervenaktivität. Ein höherer Ruhepuls kann sowohl stärkere sympathische Aktivierung als auch geringeren vagalen Einfluss und andere Ursachen abbilden. Daher keine Behauptung, zwei unabhängige autonome Zweige isoliert zu messen.

Training, Infekt, Schlaf, Temperatur, Flüssigkeit, Atmung und Messbedingungen können beide Größen verändern. Subjektive Stressfrage separat als Kontext behalten. Keine zuverlässige individuelle Diagnose psychischen Stresses oder einer Erkrankung aus einer Einzelmessung ableiten. Die wissenschaftliche Grundlage der Messgrößen validiert nicht automatisch unseren eigenen persönlichen z-Index.

Formeln und Grenzen auch in der In-App-Hilfe „Tagesauswertung“ erklärt. Auf dem Tagesscreen zuerst PNS, dann Aktivierung. Vorhandene HRV-Empfehlungen nicht durch die neuen Indizes ersetzen.

## Gelesene Fachquellen (01.11.2026)

1. [Laborde, Mosley & Thayer2017](https://doi.org/10.3389/fpsyg.2017.00213): HRV und kardiale vagale Regulation; RMSSD, Logarithmierung, Messbedingungen und Grenzen der LF/HF-Deutung.
2. [Shaffer & Ginsberg2017](https://doi.org/10.3389/fpubh.2017.00258): Kennwerte, Normen, Protokoll-/Dauerabhängigkeit und Kontext.
3. [Billman2013](https://doi.org/10.3389/fphys.2013.00026): LF/HF ist kein genaues Maß der sympathovagalen Balance; keine einfache reziproke Bilanz beider Zweige.
4. [Kim et al.2018](https://doi.org/10.30773/pi.2017.08.17): Zusammenhang HRV/Stress auf Studienebene; keine Validierung unseres persönlichen Index oder individueller Diagnosen.

Kubios-Vorarbeit: [ANS-Funktion](https://www.kubios.com/blog/hrv-ans-function/), [Analyseverfahren](https://www.kubios.com/blog/hrv-analysis-methods/) und [älteres Handbuch v3.5](https://www.kubios.com/downloads/Kubios_HRV_Users_Guide.pdf). Dort PNS aus mittleremRR, RMSSD und normalisiertemSD1, SNS aus mittlererHF, Baevsky-Stressmaß und normalisiertemSD2, standardisiert gegen Ruhe-Populationsnormen. Gewichtung proprietär. Das ist nicht unsere Formel; ±2 kein physiologischer Maximalwert.

Implementierung: `HrvAutonomicIndices`, angebunden auf dem bestehenden HRV-Speicherworker über `HrvDailyAssessment.from`. Keine Datei-/RR-Auswertung beim Zeichnen, kein zusätzlicher persistierter Score.


Nach Rückmeldung leerer eigener Indexwerte: Ursache war die zusätzliche Quellen-/Protokolltrennung, die für eigene Messungen die Importhistorie nicht mitzählte. Nutzer ausdrücklich für gemeinsame Referenz entschieden. Pro Index gültige frühere Tageswerte separat zählen (PNS braucht positive RMSSD, Aktivierung positiven Puls); fehlender heutiger Messwert hat keine irreführende Tageszählung. Mindestzahl14, Fenster60 Tage, Tagesauswahl und Formeln erhalten. Sechs gezielte Fälle erfolgreich in `hrv-shared-reference-hint-tests.log`: gemeinsame Importe/eigene Werte trotz Protokollwechsel, Referenzzählung, fehlende/konstante Werte, Qualitäts-/Letzte-Messung-Regel sowie Hinweistext/Anordnung auf schmalen Ansichten und native Tagesdarstellung. Kein Gesamtlauf oder Gerätetest.
