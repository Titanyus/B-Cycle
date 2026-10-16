# BICYCLE 0.7.29 – Akkuanzeige und Benachrichtigungs-Header

Build: 16.10.2026 15:02:09 CEST; versionName 0.7.29, versionCode 67. Tag: `v0.7.29`.
Quellstand: `8adf690858ccddf63a083aedd87fd8c1dd9fe8ee` (Akku-/Headeranpassungen plus Dashboard-Vollbild und Versionsanhebung).

## Änderungen

- Vertikale Akkubalken mit Füllung von unten nach oben; platzabhängig größere Icons
  und Prozentwerte, bis rund 27 %. Gemeinsamer Renderer für Dashboard und Karte.
- Alle unterstützten Akkuquellen in der Auswahl, unabhängig vom gewählten Bikeprofil.
  Fehlende oder veraltete Werte bleiben fehlend. Keine Änderungen an FIT/Sensorwerten.
- Kombianzeige über Schalter aktivieren/deaktivieren. Einzelne Prozentfelder bleiben
  bei aktiver Kombination ausgegraut, auch beim Kategorienwechsel. Der letzte gewählte
  Akku kann abgewählt werden; dadurch wird die Kombination deaktiviert.
- Smartphone-Auswahl bleibt bedienbar. Nur zusätzliche nicht ausgewählte Quellen
  werden bei Erreichen von vier Quellen gesperrt.
- Benachrichtigungen: Position, Anzeigedauer und Apps verwenden die zentralen Sticky
  Header einschließlich Abständen und Ablösung durch den folgenden Abschnitt.
- Dashboard-Vollbild: Android-Statusleiste standardmäßig ausgeblendet, Randgeste
  blendet sie vorübergehend ein. Design & UI bietet einen persistenten Schalter.
  Dialoge behalten die Statusleiste, Android-Navigation und Cutout-Abstände unverändert.
  Wahl auch im Design-Backup enthalten, alte Backups bleiben kompatibel.
- Vorhandene Funktionen aus [0.7.28](milestone-0728.md) bleiben enthalten.

## Prüfung

Zunächst 29 gezielte Tests für Akkuauswahl, Benachrichtigungen, Dashboard-Feldlisten,
Indoor-Auswahl und Backups bestanden. Nach Ergänzung des Dashboard-Vollbildmodus
31 gezielte Tests bestanden: DashboardFullscreenTest, FullscreenDialogTest,
MessageNotificationUiTest und SettingsBackupTest. Keine Fehler oder übersprungenen
Tests; Kotlin-Kompilierung erfolgreich. Für diesen kleinen UI-Build
keine erneute vollständige Testsuite; der vorige vollständige Lauf mit 537 Tests
ist im Nachweis zu 0.7.28 dokumentiert.

Release-Build mit R8, Resource-Shrinking und Lint Vital erfolgreich. Signatur mit
bestehendem Testschlüssel, Android 13+/Target 36 und 16-KiB-ZIP-Ausrichtung geprüft.

Der Nutzer hat die reale Benachrichtigungsfunktion am 16.10.2026 bestätigt. Neue
Akkuanordnung, Sticky Header und Statusleisten-Gesten dieser APK noch am Gerät prüfen.

## APK

[B-Cycle-0.7.29-test.apk](../downloads/B-Cycle-0.7.29-test.apk)

- 1,657,515 Byte
- SHA-256: `7ee54ce640e90ddc71ab02160c1fcf337a1c32ccea7e02efc29021861405b797`
- Als Update mit bestehendem Android-Testschlüssel installierbar.
- Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.
