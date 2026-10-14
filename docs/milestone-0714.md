# B-Cycle 0.7.14 – Test-APK

Stand: 14.10.2026. Hauptbranch-Basis: `d12be4de57e9281dbf1bbb7c463d469701b2f8a6`.

- Version 0.7.14 / versionCode 52; Paket `de.bcycle.app`.
- Android 13+, minSdk 33, Compile-/Target-SDK 36.
- Optimierte Release-APK: `downloads/B-Cycle-0.7.14-test.apk`, 1107922 Byte.
- SHA-256: `a0ec27b98db351a1b58b82edbd142be662979e4cf81c0cc6e61fb0b0152af8cf`.
- Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.
- Signatur mit 0.7.13 identisch; ZIP-Ausrichtung und Paket-/SDK-Metadaten geprüft.
- Kein Compose; der separate Experiment-Branch bleibt unverändert.

## Enthaltene Änderungen

Kompakteres Haupt-Einstellungsmenü, Schalter bei Daten & Speicher/Kartenmanager,
kreisförmige Checkboxen, persönliche Coggan-Zonen und vier Dashboard-Trainingsfelder,
unabhängiger Zonen-Glow pro Kachel sowie die neuen Streckendetails mit seitlichen
Gruppen, Trennern und festen Diagrammskalen. Siehe
[Trainingszonen](training-zones-glow.md) und [Streckendetails](route-analysis-layout.md).

## Validierung

Die komplette Suite mit 374 Tests bestand nach der Trainingszonen-Erweiterung.
Die nachfolgenden Skalen-/Layoutänderungen bestanden 14 gezielte Tests aus
RouteChartScaleTest, RouteAnalysisViewTest und RouteAnalysisUpdateTest.
Der abschließende Versions-/Release-Build bestand in 43 Sekunden mit vorhandenen
Zwischenergebnissen, einschließlich R8, Resource-Shrinking und Lint-Vital.
Keine erneute vollständige Suite allein für den APK-Auftrag.

Die APK ist mit dem bisherigen Android-Testschlüssel signiert und als Update der
nativen Haupt-App installierbar. Praxistests für Darstellung, BLE, GPS und WebDAV
auf dem Telefon sind weiterhin sinnvoll. Keine privaten FIT-Dateien, Keystores,
Zugangsdaten oder Build-Caches werden veröffentlicht.
