# Karten und Dashboards (ab 0.5.0)

Mapsforge 0.25.0 ist als native Android-View eingebunden. Die Single-Activity,
die vorhandene GPS-Filterung, Sensoren und FIT-Aufzeichnung bleiben bestehen.
Mapsforge und AndroidSVG sind die zusätzlichen Laufzeitbibliotheken;
MapLibre, Compose, Play Services und eine weitere GPS-Quelle werden nicht verwendet.

## Bedienung

Ab 0.6.1 zeigt **☰** ein dunkelrotes Symbolraster (drei Spalten, zwei Zeilen).
Die sechste Kachel **Erweitert** klappt zusätzliche Einstellungen unter einer
1-Pixel-Trennlinie auf. Alle Punkte öffnen eigene Dialoge mit demselben Hintergrund;
Aktionen wie Diagnoseexport und Zurücksetzen benötigen dort einen Buttondruck.
Ab 0.6.2 stehen die Lizenzpunkte am Ende von Erweitert. Unterdialoge kehren
beim Schließen, Abbrechen oder Zurück zum vorherigen Dialog zurück; dessen
aufgeklappte Bereiche und Auswahl bleiben erhalten. Die weißen Menü-Vektoren sind nach dem
bereitgestellten Mockup gezeichnet; eingebettete PNG-Vorschauen waren nicht
als Originaldateien im Arbeitsbereich abrufbar.

- Vier Dashboard-Seiten, jede mit einer der sechs festen Vorlagen: sieben Werte
  mit kleiner Karte, vier mit mittlerer Karte, zwei mit großer Karte oder drei
  kompakte Werte mit Kartenübersicht sowie fünf Werte in zwei Zeilen (oben zwei,
  unten drei) oder ein großer Wert oben mit drei kleineren darunter. Jede Seite
  speichert ihre eigene Belegung.
  Vorlagen unter **☰ → Dashboards** als Vorschau-Icons im 2×3-Raster wählen.
  Zuerst Seite 1–4 wählen, dann eine Vorlage; erst Speichern übernimmt die Auswahl.
  Abbrechen verwirft sie. Die bisherigen sieben Felder bleiben
  auf Seite 1 erhalten. Verdeckte Felder bleiben beim Vorlagenwechsel gespeichert.
- Seitenwechsel über **‹/›** in den Kartenbuttons oder horizontales Wischen auf
  einem Messwertfeld. Wischen auf der Karte bewegt die Karte und wechselt keine Seite.
- **◎** aktiviert die GPS-Verfolgung und fragt nötigenfalls den präzisen Standort
  an. Der Kartenhinweis „GPS aus“ kann ebenfalls angetippt werden.
- **↑/N** wechselt zwischen Fahrtrichtung und Norden, **+/−** ändert den Zoom,
  **⛶** öffnet dieselbe Karte im Vollbild; Zurück oder **↙** schließt das Vollbild.
  Ein Kartentipp macht START/STOPP und das Hauptmenü wieder erreichbar.
- Kartenbuttons und die untere START/Menü-Leiste verschwinden gemeinsam nach
  fünf Sekunden ohne Bedienung. Ein kurzer Kartentipp zeigt beide wieder.
  Die frei werdende Höhe geht vollständig an die Karte; die Datenfelder bleiben gleich groß. Kartenattribution, Position und Strecken bleiben
  sichtbar. Bei aktivierter TalkBack-Tastenerkundung bleiben Buttons sichtbar.
- Manuelles Verschieben pausiert GPS-Folgen. Ein Kartentipp setzt es nicht fort;
  dazu **◎** verwenden. In Fahrtrichtung liegt die Position etwa im unteren Drittel.
  Im Stand bleibt die letzte brauchbare Richtung erhalten; ohne Richtung erscheint
  ein Positionspunkt. Veraltete GPS-Positionen werden nicht als aktuell dargestellt.
- **☰ → Kartenmanager** speichert Kartenquelle, Ausrichtung, Verfolgung sowie
  Sichtbarkeit des Live-Tracks und der importierten Strecke. Zoom und letzte
  Kartenposition bleiben lokal gespeichert.

