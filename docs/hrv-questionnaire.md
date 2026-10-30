# Tagesbefinden

Erster ausprobierbarer Entwurf nach Build 61. Kein Erholungs-/Fitnessscore und keine Veränderung von HRV-Messwerten oder Qualitätsregeln. Angaben unterstützen spätere Zusammenhänge zwischen HRV, Befinden und Training; Langzeitdiagramme und Korrelationsauswertung sind weiterhin geplant, noch nicht implementiert.

## Einstieg und flexible Auswahl

Nach einer gespeicherten HRV-Messung erscheint unten „Tagesbefinden“. Derselbe Einstieg steht im Ergebnis einer älteren Messung in der Historie. Ein eigener Vollbilddialog mit SECONDARY-Header verwendet die bestehenden Komponenten, Gruppen und Abstände. Gültige Eingaben werden automatisch gespeichert; Speichernfehler bleiben mit Wiederholungsmöglichkeit sichtbar. Schließen stößt das Speichern noch ausstehender gültiger Änderungen an. Unten führt „Zur Auswertung“ zum vorhandenen HRV-Auswertungsbildschirm. Der Button wartet auf erfolgreiche Speicherung, bei einem Fehler bleiben Formular und Eingaben offen.

Einstellungen → HRV-Messungen → Tagesbefinden ist ein Accordion. Darin wird jede Frage separat an-/abgewählt. Die Formularansicht wird daraus aufgebaut: keine Platzhalter für deaktivierte Fragen, leere Gruppen entfallen. Vorhandene Antworten bleiben bei Deaktivierung erhalten. Änderungen der Auswahl werden auch in einer noch geöffneten Ansicht berücksichtigt. Eigene Felder werden über einen nicht leeren Namen aktiviert; bis zu drei numerische Felder, mit Namenssnapshot in der Antwort, damit spätere Umbenennung die Bedeutung alter Daten nicht still verändert.

Gezeigte Kategorien auf Deutsch: Schlafqualität, Aufsteh-/Schlafenszeit; Training am Vortag, Trainingsphase, empfundene Anstrengung (RPE), Distanz, Dauer, Motivation; körperliche Verfassung, mentale Energie, Muskelkater, Müdigkeit, Verletzung mit Beschreibung, Menstruationszyklus; Regelmäßigkeit des Alltags, Reisen, Alkohol, Krankheit, Ernährung, Aufenthaltsort und Nahrungsergänzungsmittel; Notizen und eigene numerische Angaben. **Kein TSS-Feld**, entsprechend Nutzerkorrektur. Kein automatisches Erfinden nicht vorhandener Belastungswerte.

Fahrerprofil enthält optional Mann/Frau; alte Profile bleiben ohne vorgegebene Auswahl gültig. Nur bei Frau ist die Zyklusfrage in den HRV-Einstellungen aktivierbar und gegebenenfalls im Fragebogen sichtbar. Bei Mann oder fehlender Auswahl bleibt sie gesperrt. Frühere Antworten und die gespeicherte Fragenauswahl werden nicht gelöscht. Geschlecht verändert keine bestehenden Energie-/HRV-Berechnungen. Global personenbezogen, unabhängig vom gewählten Fahrradprofil.

## Antwortskalen

