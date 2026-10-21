# B|CYCLE 0.8.23

- **Tagesbefinden:** eigener optionaler Fragebogen nach gespeicherter HRV-Messung und in historischen Ergebnissen. Flexible deutsche Gruppen, neun Stufen bei Qualitätsfragen, unberührte Mitte bleibt unbeantwortet, RPE separat 0–10. Automatische Speicherung getrennt von Messwerten.
- **Fragenauswahl:** Accordion unter HRV-Messungen zum Aktivieren einzelner Fragen sowie bis zu drei benennbare numerische Zusatzfelder. Leere Gruppen entfallen; ausgeblendete Antworten bleiben erhalten. Kein TSS.
- **Training:** gefahrene Strecken vom Vortag in der gespeicherten Messungszeitzone aufsummiert; Distanz und Fahrtzeit nebeneinander, manuell bearbeitbar. Fehlende Werte bleiben leer. Auch als gefahren importierte Tracks und alle Fahrradprofile berücksichtigt.
- **Fahrerprofil:** optionale Auswahl Mann/Frau. Zyklusfrage nur bei Frau aktivierbar; alte Profile bleiben kompatibel. Keine Änderungen an Energie-, Zonen- oder HRV-Berechnungen.
- **Sicherung:** Fragenauswahl/Geschlecht in Einstellungssicherung, Antworten in vollständiger Lokal-/WebDAV-Sicherung und HRV-Analyse-JSON. Originale HRV-Ergebnisdateien bleiben unverändert.
- **HRV-Messansicht:** Glow-Spitze 45 % statt 30 %, bpm vertikal zentriert, Start/Erneut messen in 900 Black mit 1-dp-Pillenrahmen. Grafische Lückenerkennung berücksichtigt RR-Intervall vor Bewertung zusätzlicher fehlender Zeit; echte längere Lücken bleiben sichtbar.
- **Dashboard:** organische, formtreue kubische Kurven ohne neue Extremwerte zwischen dargestellten Punkten. Linie/Füllung/Halo und Voll-/30-/60-/120-s-Fenster konsistent; Datenlücken erhalten, Aufzeichnungen unverändert.

15 gezielte Tests für Tagesbefinden, Profil-/Konfigurationssicherung, Originaldaten und Backup-Rundlauf sowie Kurvengeometrie und native Darstellung erfolgreich. Zuvor vier gezielte Prüfungen der HRV-Puls-/Graphkorrekturen erfolgreich. Echten Fragebogendialog und Graphen nativ gerendert und visuell geprüft. Kein vollständiger Gesamtlauf oder neuer Geräte-/BLE-Test. Keine neuen Abhängigkeiten. Noch kein neuer HRV-Score und keine Langzeitdiagramme/Korrelationsauswertung.

[Fragebogen](hrv-questionnaire.md) · [Dashboard-Graphen](live-graph-overlays.md) · [Optimierungsanalyse – Empfehlungen, noch nicht umgesetzt](optimization-review-2026-10-21.md).
