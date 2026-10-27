# Getrennte Streckensicherung

Unter Backup & Cloud → Backup & Restore → Strecken sichern stehen Lokal und WebDAV bereit. Die Sicherung enthält ausschließlich Strecken und zugehörige Metadaten. Einstellungen und HRV werden mit eigenen Aktionen separat gesichert. Der frühere gemeinsame Sicherungsordner wird nicht mehr erzeugt oder über die UI wiederhergestellt.

## Inhalt

Ein eindeutiger Bicycle-Routes-*-Ordner enthält Originaldateien, vollständige Aufzeichnungen und JSON-Begleitdaten. FIT: Original, vorhandene korrigierte Version und vollständige Analyse-FIT aus den aufbewahrten Messdaten. GPX: Original und abgeleitete FIT mit vorhandenen Messwerten. Dreeve-Zusammenfassungen bleiben Zusammenfassungen; keine erfundenen FIT-Aufzeichnungen. Metadaten erhalten Namen, Herkunft, Kennzahlen, portable Fahrradzuordnung, bestätigte Cloud-Verknüpfungen und vorhandene lokale Wetterdaten. Normale Fahrtuploads behalten die ausgewählte FIT-Version; diese Sicherung ist davon getrennt.

`bicycle-routes.json` ist der zuletzt geschriebene Abschlussindex, Format bicycle-routes-backup/schema 1. Dateien werden anhand Größe und SHA-256 beschrieben und geprüft. Fehler einzelner Fahrten werden im Ergebnis ausgewiesen; globale Verbindungsfehler/Unterbrechungen verhindern einen Abschlussindex. Zielordner sind eindeutig; bestehende Sicherungen werden nicht überschrieben.

## Wiederherstellung

Fahrten wiederherstellen → Lokal oder WebDAV. Direkt den Sicherungsordner oder seinen Elternordner wählen; bei mehreren Sicherungen folgt eine Auswahl. Nach Bestätigung werden fehlende Fahrten ergänzt, vorhandene nicht überschrieben. Die Verarbeitung läuft auf dem vorhandenen Bibliotheksworker. Kein automatischer Upload und kein Namensabgleich durch die Wiederherstellung.

Einstellungen und HRV-Daten bleiben unverändert. Für vollständige Profilzuordnung gegebenenfalls vorher die separate Einstellungssicherung wiederherstellen. Portable Profil-UUID ist entscheidend; gleicher Name/Slot reicht nicht. Ohne passendes Profil bleibt die gespeicherte Fahrradbezeichnung erhalten und die Fahrt später zuordenbar.

## Andere Bereiche

Profilbilder und sämtliche Einstellungen gehören ausschließlich in das passwortgeschützte SettingsArchive. HRV-Messungen und Tagesbefinden gehören ausschließlich in HrvDirectoryBackup. Strecken- und HRV-Ordner sind unverschlüsselt; die Einstellungssicherung bleibt passwortgeschützt.
