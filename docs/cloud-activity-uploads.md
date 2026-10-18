# Fahrt-IDs und getrennte Uploadziele

Stand: 15.10.2026. WebDAV, intervals.icu sowie neue getrennte Dreeve- und
Strava-Uploadpfade sind implementiert. intervals.icu wurde vom Nutzer real
bestätigt; die neuen beiden Dienste sind lokal mit HTTP-Testservern geprüft,
noch nicht mit einem echten Konto/Server. Ab APK 0.7.24 enthalten.

## Identität und Erfolgsbelege

Die bestehende interne FIT-ID bleibt die Identität der Fahrt:
`recorded-<SHA256 des unveränderten Originals>` beziehungsweise die bestehende
`imported-…`-ID wiederhergestellter Metadaten. Umbenennen und Höhenkorrektur
ändern sie nicht. Uploads bleiben auf eigene aufgezeichnete Fahrten begrenzt.

`RideServiceLinks` speichert bestätigte Aktivitätszuordnungen dauerhaft im privaten
App-Speicher: interne Fahrt-ID, Dienst, Konto-ID, externe Aktivitäts-ID und
SHA256 der hochgeladenen Dateiversion. Keine Zugangsdaten oder Dateinamen.
Identische Einträge werden nicht dupliziert. Dieselbe externe Aktivität darf im
gleichen Dienst/Konto nicht zwei lokalen Fahrten zugewiesen werden. Verschiedene
Dienste und Konten besitzen getrennte Namensräume.

Die Zuordnung ist vom Uploadstatus getrennt: Eine neue FIT-Version oder ein
fehlgeschlagener Versuch löscht frühere bestätigte externe IDs nicht. Falls ein
Dienst beim erneuten Upload eine weitere Aktivität anlegt, bleiben beide IDs
erhalten. Das ist eine Historie bestätigter Zuordnungen, keine Behauptung, dass
die aktuelle Version bereits hochgeladen sei. Bestehende erfolgreiche
intervals.icu-Belege werden vor ihrer Ersetzung beziehungsweise beim Lesen der
Zuordnungen übernommen. Ein früher bereits überschriebener Beleg kann ohne
Dienstabfrage nicht rekonstruiert werden.

Die Uploadjobs bleiben je Dienst unabhängig. Fan-out führt sämtliche Zielaktionen
aus, auch wenn eine Konfiguration oder ein Enqueue fehlschlägt, und meldet den
Fehler erst danach. Der gemeinsame Cloud-Haken erfordert weiterhin bestätigten
Erfolg aller aktivierten Ziele für den aktuellen Dateistand. Eine alte laufende
Übertragung darf einen neueren vorgemerkten Dateistand nicht als erfolgreich
markieren. Löschen einer lokalen Fahrt entfernt die lokale ID-Zuordnung, ohne
entfernte Aktivitäten zu löschen. Dienstzuordnungen sind derzeit ebenso wie
Uploadbelege nicht Teil der manuellen Einstellungssicherung.

## Einrichtung und asynchrone Uploads

Daten & Speicher → Streckendaten bietet eigene Seiten mit einfarbigen
Dienst-Icons, Upload-Schalter, WLAN-Option (Standard: an) und Zugangsdatenfeldern.
Gültige Einstellungen werden automatisch gespeichert. Aktivierte Dienste werden
bei Aufnahmeende unabhängig vorgemerkt; der Cloud-Button in den Details stößt
alle aktivierten Ziele an. Schlüssel, Client-Secret und Tokens sind über
StoredSecret/AES-GCM getrennt verschlüsselt, niemals Bestandteil von APK,
Diagnosen oder manueller Einstellungssicherung.

### Dreeve

1. In der eigenen Dreeve-Instanz unter Settings → Security → API access einen
   Schlüssel erzeugen, als DREEVE_API_KEY in der Serverkonfiguration setzen und
   die Container wie von Dreeve beschrieben neu erstellen. IMPORT_MODE muss
   `files` sein.
2. Die HTTPS-Startadresse der Instanz und den `drv_…`-Schlüssel ausschließlich in
   BICYCLE eingeben. Unterverzeichnisse werden erhalten; keine API-Endpunkte an
   die Adresse anhängen. Keine URL-Zugangsdaten, Query- oder Fragmentparameter.
3. Verbindung prüfen, anschließend Upload aktivieren.

Geprüfter API-Vertrag: https://docs.dreeve.app/api/openapi.yaml
GET /api/v1/status bestätigt App und canUpload. POST /api/v1/activity/upload
überträgt Multipart `file`; HTTP 202 / queued ist nur die Annahme. Jede Operation
hat einen dauerhaft gespeicherten eindeutigen FIT-Dateinamen. GET
/api/v1/file-imports?filters[filename]=… prüft genau diesen Namen und die Quelle
fitFile vor dem Senden sowie bei späteren Wiederholungen. Das kann eine verlorene
202-Antwort wiederfinden. Erst `success` mit eindeutiger activityId erzeugt den
Erfolgsbeleg und die ID-Zuordnung.

