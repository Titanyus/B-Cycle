# Test-Build 19 · 17.10.2026

[**APK herunterladen**](https://raw.githubusercontent.com/Titanyus/B-Cycle/test-build-19-apk/downloads/B-Cycle-0.7.30-test-build-19.apk)

## Korrekturen seit Test-Build 17

- Fahrradauswahl nach langem Drücken auf den Aufnahmebutton öffnet ausdrücklich oberhalb des Buttons, auf den sichtbaren Bildschirmbereich begrenzt. Keine automatische Dropdown-Platzierung unterhalb der unteren Toolbar.
- Feldauswahl verwendet nur einen äußeren Scrollbereich. Die Datenliste wächst vollständig statt einer festen inneren Höhe; Graph und Trainingshilfe folgen darunter. Alle Werte sind erreichbar, die Trainingshilfe schneidet die Liste nicht mehr ab.
- Kategorienwechsel und sofortige Speicherung bleiben erhalten; Auswahl bleibt geöffnet, bis der Nutzer zurückgeht.

## Herkunft und Prüfung

[Erfolgreicher GitHub-Actions-Build 19](https://github.com/Titanyus/B-Cycle/actions/runs/39008889046), Quellstand `685809c5f74779540d8c1e54c3afb46170631237`.
**25 gezielte UI-Tests erfolgreich**, einschließlich Position der Fahrradauswahl am unteren Bildschirmrand, vollständiger Datenliste vor Trainingshilfe, Kategorienwechsel, Persistenz und Dashboard-Bedienung.
Die vollständige Suite mit 571 Tests war auf dem vorigen Build 17 erfolgreich; für diese Korrekturen wurden gezielt betroffene Tests ausgeführt.

Die APK wurde von GitHub Actions gebaut und anschließend ausschließlich mit dem bisherigen Testschlüssel nachsigniert. Kein lokaler APK-Build.
App-Version **0.7.30**, versionCode **68**. Als Update über die bisherigen mit diesem Testschlüssel signierten APKs installierbar.
Signatur und 16-KiB-ZIP-Ausrichtung geprüft. Kein zusätzlicher Gerätetest in dieser Umgebung.

Größe: 8,152,926 Bytes.

SHA-256: `a7367e4409aa077b4476fd01bfb588e2643d070f8e86060fe4bffd70dd7aa934`

Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.
