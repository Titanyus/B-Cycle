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

## Profilbezogene Liniengestaltung (18.10.2026)

Unter Profile → Karten stehen zwei eigenständige Accordions: Aufgezeichnete Strecke und Gespeicherte Strecke. Unter der kleinen Inhaltsüberschrift „Trackdarstellung“ enthält jedes Accordion eine statische Kartenvorschau, ein quadratisches Farbfeld neben dem direkt editierbaren Hexwert sowie Deckkraft- und Breitenregler. Die Breite reicht von 0,5–8 dp in Schritten von 0,5 dp. Die Breite bezeichnet den farbigen Strich; die vorhandene weiße Kontur des Live-Tracks bleibt jeweils 2 dp breiter. Kontur und Farblinie erhalten gemeinsam die gewählte Deckkraft, damit sich die Alpha-Werte nicht ungewollt addieren. Start-/Endpunkte und Positionsmarker behalten ihre eigene Darstellung.

Antippen der Vorschau oder des Farbfelds öffnet eine native, symmetrische Sechseckpalette mit sechs Feldern pro Außenkante (91 Farbfelder). Die Mitte ist bei voller Helligkeit weiß; die Sättigung steigt ringweise bis zum äußeren Rand. Regelmäßige Einzel-Sechsecke und axial mit gleichem Maßstab angeordnete Mittelpunkte verhindern vertikale Verzerrung. Ein gemeinsamer Helligkeitsregler mit zehn Rastpositionen einschließlich Schwarz und voller Helligkeit ersetzt die separate Graureihe. Die Mitte liefert damit alle zehn neutralen Abstufungen. Der Regler ändert nur die angebotenen Farben, schreibt selbst keine Streckenfarbe; erst Antippen eines Farbfelds übernimmt die Auswahl. Anfangshelligkeit aus bestehender Farbe, ohne sie beim Öffnen zu ändern. Keine zusätzlichen Zahlen-/Hex-Eingaben oder Deckkraftregler im Palette-Dialog. Die gewählte Farbe wird unmittelbar übernommen und der Dialog geschlossen. Hexwert, Deckkraft und Breite bleiben direkt im Accordion verfügbar. 0 % Deckkraft ist transparent, 100 % deckend. Änderungen werden sofort gespeichert und auf der Karte aktualisiert. Ungültige Hex-Eingaben ersetzen keinen gültigen Farbwert. Keine neuen Farbauswahl-Abhängigkeiten.

MapRouteStyles speichert beide Stile in mapRouteStylesV1 innerhalb der aktiven Preferences. Der Schlüssel gehört zum Kartenteil von BikeUiConfigurationScope: getrennte Fahrradprofile, Profilübernahme und Sicherungen erhalten ihn. Alte Profile/Sicherungen ohne Schlüssel behalten die bisherigen Vorgaben: aktueller Track Rot, 3 dp farbiger Strich plus weiße Kontur, 100 %; gespeicherte Strecke Dunkelblau beziehungsweise helleres Blau in dunkler Karte, 6 dp, 50 %. Sobald eine Farbe explizit gewählt wurde, bleibt sie bei Tag/Nacht identisch. Keine FIT-/GPS-/Aufzeichnungsänderung.

Die gespeicherte Linie verwendet denselben Stil auch in der Streckendetailkarte. MapLinePreview zeichnet das identische Linienprinzip über einem lokal gebündelten OpenStreetMap-Ausschnitt; keine Livekarte und keine Netzwerkzugriffe während der Vorschau. Helle/dunkle Rasterbilder werden gemeinsam wiederverwendet; die dunkle Variante nutzt MapNightPalette. Quellenhinweis bleibt sichtbar. Rasterquelle: https://tile.openstreetmap.org/14/8663/5678.png (Krumbach, abgerufen 18.10.2026), © OpenStreetMap-Mitwirkende, Kartendaten ODbL: https://www.openstreetmap.org/copyright. WebP-Umwandlung ohne Karteninhaltsänderung.


Profile/Karten ab25.10.2026: Ein horizontaler Abschnittstrenner steht unmittelbar vor Trackdarstellung (MapLineStyleView). Separater Legendenbutton und zugehörige abschließende Linie entfernt; Kartenattribution/Quelleninformationen bleiben wie zuvor verfügbar. Gemeinsamer mapControls-Aufbau für beide Designs und alle Profile; keine Karten-/Track-/Farb-/Speicherlogik geändert.


Nach Build106: gespeicherte Tracks haben drei nummerierte Stile und drei gleichzeitige Plätze, laufende Aufzeichnung bleibt separat. Bestehender saved-Stil migriert zuTrack1. Explizite Platzwahl im Manager und farbige Augen-Buttons im Dashboard: [three-map-tracks.md](three-map-tracks.md).


Veröffentlichung 30.10.2026: Änderungen nach Build 106 sind in **APK 0.8.66 / Build 107** enthalten. [Download und Buildnachweis](build-107.md). Frühere Angaben zu fehlender APK sind Umsetzungshistorie.
