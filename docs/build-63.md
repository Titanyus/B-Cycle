# B|CYCLE 0.8.24 – Build 63

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.24-release-63.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.24-release-63.apk.sha256)
- [Erfolgreicher GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/39734822597)
- Quellstand: `0bd848c55b3ea15c1308f9cd847afeb8a27de698`
- Version 0.8.24, versionCode 94, Android 13+.
- Build vom 22.10.2026, Größe **2.176.486 Bytes (2,18 MB)**.
- APK-SHA-256: `1bd6d99360a3869c2202732110753008b53fb7738407f028e6678d925ba5ca04`
- Zertifikat-SHA-256: `94D34C46EDE28D12E6D37EFD8A7D953A75B621596EC2E5B68F61899B8EA7499B`

Optimierter Release über GitHub Actions mit R8/ Ressourcenverkleinerung. Prüfsumme mit CI abgeglichen, apksigner bestätigt den dauerhaften Schlüssel wie Build 62, aapt Version 0.8.24/94 ohne Debug-/Test-Flag, zipalign die Ausrichtung. Mapping separat im Actions-Artefakt, nicht in der APK. Direkt als APK veröffentlicht, ohne ZIP. Als Update über gleich signierte Vorgänger seit Build 53 installierbar.

## Enthalten

Finale Sliderabstände: 32 dp zwischen Fragenblöcken, 0 dp Label → Slider, −8 dp Slider → Skalenbeschriftung. Skalenbeschriftung mit 50 % Deckkraft; negative Margin am Skalencontainer verhindert abgeschnittenen Text, 48-dp-Touchfläche erhalten. Keine sichtbaren Auswahl-/Unbeantwortet-Zeilen oder Zurücksetzen pro Slider. Texteingabe-/Dropdown-Labelabstand weiterhin 4 dp, feldbezogene Inline-Rückmeldungen 4 dp.

League Gothic statt Bebas Neue, zentrale Schriftrollen, Großschreibung nur in Hauptheadern. Beinhaltet auch die seit Build 62 umgesetzten HRV-Einstiege/Endsignale, Tagesbefinden-Vereinfachungen sowie die gezielten Historien-/Lookup-/Dienststatusoptimierungen. [Änderungen](release-0.8.24.md), [HRV-Einstieg](hrv-entry.md), [Optimierungen](priority-optimizations.md), [Schriften](fonts.md), [UI-Atlas](ui-catalog.md).

## Prüfung

11 gezielte Android-Hosttests für finale Tagesbefinden-/Formularabstände erfolgreich. Browserprüfung misst 32/0/−8 dp und prüft unabhängige Vorschau-Regler, unverändert 4 dp bei Texteingaben/Inline-Hinweisen, 50 % Deckkraft, Bedienung und Breiten 320/360/412. Native Tagesbefinden-Ansicht visuell auf vollständige Skalenbeschriftung geprüft. Schriftwechsel und vorherige Schritte waren zusätzlich separat gezielt geprüft (56 unterschiedliche Tests für den Schriftwechsel).

Kein vollständiger Gesamttestlauf, Geräte-/BLE-Test oder lokaler APK-Build. HRV-Algorithmus, Rohdaten, Aufzeichnungen, FIT und Profile bleiben unverändert kompatibel. [Vorheriger Build](build-62.md).
