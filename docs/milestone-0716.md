# B-Cycle 0.7.16 – Test-APK

Stand: 15.10.2026. Hauptbranch-Basis: `522693e`.

- Version 0.7.16 / versionCode 54; Paket `de.bcycle.app`.
- Android 13+, minSdk 33, Compile-/Target-SDK 36; native Views, Hochformat.
- Optimierte Release-APK: `downloads/B-Cycle-0.7.16-test.apk`, 1169362 Byte.
- SHA-256: `cf3097cd0f149facfdac4652bf1f38258f71032933ed287c0cc99cd25e7c0c72`.
- Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.
- Signatur identisch mit 0.7.15; ZIP-Ausrichtung (16 KiB), Paket-, Versions- und SDK-Metadaten geprüft.

## Änderungen seit 0.7.15

Aufnahmegrenzen für App-Fahrtzähler korrigiert. Vier native Bikes-Profile mit
Migration vorhandener Verbindungen/FIT-Auswahl in Bike 1. Nur E-Bike aktiv;
Bike/Indoor und zusätzliche Sensorrollen vorbereitet, keine Compose-Übernahme.

Details: eigene Höhenkategorie, Aufstieg/Abstieg separat; Best Efforts bis 2 h mit
maximal vier längsten vollständig auswertbaren Zeitfenstern und W/kg aus dem
aktuellen Fahrerprofil. Streckenbezogene Zonenbalken für Leistung/Puls sowie zwei
quadratische, zeitgewichtete Verteilungskurven unter den bisherigen Diagrammen.

## Validierung

Release-Build einschließlich R8, Resource-Shrinking und Lint-Vital erfolgreich,
gemessene Dauer: 54.7 Sekunden. Anschließend signiert, Signatur und identisches
Zertifikat zur Vorgängerversion, ZIP-Ausrichtung sowie APK-Metadaten geprüft.

Keine erneute vollständige Testsuite nur für diesen APK-Build. Vorherige
Funktionsprüfungen: Aufnahmegrenzen 43 gezielte Tests, Bikes vollständige Suite
mit 403 Tests, Höhe/Best Efforts 47 gezielte Tests, Zonenbalken 32 und
Verteilungskurven 37 gezielte Tests; nach letzter visueller Glättung fünf
betroffene Tests erneut bestanden. Diese Testzahlen überlappen und werden nicht
addiert. Alle genannten Läufe waren erfolgreich. Physische BLE-Verbindungen und
die Installation auf dem eigenen Telefon bleiben dem Praxistest vorbehalten.

Die einzige Meldung im Release-Build betrifft die optionale Android-Lint-Metrikdatei
in der Cloud-Umgebung; Lint-Vital und APK-Erzeugung wurden erfolgreich abgeschlossen.

Keine privaten FIT-Dateien, Schlüssel oder Zugangsdaten veröffentlicht.
