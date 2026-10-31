# Backup & Cloud: vereinbarte Neuordnung

Umgesetzt am 27.10.2026 nach ausdrücklichem „leg los“. Die drei Bereiche werden getrennt gesichert und wiederhergestellt. Separat umgesetzt ist die Position des Passwortauges innerhalb des Eingaberahmens (BicycleForm.passwordField), rechts und vertikal mittig, für WebDAV, intervals.icu, Dreeve und Strava. Maskierung, explizites Lesen gespeicherter WebDAV-Zugangsdaten, Cursor und automatische Speicherung unverändert; vorhandene RouteExportTest-Passwortprüfung inklusive Geometrie aller vier Dienste erfolgreich.

## Backup & Restore (bisher Globale Daten)

Direkt nach dem Sticky-Header, vor den drei Sicherungs-Dropdowns, steht:

„Sichert die Einstellungen der App, Streckendaten und HRV-Messdaten samt Tagesbefinden auf Wunsch lokal oder per WebDAV. Alle Einstellungen können wiederhergestellt werden“

Drei Dropdowns: Einstellungen sichern, Strecken sichern, HRV-Daten sichern. Jedes mit Lokal (Systemauswahl) und WebDAV (bestehende Verbindung; ohne Konfiguration deaktiviert). Einstellungen immer vollständig, ohne Bereichsauswahl, weiterhin passwortverschlüsseltes ZIP inklusive Zugangsdaten. Strecken umfassen den vollständig wiederherstellbaren Bestand, keine bloße FIT-Exportkopie. HRV: letzte gültige Messung pro Tag plus Tagesbefinden; ältere lokale Messungen nicht löschen.

Nach horizontalem Trenner: Einstellungen wiederherstellen, Fahrten wiederherstellen. HRV-Dropdown mit HRV-Daten wiederherstellen und HRV4Training-CSV importieren. Eigener HRV-Sticky-Header entfällt.

## Cloud (bisher Streckendaten)

Direkt nach dem Sticky-Header, vor dem ersten Unterpunkt, steht:

„Sichert nach jeder Fahrt die Streckendaten + Metadaten lokal oder zu den entsprechenden Clouddiensten.“

Fahrten sichern entfällt hier. Streckenimport vorerst deaktivieren. Lokale Sicherung behält Zielordner. WebDAV mit Nextcloud-Symbol und bisherigen Zugangseinstellungen; intervals.icu, Dreeve und Strava behalten Verbindungen und Uploadlogik. intervals-Sammelupload: nur „Prüft vorhandene Fahrten und lädt nur fehlende hoch.“ über Button. Dreeve-Namensabgleich und Beschreibung durch horizontalen Trenner absetzen.

Lange IN-/OUT-Beschreibungen entfernen, nur kompakte log-in/log-out-Symbole: ausgehend FIT-Streckendaten bei lokaler Sicherung und allen Cloudzielen, Dreeve zusätzlich eingehend Wartungsintervalle. Keine IN-/OUT-Wörter. Texte manuell gepflegt, keine Pflicht zur laufenden automatischen Anpassung. Sonstige lange Transferbeschreibungen auch außerhalb dieses Screens entfernen, keine funktionalen Sicherheitshinweise entfernen.

## Kompatibilität und bewusste Einschränkungen

Die frühere RideBackup-Gesamtsicherung enthielt Einstellungen und HRV zusätzlich zu Fahrten; die neuen Aktionen trennen diese Bereiche. Nach ausdrücklicher Nutzerkorrektur wird vollständig auf die neue Methode umgestellt: Alte Gesamtsicherungen und bisherige HRV-Exporte müssen nicht eingelesen werden können. Keine Kompatibilitätslogik für diese Formate entwickeln. HRV4Training-CSV ist davon unabhängig und bleibt als ausdrücklich gewünschter Import erhalten. Zugriff zur Wiederherstellung über WebDAV erhalten. Wiederherstellung mit Bereichsauswahl nicht mit der bewusst entfallenden Bereichsauswahl beim Sichern verwechseln.

Bisheriger HrvDiagnosticExport enthält auch verworfene und ältere Messungen und ist nicht gleichbedeutend mit der geplanten Tageswertsicherung. Diagnoseumfang wäre durch die neue Sicherung nicht abgedeckt; Nutzer wurde darauf hingewiesen. Kein eigenmächtiger zusätzlicher Menüpunkt oder Datenlöschung.

