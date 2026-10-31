# Aktueller Auftrag –31.10.2026

Neue APK ausdrücklich beauftragt. Letzter veröffentlichter Stand: [0.8.70/Build111](build-111.md). Aktuelle Änderungen lokal, Veröffentlichung erst nach erfolgreichem Actions-Build und APK-Prüfung.

## Seit Build111

- HRV-Ruhe-/Messring ohne separaten Abschlusskreis, runde Bogenenden erhalten. Commitd67a988.
- [Streckenübersicht](route-list-groups.md): Gruppen nach Monat/Jahr oder vereinbarten Distanz-/Zeit-/Tempobereichen, gefilterte Summen und gewichtetes Tempo. Cached bis Daten-/Sortänderung. Fast-Scroller springt auf Gruppenanfänge (letzte ausdrückliche Nutzerentscheidung nach Chatabsturz); normales Scrollen bleibt stufenlos. Commit5fb97b9.
- [Intervals-HRV](intervals-hrv.md): BereichsGET ohne fields-Liste, EinzelPUT ohne redundantes id-Feld; sichere422-Diagnose mit Anfrageschritt. Nutzer bestätigt erfolgreichen Einzelupload, Ursache des Sammelfehlers nicht privat live verifiziert.
- Wellness ergänzt numerischen Schlafscore0–10, Verletzung nein→1/ja→4 und neue Stressfrage „Wie gestresst bist du?“ mit Wenig/Durchschnittlich/Sehr/Extrem→1/2/3/4. Optional, ohne Standardantwort, proTag gespeichert/vorausgewählt, validiert und HRV-gesichert. FragenauswahlSchema2 migriert einmalig mit neuer Frage; späteres Abschalten bleibt erhalten. Keine Änderung der HRV-Empfehlungen.
- Nutzer genehmigt Sammelergänzung eigener unveränderter HRV-Tage um ausschließlich fehlende sleepScore/stress/injury. TagesGET prüft Eigentum/Fehlfelder erneut; ZusatzPUT ohneHRV. Vorhandene/fremde Werte erhalten, Beleg erst nach bestätigtem PUT ergänzen.
- Fitness/Ermüdung/Form über Intervals als möglich beantwortet, kein Implementierungsauftrag.

## Prüfungen

Gruppen-/Cloudtests40 erfolgreich, native Gruppenvorschau visuell geprüft (`route-groups-hrv-final-tests.log`). Stress/Backup/Fragebogen/Intervals34 erfolgreich (`hrv-wellness-stress-tests.log`). Abschließende36 Ergänzungs-/Fragebogentests erfolgreich unter`/workspace/.tools/hrv-supplement-final-tests.log`. Typografie/Atlas/Diff geprüft. Keine private Liveübertragung, keine vollständige Gesamtsuite oder physischer Gerätetest.

APK nur via GitHubActions, keine lokalen APKs. Dauerhafte Signatur prüfen, CI-SHA/Version/Manifest/ZIP prüfen; veröffentlichen mit[skip ci], direktenRawDownload binär vergleichen. Historie: [Build111](build-111.md), Snapshot vor Bereinigung alsTag`vor-den-bereinigungen-2026-10-30`.
