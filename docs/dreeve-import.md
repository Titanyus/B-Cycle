# Dreeve-Importprototyp

Stand: 20.10.2026. GPX-Prototyp ab 0.8.13; Erweiterung um Zusammenfassungen ab 0.8.14.

Streckenmanager → Strecke importieren → Aus Dreeve importieren öffnet eine eigene einfache Seite. Namen, Startdatum, Distanz, Sportart und GPX-/Zusammenfassungsverfügbarkeit anzeigen; eine oder mehrere Fahrten anhaken und importieren. Alle Seiten werden automatisch in 50er-Anfragen geladen. „Mehr laden“ setzt nach einem Abruffehler fort; vorhandene Auswahl bleibt erhalten. „Neu laden“ erneuert die Liste. Der Importmenüpunkt bleibt sichtbar, aber deaktiviert, bis eine gültige Dreeve-Adresse und ein lesbarer API-Schlüssel hinterlegt sind. Der automatische Upload muss dafür nicht aktiviert sein. Die Konfiguration erfolgt unter Daten & Speicher; auf der Importseite gibt es keinen Konfigurieren-Button.

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

## Herkunft beim Import auswählen

Im Bestätigungsdialog zur Fahrradzuordnung steht die gemeinsame Herkunftsauswahl
**Importiert / Gefahren**. Standard ist Importiert; die Wahl gilt für alle ausgewählten
neuen Fahrten, auch für GPX-lose Zusammenfassungen und Aktivitäten ohne Gear-ID.
Die Auswahl ist auf diesen Import beschränkt und verändert vorhandene Duplikate nicht.
Herkunft bleibt bei Neustart, Umbenennen und vollständiger Sicherung/Wiederherstellung
erhalten. Ältere Zusammenfassungen ohne Herkunftsfeld bleiben Importiert.
Der Import startet auch bei Gefahren keinen Cloud-Upload; GPX/JSON werden dadurch
nicht zu uploadfähigen FIT-Aufnahmen. IN-/OUT-Hinweise berücksichtigen diese Auswahl.

## NP und Performancewerte

Importiertes GPX mit Zeitstempeln und tatsächlichen Leistungsmesswerten verwendet
beim Öffnen der Details dieselbe PerformanceEngine wie die FIT-Auswertung. Berechnet
werden bei ausreichender Datengrundlage NP, mechanische Arbeit/Energie, geschätzte
Kalorien, Best Efforts und eine FTP-Schätzung aus einem vollständigen 20-Minuten-
Leistungsfenster. EF benötigt zusätzlich passende Pulswerte, Geschwindigkeit/Leistung
passende Geschwindigkeitswerte (bei GPX aus GPS und Zeit). Fehlende Daten, unzureichende
Abdeckung oder zu kurze Verläufe bleiben fehlend. Die Berechnung beim Detailabruf
verändert das GPX-Original nicht und erfordert keine vorberechneten GPX-Summaryfelder.
Reine Dreeve-Zusammenfassungen liefern keine Grundlage für NP, FTP, EF oder Best Efforts.
Ein gezielter Import-/Neulade-/Analysetest prüft einen wechselnden 20-Minuten-Verlauf,
damit NP ausdrücklich nicht mit der Durchschnittsleistung verwechselt wird.

Nach 0.8.16: Im Importbildschirm steht oben nur die Anzahl geladener Fahrten; die fachliche Beschreibung liegt im vorhandenen ⓘ-Hilfetext. Der oberste Filter Importstatus bietet Alle Tracks / Noch nicht importiert. Neu importierte oder als Duplikat erkannte Fahrten verschwinden sofort aus der zweiten Auswahl. Filter zurücksetzen stellt Alle Tracks wieder her.

Jede Fahrt hat vier getrennte Zeilen: einzeiliger Titel mit Ellipse; Datum · km · Dauer; Fahrradname (Fahrttyp); GPX-Verfügbarkeit. Vorhandene Fahrten sind mit einem deaktivierten Haken markiert, Importfehler bleiben sichtbar. Die gesamte Zeile bedient die native Checkbox; vollständige Texte bleiben für Accessibility verfügbar.

## Gefahrene GPX, Profil-Radtyp und Pausen

Als Gefahren importierte GPX behalten den bestätigten Dreeve-Herkunftslink. Manuelle Übertragung zu intervals.icu, WebDAV oder anderen aktivierten Diensten erstellt eine FIT-Aktivität aus realen GPS-/Zeit-/Sensorpunkten; keine erfundenen Messwerte. Alle GPX-Punkte müssen gültige, nicht rückwärts laufende Zeitstempel haben. Geplante Tracks und reine Zusammenfassungen bleiben ausgeschlossen; Import allein startet keinen Upload.

Maßgeblich ist der aktuelle Typ des bestätigten lokalen Fahrradprofils. Eine Dreeve-Ride kann lokal daher E-Bike sein. FIT-Übertragung und vollständige Sicherung erhalten diesen Typ; nach Profil-Löschung bleibt der letzte Typ über die stabile Profil-UUID erhalten. Wiederverwendete Slots ordnen alte Fahrten nicht einem neuen Rad zu. Original-GPX/FIT bleiben unverändert. Bereits bei Dreeve gespeicherte Ursprungsaktivitäten werden dadurch nicht automatisch nachträglich umklassifiziert.

Bosch-Flow-GPX kann bei Fahrpausen große Zeitlücken enthalten. Eindeutig stationäre Lücken (vorher Leistung und Kadenz null, mehr als 5 s, höchstens 20 m Versatz und abgeleitete Geschwindigkeit unter 0,7 m/s) werden als Pausen aus aktiver Leistungszeit und NP-Fenstern herausgerechnet. Bewegte/ungeklärte Lücken nicht ausblenden. NP benötigt weiterhin mindestens ein gültiges 30-s-Fenster, mindestens 95 % Powerabdeckung und 95 % gültige NP-Fenster; Halten höchstens 5 s, echte Nullwerte bleiben erhalten. Details zeigen die Qualitätsursache fehlender NP und erkannte Pausen.

ZIP-Dateien verwenden dieselbe Auswahlliste: sofort sichtbar mit mittigem Verarbeitungshinweis, Entpacken/Prüfen im Hintergrund; anschließend Filter, Sortierung, Fastscroller und gezielte Mehrfachauswahl. Noch keine Bibliotheksänderung beim Entpacken. Doppelte Fahrten markieren und überspringen, temporäre Dateien nach Schließen auf dem Bibliotheksworker bereinigen.
