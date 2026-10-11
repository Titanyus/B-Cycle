# Kartenlinie, Höhe und Aufnahmestatus

Übernommene Gestaltung der Vorschau:

- Geplante/importierte Strecke: durchgehend, 6 dp breit, Dunkelblau `#123B75`, Deckkraft 50 % (Alpha 128/255), ohne weiße Kontur. Die bestehende Darstellung des gefahrenen Live-Tracks und der Start-/Endpunkte bleibt erhalten.
- Aktuelle Höhe aus dem bestehenden gefilterten GPS-Wert als kompakte Meterangabe rechts am Positionsmarker, schwarzer Text auf halbtransparentem Weiß. Keine verfügbare aktuelle Höhe → keine Anzeige. Label bleibt auch bei Kartendrehung aufrecht; ebenso aktualisiert bei unveränderten Koordinaten.
- Automatische GPS-Verfolgung bei genordeter Karte: horizontal mittig, vertikal bei 2/3 der Kartenhöhe. Fahrtrichtungsansicht behält die vorhandenen 68 %. Manuelles Verschieben und die Strecken-Gesamtansicht behalten ihre bisherigen Kameraregeln.
- Während START…STOPP: statischer roter Kreis (13 dp Durchmesser) unmittelbar vor dem Wert im mittleren globalen Karten-Datenfeld. Kein konfigurierter/verfügbarer Wert → Kreis allein mittig. STOPP entfernt den Kreis sofort, auch während des anschließenden Speicherns/Uploads. Keine neue Zeitberechnung, Animation oder zusätzliche Touchfläche; die drei Datenfelder bleiben frei belegbar.

Komponenten: `RouteTrackLayer`, `MapPanel`, `MapDataFieldsView`. Darstellungstests prüfen Linienbreite/Alpha, verfügbaren bzw. fehlenden Höhenwert, Kameraposition, Aufnahmebeginn/-ende und einen tatsächlich gerenderten mittigen roten Kreis ohne Zahlen. Bestehender Test für GPX-Skalierung während Pinch-Zoom bleibt erhalten.

## Sichtbarkeit über das Auge links oben

Der Kartenbutton öffnet keinen Manager mehr. Das zentrale Lucide-Auge zeigt die geladene Strecke oder blendet sie aus; ausgeblendet mit durchgestrichenem Auge. Auswahl und Sichtbarkeit werden getrennt gespeichert. Die gleiche Trennung verwendet auch das Auge im Streckenmanager, damit Ausblenden keine Strecke abwählt.

Ist noch keine Strecke ausgewählt, lädt die vorhandene Kartenverwaltung die neueste selbst gefahrene FIT-Fahrt mit darstellbaren GPS-Punkten. Importierte Dateien sind kein automatischer Ersatz. Die Ersatz-Auswahl wird ebenfalls gespeichert. Gibt es keine darstellbare Strecke, ist das Auge deaktiviert. Das Erstellen einer neuen Fahrt ohne bisherige Auswahl berücksichtigt diese nach dem bestehenden Bibliotheks-Refresh. Kartenkameraposition, Live-Track und Aufzeichnung werden vom Auge nicht verändert.

## Kompaktere Datenfelder und Sichtbarkeit bei Bewegung

Die drei Kartenwerte verwenden rund 30 % größere Schrift (16–42 sp); die Lichtanzeige
ist jetzt 21 dp groß (vorher 14 dp). Die große Dashboard-Beschriftungsfläche wurde
halbiert, bei gleicher Messwertschrift und gleicher Größe der kleinen Felder.

Kartenbuttons und untere Bedienleiste verschwinden nach fünf Sekunden nur bei
frischer, positiver Rad- oder GPS-Geschwindigkeit. Bei Stillstand oder fehlender
aktueller Geschwindigkeit sind sie sichtbar; Anhalten blendet sie automatisch ein.
Touch und Wiederaufnahme der App machen die Bedienelemente weiterhin sichtbar.
