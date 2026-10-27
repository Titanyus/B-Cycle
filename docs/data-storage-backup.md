# Backup & Cloud

Die Einstellungsseite enthält zwei Sticky-Abschnitte: **Backup & Restore** und **Cloud**. Beide beginnen mit dem vom Nutzer vorgegebenen kurzen Erklärungstext. Datenbestand und laufende Fahrtübertragung sind getrennte Funktionen.

## Backup & Restore

Drei unabhängige Dropdowns: Einstellungen sichern, Strecken sichern, HRV-Daten sichern. Jedes enthält die unmittelbaren Aktionen **Lokal** und **WebDAV**. Aufklappen startet keine Sicherung. WebDAV ist ohne gültig konfigurierte Verbindung deaktiviert, unabhängig davon, ob der automatische Fahrtupload aktiviert ist. Die vorhandenen WebDAV-Einstellungen bleiben die einzige Zugangskonfiguration.

- Einstellungen: immer alle BackupSection-Bereiche einschließlich Zugangsdaten und Profilbildern. Passwortgeschütztes ZIP über SettingsArchive; lokal ACTION_CREATE_DOCUMENT, WebDAV als eindeutige Bicycle-Settings-*.zip im konfigurierten Ordner. Keine Bereichsauswahl beim Sichern.
- Strecken: eigener Bicycle-Routes-*-Ordner, vollständige vorhandene FIT-/GPX-/Zusammenfassungsdaten und Fahrtmetadaten. Keine Einstellungen, Profilbilder oder HRV-Dateien. Details in ride-directory-backup.md.
- HRV: eigener Bicycle-HRV-*-Ordner mit letzter gültiger Messung je Tag und Tagesbefinden, einschließlich Tagesbefinden ohne gültige Messung. Ergebnisse und vorhandene Rohdaten/Importoriginalfelder bleiben erhalten. Fehlgeschlagene oder ältere lokale Messungen werden nicht gelöscht. Details in backup-cloud-reorganization.md.

Nach einem Trenner: Einstellungen wiederherstellen, Fahrten wiederherstellen und ein HRV-Dropdown mit HRV-Daten wiederherstellen / HRV4Training-CSV importieren. Wiederherstellung bietet jeweils Lokal/WebDAV; für Einstellungen ist die bestehende Bereichsauswahl und Bestätigung weiterhin verfügbar. Auf Android lokale Ordnerfreigaben und Berechtigungen neu erteilen. Keine Legacy-Gesamtsicherungs-/HRV-Export-Kompatibilität erforderlich; CSV-Import bleibt unabhängig.

## Cloud

Streckenimport ist vorerst sichtbar deaktiviert. Danach lokale automatische FIT-Speicherung, WebDAV, intervals.icu, Dreeve, Strava mit unveränderten Einstellungen und automatischen Uploads. Reines Aufklappen schaltet keinen Dienst ein. WebDAV verwendet zentral das Nextcloud-Symbol, auch in Übertragungsanzeigen. Passwörter/API-Schlüssel behalten ihr vorhandenes Maskierungsverhalten; das Auge sitzt rechts innerhalb des Feldrahmens.

Nur kompakte, manuell gepflegte Transferangaben mit log-out/log-in: ausgehend FIT-Streckendaten; bei Dreeve zusätzlich eingehend Wartungsintervalle. Bisherige lange IN-/OUT-Blöcke entfallen. intervals.icu-Sammelupload behält seine Logik; der kurze Satz steht über dem Button. Dreeve-Namensabgleich ist durch einen Trenner abgesetzt.

## Prüfung

Gezielte native Navigation, vollständiges passwortgeschütztes Settings-ZIP und SAF-Abbruch/Recreation, getrennte Strecken-/HRV-Roundtrips und unveränderte Fremdbereiche; Prüfsummen, Dubletten, lokale Ordnerzugriffe und WebDAV-Transport über MockWebServer. Kein privater Cloudzugang oder Gerät für die Prüfung verwendet.
