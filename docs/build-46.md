# B|CYCLE 0.8.9 – optimierte APK

Build 46 vom 19.10.2026, versionCode 79.

**[APK herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/v0.8.9/downloads/B-Cycle-0.8.9.apk)**

## Änderungen

- Filter-/Sortierhauptfenster und Auswahlbuttons SECONDARY. Jede Gruppe öffnet ein eigenes natives Unter-Dropdown mit TERTIARY-Fläche und gemeinsamem Rahmen/Rundungswert. Normale Buttons und Texteingaben darin SECONDARY.
- Dropdowns wie in den Profilen am Auswahlbutton verankert; der auslösende Titel bleibt frei. Unterfenster schließt bei außerhalb Tippen oder Zurück; Hauptfenster schließt auch sein Unterfenster.
- Fensterhöhe nach Inhalt und verfügbarem Platz. Feste 320-dp-Sortiergrenze entfernt, endgültige Controlabstände vor Messung anwenden. Kleine Sortierauswahlen benötigen keinen künstlichen Scrollbereich.
- Zusätzlichen gefüllten Rahmen hinter der Fahrradtyp-Segmentreihe entfernt. Auswahlzustände und unmittelbare Filter-/Sortieranwendung erhalten.
- [Aktuelle Konventionen](route-query-and-accordions.md).

## Prüfung

Sechs gezielte Tests bestanden: Filter-/Sortierbedienung, Bereichseingaben und ungültige Entwürfe, Slider-Synchronisierung, Mehrfachauswahl, Tastatureingabe, Flächenrollen, Abstände, inhaltsgerechte Sortierhöhe, Schließen der Unterfenster, Query-Kombinationen und stabile Sortierung. Kein vollständiger Testlauf und kein physischer Gerätetest. Versionserhöhung danach ohne funktionale Änderungen.

[GitHub Actions erfolgreich](https://github.com/Titanyus/B-Cycle/actions/runs/39213448527), Quellstand 3f732fcbb8943ac530482dda1d4178bdd4151829. Optimierter Release mit R8 und Ressourcenverkleinerung, kein lokaler APK-Build. Artefakt-Prüfsumme geprüft; mit bisherigem Testschlüssel nachsigniert. Zertifikat, Version 0.8.9/79, nicht-debuggable Manifest und 16-KiB-Zip-Ausrichtung geprüft.

- Größe: 1,862,820 Bytes.
- SHA-256: `12158deb621676e624ef062a85f2e5e193de868b68dfb91fb0586888b9429cb3`.
- Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.
- [Prüfsumme](../downloads/B-Cycle-0.8.9.apk.sha256) · [R8-Mapping](../downloads/B-Cycle-0.8.9-mapping.txt.gz).
