# B|CYCLE 0.8.32

- Einstellungssicherung als Standard-AES-256-ZIP mit eigenem Passwort und Bestätigung; enthält ausgewählte Einstellungen einschließlich zugehöriger Zugangsdaten und Fahrradbilder.
- WebDAV-, intervals.icu-, Dreeve-, Strava- und Kartenzugänge auf dem Zielgerät neu verschlüsselt. Alte JSON-Sicherungen bleiben importierbar. Android-Berechtigungen und Ordnerfreigaben müssen ggf. erneut erteilt werden.
- Vollständigkeit ergänzt: lokale Exportordnerreferenz, Kartenposition/Zoom/Trackauswahl und aktive Projektion der profilbezogenen UI-/Kartenwerte. Alle aktiven/inaktiven Profile bleiben enthalten.
- Vollständige Verzeichnissicherung verwendet ebenfalls settings.zip. FIT-/GPX-/HRV-Dateien daneben bleiben unverschlüsselt.
- FIT-Metadaten-Accordion als erster Punkt unter Strecken-Aufzeichnung; Datenfelder dieser Kategorie als Schalter. Werte, Bedienlogik und Profilfilter erhalten.

36 gezielte Tests der Sicherung/Wiederherstellung und drei gezielte Tests der Aufzeichnungs-UI erfolgreich. Typografie-/Atlasprüfung erfolgreich. Kein Gesamttestlauf oder Hardwaretest. [Sicherungsformat und Umfang](settings-archive.md).
