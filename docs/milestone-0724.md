# BICYCLE 0.7.24 – Teststand

Build: 16.10.2026 08:02:20 CEST, versionName 0.7.24, versionCode 62.
Quellstand vor Build: `4cb8478d324cca0a6f80a0d696b7321a5b709f66`. Tag: `v0.7.24`.

## Änderungen

- Zentrale Seiten-/Kategorieabstände, schattenfreie Sticky Header in SECONDARY
  mit FOREGROUND-Linien. Dünne UI-Linien und Feldrahmen 0,75 dp, auf volle Pixel
  gerundet; Abschnittstrenner mit 12 dp Abstand oben und unten.
- Icon und Text als Gruppe in Textbuttons; zentrale Sensor-Icons auch im
  Dashboard. Sensor-Schalter mit passendem Icon, Powermeter = zap.
- Privates Fahrradbild je Bike, Bildauswahl/Löschen, zweizeilige Namen und Typ
  oberhalb des Symbols. FIT-Hauptquelle und Aufzuzeichnende Werte neu gegliedert.
- Streckendetails: Datum/Wetter wie Streckentitel, gruppierte Wetter-Icons,
  quadratische Karte, keine Mapterhorn-Zusatzzeile im Höhenabschnitt.
- Aus vorherigen Quellständen: getrennte Dreeve-/Strava-Uploads mit IDs und
  OAuth für persönliche Strava-App, BLE-Messwertarten und Dialogerhalt beim
  normalen App-Wechsel. Zentrale Strava-App mit Backend bleibt vorgemerkt.

## Verifikation

61 gezielte Tests ohne Fehler/übersprungene Fälle zu UI, Navigation,
Inhaltsabständen, Linien/Dichten, Sticky-Verhalten, Fotos, Wetter, Detailkarte,
Dashboard und Feldauswahl. Nach der abschließenden Rahmen-/Versionsanpassung
vier Bike-/Sticky-Tests erneut bestanden. Kein erneuter vollständiger Testlauf.
Release-Build erfolgreich mit R8, Resource-Shrinking und lintVital;
Version 62/0.7.24, minSdk 33, targetSdk 36 über aapt bestätigt.

Poco-Anzeigedichte/optische Linienwirkung, Android-Fotopickeranbieter sowie
Strava-OAuth und echte Dreeve-/Strava-Uploads auf dem Nutzergerät noch prüfen.
Die Tests simulieren unter anderem Dichte 2,75; 0,75 dp ergeben dort 2 Pixel.
Einrichtung: [Dienstanbindung](cloud-activity-uploads.md).

## APK

Datei: [B-Cycle-0.7.24-test.apk](../downloads/B-Cycle-0.7.24-test.apk)

- 1,583,619 Byte
- SHA-256: `6c1780f1dfd3afe66ac89eb18bc0dee7007cb3b6f34eda0d950d597bbf379a40`
- Mit bestehendem Android-Testschlüssel signiert, v3 geprüft.
- Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`
- zipalign: 4-Byte-/16-KiB-Ausrichtung geprüft.

Aktuelle native Ansichten: [Bikes](previews/bikes-sections.png),
[Sticky Header](previews/bikes-sticky.png).