Qualitative Slider haben elf diskrete Positionen 0–10. Muskelkater und Müdigkeit stehen anfangs auf 0, die übrigen auf 5. Ihre ACCENT-Hervorhebung läuft vom jeweiligen Ursprung zur aktuellen Position, bei mittigem Ursprung nach links oder rechts. Labels je Frage Schlecht–Normal–Gut, Gar nicht–Mittel–Sehr, Wenig–Normal–Viel oder Unregelmäßig–Normal–Routine. Sichtbare Slider übernehmen ihren angezeigten Standard als Antwort, auch ohne Berührung. Vorhandene Antworten gewinnen immer. Nicht sichtbare bzw. deaktivierte Fragen erhalten keine neuen Standardantworten. Ausgeblendete Fragen löschen keine Antworten. RPE bleibt als etablierte eigene Skala 0–10, anfangs 5, ebenfalls ohne zusätzliche Bestätigung gültig. Keine stillen Nein-/Kein-Alkohol-Standardantworten. RPE zeigt keinen zusätzlichen sichtbaren „RPE x / 10“-Status; Zahlen bleiben für Accessibility vorhanden. Links „Gar nicht“. Schlafdauer, Einschlaf- und Aufwachzeit stehen jeweils in einer eigenen Zeile: Titel links, gleich breites Zeitfeld rechts. Uhrzeiten und Schlafdauer verwenden die gemeinsame Zeitmaske; Freitext und numerische Angaben bleiben optional.

## Fahrten des Vortags

Bezugstag ist der gespeicherte Messtag in der bei Messbeginn gespeicherten Zeitzone, nicht der Tag des späteren Öffnens. Vortag als Kalenderintervall von Mitternacht bis Mitternacht, auch bei Sommer-/Winterzeit. Aus vorhandenen Metadaten werden gefahrene Tracks über alle Fahrradprofile summiert, einschließlich als gefahren importierter GPX-/FIT-Tracks; geplante/nur importierte Tracks nicht. IDs werden entdoppelt. Keine FIT-Dekodierung oder Datei-IO im UI.

Trainingsdetails erscheinen bei gewähltem Training; ist die übergeordnete Trainingsfrage deaktiviert, können separat aktivierte Unterfelder direkt verwendet werden. Distanz und Dauer stehen nebeneinander; nur ein aktives Feld nutzt die gesamte Breite. Vorbelegung ist bearbeitbar, um anderes Training ergänzen zu können. Dauer verwendet Fahrtzeit, ersatzweise Gesamtdauer. Fehlende Kennzahlen bleiben leer statt eine unvollständige Summe als Gesamtwert auszugeben. Ohne verfügbare Fahrten manuelle Eingabe; bei noch ladender Bibliothek entsprechender Hinweis und später erneut öffnen. Herkunft „rides“/„manual“ und Ride-IDs werden mitgespeichert. Eine manuelle Änderung wird beim erneuten Öffnen nicht überschrieben. Bereits vorhandene ausgeblendete Angaben bleiben erhalten.

## Speicherung und Sicherung

`files/hrv-measurements/days/<JJJJ-MM-TT>.json` ist der gemeinsame personenbezogene Datensatz je gespeichertem lokalem Kalendertag. Jede Messung desselben Tages liest und bearbeitet dieselben Antworten. HRV-Messwerte, letzte Tagesmessung, Rohpakete und `result.json` bleiben unverändert. Die 04:00-Uhr-Regel gilt ausschließlich für das tägliche Dashboard-Angebot, nicht für diese fachliche Tageszuordnung.

Alte messungsbezogene `questionnaire.json` werden beim ersten Zugriff des Tages deterministisch zusammengeführt: spätere Antwort-Aktualisierung gewinnt, bei Gleichstand spätere Messzeit; fehlende Antworten werden aus älteren Angaben erhalten. Die alten Dateien bleiben unverändert. Nach der Übernahme ist ausschließlich die Tagesdatei maßgeblich; absichtlich geleerte Antworten werden nicht erneut aus Altdateien eingesetzt. Das Löschen einer Messung löscht den gemeinsamen Tagesdatensatz nicht.
Atomare Speicherung auf dem vorhandenen HRV-Worker; kurze Bündelung der UI-Änderungen. Tagesbefinden-Lade-/Schreibfehler verändern weder HRV-Ergebnis noch dessen Save-/Qualitätszustand. Die Fragenkonfiguration liegt separat und global unter `hrv-questionnaire-settings/config`; Einstellungssicherung in der bestehenden Profil-Sektion enthält sie sowie die neue optionale Geschlechtsangabe. Reine Einstellungssicherung enthält keine Antworten.

