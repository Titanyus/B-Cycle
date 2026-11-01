# Aktueller Stand: APK0.8.75 / Build116 veröffentlicht –31.10.2026

[APK direkt](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.75-release-116.apk) · [Buildnachweis](build-116.md) · [Änderungen](release-0.8.75.md). Code145, Quelle`e8d3286b2a2037e1f7241ae54800e391e7eb47b7`, 2566981Bytes, SHA-256`5ce2bf3d221586e42dd4c12417e1ad0d1a3bd0aebaadf7afed12f533e33ae373`. CI-Prüfsumme, Signatur/Update-Kompatibilität, Manifest, Version und ZIP-Ausrichtung geprüft. APK-Auftrag erledigt. Intervals-HRV422 nach verbesserter Schlafdauer-/Fehlerbehandlung noch live zu bestätigen.

## Lokaler Stand nach Build116 – 01.11.2026

Auftrag erledigt, noch unveröffentlicht: Untere Verteilungsdiagramme Puls→Leistung (`24aafcd`), jetzt zusätzlich getrennte Einstellungsziele „Gefahrene Strecken“ und „Streckenvorlagen“ auf derselben Bibliothek. Fester interner Scope, kein Streckenart-Filter in beiden Listen, getrennt gespeicherte Filter/Sortierung inkl. Einstellungssicherung. Bestehende Import-Zuordnung sowie drei gemeinsame Kartenplätze erhalten; GPX/FIT für beide Ziele. Details: [Sammlungen](route-collections.md).

Vorlagen: Datum/Uhrzeit gleich groß nebeneinander, Herkunft „Erstellt am:“ bei GPX-Zeit sonst „Hinzugefügt am:“, Distanz und geschätzte Zeit. Gruppen nur Anzahl/Distanz. Zeitsortierung/-filter nutzt die Schätzung, Tempooptionen entfallen dort. RecentRidePace speichert das arithmetische Mittel gültiger Fahrt-Durchschnitte der letzten28 Tage ohne Virtual-Rides/Indoor. Nur bei geänderten Fahrtdaten neu berechnen, sonst gespeicherten Wert nutzen. Keine Grundlage→„--“. GPX-Datumsherkunft/Virtual-Flag und FIT-Virtual-Activity-Metadaten rückwärtskompatibel im Index/Backup; Originale unverändert.

Zusatzfehler: Alle vier Zeilenwerte nach echtem Recycling abgeschnitten. RouteListReading auf echte Einzeilenfelder und gezielte Neuvermessung der Wertezeile/Fahrtkarte beim Bind korrigiert. Keine neue eigene Schriftgrößenberechnung; native Größenanpassung bleibt. Nachweis für Datum, Distanz, Fahrzeit, Tempo und Einheiten in320/360/420dp nach weiten Scrollsprüngen: `route-reading-bind-remeasure-final-tests.log` erfolgreich.

Prüfung:75 fachlich gezielte Fälle für Sammlungen/Metadaten/Import/Backup/Filter/Ansichten;72 zunächst erfolgreich. Danach auf Nutzerwunsch nur offene oder nachgebesserte Fälle erneut geprüft: vier erfolgreiche Einstellungs-/Navigation-/Datums-/Virtual-Fälle in `route-collections-targeted-final-tests.log`; der verbleibende Recyclingfall anschließend erfolgreich wie oben. Getrennte Filter inkl. Backup gezielt erfolgreich in `route-collection-filter-backup-final-tests.log`. Kein Gesamtlauf, kein physischer Gerätetest. Typografie/Atlas/Diff geprüft. Keine neue APK beauftragt.

## HRV-Umbau umgesetzt – 01.11.2026

Persönliche Indizes vom Nutzer bestätigt: PNS standardisiert lnRMSSD, Aktivierungsindex standardisiert Ruhepuls; mindestens14 gültige vergleichbare Vortage innerhalb60 Kalendertagen. Keine isolierte SNS-/Stressmessung behaupten, fehlende Referenz/Streuung→„–“, Zahlen unbegrenzt. Formeln/Grenzen in In-App-Hilfe und [Fachdokument](hrv-autonomic-index-proposal.md). Bestehende Tagesempfehlung und HRV-/Puls-Korridore unverändert.

Tagesscreen mit150°-Bögen, HRV doppelt dick, weißem Puls, festen Korridormarkern bei25/75%, gemeinsamen ruhenden Glow bei Messscreen-Spitzenintensität. PNS-/Aktivierungsbalken von0 aus, neutral ohne neue Warnschwellen. Freie Empfehlung mit24dp Abstand. [Gestaltung](hrv-day-screen-design.md).

Messung:24dp zusätzlich zwischen Kreis/Diagramm, keine untere Navigation/Ergebniskarten mehr. Start/Abbruch/Speicherwiederholung im Kreis bleibt. Nach gespeicherter abgeschlossener Messung automatisch Tagesfragen, danach Tagesauswertung mit Textbutton „Zur genauen Auswertung“. Gemeinsame native Scrollfläche: Tagesbereich bleibt bis zum Kontakt stehen, Verlauf schiebt ihn dann heraus, beide gemeinsam scrollbar; kleine Displays bleiben bedienbar. Ein gemeinsamer ausgewählter Tag steuert inzwischen Kopf, Tagesansicht und Tagesaktionen; Titel nur Datum, frischer Einstieg immer heute (auch ohne heutige Messung). [Ablauf](hrv-measurement-flow-revision.md).

