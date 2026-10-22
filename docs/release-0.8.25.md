# Version 0.8.25

- HRV4Training-CSV unter Daten & Speicher → HRV-Pulsdaten importieren: schreibfreie Vorschau, Datumsformatprüfung und Dublettenschutz. Vorhandene Originalwerte und Tagesangaben bleiben erhalten; keine erfundenen RR-Rohdaten oder Messqualität. Details: [Import](hrv4training-import.md).
- Pro lokalem Kalendertag nur die letzte HRV-Messung anzeigen. Frühere Messungen mit Rohdaten und Antworten bleiben archiviert und im Export enthalten. Auch eine neuere abgelehnte Messung ersetzt die bisherige Tagesauswahl, ohne dadurch eine Qualitätsfreigabe zu erhalten. Details: [Tagesauswahl](hrv-daily-selection.md).
- Tagesbefinden: 32-dp-Abstand auch über optionale Trainingsgruppen hinweg; Distanz und Dauer als Platzhalter direkt in ihren Eingabefeldern.

Keine neue Score- oder Altersformel. HRV-Messung, Artefaktkorrektur und vorhandene Aufzeichnungen bleiben erhalten. Keine neuen externen Abhängigkeiten.

Gezielte Prüfungen: 16 HRV-Regressionstests für Tagesauswahl, Speicher, Controller, Historie und CSV-Import erfolgreich; Import und Formularabstände zusätzlich im vorherigen Arbeitsschritt geprüft. Zentrale Typografie und generierter UI-Atlas konsistent. Kein vollständiger Gesamttestlauf und kein Hardwaretest.
