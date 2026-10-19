# Dreeve-Importprototyp

Stand: 19.10.2026, ab 0.8.13.

Streckenmanager → Strecke importieren → Aus Dreeve importieren öffnet eine eigene einfache Seite. Namen, Startdatum, Distanz, Sportart und GPX-Verfügbarkeit anzeigen; eine oder mehrere Fahrten anhaken und importieren. „Mehr laden“ lädt weitere Seiten à 50 Einträge, vorhandene Auswahl bleibt erhalten. „Neu laden“ erneuert die Liste; „Konfigurieren“ öffnet die vorhandene Dreeve-Einstellung.

## Schnittstelle und Daten

Bestehende verschlüsselte Dreeve-Zugangsdaten und Instanzadresse wiederverwenden. Ein bewusster manueller Abruf ist unabhängig vom automatischen Upload-Schalter/WLAN-Filter. `GET /api/v1/activities` mit Rad-Sportartenfilter und dokumentierter Pagination; `GET /api/v1/activities/{activityId}/gpx` für einzelne ausgewählte Fahrten. Redirects bleiben gesperrt, JSON-Antworten und GPX-Downloads begrenzt; GPX maximal 20 MiB/200.000 Punkte wie beim bisherigen Import.

Original-GPX unverändert lokal speichern. Position/Zeit/Höhe und verfügbare GPX-power-/Garmin-TrackPointExtension-Messwerte lesen. Leistung, Puls und Kadenz in bestehende Analyse übernehmen; Temperatur im Original erhalten. Nullleistung/Nullkadenz sind gültig, fehlende Felder bleiben fehlend. Nach erfolgreichem Import verfügbare Felder in der Zeile auflisten. Der Radtyp stammt aus Dreeves expliziter Sportart; Herkunft bleibt importiert. Keine Vermutung eines lokalen Fahrradprofils.

Kein Original-FIT-Download dokumentiert, keine vollständigen E-Bike-/Akkukanäle und keine verlustfreie Messpunktübernahme zugesichert. Ohne GPX/GPS abrufbare Indoor-/manuelle Fahrten sind nicht auswählbar. Zusammenfassungen nicht in Messwertverläufe umwandeln. Dies ist ausdrücklich ein Test des verfügbaren Exports.

## Identität und Ausführung

Alle Bibliotheksänderungen auf MapStates bestehendem einzelnen Worker. Stabile lokale GPX-UUID aus Dreeve-Instanzidentität und Aktivitäts-ID; Zuordnung über RideServiceLinks inklusive Dateihash bestätigen. Bereits zu Dreeve hochgeladene, lokal vorhandene FIT-Fahrten anhand ihrer bestätigten externen ID ebenfalls überspringen. Keine Erkennung beliebiger früherer Dateiimporte ohne externe ID anhand unsicherer Namens-/Distanzvergleiche.

Jede Fahrt separat übernehmen; ein Fehler blockiert andere Auswahlen nicht. Fehlerhafte Zeilen bleiben für erneuten Versuch auswählbar. Fehlgeschlagene Bestätigung neuer Einträge zurückrollen; temporäre Downloads immer entfernen. Schließen beendet weitere Abrufe/Importe, bereits erfolgreich übernommene Fahrten bleiben erhalten. Bestehende Karten-/Streckenauswahl bleibt erhalten. Kein automatischer Cloud-Upload von Importen.

## Prüfung

41 unterschiedliche gezielte Tests bestanden: HTTP-Anmeldung/Pagination/URL-Kodierung, Antwort-/Downloadgrenzen, Abbruch, GPX-Erweiterungen und fehlende Felder, unveränderte Datei, stabiler Name/Datum/Identität, bekannte externe FIT-ID, Wiederholung ohne Duplikat, Teilfehler, vorhandene Upload-/GPX-/Archiv-/Analyse-/Hilferegressionen sowie Mehrfachauswahl und Navigation vom Manager. MockWebServer und Robolectric; kein echter Nutzerkonto-Abruf und kein Gerätetest. Der erste reale Abruf ist vom Nutzer in der App zu prüfen.
