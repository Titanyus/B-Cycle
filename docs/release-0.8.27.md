# Version 0.8.27

- Tagesbefinden mit elf Sliderstufen 0–10. Neutral bleibt anfangs unbeantwortet in der Mitte (5); Muskelkater und Müdigkeit beginnen bei 0.
- Passende HRV4Training-Antworten werden auf ganze Zahlen gerundet, Originalwerte bleiben exakt erhalten. Beim Öffnen bestehender importierter Tagesbefinden werden ausschließlich fehlende Ratings aus dem archivierten Original ergänzt; manuelle Antworten bleiben geschützt.
- Alte Qualitätsantworten der Skala 0–8 werden proportional auf 0–10 übertragen; Originalwerte bleiben in legacyRatings erhalten. Fragebogen-Schema 2, vorhandenes Schema 1 weiter lesbar. HRV-Ergebnisdateien und Messalgorithmus unverändert.
- Normalbereich im HRV-Diagramm weiß mit 40 % Deckkraft, oben und unten gepunktete weiße Begrenzungen von je 1 dp. Statistikfenster und Berechnung bleiben erhalten.

[Tagesbefinden und Migration](hrv-questionnaire.md) · [CSV-Import](hrv4training-import.md) · [HRV-Auswertung](hrv-trends.md).

21 gezielte Tests erfolgreich: Rundung, Skalenmigration, Ergänzung ohne Überschreiben, Original-/Backup-Erhalt, CSV-Import, Tagesbefinden, Speicherfreigabe und native Diagrammdarstellung. Korridor visuell geprüft; Typografie und UI-Atlas konsistent. Kein Gesamttestlauf oder Hardwaretest.
