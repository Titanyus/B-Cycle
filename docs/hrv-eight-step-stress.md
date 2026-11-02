# Eine achtstufige Stressfrage (02.11.2026)

Nutzerentscheidung: separate Intervals-Vierfachfrage entfernen, bisherigen Alltags-/Regelmäßigkeits-Slider zum Stressindikator umbauen. Acht Stufen, links wenig und rechts extrem. Nutzer bestätigt explizit: bisher links/0 viel Stress, rechts/10 wenig Stress; alte Angaben umkehren. Nutzer hat die bisherige Frage bereits als Stress verstanden.

## Darstellung und Werte

Eine Frage **„Wie gestresst bist du?“** an der bisherigen Alltagsposition. Slider mit acht Positionen0–7, Beschriftung Wenig / Durchschnittlich / Extrem. Standard wie bei anderen sichtbaren Slidern gültig (hier3); vorherige/gespeicherte Antwort vorauswählen. Die getrennte Vierfachauswahl und ihr zusätzlicher Einstellungs-Eintrag entfallen. Persistierte Enum-IDs nicht verschoben: LIFESTYLE bezeichnet die verbleibende Frage; alte STRESS-Einstellung nur noch beim Lesen als Alias dafür übernehmen. Waren beide aus, bleibt die Frage aus.

Neue Antworten unter `ratings.stress`0–7. Für Intervals gilt genau `1 + Wert / 2` (Ganzzahldivision):0/1→1 wenig,2/3→2 durchschnittlich,4/5→3 sehr,6/7→4 extrem. Keine neue Normalisierung von RMSSD, SDNN oder Ruhepuls.

## Historie und Kompatibilität

Reihenfolge zur Bestimmung des Stresswertes:
1. Neuer expliziter achtstufiger Wert.
2. Bisheriger0–10-Regelmäßigkeitswert, umgekehrt und auf acht Stufen gerundet: `round((10 - lifestyle) × 7 / 10)`.
3. Falls kein Regelmäßigkeitswert vorhanden ist: alte direkte Vierfachantwort, auf0–7 verteilt; ihre Intervals-Stufe bleibt erhalten.

Originale `ratings.lifestyle`, frühere Vierfachantwort `stress` und vorhandene Legacywerte bleiben unverändert gespeichert. Lesen/Exportieren kehrt keine Datei destruktiv um; die Darstellung/Intervals-Zuordnung verwendet den berechneten Wert. Beim Bearbeiten eines alten Tages wird der angezeigte achtstufige Wert zusätzlich kanonisch gespeichert. Er hat danach Vorrang, sodass erneutes Öffnen keine zweite Umkehrung erzeugt. HRV4-Importe behalten den ursprünglichen lifestyle-Wert und werden über dieselbe Regel behandelt; im Importdialog bleibt die ursprüngliche Spaltenbedeutung benannt.

Fragebogen- und Fragenauswahl-Schema jetzt3; Schema1/2 werden weiter gelesen. Bestehende HRV-/Einstellungssicherung nutzt dieselben Decoder/Validatoren und erhält Originale samt neuem Wert. HRV-Score, PNS/Aktivierung, subjektiver Wohlbefindensmittelwert und Empfehlung werden durch diese Änderung nicht neu gewichtet. Automatische Intervals-Eigentumsregeln und ausdrücklicher aktualisierender Sammelupload unverändert.

## Gezielte Prüfung

Sechs Fälle erfolgreich: eine Sliderfrage statt zweier Fragen mit Wiederöffnen/Ausblenden; Einstellungsalias/Opt-out/Validierung; alle acht neuen Intervals-Zuordnungen und alle elf historischen Richtungswerte samt doppelter Roundtrip-Umkehrsicherung; zwei bestehende Wellness-Mappingfälle; echter HRV-Verzeichnisbackup-Roundtrip mit neuem Stresswert und erhaltenen Originalen. `/workspace/.tools/hrv-eight-step-stress-tests.log`. Kompilation/Typografie/Atlas/Diff erfolgreich; kein Gesamtlauf und keine neue APK.
