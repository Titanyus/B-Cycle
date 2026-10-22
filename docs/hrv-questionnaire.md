# Tagesbefinden

Erster ausprobierbarer Entwurf nach Build 61. Kein Erholungs-/Fitnessscore und keine Veränderung von HRV-Messwerten oder Qualitätsregeln. Angaben unterstützen spätere Zusammenhänge zwischen HRV, Befinden und Training; Langzeitdiagramme und Korrelationsauswertung sind weiterhin geplant, noch nicht implementiert.

## Einstieg und flexible Auswahl

Nach einer gespeicherten HRV-Messung erscheint unten „Tagesbefinden“. Derselbe Einstieg steht im Ergebnis einer älteren Messung in der Historie. Ein eigener Vollbilddialog mit SECONDARY-Header verwendet die bestehenden Komponenten, Gruppen und Abstände. Gültige Eingaben werden automatisch gespeichert; Speichernfehler bleiben mit Wiederholungsmöglichkeit sichtbar. Schließen stößt das Speichern noch ausstehender gültiger Änderungen an. Unten führt „Zur Auswertung“ vorerst zur vorherigen Ansicht zurück; der nächste Auswertungsscreen ist noch nicht implementiert. Der Button wartet auf erfolgreiche Speicherung, bei einem Fehler bleiben Formular und Eingaben offen.

Einstellungen → HRV-Messungen → Tagesbefinden ist ein Accordion. Darin wird jede Frage separat an-/abgewählt. Die Formularansicht wird daraus aufgebaut: keine Platzhalter für deaktivierte Fragen, leere Gruppen entfallen. Vorhandene Antworten bleiben bei Deaktivierung erhalten. Änderungen der Auswahl werden auch in einer noch geöffneten Ansicht berücksichtigt. Eigene Felder werden über einen nicht leeren Namen aktiviert; bis zu drei numerische Felder, mit Namenssnapshot in der Antwort, damit spätere Umbenennung die Bedeutung alter Daten nicht still verändert.

Gezeigte Kategorien auf Deutsch: Schlafqualität, Aufsteh-/Schlafenszeit; Training am Vortag, Trainingsphase, empfundene Anstrengung (RPE), Distanz, Dauer, Motivation; körperliche Verfassung, mentale Energie, Muskelkater, Müdigkeit, Verletzung mit Beschreibung, Menstruationszyklus; Regelmäßigkeit des Alltags, Reisen, Alkohol, Krankheit, Ernährung, Aufenthaltsort und Nahrungsergänzungsmittel; Notizen und eigene numerische Angaben. **Kein TSS-Feld**, entsprechend Nutzerkorrektur. Kein automatisches Erfinden nicht vorhandener Belastungswerte.

Fahrerprofil enthält optional Mann/Frau; alte Profile bleiben ohne vorgegebene Auswahl gültig. Nur bei Frau ist die Zyklusfrage in den HRV-Einstellungen aktivierbar und gegebenenfalls im Fragebogen sichtbar. Bei Mann oder fehlender Auswahl bleibt sie gesperrt. Frühere Antworten und die gespeicherte Fragenauswahl werden nicht gelöscht. Geschlecht verändert keine bestehenden Energie-/HRV-Berechnungen. Global personenbezogen, unabhängig vom gewählten Fahrradprofil.

## Antwortskalen

Qualitative Slider haben neun diskrete Positionen 0–8. Muskelkater und Müdigkeit stehen anfangs auf 0, die übrigen auf 4. Ihre ACCENT-Hervorhebung läuft vom jeweiligen Ursprung zur aktuellen Position, bei mittigem Ursprung nach links oder rechts. Labels je Frage Schlecht–Normal–Gut, Gar nicht–Mittel–Sehr, Wenig–Normal–Viel oder Unregelmäßig–Normal–Routine. Unberührt bedeutet **nicht beantwortet**. Auch eine Berührung ohne Verschieben kann den Mittelwert ausdrücklich bestätigen. „Angabe entfernen“ setzt die Antwort auf fehlend zurück. RPE bleibt als etablierte eigene Skala 0–10, anfangs 5, ebenfalls zunächst unbeantwortet. Keine stillen Nein-/Kein-Alkohol-Standardantworten. RPE zeigt keinen zusätzlichen sichtbaren „RPE x / 10“-Status; Zahlen bleiben für Accessibility vorhanden. Links „Gar nicht“. Aufsteh- und Schlafenszeit stehen bei gemeinsamer Aktivierung nebeneinander, ein einzelnes aktives Zeitfeld nutzt die Breite. Uhrzeiten HH:mm; Freitext und numerische Angaben optional.

## Fahrten des Vortags

Bezugstag ist der gespeicherte Messtag in der bei Messbeginn gespeicherten Zeitzone, nicht der Tag des späteren Öffnens. Vortag als Kalenderintervall von Mitternacht bis Mitternacht, auch bei Sommer-/Winterzeit. Aus vorhandenen Metadaten werden gefahrene Tracks über alle Fahrradprofile summiert, einschließlich als gefahren importierter GPX-/FIT-Tracks; geplante/nur importierte Tracks nicht. IDs werden entdoppelt. Keine FIT-Dekodierung oder Datei-IO im UI.