Vollständige Lokal-/WebDAV-Sicherung nimmt den gemeinsamen `questionnaire`-Teil mit `questionnaireScope=day` pro HRV-Messung auf und stellt ihn getrennt wieder her. Analyse-ZIP enthält ihn neben `result` und `packets` mit derselben Kennzeichnung. Tagesdaten ohne verbleibende Messung erhalten eine zusätzliche `hrv-day-<Datum>.json` in der Verzeichnissicherung bzw. `day-<Datum>.json` im Analyse-ZIP (`bicycle-hrv-day-backup`, Schema 1). Ältere Backups ohne Scope behalten ihre Altdatei für die gemeinsame Migration. Aktuelle Tagesantworten werden bei Restore nur durch eine neuere Antwort-Aktualisierung ersetzt; gleiche/ältere Sicherungen überschreiben sie nicht. Alte Sicherungen ohne diesen Teil bleiben unverändert lesbar; identische Sicherungen sind wiederholbar. Abweichende schon vorhandene Sicherungen werden weiterhin nicht überschrieben, auch bei anderen Antworten. Validierung vor Anlage neuer Verzeichnisse; Messungsdatum muss passen, Skalen-/Enum-/Text-/Zahlgrenzen werden geprüft. Kein automatischer Dienstupload. Sichtbare IN-/OUT-Hinweise und Exportbeschreibung angepasst.

## Gezielte Prüfung

Zehn relevante Tests für Formular/Neutralantwort, Vorbelegung und manuelle Eingaben, dynamische Auswahl, eigene Felder, Geschlechtsfreigabe und Fahrerprofil-/Konfigurationssicherung, echten Ergebnisbutton/Worker-Speicherung, unveränderte Messungsbytes, alten/neuen HRV-Backup-Rundlauf und vollständige Verzeichnissicherung. Neue UI nativ gerendert. Kein vollständiger Gesamttestlauf, veröffentlicht in APK 0.8.23 / Build 62, kein neuer Geräte-/BLE-Test für diese Änderungen.

Sportart wird weder im Formular noch in der Fragenauswahl angeboten. Frühere Einstellungen mit SPORT werden kompatibel gelesen, beim Lesen und Sichern aber aus der aktiven Auswahl entfernt. Historische Antworten mit Sportart bleiben unverändert lesbar und gesichert. Details zum neuen Einstieg: [hrv-entry.md](hrv-entry.md).

## Vereinfachte Sliderdarstellung (22.10.2026)

Bei allen Tagesbefinden-Slidern entfallen die sichtbare Auswahl-/Unbeantwortet-Anzeige und die jeweilige Zurücksetzen-Aktion. Fragen, Slider und Skalenbegriffe bleiben sichtbar. Die Skalenbegriffe verwenden die zentrale Rolle BicycleComponents.sliderScaleLabel mit 50 % Deckkraft (BicycleDesign.sliderScaleLabelAlpha), appweit als Regel für Beschriftungen unter Slidern. Unberührte Standards bleiben fehlende Antworten; Bedienung, Speicherung, alte Antworten und Accessibility-Zustandsbeschreibung bleiben erhalten. Auch der HTML-Atlas folgt dieser Darstellung.

Slider-Skalenbegriffe: Abstand nach oben zentral sliderScaleLabelGap = controlGap / 2 (4 statt 8 dp). Keine nachträgliche normale Control-Margin auf diese Beschriftungsrolle anwenden; Touchhöhe des Sliders bleibt unverändert.

Slider-Fragen bilden eine Gruppe: Frage, 48-dp-Slider, Skalenbegriffe mit 4 dp Abstand/50 % Deckkraft. Danach 24 dp Gruppenabstand (sliderQuestionGap). Diese konkrete Gruppierung ist jetzt in Tagesbefinden und Atlas umgesetzt; der allgemeine Formular-Abstandsentwurf bleibt separat.

