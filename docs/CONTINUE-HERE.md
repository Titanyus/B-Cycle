# Aktueller Stand: lokale Verfeinerungen nach Build118 –01.11.2026

[APK direkt](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.77-release-118.apk) · [Buildnachweis](build-118.md) · [Änderungen](release-0.8.77.md). Code147, Quelle`e7478861dedc8a66c0f2b1b62edb78e589fd8797`, 2592869Bytes, SHA-256`b2a6128cc2c44140caffd25609fbef81c7587dc96b1301ab861d925a1fd74369`. GitHubActions erfolgreich; CI-Prüfsumme, dauerhafte Signatur/Update-Kompatibilität, Manifest, Version und ZIP-Ausrichtung geprüft. Tatsächlicher direkter Raw-Download erneut geladen und binär identisch mit Actions-Artefakt. APK-Auftrag erledigt.

## Lokal geändert, noch nicht als APK veröffentlicht

- Kurzempfehlung eigener Absatz, restliche Empfehlung ein zusammenhängender Text. HRV-Kacheln gleiche volle Höhe/zentriert; Weitere als schwebendes rechtsbündiges Dropdown über drei Hauptspalten, Auswahl schließt, Weitere bleibt markiert.
- HRV-Messdiagramm315dp (+10%), Abstand zum Kreis36dp (+12).
- Einstellungen „HRV & Tagesbefinden“ direkt nach Fahrradprofile; Erststartangebot einfacher Schalter direkt nach Trenner vor Vibrieren, ohne Erklärung.
- Fahrradprofile/Karten: GPX-Anzeigeschalter entfernt, Dashboard-Kartensteuerung und gespeicherte Sichtbarkeit erhalten.
- Sensorübersicht aller freigeschalteten Profiltypen: Bluetooth-Zeilen/Abstand nur bei Zuordnung oder aktuellem Suchfund. Ausschalten und Verbindung trennen lassen zugeordnete Sensoren sichtbar; erst Entfernen löscht die Anzeige (inklusive noch vorhandenem Suchfund). GPS bleibt als integrierter Handysensor verfügbar. Keine Verbindungs-/Aufzeichnungslogik geändert.
- Gezielte Prüfungen: `hrv-refinement-tests.log` fünf Fälle erfolgreich; Menüfall in `profile-sensor-settings-tests.log` erfolgreich. Sensorgeometrie-/Lebenszyklusfall in `profile-sensor-visibility-tests.log`. Typografie/Atlas und Diff geprüft. Kein neuer APK-Auftrag, nicht auf main pushen.

## In Build118 enthalten

- Sticky-Gruppenkopf: Summenziffern/Einheiten behalten beschränkte Textlayouts und bleiben beim Anheften sichtbar. Fahrtzeilen unverändert. [Gruppen](route-list-groups.md).
- Manueller Intervals-HRV-Sammellauf aktualisiert vorhandene abweichende Mess-/Tagesfelder einschließlich Ruhepuls, unabhängig von alten Belegen. Nur identische Werte überspringen, keine fehlenden lokalen Angaben erfinden/sonstige Remote-Felder löschen. Monatsvergleich, frischer Tages-GET und Antwortprüfung erhalten. Automatische Outbox weiterhin mit bisherigen Eigentums-/Fremdschutzregeln. Herkunft der früher bei Intervals abweichenden Pulse ohne Remote-Verlauf nicht belegt. [Intervals](intervals-hrv.md).
- Windrose1,8-fach sichtbar (43,2dp auf gleicher24-dp-Ankerfläche), korrigierter Mittelpunkt erhalten. Eigene Nordebene dynamisch FOREGROUND, Rest schwarz, gemeinsame70% Deckkraft. [Karte](map-motion.md).
- HRV-Empfehlung/Begründung/subjektives Befinden ein Absatz; zusätzliche Zeitraumzeile entfernt. Mess-/Tagesseite8 statt16dp Seitenabstand, sichtbare Kreisränder ca.14/20 statt22/28dp unter Header. Obere Grenztexte3dp hoch, untere3dp runter. [Gestaltung](hrv-day-screen-design.md).
- Weiche Gesten: Touchslop vom sichtbaren Weg abziehen, Anfangssprung vermeiden; aktuelle Fingergeschwindigkeit in monotone Hermite-Kurve übernehmen und mit Geschwindigkeit0 enden, maximal300ms. Gleicher Viewport im HRV-Tagesbereich, Kalender-Tagesschritte bis heute; gemeinsamer Tag/Datum/Empfehlung/Diagrammaktionen, fester Footer. Modell vor Übergang vorbereiten,16 besuchte Tage im RAM-Cache, nach Bearbeiten/Löschen leeren. Diagrammgesten separat. [Übergänge](dashboard-page-transition.md), [Ablauf](hrv-measurement-flow-revision.md).
- Genau eine gemeinsame persönliche Messhistorie für PNS/Aktivierung, Importe als Eingangsadapter, keine Quellen-/Protokolltrennung danach.14 frühere gültige Tageswerte innerhalb60 Tagen, getrennte gültige Referenzanzahl je Index. Rechts neben dem Indexnamen statt Strich „noch XX Tage bis zum ersten Wert“, fehlender Tageswert/fehlende Streuung kurz erklärt. Formel unverändert: z(lnRMSSD), z(Ruhepuls).±4-Darstellung, persönliche−1…+1-Zone weiterhin mittlere50%, gröber skalierte äußere Viertel. Zahlen unbegrenzt, keine Kubios-Nachbildung. [Formeln](hrv-autonomic-index-proposal.md).
- HRV als erste/default Kennzahl im Verlauf, Tageswert/Korridor2×lnRMSSD wie Übersicht;7-Tage-Linie mittelt tatsächliche HRV-Tageswerte ab5 gültigen Tagen. Wochen/Monate aggregiert, dimensionlose Achse auch negativ/über10. Erste Reihe HRV/Puls/SDNN/RMSSD/Weitere, darunter ausklappbare pNN50/LF/HF-Kacheln. Weitere bleibt bei Zusatzkennzahl markiert, auch geschlossen. Keine neue Persistenz.

