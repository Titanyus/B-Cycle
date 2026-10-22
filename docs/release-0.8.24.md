# B|CYCLE 0.8.24

League Gothic ersetzt Bebas Neue, Schriftfamilien und Eingaberollen sind zentral; nur Hauptheader erzwingen Großbuchstaben. HTML-UI-Atlas mit App-Tokens, Themen, Maßeinspektor und unabhängigen Abstandsreglern.

Tagesbefinden-Slider ohne sichtbare Wert-/Unbeantwortet-Zeile und ohne Zurücksetzen pro Slider. Skalenbeschriftung 50 % Deckkraft. Finale Abstände: Gruppen 32 dp, Label → Slider 0 dp, Slider → Skala −8 dp. Negative Margin an der gesamten Skalenzeile verhindert abgeschnittene Schrift; Touchhöhe bleibt 48 dp. Labels über Texteingaben/Dropdowns 4 dp, feldbezogene Inline-Rückmeldungen 4 dp.

Enthält außerdem die Änderungen seit Build 62: optionaler erster HRV-Tagesstart, manueller HRV-Einstieg in Einstellungen, Endsignale und vereinfachtes Tagesbefinden sowie die gezielten Historien-/Lookup-/Dienststatusoptimierungen. Details docs/hrv-entry.md und docs/priority-optimizations.md. HRV-Berechnung, Rohdaten, Antwortformate, Aufzeichnungen und Profile bleiben kompatibel.

Vor diesem Build 11 gezielte Hosttests für Tagesbefinden und Formularabstände erfolgreich; Schriftwechsel und vorherige Schritte bereits gezielt geprüft. Kein vollständiger Gesamttestlauf oder neuer Geräte-/BLE-Test. Optimierter Release-Build über GitHub Actions mit dauerhaftem Signierschlüssel.
