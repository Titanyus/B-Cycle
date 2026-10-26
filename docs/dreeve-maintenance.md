# Dreeve-Wartung: schreibgeschützter Prototyp

Einstellungen → Wartung lädt beim Öffnen und über „Von Dreeve abrufen“ die vorhandenen Wartungsaufgaben. Bestehende Adresse und verschlüsselter API-Schlüssel aus Daten & Speicher → Dreeve werden verwendet; automatischer Fahrtupload muss nicht aktiviert sein. Keine zusätzlichen Kontodaten oder Einstellungen.

## API und Anzeige

GET /api/v1/gear/maintenance-tasks, Bearer-Authentifizierung, gemeinsame ActivityHttp-Grenzen/Timeouts/Redirectsperre. GET /api/v1/gear ergänzt Fahrradnamen; bei dessen Fehler bleiben Aufgaben sichtbar, IDs statt Namen und konkreter Warnhinweis. Mehrere Räder an einer gemeinsam genutzten Komponente und derzeit nicht zugeordnete Komponenten bleiben erhalten. Keine Vermutung eines lokalen Profils aus Namen.

Aufgabe, Komponente, Fahrradnamen/-IDs, Intervall (km, mi, Stunden, Kalendertage oder Nutzungstage), positiver Rest beziehungsweise Überschreitung, letzter Wartungstermin und Status Fällig/Überfällig/Anstehend. Dreeve berechnet Fortschritt und Reihenfolge; kein eigener Schwellenwert und keine fiktive Terminprognose. Auch noch nicht fällige Aufgaben sind sichtbar. Leere Antwort kann „keine Aufgaben“ oder „Wartung deaktiviert“ bedeuten; beides im Text benennen. Der geprüfte API-Handler liefert alle Aufgaben in einer Seite, keine zusätzliche Filterung/Pagination durch B-Cycle.

Unbekannter Endpunkt404: Versionshinweis. Authentifizierungs-, Netzwerk-, TLS-, Timeout- und Serverfehler verwenden ServiceProblems. Ungültige JSON-/Aufgabendaten als Antwortfehler behandeln. Abruf auf eigenem Worker, Sichtbarkeits-/Konfigurationsprüfung vor Anzeige; Schließen verwirft späte Antworten und beendet den Worker. Keine Pollingjobs, keine dauerhafte Wartungskopie und keine neuen Sicherungsfelder. Bearbeiten/Erledigtmelden/Upload ist nicht implementiert. Sichtbare IN/OUT-Texte und Hilfe wurden erweitert.

## Nachweise

Dreeve-Quellstand `93c214fd11cb0ff0d3fc2bad74fe2a0d910a0cce`:

- [API-Handler](https://github.com/dreeveapp/dreeve/blob/93c214fd11cb0ff0d3fc2bad74fe2a0d910a0cce/src/Controller/Api/V1/Gear/GearMaintenanceTaskSearchRequestHandler.php)
- [Intervall-Einheiten](https://github.com/dreeveapp/dreeve/blob/93c214fd11cb0ff0d3fc2bad74fe2a0d910a0cce/src/Domain/Gear/Maintenance/Task/IntervalUnit.php)
- [Öffentliche Testantwort](https://github.com/dreeveapp/dreeve/blob/93c214fd11cb0ff0d3fc2bad74fe2a0d910a0cce/tests/Controller/Api/V1/Gear/__snapshots__/GearMaintenanceTaskSearchRequestHandlerTest__testItListsMaintenanceTasksMostUrgentFirst__1.json), als API-Testfixture übernommen.

Sechs gezielte Tests erfolgreich: vier API-/UI-/Navigationstests, tatsächliches Einstellungsmenü und bestehende Dreeve-Pagination/Gear-Abfrage. Keine echte Verbindung zum Nutzer-Server und kein Gerätetest; installierte Dreeve-Version unbekannt. Keine neue APK angefordert.
