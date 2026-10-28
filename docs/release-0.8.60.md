# B|CYCLE 0.8.60

- HRV4Training: Fehlende Befindenswerte älterer Importe werden für Kontextfarben und Tagesauswertung aus den archivierten CSV-Feldern ergänzt, auch wenn Tagesantworten bereits teilweise vorhanden sind. Kein Neuimport oder vorheriges Öffnen des Fragebogens nötig. Vorhandene Antworten einschließlich expliziter Nullwerte, Notizen und Zeitstempel bleiben erhalten.
- HRV-Kontextauswahl: Standard · Tagesbefinden · Training · Schlaf · Energie · Muskelkater. Gewünschte Lucide-Symbole `face-slightly-smiling`, `bed`, `biceps-flexed`, `target`; Berechnungen und Farben unverändert.

Prüfung:26 gezielte Import-/Fragebogen-/Tagesauswertungsfälle sowie bestehende native Kontextvorschau bei320/360dp in Flat und Glow erfolgreich. Typografie, UI-Katalog und Diff geprüft. Keine Gesamtsuite und kein neuer Telefon-/Cloudtest.

Buildnachweis folgt nach Veröffentlichung.