Die Dashboard-Beschriftungen sind kurz (z. B. Tempo, Distanz, Fahreranteil).
Alle sichtbaren Titel verwenden dieselbe, gemeinsam passende Schriftgröße.
Einheiten stehen rechts vom Messwert und mittig in dessen Zeile.
Ein weißes Fahrrad steht für E-Bike, eine weiße Weltkugel für GPS. Feldauswahl
und TalkBack behalten die ausführlichen Beschreibungen und Quellen.

## Online-Karten und Daten

Standard ist **CyclOSM**:
`https://a.tile-cyclosm.openstreetmap.fr/cyclosm/{z}/{x}/{y}.png`.
Alternativ ist die normale OpenStreetMap-Karte auswählbar. CyclOSM stellt
Radwege, Radfahrstreifen, Fahrradrouten sowie vorhandene Belags-/Qualitätsangaben
in einer fertigen Rasterkarte dar. Die Attribution öffnet die offizielle Legende.

Die Kartenfarben entsprechen dem Anbieter. Fahrradrouten und Belagsdarstellung
können in diesen fertigen Bildern nicht unabhängig umgestaltet oder ausgeblendet
werden. Fehlende OSM-Angaben lassen sich daraus nicht ergänzen. Eine ausgewiesene
Fahrradroute kann unbefestigt sein; „befestigt“ umfasst auch Kopfsteinpflaster und
ist keine Garantie für Rennradtauglichkeit. Eine Klassifikation der importierten
GPX-Strecke nach Untergrund wird nicht berechnet.

Online-Anfragen übertragen den benötigten Kartenausschnitt an den Kartenanbieter.
FIT, GPS-Messung und importierte Dateien werden nicht hochgeladen. Attribution
bleibt dauerhaft sichtbar. Es gibt keine Werbung oder Analyseintegration.

Die App lädt nur zur Anzeige benötigte Kacheln, mit identifizierbarem User-Agent
und zwei Download-Threads. Ab 0.5.1 passt sich der RAM-Cache an die tatsächlich
zum Rendern benötigten Kacheln an, einschließlich Überstand und Drehung. Er wächst
nicht mit der Fahrtdauer und schrumpft bei deutlich kleinerem Kartenausschnitt.
Auf Disk bleiben maximal 512 Kacheln je Quelle. Expires-Angaben werden durch Mapsforge berücksichtigt;
die zusätzliche Cache-Lebensdauer beträgt sieben Tage. Kein Vorladen von Regionen,
kein Massendownload. Normale Cachetreffer ersetzen keine zugesicherte Offline-Karte.
Nicht sichtbare Karten pausieren Render- und Download-Arbeit; die Aufzeichnung
läuft unabhängig weiter. Mapsforge selbst ist frei; die öffentlichen Kartenserver
bieten keine zugesicherte Verfügbarkeit. Bei später größerer Verteilung ist eine
eigene bzw. vertraglich passende Kartenquelle erforderlich.

## Live-Track und GPX

Der rote Live-Track beginnt mit START, wenn **GPS-Route / Position** im
Aufzeichnungsmanager gewählt wurde. Er nutzt dieselben Samples wie das Fahrtjournal,
auch wenn die Activity nicht sichtbar ist. Eine begrenzte Vorschau hält bis zu
20.000 Punkte, dünnt bei Bedarf aus und verbindet GPS-Ausfälle nicht durch gerade
Linien. Die originalen FIT-Punkte werden dadurch nicht ausgedünnt. Ohne gewählte
Positionsaufzeichnung entsteht keine Trackhistorie. STOPP lässt die Vorschau bis
zum nächsten START stehen; nach Prozessende wird sie nicht aus FIT wiederhergestellt.

