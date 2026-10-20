# B|CYCLE 0.8.16 – Build 55

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.16-release-55.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.16-release-55.apk.sha256)
- [Erfolgreicher GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/39474797792)
- Quellstand: `6441c000fdc1a019d89c832e379f7aa857c4838a`
- Version 0.8.16, versionCode 86, Android 13+, Build vom 20.10.2026.
- Größe: **2.028.855 Bytes (2,03 MB)**.
- APK-SHA-256: `fa00c772730836c53adbfe8f13a7db759000bc8a54bd8a15c8b1ae7673019e9e`
- Zertifikat-SHA-256: `94D34C46EDE28D12E6D37EFD8A7D953A75B621596EC2E5B68F61899B8EA7499B`

Optimierter Release mit R8 und Ressourcenverkleinerung. Kein gesetztes Debug- oder
Test-Flag, kein lokaler APK-Build. Nach Download geprüft: SHA-256 stimmt mit der
CI-Prüfsumme überein, apksigner bestätigt den dauerhaften Schlüssel von Build 53,
Manifest meldet 0.8.16/86, zipalign bestätigt die Ausrichtung. R8-Mapping ist eine
separate Datei im Actions-Artefakt und nicht Teil der APK.

Die APK kann Build 53 mit gleicher Signatur als Update ersetzen. Für ältere anders
signierte Installationen, insbesondere Build 52 und die bisherige Testschlüsselserie,
ist weiterhin kein direktes Update möglich. Bestehende Appdaten vor einem
Installationswechsel sichern.

106 gezielte Hosttests und 12 HRV-Kerntests bestanden, einschließlich 62 gepinnter
NeuroKit2-Referenzfälle. Geprüft wurden HRV-Messablauf, geringe Abdeckung als Warnung,
Originalpakete und Empfangszeit auf Android 13/15/16, Speicherung/Wiederherstellung,
Dashboard und Themewechsel sowie relevante Aufnahme-, Import- und Sicherungspfade.
Die Messansicht wurde mit nativer Grafik gerendert und visuell geprüft.
Echte Gurt-/Telefonprüfung sowie persönlicher WebDAV-Server bleiben praktisch zu prüfen.

[Änderungen und Funktionsumfang](release-0.8.16.md) · [HRV-Details](hrv-measurement.md).
Build 54 wurde vor Veröffentlichung gestoppt; Build 55 enthält zusätzlich die
Absicherung verzögert zugestellter BLE-Pakete beim Messbeginn.
