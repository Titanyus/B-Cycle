# RGB-Editor, Ortsübersicht und Radwege

Aufträge 05.11.2026 nach Build 133.

## Eigenständiger Editor

Eigenes Stift-Icon rechts neben dem Ebenenicon (links Auge → Ebenen → Editor). Der Editor ist aus dem Ebenen-Dropdown entfernt und öffnet direkt sein eigenes natives Popup. Drei zentrale BicycleSettingSlider für Rot/Grün/Blau, jeweils 0–255 mit Zahlenwert im Zugpunkt; keine Wabenpalette oder Helligkeitsumrechnung für Kartenobjekte mehr. HEX-Wert und Farbvorschau bleiben sichtbar. Native Objektauswahl, Linie, Original/Reset, Profilpersistenz und JSON-Dateiexport unverändert. Farbpaletten anderer Funktionen, etwa Trackfarben, bleiben erhalten.

Beim Objektwechsel RGB exakt aus gespeicherter/ursprünglicher Objektfarbe lesen, während der UI-Befüllung nichts speichern. Änderungen ergeben exakt Color.rgb(R,G,B), Vorschau weiterhin 180 ms bündeln, beim Schließen letzte Änderung anwenden. Eigener Popup-Lebenszyklus: bei Profilwechsel, Pause, Editor für Dashboard-Feldbelegung oder Rasterquelle schließen; während offenem Editor Kartencontrols nicht automatisch verstecken. Linke neue Buttonposition bei Breitenberechnung reservieren, Höhe/aktuelle Footer-Inset-Logik beibehalten, keine weitere MapView.

## Frühere Ortsnamen

Die frühere Regel erlaubte vorhandene Namen 0,8 Zoomstufen unterhalb der Klassenreferenz, neue erst 0,2 oberhalb. Das führte beim Reinzoomen zu einer ganzen Stufe Verzögerung gegenüber dem Ausblenden. Jetzt gilt derselbe Grenzwert für beide Richtungen; 240-ms-Fades, Größenpriorität, Abstände und Bevorzugung bereits sichtbarer gleichklassiger Namen erhalten.

Zusätzlich Klassen früher zulassen (VTM-Zoom, MapLibre = VTM −1): Stadt 7,95; town/Kleinstadt 9,45; Dorf 11,45; Weiler 13,45; übrige kleine Orte/Stadtteile 14,45. Regionen/Länder bleiben in ihrer bisherigen frühen Übersicht. Damit sind auf den bedienbaren halben Stufen Städte ab 8, towns ab 9,5, Dörfer ab 11,5, Weiler ab 13,5 und kleine übrige Orte ab 14,5 zugelassen. Kein gleichzeitiges Erzwingen aller Namen bei Kollisionen, keine Änderung von Quelle oder Referenz-Bright-Assets.

Öffentliche OpenFreeMap-Datenprobe Raum Ulm: eine Quellkachel z9 enthält im gepufferten Layer 288 Ortsmerkmale, davon 142 class=town, z10 424 Ortsmerkmale/39 towns; darunter Neu-Ulm, Heidenheim, Biberach, Geislingen, Ehingen und Günzburg. Diese Anzahl ist die der gelieferten Kachel inklusive Puffer, kein Sollwert gleichzeitig sichtbarer Namen. Der wesentliche Engpass war unsere Klassenfreigabe, nicht ausschließlich das Fehlen der Daten.

## Radwegehervorhebung

Bisher ab VTM-Zoom 12 einheitlich 2,5 Pixel. Jetzt ab 9, mit dünneren Strichen in der Übersicht: 9–10 = 0,6; 11 = 0,9; 12 = 1,4; ab 13 = 2,5 (native fixed line width). Schutz gleicher Schalter-/Style-IDs: Radwegehervorhebung und Wege-Layerfilter gelten weiter, native Stiländerung CYCLE/Farbe/Linienmuster greift auch auf die neuen Stufen. Beide optionalen Basispässe verwenden dasselbe Theme, also Radwege weiterhin über Höhenlinien/Gelände.

Erfasst subclass=cycleway sowie path/footway/pedestrian/track mit bicycle=designated/official. Tatsächliche Quellkacheln im Raum Ulm liefern bei z9/z10 je drei passende Merkmale, z11 zwei, z12 dreizehn (unterschiedliche Tileabdeckungen). Die bisherige Zoom-12-Grenze blendete die schon früher vorhandenen Merkmale unnötig aus. Detailradwege, die eine kleine Quellstufe auslässt, können auch mit früherer Freigabe nicht angezeigt werden. Keine großflächigen hochauflösenden Zusatz-Downloads zur Simulation fehlender Übersichtsdaten.

## Nützliche offizielle VTM-Beispiele

Am 05.11.2026 tatsächlich gelesen:

- [ThemeStylerActivity](https://github.com/mapsforge/vtm/blob/master/vtm-android-example/src/org/oscim/android/test/ThemeStylerActivity.java): vorhandene LineStyle/AreaStyle über RuleVisitor und RenderStyle.set aktualisieren, updateStyles und map.render statt vollständigem Layerwechsel. Interessanter nächster Optimierungsschritt für reine Farbänderungen. Nicht blind auf Text, Dash-Texturen und zwei Renderpässe übertragen; in dieser Änderung bleibt der bisher geprüfte Stil-/Layer-Neuaufbau.
- [MapsforgeStyleActivity](https://github.com/mapsforge/vtm/blob/master/vtm-android-example/src/org/oscim/android/test/MapsforgeStyleActivity.java): Stilgruppen/Theme-Menü über XmlRenderThemeStyleMenu, explizite Themefreigabe. Nützliche Vorlage für größere Stilvarianten; unsere Bright-Filter/Profile sind bereits separat integriert.
- [OpenMapTilesMvtActivity](https://github.com/mapsforge/vtm/blob/master/vtm-android-example/src/org/oscim/android/test/OpenMapTilesMvtActivity.java): MVT-Quelle, gemeinsamer HTTP-Cache und eigener LabelLayer. Die Grundarchitektur ist bei uns bereits vorhanden; keine fertige bessere Ortsplatzierung aus diesem Beispiel behaupten.

## Gezielte Prüfung

Echte RGB-UI-Werte 17/92/203 ergeben #115CCB und bleiben nach Objektwechsel erhalten; bestehender Linien-/JSON-/Reset-Fall weiter grün. Tatsächlicher Kartenbutton öffnet separaten Editor, Ebenenpopup enthält keinen verschachtelten Editor, bestehende Live-Map und Rasterumschaltung erhalten. Native Richtungs-/Fade-/Prioritätsfälle mit symmetrischen früheren Schwellen grün. Native Cycleway-Themeprüfung über Stufen 8–13 bestätigt dünne Übersichtslinien, Schalter aus, designated-Fälle und Editorfarbe/Dash-Textur. Typografie/UI-Katalog/Bright-Generator/Diff grün. Keine lokale APK oder neuer APK-Auftrag. Übersichtsdichte und Radwegsichtbarkeit am Gerät weiter beurteilen.