## Gezielte Prüfnachweise

`route-sticky-summary-fixed-tests.log` drei Fälle; `intervals-hrv-upsert-tests.log` acht Fälle; `wind-rose-size-north-tests.log` Native-Pixel-/Theme-/Geometrie; `hrv-compact-header-label-tests.log` zwei Fälle und zuvor tatsächlich gemessene Abstände; `page-smooth-handoff-tests.log` drei Übergangsfälle; fünf Integrationsfälle in `hrv-day-swipe-smooth-pages-tests.log`, neuer Tageswischfall nach Korrektur in `hrv-day-swipe-final-test.log`; `hrv-shared-reference-hint-tests.log` sechs Fälle; vier Score-/Weitere-Fälle zunächst erfolgreich in `hrv-score-more-tiles-tests.log`, verbleibende Fälle nach Korrektur in `hrv-more-tiles-final-tests.log` und `hrv-more-tile-native-final-test.log`; Monatsnavigation in `hrv-score-month-selection-test.log`; zwei Indexdarstellungsfälle in `hrv-index-four-range-tests.log`. Typografie/Atlas/Diff sowie native Vorschauen Flat/Glow320/360dp geprüft. Nur offene/betroffene Fälle wiederholt, kein Gesamtlauf oder physischer Gerätetest.

## Offen und Vorarbeit

Physisches Wischgefühl und Intervals-Liveupload mit Aktualisierung nach erneutem Sammellauf bestätigen. Alte HRV422-Bestandsmeldung nach erfolgreichem Sammelupload laut Nutzer erledigt; Ursache älterer gespeicherter abweichender Pulswerte ohne Remote-Verlauf offen, Mapping aus bereitgestellter Sicherung korrekt. Keine privaten Messdateien oder Gesundheitswerte ins Repository übernehmen.

Build117-Basis enthält getrennte Ansichten gefahrene Strecken/Vorlagen mit persönlicher28-Tage-Fahrzeitschätzung, gemeinsame Kartenplätze, Importmetadaten und vollständige Listenwerte; gemeinsamen HRV-Tag/Verlauf mit Footer-Schalter/Farblegende. [Build117](build-117.md), [Sammlungen](route-collections.md).

Ältere wichtige Grundlagen: Sicherung/Restore per dataSync-FGS/Wake-Lock und Datum im HRV-Dateinamen; gültige Bosch-Fahrdaten bei GPS-Ausfall, echte Höhenlücken nicht füllen; randfüllender Detailkopf, offene Puls-Z1, mitlaufendes rundes X-Raster. [Backup](backup-cloud-reorganization.md), [FIT](fit.md), [Build116](build-116.md).

Neue APK nur auf Auftrag ausschließlichGitHubActions; nächste Version voraussichtlich0.8.78/code148/Build119. Dauerhafte Signatur und tatsächlichen Raw-Download prüfen; Appcode nie mit[skip ci] überspringen. Snapshot vorBereinigung: `vor-den-bereinigungen-2026-10-30`.