`skipped` enthält laut API keine Aktivitäts-ID. Dann gibt es einen festen Hinweis
in der Dienstkonfiguration und keine erratene Zuordnung. Bereits über WebDAV
von Dreeve eingelesene Dateien können diesen Fall auslösen; für dieselbe Fahrt
nicht parallel zwei Dreeve-Importwege betreiben. WebDAV bleibt unabhängig und
wird nicht eigenmächtig deaktiviert. Ohne Idempotency-Key des Dienstes ist eine
exakt einmalige Übertragung nach einem Verbindungsabbruch nicht garantierbar.

### Strava – persönliche API-App

1. Eigene App unter https://www.strava.com/settings/api registrieren.
   Authorization Callback Domain: `localhost`.
2. Eigene Client-ID und Client-Secret nur in BICYCLE eingeben, dann
   „Mit Strava verbinden“. Kein gemeinsamer geheimer App-Schlüssel in der APK.
3. Anmeldung im Browser beziehungsweise Strava-App bestätigen. Danach den
   Upload-Schalter in BICYCLE aktivieren.

Offizieller mobiler OAuth-Endpunkt /oauth/mobile/authorize, Berechtigung nur
activity:write. Redirect `bcycle://localhost/strava` mit genau geprüftem,
einmaligem State (10 Minuten gültig), Scope und Pfad. State bleibt bei einer
Prozessneuerstellung erhalten. Codeaustausch über POST /oauth/token; der Code
wird nicht gespeichert. Access- und jeweils neueste Refresh-Tokens werden
verschlüsselt gespeichert. Abgelaufene Tokens werden im Hintergrund erneuert;
veraltete Refresh-Antworten dürfen neuere Tokens nicht überschreiben. Änderung
der Client-Konfiguration entfernt die zugehörige lokale Anmeldung.

POST /api/v3/uploads überträgt FIT mit data_type=fit und stabiler external_id
(interne Fahrt-ID plus Dateihash). Die zurückgegebene Upload-ID wird vor späteren
Schritten dauerhaft gespeichert. GET /api/v3/uploads/{id} setzt die Verarbeitung
nach Job-/App-Neustart fort. Nur bestätigte activity_id bedeutet Erfolg; keine
IDs aus englischen Fehlertexten oder HTML extrahieren. 64-Bit-IDs bleiben exakt.
Offizielle Verträge:
https://developers.strava.com/docs/authentication/ und
https://developers.strava.com/docs/uploads/.

### Gemeinsamer Ablauf

Eigene Android-Jobs 8303 (Dreeve) und 8304 (Strava), getrennte Sperren und
Warteschlangen. Netzwerk-/Rate-Limit-/Serverfehler und laufende Verarbeitung
nutzen Android-Backoff (anfangs 60 Sekunden). Kein sekündliches UI-Polling oder
zusätzlicher permanenter Service. Authentifizierungs-/Importfehler erzeugen
keinen Erfolg; letzter Fehler ist auf der jeweiligen Einstellungsseite sichtbar.
Dateiversionen haben getrennte Operationen. Eine ältere angenommene Version
kann noch zu Ende geprüft werden, markiert aber nicht die aktuelle FIT als
erfolgreich. Abbruch nach Annahme erhält die Upload-ID für die nächste Prüfung.

Namensabgleich, Wartungsdaten und Trainingsprogramme bleiben spätere Schritte.
Eine gemeinsam angebotene Strava-App mit zentralem Client-Secret benötigt eine
andere Anmeldestruktur (z. B. Tokenaustausch über ein Backend); die persönliche
API-Konfiguration ist kein solches Backend. Der reale OAuth-Roundtrip und echte
Uploads sind mit dem Nutzerkonto auf dem Gerät noch zu prüfen.

Dienstmarken: Strava aus Simple Icons (CC0); Dreeve aus
https://docs.dreeve.app/assets/images/logo.svg, einfarbig mit transparentem
Hintergrund adaptiert. Marken gehören ihren jeweiligen Inhabern.

## Vorgemerkt: zentrale Strava-Anmeldung

Am 16.10.2026 als zukünftige Erweiterung festgehalten, jetzt nicht implementieren:

- BICYCLE einmalig als zentrale Anwendung bei Strava registrieren. Nutzer
  benötigen anschließend keine eigene API-App und keinen Entwicklerzugang.
- Bedienung: „Mit Strava verbinden“ → bei Strava anmelden → Zugriff erlauben.
  Client-ID-/Secret-Eingaben sollen für diesen Ablauf entfallen.
