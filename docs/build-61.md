# B|CYCLE 0.8.22 – Build 61

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.22-release-61.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.22-release-61.apk.sha256)
- [GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/39615540854)
- Quellstand: `f0dfbec5f3955c93f1c2f42bbab3c83b0742773b`
- Version 0.8.22, versionCode 92, Android 13+.
- Build vom 21.10.2026, Größe: **2.112.787 Bytes (2,11 MB)**.
- APK-SHA-256: `81411756c0b51c79415de70274c637ffa336f51cc0e9aa75ee0dbeff2b542e75`
- Zertifikat-SHA-256: `94D34C46EDE28D12E6D37EFD8A7D953A75B621596EC2E5B68F61899B8EA7499B`

Optimierter Release über GitHub Actions mit R8 und Ressourcenverkleinerung. Prüfsumme mit CI abgeglichen, apksigner bestätigt den dauerhaften Schlüssel wie Build 60, aapt die Version 0.8.22/92 ohne Debug-/Test-Flag, zipalign die Ausrichtung. Mapping separat im Actions-Artefakt, nicht in der APK. Als Update über gleich signierte Vorgänger installierbar. Direkt als APK veröffentlicht, kein ZIP-Download erforderlich.

## Prüfungen

Fünf gezielte UI-Tests der enthaltenen Änderungen erfolgreich: VisualFollowupTest (Namensvorschau, editierbarer Grundname/Speicherung einschließlich leerem Namen und 60-Zeichen-Grenze; Trainingshilfe-Scrollen ohne Auswahländerung; Kartentrenner), bestehender RideNaming-Profil-/Backup-Test sowie native HrvView-Render-/Theme-Prüfung einschließlich Diagrammhöhe und tatsächlicher 50-%-Textalpha. Namensvorschau und HRV-Ansicht nativ gerendert und visuell kontrolliert.

Keine Änderungen an Messung, Berechnungen, Datenformaten oder Abhängigkeiten. Kein vollständiger Gesamttestlauf oder neuer Geräte-/BLE-Test. [Vorheriger Build](build-60.md).

[Änderungen](release-0.8.22.md) · [UI-Konventionen](ui-conventions-audit.md).

GitHub-Status bei Veröffentlichung: Alle 16 einzelnen Jobschritte einschließlich APK-Erstellung, Signaturprüfung, Artefakt-Upload und Nachbereitung melden erfolgreich abgeschlossen. Die übergeordnete API-Anzeige des Laufs/Jobs meldet noch `in_progress`; kein fehlgeschlagener Schritt. Die heruntergeladene APK wurde unabhängig wie oben geprüft.
