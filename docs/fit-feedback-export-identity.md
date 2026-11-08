# Doppelter Streckeneintrag nach dem Speichern mit RPE (08.11.2026)

Nutzerbeleg nach Build148: einmal lokal gespeichert und je einmal zu Intervals/Dreeve hochgeladen, trotzdem zwei Managerzeilen. Richtiger Name/Fahrrad/RPE am ursprünglichen Eintrag, generischer B-Cycle-Dateiname ohne diese lokalen Metadaten am zweiten.

## Nachgewiesene Ursache

RideAnnotationFiles erzeugt eine öffentliche FIT mit workout_rpe und gegebenenfalls Notiz. Diese Bytes unterscheiden sich absichtlich vom unveränderlichen Original; Original-ID bleibt dessen SHA-256. Der automatische Downloads-Scan von FitLibrary kennt bisher Original-, Höhenkorrektur- und Reexport-Hashes, aber nicht diese Feedbackkopie. Er importiert die eine externe Datei daher unter einem weiteren recorded-Hash. Lokale Profil-/Notiz-/Namensmetadaten hängen weiter an der ersten ID; ein Downloads-Import startet selbst keinen weiteren Cloud-Upload.

Reproduktionsfall vor Korrektur: summaryExportWithRpeIsOneDownloadAndRemainsOneRideAfterScanAndRestart, eine erzeugte Downloadsdatei mit RPE6, anschließend zwei verschiedene recorded-IDs. Log /workspace/.tools/ride-export-duplicate-before.log. Es waren weder zwei lokale Exporte noch zwei Finish-Ereignisse nötig.

## Korrektur

FitExportIdentities speichert exakte SHA-256-Zuordnungen erzeugter FIT-Exportkopien zur vorhandenen internen Original-ID. LocalRouteExport hinterlegt sie dauerhaft **vor** Veröffentlichung einer neuen Downloadsdatei bzw. vor Aktualisierung eines bestehenden Exportdokuments. So kann der automatische Scanner niemals eine fertig veröffentlichte Kopie vor ihrer Zuordnung sehen. Byte-identische Originale benötigen keine zusätzliche Zuordnung.

FitLibrary.add erkennt bekannte Exporthashes wieder, wenn der zugehörige Eintrag und sein internes Original noch vorhanden sind. Originaldateien werden weder ersetzt noch umgeschrieben. Ältere Feedbackversionen behalten ihre Zuordnung nach nachträglichen Änderungen und nach Neustart, unabhängig vom Dateinamen. Eine ausdrücklich gelöschte Fahrt bleibt auch beim automatischen Einlesen ihrer annotierten Exportversion ausgeschlossen. Fremde Dateien und echte andere Fahrten folgen weiterhin der bisherigen Importlogik; keine Zusammenfassung anhand Datum, Strecke, Puls oder ähnlicher Werte.

Atomare sidecar-Datei files/fit-export-identities.json mit Schema-/ID-/Hashvalidierung und8MiB-Lesegrenze; kleiner Cache für höchstens4 Appverzeichnisse, nach Länge/mtime und AtomicFile-Backup erneuern. Schreibende/lesende Registryzugriffe seriell. Nur Metadaten zu eigenen erzeugten Kopien; bestehende Uploadqueues und Quittungen bleiben unverändert.

Bereits früher angelegte doppelte Einträge werden nicht automatisch entfernt oder zusammengeführt. Die Korrektur verhindert neue Wiederimporte der ab jetzt registrierten Exportversionen. Alte lokale Kopien werden bei einer späteren regulären Aktualisierung ebenfalls registriert; eine vollständige frühere Exporthistorie wird nicht aus ähnlichen Fahrtdaten geraten.

## Gezielte Prüfungen

10 verschiedene Fälle aus RouteStorageTest, RideFinishTransfersTest und LocalRouteExportTest nach Anpassung einer alten Kartenplatz-Testannahme bestanden. Neue Regressionen: genau ein lokaler Export und ein interner Eintrag nach RPE/Speichern/Scan/Neustart, Name/Fahrrad/RPE und Originalbytes erhalten; alte und aktualisierte Kopien inklusive RPE0/9 und Notiz, persistente Zuordnung ohne RAM-Cache, manuell umbenannte Kopien, echte andere Fahrt und Ausschluss nach explizitem Löschen. SAF-Export, Fehlerpfade/Quittungen, unveränderte Originale und idempotente Freigabe weiterhin geprüft. Nur synthetische Testdaten/Provider, keine persönlichen Dateien/Konten.

Logs: /workspace/.tools/ride-export-duplicate-fixed.log (9 von10 direkt grün; veraltete Kartenplatzannahme im übrigen Fall), ride-export-duplicate-final.log (beide aktualisierten Fälle grün), ride-export-identity-deletion-test.log (gezielter zusätzlicher Löschschutz grün). Der alte Speichertest setzt vor neuer exklusiver Kartenauswahl nun ausdrücklich die vorhandenen Slots zurück; Produktions-Kartenlogik unverändert. Typografie/UI-Katalog/Diff grün. Kein Gesamtlauf/Profiling/APK beauftragt.
