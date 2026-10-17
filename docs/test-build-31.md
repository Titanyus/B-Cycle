# Test-Build 31 · 17.10.2026

[**APK herunterladen**](https://raw.githubusercontent.com/Titanyus/B-Cycle/test-build-31-apk/downloads/B-Cycle-0.7.31-test-build-31.apk)

## Helle Einstellungen und vorgemerkte UI-Anpassungen

- Sonne-/Mond-Button rechts im Einstellungsheader. Icon zeigt den aktuellen Zustand. Helle Einstellungen: SECONDARY #FFFFFF, TERTIARY #CCCCCC, FOREGROUND #202020; PRIMARY und ACCENT bleiben unverändert. Dashboard, Rand-Glow, Nachrichtenfenster während der Fahrt und Kartenhelligkeit werden nicht umgestellt. Standard dunkel, je Fahrradprofil gespeichert und in Design-Backups enthalten. Im Streckendetailheader bleibt das Umbenennen-Icon erhalten; die Helligkeit folgt den Einstellungen.
- Einheiten und Zusatztexte verwenden in Einstellungsansichten 65 % des aktuellen FOREGROUND für Lesbarkeit auch auf Weiß.
- Neues Theme **Embers Inv.**: #BF281B / #1D1D1D / #000000 / #FFFFFF / #BF281B. Flächen gegenüber Embers getauscht, bisheriges Grau um ca. 30 % abgedunkelt. Ursprüngliches Embers erhalten.
- Formularbuttons, Dropdowns und Eingabefelder (einschließlich Radumfang) mit **1 dp Rahmen, 50 % FOREGROUND, 6 dp Rundung**. Nur Konturen halbtransparent, Flächen und Typografie der Eingabefelder erhalten.
- **Dashboard im Vollbild** steht zuerst in Design & UI.
- Benachrichtigungen ohne Header Position/Anzeigedauer. Dauertitel links neben dem Slider, vertikal zur Sliderlinie zentriert; Sekundenwert darüber. Apps bleibt Sticky-Abschnitt.
- Mülltonne über dem Fahrradfoto ohne Hintergrund/Kontur. Vergrößern-Symbol über der Streckendetailkarte fest schwarz.

[Details der Einstellungsdarstellung](settings-appearance.md)

## Herkunft und Prüfung

[Erfolgreicher GitHub-Actions-Build 31](https://github.com/Titanyus/B-Cycle/actions/runs/39056204878), Quellstand `459abd4ff9d34e3d3eb78aac0fb8ef0d776c93c3`.

**57 gezielte Tests erfolgreich** für Theme-/Headerumschaltung, unveränderte Dashboard-Palette, Profil-/Backup-Roundtrip, Themeauswahl, Sticky-Abschnitte, Formularrahmen, Vollbild und Benachrichtigungs-Layout. Zusätzlich im vorhergehenden Änderungsschritt Fahrradfoto und Streckendetails gezielt geprüft. Keine vollständige Suite und kein Gerätetest.

APK durch GitHub Actions gebaut und anschließend mit dem bisherigen Testschlüssel nachsigniert. Signatur und 16-KiB-ZIP-Ausrichtung geprüft. Kein lokaler APK-Build.

Version **0.7.31**, versionCode **69**; als Update über bisherige mit diesem Testschlüssel signierte APKs installierbar.

Größe: 8,243,119 Bytes.

SHA-256: `3d03ae9f62809c41bca9d7c98207efb107289eb6c6b20d2345a0e469728afb08`

Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.
