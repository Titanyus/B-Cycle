# B|CYCLE 0.8.33

- HRV-Auswertung: Datumsbeschriftung an jedem Montag mit senkrechtem Trennstrich davor. Datum bleibt fest an der Kalenderposition des Balkens und scrollt gleichmäßig mit.
- Beschriftungen an beiden Rändern der Auswertung und des RR-Messdiagramms direkt abgeschnitten; kein Festhalten am Rand oder Zurückspringen.
- Native Scrollträgheit: nach dem Loslassen gleitet die Auswertung weiter und bremst weich ab. Neue Berührung stoppt das Nachgleiten, Pinch-Zoom und Tagesauswahl bleiben erhalten.
- Messdaten, feste Y-Skalierung und Statistik unverändert.

Sieben gezielte native Tests erfolgreich: Kalenderverankerung/Clipping, Messsekunden, Nachgleiten/Stoppen, bestehender Zoom/Tagesauswahl und Teilbalken. Native Vorschau visuell geprüft; Typografie-/Atlasprüfung erfolgreich. Kein Gesamtlauf oder Hardwaretest. [Diagrammverhalten](hrv-trends.md).
