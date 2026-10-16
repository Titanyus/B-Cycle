# BICYCLE 0.7.25 – Indoor-Testversion

Build: 16.10.2026 09:31:52 CEST; versionName 0.7.25, versionCode 63.
Quellstand: `51de799a2776645b69c10b1a3dd55819e9b8562c`. Tag: `v0.7.25`.

## Enthalten

- BLE-/FTMS-Empfang und Indoor-Bike aktiv, ausschließlich Trainer plus optionalem
  Pulsmesser. Kein GPS und keine Trainersteuerung; manueller Widerstand bleibt am Hebel.
- Profilbezogene Trainerzuordnung, Schalter, Akku soweit verfügbar, Messwertarten,
  Verbindungs-/Live-/Veraltetstatus und automatische Wiederverbindung.
- Bestehende Live-Daten, Graphen, Trainingszonen und Performance-Engine mit FTMS
  als eigener Quelle. Fahrtstatistiken nur während Aufnahme, nach STOPP eingefroren.
- Journal V6 mit Rückwärtslesen von V4/V5; FIT Indoor Cycling ohne erfundene
  GPS-/Motorwerte. Bosch-Nullfortschreibung nicht auf FTMS-Lücken anwenden,
  auch beim nachträglichen Einlesen. Optionale Performancefelder bleiben auswählbar.
- Vorheriger Quellstand enthalten: sechs dynamische Bikes, Hinzufügen/Löschen,
  horizontale Auswahl und Drag-and-drop-Reihenfolge; kompakte Streckendetails,
  korrigierte Header-Abstände und zentrierte Wettergruppen mit Trennpunkten;
  Streckenlistentrenner mit 55 % Deckkraft.

Einrichtung: [Indoor/FTMS](indoor-ftms.md). [Native Vorschau](previews/bikes-indoor.png).

## Verifikation

Vollständige Testsuite: **511 Tests, keine Fehler, keine übersprungenen Tests**.
Nach abschließender Quellenabsicherung beim FIT-Einlesen: **29 weitere gezielte
Tests, keine Fehler, keine übersprungenen Tests**, einschließlich eines neuen
Regressionsfalls zu fehlender Indoor-Leistung bei weiter ankommender Geschwindigkeit.

Parser/Flags, optionale Felder, Nullen und Lücken; GATT-CCCD vor Battery-Read,
keine Steuerbefehle, Main-Thread/alte Callbacks, Scan und Wiederverbindung;
Profil-/FIT-/Dashboard-Auswahl und Hintergrundaufnahme geprüft. Garmin-FitDecoder
und ActivityFileValidationPlugin validieren die Indoor-FIT; erneutes Detail-Einlesen,
NP, mechanische Arbeit und Estimated FTP einschließlich echter Nullleistung geprüft.
Alte Tests an bereits geltende Profil-, Accordion-, Geburtstagsfeld- und Abstands-
konventionen angepasst; bestehende fachliche Prüfungen erhalten.

Release-Build mit R8, Resource-Shrinking und lintVital erfolgreich. Nach letzter
FIT-Änderung neu gebaut; Version und Signatur geprüft, ZIP-Ausrichtung 16 KiB.

**Noch am Gerät prüfen:** echte FTMS-Verbindung zum Matrix ICR50, Genauigkeit der
Gerätewerte, längere Ausfälle, Wiederverbindung und Langzeitaufnahme. Der vorhandene
ANT+ Explorer-Referenztest belegt das Protokoll, noch nicht diesen BICYCLE-Build.

## APK

[B-Cycle-0.7.25-test.apk](../downloads/B-Cycle-0.7.25-test.apk)

- 1,612,291 Byte
- SHA-256: `67f547d22a0a629c8d41259427ce7074a014c47f20f2e5fbeaf0e781e5704868`
- Mit bestehendem Android-Testschlüssel signiert; als Update installierbar.
- Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.
