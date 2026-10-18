# B|CYCLE 0.8.4 – optimierte APK

Build 39 vom 18.10.2026, versionCode 74.

**[APK herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/v0.8.4/downloads/B-Cycle-0.8.4.apk)**

## Änderungen

- Profilseite mit Abstand zum Footer; Karten bei Indoor ausgeblendet, Bosch-Fahrmodus nur bei Bosch-E-Bikes. Gespeicherte Einstellungen bleiben erhalten.
- Profilbezogene Farben, Breiten und Deckkraft für beide Streckenlinien mit offline Kartenvorschau.
- Neue Fahrten speichern ihre Profilzuordnung: PRIMARY-Farbrechteck in der Streckenliste und zentrierter Fahrradname in 900 Black über dem Datum in den Details.
- Horizontaler Trenner zwischen Datum und verfügbaren Wetterdaten.
- Dezenter GPS-Genauigkeitskreis mit Hysterese; ungenaue Positionen ausschließlich für die Kartenanzeige, Aufnahmefilter bleiben erhalten.

## Prüfung und Grenzen

Gezielte Tests für Profilabschnitte und Abstände, Footerwechsel, Profil-/Farbzuordnung, Detailkopf, GPS-Anzeigedaten und gerenderten Genauigkeitskreis bestanden. Speicherung inklusive Profilmetadaten nach fehlgeschlagenem Export und anschließendem Wiederholen geprüft. Zuvor die Liniengestaltung einschließlich Vorschau, Alpha, Profilübernahme und Sicherung gezielt geprüft. Kein vollständiger Testlauf, kein physischer Gerätetest dieser APK.

Ältere Fahrten und Importe ohne belastbare Profilzuordnung erhalten keinen geratenen Fahrradnamen oder Farbindikator. Genauigkeitskreis zeigt Androids geschätzten Radius, keine garantierte Fehlergrenze. Details: [Profil-/Kartenverbesserungen](profile-map-refinements.md), [Liniengestaltung](map-route-style.md).

## Build und Datei

- [Erfolgreicher GitHub-Actions-Build](https://github.com/Titanyus/B-Cycle/actions/runs/39142010398), Quellstand c040f917fa62b85a92cae09f5e0cee3fecfcee49.
- Release mit R8 und Ressourcenverkleinerung; kein lokaler APK-Build. Vorhandenes Artefakt nach Wiederherstellung des GitHub-Zugriffs verwendet, kein zweiter Build.
- Artefakt-Prüfsumme vor Nachsignierung geprüft; etablierter Testschlüssel für Update-Kompatibilität. Zertifikat, Version 0.8.4/74, 16-KiB-Zip-Ausrichtung und nicht-debuggable Manifest geprüft.
- Größe: 1,821,748 Bytes (rund 1.8 MB).
- SHA-256: `a9638111db4064e69e66831a023b9da2cb4471a6ce5ac33f2f036f2e2314e052`.
- Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.
- [Prüfsumme](https://raw.githubusercontent.com/Titanyus/B-Cycle/v0.8.4/downloads/B-Cycle-0.8.4.apk.sha256) · [R8-Mapping](https://github.com/Titanyus/B-Cycle/blob/v0.8.4/downloads/B-Cycle-0.8.4-mapping.txt.gz).

Dauerhafter Download am Versionstag; Meilenstein 0.8 bleibt erhalten. Dreeve-Bestätigungsoptimierung und zentrale Strava-Anmeldung bleiben vorgemerkt.
