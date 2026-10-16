# BICYCLE 0.7.30 – Dashboard, Akku und Strecken

Build: 16.10.2026 16:10:33 CEST; versionName 0.7.30, versionCode 68. Tag: `v0.7.30`.
Quellstand der Funktionsänderungen: `bd65f9a`, Versionsanhebung und CI-Einrichtung anschließend.
Auf ausdrücklichen Nutzerwunsch einmalig lokal gebaut; künftige Standard-Builds über
[GitHub Actions](github-actions.md).

## Änderungen

- Dashboard-Graphen: ausschließlich Skalenbeschriftung um 20 % kleiner.
- Zonenvisualisierung heller, dieselbe zentrale Lichtintensität wie der Rand-Glow.
  Farben, Geometrie und weiche Übergänge erhalten.
- Design & UI → Fahrmodus-Anzeige: Oben oder Seitlich (Standard). Akkuwarnung
  folgt dieser Position auch bei deaktiviertem Modus-Glow; Breathing unverändert.
- Kombiniertes Akku-Raster im Dashboard und über der Karte 10 % kleiner.
- Direkte Auswahl bis vier Akkus; kein zusätzlicher Kombischalter oder einzelne
  Prozent-Auswahlfelder mehr. Nur verfügbare Akkudaten angeschlossener Geräte passend
  zum Bikeprofil plus Smartphone. Vorhandene Prozent-Feld-IDs bleiben lesbar.
- Bosch-Akkustand bleibt während der laufenden LIVE-Verbindung auch bei längeren
  Sendepausen erhalten. Bei Trennung/Profilwechsel Puffer verwerfen, 0 % gültig.
- Best Efforts: weiterhin maximal vier längste auswertbare Zeitfenster, Darstellung
  mit den kürzeren Zeitfenstern links.
- Reimport einer höhenkorrigierten Download-Kopie erkennt die aktive FIT anhand
  ihres exakten Inhalts und behält die vorhandene Fahrt-ID und den Namen.
  Bereits vorhandene doppelte Einträge werden nicht automatisch entfernt.
- Alle Funktionen aus [0.7.29](milestone-0729.md), einschließlich Dashboard-Vollbild.

[Details und Prüfgrenzen](dashboard-adjustments-2026-10-16.md).

## Verifikation

60 gezielte Tests bestanden, keine Fehler oder übersprungenen Tests:
BatteryFieldsTest, BoschModeGlowTest, TrainingZoneUiTest, LiveGraphUiTest,
SettingsBackupTest, FitLibraryTest, BestEffortsTest und BestEffortsViewTest.
Keine erneute vollständige Testsuite für diesen Build. Letzter vollständiger Lauf:
537 Tests für 0.7.28. Release-Build mit R8, Resource-Shrinking und Lint Vital erfolgreich.
Signatur mit bestehendem Testschlüssel, Android 13+/Target 36 und 16-KiB-Ausrichtung geprüft.

Neue Darstellung und Verbindungsverhalten noch am Gerät prüfen. Der reproduzierte
Duplikatpfad ist abgesichert; ohne die beiden Gerätedateien bleibt die konkrete
Zuordnung des gemeldeten Paars offen.

## APK

[B-Cycle-0.7.30-test.apk](../downloads/B-Cycle-0.7.30-test.apk)

- 1,657,515 Byte
- SHA-256: `b30cf8ba62fde4cfd2ccaaa9dbc01b682d2cbbb5c0311467544a0a9b95d14915`
- Als Update mit dem bisherigen Android-Testschlüssel installierbar.
- Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.