- Einen kleinen Backend-Dienst für den sicheren Codeaustausch und die
  Token-Erneuerung vorsehen; das gemeinsame Client-Secret bleibt ausschließlich
  serverseitig. Tokenverwaltung und Zugriffsschutz vor Umsetzung konkret planen.
- FIT-Uploads können weiterhin direkt vom Smartphone erfolgen; unabhängige
  Uploadjobs und die bestehenden Fahrt-/Dienst-ID-Zuordnungen erhalten.
- Vor Freigabe für weitere Nutzer die dann geltenden Strava-Anforderungen,
  Athletenkapazitäten und gegebenenfalls erforderliche App-Prüfung klären.
- Die bestehende persönliche API-Anbindung bleibt bis zur Umsetzung erhalten.
  Dreeve und intervals.icu bleiben unabhängig.

## BLE-Anzeige

In Bikes folgt die Anzahl unterschiedlicher empfangener Messwertarten in
derselben Statuszeile, beispielsweise `Pulsmesser · Live · 2 Messwerte`.
Wiederholte Nachrichten erhöhen die Anzahl nicht. Gezählt werden erfolgreich
dekodierte Messwerte der aktuellen Verbindung, keine beworbenen Dienste,
Konfigurationsnamen oder App-Berechnungen. Null ist ein Messwert.

Bosch zählt die empfangenen LiveMetric-Felder sowie ein vorhandenes
Reichweitenfeld. Ein Pulsmesser kann Puls, Energie, RR-Intervalle und Akku liefern;
gezählt werden nur tatsächlich gelieferte gültige Typen. Nach Trennen und beim
Neuverbinden wird zurückgesetzt; alte GATT-Callbacks bleiben ausgeschlossen.
GPS ist kein BLE-Gerät. Zusätzliche Powermeter sind weiterhin vorbereitet, noch
nicht unterstützt; dort werden keine Messwerte vorgetäuscht. FIT, Sensorwerte
und Performanceberechnungen bleiben unverändert.

## Prüfung

40 gezielte Tests erfolgreich: ActivityServiceTest (9),
DataSettingsNavigationTest (7), IntervalsTest (8), SettingsMenuTest (6),
RideServiceLinksTest (3) und RouteExportTest (7). Release-Build inklusive R8
und lintVital erfolgreich. Keine übersprungenen Tests. Kein
realer Strava-/Dreeve-Upload mit Nutzerdaten; diese Verifikation steht auf dem
Gerät aus. BLE-Anzahl zusätzlich auf realen Geräten prüfen.

## Vorgemerkt: Dreeve-Bestätigung und Statusanzeige verbessern (18.10.2026)

Nutzer bestätigt erfolgreichen echten API-Upload. B|CYCLE bleibt jedoch sehr lange oder dauerhaft bei „Übertragung / Verarbeitung“ beziehungsweise Wartestatus stehen. Noch keine Funktionsänderung vornehmen, keine APK hierfür bauen.

Aktueller Codepfad: POST /api/v1/activity/upload erwartet HTTP 202 mit status=queued. Das gilt nur als Annahme. Eine spätere Abfrage von GET /api/v1/file-imports mit filters[filename] und pagination[size]=100 sucht exakt den dauerhaft gespeicherten eindeutigen Dateinamen und source=fitFile. Erst status=success mit activityId bestätigt Erfolg und schreibt die externe Zuordnung. Bei fortgesetzter Verarbeitung wird jobFinished(..., true) genutzt: Android-JobScheduler-Backoff beginnt bei 60 Sekunden, exponentiell, tatsächliche Ausführung kann später erfolgen. Kein zeitnaher separater Bestätigungsabruf nach dem POST. Bei bereits angenommener Operation ohne gefundenen Import bleibt processing bestehen, ohne erneuten POST.

Für die spätere Korrektur prüfen: Annahme und laufenden Import im UI klar unterscheiden; nach Annahme begrenzte zeitnahe Bestätigungsabfragen statt exponentiellem Fehler-Backoff für reguläre Verarbeitung; Fehler-Backoff separat erhalten. Reale Dreeve-Antworten auf Dateiname, Quellen-/Statuswerte, Aktivitäts-ID und Pagination abgleichen, bevor ein dauerhafter Zuordnungsfehler angenommen wird. Unaufgelöste angenommene Importe sichtbar diagnostizieren, nicht endlos scheinbar übertragen und nicht blind erneut hochladen. Vorhandene externe IDs, Duplikatschutz und Unabhängigkeit aller Dienste erhalten. Ursache am konkreten Server noch nicht nachgewiesen; Quellcode erklärt jedoch bereits die verspätete Bestätigungsabfrage.