Trainingsdetails erscheinen bei gewähltem Training; ist die übergeordnete Trainingsfrage deaktiviert, können separat aktivierte Unterfelder direkt verwendet werden. Distanz und Dauer stehen nebeneinander; nur ein aktives Feld nutzt die gesamte Breite. Vorbelegung ist bearbeitbar, um anderes Training ergänzen zu können. Dauer verwendet Fahrtzeit, ersatzweise Gesamtdauer. Fehlende Kennzahlen bleiben leer statt eine unvollständige Summe als Gesamtwert auszugeben. Ohne verfügbare Fahrten manuelle Eingabe; bei noch ladender Bibliothek entsprechender Hinweis und später erneut öffnen. Herkunft „rides“/„manual“ und Ride-IDs werden mitgespeichert. Eine manuelle Änderung wird beim erneuten Öffnen nicht überschrieben. Bereits vorhandene ausgeblendete Angaben bleiben erhalten.

## Speicherung und Sicherung

`files/hrv-measurements/<Messungs-UUID>/questionnaire.json`, separat von unveränderter `result.json` und Rohpaketen. Schema 1 mit Messtag, optionalen Antworten, Trainingsmengen/Herkunft, benannten Zusatzwerten und Aktualisierungszeit. Pro Messung; mehrere Messungen am Tag können unterschiedliche Antworten besitzen. Produktziel ist eine tägliche Readiness-Messung; keine zusätzliche Tagesaggregation oder Mehrfachmessungsauswahl einführen.

Atomare Speicherung auf dem vorhandenen HRV-Worker; kurze Bündelung der UI-Änderungen. Tagesbefinden-Lade-/Schreibfehler verändern weder HRV-Ergebnis noch dessen Save-/Qualitätszustand. Die Fragenkonfiguration liegt separat und global unter `hrv-questionnaire-settings/config`; Einstellungssicherung in der bestehenden Profil-Sektion enthält sie sowie die neue optionale Geschlechtsangabe. Reine Einstellungssicherung enthält keine Antworten.

Vollständige Lokal-/WebDAV-Sicherung nimmt den optionalen `questionnaire`-Teil pro HRV-Messung auf und stellt ihn getrennt wieder her. Analyse-ZIP enthält ihn als eigenes Objekt neben `result` und `packets`. Alte Sicherungen ohne diesen Teil bleiben unverändert lesbar; identische Sicherungen sind wiederholbar. Abweichende schon vorhandene Sicherungen werden weiterhin nicht überschrieben, auch bei anderen Antworten. Validierung vor Anlage neuer Verzeichnisse; Messungsdatum muss passen, Skalen-/Enum-/Text-/Zahlgrenzen werden geprüft. Kein automatischer Dienstupload. Sichtbare IN-/OUT-Hinweise und Exportbeschreibung angepasst.

## Gezielte Prüfung

Zehn relevante Tests für Formular/Neutralantwort, Vorbelegung und manuelle Eingaben, dynamische Auswahl, eigene Felder, Geschlechtsfreigabe und Fahrerprofil-/Konfigurationssicherung, echten Ergebnisbutton/Worker-Speicherung, unveränderte Messungsbytes, alten/neuen HRV-Backup-Rundlauf und vollständige Verzeichnissicherung. Neue UI nativ gerendert. Kein vollständiger Gesamttestlauf, veröffentlicht in APK 0.8.23 / Build 62, kein neuer Geräte-/BLE-Test für diese Änderungen.

Sportart wird weder im Formular noch in der Fragenauswahl angeboten. Frühere Einstellungen mit SPORT werden kompatibel gelesen, beim Lesen und Sichern aber aus der aktiven Auswahl entfernt. Historische Antworten mit Sportart bleiben unverändert lesbar und gesichert. Details zum neuen Einstieg: [hrv-entry.md](hrv-entry.md).

## Vereinfachte Sliderdarstellung (22.10.2026)

Bei allen Tagesbefinden-Slidern entfallen die sichtbare Auswahl-/Unbeantwortet-Anzeige und die jeweilige Zurücksetzen-Aktion. Fragen, Slider und Skalenbegriffe bleiben sichtbar. Die Skalenbegriffe verwenden die zentrale Rolle BicycleComponents.sliderScaleLabel mit 50 % Deckkraft (BicycleDesign.sliderScaleLabelAlpha), appweit als Regel für Beschriftungen unter Slidern. Unberührte Standards bleiben fehlende Antworten; Bedienung, Speicherung, alte Antworten und Accessibility-Zustandsbeschreibung bleiben erhalten. Auch der HTML-Atlas folgt dieser Darstellung.

Slider-Skalenbegriffe: Abstand nach oben zentral sliderScaleLabelGap = controlGap / 2 (4 statt 8 dp). Keine nachträgliche normale Control-Margin auf diese Beschriftungsrolle anwenden; Touchhöhe des Sliders bleibt unverändert.

Slider-Fragen bilden eine Gruppe: Frage, 48-dp-Slider, Skalenbegriffe mit 4 dp Abstand/50 % Deckkraft. Danach 24 dp Gruppenabstand (sliderQuestionGap). Diese konkrete Gruppierung ist jetzt in Tagesbefinden und Atlas umgesetzt; der allgemeine Formular-Abstandsentwurf bleibt separat.