## Gemischte Trainingsgruppen nach Build 63

Die 32-dp-Gruppenregel gilt auch vor RPE und Motivation, unabhängig davon, ob Trainingphase, Trainingsdetails, RPE oder Mengenfelder aktiviert/sichtbar sind. Zuvor war nur die untere Margin des Sliders gesetzt. Die gemeinsame BicycleForm.sliderQuestionSpacing-Regel berücksichtigt nun die vorherige sichtbare Gruppe über Wrapper hinweg und zieht vorhandene Nachlaufabstände ab. Dynamischer Neuaufbau wendet dieselbe Regel erneut an; zwei Slider bekommen keine doppelten 64 dp.

Distanz und Dauer stehen weiter nebeneinander, jetzt ohne äußere Textzeilen. Leere Felder zeigen „Distanz · km“ beziehungsweise „Dauer · min“ als Platzhalter mit 50 % Deckkraft; zugängliche Beschriftung bleibt auch bei gefüllten Feldern erhalten. 8 dp trennen den Vortagshinweis von der Eingabezeile. Werte, Vorbefüllung, unbekannte Mengen, Validierung und Speicherung unverändert. Kein neuer APK-Auftrag für diese Korrektur.

Prüfung dieser Korrektur: 7 gezielte bestehende/ergänzte Tagesbefinden-Tests erfolgreich, einschließlich optionaler Trainingphase, ein-/ausgeblendeter RPE-/Mengenfelder, exakt 32 dp ohne Doppelabstand, Platzhalter, manueller Eingabe und vorhandener Vorbefüllung/Backup. Browserprüfung des aktualisierten Atlas erfolgreich (Platzhalter, 8-dp-Hinweisabstand und bisherige Regler/320–412-Breiten). Native Ansicht visuell geprüft. Kein Gesamtlauf und keine neue APK.

## Elf Stufen und HRV4-Antworten nach Build 65

Neue Fragebögen verwenden Schema 2 (Ratings 0–10). Schema 1 bleibt lesbar: Qualitätsantworten 0–8 werden genau einmal proportional mit kaufmännischer Rundung auf 0–10 übertragen, RPE bleibt unverändert. Fehlende Antworten bleiben fehlend. Die alten Qualitätswerte bleiben in legacyRatings (Originalskala 0–8) erhalten; Ergebnisdateien und alte Antwortdateien werden beim reinen Lesen nicht umgeschrieben. Speichern/Backup/Export verwenden Schema 2, Restore akzeptiert beide Versionen. Eine ältere App kann neue Schema-2-Antworten nicht lesen; nicht ohne Datensicherung downgraden.

HRV4-Zuordnung: sleep_quality → sleep, trainingMotivation → motivation, physical_condition → condition, mental_energy → energy, muscle_soreness → soreness, fatigue → fatigue, lifestyle → lifestyle, trainingRPE → rpe. Gleiche Richtung: höher bedeutet bei Schlaf/Motivation/Zustand/Energie/Routine mehr davon, bei Muskelkater/Müdigkeit/Anstrengung mehr Belastung. Endpunkte nicht invertieren. Unterstützter Quellbereich 0–10; endliche Komma-/Punktwerte werden auf die nächste ganze Zahl gerundet, .5 nach oben. Unbekannte oder außerhalb liegende Werte bleiben als Originalfeld erhalten und werden nicht geklemmt. Keine dateiabhängige Min-/Max-Normalisierung.

Bei vorhandenen HRV4-Importen ergänzt questionnaireForEditing auf dem HRV-Worker beim Öffnen des Tagesbefindens ausschließlich fehlende Ratings aus archivierten originalColumns; bestehende Antworten, Notizen und Zeitstempel erhalten. Atomare Speicherung nur bei tatsächlicher Ergänzung. Neue CSV-Inhalte können vorhandene Antworten/Originale beim erneuten Import nicht überschreiben. Keine automatische Änderung der aktiven Fragen oder der HRV-Messwerte. Noch keine neue APK für diese Ergänzung.

