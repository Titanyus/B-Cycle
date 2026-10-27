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
- **Importieren** unten links (ab APK 0.6.9): anschließend **Geplante Strecke**
  für GPX/FIT als importierte Strecke oder **Aufgezeichnete Fahrt** für eine
  bestehende FIT-Aufzeichnung als gefahrene Aktivität wählen.
- Auge: die gewählte Strecke ein-/ausblenden. Eine gemeinsame Kartenauswahl.
- ⋮: Ansehen (bei Koordinaten), Umbenennen, unverändertes Original nach Downloads
  exportieren und bestätigtes Löschen aus der Sammlung. Importierte Strecken mit
  Koordinaten sind auch als geplante Strecke auswählbar. Kein Routing/Abbiegehinweis.
- Abbrechen/Schließen eines Unterdialogs kehrt zum vorherigen Dialog zurück.

## Neue Streckeneinstellungen ab APK 0.6.8

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

## Kompakte Oberfläche ab APK 0.6.9

Die beiden Importbuttons oberhalb der Liste werden durch **Importieren** unten
links neben **Schließen** ersetzt. Die Auswahl mit Symbolen ordnet eine geplante
GPX-/FIT-Strecke weiterhin als importiert und eine übernommene FIT-Aufzeichnung
als gefahren ein; Datei- und Speicherabläufe bleiben erhalten. Ab Version 0.6.10
öffnet sich diese Auswahl als Dropdown direkt am Importieren-Button, ohne
zusätzlichen Dialog. Schließen des Dropdowns lässt die Streckenliste geöffnet.
Während Dateioperationen ist Importieren gesperrt.

Der feste Filter verwendet ab 0.6.10 dasselbe Dunkelrot wie die Streckenkarten.
Einzeilige Titel enden bei Bedarf mit „…“.
Herkunft, FIT/GPX und private Speicherung werden durch weiße Symbole
oben rechts angezeigt. Ab 0.6.10 zeigen sie Fahrrad, Download-Pfeil, FIT-/GPX-Datei
und Smartphone; ihre Flächen verwenden das Rot des Einstellungsfensters.
Der WebDAV-Status erhält ein zusätzliches Cloud-Symbol
(gesichert, ausstehend/pausiert/laufend oder Fehler). Alle Kennzeichen haben
sprechende Beschreibungen für TalkBack und Tooltips mit den vollständigen Angaben.
Die bisherigen Herkunfts-/Dateiformat-/Speichertextzeilen entfallen. Vor Datum,
Distanz und Dauer stehen Symbole; alle drei Texte verwenden 14 sp. Bei zu wenig
Breite stehen Kilometer und Dauer untereinander, ohne Schriftverkleinerung.
Rechts unten stehen das kleinere weiße Auge und ⋮ ab 0.6.10 nebeneinander
in eigenen 48-dp-Berührungsflächen. Die Vorschau und Datentexte nutzen
dadurch die volle restliche Zeilenbreite.

Ab 0.6.10 haben die Fahrteinträge keine eigene Hintergrundbox mehr. Eine weiße
horizontale Linie von einem Pixel trennt die Einträge; das Filter-Dropdown behält
den zuvor verwendeten dunkleren Rotton.

Ab 0.6.11 sind die Drei-Punkte-Buttons der Einträge mit dem Titelbutton
ausgerichtet; das Auge rückt ebenfalls nach rechts. Die Streckenaktionen
verwenden das gleiche native `ListPopupWindow` wie die Importauswahl, direkt
am jeweiligen Drei-Punkte-Button. Das Schließen des Dropdowns verändert die
Streckenauswahl nicht. Umbenennen und die Bestätigung vor dem Löschen behalten
ihre Dialoge und kehren beim Abbrechen zur vorhandenen Streckenliste zurück.

## Fast-Scroller nach Build92

Der gemeinsame BicycleFastScrollList normalisiert beim Griffziehen den nativen Touch-/Eingabefokus und wendet setSelectionFromTop mit layoutChildren unmittelbar an. Dadurch wird die Zielposition sichtbar, bevor ein weiterer Seitenlayoutpass den alten Anker wiederherstellt. Keine privaten Android-APIs im Produktivcode, keine Änderung an Mehrfachauswahl/Fahrtaktionen. Der Daumen nutzt außerhalb des Drags native Scrolloffset-/Extent-/Range-Metriken statt einer bei Zeilengrenzen springenden Ganzzahl sichtbarer Zeilen. Importliste verwendet denselben Fix. Regression: echte Streckenkarten im FullscreenHost, Touchmodus, zuvor fokussiertes Auge, Drag/Release/Relayout, monotone Daumenposition und keine ausgelöste Fahrtaktion; bestehender Importlisten-Scroller bleibt geprüft.
