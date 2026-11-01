# Vorgemerkt: eigene PNS-/Aktivierungsindikatoren – 01.11.2026

Nutzer wünscht nachvollziehbare eigene Werte für parasympathische und sympathische Regulation, um Stress/Belastung einschätzen zu können. Kubios muss ausdrücklich nicht exakt nachgebildet werden. In den vorgemerkten HRV-/Tagesscreen-Umbau aufnehmen; Gestaltung und konkrete eigene Formel noch festlegen, aktuell keine Berechnung/Empfehlung oder UI eigenmächtig umstellen.

## Quellen und Grenzen

[Kubios: PNS/SNS](https://www.kubios.com/blog/hrv-ans-function/), am01.11.2026 gelesen: PNS aus mittleremRR, RMSSD, normalisiertemSD1; SNS aus mittlererHF, Baevsky-Stressindex, normalisiertemSD2. Einzelgrößen gegen Ruhe-Populationsnormen standardisiert; Gewichtung proprietär. Keine Behauptung, ein bloßes arithmetisches Mittel der z-Werte sei die Kubios-Formel. ±2 ist typischer Darstellungs-/Ruhebereich, keine harte Grenze; SNS kann laut Kubios bei Belastung deutlich darüber liegen. [Analyseverfahren](https://www.kubios.com/blog/hrv-analysis-methods/) und [Handbuch v3.5](https://www.kubios.com/downloads/Kubios_HRV_Users_Guide.pdf): dortiger Stressindex ist die Quadratwurzel des Baevsky-Index; Normgrundlagen und Aufbereitung sind zusätzlich relevant.

HRV liefert Hinweise auf kardiale autonome Regulation. Parasympathische/vagale Modulation ist besser aus kurzfristiger HRV zugänglich als isolierte Sympathikusaktivität. Einen eigenen SNS-Wert daher als Aktivierungsindikator erklären, keine direkte Nervenmessung und keinen validierten psychischen Stressmesser behaupten. Auch Training, Infekt, Wärme, Flüssigkeitsmangel, Atmung und Messbedingungen können dieselben Größen verändern. PNS/SNS nicht als unabhängige direkte Messungen behandeln; eine belastbare Stressklassifikation erst mit geeigneter Validierung beanspruchen.

## Vorschlag für die Ausgestaltung – noch keine festgelegte Formel

- Persönliche Referenz aus ausreichend vielen vergleichbaren Ruhemessungen, gleiche Datenqualität/Protokolle berücksichtigen. Referenzniveau0, positive/negative Abweichungen; nicht künstlich auf−2…+2 begrenzen.
- PNS-Indikator primär aus kurzfristiger HRV (insbesondere lnRMSSD), weitere Größen nur bei begründetem Mehrwert.
- SNS-/Aktivierungsindikator aus Ruhepuls und gegebenenfalls einem aus echten RR-Daten berechneten Baevsky-Stressmaß; genaue Aufbereitung, Normierung und Kombination vor Implementierung explizit definieren.
- Messqualität und fehlende Referenzwerte sichtbar berücksichtigen. Keine Ersatz-RR-Reihen aus aggregierten Importwerten erfinden. Vorhandene subjektive Stressangabe als Kontext nutzbar, nicht als Beleg einer eindeutigen HRV-Ursache ausgeben.
- Nachvollziehbare Formeln und Begründungen dokumentieren; bestehende Trainingsempfehlungen ohne gesonderten Auftrag nicht umstellen.

Aktueller Datenbestand: HrvStore speichert meanNnMs/RMSSD/lnRMSSD/SDNN/Puls plus originale BLE-Pakete in capture.jsonl für eigene Messungen. Metrics berechnet diese aus der bereinigten zusammenhängenden NN-Reihe. Aggregierte HRV4-Importe liefern nicht automatisch RR-Rohdaten und damit keinen Baevsky-Index.
