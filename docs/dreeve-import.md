# Dreeve-Importprototyp

Stand: 20.10.2026. GPX-Prototyp ab 0.8.13; Erweiterung um Zusammenfassungen ab 0.8.14.

Streckenmanager → Strecke importieren → Aus Dreeve importieren öffnet eine eigene einfache Seite. Namen, Startdatum, Distanz, Sportart und GPX-/Zusammenfassungsverfügbarkeit anzeigen; eine oder mehrere Fahrten anhaken und importieren. Alle Seiten werden automatisch in 50er-Anfragen geladen. „Mehr laden“ setzt nach einem Abruffehler fort; vorhandene Auswahl bleibt erhalten. „Neu laden“ erneuert die Liste; „Konfigurieren“ öffnet die vorhandene Dreeve-Einstellung.

## Schnittstelle und Daten

Bestehende verschlüsselte Dreeve-Zugangsdaten und Instanzadresse wiederverwenden. Ein bewusster manueller Abruf ist unabhängig vom automatischen Upload-Schalter/WLAN-Filter. `GET /api/v1/activities` mit Rad-Sportartenfilter und dokumentierter Pagination; `GET /api/v1/activities/{activityId}/gpx` für einzelne ausgewählte Fahrten. Redirects bleiben gesperrt, JSON-Antworten und GPX-Downloads begrenzt; GPX maximal 20 MiB/200.000 Punkte wie beim bisherigen Import.

Original-GPX unverändert lokal speichern. Position/Zeit/Höhe und verfügbare GPX-power-/Garmin-TrackPointExtension-Messwerte lesen. Leistung, Puls und Kadenz in bestehende Analyse übernehmen; Temperatur im Original erhalten. Nullleistung/Nullkadenz sind gültig, fehlende Felder bleiben fehlend. Nach erfolgreichem Import verfügbare Felder in der Zeile auflisten. Der Radtyp stammt aus Dreeves expliziter Sportart; Herkunft bleibt importiert. Keine Vermutung eines lokalen Fahrradprofils.

Kein Original-FIT-Download dokumentiert, keine vollständigen E-Bike-/Akkukanäle und keine verlustfreie Messpunktübernahme zugesichert. Ohne GPX können diese Fahrten seit der Erweiterung als Zusammenfassung übernommen werden. Zusammenfassungen nicht in Messwertverläufe umwandeln. Dies ist ausdrücklich ein Test des verfügbaren Exports.

## Fahrten ohne GPX – Zusammenfassung (20.10.2026)

Der allgemeine Aktivitäten-Endpunkt liefert Metadaten auch ohne GPS. Der bisherige GPX-Prototyp zeigte solche Fahrten an, sperrte aber ihren Import. Nach ausdrücklicher Nutzerentscheidung dürfen sie jetzt als Zusammenfassung übernommen werden. Kein GPX-Abruf bei `hasGpx=false`; die Aktivitätenliste enthält bereits die nötigen Kennzahlen, ein zusätzlicher Einzelabruf ist hierfür nicht nötig.

Dokumentierte Felder übernehmen: Dauer, Bewegungszeit, Distanz, Durchschnitts-/Maximalgeschwindigkeit, Aufstieg, Durchschnitts-/Maximalleistung und Puls, Durchschnittskadenz, Energie/Kalorien, jeweils soweit gültig vorhanden. Externe Gear-ID und Gerätebezeichnung erhalten; keine Vermutung eines lokalen Fahrradprofils. `startDateLocal` bleibt im Snapshot erhalten; Listenzeitpunkt einmalig in Gerätezeitzone beim Import auflösen und speichern, bei späterem Zeitzonenwechsel nicht neu interpretieren.

DreeveSummaryLibrary speichert pro Aktivität eine atomare JSON-Datei mit den erlaubten Zusammenfassungsfeldern; keine Zugangsdaten. Eigenes Format JSON im Manager und ausdrücklicher Hinweis in Details. Normales Anzeigen, Filter/Sortierung, Umbenennen, Löschen und lokaler JSON-Export; keine Karte, Diagramme, Best Efforts, Norm. Leistung oder Zonenverteilungen. Keine synthetischen FIT-Dateien/Messpunkte und kein automatischer Cloud-Upload. Wiederholungsimport erzeugt keine Kopie; spätere Umwandlung einer bereits importierten Zusammenfassung in GPX ist nicht automatisiert.

## Identität und Ausführung

Alle Bibliotheksänderungen auf MapStates bestehendem einzelnen Worker. Stabile lokale UUID für GPX und Zusammenfassung aus Dreeve-Instanzidentität und Aktivitäts-ID; Zuordnung über RideServiceLinks inklusive Dateihash bestätigen. Bereits zu Dreeve hochgeladene, lokal vorhandene FIT-Fahrten anhand ihrer bestätigten externen ID ebenfalls überspringen. Keine Erkennung beliebiger früherer Dateiimporte ohne externe ID anhand unsicherer Namens-/Distanzvergleiche.

Jede Fahrt separat übernehmen; ein Fehler blockiert andere Auswahlen nicht. Fehlerhafte Zeilen bleiben für erneuten Versuch auswählbar. Fehlgeschlagene Bestätigung neuer Einträge zurückrollen; temporäre Downloads immer entfernen. Schließen beendet weitere Abrufe/Importe, bereits erfolgreich übernommene Fahrten bleiben erhalten. Bestehende Karten-/Streckenauswahl bleibt erhalten. Kein automatischer Cloud-Upload von Importen.

## Erweiterung: vollständige Seite und Zuordnung

Filter/Sortierung gemeinsam mit dem Manager, automatische Seitennachladung, Fahrradzuordnung und Namensabgleich sind umgesetzt. Architektur und aktuelle Verifikation: [Dreeve-Workflow und private Auswertung](dreeve-workflow-and-private-analysis.md).

## Prüfung

Aktuelle Erweiterung: 40 gezielte Tests bestanden (API-/Uploadablauf, begrenzte Bestätigungsabfragen und Abbruch, geöffnetes Uploadmenü mit Änderung vom Hintergrundthread, GPX-Import, Zusammenfassungsimport ohne GPX-Request, JSON-Speicherung/Neuladen, Analyse ohne erfundene Verläufe, Managerintegration mit Umbenennen/Löschen, Mehrfachauswahl, vorhandene GPX-/Analyse-/Exportnavigation). MockWebServer und Robolectric. Kein echter Nutzerkonto-Abruf und kein Gerätetest; keine neue APK gebaut. Der vorangegangene GPX-Prototyp hatte 41 eigene gezielte Tests bestanden; kein vollständiger Testlauf für diese Erweiterung.
