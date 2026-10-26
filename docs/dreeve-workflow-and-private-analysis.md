# Dreeve, Fahrtnamen und vollständige private Auswertung

Stand: 20.10.2026, ab APK 0.8.14. [Build 51 und Download](build-51.md).

## Dreeve-Seite und gemeinsame Auswahl

Eigener Vollbild-Screen über den bestehenden Importbutton, fester nativer Header/Footer und gemeinsame horizontale Abstände. DreeveClient.allActivities lädt alle API-Seiten automatisch in 50er-Anfragen; keine 50-Fahrten-Begrenzung im UI. Fortschritt pro Seite, IDs über Seitengrenzen zusammenführen. Bei Fehler bereits geladene Einträge behalten und über Mehr laden ab der nächsten Seite fortsetzen. Zurück schließt den Screen und beendet weitere Abfragen. Alle gefilterten markiert/demarkiert nur sichtbare, noch nicht importierte Fahrten; bestehende Auswahl außerhalb des Filters bleibt erhalten.

Filter und Sortierung sind ausdrücklich keine zweite Implementierung: RouteQuery und RouteQueryPopup wie im Streckenmanager, mit injiziertem Profilresolver für externe Aktivitäten. Footer-Buttonreferenzen bleiben nach dem Herauslösen in den gemeinsamen Vollbildhost gültig. Dateibestand, Filtermodell und UI-Messung bleiben bei den bestehenden Komponenten.

GET /api/v1/gear liefert Namen zu gearId. Liste zeigt zusätzlich Dauer und Radname; bei fehlgeschlagenem Gear-Abruf bleibt die bekannte ID sichtbar, Fahrtenabruf bleibt möglich. Vor Import expliziter Zuordnungsdialog mit gemeinsamen Spinnerstilen. Ohne Zuordnung ist gültig. DreeveBikeMappings speichert Instanz → Gear-ID → stabiler Profilslot, entfernt nicht mehr vorhandene Profile aus der Vorbelegung. Erst bestätigter Dialog speichert. Neue lokale Einträge erhalten RideProfileColor-Zuordnung mit Profilname und PRIMARY-Rückfall. Kein Fahrrad aus dem Aktivitätsnamen oder Sporttyp erraten. Bereits vorhandene Fahrten nicht heimlich neu zuordnen.

Seit26.10.2026 bevorzugt der Import unveränderte FIT-Originale über die Dreeve-Dateiimport-API; GPX ist Ersatz, reine Zusammenfassungen bleiben letzter Rückfall. Hash-basierte FIT-IDs, Originalbytes und bestehende Dubletten erhalten; neue Links/Profilzuordnungen auf die tatsächliche lokale FIT-ID. Details und Prüfgrenzen: [Originaldateien](dreeve-original-files-api.md).

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

Langer Druck startet Mehrfachauswahl. Aktuelle Markierung sichtbar mit ACCENT-Haken, Auswahlzahl und rahmenlosem TERTIARY-Hintergrund; weitere Tipps wählen, statt Details zu öffnen. Kartenauge im Auswahlmodus ausblenden. Rechts fester Löschen-Button, nur bei Auswahl aktiv. Gemeinsame Bestätigung, IDs auf bestehendem Bibliotheksworker löschen; keine Remote-Löschung. Ungewählte Kartenroute erhalten, ausgewählte aktive Route ausblenden. Profilzuordnung, Namensaufträge und private Analysequelle passend bereinigen. Header-/Android-Zurück beendet zuerst die Auswahl.

Detailtitel: feste headingSp/Bebas-Neue-Typografie ohne AutoSize. Vollständiges Textlayout hinter festem Clip mit gemeinsamer Timeline: 3 s einblenden, 4 s Halt, 4 s beschleunigen, 18 dp/s, 4 s abbremsen, 4 s Halt, 3 s ausblenden, 2 s leer. Kurze Titel zentriert, deaktivierte Systemanimationen respektieren. Konvention: [Text-Scroller](text-scrollers.md). Nach Rad/Datum/Wetter 10 dp Abstand; horizontale Abschnittstrenner bleiben erhalten.