Prüfstand dieser Ergänzung: 21 gezielte Tests erfolgreich (drei neue Rundungs-/Migration-/Ergänzungsprüfungen, neun CSV-Importtests, sieben Tagesbefinden-Tests, bestehende Slider-/Speicherfreigabeprüfung und native Auswertungsdarstellung). Korridor nativ visuell geprüft; Typografie und generierter Atlas konsistent. Kein Gesamtlauf und keine neue APK.

Veröffentlichung der Elf-Stufen-/Antwortimport-/Korridor-Erweiterung: in APK **0.8.27 / Build 66** enthalten. [Buildnachweis](build-66.md). Frühere Angaben zu fehlender APK sind Verlauf.

## Schlafoptionen nach Build 67

Aufsteh- und Schlafenszeit haben in den HRV-Einstellungen einen gemeinsamen Auswahlpunkt. Persistiert bleiben WAKE/BED für vorhandene Sicherungen lesbar; sobald eine alte Einzelzeit aktiv war, wird die Kombination beim Lesen/Schreiben vereinheitlicht. Abwählen entfernt beide Fragen, vorhandene Zeitantworten bleiben erhalten. Neue unabhängige Option SLEEP_DURATION, standardmäßig aus: Schlafdauer als optionale Dezimalstunden (z. B. 7,5), Eingabebereich 0–24. Beide Wege können nach Bedarf aktiviert werden; keine automatische Ableitung von Schlafdauer aus Bett-/Aufstehzeit.

Dauer in extras.sleepHours als Stunden, in Fragebogen-/Analyseexport und vollständiger Sicherung enthalten. Frageauswahl im bisherigen Einstellungsbackup. HRV4 sleep_time wird in Stunden übernommen und beim Öffnen bestehender Importe nur ergänzt, wenn noch keine eigene Dauer hinterlegt ist. Originalfeld bleibt erhalten.

Prüfstand Schlafoptionen/globale Auswahlabstände: 15 gezielte Tests erfolgreich (drei Schlafprüfungen, sieben bestehende Tagesbefinden-Tests, bestehende Slider-/Speicherfreigabe, globale Auswahlfarben/Geometrie sowie drei Skalen-/Import-/Backup-Prüfungen). Typografie und generierter Atlas konsistent.

Schlafoptionen und globaler Auswahlabstand sind in APK **0.8.29 / Build 68** enthalten. [Buildnachweis](build-68.md).

## Eingabemasken ab 24.10.2026

Gemeinsame `BicycleTimeInput` für Stunden-/Minutenangaben: im Feld ausschließlich `00:00 h`. Titel Schlafdauer, Eingeschlafen und Aufgewacht stehen außerhalb links, das Eingabefeld rechts; drei Zeilen untereinander über `BicycleForm.inputRow`. Fester Doppelpunkt, noch leere Ziffernplätze und Einheit verwenden die zentrale 50-%-Hintfarbe; eingegebene Ziffern die Eingabefarbe. Zifferneingabe überspringt den Doppelpunkt automatisch. Ersetzen, Löschen und Einfügen bleiben möglich, Dekoration wird nicht als Antwort gespeichert. Unvollständige/ungültige Eingaben überschreiben keinen gültigen alten Wert. Leeren aller Ziffern entfernt die Angabe. Uhrzeit 00–23:00–59; Schlafdauer 00:00–24:00. Schlafdauer bleibt kompatibel in `extras.sleepHours` als Stundenwert, Uhrzeiten als `HH:mm`; keine neuen Frage-/Backup-Einstellungen.

Trainingsmengen bleiben Zahlenfelder nebeneinander mit optionalen Werten und dauerhaft geghosteten Einheiten unmittelbar nach dem Wert (`80 km`, `160 min`). Auch die Stunden-/Minuten-Grenzen im Streckenfilter benutzen die Zeitmaske, ohne die Bereichs-/Sliderfunktion zu ändern.

