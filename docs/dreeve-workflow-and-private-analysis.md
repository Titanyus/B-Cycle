# Dreeve, Fahrtnamen und vollständige private Auswertung

Stand: 20.10.2026, nach 0.8.13 lokal umgesetzt. Keine neue APK und keine Veröffentlichung dieses Entwicklungsschritts.

## Dreeve-Seite und gemeinsame Auswahl

Eigener Vollbild-Screen über den bestehenden Importbutton, fester nativer Header/Footer und gemeinsame horizontale Abstände. DreeveClient.allActivities lädt alle API-Seiten automatisch in 50er-Anfragen; keine 50-Fahrten-Begrenzung im UI. Fortschritt pro Seite, IDs über Seitengrenzen zusammenführen. Bei Fehler bereits geladene Einträge behalten und über Mehr laden ab der nächsten Seite fortsetzen. Zurück schließt den Screen und beendet weitere Abfragen. Alle gefilterten markiert/demarkiert nur sichtbare, noch nicht importierte Fahrten; bestehende Auswahl außerhalb des Filters bleibt erhalten.

Filter und Sortierung sind ausdrücklich keine zweite Implementierung: RouteQuery und RouteQueryPopup wie im Streckenmanager, mit injiziertem Profilresolver für externe Aktivitäten. Footer-Buttonreferenzen bleiben nach dem Herauslösen in den gemeinsamen Vollbildhost gültig. Dateibestand, Filtermodell und UI-Messung bleiben bei den bestehenden Komponenten.

GET /api/v1/gear liefert Namen zu gearId. Liste zeigt zusätzlich Dauer und Radname; bei fehlgeschlagenem Gear-Abruf bleibt die bekannte ID sichtbar, Fahrtenabruf bleibt möglich. Vor Import expliziter Zuordnungsdialog mit gemeinsamen Spinnerstilen. Ohne Zuordnung ist gültig. DreeveBikeMappings speichert Instanz → Gear-ID → stabiler Profilslot, entfernt nicht mehr vorhandene Profile aus der Vorbelegung. Erst bestätigter Dialog speichert. Neue lokale Einträge erhalten RideProfileColor-Zuordnung mit Profilname und PRIMARY-Rückfall. Kein Fahrrad aus dem Aktivitätsnamen oder Sporttyp erraten. Bereits vorhandene Fahrten nicht heimlich neu zuordnen.

GPX bleibt unverändert; RouteAnalysisRead berechnet Norm. Leistung, Energie, EF, Verhältnis, Best Efforts und FTP-Schätzung nach den bestehenden Qualitäts-/Zeitregeln, soweit tatsächliche Messreihen reichen. Für GPX wird die allgemeine Powerquelle ohne Bosch-spezifisches Festhalten unveränderter Nullwerte verwendet. Reine JSON-Zusammenfassungen erhalten keine erfundenen Reihen oder Trainingsauswertungen.

## Verknüpfung und Namensabgleich

Importierte Dreeve-Fahrten zeigen Mit Dreeve verknüpft samt Haken, auch bei abgeschaltetem automatischem Upload. Nachweis sind Dienst/Instanz/Aktivitäts-ID in RideServiceLinks. Daraus keine Dateiversions-Erfolgsquittung für einen geänderten eigenen FIT-Export machen. Uploadberechtigung importierter Fahrten unverändert.

RideNames speichert lokale Namen und dauerhafte Änderungsaufträge, getrennt von Datei-Uploads. Nach bestätigtem Dreeve-Import mit activityId einen einmaligen GET-Abruf des Namens frühestens drei Sekunden später vormerken. Keine periodische Titelprüfung. Der Dreeve-Uploadworker kann diesen Abgleich unmittelbar ausführen; Job 8305 sichert Wiederaufnahme nach Prozessende. Ältere automatische Leseaufträge derselben Fahrt/Instanz durch den neueren ersetzen.

Manuelle Umbenennung lokal sofort erhalten und als manuell kennzeichnen. Sie hat dauerhaft Vorrang vor einer gleichzeitig/später eintreffenden automatischen Antwort. Aufgaben nach lokalem Ride/Dienst/Konto/Remote-ID zusammenfassen, tokenweise gegen inzwischen ersetzte Aufträge prüfen. Dreeve-Automationsname nach Übernahme an bereits verknüpfte intervals.icu-/Strava-Aktivitäten weiterreichen; spätere Uploadbestätigung eines weiteren Dienstes übernimmt den inzwischen bekannten Namen ebenfalls.

Dreeve: GET/PATCH /api/v1/activities/{id}; intervals.icu: PUT /api/v1/activity/{id}; Strava: PUT /api/v3/activities/{id}. Bestehende verschlüsselte Zugangsdaten, OAuth-Erneuerung und HTTP-Grenzen verwenden. Nie Fahrtdaten erneut hochladen, um nur einen Namen zu ändern. Fehler pro Ziel getrennt behandeln, vorübergehende Fehler mit Android-Backoff; nach korrigierter Konfiguration fehlgeschlagene Aufgaben wieder vormerken. Bei Kontowechsel passende Aufträge pausieren, nicht an ein anderes Konto schicken. Upload-Schalter betrifft Dateiübertragung; bewusst vorgenommene Namensänderungen verknüpfter Fahrten sind davon unabhängig.

