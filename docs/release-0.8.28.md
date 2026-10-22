# Version 0.8.28

- HRV-Diagramm 40 % höher (504 dp), linker Platz besser genutzt und Skalentexte erhalten.
- Feste Y-Achse je Kennzahl aus dem gesamten geladenen Verlauf einschließlich Korridor und Mittelwert. Puls mit fünf bpm Reserve, andere Kennzahlen mit etwa fünf Prozent Abstand; Zoom und Blättern verändern die Y-Skala nicht.
- Balken ACCENT, nur unter der vorhandenen unteren Normalbereichsgrenze weiß. 7-Tage-Mittel weiß, Korridor weiß mit 30 % Deckkraft und gepunkteten 1-dp-Grenzen.
- Ein gemeinsamer persönlicher Bestand aus B-Cycle und HRV4Training, ohne Neustart der Referenzhistorie bei Quellen-/Protokollwechsel. Pro Tag weiterhin nur die letzte Messung; fehlende oder abgelehnte Werte nicht erfinden. Unbekannte CSV-LF/HF-Einheiten bleiben Originalfelder.

Mittelwert/Korridor können bei ausreichenden Daten bis zur aktuellen Messung reichen. Keine Extrapolation und keine Wiederherstellung eines konkret fehlenden Messwerts. HRV-Messalgorithmus und gespeicherte Originaldateien unverändert.

Zehn gezielte Auswertungstests erfolgreich: Kalenderfenster/Mindestdaten, Quellenwechsel mit vorgestriger Lücke, gemeinsame Historie, feste Achsen, niedrige Werte, echte Pinch-/Wisch-/Tagesauswahl, Navigation und Streaming. Native Darstellung visuell geprüft; Typografie-/Atlasprüfung erfolgreich. Kein Gesamttestlauf oder Hardwaretest. [Details](hrv-trends.md).
