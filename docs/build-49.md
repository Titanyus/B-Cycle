# B|CYCLE 0.8.12 – optimierte APK

Build 49 vom 19.10.2026, versionCode 82.

**[APK herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/v0.8.12/downloads/B-Cycle-0.8.12.apk)**

## Änderungen

- Profilbezogene Trink-Erinnerung unterhalb des Bosch-Fahrmodus, auch ohne Bosch-Anbindung vorhanden. Gemeinsames Schalter-Accordion: Aufklappen schaltet nichts ein.
- Intervall 10–60 Minuten in 5-Minuten-Schritten. Standard aus / 30 Minuten. Schnellzugriff als eigener Abschnitt in der Dashboard-Feldauswahl, unabhängig vom gewählten Datenfeld. Beide Stellen verwenden dieselben Profilwerte.
- Nur während Aufnahme: Hinweis mit Flasche und „Trinken nicht vergessen!“ mittig über der Karte; bei kartenlosem Dashboard im Anzeigebereich. Nach 15 Sekunden oder Antippen schließen, ohne das Intervall zurückzusetzen.
- Laufzeit zählt Aufnahmedauer einschließlich Stillstand. Neustart bei neuer Aufnahme, Ende bei Stopp; keine Warteschlange verpasster Hinweise im Hintergrund. Einstellungen/Editor unterdrücken die Anzeige und holen höchstens einen fälligen Hinweis nach.
- Einstellungen gehören zur Profilübernahme und Sicherung. Profil-/Feldauswahlhilfe ergänzt.
- [Konventionen](drink-reminder.md). Vorgemerkte Streckendetail-Optik weiterhin nicht umgesetzt.

## Prüfung

37 unterschiedliche gezielte Tests über abgeschlossene Läufe bestanden: Timergrenzen, wiederholte Intervalle, Hintergrund/Nachholen, Deaktivierung/Intervallwechsel, native Schalter-/Accordion-/Sliderbedienung, Hinweis-Timeout/Antippen/Abhängen, Dashboardanzeige und Aufnahmeende, Profilübernahme/Wechsel, Design-Sicherung, Hilfe und Feldauswahl. Kein vollständiger Testlauf. Physische Darstellung/Positionierung und Bedienung auf dem Gerät noch zu testen. Versionserhöhung danach ohne funktionale Änderungen.

[GitHub Actions erfolgreich](https://github.com/Titanyus/B-Cycle/actions/runs/39267668973), Quellstand 5cf914a83fba2df4c5cdb15a0ab6d082a61d6446. Optimierter Release mit R8 und Ressourcenverkleinerung, kein lokaler APK-Build. Artefakt-Prüfsumme geprüft, mit bisherigem Testschlüssel nachsigniert. Zertifikat, Version 0.8.12/82, nicht-debuggable Manifest und 16-KiB-Zip-Ausrichtung geprüft.

- Größe: 1,875,164 Bytes.
- SHA-256: `f3418f1479411605f769da25fb4de29a9f7240310fc276eff068e3a6eea8862d`.
- Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.
- [Prüfsumme](../downloads/B-Cycle-0.8.12.apk.sha256) · [R8-Mapping](../downloads/B-Cycle-0.8.12-mapping.txt.gz).