Globale Auswahlkonvention: Fragenliste in den HRV-Einstellungen verwendet Schalter statt Auswahlpunkte, ohne Änderungen an Aktivierung, Mehrfachauswahl, Zyklussperre oder Sicherung. Maßeinheiten bei Trainingsmengen werden über `BicycleInputUnit` gezeichnet; der editierbare/gespeicherte Text bleibt rein numerisch. Datumsspeicherung, Sliderwerte und 04:00-Uhr-Angebot bleiben unverändert.

## Automatische Trainingsvorauswahl30.10.2026

Beim Öffnen werden vorhandene, als Gefahren markierte Fahrten des vorherigen Kalendertags über alle Fahrradprofile berücksichtigt. Zeitgrenzen folgen der gespeicherten Messzeitzone, einschließlich Sommerzeitwechsel. Reine importierte/geplante Strecken zählen nicht automatisch als gefahren. Vorhandene Tagesantworten und manuelle Werte gewinnen; eine weitere Messung desselben Tages darf sie nicht neu einstufen.

Die vorhandene MapState-Workerpipeline liest die Original-/vollständigen privaten FIT-Analysedaten bzw. GPX/JSON-Zusammenfassungen. Verfügbare Pulswerte werden zuerst innerhalb der Fahrt anhand der tatsächlich aufgezeichneten gültigen Intervalle zeitgewichtet, dann nach erfasster Pulsdauer über die Fahrten gemittelt. Der Histogramm-Mittelwert wird vor der Darstellungsrasterung berechnet, keine ausgedünnte Diagrammkurve verwenden. Keine Überbrückung von Sensorausfällen oder Timerpausen. Für reine Zusammenfassungen können vorhandener Durchschnittspuls und Fahrtdauer als Gewicht dienen.

Die aktuellen fünf Fahrer-Pulszonen geben die Vorauswahl vor: Z1 leicht, Z2/3 moderat, Z4/5 schwer. Kein Trainingsnachweis → Kein Training; bei nachgewiesener Fahrt ohne auswertbaren Puls/definierte Zone wird keine Intensität geraten. Kurze Spitzen bestimmen nicht allein die Einstufung. Dies ist eine editierbare Beschreibung des Vortags, keine medizinische Belastungsbewertung und kein neuer HRV-Score. Keine Rohwerte/FIT ändern.

Die subjektive Anstrengungsfrage RPE erscheint nur bei erkannter Fahrt oder manuell positiver Trainingsstufe. Das gilt auch, wenn die separate Trainingsfrage deaktiviert ist: ohne Beleg kein unberührter Standard-RPE. Bei erkannter Fahrt ohne Puls kann RPE sichtbar sein, während die Intensitätsauswahl noch offen bleibt. Vorhandene manuelle Distanz-/Dauerfelder bei deaktivierter Trainingsfrage bleiben nutzbar.

Explizites „Kein Training“ entfernt Kilometer, Minuten, Ride-IDs, Herkunft und RPE aus der Tagesantwort und blendet Trainingsdetails aus; diese versteckten Werte dürfen nicht in eine spätere Auswertung eingehen. Die ursprünglichen Fahrten bleiben erhalten. Bei erneuter positiver Auswahl können vorhandene Vortagsfahrten wieder zur Vorbelegung dienen. Andere Tagesantworten bleiben unangetastet.

Prüfung: HrvTrainingSuggestionTest5 (alle Zonen, Pulsdauergewichtung, fehlende Daten, echte FIT→MapState-Worker→Vorauswahl, unveränderte Datei/Karte, manuelle Vorränge, kein Training, bedingte RPE). Bestehende HrvQuestionnaireTest8, HrvDailyQuestionnaireTest5, RouteValueDistributionTest5, RouteZoneDistributionTest4 und HrvDayFooterTest2 grün. Keine Gesamtsuite und kein physischer Geräteversuch.
