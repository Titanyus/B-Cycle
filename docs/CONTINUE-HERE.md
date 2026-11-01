# Aktueller Stand: APK0.8.76 / Build117 veröffentlicht –01.11.2026

[APK direkt](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.76-release-117.apk) · [Buildnachweis](build-117.md) · [Änderungen](release-0.8.76.md). Code146, Quelle`87a336a4fd262a8d0753e7afb78253b0e1b4eeab`, 2584637Bytes, SHA-256`eedf59f87207db93990c3381a70494b42c1c37ea2df4f306279db9de238878fa`. GitHubActions erfolgreich; CI-Prüfsumme, dauerhafte Signatur/Update-Kompatibilität, Manifest, Version und ZIP-Ausrichtung geprüft. Tatsächlicher direkter Raw-Download erneut geladen und binär identisch mit Actions-Artefakt. APK-Auftrag erledigt.

## In Build117 enthalten

- Gefahrene Strecken/Streckenvorlagen auf derselben Sammlung, feste Ansichten ohne sichtbaren Streckenartfilter, getrennte Filter/Sortierung inkl. Backup, gemeinsame drei Kartenplätze. Vorlagen: Zeitstempelherkunft, Distanz, geschätzte Zeit aus gespeichertem arithmetischem Mittel der Fahrt-Durchschnitte der letzten28 Tage ohne Virtual-Rides/Indoor; kein Wert→„–“. Import-/Virtual-Metadaten rückwärtskompatibel, Originale erhalten. [Sammlungen](route-collections.md).
- Vier Listenwerte nach Recycling neu vermessen ohne eigene Schriftgrößenberechnung; untere Streckendetail-Verteilungen Puls vor Leistung.
- HRV-Tagesscreen mit150°-Bögen, festen Korridormarkern, gemeinsamem ruhenden Glow und freier Empfehlung. PNS aus persönlich standardisiertem lnRMSSD, Aktivierung aus standardisiertem Ruhepuls; mindestens14 vergleichbare gültige Vortage innerhalb60 Tagen. Keine isolierte SNS-/Stressmessung, fehlende Referenz/Streuung→„–“. Bestehende Empfehlung/Korridore unverändert. [Formeln](hrv-autonomic-index-proposal.md), [Gestaltung](hrv-day-screen-design.md).
- Messung:24dp mehr Abstand, untere Navigation entfernt, nach gespeichertem Abschluss automatisch Tagesfragen und gemeinsame Tages-/Verlaufsauswertung. Textbutton fährt Verlauf zum oberen Rand, Tagesansicht bleibt bis zum Kontakt stehen und wird dann weggeschoben. [Ablauf](hrv-measurement-flow-revision.md).
- Ein gemeinsamer Tag für Datum, Empfehlung, Werte und Tagesaktionen; frischer Einstieg/Messabschluss heute, fehlende heutige Werte nicht durch alte ersetzen. Revision gegen verspätete Workerantworten. Datumstitel ohne Präfix, drei Footeraktionen mit zentralem Schalter, initial verborgen. Kontextlegende außer Standard mit echten Quadraten aus derselben Palette und korrekten Endpunkten. [Legende](hrv-context-colors.md).
- Dashboard folgt kurzem horizontalem Drag direkt; längerer Drag/kurzer schneller Flick übernimmt, maximal300ms ab aktueller Position; Abbruch180ms zurück. Footer fest, genau eine MapView, Karten-/Vertikalgesten und Editor erhalten. [Gesten](dashboard-page-transition.md).

## Gezielte Prüfnachweise

Sammlungen: zunächst75 gezielte Fälle,72 erfolgreich; danach nur offene/angepasste Fälle erneut erfolgreich in `route-collections-targeted-final-tests.log`, `route-reading-bind-remeasure-final-tests.log`, `route-collection-filter-backup-final-tests.log`. HRV:12 Fälle in `hrv-day-revision-tests.log`, vier nachgebesserte UI-/Ablauffälle in `hrv-day-revision-final-tests.log`, einzelner Scrollfall in `hrv-evaluation-section-final-test.log`. Gemeinsamer Tag/Footer: sieben Fälle in `hrv-shared-day-footer-tests.log`, nach Anpassung zweier überholter Erwartungen beide erfolgreich in `hrv-footer-legend-final-tests.log`; Legendenfall danach erfolgreich in `hrv-context-legend-native-test.log`; Integration inkl. Screenreader-Leerwerten zuletzt erfolgreich in `hrv-shared-day-final-test.log`. Dashboard: sechs der neun Fälle sofort erfolgreich, drei neue Dragfälle nach Korrektur erster Move-Verarbeitung und passender Android-Touchschwelle erfolgreich in `dashboard-direct-drag-final-tests.log`. Typografie/Atlas/Diff und native Vorschauen Flat/Glow320/360dp geprüft. Kein Gesamtlauf oder physischer Gerätetest.

## Offen und Vorarbeit

Intervals-HRV422: historischer Tages-PUT, erster Messtag2026-08-28; heutiger Auto-Upload funktionierte. Seit Build115 unbekannte Schlafdauer bei gleichen Zeiten nicht als0 übertragen, sichere Feldfehlerdiagnose verbessert. Erst nach Livebestätigung als gelöst bezeichnen. [Intervals](intervals-hrv.md).

Build115 enthält BackupWork/dataSync-FGS/Wake-Lock für Sicherung/Restore bei Display aus und Datum in HRV-Dateinamen. [Backup](backup-cloud-reorganization.md). Bosch-FIT: gültige Fahrdaten bei GPS-Ausfall erhalten, spärliche Kanäle korrekt, echte lange Höhenlücke nicht füllen; kaputten mehrstufigen GPX-Export ausdrücklich nicht reparieren. [FIT](fit.md).

Basis Build116: randfüllender TERTIARY-Detailkopf mit8dp unten, linksbündiger Fahrtname, offene Puls-Z1, runde mitlaufende X-Raster und Clipping unter festen Randwerten. [Build116](build-116.md).

Keine weiteren offenen Implementierungsaufträge. Neue APK ausschließlich auf Auftrag über GitHubActions. Nächste Version voraussichtlich0.8.77/code147/Build118. Dauerhafte Signatur und tatsächlichen direkten Raw-Download prüfen; Appcode nie mit[skip ci] überspringen. Snapshot vor Bereinigung: `vor-den-bereinigungen-2026-10-30`.
