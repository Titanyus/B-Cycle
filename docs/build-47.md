# B|CYCLE 0.8.10 – optimierte APK

Build 47 vom 19.10.2026, versionCode 80.

**[APK herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/v0.8.10/downloads/B-Cycle-0.8.10.apk)**

## Änderungen

- Streckenart als unabhängige Mehrfachauswahl „Importiert / Gefahren“, daneben eigener Filter Radtyp „Bike / E-Bike / Indoor“. Innerhalb einer Gruppe ODER, zwischen Gruppen UND. Eigene ältere Fahrten ohne Radtyp zählen zu Gefahren.
- Gemeinsamer dezenter Rahmen der Segmentauswahl wie bei den Trainingszonen. Zentraler Helfer für Trainingshilfe und Streckenfilter, mit Innenpadding in Rahmenbreite.
- Dynamische Verfügbarkeit nur für Herkunft, Radtyp und Fahrrad einschließlich Ohne Zuordnung. Optionen ohne passende Fahrt unter den übrigen Filtern werden deaktiviert und gedimmt. Die jeweils eigene Gruppe bleibt bei der Verfügbarkeitsprüfung unberücksichtigt; bereits gewählte Optionen bleiben immer abwählbar.
- Datum und Wertebereiche bleiben jederzeit bedienbar; gesetzte Grenzen wirken bei der Verfügbarkeitsprüfung mit. Keine automatische Änderung einer Auswahl.
- Die vorgemerkte Streckendetail-Optik ist weiterhin nicht umgesetzt; siehe [Vormerkungen](pending-ui-changes.md).
- [Filterkonventionen](route-query-and-accordions.md).

## Prüfung

13 gezielte Tests für Filtermodell, native Auswahlbedienung und Streckenmanager bestanden, einschließlich dynamischer Verfügbarkeit, Mehrfachauswahl, unbekannter Radtypen/Profilzuordnung, Datum/Wertebereiche und abwählbarer Optionen ohne Treffer. Kein vollständiger Testlauf und kein physischer Gerätetest. Versionserhöhung danach ohne funktionale Änderungen.

[GitHub Actions erfolgreich](https://github.com/Titanyus/B-Cycle/actions/runs/39215972182), Quellstand 4ace06a6134463b8ff183671cd5b4f4e008487cc. Optimierter Release mit R8 und Ressourcenverkleinerung, kein lokaler APK-Build. Erfolgreiches Artefakt nach erneuertem API-Zugriff heruntergeladen, kein erneuter Build. Artefakt-Prüfsumme geprüft, mit bisherigem Testschlüssel nachsigniert. Zertifikat, Version 0.8.10/80, nicht-debuggable Manifest und 16-KiB-Zip-Ausrichtung geprüft.

- Größe: 1,866,916 Bytes.
- SHA-256: `b42108416909eb8ad8758a916f392f7a52f2180424f9636de58405babf0bb3e7`.
- Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.
- [Prüfsumme](../downloads/B-Cycle-0.8.10.apk.sha256) · [R8-Mapping](../downloads/B-Cycle-0.8.10-mapping.txt.gz).
