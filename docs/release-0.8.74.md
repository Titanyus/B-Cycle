# B|CYCLE 0.8.74

- Sicherung und Wiederherstellung laufen auch bei ausgeschaltetem Bildschirm weiter; HRV-Sicherungsdateien enthalten das Datum im Namen.
- Unbekannte Schlafdauer aus importierten Tagesdaten wird nicht als null Sekunden zu Intervals übertragen. Sichere Fehlerdiagnose für HTTP422 verbessert; Behebung des konkreten Backendfehlers bleibt durch erneuten Upload zu bestätigen.
- Fahrradprofile und Streckendetails erhalten den Footer-Umschalter links neben Info, beim Öffnen standardmäßig ausgeblendet.
- Gemeinsame Gestenfläche über alle vorhandenen Detaildiagramme einschließlich Zwischenräumen; bisheriger Pinch-Zoom und Nachlauf erhalten.
- Detailkopf mit Fahrrad, Datum und Wetter gemeinsam mit TERTIARY-Hintergrund.
- Unabhängige Kadenz-/Höhenaktualisierungen aus Bosch-FIT werden als Kurven dargestellt; echte längere Kanallücken bleiben offen. Eigene Sekundenaufnahmen behalten ihre bisherigen Nullwert-/Lückenregeln. Tempo und Distanz bleiben unabhängig von GPS-Unterbrechungen nutzbar, wenn gültige Fahrdaten vorliegen.

Geprüft: 51 gezielte Tests für Diagramme, Footer und FIT einschließlich lokaler Originaldatei-Prüfung; dauerhafte synthetische Regression für eigene FIT-Aufnahmen mit GPS-Ausfall. Backup-/Restore-/Lifecycle-/Intervals-Prüfungen einschließlich 64-Tage-HRV-Export erfolgreich, zuletzt 16 gezielte Screen-off-/MapState-/Startfehlerprüfungen. Typografie, UI-Atlas und Diff geprüft. Keine vollständige Gesamtsuite, kein physischer Gerätetest und kein privater Liveupload. Private Nutzerdaten und temporäre Tests nicht enthalten.

[Buildnachweis und Download](build-115.md).
