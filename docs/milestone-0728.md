# BICYCLE 0.7.28 – Akku-Kombifelder und Dashboard 10

Build: 16.10.2026 13:40:31 CEST; versionName 0.7.28, versionCode 66.
Quellstand: `e036b4dc9b17eb777303a361a961531898608b83`. Tag: `v0.7.28`.

## Enthalten

- Neue Akku-Kategorie mit Einzelständen und Kombianzeige. Pro Datenfeld ein bis vier
  Quellen wählen; 2×2 mit Sensoricon, Balken und Prozent, ohne Zusatzüberschrift.
  E-Bike, Pulsmesser, Indoor-Trainer soweit verfügbar und Smartphone. Unverfügbare
  Werte behalten ihren Slot als Strich, echte 0 % bleiben gültig.
- Unter 20 % Gelb/Orange, unter 10 % Rot; sonst neutral. Dashboard hell, Karte dunkel,
  dort dunklere Warnfarbtöne. Kombination/Persistenz auch in Karte, Presets und Backup.
- Karten-Datenzeile nur bei drei/vier Quellen 76 statt 62 dp hoch. Aufnahme/Licht und
  Zoom-Abstände erhalten; obere Datenfelder bleiben unverändert.
- Smartphone-Kategorie mit Smartphone-Icon: lokale Uhrzeit HH:mm und Akkustand.
  Systembroadcast statt zusätzlichem Timer, keine neuen FIT-Felder.
- Dashboard 10: drei große Einzelfelder untereinander, darunter zwei Felder, keine Karte.
  Volle Datenhöhe, Vorschau, separater Editor-Beenden-Button, untere Leiste erhalten.
  Kartenlebenszyklus auf dieser Seite pausiert, GPS/FIT bleiben unabhängig.
- Wetter-Auswahl auch bei Indoor sichtbar: vier bestehende Solar-/Tageslichtwerte,
  ohne gültige GPS-Position weiterhin keine Berechnung und kein GPS-Start für Indoor.
- Alle Funktionen aus [0.7.27](milestone-0727.md) einschließlich Nachrichtenfenster und
  Indoor-Korrekturfaktoren enthalten.

Details und native Testdaten-Vorschauen: [Akku/Dashboard 10](battery-dashboard.md).

## Verifikation

Vollständige Testsuite: **537 Tests, keine Fehler oder übersprungenen Tests**.
Quellen-/Grenzwerte/0/fehlende Werte, Broadcast-Lebenszyklus, Auswahlgrenzen, Quellen-
und Ressourcenlisten, schmale Dreierfelder, Kartenstatuszeichen, Persistenz und Backup,
Dashboard 10 samt Editor und Rückkehr zur Karte geprüft. Alte Karten-Geometrietests
weiter für Kartenlayouts; neue Vollbild-Geometrie separat geprüft.

Release-Build mit R8, Resource-Shrinking und Lint Vital erfolgreich. Bestehender
Testschlüssel, Version/minSdk/targetSdk und 16-KiB-ZIP-Ausrichtung geprüft.

Noch am Gerät prüfen: Lesbarkeit der kompakten Anzeige, reale Smartphone-/Sensor-
Akkumeldungen und Dashboard-10-Bedienung. Die reale Messenger-Zustellung aus 0.7.27
bleibt ebenfalls praktisch zu testen. Keine FIT-/Sensor-/Performance-Logik geändert.

## APK

[B-Cycle-0.7.28-test.apk](../downloads/B-Cycle-0.7.28-test.apk)

- 1,653,419 Byte
- SHA-256: `88a4fc631afa372f37cf179dccad592580733bd34bad5d0c2eb7bfd5bdeb42a5`
- Als Update mit bestehendem Android-Testschlüssel installierbar.
- Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.
