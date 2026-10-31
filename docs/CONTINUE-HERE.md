# Aktueller Stand: APK0.8.70 / Build111 veröffentlicht – 30.10.2026

[APK direkt](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.70-release-111.apk) · [Buildnachweis](build-111.md) · [Änderungen](release-0.8.70.md). Code140, Quelle`8c74a236554af2b4361e7b51e52d2c8ea4138b2a`, 2540981Bytes, SHA-256`159293c006204c600abb966e910bc5e9ae8977a6515bf7b9e5da0e88650932dd`. CI-SHA, Signatur/Update-Kompatibilität, Manifest und ZIP-Ausrichtung geprüft. Alle aktuellen Aufträge abgeschlossen, kein offener Buildauftrag.

## Enthalten seit Build110

- Drag-and-drop-Kacheln kehren nach Ablegen/Abbruch/neuem Drag/Detach auf volle Größe zurück; Einrasten nachDRAG_ENDED.
- [Dashboard/Summary](dashboard-summary-adjustments.md): Statuslinks25/rechts25+4Privacy; Licht bei Androidstatus oben0/rechts16. Schaltwerk-Akku bei Stopp festhalten und links vor Bike-Akku; Summary-Warnfarben inklusive20/10%. Footer: untere mittlere Dreierkachel ohne Karte12dp höher. Menütrenner32dp je Seite,27dp vor Bild nachErweitert. Aufzeichnungs-/Schaltwerk-Accordions kompakter; normale Sensoraktionen.
- [Intervals-HRV](intervals-hrv.md): monatlicherGET aufwellness.json, Nulltag, konkrete Fehler mitTag/HTTP-Code. Fremde HRV erhalten. Nutzer bestätigt FIT-Zugang (400Uploads); privater HRV-Live-Test nicht möglich.
- [Windrose](map-motion.md): Schwarz70%,24-dp-Iconfläche am Vergrößern-Symbol, nurFolgen+Buttonsunsichtbar, tatsächliche Kartenrotation. Profiloption standardaus und mitgesichert. Original-Innenkreis(45.3,46.9) stattSVG-Flächenmitte; quadratischer93.8-Vektor mittranslateX1.6 bringt Kreis exakt aufDrehpunkt. Smartphone-Pfeil bleibt getrennt vonGPS-Kurs; keine Änderung am Kompass.
- [Streckendetaildiagramme](route-analysis-navigation.md): gemeinsamerX-Zoom bis64×, Pan/OverScroller wieHRV, direkte Pointerdistanz ohne Mindestspanne, stabileY-Achsen, gecachte Kurvenpfade/Clipping. Kadenzverdichtung erhält echte zusammenhängende Tretabschnitte und echteLücken, max4096 Punkte; Kennzahlen/Rohdaten unverändert.
- [Streckenfilter](three-map-tracks.md) ganz oben: alle3 zugewiesenen Plätze unabhängig von Sichtbarkeit.

## Prüfung und Historie

Logs unter`/workspace/.tools/`: tile-reorder-scale-tests (7), ui-summary-hrv-tests/final-tests (abschließend24), map-wind-rose-tests/final-tests/centre-tests, route-chart-filter-final-tests (41), route-chart-pixels-final-tests (4). Typografie/Atlas/Diff geprüft. Keine vollständige Gesamtsuite, kein physischer Gerätetest. Screenshot-Fahrt61,5km nicht als Datei vorliegend; Kadenzfehler mit gezielter16k/80-Abschnitte-Regression und100k-Budgettest geprüft.

Build110 bleibt [hier](build-110.md), Snapshot vor Bereinigung als Tag`vor-den-bereinigungen-2026-10-30` aufGitHub. Alte Übergabe/Regeln imArchiv; aktuelleAGENTS.md beachten. Fachänderungen getrennt committen; APK ausschließlichActions, Veröffentlichung[skip ci].

## Nach Build111 – 31.10.2026

HRV-Messbild: separaten Abschluss-/Endpunktkreis am Fortschrittsbogen entfernt, gemeinsam für Ruhe- und Messphase. Nur der farbige sich füllende Ring bleibt; abgerundete Linienenden erhalten. Ungenutzten hrvRingEndScale-Token entfernt. Typografie/Atlas/Diff geprüft, reine Zeichenänderung; keine neue APK beauftragt.