Gezielt12 HRV-Fälle erfolgreich in `hrv-day-revision-tests.log` (Indizes/Referenzen/Empfehlungen/Native-UI/Messdarstellung/Ablauf). Nach Kopf-/Scrollabsicherung nur vier betroffene UI-/Ablauffälle erneut erfolgreich in `hrv-day-revision-final-tests.log`. Native Vorschau320/360dp in Flat/Glow visuell geprüft. Zusätzlich einzelner Scrollfall nach Begrenzung der Kopfaktualisierung auf tatsächliche Bereichswechsel erfolgreich in `hrv-evaluation-section-final-test.log`. Kein Gesamtlauf, kein physischer Gerätetest, keine neue APK.

## HRV-Nachbesserung: gemeinsamer Tag, Footer und Legende

Redundanten Footerpunkt Auswertung entfernt, drei Tagesaktionen bleiben. Gemeinsamer zentraler Footer-Schalter im Kopf, initial verborgen, Tagesklick darf temporär einblenden. Immer gemeinsame Seite auch bei Einstieg über Einstellungen: initial heute, ebenso nach Messung/Fragen. Fehlt heutige Messung, leere Tageswerte und keine älteren Daten/Aktionen als Ersatz. Datumsbasierter Worker lädt Empfehlung/Puls/Indizes bei Tagesauswahl mit; Revision schützt vor verspäteten Antworten bei schneller Auswahl. Bearbeiten/Löschen aktualisiert denselben Tag. Datumstitel überall ohne Auswertungspräfix, kein Titelwechsel beim vertikalen Scrollen. Überholte Abschnittstitel-Callbacks/Navigation entfernt; gemeinsame Hilfe enthält Tages- und Verlaufserklärungen.

Unter Kontextauswahl inline Farblegende für alle Modi außer Standard: Name · Endpunkt, echte farbige Quadrate, Endpunkt. Palette direkt aus Diagrammzuordnung, Training vier, sonst fünf Farben; positive Schlaf-/Energiewerte in passender Richtung beschriftet. [Legende](hrv-context-colors.md), [Ablauf](hrv-measurement-flow-revision.md).

Gezielt sieben Footer-/Tages-/Navigations-/Ablauf-/Scrollfälle in `hrv-shared-day-footer-tests.log`: fünf erfolgreich, zwei alte Erwartungen an Hilfe/Initialauswahl angepasst. Diese beiden danach erfolgreich in `hrv-footer-legend-final-tests.log`. Zusätzlicher Native-Legendenfall für alle Modi bei320/360dp in Flat/Glow: nach Anpassung an bereits bestehende Abdunklung des gewählten Balkens erfolgreich in `hrv-context-legend-native-test.log`, Vorschau visuell geprüft. Anschließend einzelner Tages-/Footer-Integrationstest inkl. Screenreader-Leerwerten erfolgreich in `hrv-shared-day-final-test.log`. Hilfe-/Bestätigungstexte und XML-Zuordnung geprüft. Kein Gesamtlauf oder physischer Gerätetest, keine neue APK beauftragt.

## Dashboard-Wischverhalten umgesetzt – nach HRV

Kurzer klar horizontaler Drag auf Datenfeldern folgt direkt dem Finger; Übernahme bei48dp/15% der Breite (kleinerer Wert, mindestens3×Touchslop), alternativ kurzer schneller Flick beim Loslassen. Automatische Animation setzt ohne Sprung an dieser Position fort, maximal300ms; kurzer/abgebrochener Drag180ms zurück ohne Seitenwechsel/Bitmap. Footer fest; Karten-/Vertikalgesten/Editor unverändert, genau eine MapPanel-Instanz. Abbruch/Fokusverlust/Detach und Animation aus bleiben erhalten. [Details](dashboard-page-transition.md).

Neun gezielte Gesten-/Bestandsfälle in `dashboard-direct-drag-tests.log`: sechs erfolgreich, drei neue Dragfälle zunächst offen. Erste Move-Verarbeitung korrigiert, tatsächliche Test-Touchschwelle16px berücksichtigt (Testdrag20px). Nur diese drei erneut erfolgreich in `dashboard-direct-drag-final-tests.log`. Kein Gesamtlauf/physischer Gerätetest. Typografie/Atlas/Diff geprüft. Keine APK beauftragt.

## Offenes und wichtige Vorarbeit

Intervals-HRV422 betrifft den historischen Tages-PUT, erster Messtag2026-08-28; heutiger Auto-Upload funktionierte. In Build115 unbekannte Schlafdauer bei gleichen Bett-/Aufwachzeiten nicht mehr als0 ableiten und sichere Feldfehlerdiagnose erweitert. Konkreten Backendfehler erst nach erneutem Liveversuch als behoben bezeichnen. [Details](intervals-hrv.md).

Build115 enthält BackupWork/dataSync-FGS/Wake-Lock für Sicherung/Restore bei Display aus sowie Datum im HRV-Dateinamen. [Sicherung](backup-cloud-reorganization.md). Original-Bosch-FIT: gültige Fahrdaten trotz GPS-Ausfall; spärliche Kanäle korrekt, tatsächliche lange Höhenlücke nicht füllen, kaputten mehrstufigen GPX-Export ausdrücklich nicht reparieren. [FIT](fit.md).

Build116: TERTIARY-Detailkopf randfüllend mit8dp unten, Fahrtname linksbündig, offene Puls-Z1 über alle Modelle, runde mitlaufende X-Raster mit Clipping unter festen Randwerten. [Details](release-0.8.75.md).

APK ausschließlichGitHubActions, erst auf Auftrag. Nächste Version voraussichtlich0.8.76/code146/Build117. Dauerhafte Signatur und direktenRawDownload prüfen; Appcode nie mit[skip ci] überspringen. Snapshot vorBereinigung: `vor-den-bereinigungen-2026-10-30`.