## Implementierung

DataSettingsView und BackupDestinationView bilden die neue UI; bestehende Serviceformulare bleiben. RideDirectoryBackup schreibt nur Strecken in bicycle-routes.json-Snapshots. HrvDirectoryBackup schreibt eigene bicycle-hrv.json-Snapshots, jeweils mit SHA-256/Größe und Abschlussindex zuletzt. Gültige eigene Messungen (acceptable) und importierte HRV4-Messwerte werden vor der Tagesauswahl berücksichtigt. Gemeinsame Tagesantworten werden auch für Tage ohne gültige Messung gesichert; keine Rohdatenlöschung. Wiederholte Wiederherstellung ergänzt fehlende Daten ohne neue Uploads.

Die komplette Einstellungssicherung verwendet unverändert SettingsArchive und das Sicherungspasswort. WebDAV-Sicherungen erhalten eindeutige Dateinamen; Wiederherstellung listet passende ZIPs. Lokal wird die Systemdateiauswahl verwendet. Stand-alone-Strecken-/HRV-Ordner verwenden weiterhin die vorhandene BackupDirectory-Abstraktion für Android-Ordner und WebDAV.

Prüfung abgeschlossen: 32 unterschiedliche gezielte Fälle aus Einstellungen/ZIP, Navigation, Strecken-/HRV-Roundtrips, Dubletten/Integrität und SAF-/WebDAV-Transport erfolgreich. Keine echten Cloudkonten oder Gerätetests verwendet. Keine neue APK beauftragt.

Darstellung nach Build93: Die drei Sicherungsdropdowns und HRV-Wiederherstellung verwenden BicycleFrameDropdown mit gemeinsamem Rahmen um Kopf und geöffneten Inhalt, führendem Icon und Chevron rechts. Direkte Einstellungs-/Fahrten-Wiederherstellungsbuttons sind linksbündig mit zentralem Wiederherstellungssymbol. Speicherziele, Verfügbarkeit und sämtliche Aktionen bleiben unverändert.

Auch lokale Speicherung und alle Cloud-Speicherziele verwenden den gemeinsamen Rahmen um Header und Inhalt. Verbindliche Anordnung nach Nutzerkorrektur: Icon · Name · Chevron · Schalter (Chevron links vom Schalter). Bestehende native Switches werden mit ihren Listenern/Zuständen umgehängt; Aufklappen bleibt unabhängig. Das Rahmenpadding ersetzt das bisherige horizontale Formpadding, damit kein doppelter Innenabstand entsteht.

## Display aus: Sicherung und Wiederherstellung nach Build 114

BackupWork/BackupWorkService schützt benutzerinitiierte Arbeit mit einem dataSync-Vordergrunddienst, laufender Benachrichtigung und PARTIAL_WAKE_LOCK. Das Display darf ausgeschaltet bleiben. Die normale FOREGROUND_SERVICE_DATA_SYNC-Berechtigung ergänzt vorhandene Rechte; keine neue Laufzeitabfrage. Mehrere Arbeitsschritte teilen den Schutz. Erst das tatsächliche Ende der letzten Operation gibt ihn frei; vor dem Idle-Stopp wird erneut auf neue Arbeit geprüft.

HRV/Strecken bleiben auf dem bestehenden MapState-Worker. Einstellungen: Lesen, Entschlüsseln, Wiederherstellen und Dateischreiben sind geschützt. Verschlüsseln und WebDAV-Upload bilden eine einzige geschützte Operation ohne Lücke im UI-Callback. Passwortdialog und Systemdateiauswahl selbst halten keine Sperre. Abgelehnter Start beginnt keine ungeschützte Arbeit und meldet den Fehler. Android-Datensync-Timeout unterbricht die Worker und beendet den Dienst. Erzwungenes App-Beenden, Prozessverlust und echter Netzausfall sind keine Erfolgsgarantie; keine blinde Wiederholung halber Schreibvorgänge.

HRV-Messdateien heißen jetzt `hrv-JJJJ-MM-TT-UUID.json`. Der Messtag kommt aus HrvDailySelection inklusive Messzeitzone/Importtag. Manifest und Wiederherstellung verwenden die Dateinamen unverändert; bisherige indexierte Namen bleiben lesbar. Antworten ohne Messung heißen bereits `day-JJJJ-MM-TT.json`. Tagesauswahl, IDs, Inhalte und Prüfsummen bleiben erhalten.