## Verifikation

136 unterschiedliche gezielte Tests über abgeschlossene Läufe bestanden, keine Fehler/übersprungenen Fälle: HTTP-Verträge, automatische Pagination über 50 Einträge hinaus und Fortsetzung, feste Footer-Navigation, gleiche Filterlogik, Fahrradzuordnung und Formulardrafts, Quelle/Instanz-Abgrenzung, Norm. Leistung aus GPX, private vollständige Werte bei reduziertem FIT, Dateiintegrität und Exportpfade, Journalkompatibilität, Aufnahmeabschluss, Namenspriorität/Koaleszierung/Teilfehler, importierte Verknüpfung bei ausgeschaltetem Upload, Mehrfachlöschen und unveränderte ungewählte Karte, Titeltypografie und Animatorabbau, Details und bisherige Archiv-/Cloudregressionen.

MockWebServer/Robolectric und vorhandene native FIT-Decoder. Kein vollständiger Testlauf; optimierte APK 0.8.14 über GitHub Actions erstellt. Keine echten Cloudkonten oder physisches Gerät getestet. Reale Dreeve-Automationslaufzeit und Namensänderungen bei den drei Diensten noch am Gerät prüfen.


## Korrekturen nach Build 51 (ohne neue APK)

- Die gemeinsame Dialogformatierung leitete Änderungsbenachrichtigungen des
  ursprünglichen Listenadapters nicht weiter. Dadurch konnte die native Dreeve-Liste
  trotz „398 von 398“ beim ersten Seitenbestand bleiben. Der Stylingadapter reicht
  An-/Abmeldungen von Datenbeobachtern jetzt an den ursprünglichen Adapter weiter.
  Native Header/Footer werden nicht nochmals in den Stylingadapter eingepackt;
  wiederholtes Styling fügt den Fortsetzungsfooter nicht doppelt hinzu.
- Strecken-Mehrfachauswahl verwendet TERTIARY ohne Rahmen und den Haken in ACCENT.
- Dreeve-Import bleibt im Importmenü deaktiviert, solange Adresse oder lesbarer
  Schlüssel fehlen. Automatischer Upload ist keine Voraussetzung. Konfiguration
  weiterhin unter Daten & Speicher, ohne zusätzlichen Button auf der Importseite.
- Regression mit acht API-Seiten und 398 Fahrten prüft Änderungsbenachrichtigungen,
  Scrollen zur letzten Fahrt, deren Auswahl und die Auswahl aller 398 Fahrten im
  tatsächlichen Dialog-Styling. Keine APK für diese Korrekturen erstellt.
  Abschlussprüfung: 42 gezielte Tests in ActivityServiceTest, DreeveImportNavigationTest,
  DreeveImportUiTest, DreeveWorkflowTest, FullscreenDialogTest und RouteManagerViewTest
  bestanden; keine Fehler oder übersprungenen Tests. Kein echtes Dreeve-Konto geprüft.

## Vollständige Titellaufschrift

Lange Titel in den Streckendetails zeichnen das vollständige Textlayout in einen
festen Ausschnitt. Kein erneutes Clippen durch TextView nach der Verschiebung; auch
die ursprünglich unsichtbaren letzten Zeichen werden sichtbar. Schriftgröße und
Header bleiben unverändert. Ablauf gemäß [gemeinsamer Text-Scroller-Konvention](text-scrollers.md): 3 s Einblenden → 4 s Halt → 4 s Beschleunigen → 18 dp/s → 4 s Abbremsen → 4 s Halt → 3 s Ausblenden → 2 s leer → Wiederholung. Kurze Titel bleiben zentriert; bei deaktivierten Systemanimationen
gilt die bestehende statische Ellipsenanzeige. Ein nativer Render-Regressionstest
prüft ausdrücklich ein farbiges Titelende außerhalb des anfänglichen Ausschnitts.

## Manueller Namensabruf nach Build 72

