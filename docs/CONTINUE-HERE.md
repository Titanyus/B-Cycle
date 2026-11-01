# Aktueller Stand: APK0.8.75 / Build116 veröffentlicht –31.10.2026

[APK direkt](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.75-release-116.apk) · [Buildnachweis](build-116.md) · [Änderungen](release-0.8.75.md). Code145, Quelle`e8d3286b2a2037e1f7241ae54800e391e7eb47b7`, 2566981Bytes, SHA-256`5ce2bf3d221586e42dd4c12417e1ad0d1a3bd0aebaadf7afed12f533e33ae373`. CI-Prüfsumme, Signatur/Update-Kompatibilität, Manifest, Version und ZIP-Ausrichtung geprüft. APK-Auftrag erledigt. Intervals-HRV422 nach verbesserter Schlafdauer-/Fehlerbehandlung noch live zu bestätigen.

## Lokaler Stand nach Build116 – 01.11.2026

Auftrag erledigt, noch unveröffentlicht: Untere Verteilungsdiagramme Puls→Leistung (`24aafcd`), jetzt zusätzlich getrennte Einstellungsziele „Gefahrene Strecken“ und „Streckenvorlagen“ auf derselben Bibliothek. Fester interner Scope, kein Streckenart-Filter in beiden Listen, getrennt gespeicherte Filter/Sortierung inkl. Einstellungssicherung. Bestehende Import-Zuordnung sowie drei gemeinsame Kartenplätze erhalten; GPX/FIT für beide Ziele. Details: [Sammlungen](route-collections.md).

Vorlagen: Datum/Uhrzeit gleich groß nebeneinander, Herkunft „Erstellt am:“ bei GPX-Zeit sonst „Hinzugefügt am:“, Distanz und geschätzte Zeit. Gruppen nur Anzahl/Distanz. Zeitsortierung/-filter nutzt die Schätzung, Tempooptionen entfallen dort. RecentRidePace speichert das arithmetische Mittel gültiger Fahrt-Durchschnitte der letzten28 Tage ohne Virtual-Rides/Indoor. Nur bei geänderten Fahrtdaten neu berechnen, sonst gespeicherten Wert nutzen. Keine Grundlage→„--“. GPX-Datumsherkunft/Virtual-Flag und FIT-Virtual-Activity-Metadaten rückwärtskompatibel im Index/Backup; Originale unverändert.

Zusatzfehler: Alle vier Zeilenwerte nach echtem Recycling abgeschnitten. RouteListReading auf echte Einzeilenfelder und gezielte Neuvermessung der Wertezeile/Fahrtkarte beim Bind korrigiert. Keine neue eigene Schriftgrößenberechnung; native Größenanpassung bleibt. Nachweis für Datum, Distanz, Fahrzeit, Tempo und Einheiten in320/360/420dp nach weiten Scrollsprüngen: `route-reading-bind-remeasure-final-tests.log` erfolgreich.

Prüfung:75 fachlich gezielte Fälle für Sammlungen/Metadaten/Import/Backup/Filter/Ansichten;72 zunächst erfolgreich. Danach auf Nutzerwunsch nur offene oder nachgebesserte Fälle erneut geprüft: vier erfolgreiche Einstellungs-/Navigation-/Datums-/Virtual-Fälle in `route-collections-targeted-final-tests.log`; der verbleibende Recyclingfall anschließend erfolgreich wie oben. Getrennte Filter inkl. Backup gezielt erfolgreich in `route-collection-filter-backup-final-tests.log`. Kein Gesamtlauf, kein physischer Gerätetest. Typografie/Atlas/Diff geprüft. Keine neue APK beauftragt.

## Vorgemerkt, Umsetzung wartet auf Tagesscreen-Gestaltung

Nächster Nutzerbeitrag soll die HRV-Tagesscreen-Darstellung festlegen. Noch keine Implementierung dieses HRV-Umbaus: Messung nur Kreis/Diagramm mit24dp mehr Abstand und ohne unteren Button; danach automatisch Tagesfragen, deren unterer Button zur gemeinsamen Auswertung führt. Anfangs nur Tageswert/Empfehlung; Textbutton „Zur genauen Auswertung“. Tagesbereich bleibt zuerst stehen, Detailbereich kommt von unten bis auf den vorgesehenen Abstand heran, schiebt dann den Tagesbereich wie bei einem Sticky-Header nach oben hinaus. Ziel: Detailauswertung mit oberem Abstand vollständig sichtbar. Danach beide Bereiche gemeinsam vertikal scrollbar. Berechnung/Empfehlungen/Diagrammfunktionen erhalten. [Genaue Vorgaben](hrv-measurement-flow-revision.md).

## Offenes und wichtige Vorarbeit

Intervals-HRV422 betrifft den historischen Tages-PUT, erster Messtag2026-08-28; heutiger Auto-Upload funktionierte. In Build115 unbekannte Schlafdauer bei gleichen Bett-/Aufwachzeiten nicht mehr als0 ableiten und sichere Feldfehlerdiagnose erweitert. Konkreten Backendfehler erst nach erneutem Liveversuch als behoben bezeichnen. [Details](intervals-hrv.md).

Build115 enthält BackupWork/dataSync-FGS/Wake-Lock für Sicherung/Restore bei Display aus sowie Datum im HRV-Dateinamen. [Sicherung](backup-cloud-reorganization.md). Original-Bosch-FIT: gültige Fahrdaten trotz GPS-Ausfall; spärliche Kanäle korrekt, tatsächliche lange Höhenlücke nicht füllen, kaputten mehrstufigen GPX-Export ausdrücklich nicht reparieren. [FIT](fit.md).

Build116: TERTIARY-Detailkopf randfüllend mit8dp unten, Fahrtname linksbündig, offene Puls-Z1 über alle Modelle, runde mitlaufende X-Raster mit Clipping unter festen Randwerten. [Details](release-0.8.75.md).

APK ausschließlichGitHubActions, erst auf Auftrag. Nächste Version voraussichtlich0.8.76/code146/Build117. Dauerhafte Signatur und direktenRawDownload prüfen; Appcode nie mit[skip ci] überspringen. Snapshot vorBereinigung: `vor-den-bereinigungen-2026-10-30`.
