# Test-Build 22 · 17.10.2026

[**APK herunterladen**](https://raw.githubusercontent.com/Titanyus/B-Cycle/test-build-22-apk/downloads/B-Cycle-0.7.30-test-build-22.apk)

## Änderungen seit Test-Build 19

- Optionaler Oval-Folgemodus: neuer Button oben rechts auf der Karte. Die Karte bleibt genordet; die Position wandert abhängig von der GPS-Fahrtrichtung sanft auf einem Oval, damit mehr Kartenfläche in Fahrtrichtung sichtbar bleibt. Im Stand bleibt die Position stabil. Manuelles Verschieben unterbricht das Folgen.
- Kontinuierlichere Kartenbewegung durch leicht verzögerte Wiedergabe tatsächlich empfangener GPS-Positionen. Keine erfundenen Positionen oder Änderungen an Aufzeichnung und Messwerten. Der Positionspunkt bewegt sich auch bei manuell verschobener Karte weiter.
- Kleiner Richtungspfeil am Positionspunkt zeigt die Smartphone-Ausrichtung, auch im Stand. Fehlende, unzuverlässige oder veraltete Kompassdaten blenden den Pfeil aus. Die Kamera verwendet weiterhin die GPS-Fahrtrichtung.
- Schalter zentral als umrandete Pille: FOREGROUND-Rahmen, runder Knopf in FOREGROUND bei Aus und ACCENT bei Ein.
- GPS-Positionsaufzeichnung an den GPS-Sensorschalter gekoppelt; separate FIT-Checkbox entfernt. Wechsel zum Indoor-Bike löscht zuvor gewählte, dort ausgeblendete GPS-/E-Bike-Aufzeichnungswerte nicht mehr.
- Streckenliste liest eigene zusätzliche FIT-Distanz-/Geschwindigkeitsfelder konsistent mit der Detailansicht. Alte Listenmetadaten werden aus vorhandenen FIT-Dateien erneuert. Fehlende GPS-Koordinaten lassen sich nachträglich nicht rekonstruieren.

## Herkunft und Prüfung

[Erfolgreicher GitHub-Actions-Build 22](https://github.com/Titanyus/B-Cycle/actions/runs/39024933834), Quellstand `2898b1451c369c0e3abbe4d649f0cb7e8423e013`.

Gezielte Tests für Kartenbewegung, Oval-Geometrie, manuelle Kartenbedienung, Smartphone-Ausrichtung, Positionsdarstellung, kompakte Kartenflächen, Sicherung und Schalter erfolgreich. GPS-/FIT-Auswahl und Metadatenkorrektur wurden ebenfalls gezielt geprüft. Eine vollständige Suite mit 574 Tests war vor den anschließenden Kartenänderungen erfolgreich; danach wurden wie gewünscht nur betroffene Tests ausgeführt.

Die APK wurde von GitHub Actions gebaut und anschließend ausschließlich mit dem bisherigen Testschlüssel nachsigniert. Kein lokaler APK-Build.
App-Version **0.7.30**, versionCode **68**. Als Update über die bisherigen mit diesem Testschlüssel signierten APKs installierbar.
Signatur und 16-KiB-ZIP-Ausrichtung geprüft. Die Wirkung der Kartenbewegung und des Kompasses auf dem echten Gerät bleibt zu testen.

Größe: 8,173,487 Bytes.

SHA-256: `10ad7e98e5ff84475fd43b0fe8da776238774b90a6488d5d7eadff29bb2c09a9`

Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.
