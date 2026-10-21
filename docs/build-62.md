# B|CYCLE 0.8.23 – Build 62

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.23-release-62.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.23-release-62.apk.sha256)
- [GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/39652351462)
- Quellstand: `0a5dc9abb418320dbc82044c9eed59558a7d1e9f`
- Version 0.8.23, versionCode 93, Android 13+.
- Build vom 21.10.2026, Größe: **2.154.363 Bytes (2,15 MB)**.
- APK-SHA-256: `71afb280b703842d0608e3af083bfda1884e790eea045dece34677394d917acd`
- Zertifikat-SHA-256: `94D34C46EDE28D12E6D37EFD8A7D953A75B621596EC2E5B68F61899B8EA7499B`

Optimierter Release über GitHub Actions mit R8 und Ressourcenverkleinerung. Prüfsumme mit CI abgeglichen, apksigner bestätigt den dauerhaften Schlüssel wie Build 61, aapt die Version 0.8.23/93 ohne Debug-/Test-Flag, zipalign die Ausrichtung. Mapping separat im Actions-Artefakt, nicht in der APK. Als Update über gleich signierte Vorgänger installierbar. Direkt als APK veröffentlicht, kein ZIP-Download erforderlich.

## Enthalten und geprüft

Tagesbefinden mit konfigurierbarer deutscher Fragenauswahl und drei eigenen Zahlenfeldern; Training aus Vortagsfahrten und manuelle Mengen nebeneinander; Fahrerprofil Mann/Frau und bedingte Zyklusfrage; getrennte atomare Antwortdateien in vollständiger Sicherung und Analyseexport. Kein TSS. HRV-Glow/bpm/Pillenbutton und korrigierte grafische Lückenerkennung. Formtreue Dashboard-Kurven ohne Überschwingen in allen Zeitfenstern.

15 gezielte Tests vor Release erfolgreich: sechs Tagesbefinden-Tests; bestehende HRV-Speicherung/alter-neuer Backup-Rundlauf; vollständige Verzeichnissicherung mit Antworten; Fahrerprofil-Rundlauf; HRV-Einstellungsbackup; drei Kurvenprüfungen sowie bestehende Halo/Lücken- und Zeichenbereichsprüfungen. Zuvor vier gezielte Tests der HRV-Puls-/Graphkorrekturen erfolgreich. Echten Fragebogendialog und Kurven nativ gerendert und visuell geprüft. Nur notwendige UI-Prüfungen nach letzten Abstandsänderungen wiederholt.

Keine neuen Abhängigkeiten. Kein vollständiger Gesamttestlauf oder neuer Geräte-/BLE-Test. HRV-Messwerte, Qualitätsbewertung und Aufzeichnungen bleiben unverändert; Antworten sind getrennte Zusatzdaten. Kein neuer HRV-Score, keine Langzeit-/Korrelationsdiagramme und keine Umsetzung der anschließenden Optimierungsempfehlungen.

[Änderungen](release-0.8.23.md) · [Tagesbefinden](hrv-questionnaire.md) · [Vorheriger Build](build-61.md).
