# B|CYCLE 0.8.17 – Build 56

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.17-release-56.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.17-release-56.apk.sha256)
- [Erfolgreicher GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/39549876412)
- Quellstand: `280bd90a6b4a60791ebe95c6298d77737767aec7`
- Version 0.8.17, versionCode 87, Android 13+, Build vom 21.10.2026.
- Größe: **2.082.919 Bytes (2,08 MB)**.
- APK-SHA-256: `43e7cc56648255251af8fcbde315ebae0574e9f10df1f3fddce870122a1ec936`
- Zertifikat-SHA-256: `94D34C46EDE28D12E6D37EFD8A7D953A75B621596EC2E5B68F61899B8EA7499B`

Optimierter Release über GitHub Actions mit R8 und Ressourcenverkleinerung; kein lokaler APK-Build. APK-Prüfsumme mit CI-Datei abgeglichen, apksigner bestätigt den dauerhaften 4096-Bit-Schlüssel der Builds 53/55, aapt meldet 0.8.17/87 ohne Debug-/Test-Flag, zipalign bestätigt die Ausrichtung. Das R8-Mapping liegt separat im Actions-Artefakt und ist nicht Teil der APK. Direkt als APK veröffentlicht, ohne ZIP-Zwischenschritt für den Nutzer.

Update über Builds 53 und 55 mit gleicher Signatur möglich. Ältere anders signierte Installationen können nicht direkt aktualisiert werden; Daten vor Installationswechsel sichern.

## Prüfungen

144 unterschiedliche Android-Hosttests und 19 HRV-Kerntests bestanden, einschließlich der 62 gepinnten NeuroKit2-Referenzfälle. Neue Prüfungen umfassen Best-Pass-Korrektur und Warnungen, bekannte LF-/HF-Testsignale, Einminuten-Protokoll, HRV-Speicherung und JSON-ZIP, Einstellungen/Backup, Native-Rendering/Themewechsel, vierzeilige Dreeve-Auswahl und alle 398 Fahrten, ZIP-Vorbereitung/Auswahl/Abbruch, unmittelbare Verarbeitungsansicht, kompakten Fastscroller, Titellaufschrift, aktuelle/gelöschte Profiltypen und GPX-FIT-Aktivitäten durch die WebDAV-/intervals-/Dreeve-Queues. Letzte Ergänzung der Paket-/Capture-Diagnostik separat erfolgreich geprüft. Die HRV-Messansicht wurde nativ gerendert und visuell kontrolliert.

Die gemeldete private Flow-GPX nur lokal geprüft: 19 erkannte stationäre Pausen, 26 min, 2:09:05 aktive Leistungszeit, volle aktive Power-/Fensterabdeckung, NP ca. 155,15 W. Keine persönlichen GPS-/Sensordaten eingecheckt.

Im breiteren Lauf von 147 Android-Tests scheitern drei ältere UI-Prüfungen. Identische Fehler am unveränderten Ausgangscommit `734d5e3` in isolierter Arbeitskopie reproduziert:

- `SettingsDesignMigrationTest.fieldDropdownAndNewRowsUseActiveThemeWhileSourceSelectionRemainsFixed`: veraltete Dropdownhintergrund-Annahme.
- `SettingsDesignMigrationTest.mainScreensAndAdvancedDialogsRecolorWithoutChangingGeometryOrNavigation`: veraltete Schriftannahme.
- `SettingsSectionsTest.newSectionsUseOneScrollHostAndNumericRowsStayInlineAcrossThemes`: erwartet einen bereits zuvor verschobenen Bosch-Abschnitt.

Diese drei Fehler wurden nicht als bestanden gewertet und nicht im Rahmen der Änderungen korrigiert. Kein vollständiger Gesamttestlauf. Physische H10-/Cycplus-H2-Messungen und echte Dienstkonten bleiben zusätzlich zu Host-/Mocktests zu prüfen; die zuvor abgelehnten RR-Rohfolgen liegen noch nicht vor. LF/HF ist eine dokumentierte spektrale Schätzung, keine klinische Validierung oder autonome Balancebewertung.

[Änderungen und Funktionsumfang](release-0.8.17.md) · [HRV-Fachquellen](hrv-artifact-research.md) · [Übergabe für neue Chats](CONTINUE-HERE.md).
