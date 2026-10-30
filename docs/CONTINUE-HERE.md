# Aktueller Stand: UI-/Zusammenfassungs-/HRV-Korrekturen nach Build 110 – 30.10.2026

[APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.69-release-110.apk) · [Buildnachweis](build-110.md). VersionCode 139, Quelle `1830fc1ab844e1676be52ce5772e39475b151fe1`, 2531313 Bytes, SHA-256 `0657892cbea304f8f922eaf09a035a5e8c4084ff207c6ecf618fd87f95abb641`. Signatur/Update-Kompatibilität/Manifest/ZIP-Ausrichtung/CI-Prüfsumme geprüft. Veröffentlichung mit [skip ci]. Letzte veröffentlichte APK; die folgende Korrektur ist darin noch nicht enthalten. Kein neuer APK-Auftrag.

## Enthalten

[Bereinigung und zusätzliche UI-Korrekturen](cleanup-2026-10-30.md), [300-ms-Dashboardwechsel](dashboard-page-transition.md), [Versionsänderungen](release-0.8.69.md). Normale/inaktive Rahmen und horizontale UI-Trenner 2 dp/25 %. Rahmen-Accordion-Druck über gesamte Fläche; Sensoren, Fahrtbenennung, Strecken-Aufzeichnung, Trink-Erinnerung und Daten/Trend/Graph im Rahmen. Abstand vor Dashboardkacheln, Höhenzeile ohne GPS weg, Zahlen unabhängig von Einheit unter Icon zentriert.

Snapshot vor Bereinigung: Tag `vor-den-bereinigungen-2026-10-30`, Quelle ed35551, auf GitHub. Entwicklungsschritte: Bereinigung 8000677; Detailzentrierung 9d6ae8c; Test-/Textereste a973a10; Dashboardübergang 896eae5. Vollständige alte Übergabe im [Archiv](archive/CONTINUE-vor-bereinigung-2026-10-30.md), keine aktuellen offenen Aufträge daraus ableiten.

## Prüfstand

Gezielte Cleanup-/Migrations-/Backup-/Frame-/Field-/Sensor-/Trink-/Licht-/Graphprüfungen erfolgreich. Finale Dashboardprüfung `/workspace/.tools/dashboard-transition-release-checks.log`: Viewport 5, Editor 2, Zentrierung 1; `/workspace/.tools/cleanup-current-profile-test.log` erfolgreich. Native Zwischenstandsvorschau visuell geprüft. Typografie/Atlas/Diff erfolgreich. Keine vollständige Gesamtsuite und kein physischer Gerätetest. Frühere Timingtestversuche verwendeten automatische Robolectric-Vsync; final den echten ValueAnimator gezielt getaktet. Eine lebende MapView, begrenzter Snapshot, Footer fest; Abbruch/Freigabe/keine Animation bei deaktivierter Systemanimation geprüft.

## Seit Build 110: Kachelgröße nach Drag-and-drop

Nutzer meldete verkleinert hängenbleibende Kacheln nach Umsortieren. `TileReorder`: DROP speichert weiterhin einmal, aber die 260-ms-Einrastanimation startet erst nach DRAG_ENDED. Dessen bisherige Bereinigung konnte eine bereits gestartete Animation abbrechen, ohne die Skalierung zurückzusetzen. Vorschau/Abbruch/neuer Drag/Detach normalisieren jetzt ausdrücklich beide Skalierungen, ausstehende Rückanimationen werden entfernt. Gemeinsame Lösung für Fahrrad-, Dashboard-, Status- und Namenskacheln; keine Änderungen an Auswahl oder Speicherung.

Prüfung: vier neue native TileReorder-Regressionsfälle (DROP/END-Reihenfolge, neuer Drag vor/während Rückanimation, Abbruch/Detach/Sperre) sowie bestehende RideNamingReorder-, Dashboard- und Fahrrad-Drag-/Accessibility-Tests: sieben Tests erfolgreich. Log `/workspace/.tools/tile-reorder-scale-tests.log`. Typografie/Atlas/Diff geprüft. Kein physischer Gerätetest, keine neue APK erstellt.

## Weitere Änderungen nach Build 110

[Dashboard, Zusammenfassung, Sensoren und Abstände](dashboard-summary-adjustments.md) umgesetzt. Status links Grundabstand25dp, rechts25dp+4Privacy; Licht oben0/rechts16dp bei sichtbarer Android-Statusleiste. Akkusnapshot Schaltwerk bei Stopp links vor Bike-Akku; Summary inklusive20/10%-Farben. Menütrenner nur hier32dp je Seite. Untere mittlere Dreierkachel ohne Karte bei Footer12dp höher. Aufzeichnungsaccordions ohne doppelte Schaltermarge, Schaltwerk wie andere Sensoren ohne separate Abfrage-/Hinzufügenbuttons.

[Intervals-HRV-Korrektur](intervals-hrv.md): Bereichs-GET auf `wellness.json` statt `wellness`, Nulltag zulässig, fremde Daten weiterhin geschützt. Fehlermeldung mit Tag/HTTP-Code vor Zählung, Serverfehler nicht pauschal „unterbrochen“. Nutzer bestätigt FIT-Zugang funktioniert (400 Uploads), Fehlanzeige zuvor nur „Vorgang unterbrochen“. Privaten Live-HRV-Upload nicht möglich.

Logs: `/workspace/.tools/ui-summary-hrv-tests.log` und abschließende `/workspace/.tools/ui-summary-hrv-final-tests.log`. Native/HTTP-Tests, Typografie/Atlas/Diff; keine neue APK angefordert/gebaut. Kompassfrage beantwortet: Pfeil ist Smartphone-Ausrichtung, getrennt vom GPS-Kurs; Kalibrierung oder magnetische Störung als mögliche Abweichung, keine Sensoränderung beauftragt.

## Windrose auf Karte – ebenfalls nach Build 110

Nutzer-SVG als schwarzer70%-Vektor in24-dp-Iconfläche mittig auf dem Vergrößern-Symbol. Nur bei Folgen+ausgeblendeten Buttons, nicht im Editor; dreht mit Kartenrotation (Norden im Original oben). Neue profilbezogene Kartenoption „Windrose im Folgemodus anzeigen“, zunächst aus, mitgesichert. [Details](map-motion.md). Native MapPanel-/Backuptests und Typografie/Atlas/Diff; Logs `map-wind-rose-tests.log`/`map-wind-rose-final-tests.log` unter `/workspace/.tools`. Noch keine neue APK angefordert.

Windrose-Mittelpunkt nach Nutzerhinweis korrigiert: Innenkreis des Originals(45.3,46.9) statt SVG-Flächenmitte. Quadratischer93.8-Vektor mittranslateX1.6, tatsächlicher Kreismittelpunkt deckungsgleich mit24-dp-Icon-/Drehmittelpunkt.
