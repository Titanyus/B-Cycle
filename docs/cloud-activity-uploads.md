# Fahrt-IDs und getrennte Uploadziele

Stand: 15.10.2026. WebDAV und intervals.icu sind funktionsfähig; Strava und
Dreeve sind beauftragt, aber noch nicht angebunden.

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

## Nächster Ausbau

- Dreeve: Anmeldung, Datei-/Aktivitätsupload und bestätigte externe ID anhand
  der offiziellen OpenAPI prüfen:
  https://docs.dreeve.app/api/openapi.yaml
  Der Abruf ist in der aktuellen Entwicklungsumgebung durch die Netzwerkregel
  gesperrt. Kein Dreeve-Client auf Grundlage vermuteter Endpunkte implementiert.
- Strava: Noch keine OAuth-App für BICYCLE registriert. Anmelde-/Tokenfluss und
  Einsatz als persönliche oder gemeinsam bereitgestellte App müssen geklärt
  werden; kein gemeinsames Client-Secret in die APK einbauen. Ein angenommener
  Uploadauftrag allein darf noch keinen erfolgreichen Aktivitätsupload bedeuten.
- Beide erhalten eigene Einstellungen und Icons unter Daten & Speicher →
  Streckendaten, eigene Jobs sowie die gemeinsame ID-Zuordnung. Erst anhand
  bestätigter API-Antworten Erfolgsbelege erzeugen.
- Namensabgleich, Wartungsdaten und Trainingsprogramme bleiben spätere Schritte.

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

26 gezielte Tests erfolgreich: LiveDataTest, HeartRateConnectionTest (Android
33/35/36), BikesViewTest, IntervalsTest, RideServiceLinksTest und RouteExportTest.
Release-Build inklusive R8 und lintVital erfolgreich. Kein echter Strava- oder
Dreeve-Upload getestet; BLE-Anzahl zusätzlich auf realen Geräten prüfen.
