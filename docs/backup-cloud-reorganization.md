# Backup & Cloud: vereinbarte Neuordnung

Nutzerentwurf 27.10.2026, bisher zusammengefasst und hinsichtlich Funktionsumfang abgeglichen. Die folgende Neuordnung ist noch nicht implementiert. Separat umgesetzt ist die Position des Passwortauges innerhalb des Eingaberahmens (BicycleForm.passwordField), rechts und vertikal mittig, für WebDAV, intervals.icu, Dreeve und Strava. Maskierung, explizites Lesen gespeicherter WebDAV-Zugangsdaten, Cursor und automatische Speicherung unverändert; vorhandene RouteExportTest-Passwortprüfung inklusive Geometrie aller vier Dienste erfolgreich.

## Backup & Restore (bisher Globale Daten)

Drei Dropdowns: Einstellungen sichern, Strecken sichern, HRV-Daten sichern. Jedes mit Lokal (Systemauswahl) und WebDAV (bestehende Verbindung; ohne Konfiguration deaktiviert). Einstellungen immer vollständig, ohne Bereichsauswahl, weiterhin passwortverschlüsseltes ZIP inklusive Zugangsdaten. Strecken umfassen den vollständig wiederherstellbaren Bestand, keine bloße FIT-Exportkopie. HRV: letzte gültige Messung pro Tag plus Tagesbefinden; ältere lokale Messungen nicht löschen.

Nach horizontalem Trenner: Einstellungen wiederherstellen, Fahrten wiederherstellen. HRV-Dropdown mit HRV-Daten wiederherstellen und HRV4Training-CSV importieren. Eigener HRV-Sticky-Header entfällt.

## Cloud (bisher Streckendaten)

Direkt nach dem Sticky-Header, vor dem ersten Unterpunkt, steht:

„Sichert nach jeder Fahrt die Streckendaten + Metadaten lokal oder zu den entsprechenden Clouddiensten.“

Fahrten sichern entfällt hier. Streckenimport vorerst deaktivieren. Lokale Sicherung behält Zielordner. WebDAV mit Nextcloud-Symbol und bisherigen Zugangseinstellungen; intervals.icu, Dreeve und Strava behalten Verbindungen und Uploadlogik. intervals-Sammelupload: nur „Prüft vorhandene Fahrten und lädt nur fehlende hoch.“ über Button. Dreeve-Namensabgleich und Beschreibung durch horizontalen Trenner absetzen.

Lange IN-/OUT-Beschreibungen entfernen, nur kompakte log-in/log-out-Symbole: ausgehend FIT-Streckendaten bei lokaler Sicherung und allen Cloudzielen, Dreeve zusätzlich eingehend Wartungsintervalle. Keine IN-/OUT-Wörter. Texte manuell gepflegt, keine Pflicht zur laufenden automatischen Anpassung. Sonstige lange Transferbeschreibungen auch außerhalb dieses Screens entfernen, keine funktionalen Sicherheitshinweise entfernen.

## Kompatibilität und bewusste Einschränkungen

Bisherige RideBackup-Gesamtsicherung enthält Einstellungen und HRV zusätzlich zu Fahrten; neue Aktionen sollen diese Bereiche trennen. Nach ausdrücklicher Nutzerkorrektur wird vollständig auf die neue Methode umgestellt: Alte Gesamtsicherungen und bisherige HRV-Exporte müssen nicht eingelesen werden können. Keine Kompatibilitätslogik für diese Formate entwickeln. HRV4Training-CSV ist davon unabhängig und bleibt als ausdrücklich gewünschter Import erhalten. Zugriff zur Wiederherstellung über WebDAV erhalten. Wiederherstellung mit Bereichsauswahl nicht mit der bewusst entfallenden Bereichsauswahl beim Sichern verwechseln.

Bisheriger HrvDiagnosticExport enthält auch verworfene und ältere Messungen und ist nicht gleichbedeutend mit der geplanten Tageswertsicherung. Diagnoseumfang wäre durch die neue Sicherung nicht abgedeckt; Nutzer wurde darauf hingewiesen. Kein eigenmächtiger zusätzlicher Menüpunkt oder Datenlöschung.
