# B|CYCLE 0.8.45 – Build 85

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.45-release-85.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.45-release-85.apk.sha256)
- [GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/40191128906)
- Quellstand: `6b8395743f08d26661f0575bbb44f25dee560173`
- Version: 0.8.45, versionCode115, Android13+.
- Größe: 2.437.205 Bytes (2,44 MB).
- SHA-256: `d8c1ebb3c635986acee7c619679225b54e442c4bc9c99b5551c61defbff8fb6b`
- Signierzertifikat SHA-256: `94d34c46ede28d12e6d37efd8a7d953a75b621596ec2e5b68f61899b8ea7499b`.

Optimierter Release aus GitHub Actions mit R8 und Ressourcenverkleinerung. CI-Prüfsumme, dauerhafte Signatur wie Build84, Version/Manifest ohne Debug-/Test-Flag und ZIP-Ausrichtung geprüft. Direkter APK-Download ohne ZIP; Update über gleich signierte Vorgänger seit Build53 möglich.

Neue Sensor-Startkarte bei fehlender Hauptzuordnung im gewählten E-Bike-/Indoorprofil, mit profilbezogener Tagespause und vorhandenem Sucheinstieg. HRV-Policyv5: Verbleibende Detektor-Auffälligkeiten allein warnen bei sonst auswertbaren neuen Messungen, statt die Kennzahlen zu sperren. Rohdaten/Korrektur unverändert; bestehende technische Konsistenzregeln bleiben. Keine stille Neubewertung alter Ergebnisse.

Enthält auch alle seit Build84 unveröffentlichten UI-Nachträge: globale Positionskacheln im Fahrradprofilstil, fünf einzelne umsortierbare FIT-Namenskarten mit gemeinsamem Datum-/Zeit-Dragblock und kompatibler Speicherung, Glow-Sticky-Header rundum/8dp angeheftet, TERTIARY-Footer ohne Rahmen. [Alle Änderungen](release-0.8.45.md).

Gezielte Tests erfolgreich: vorherige UI-/Drag-/Lock-/Namen-/Profil-/Sicherungs-/Footer-/Sticky-Prüfungen (62 Fälle mit gezielten Nachprüfungen der neuen8dp-Erwartung), jetzt16HRV-Kerntests und17AppTests für Sensorstart/Tag/Profil/Lifecycle/Suche/HRV-Einstieg/Controller/Speicherung/Resultat. Abschließende Prüfungen von Sensor-Schatten auch inFlat, bestehender Alertdarstellung und HRV-Warnanzeige ebenfalls erfolgreich. Native Vorschauen visuell geprüft; Typografie-/Katalog-/Diffchecks erfolgreich. Kein vollständiger Gesamtlauf, kein BLE-/Gerätetest. Unveränderte synthetische Restartefakt-Fixture mit193,5s RR-Dauer erhält im neuen Policytest ein200s-Protokoll, um Restwarnung isoliert von der weiterhin gültigen RR_DURATION-Prüfung zu testen.

[Vorheriger Download](build-84.md).
