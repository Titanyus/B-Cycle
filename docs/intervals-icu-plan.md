# intervals.icu – FIT-Upload und spätere Trainingsprogramme

Stand: 15.10.2026. Erster Ausbau implementiert; produktiver Kontotest steht aus.

## Bedienung

Daten & Speicher → Streckendaten enthält getrennte Einträge Cloud & WebDAV (Server-Icon)
und intervals.icu (Trainingskurve). Bei intervals.icu Athleten-ID und persönlichen
API-Schlüssel eingeben und Verbindung prüfen. Schlüssel ausschließlich in der
App eingeben, niemals in Chat, Diagnosen oder Repository. Einstellungen werden
wie bestehende Dialoge automatisch gespeichert, solange sie gültig sind.

Der eigene Upload-Schalter aktiviert das Vormerken nach Aufnahmeende. WLAN-
Beschränkung ist standardmäßig aktiv. WebDAV, lokale Speicherung und intervals.icu
bleiben unabhängig. Der vorhandene Cloudupload im Detailfooter sendet an die
aktivierten Dienste; nicht importierte GPX-/FIT-Strecken, sondern nur eigene
aufgezeichnete FIT-Fahrten. Verwendet wird die aktive, gegebenenfalls
höhenkorrigierte Datei; das Original bleibt erhalten.

## Architektur und Status

IntervalsClient verwendet HTTPS und HTTP Basic mit Benutzer API_KEY sowie dem
API-Schlüssel als Passwort. Athletenprüfung über GET /api/v1/athlete/{id}; FIT
über POST /api/v1/athlete/{id}/activities als multipart/form-data (file).
Keine Weiterleitungen mit Zugangsdaten, keine Rohantworten in Fehleranzeigen.
Nur eine erfolgreiche Antwort mit bestätigter Aktivitäts-ID erzeugt Erfolg;
Abbruch, Fehler oder mehrdeutige Antwort nicht. Reale Anbieterantworten müssen
noch mit einem eingerichteten Konto geprüft werden.

IntervalsUploads nutzt einen separaten persistenten Android-Job und eigene
Warteschlange/Erfolgsbelege. Hash, Dateistand und Athletenkonto gehören zum Beleg.
Änderungen entwerten alte Erfolgsstatus. Wiederholtes Vormerken derselben Datei
verursacht keinen zusätzlichen Auftrag. Netzwerk-/Serverfehler werden mit
Backoff wiederholt, Authentifizierungsfehler erst nach Änderung der Einstellungen
bzw. erneutem manuellen Anstoß. Der Cloud-Haken gilt nur, wenn alle aktuell
aktivierten Dienste den aktuellen Dateistand erfolgreich übertragen haben.

API-Schlüssel und WebDAV-Passwort nutzen denselben AES-GCM-Helfer mit getrennten
Android-Keystore-Schlüsseln. Bestehende WebDAV-Verschlüsselung/Format bleiben
kompatibel. intervals.icu-Zugangsdaten und Konfiguration sind derzeit nicht Teil
der manuellen Einstellungssicherung; Android-Backups schließen App-Daten weiter
wie bisher aus. Erfolgsbelege enthalten keine Zugangsdaten. Keine zusätzlichen
Bibliotheken erforderlich. Der direkte Anbieter-/Dokumentationszugriff ist hier
per Netzwerkregel blockiert; Tests nutzen lokale HTTP-Testserver, keinen echten
Upload. Quellenlink zentral unter Erweitert → Quellen & Dienste.

## Später

Strukturierte Trainingsprogramme mit Abschnitten, Dauer und Zielintensität mit
intervals.icu austauschen; danach Programme auch direkt in BICYCLE integrieren.
Genauen API-Umfang für Einzeltrainings/mehrwöchige Pläne sowie Darstellung,
Bedienung und Durchführung gemeinsam festlegen. Noch kein Workout-Player,
keine Trainingssteuerung und keine automatische Übernahme von FTP-/Profilwerten.