Name-Overlay und Revisionsbeobachtung aktualisieren Liste/Titel ohne UI-Neustart. Keine Bibliotheksdatei vom Netzwerkworker aus verändern. Im Uploadmenü bleiben bestätigte Verknüpfungen erhalten, ausstehender/fehlgeschlagener Namensabgleich wird zusätzlich angezeigt. Jede bestätigte externe ID erhalten, insbesondere bei korrigierten FIT-Versionen.

## Vollständige private Aufnahmedaten

Das bisherige Journal ließ abgewählte Rohfelder schon beim Schreiben weg. Neues Journalformat BCY7 speichert alle verfügbaren bekannten Rohfelder und Empfangsereignisse während Aufnahme; GPS nur, wenn Runtime tatsächlich Positionen liefert, und nie bei Indoor. Die bisherige Nutzerauswahl bleibt gesondert im Journal erhalten. BCY4/5/6 bleiben lesbar.

FitWriter filtert für den normalen Export Standardfelder, Entwicklerfelder und GPS ausdrücklich anhand dieser Auswahl. Private vollständige Auswertungsdatei separat atomar unter ride-analysis/<immutable-id>.fit erstellen, bevor das Journal entfernt wird. Öffentliche Original-FIT-ID, Prüfsummen, Dateiversionsbelege, Ordnerexport und Cloud-Uploads verwenden ausschließlich den reduzierten FIT. Keine zweite Sensorverbindung oder neue Messwerterzeugung.

FitLibrary-Metadaten, Karte und Details können die private vollständige Quelle verwenden. Neu verfügbare private Quellen beim Refresh erkennen, statt eine vorzeitig eingelesene reduzierte Vorschau dauerhaft zu behalten. Bei Höhenkorrektur korrigierte Höhenkennzahlen/-reihe übernehmen und übrige vollständige Messwerte erhalten. Details verwenden weiterhin den Export-Dateistempel für Uploadstatus. Private Quelle beim Löschen der lokalen Fahrt entfernen; alte Fahrten ohne private Quelle bleiben auf tatsächlich vorhandene Dateien beschränkt. Bereits verworfene Werte nicht rekonstruieren oder behaupten.

## Streckenmanager und Details

Langer Druck startet Mehrfachauswahl. Aktuelle Markierung sichtbar mit Haken, Auswahlzahl und Rahmen; weitere Tipps wählen, statt Details zu öffnen. Kartenauge im Auswahlmodus ausblenden. Rechts fester Löschen-Button, nur bei Auswahl aktiv. Gemeinsame Bestätigung, IDs auf bestehendem Bibliotheksworker löschen; keine Remote-Löschung. Ungewählte Kartenroute erhalten, ausgewählte aktive Route ausblenden. Profilzuordnung, Namensaufträge und private Analysequelle passend bereinigen. Header-/Android-Zurück beendet zuerst die Auswahl.

Detailtitel: feste bisherige headingSp/Bebas-Neue-Typografie ohne AutoSize. Bei Überlänge ca. 30 dp/s, 1,5 s Anfangspause, 0,8 s Endpause, 0,7 s Ausblenden, dann Wiederholung. Kurze Titel zentriert. Systemweite reduzierte Bewegung respektieren, Animator bei Verbergen/Abhängen beenden. Nach Rad/Datum/Wetter 10 dp Abstand. Vertikale Kennzahlentrenner einschließlich Best Efforts entfernt; horizontale Abschnittstrenner bleiben erhalten. Die weiteren vorgemerkten Iconänderungen bleiben offen.

## Verifikation

136 unterschiedliche gezielte Tests über abgeschlossene Läufe bestanden, keine Fehler/übersprungenen Fälle: HTTP-Verträge, automatische Pagination über 50 Einträge hinaus und Fortsetzung, feste Footer-Navigation, gleiche Filterlogik, Fahrradzuordnung und Formulardrafts, Quelle/Instanz-Abgrenzung, Norm. Leistung aus GPX, private vollständige Werte bei reduziertem FIT, Dateiintegrität und Exportpfade, Journalkompatibilität, Aufnahmeabschluss, Namenspriorität/Koaleszierung/Teilfehler, importierte Verknüpfung bei ausgeschaltetem Upload, Mehrfachlöschen und unveränderte ungewählte Karte, Titeltypografie und Animatorabbau, Details und bisherige Archiv-/Cloudregressionen.

MockWebServer/Robolectric und vorhandene native FIT-Decoder. Kein vollständiger Testlauf, keine neue APK, keine echten Cloudkonten oder physisches Gerät getestet. Reale Dreeve-Automationslaufzeit und Namensänderungen bei den drei Diensten noch am Gerät prüfen.
