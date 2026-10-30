# B|CYCLE 0.8.69

Änderungen seit Build 109:

- Vorherigen Stand als Git-Tag [vor-den-bereinigungen-2026-10-30](https://github.com/Titanyus/B-Cycle/tree/vor-den-bereinigungen-2026-10-30) gesichert.
- Alte Modus-/Lichteinstellungen, seitlichen Bosch-Zeichenpfad und wirkungslose Schaltwerk-Warnoption entfernt. Alte gültige Sicherungen weiter lesbar, neue Exporte bereinigt. Fahrradfilter-Kachelstil zentralisiert, ungenutzte Texte/Variablen entfernt, aktuelle Entwicklungsregeln und Übergabe von der archivierten Historie getrennt. [Bereinigung](cleanup-2026-10-30.md).
- Dashboardwechsel als gemeinsamer seitlicher 300-ms-Übergang für Daten, Karte und Kartenbuttons; Footer bleibt fest. Eine lebende Karte, kurzer begrenzter RAM-Snapshot, saubere Freigabe/Abbruch, Systemanimationen berücksichtigt. [Details und Vorschau](dashboard-page-transition.md).
- Druckeffekt der Rahmen-Accordions über die gesamte Rahmenfläche, auch bei Sensoren, Fahrtbenennung und Strecken-Aufzeichnung. Schalter unabhängig.
- Trink-Erinnerung sowie Daten/Trendindikator/Graph in der Dashboard-Feldauswahl als Rahmen-Accordions. Trenner vor Trink-Erinnerung entfernt.
- Abstand zwischen Dashboard-Layout-Überschrift und Kacheln vereinheitlicht. Horizontale UI-Trenner 2 dp / 25 % wie die normalen Rahmen.
- Streckendetails ohne GPS ohne Höhen-Kennzahlenzeile. Werte unabhängig von rechts stehender Einheit unter dem Icon zentriert; übrige Wertezeilen behalten ihre Ausrichtung.

Gezielte Native-/Sicherungs-/Migrations-/UI-Prüfungen erfolgreich, darunter alter Backupbestand, neue Exporte, Frame-Ripple, Sensor-/Graph-/Trend-/Trinkbedienung, 300-ms-Zwischenstände und Abbruch/Freigabe, tatsächlicher Activity-Wechsel mit/ohne Karte bei fester Footerposition, Editor-Persistenz und Detailwertausrichtung. Typografie und UI-Katalog/Diff geprüft. Keine vollständige Gesamtsuite und kein neuer physischer Gerätetest. Prüfprotokolle in den Detaildokumenten.

[Buildnachweis und Download](build-110.md).
