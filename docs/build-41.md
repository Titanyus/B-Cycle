# B|CYCLE 0.8.6 – optimierte APK

Build 41 vom 19.10.2026, versionCode 76.

**[APK herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/v0.8.6/downloads/B-Cycle-0.8.6.apk)**

## Änderungen

- Auswahlkreise in kompakteren Zeilen mit derselben Typografie, Polsterung und Mindesthöhe wie Dropdownzeilen; keine zusätzlichen Abstände zwischen benachbarten Auswahlzeilen. Abstand zu anderen Elementen bleibt erhalten.
- Texteingaben zentral mit Bebas Neue/settingsMetricSp, transparentem Hintergrund und 1-dp-Rahmen in FOREGROUND mit 50 % Deckkraft sowie 6-dp-Rundung. Native Eingabetypen und Passwortschutz erhalten.
- Filter und Sortierung als kompakte Accordions wie in der Feldauswahl; Fensterhöhe passt sich dem Inhalt an, bleibt begrenzt und scrollbar. Kein Zurückbutton; außerhalb tippen oder Android-Zurück schließt.
- Streckenarten als mehrfach auswählbare Segmente; Fahrradfilter als Profilkacheln mit maximal drei Spalten.
- Distanz, Bewegungszeit und Durchschnittsgeschwindigkeit mit synchronen Zwei-Griff-Slidern und Von-/Bis-Eingaben. Skalen aus den vorhandenen Fahrten, größere Texteingaben erweitern sie. Getrennte Accessibility-Aktionen für beide Griffe und Tastaturbedienung.
- Kleine Überschrift Trackdarstellung; beide Linien-Accordions mit Kartenvorschau, quadratischem Farbfeld und Hexwert sowie direkten Reglern für Deckkraft und Breite.
- Native feste Farbpalette mit 61 Spektrumfarben und neun Graustufen. Keine HSV-/Deckkraft-Regler oder Zahlenwerte im Palette-Dialog, keine zusätzliche Bibliothek. Auswahl sofort übernehmen und schließen.

## Prüfung

Gezielte Tests für Filterlogik, Popup-Navigation, Mehrfachauswahl, sechs Fahrradkacheln in drei Spalten, gekoppelte Zahlen-/Sliderwerte, nicht kreuzende Griffe und Accessibility-Aktionen bestanden. Kompakte Auswahlzeilen direkt mit gemessenen Dropdownzeilen verglichen. Farbpalette, ungültige Hexwerte, getrennte Linienoptionen, Profilübernahme/Sicherung sowie native Tastatur und bestehende Trainingshilfe gezielt geprüft. Kein vollständiger Testlauf und kein physischer Gerätetest.

[GitHub Actions erfolgreich](https://github.com/Titanyus/B-Cycle/actions/runs/39204932821), Quellstand 13688e6692357a6ab4c7924d91ab6ea87005deb2. Release mit R8 und Ressourcenverkleinerung; kein lokaler APK-Build. Artefakt-Prüfsumme geprüft, mit bisherigem Testschlüssel nachsigniert; Zertifikat, Version 0.8.6/76, nicht-debuggable Manifest und 16-KiB-Zip-Ausrichtung geprüft.

- Größe: 1,854,628 Bytes.
- SHA-256: `ecfbc84b94ec1cf0c5137d1fad9d4dbb413936c2bc6f9c76e7edf228b6db2cd7`.
- Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.
- [Prüfsumme](../downloads/B-Cycle-0.8.6.apk.sha256) · [R8-Mapping](../downloads/B-Cycle-0.8.6-mapping.txt.gz).
