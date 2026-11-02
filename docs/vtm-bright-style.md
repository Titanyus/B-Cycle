# Bright als aktueller Vektor-Basisstil (02.11.2026)

Nutzerwunsch nach Build121: vorerst von Liberty auf **Bright** wechseln. Die bestehende VTM-Adaption verwendet jetzt die Bright-Palette aus `https://tiles.openfreemap.org/styles/bright`. Auswahl heißt **OpenFreeMap · Bright**, gespeicherte Quellenkennung `VECTOR` bleibt gleich; keine zusätzlichen Profil-/Backupoptionen.

Aktive Datei: `app/src/main/assets/styles/bright-vtm.xml`. Heller Hintergrund, hellblaues Wasser, klar erkennbarer, deckender Wald, hellere Wiesen, gelbgrüne Ackerflächen und grüne Parkflächen, Bright-Farben für Schule/Gewerbe/Industrie und flache Gebäude. Große Straßen weiterhin orange/gelb, kleine Straßen weiß, Wege braun. Das ist eine VTM-Adaption mit vorhandenen Geometrie-/Beschriftungsregeln; MapLibre-Ausdrücke wie stufenlose Farbinterpolation, Symbole und Zoomstaffelung nicht pixelgleich übernommen. Keine Live-JSON-Stilunterstützung.

Draufsicht und native Tilt-Sperre bleiben; keine Extrusionen, auch keine versetzten Gebäudeoberseiten. Flache Grundrisse, GPS-/Höhen-/Live-Strecken-Korrektur, Nachbarseitenvorschau und optionale Radwegehervorhebung unverändert. Liberty-Datei im aktuellen Quellstand ersetzt; veröffentlichte APK0.8.80/Build121 enthält weiterhin Liberty. Minimal/OLED-Stile nur vorgemerkt.

Bright-BSD-Hinweis mit Herkunft als `Bright-LICENSE.txt` gespeichert und im bestehenden Bibliothekslizenzdialog eingebunden. VTM-Quell-/Lizenzhinweis erhalten.

Nur den bestehenden Stil-/Draufsichttest mit Bright-Wasserfarbe aktualisiert und gezielt ausgeführt: reale Theme-Dekodierung, Hintergrund/Wasser/Autobahn, flache Gebäude bei Zoom13/16/19 sowie gesperrte Kamera bei erhaltener Fahrtrichtungsdrehung. Log `/workspace/.tools/vtm-bright-tests.log`. Stiltest sowie Kompilation/Typografie/Atlas/Diff erfolgreich; kein Gesamtlauf und keine APK. Nächste APK-Publikationsprüfung muss `assets/styles/bright-vtm.xml`/`assets/Bright-LICENSE.txt` statt Liberty sowie27 Shader erwarten.

## Flächenkorrektur nach Build 122

Der Nutzer meldete graue Felder/Wiesen/Wälder. Die bisherige Bright-Adaption zeichnete Wald mit nur 10 % Deckkraft; für OpenMapTiles-landcover-Klassen farmland und scrub fehlten Regeln. wetland verwies zudem auf einen nicht definierten Stil. Diese Lücken sind geschlossen: Wald #a9cf91, Wiese/Gras #d1e7b5, Acker/Obstflächen #e8e5b8, Buschland #c4d6ad und Feuchtflächen #c6ded0, jeweils deckend. Die Unterscheidung folgt der OpenMapTiles-Klasse (z. B. wood/grass/farmland), nicht nur dem OSM-Untertyp forest/meadow/orchard. Vorhandene Zoom-Einblendung bleibt erhalten. Straßen/Wasser/Gebäude und Kartenfunktionen unverändert.

Prüfung: bestehender Stiltest um sechs Flächenfälle bei drei Zoomstufen ergänzt; bestehender echter MVT-Test prüft zusätzlich deckende Regeln für die enthaltenen ländlichen Polygone. Beide betroffenen Tests erfolgreich (0 Fehler); Typografie, Atlas und Diff ebenfalls erfolgreich. Log: `/workspace/.tools/vtm-bright-rural-tests.log`. Keine neue APK ohne Auftrag.
