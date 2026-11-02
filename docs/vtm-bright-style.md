# Bright: verbindliche JSON-Vorlage und VTM-Abbildung

Seit dem Auftrag nach APK 0.8.82 / Build 123 ist die vom Nutzer hochgeladene `bright.json` die verbindliche Stilbasis. Beide Uploads waren bytegleich. Die Datei liegt unverändert in [styles/bright.json](../app/src/main/assets/styles/bright.json), SHA-256 `cda8931095c5101cbad5d2a60fa129600a1a0ad3dea27d802be341fb5eae071f`. Eigene Farben/Breiten werden vorerst nicht ergänzt. Damit ersetzt diese Zuordnung die bisherige handgeschriebene Bright-Adaption einschließlich der kräftigeren Flächenfarben aus Build 123.

## Pflege und Zuordnung

- Vorlage im Maputnik-Format v8 bearbeiten: `app/src/main/assets/styles/bright.json`.
- `python3 scripts/build-bright-vtm.py` erzeugt `styles/bright-vtm.xml`, lokale Spritevarianten und [den Zuordnungsbericht](bright-vtm-mapping.json).
- `python3 scripts/build-bright-vtm.py --check` prüft Reproduzierbarkeit einschließlich aller abgeleiteten Symbole. Python/Pillow nur als Entwicklungswerkzeug; keine neue Appbibliothek, kein Python auf dem Gerät.
- XML/Spritevarianten/Bericht nicht von Hand ändern. Vorlage und generierte Dateien gemeinsam committen. Neue unbekannte Paint-/Layout-Eigenschaften oder Ausdrücke werden vom Generator abgelehnt, statt still ignoriert.
- `VtmBrightTheme` kompiliert die ursprünglichen JSON-Filter beim Laden einmalig in Prädikate. Exakte UND-/ODER-Verknüpfungen, Negation, Vorhandensein, Geometrietyp und numerische Vergleiche bleiben erhalten. Pro Geometrie werden ausschließlich passende Ebenenflags an die generierte native VTM-Theme gegeben; deren begrenzter Stilcache und Ressourcenfreigabe bleiben aktiv. Keine JSON-Auswertung pro gezeichnetem Frame, keine Änderung der Geometrie-/Sensordaten.
- Zwei dokumentierte Schemaanpassungen: aktuelles OpenMapTiles `class=minor` entspricht dem alten `minor_road` im Tunnel-Filter; Auffahrten mit Basisklasse plus `ramp=1` entsprechen `motorway_link`/`trunk_link` usw. Originaltags bleiben unverändert.

116 Ebenen inventarisiert, 114 einschließlich Hintergrund abgebildet; zwei verschobene Kopien bewusst ausgelassen: `building-top` wegen gewünschter flacher Gebäude ohne versetzte Oberseite, `water-offset` mangels XY-Flächenverschiebung in VTM. Gebäudegrundrisse und normales Wasser bleiben erhalten. Jede Ebene samt angenäherten/ausgelassenen Eigenschaften steht im Bericht. Keine Extrusionen; native Tilt-/Roll-Sperre und Nord-/Fahrtrichtungsdrehung bleiben erhalten.

Quelle bleibt **OpenFreeMap/OpenMapTiles**. Die in der Referenz enthaltenen MapTiler-/Glyphen-URLs mit `{key}` werden nicht als neuer Dienst aktiviert. Die in der Vorlage referenzierten Sprite-PNG/JSON sind mit Herkunft und Prüfsummen lokal festgehalten, 111 zugeschnittene/skalierte/gedrehte Varianten erzeugt. Wasserwellen, POI-Symbole, Straßenschilder und Richtungspfeile stammen damit aus dem Referenzatlas. Keine zusätzliche HTTP-Abhängigkeit für Symbole. Bestehende Bright-Lizenz/Bibliotheksanzeige und Kartenattribution erhalten.

## Farben und Linien

Farbsyntax `#rgb`, `#rrggbb`, RGB(A), HSL(A), Deckkraft und lineare/exponentielle Zoomfunktionen werden übernommen. Ebenenreihenfolge entspricht der JSON: Landflächen, Wasser/Gebäude, Tunnel, Straßenränder/Fahrbahnen, Brücken und Beschriftungen. Straßenränder und Innenlinien haben getrennte Ebenen; positive Linienbreiten, Kappen, Strichmuster und Einblendgrenzen kommen aus der Vorlage. Relative MapLibre-Strichlängen werden in Pixel umgerechnet. VTM-native Dash-Texturen statt einfacher durchgezogener Ersatzlinien.

