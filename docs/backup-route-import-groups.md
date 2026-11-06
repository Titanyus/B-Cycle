# Backup & Restore: Importgruppen

Stand: 05.11.2026, nach Build139, noch nicht veröffentlicht.

Unter dem bestehenden Sticky-Header Backup & Restore stehen die kleinen Überschriften „Sichern“, „Wiederherstellen“ und „Importieren“. Horizontale Trenner grenzen die Gruppen ab. Bestehende Sicherungs-/Wiederherstellungsrahmen samt Lokal/WebDAV und HRV4Training-Import bleiben erhalten.

Der bisher deaktivierte Streckenimport ist als dritte Gruppe reaktiviert: geplante Strecken, gefahrene Strecken, Archiv und Dreeve stehen als einzelne Menüzeilen ohne Accordion/Dropdown. Die Cloud-Gruppe enthält keinen separaten deaktivierten Import mehr.

`RouteImportAction.entries` ist die einzige Liste für diese Zeilen und das Import-Dropdown im Streckenmanager. Titel, Icons und Kennungen kommen aus demselben Enum; beide Einstiege nutzen `performRouteImport` und `routeImportEnabled`. Neue Einträge erscheinen automatisch in beiden Menüs; der exhaustive Handler verlangt die zugehörige Aktion. Dreeve bleibt ohne konfigurierte URL/Secret sichtbar und deaktiviert, unabhängig vom Upload-Schalter. Verfügbarkeit wird nach Konfigurationsänderungen sowie beim Wiederanzeigen aktualisiert.

Gezielt geprüft:

- `DataSettingsNavigationTest.individualRouteImportsAreEnabledAndHrvRestoreKeepsItsExplicitActions`: Gruppenreihenfolge/Trenner, vier einzelne Zeilen, drei native Dateiauswahlen mit bisherigen Requestcodes und unveränderter HRV-CSV-Einstieg.
- `DreeveImportNavigationTest.managerKeepsUnconfiguredDreeveVisibleButDisabledAndImportScreenHasNoConfigurationAction`: bestehendes Importmenü und Dreeve-Freigabe.
- Logs: `/workspace/.tools/shared-route-import-and-backup-tests.log` und `/workspace/.tools/backup-import-groups-final-test.log`, erfolgreich.
- Typografie, UI-Katalog und `git diff --check` erfolgreich. Kein Gesamttestlauf, keine APK erstellt.


06.11.2026: Neuere Nutzerentscheidung ersetzt die flachen Einzelzeilen: Rahmen-Accordion „Strecken importieren“ mit denselben vier zentralen RouteImportAction-Einträgen. Darunter Rahmen-Accordion „Trainings importieren“ mit Cloud-/Dateiimport aus TrainingImportAction, gemeinsam mit Trainingsfooter. Gruppenüberschriften/Trenner und Sicherungsfunktionen erhalten. Siehe [Trainingssammlung](training-library.md).
