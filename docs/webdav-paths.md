# Nextcloud-WebDAV-Pfade

## Eingaben

- Nextcloud-Adresse: Cloud-Startadresse, z. B. `https://cloud.example.org` oder `https://example.org/nextcloud`.
- Alternativ die vollständige, in Nextcloud angezeigte WebDAV-Adresse des Benutzer-Hauptverzeichnisses: `https://cloud.example.org/remote.php/dav/files/interne-benutzerkennung/`. Diese Form enthält die interne Benutzerkennung bereits; der Login kann davon abweichen, etwa eine E-Mail-Adresse sein. Auch Nextclouds `remote.php/webdav`-Endpunkt wird akzeptiert.
- Zielverzeichnis: relativer Pfad unter den persönlichen Dateien, z. B. `B-Cycle` oder `Sport/Fahrten`. Leere Eingabe bedeutet persönliches Hauptverzeichnis. Keine URL und kein `remote.php`-Pfad. Normale Ordnernamen mit Leerzeichen eingeben; die App kodiert sie für die Übertragung. Fehlende Ordner werden angelegt.
- Bei Verwendung der Cloud-Startadresse muss der Benutzername der internen Nextcloud-Benutzerkennung entsprechen, weil daraus der DAV-Dateipfad aufgebaut wird. Die vollständige WebDAV-Adresse vermeidet diese Mehrdeutigkeit.

## Gefundene Fehler und Korrektur

Der bisherige Builder erzeugte beim leeren Zielpfad einen doppelten Schrägstrich hinter dem Benutzerverzeichnis. Der Verbindungstest prüft dieses Verzeichnis zuerst, auch wenn ein Zielordner angegeben ist. Korrigiert: exakt ein abschließender Slash.

Außerdem wurde an eine bereits vollständige DAV-Adresse erneut der DAV-Endpunkt einschließlich Benutzername angehängt. Korrigiert: vollständige DAV-Benutzerwurzel unverändert als Basis nutzen, danach ausschließlich den separat konfigurierten Zielordner anhängen. Ein bereits in der Serveradresse angehängter Unterordner wird mit verständlichem Eingabefehler abgelehnt; er gehört ins Zielverzeichnis-Feld.

Ein abweichender Fehler bei falschem Passwort belegt nicht, dass die Benutzerkennung im Dateipfad korrekt ist: HTTP-Authentifizierung kann erfolgreich sein, während das angefragte Benutzerverzeichnis nicht existiert. Ohne konkrete Serveradresse und Serverantwort ist die Ursache auf dem tatsächlichen Server noch nicht bestätigt.

## Prüfung

TLS-Mockserver prüft den tatsächlichen PROPFIND-Request mit leerem Zielverzeichnis und exakt einem Slash. Der vorhandene verschachtelte Ordner-Test prüft nun auch die zuerst angefragte Benutzerwurzel. Weiterer Regressionstest prüft eine kopierte vollständige DAV-URL mit kodierter interner Kennung und E-Mail-Login, Unterverzeichnisse und den Legacy-Endpunkt. Bestehende Authentifizierungs-, TLS-, Redirect-, Upload- und Inhaltsprüfungen bleiben erhalten. Es wurden keine Zugangsdaten des Nutzers abgefragt, gespeichert oder an einen realen Server gesendet.

Geändert: `WebDavClient.kt`, `RouteExportView.kt` (präzisere Eingabehinweise), `WebDavClientTest.kt`.

Bestätigung durch Nutzereingabe: Es wurde eine vollständige DAV-Benutzeradresse eingetragen. Der alte Builder hängte hier tatsächlich einen zweiten DAV-Pfad an. Vollständiger Testlauf: 155 Tests bestanden, keine Fehler oder übersprungenen Tests. Release-Build erfolgreich in 1 Minute 12 Sekunden. Für die Test-APK wird Version 0.6.15 (Code 31) gebaut.
