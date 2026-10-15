# B-Cycle 0.7.23 – Test-APK

Version 0.7.23 / versionCode 61; Android 13+, Compile-/Target-SDK 36.
Build-Zeit: 15.10.2026 17:34:46 CEST. Basis 4be2773 mit nachfolgenden Änderungen.

## Änderungen seit 0.7.22

- Eigener intervals.icu-Eintrag neben Cloud & WebDAV unter Streckendaten;
  Athleten-ID/API-Schlüssel, Verbindungstest und unabhängiger automatischer Upload.
- Der vorhandene Cloud-Button sendet an aktivierte Dienste; unabhängige
  Warteschlangen/Bestätigungen, keine importierten Strecken. Aktive FIT-Version.
- Wetterzustand/Temperatur, Feuchtigkeit und Wind unter Datum in den Streckendetails,
  bezogen auf die zeitliche Tourmitte. Open-Meteo-Modellwerte und dauerhafte
  atomare Wettermetadaten pro Fahrt außerhalb des temporären Caches.
- Quellen & Dienste zentral unter Erweitert.
- Umbenennen per Titel/Stift; Höhenkorrektur als zweite Footer-Aktion.
- Bestätigungsdialoge mit zentral mindestens 8 dp Abstand; Header-Schatten
  12 dp / maximal 40 % Deckkraft. Plus/Minus im Editor ebenfalls gedimmt.
- Zusätzlichen GPS-Aktivierungsbutton aus Karten und Label Fahrradkategorie
  aus Bikes entfernt. GPS-Berechtigung über Bikes bleibt erhalten.
- Geburtsdatum als schmaler Zahlen-/Datumswert im dezenten Rahmen wie Körpergröße;
  DatePicker und unmittelbare Speicherung erhalten.
- Neues Nutzer-App-Icon; vorheriger Vektor bleibt im Quellcode. Einstellungsbild
  als bereitgestelltes WebP (68.900 Byte) statt PNG (1.014.317 Byte).

## Validierung

50 gezielte Tests in mehreren Läufen bestanden: IntervalsTest (6), RideWeatherTest
(5), RouteExportTest (7), DataSettingsNavigationTest (5), SettingsMenuTest (4),
RouteDetailActionsTest (3), RouteAnalysisViewTest (7), FitElevationCorrectionTest
(8), RiderProfileViewTest (5). Die letzte Version wurde zusätzlich mit den fünf
betroffenen Wetter-/Upload-/Einstellungs-/Fahrer-Testklassen (25 Tests) geprüft.
Keine erneute vollständige Testsuite.

Geprüft: Basic-Auth und Multipart-Anfragen mit lokalem HTTP-Testserver,
bestätigte IDs, Fehler/Abbruch, dauerhafte unabhängige Uploadbelege, Duplikate,
korrigierte FIT-Version und Vormerken beim Aufnahmeende. Bestehende WebDAV-
Verschlüsselung bleibt kompatibel. Wetter: zeitliche Mitte, UTC-Stunden, Archiv/
rezente Daten, echte Nullen/fehlende Werte, dauerhafte Speicherung und Offline-
Wiedereinlesen nach Löschen des Caches. Navigation und Geburtsdatum erhalten.

Optimierter Release-Build mit R8, Resource-Shrinking und Lint-Vital erfolgreich
in 1 Minute 15 Sekunden. Signatur, gleiches Zertifikat wie bisher, ZIP-Ausrichtung
(16 KiB), Version und SDK-Metadaten geprüft. Kein Hardwaretest.

Der direkte Zugriff auf intervals.icu und die Wetteranbieter ist hier durch die
Domain-Netzwerkregel blockiert. Keine produktiven Uploads oder echten Wetterabrufe
als erfolgreich ausweisen; Kontoverbindung und Anbieterantworten auf dem Gerät
prüfen. intervals.icu-Zugang unter Daten & Speicher konfigurieren.

Wetter wird beim ersten erfolgreichen Abruf in der Detailansicht dauerhaft
lokal zur Fahrt gespeichert. Wettermetadaten sind derzeit kein Bestandteil des
FIT-Exports oder der reinen Einstellungssicherung. Details:
[intervals.icu](intervals-icu-plan.md), [Wetter](ride-weather.md).

APK: `downloads/B-Cycle-0.7.23-test.apk`, 1546643 Byte.
SHA-256: `a01b442962f076738c269755bb7c7da93e3a8e7d0220b2ab4f440bcba0080544`.
Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.
Mit bisherigem Testschlüssel als Update installierbar.