**☰ → Streckenmanager** bewahrt mehrere importierte GPX-Strecken mit ihrem
Dateinamen auf. **GPX-Strecke importieren** öffnet dort die Android-Dateiauswahl.
Tracks (`trk`), Trackabschnitte (`trkseg`) und Routen (`rte`) werden unterstützt.
Das Auge zeigt jeweils eine Strecke an oder blendet sie aus; die anderen bleiben
auf dem Gerät gespeichert. Ein neuer Import wird ausgewählt, ersetzt jedoch
keine vorhandene Strecke. Auswahl und Dateien bleiben bei einem Neustart erhalten.
Die bisherige einzelne GPX-Datei wird beim Update automatisch übernommen.

Der Mülleimer löscht nach Bestätigung die private Kopie; die ursprüngliche Datei
bleibt erhalten. **Angezeigte Strecke vollständig zeigen** passt den Kartenausschnitt
an. Die gestrichelte violette Linie unterscheidet sich vom Live-Track; Start und
Ende erscheinen grün bzw. rot. Dies ist eine GPX-Streckenverwaltung; lokale
Offline-Basiskarten sind weiterhin nicht implementiert.

Import und Speicherung laufen außerhalb des UI-Threads. Grenzen: 20 MB,
200.000 Punkte, 1.000 Abschnitte und XML-Tiefe 32. DTD/externe Entitäten sind
nicht zugelassen. Falsches Format, ungültige Koordinaten und unvollständiges XML
werden abgewiesen. Ein fehlgeschlagener Import lässt alle bisherigen Strecken und die Auswahl erhalten.
Originaldateien und Verwaltungsindex werden atomar privat gespeichert; die Darstellung wird auf etwa
20.000 Punkte plus Abschnittsenden reduziert. Es gibt keine Routenberechnung,
Abbiegehinweise oder automatische Neuberechnung.

## Offline und Lizenzen

Eine spätere lokale Mapsforge-Quelle kann die Basiskartenebene ersetzen; Position,
Track, GPX und Dashboardgrößen hängen nicht von der Online-Quelle ab. Offline-Datei-
Import, Downloadmanager und ein eigener XML-Kartenstil sind noch nicht implementiert.

- [Mapsforge](https://github.com/mapsforge/mapsforge), LGPL-3.0 mit Ausnahme zu
  §4(d)/§4(e); AndroidSVG unter Apache-2.0. Lizenztexte in der App im Hauptmenü.
- [CyclOSM: Quelle, Funktionen und Nutzungsbedingungen](https://github.com/cyclosm/cyclosm-cartocss-style)
- [OSM-Attribution](https://www.openstreetmap.org/copyright)
- [Kachelnutzung](https://operations.osmfoundation.org/policies/tiles/)

## Prüfung

Automatisierte Tests prüfen GPX-Segmente, Fehler, große Dateien, begrenzten
Trackspeicher, Kursglättung über Norden, Kartenbuttons, Vollbild, vier Vorlagen
und getrennte Feldbelegung. Ab 0.6.0 werden zusätzlich die Fünf-Werte-Vorlage,
kurze Beschriftungen, die gemeinsam ausblendende Bedienleiste mit wachsender Karte
sowie GPX-Verwaltung, Neustart, Löschung, fehlerhafter Import und Migration geprüft. Die Aufzeichnungstests prüfen dieselben Trackpunkte
im Hintergrund und abgewählte Positionen; bestehende Garmin-FIT-Tests bleiben aktiv.

Ab 0.5.1 prüfen zusätzliche Tests echtes Mapsforge-Raster-Rendering mit lokal
gespeicherten Testkacheln bei Zoom, Verschieben und Drehung. Der bisherige feste
RAM-Cache mit 48 Kacheln reproduziert dabei die gemeldeten Lücken; der angepasste
Cache hält alle benötigten Kacheln auch über wiederholte Zeichenzyklen bereit.
Ein weiterer Test prüft das Verkleinern des Caches.

Der Nutzer hat Online-Kacheln auf dem Poco getestet und dabei diese Lücken gemeldet.
Der Nutzer hat die Korrektur auf dem Handy bestätigt. Der Cloud-Zugriff auf den
echten CyclOSM-Kachelserver war durch die Netzwerkfreigabe gesperrt; Sonnentauglichkeit,
Kartenbewegung während einer Fahrt und Energieverbrauch sind hier nicht verifiziert.
