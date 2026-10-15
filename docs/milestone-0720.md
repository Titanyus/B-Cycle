# B-Cycle 0.7.20 – Test-APK

Stand: 15.10.2026, Basis 34f094a.

Version 0.7.20 / versionCode 58, Android 13+, Compile-/Target-SDK 36.
APK: `downloads/B-Cycle-0.7.20-test.apk`, 1.177.554 Byte.
SHA-256: `d460c204a4b8b5bf2373db91eef9382ca47953875f61917245b8c090bf5867c5`.
Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.
Gleicher Testschlüssel wie 0.7.19, als Update installierbar.

## Änderungen

12 dp deckende Trennung oberhalb des Kategorieheaders bleiben auch beim
Andocken erhalten. Zentraler Sticky-Z-Wert liegt über nativen Buttons auch
im gedrückten Zustand; kein zusätzlicher Schatten. Sensor-Aktivierungsschalter
verwenden die gemeinsame Abschnittsschrift (23 sp / 900 Black), ohne doppelte
Überschriften. Keine Änderung an Sensorfunktionen, FIT oder Berechnungen.

## Prüfung

3 gezielte Tests aus BicycleStickySectionsTest und BikesViewTest bestanden:
Andocken/Verdrängen/Abschnittsende in zwei Hochformatbreiten, native Draggesten,
Themewechsel, Linien, Auswahlrahmen, Sensor-Schriften, Bike-/FIT-Bedienung sowie
native Pixelprüfung des oberen Abstands und der Verdeckung eines erhöhten,
gedrückten Suchbuttons. Keine vollständige Testsuite ausgeführt.

Release-Build mit R8, Resource-Shrinking und Lint-Vital erfolgreich (1 min 2 s
mit erstem gezielten Testlauf). Abschließender Testlauf nach Bereinigung einer
veralteten Test-API und Ergänzung des Vorschaubilds: 16 s, ohne Compilerwarnung.
APK-Signatur, Zertifikat, ZIP-Ausrichtung (16 KiB), Paket-, Versions- und
SDK-Metadaten geprüft. Keine Installation auf einem realen Gerät getestet.

[Sensoren mit Sticky Header](previews/bikes-sticky-sensors.png).
