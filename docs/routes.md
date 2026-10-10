# Gemeinsame Streckenverwaltung

Ab APK 0.6.7; Quellcode und installierbare APK sind im Repository gespeichert.

`MapState` betreibt den vorhandenen Manager weiter: eine gemeinsame Liste und
Kartenauswahl für GPX-Importe und FIT-Aktivitäten. `RouteManagerView` verwendet
native Views mit festem Quellenfilter und recycelnden ListView-Zeilen. Vorschaulinien
enthalten höchstens 97 Punkte; es werden keine zusätzlichen Karten oder Sensoren erzeugt.

## Bedienung

- **FIT-Aufzeichnung**: unveränderte Speicherfeld-/Hauptquellenwahl für die nächste Fahrt.
- **Streckenmanager**: Dropdown Alle, Gefahren, Importiert; Datum, Format, Herkunft,
  verfügbare Distanz/Dauer und Streckenvorschau. Bei GPX ist das Datum der Import;
  bei eigenen FIT-Fahrten der FIT-Zeitstempel. Ohne GPX-Zeitangaben keine erfundene Dauer.
- **Strecke importieren**: GPX/FIT über Androids Dateiauswahl als importierte Strecke.
- **FIT-Fahrt übernehmen**: bestehende FIT-Aufzeichnung über Dateiauswahl als gefahrene Aktivität.
- Auge: die gewählte Strecke ein-/ausblenden. Eine gemeinsame Kartenauswahl.
- ⋮: Ansehen (bei Koordinaten), Umbenennen, unverändertes Original nach Downloads
  exportieren und bestätigtes Löschen aus der Sammlung. Importierte Strecken mit
  Koordinaten sind auch als geplante Strecke auswählbar. Kein Routing/Abbiegehinweis.
- Abbrechen/Schließen eines Unterdialogs kehrt zum vorherigen Dialog zurück.

## Neue Streckeneinstellungen im Quellcode nach APK 0.6.7

Das ⋮ rechts neben der Überschrift öffnet die Streckeneinstellungen. Die
Schalter **Speichern im Zielverzeichnis** und **WebDAV** sind unabhängig.
Standard: lokaler Export nach Downloads/B-Cycle aktiv, WebDAV aus. Androids
Ordnerauswahl gewährt dauerhaften Zugriff auf ein anderes Ziel; bei entzogenem
Zugriff gibt es einen Fehler statt eines stillen Rückfalls nach Downloads.
Abbrechen/Schließen kehrt zur Streckenliste zurück. Karten-Datenfelder liegen
jetzt unter **Dashboards**. Das Auge ist größer und immer weiß; durchgestrichen
bedeutet ausgeblendet.

WebDAV-Eingaben: HTTPS-Nextcloud-Basisadresse (auch mit Installations-Unterordner),
Benutzername, separates App-Passwort, Zielverzeichnis und optional WLAN ohne
Datenlimit. Kein kompletter /remote.php/dav/files/...-Link. Der Verbindungstest
prüft PROPFIND am Benutzerverzeichnis und legt fehlende Zielordner an. Das
App-Passwort wird mit AES-GCM und einem Android-Keystore-Schlüssel verschlüsselt;
Passwörter stehen nicht in Jobs, FIT, Diagnosen oder Backups.

STOPP bewahrt zuerst die originale FIT-Datei privat. Danach wird ein aktivierter
Upload dauerhaft vorgemerkt und ein aktivierter Ordnerexport geschrieben.
Der Upload wartet nicht auf den Ordnerexport und blockiert nicht die Oberfläche.
Androids JobScheduler führt ihn bei passendem Netzwerk im Hintergrund aus,
auch nach Neustart. Vorübergehende Netzwerk-/Serverfehler verwenden exponentielle
Wiederholungen; Zugang-/Zertifikatsfehler sind in der Liste sichtbar und erfordern
korrigierte Einstellungen oder die FIT-Aktion **Zu Nextcloud hochladen / erneut
versuchen**. WebDAV aus pausiert die Warteschlange. Beim Wechsel des Servers oder
Zielordners werden alte Jobs nicht ungefragt auf das neue Ziel umgeleitet; eine
bewusste Wiederholung verwendet den aktuellen Zugang.

