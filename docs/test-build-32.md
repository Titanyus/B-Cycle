# Test-Build 32 · 17.10.2026

[**APK herunterladen**](https://raw.githubusercontent.com/Titanyus/B-Cycle/test-build-32-apk/downloads/B-Cycle-0.7.31-test-build-32.apk)

## Neue Themes und Inline-Einstellungen

- **14 feste Themes** ersetzen die bisherigen: HELLFIRE, SOLAR FLARE, SUNSTRUCK, ACID POP, DEEP DIVE, BLUE RIOT und PURPLE HAZE mit jeweils farbigen Flächen und DARK-Variante. Exakte vorgegebene Werte, alte Profil-/Backup-IDs werden passend zugeordnet. Kein Reserviert-Eintrag und kein Einstieg zur eigenen Farberstellung.
- Theme-Vorschau zeigt nur PRIMARY, SECONDARY, TERTIARY; Titel **21 sp Bebas Neue**, wie schmale Eingabewerte.
- **Hell/Dunkel global** für Einstellungen, Sonne/Mond nur auf der Einstellungs-Hauptseite. Themeauswahl bleibt profilbezogen. Alte profilbezogene Hell/Dunkel-Werte werden ignoriert. Helle Darstellung: Weißer Hintergrund, Schrift aus #101010 mit 5 % PRIMARY und graue Fläche aus #D0D0D0 mit 8 % PRIMARY. Dashboard und Kartenhelligkeit unverändert.
- WebDAV, intervals.icu, Dreeve und Strava direkt in **Daten & Speicher** als native Schalter-/Accordion-Zeilen. Schalten und Aufklappen unabhängig; Öffnen aktiviert keinen Upload. Bestehende Formulare und Auto-Save-Listener erhalten; Dienst-Worker werden beim Schließen der gemeinsamen Seite aufgeräumt.
- **Einstellungen sichern** inline mit Bereichsauswahl und Sicherungsaktion. Wiederherstellen im selben Zeilenstil, weiterhin direkte Android-Dateiauswahl und anschließende Bestätigung. Lokale Streckenspeicherung behält ihren Unterdialog.
- Normale Rahmen der Themeauswahl und Strecken-Dropdowns ebenfalls zentral 1 dp / 50 % FOREGROUND / 6 dp Rundung. Aktive Auswahlmarkierungen weiterhin ACCENT.

[Paletten und Migration](theme-palettes.md) · [Hell/Dunkel](settings-appearance.md) · [Daten & Speicher](data-storage-backup.md)

## Herkunft und Prüfung

[Erfolgreicher GitHub-Actions-Build 32](https://github.com/Titanyus/B-Cycle/actions/runs/39070731223), Quellstand `a218c50e5f98063433881906dc016459b546abbf`.

**52 unterschiedliche gezielte Tests erfolgreich**: exakte Paletten, Themeliste/Profil-/Backup-Kompatibilität, helle Darstellung ohne Dashboardänderung, globaler Zustand und fehlender Umschalter auf Unterseiten, unabhängige Service-Schalter/Accordions samt Persistenz, Sicherung/Wiederherstellung einschließlich Dateiauswahl und Activity-Neustart, Rückkehr aus anderen Apps sowie Dropdown-/Navigationsdarstellung. Betroffene Dropdown- und Abstandsprüfungen nach den letzten Anpassungen erneut erfolgreich. Native Darstellung geprüft; keine vollständige Testsuite und kein Gerätetest.

APK ausschließlich durch GitHub Actions gebaut, anschließend mit dem bisherigen Testschlüssel nachsigniert. Signatur und 16-KiB-ZIP-Ausrichtung geprüft. Kein lokaler APK-Build.

Version **0.7.31**, versionCode **69**, über bisherige mit demselben Testschlüssel signierte APKs als Update installierbar.

Größe: 8,251,311 Bytes.

SHA-256: `594ce793a8c918e2db3a8ac8cfc48c8a5d6ef7d8064cb7ceca776643ef2485df`

Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.