Wichtig: VTM verwendet eine Weltbreite von 256 Pixeln bei Zoom 0, MapLibre 512 Pixel. Gleicher geografischer Maßstab bedeutet deshalb **MapLibre-Zoom = VTM-Zoom − 1**. Die Auswertung berücksichtigt das. VTM extrudiert die halbe Linienbreite; die generierte Breite ist entsprechend halbiert. `fix=true` verhindert zusätzliche VTM-Breitenmultiplikation nach Zoom und Breitengrad, die den Abgleich bisher verfälschte.

Die Referenz selbst zeichnet Wald `#6a4` mit 10 % Deckkraft (ARGB `#1a66aa44`), Gras/Wiesen `#d8e8c8`, Park mit 80 % Deckkraft, Wasser `hsl(210,67%,85%)`, Ackerflächen ohne eigene Füllung. Aktuelles OpenMapTiles klassifiziert auch Buschland teilweise als `grass` mit `subclass=scrub`; maßgeblich ist die Klasse. `track` ist in der JSON zusammen mit `minor`/`service` eingeordnet, `path` separat dünn und gestrichelt. Diese gestalterischen Entscheidungen werden auf ausdrücklichen Wunsch zunächst exakt als Basis beibehalten, auch wenn Mapnik anders aussieht. Keine erneuten eigenen grünen Ackerflächen oder besonderen Track-Breiten ohne Auftrag.

## Grenzen gegenüber MapLibre

- VTM wählt Styles beim Laden je ganzzahliger Kachelzoomstufe. Funktionen werden an diesen Stufen korrekt ausgewertet; dazwischen keine kontinuierliche MapLibre-Interpolation. Gebrochene Einblendgrenzen greifen daher bei der nächsten passenden Kachelstufe.
- Native VTM-Linien haben eigene Kanten-/Verbindungsrasterung und bei festen Vollinien eine minimale halbe Breite von 1 Pixel. Sehr dünne Linien können deshalb abweichen; Strichmuster werden in native Texturen quantisiert. Keine exakte Pixelgleichheit behaupten.
- VTM-Beschriftung mit bisherigen lokalisierten Namen, Android-Schriften, Kollisionsregeln und Wiederholung. Farben, Größen, Fett/Kursiv, Halos und Symbole übernommen; Noto-Remote-Glyphen, Buchstabenabstand, MapLibre-Zeilenumbruch, Halo-Blur und exakte Ankerplatzierung fehlen. Namen bleiben in Originalschreibweise. Diese Punkte sind je Ebene ausgewiesen.
- XY-Verschiebungen von Wasserpattern/-kopie sowie Gebäudeoberseiten fehlen. Wassermuster selbst ist vorhanden. Kartenkamera bleibt direkt von oben.
- Radwegehervorhebung ist weiterhin die getrennte, bestehende Profiloption über dem Basisstil; für einen reinen Referenzvergleich ausschalten. Aufzeichnung/Tracks/GPS/Bedienung und Rasterkarten nicht geändert.

## Prüfung und nächster Abgleich

Vier eindeutige gezielte Fälle erfolgreich, kein Gesamtlauf: Palette/Flächen/2D-Kamera; echte öffentliche OpenFreeMap-Kachel mit ursprünglichen wood-/grass-/farmland-Klassen und optionale Radhervorhebung; Straßenhierarchie einschließlich exponentieller Breite, Dash-Textur, Tunnel/Brücken/Auffahrten und numerischer Filter; Quellenwechsel Raster/Vektor samt Profil-/Backupoptionen und erhaltenem Streckenlayer. Nach der Ergänzung nur die neue Breitenprüfung und der betroffene Quellenwechsel erneut ausgeführt. Logs `/workspace/.tools/bright-json-style-tests.log` und `bright-json-final-tests.log`. Generator-Check, Kompilation, Typografie/Atlas/Diff erfolgreich. Keine neue APK beauftragt/gebaut.

Nutzer bietet Vergleichsscreenshots aus App/Maputnik an. Möglichst gleicher geografischer Ausschnitt/Maßstab, Draufsicht/Nordausrichtung, mit Dorf/Wald/Wiesen/Feldwegen/Hauptstraße. Noch keine Bilder eingegangen. Physische GPU-Ausgabe und der optische Abgleich sind damit noch offen; aus Struktur-/Stiltests keine Pixelgleichheit ableiten.

Nächste APK-Paketprüfung: ursprüngliche JSON, generierte XML, zugehörige Sprite-/Pattern-Dateien sowie Bright-Lizenz zusätzlich zu den weiterhin 27 Shadern prüfen. Frühere Build-123-Prüfung der selbst gewählten deckenden Wald-/Ackerfarben gilt nicht mehr; Referenzfarben und Dateiidentität prüfen.