Daten & Speicher → Dreeve → **Fahrtnamen synchronisieren**. Mit konfigurierter Verbindung auch bei ausgeschaltetem Upload-Schalter nutzbar. Explizite Aktion, keine Abfrage beim Öffnen und kein periodischer Job. Vorab sichtbarer Hinweis: Dreeve-Namen ersetzen bewusst auch eigene lokale Namen. Ein Fortschrittsstatus und Zähler für aktualisiert/unverändert/fehlgeschlagen erscheinen in der Dreeve-Gruppe.

Nur lokal vorhandene FIT-/GPX-/Zusammenfassungsfahrten mit bestätigter Verknüpfung zur aktuellen Dreeve-Instanz. Bei mehreren Links einer Fahrt zuletzt bestätigte Dreeve-Aktivität wählen, nur ein Abruf pro lokaler Fahrt. Bestehendes GET /api/v1/activities/{id} verwenden. Ausschließlich Namens-Overlay aktualisieren, weder Fahrtdatei/Metadatenformat ändern noch Uploads oder ausgehende Namensänderungen auslösen. Automatischen Namensworker über dessen workerLock serialisieren; veraltete Namensaufträge der übernommenen Fahrt entfernen, damit kein älterer Titel zurückgeschrieben wird.

Beim Netzwerkabruf lokalen Namens-/Auftragszustand festhalten; bei konkurrierender Bearbeitung Antwort nicht übernehmen und als fehlgeschlagen zählen. Link und lokale Datei vor Schreiben erneut prüfen. Kontowechsel, geänderte Verbindungseinstellungen oder geschlossener Screen brechen weitere Übernahmen ab; bereits erfolgreich übernommene Namen bleiben. Teilfehler stoppen andere Fahrten nicht. Automatischer Abgleich außerhalb dieser ausdrücklich angeforderten Aktion behält bisherige Prioritätsregeln. Keine neue Einstellung, Zugangsdaten bleiben im vorhandenen verschlüsselten Dienstspeicher. IN-/OUT-Hilfe aktualisiert; keine neue APK.

Prüfstand manueller Namensabruf: neun gezielte Tests erfolgreich (bestehender/gezielter Abruf, Quellkonto/Linkauswahl, Dateiexistenz, Konflikte/Abbruch/Teilfehler, keine ausgehenden Namensaufträge, HTTP-Verträge und UI). Typografie-/Atlasprüfung erfolgreich. Kein echter Dreeve-Kontentest und keine APK.

## Uploadbestätigung nach Build 72

Nach HTTP-Annahme bis zu 30 s im ersten Worker zeitnah bestätigen (2/3/5/10/10 s). Ist der Import danach noch offen, Android-Ausführung ohne Fehler abschließen und einen persistenten Bestätigungsjob frühestens 30 s später einplanen. Bestehende Importaufträge pro Folgelauf einmal abfragen, keine erneute POST-Anfrage. Keine exponentielle Fehlerwartezeit allein wegen normaler Dreeve-Verarbeitung. Android darf Jobs abhängig von Netz/Energie/Quoten später ausführen; kein exaktes Zeitversprechen. Echte Netzwerkfehler behalten Backoff, Abschalten/Kontowechsel und bestätigte Aktivitäts-ID bleiben geschützt. Menü/Details beobachten vorhandene Revisions-/Preference-Änderungen. 16 gezielte Dienst-/Statusfälle erfolgreich, einschließlich langsamer Bestätigung über mehrere Jobs ohne erneuten Upload, Netzwerkfehler und Abschalten. Kein Live-Dreeve-Test oder APK-Auftrag.

## Kompakte Importliste nach Build90

Drei normale Zeilen statt vier, Fahrradzeile2dp näher an Datum/Strecke. Bestätigt verfügbare FIT/GPX rechts über dem Schalter als bestehende kleine Formatkennzeichen; Status importiert nur durch markierte deaktivierte Auswahl, Fehler weiter sichtbar. Optionale gemeinsame Servermetadatenabfrage nach vollständig geladener Liste, kein Vorabdownload. Archivliste behält vier Zeilen. Druckmarkierung auf die Schalterpille begrenzt, Touchfläche erhalten. Details [Originaldateien](dreeve-original-files-api.md).