Dateinamen bleiben bei Wiederholungen stabil. PUT verwendet If-None-Match: *;
anschließend prüft GET den SHA-256 der gesamten Datei. Eine bereits identische
Zieldatei gilt als gesichert, eine abweichende wird nicht überschrieben.
Es gibt keine Weiterleitungen mit Zugangsdaten, keine Cloud-Löschungen und keine
bidirektionale Synchronisation. Manuelles Löschen in der Sammlung entfernt
weiterhin nur die private Kopie. Appdaten löschen/Deinstallation entfernt auch
Zugang und Warteschlange; externe Ordner und Nextcloud bleiben bestehen.

Android darf Hintergrundjobs verzögern; ein STOPP garantiert eine lokale
Sammlungskopie und eine dauerhafte Upload-Vormerkung, keine sofort abgeschlossene
Netzübertragung. Energiespar-/Netzwerkeinstellungen am Poco und eine echte
Nextcloud-Instanz müssen noch praktisch geprüft werden.

## Dauerhafte Dateien und Migration

Die vorhandene `GpxLibrary` und ihr atomarer Index bleiben erhalten. Der Index
wird um Metadaten und begrenzte Vorschauen erweitert; ältere Einträge werden
auf dem Datei-Worker nachgetragen. GPX-Originale werden nicht neu serialisiert.

`FitLibrary` verwaltet originale private FIT-Dateien und einen atomaren JSON-Index.
Neue Aufzeichnungen werden nach FIT-Finalisierung zunächst atomar privat bewahrt,
dann entsprechend der Streckeneinstellungen exportiert und/oder zum Upload vorgemerkt. Ein Prozessabbruch zwischen
Originalspeicherung und Indexeintrag ist durch das Einlesen neuer privater Dateien
beim nächsten Start abgedeckt. Ein Downloadfehler erhält auch das Journal für die
bestehende Wiederholungsfunktion. SHA-256 dient als stabile Dateiidentität und
verhindert doppelte Sammlungsdateien bei gleichem Inhalt.

Beim Laden/Öffnen werden über MediaStore erreichbare, fertig veröffentlichte alte
B-Cycle-FIT-Dateien aus Download/B-Cycle kopiert, ohne die Downloads zu verändern.
Android kann Zugriffe nach Neuinstallation oder bei fremdem Datei-Eigentümer
beschränken; dafür steht die gezielte Android-Dateiauswahl bereit. Keine allgemeine
Speicherberechtigung oder dauerhafte Berechtigung an Importquellen erforderlich.

Umbenennen ändert ausschließlich den Index. Export schreibt die Originalbytes.
Löschen entfernt die appinterne Kopie, niemals Downloads oder Importquellen. Eine
persistente Ausschlussliste verhindert, dass ein alter Download den gelöschten
Eintrag automatisch wiederherstellt. Bewusster manueller Import bleibt möglich.
Appdaten löschen/Deinstallieren entfernt den privaten Katalog; vorhandene Downloads
bleiben erhalten und können bei ausreichendem Zugriff erneut übernommen werden.

## FIT und Ressourcen

Garmins offizieller FIT-Java-Decoder 21.218.0 validiert die vollständige CRC und
liest Records/Sessions als Stream. Es gibt keine GPX-Konvertierung. Native FIT-
Distanz, Timerdauer und Positionen werden gemäß Profil ausgewertet. Ohne GPS-Punkte
bleibt die Aktivität verwaltbar, aber ohne Kartenaktion. GPS-Lücken bleiben getrennt.

Importgrenzen: FIT 100 MB/600.000 Records; GPX weiterhin 20 MB/200.000 Punkte.
Kartenpunkte werden durch den bestehenden MapTrack begrenzt; nur die ausgewählte
Strecke wird für die Karte vollständig als begrenzte Punktfolge gelesen.
Dateioperationen und Decoder laufen außerhalb des UI-Threads. Der Recording-Worker
schreibt unveränderliche FIT-Originale; allein der MapState-Worker verwaltet den Index.
Keine Datenbank und keine neue GPS-Quelle. Uploads verwenden einen nativen JobService;
OkHttp 4.12.0 ermöglicht die von Android URLConnection abgelehnten WebDAV-Methoden.

Automatisierte Prüfungen decken Garmin-Einheiten/CRC, FIT ohne GPS, unveränderte
Originale, doppelte Importe, private Speicherung trotz Downloadfehler, GPX-
Metadaten und Migration, Auswahl/Rename/Export/Delete nach Neustart sowie
Filter/Recycling/Rücknavigation ab. Die Bedienung am Poco muss noch getestet werden.
