# Statistiken

Einstellungen → Statistiken, unmittelbar nach Strecken. Eigener Vollbild-Einstellungsdialog mit normalem Zurück-Verhalten und einem vertikalen Scrollbereich.

- Monatliche Kilometer nach lokalem Kalenderjahr; ein oder mehrere Jahre auswählbar. Gemeinsame lineare Skala und formtreue Kurven ohne Überschwingen, zwölf Monatsbeschriftungen. Jahresfarben aus der vorhandenen Zonenpalette. Jahresauswahl bei vielen Jahren horizontal scrollbar.
- Kilometer je ISO-Kalenderwoche des aktuellen Jahres; native horizontal scrollbare Balken mit Kalenderwochen und Kilometerwerten. Korrekte 52/53-Wochen-Jahre, auch an Jahresgrenzen: zum Beispiel gehört der 29.12.2025 zur KW1/2026, der 1.1.2027 zur KW53/2026.
- Tortengrafik der gesamten Fahrzeit je Fahrrad, über alle Jahre. Bewegungszeit, bei fehlendem/ungültigem Wert gespeicherte Fahrtdauer. Profilfarben aus demselben RideProfileCatalog wie die bestehende Fahrradzuordnung; Legende mit Name, Stunden und Prozentanteil. Ohne Zuordnung neutral. Gelöschte Profile behalten gespeicherte Identität/Name/Farbe, statt nach Slot-Wiederverwendung ungefragt neu zugeordnet zu werden.

Es zählen nur Einträge mit TrackOrigin.RECORDED: eigene Fahrten sowie ausdrücklich als gefahren markierte Importe, einschließlich Dreeve-Zusammenfassungen. Geplante/importierte Routen zählen nicht automatisch als gefahren. Fehlende Distanz/Zeit wird nicht geschätzt. Fahrten ohne Datum fließen nicht in Monats-/Wochenstatistiken ein, vorhandene Zeit kann trotzdem in die Radverteilung eingehen. Keine neuen FIT-/GPX-Lesevorgänge und keine Änderungen an Aufzeichnungen, Fahrtkennzahlen, Uploads oder Sicherungen.

## Cache und Aktualisierung

MapState verwendet den bereits vorhandenen Bibliotheksworker; keine zusätzliche dauerhafte Arbeitsqueue. `RideStatisticsCache` hält einen unveränderlichen aggregierten Snapshot im Speicher, auch nach Schließen der Seite. Nach App-Neustart wird er bei der ersten Anforderung einmal aufgebaut. Eingaben sind vorhandene Kurzkennzahlen, stabile Fahrradidentitäten, Namen/Farben, aktuelles Jahr und Zeitzone. Keine privaten Daten in öffentlichen Dateien.

Auf der UI-Seite werden bereits vorhandene Referenzen von Bibliothek/Profilkatalog sowie Jahr/Zone verglichen. Unveränderte Eingaben lösen keine neue Workeraufgabe aus. Nach Änderungen am Bestand/Katalog normalisiert der Worker die statistikrelevanten Felder; reine Umbenennung einer Fahrt oder Änderung der Kartensichtbarkeit baut die Aggregation nicht neu auf. Hinzufügen/Löschen, neue Distanz/Zeit/Datum, Fahrradzuordnung, Profilname/-farbe und Jahres-/Zeitzonenwechsel aktualisieren sie. Monats-Jahreswahl und Scrollen berechnen keine Fahrtstatistiken neu. Veraltete Workerantworten werden über Generationen verworfen, Callback beim Schließen entfernt. Der View zeichnet unveränderte Snapshots nicht neu auf und erhält dadurch Auswahl und Scrollposition.

## Prüfung

Gezielte Modellfälle: lokale Monate, ISO-Wochen/Jahresgrenzen, 53-Wochen-Jahr2026, Ausschluss geplanter Routen, Fahrzeit-Fallback, stabile Fahrradidentität/Profilfarbe, fehlende Werte und Wiederverwendung desselben Cache-Snapshots bei irrelevanten Änderungen. Native Darstellung in Flat und Glow: Jahresvergleich, horizontaler Wochen-Scrollbereich, tatsächliche Profilfarben in der Tortengrafik, Stunden-/Prozentlegende, unveränderter Snapshot. Menü-Reihenfolge und eigenständige Navigation mit Rückkehr zum Menü. Synthetische Daten, keine privaten Fahrten oder Cloudabfragen.
