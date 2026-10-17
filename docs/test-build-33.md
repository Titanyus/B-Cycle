# Test-Build 33 · 17.10.2026

[**APK herunterladen**](https://raw.githubusercontent.com/Titanyus/B-Cycle/test-build-33-apk/downloads/B-Cycle-0.7.31-test-build-33.apk)

## Lesbarkeit heller Einstellungen und Diagramme

- Seitenheader auf PRIMARY verwenden im Hellmodus weiße Schrift, auch bei neu geöffneten Unterseiten. Normale Inhalte und Sticky-Kategorien behalten die dunkle Textfarbe.
- Helle TERTIARY-Flächen für Themes ohne DARK-Zusatz verwenden die Basis #E3E3E3 mit weiterhin 8 % PRIMARY. DARK-Themes behalten im Hellmodus ihre Basis #D0D0D0. Die eigentlichen Basispaletten bleiben unverändert.
- Puls- und Leistungsverteilungen unten in Streckendetails verwenden im Hellmodus die dunkle Textfarbe für Kurve, Leuchtsaum und transparenten Füllverlauf. Damit bleiben sie auch im weißen Bereich unterhalb Zone 1 sichtbar. Im Dunkelmodus bleibt die Kurve weiß. Deckende Zonenfarben bleiben erhalten; der Füllshader aktualisiert sich auch bei Umschaltung ohne Größenänderung.
- Die fünf Verlaufsdiagramme verwenden in beiden Modi dieselben Diagrammfarben der jeweiligen Basispalette. Der Wechsel zu sehr dunklen Farben allein aufgrund eines weißen Seitenhintergrunds entfällt. Flächenfüllung weiterhin 100 %.

Keine Änderung an Messwerten, Zonengrenzen, Statistik oder FIT-Aufzeichnung.

## Herkunft und Prüfung

[Erfolgreicher GitHub-Actions-Build 33](https://github.com/Titanyus/B-Cycle/actions/runs/39072233526), Quellstand `03c2b7adddabc333beea504123d6612c63f15624`.

**12 gezielte Tests erfolgreich** für Theme-/Headerdarstellung, Profil- und Backup-Kompatibilität, native Verteilungskurven samt Füllverlauf unter Zone 1, sofortige Aktualisierung ohne Resize, identische Farben aller fünf Verlaufsgraphen sowie bestehende Einstellungsdarstellung. Keine vollständige Testsuite und kein Gerätetest.

APK ausschließlich durch GitHub Actions gebaut, anschließend mit dem bisherigen Testschlüssel nachsigniert. Signatur und 16-KiB-ZIP-Ausrichtung geprüft. Kein lokaler APK-Build.

Version **0.7.31**, versionCode **69**, über bisherige mit demselben Testschlüssel signierte APKs als Update installierbar.

Größe: 8,255,407 Bytes.

SHA-256: `30684dded1590aca345e3d4a22409f053a067ee29fbe1caad8c5bd5c30aaa4e5`

Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.
