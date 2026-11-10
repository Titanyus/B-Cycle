# Korrektur Speicherdialog und Bright nach Build150

Gemeldeter Fehler bestätigt: BicycleNavigation.hideNavigationFooter blendet native Standardbuttons „Speichern“/„Abbrechen“ absichtlich aus. Der neue Stildialog hatte genau solche Buttons verwendet; frühere Tests konnten unsichtbare Buttons via performClick auslösen und übersahen damit den Gerätefehler. Jetzt eigene sichtbare Aktionen nebeneinander im Dialoginhalt, gleiche zentrale Buttonkomponente; allgemeine Dialognavigation unverändert. Tests prüfen vor dem Auslösen isShown und eine nichtleere getGlobalVisibleRect-Fläche.

Der Speicherdialog listet zusätzlich Bright als Ausgangs-/Überschreibziel und wählt es ohne eigenen aktiven Stil vor. Speichern erzeugt bei Bedarf einen normalen validierten Bright-Snapshot mit stabiler UUID; weitere Speicherungen überschreiben ihn. MapSourcePicker bindet diesen Snapshot an die bestehende Bright-Zeile, statt eine zweite Bright-Zeile anzulegen. Andere eigene Stile behalten ihre bisherigen Zeilen. Ohne Bright-Snapshot bleibt die ursprüngliche Bright-Vorlage verfügbar; Änderungen an der Arbeitskopie weiterhin separat speichern. Initiales Spinnerereignis überschreibt eingegebenen Namen nicht.

Fünf gezielte MapStyleLibraryTest-Fälle grün, einschließlich sichtbarer Aktionen, Bright neu/überschreiben/laden/einzelner Quellzeile, eigener Namen, Arbeitskopie, Profilwechsel, MAPS/MAPS+PROFILE-Backup und bestehender JSON-Ausgabe. Log map-style-save-visible-bright-tests.log. Typografie/UI-Katalog/Bright/Diff geprüft. Nutzer hat anschließend ausdrücklich neue APK angefordert.

# Interne Kartenstile10.11.2026

Zusätzliche Save-Aktion in der kompakten Editorzeile (jetzt fünf Icons): kleiner Dialog mit „Neuer Stil“/vorhandenen Stilen und Name. Neue Namen erzeugen einen Snapshot, bestehende Auswahl oder derselbe Name (Groß-/Kleinschreibung ignoriert) überschreibt genau den vorhandenen Stil mit stabiler UUID. Aktueller Stil vorausgewählt. Namen1–60 Zeichen, maximal32 Stile, strikt validierte begrenzte JSON-Sammlung. Profilwechsel während des Dialogs verwirft die Save-Aktion. Der bisherige externe JSON-Export bleibt unabhängig und schema-kompatibel.

MapStyleLibrary speichert die gemeinsame Sammlung unter savedVectorStylesV1 in bike-Preferences; ausdrücklich kein per-bike-Scope für diese Sammlung. mapSavedStyleId ist dagegen Teil von BikeUiConfigurationScope und wird je Fahrradprofil gesichert/kopiert/geladen. Gespeichert wird der vollständige vorhandene Export: Objekt-/Outlinefarben, Alpha/Breiten, Schriftklassen und Ebenen einschließlich Cycling-Einstellungen. Die aktive Arbeitskopie bleibt weiterhin in den bisherigen Einstellungen. Edits überschreiben den benannten Snapshot erst durch erneutes Speichern. Ein Überschreiben ändert nicht heimlich die Arbeitskopie anderer Fahrradprofile.

MapSourcePicker fügt nach Bright die gespeicherten Namen unter „Vektorkarten“ an; bestehende Quell-IDs, Rasterkategorien/-positionen und Providerlogik erhalten. Eine echte Auswahl lädt den Snapshot in einer gemeinsamen Preference-Aktualisierung. Ebenenschalter und Cycling-Controls werden mit aktualisiert; unveränderte User-Trackfarben, Streckenauswahl, GPS und Kameraposition erhalten. Bright lädt die Standarddarstellung. Bloßes Öffnen der Einstellungen lädt nicht erneut: auch ein noch nicht gespeicherter Entwurf oder eine Profilwiederherstellung ohne mitgebrachte globale Sammlung bleibt erhalten. Anschließend liest der Karteneditor unmittelbar dieselbe geladene Arbeitskopie.

SettingsBackup/MAPS enthält die Sammlung und aktive Stil-ID als optionale neue Felder. Alte Sicherungen ohne diese Felder bleiben lesbar und löschen keine Sammlung. Bei PROFILE+MAPS-Wiederherstellung werden außerdem die aktiven IDs/Arbeitskopien der Fahrradprofile über den vorhandenen Profilbankpfad validiert und wiederhergestellt. Sammlungen mit ungültigen IDs, doppelten Namen oder fremder/fehlerhafter Stilspezifikation vor Übernahme ablehnen. Die bisherige Gesamtgrößenbegrenzung der Einstellungssicherung bleibt bestehen.

Gezielt geprüft: MapStyleLibraryTest4 Fälle (Snapshot/Draft/Überschreiben/Profile/Validierung und MAPS-Backup; echter Save-Dialog mit Überschreiben/Export/Profilwechsel; reale Einstellungen-/Stilauswahl und Entwurf ohne verfügbare Sammlung; vollständiges MAPS+PROFILE-Backup). Dazu bestehender nativer Editor-/JSON-/Profilfall2 und Raster-/Vektor-/Zoomauswahl1, insgesamt7 relevante unterschiedliche Fälle in den abschließenden Läufen grün. Logs /workspace/.tools/map-saved-styles-final-tests.log und map-saved-styles-profile-backup-tests.log. UI-Vorschau map-editor-outline.png mit fünf Aktionen visuell geprüft. Compile, Typografie/UI-Katalog/Bright/Diff grün; kein Profiling/Gesamtlauf oder APK.

Der erste weiter gefasste Editorlauf (map-saved-styles-tests.log) hatte7/9 grün. Zwei fachfremde Alt-Testannahmen: Flat-Sticky-Header noch SECONDARY erwartet trotz bestehender TERTIARY-Vorgabe; direkter alter VtmBaseMap-Test ohne passende Zoomgrenzen. Keine Änderung an diesen Funktionen/Tests als Nebenarbeit. Die sieben oben genannten aktuellen Funktionsfälle gezielt erfolgreich geprüft; keine Behauptung einer grünen Gesamtsuite.

# Kleiner Karteneditor und Reihenfolge

Auftrag 04.11.2026 nach Build 130. Seit 05.11.2026 auf der Dashboardkarte eigener Stiftbutton neben Auge/Ebenen → Karteneditor. Native Objektauswahl, vier RGBA-Regler 0–255 mit sichtbarem HEX-Wert und Farbvorschau, Linienstile Original / Durchgezogen / Gestrichelt / Gepunktet / Strich-Punkt. Farbe und Linienstil unabhängig, einzelne oder alle Stiländerungen zurücksetzen, JSON exportieren. Keine freie Auswahl der Zeichenreihenfolge beauftragt; die gewünschte feste Reihenfolge ist unten beschrieben.

## Stiländerungen

Kuratiertes Objektverzeichnis: Hintergrund, Wald, Gras/Wiesen/Parks, Wohn-/Gewerbe-/Industrie-/Friedhof-/Krankenhaus-/Schulflächen, Sand/Eis, Wasserflächen/Flüsse/Bäche, Autobahnen/Bundes- und Fernstraßen/weitere übergeordnete Straßen/kleine Straßen/Wege, Radwegehervorhebung/Eisenbahn, Gebäude/Grenzen, Orts-/Straßen-/Gewässernamen und Höhenlinien. Gruppierung folgt den tatsächlichen Bright-Stilen: z. B. sekundäre und tertiäre Straßen teilen dort einen Stil, ebenso Haupt-/Fernstraßen auf Brücken/in Tunneln. Keine Bearbeitung von POI-Icongrafiken oder nicht in Bright gezeichneten Objektklassen.

`MapStyleOverrides` je Fahrradprofil unter `mapStyleOverridesV1`, mit Kopie/Wechsel und strikter Validierung in MAPS-/Profilbanksicherung. Nur bekannte Objekte/Enums, #RRGGBB-Farben, Schema 1 und aktuelle Bright-Basis zulässig. Native Render-XML-Instruktionen vor ThemeLoader anpassen; Originaldateien bright.json/bright-vtm.xml/Sprites unverändert. Eigene Änderungen als Delta, keine direkte Änderung von MapLibre-JSON-Ausdrücken.

Alle Zoomvarianten der zugeordneten nativen Stile anpassen. Originale Transparenz, Breiten, Zoomregeln, Beschriftungspriorität und Textkonturen bleiben. Straßenfarben verändern die Fahrbahnfarbe; die vorhandenen Ränder behalten ihre Farbe, Linienmuster wirken auch auf zugeordnete Ränder. Native VTM-Dash-Texturen über denselben XML-Parser erzeugen, keine manuell gemeinsam genutzten Texturkopien. Linienmuster nur bei Linienobjekten, RGB-Farbe auch für Flächen/Text. Seit Folgeauftrag 05.11.2026 reine RGB-Farben im 48-ms-Fenster direkt in vorhandenen VTM-Stilen aktualisieren. Textfarben/Muster weiterhin 180 ms bündeln und neu aufbauen; beim Schließen letzte Änderung anwenden. [Live-Farben](vtm-live-colors.md). Schalter bleiben davon unabhängig.

## JSON-Datei

Export über Android ACTION_CREATE_DOCUMENT, MIME application/json, Dateiname B-Cycle-map-style-DATUM.json. UTF-8, außerhalb des UI-Threads schreiben. Export-Snapshot beim Start des Pickers erfassen und bei Activity-Neuerstellung erhalten; Abbruch verwirft den Exportauftrag, Stil bleibt gespeichert. Erfolg/Fehler über bestehende Meldungen.

Datei enthält Format bcycle-map-style, version 1, base bright, baseSha256 der Originalvorlage, objects mit optional color/lineStyle sowie die aktuelle visibility und cycleways. Damit lässt sich die Darstellung nachvollziehbar übergeben. Kein vollständiger Maputnik-/MapLibre-Stil und noch kein Dateiimport in der App beauftragt.

## Zeichnen

Mit Höhenlinien:

1. GROUND: Bright-Hintergrund sowie landcover/landuse/park/water.
2. Höhenlinien als native Vektorlinien.
3. FOREGROUND: übrige Kartengeometrie, insbesondere Straßen/Wege und Gebäude, Wasserläufe/Grenzen/sonstige Objekte.
4. Gemeinsame Beschriftung.
5. Gespeicherte/aktive Tracks und eigener Standort, wie bisher.

VtmBaseMap verwendet nur bei eingeschalteten Höhenlinien zwei gefilterte VTM-Basispässe. Vordergrund hat transparenten Overdraw-Hintergrund, damit Gelände/Höhenlinien darunter sichtbar bleiben. Namen nur einmal am Vordergrund-Tilemanager sammeln. Ohne Höhenlinien weiterhin ein Basispass, kein zweiter Tile-Decoder oder zusätzlicher Puffer. Beide Pässe je ein Loader statt zwei im Einzelpass; gleicher begrenzter HTTP-Diskcache. Kalt-Anfragen gleicher Tile-URL über kurze gestreifte Sperren serialisieren und Antwort vor Freigabe konsumieren, damit der Cache vor dem zweiten Abruf bereit ist. In beiden Pässen wird MVT separat dekodiert; keine zusätzliche MapView/GLSurfaceView, kein Reparenting.

Profil-/Stil-/Layerwechsel behalten Kamera, Footer-/Kartengeometrie, GPS und Track-Layer. Rasterquellen bleiben unverändert. Die Karten-Stiländerungen gelten auch für Streckendetailkarten.

## Verwaltungsgrenzen

Zusätzlicher Schalter „Verwaltungsgebiete zwischen Gemeinden und Landkreisen“ für admin_level 7, beispielsweise Verwaltungsgemeinschaften. Gemeinde-/Ortsteilgrenzen bleiben separat bei admin_level ≥8. Landkreise (typisch Ebene 6) und größere Grenzen unabhängig erhalten. Level 7 zunächst an, um den bisherigen Zustand nicht beim Update zu verändern. In Deutschland ist diese Einordnung plausibel; die genaue Verwaltungsbedeutung stammt aus den jeweiligen OSM-Daten und ist international nicht einheitlich.

## Flat-Sticky-Header

Global im Flat-Design Hintergrund aus PRIMARY berechnen: R/G/B × 0,7, Alpha erhalten. Alle vordefinierten und benutzerdefinierten Farbprofile verwenden dieselbe Formel, auch nach Themenwechsel. Bisherige obere 2-dp-Linie, TERTIARY-Inhalte, Geometrie und Glow-Header unverändert.

## Prüfung

Gezielte native Fälle: tatsächliche VTM-Linientexturen/Farben und ursprüngliche Waldtransparenz nach JSON-/Profil-/Backup-Rundreise, Reihenfolge und Passfilter mit Straße/Gebäude/Gelände/Wasser, kein zusätzlicher Pass ohne Höhenlinien, echte Editorsteuerung/Reset/Export-Snapshot, nativer Dokumentpicker samt UTF-8-Dateiausgabe, berechnete Flat-Farbe für alle Presets. Bestehender Layer-/Popup-/Rasterwechsel und admin_level-7-Filter/Sicherungsrundreise mitgeprüft. Bright-Generator, Typografie, UI-Katalog und Diff-Prüfung erhalten. Kein Gesamtlauf, keine lokale APK. Echtgerät-Lesbarkeit, Dash-Bild und Überdeckung während Zoom weiter beurteilen.

## Folgeänderungen 05.11.2026

[Eigenständiger RGB-Editor, frühere Ortsübersicht und länger sichtbare Radwege](vtm-rgb-and-overview.md). Frühere Wabenpalette nur bei Kartenobjekten ersetzt.

## Kompakter Editor ab 05.11.2026 nach Build 134

Keine Überschrift/Erklärung, Farbfeld rechts neben der Objektauswahl, HEX in deren Beschriftungszeile. Vier Regler mit 48-dp-Touchhöhe ohne bisheriges 8-dp-Padding oben/unten. Objektreset/Gesamtreset/JSON/Schließen als vier zugängliche Iconbuttons mit Tooltips in einer Zeile. Popup belegt nur die benötigte Inhaltshöhe (bei kurzen Screens begrenzt/scrollbar). Letztes ausgewähltes Objekt lokal unter `mapStyleEditorLastTarget` merken, auch nach Schließen/Neustart; kein Bestandteil des Kartenstils.

JSON-Version 1 bleibt kompatibel: optionales ganzzahliges `alpha` 0–255 je Objekt; alte reine RGB-Deltas behalten die nativen Transparenzen. Gleiches Schema in Profilen/Sicherung/Export. Alpha wird bei Flächen/Linien/Hintergrund direkt über native Live-Stile gesetzt, bei Text über den bisherigen Atlas-Neuaufbau. Ohne Alpha-Override genaue Transparenz je Stilvariante/Zoom erhalten. Die eine Objektauswahl zeigt den ersten passenden Basisstil (Wald 26, kleine Höhenlinie 102); eine explizite Änderung setzt den gewählten Wert für alle Varianten dieser Objektgruppe.

## Linienbreite und eigene Feldwege nach Build 135

Linienobjekte bekommen den kompakten Regler „Linienbreite“ (25–300 % des jeweiligen Basisstils; 100 % Standard). Faktor statt fester Pixelzahl erhält unterschiedliche Breiten je Zoom und die Proportion von Füllung/Rand. Optionales ganzzahliges `widthPercent` im bestehenden Objekt-JSON, validiert nur für Linien; Profilkopie/Sicherung/Export verwenden denselben Parser. Breitenänderungen nutzen den 180-ms-Neuaufbaupfad, Farben/Alpha weiterhin Live-Stile.

„Feld- und Wirtschaftswege“ als eigener Editor-Eintrag TRACK. Nur class=track aus den bisher gemeinsamen highway-minor/tunnel-service-track-Regeln abspalten; kleine Straßen/service behalten Bright. Eigene Laufzeitregeln bcycle-track/bcycle-track-tunnel, zunächst halbe bisherige Füllbreite, braun #ccbbaa, gestrichelt 3/2, ohne breite Straßenumrandung. Farbe/Alpha/Muster/Breite getrennt editierbar, bestehender Wege-Schalter erhalten. Referenz-JSON und generiertes Bright-XML bleiben unverändert. Wegeart ist kein Nachweis des tatsächlichen Belags.

Radwegehervorhebung ab VTM6 (unterster Kamera-Zoom) statt9: Breiten 0,35/0,45/0,6/0,75/0,9/1,1/1,4/1,8/2,2 für Zoom6–14, ab15 bisher2,5. Editor-Breitenfaktor wirkt auch hier auf jede Zoomvariante. Zeichnet nur vorhandene Quelldaten; aus niedrigem Quellzoom entfernte Radwege lassen sich durch Stilregeln nicht zurückholen.

Prüfung: drei gezielte Fälle (Editor-/Profil-/Backup-/Export-/Validierungsrundreise samt nativen Feldwegfarben und relativer Breite über mehrere Zooms, Editor-Breitenregler/Wiederöffnen, native Radwege-Übersicht/Schalter/Stil). `map-width-and-tracks-tests.log` erfolgreich; Typografie/UI-Katalog/Bright-Generator/Diff grün, kein Gesamtlauf. Optischer Gerätevergleich noch offen.

## Ortsbeschriftung im Designer

Zunächst geschlossenes Rahmen-Dropdown mit den vier OSM-Klassen city/town/village/hamlet. Darin Klassenauswahl, Gewichts-Dropdown100–900 in100er-Schritten und kompakter Größenregler50–200 % der unveränderten Bright-Schriftgröße. Defaults: city/town900/90 %, village400/100 %, hamlet bisheriger Bold700/100 %. Ein globaler Stilreset setzt auch diese Werte zurück.

Optionales `placeLabels` im gleichen validierten Profil-/Export-/Sicherungsschema, Schlüssel CITY/TOWN/VILLAGE/HAMLET mit ganzzahligem weight/sizePercent. Alte JSON-Dateien ohne diesen Block verwenden die Vorgaben. Native Schriftvarianten nur im vorhandenen 256er-Cache; Gewicht über eine reflektionsfreie AndroidPaint-Paketbrücke und neun einmalig gecachte Android-Typefaces. Füll-/Halo-Paint und Höhen-/Descent-Metriken stimmen überein. Größen-/Gewichtsänderungen laufen über den bisherigen180-ms-Atlasaufbau, reine Farben weiter live.

Gezielter MapPlaceTextEditorTest erfolgreich: vier unabhängige Klassen, keine Änderungen beim Öffnen, exaktes natives Typeface-Gewicht und Schriftgröße/Metriken, erhaltene Priorität, Wiederöffnen, Profil/Sicherung/JSON, Reset und ungültige Gewichte. Nachweis /workspace/.tools/map-place-text-tests.log.

## Belagsregel nach Build138

Straßen-/Wege-Strichmuster werden nun aus bekannten Belagsdaten abgeleitet. Der Designer zeigt dafür einen festen kurzen Hinweis statt manueller Musterwahl; übrige Objektmuster/Farben/Alpha/Breiten erhalten. JSON roadSurfaceStyle=unpaved-dashed dokumentiert die Regel und ihren Vorrang vor alten Straßenmustern. [Umsetzung und Prüfung](map-road-surfaces.md).

## Zweispaltiger Outline-Editor (08.11.2026, nach Build148)

Autobahn, Bundes-/Fernstraße, Landstraße und kleine Straße besitzen zusätzlich einen optionalen `outline`-Block mit `color` (#RRGGBB), `alpha` (0–255) und `widthPercent` (25–300). Derselbe MapStyleOverridesV1-Key und dasselbe Exportformat, alte JSONs unverändert lesbar; strikte Typ-/Bereichsprüfung, Outlines bei anderen Objekttypen nicht akzeptieren. Profilbank und Kartensicherung verwenden den erweiterten gemeinsamen Parser.

Editor: Dropdown ohne separate Objekttyp-/HEX-Zeile, daneben zwei32dp-Farbfelder. Links RGBA/Innenbreite, rechts RGBA/Outlinebreite; rechte Spalte nur bei diesen vier Straßenklassen. Keine R/G/B/A-Texte, Thumb/Track rot/grün/blau/weiß; kontrastgerechter Thumbtext, Accessibility-Bezeichnungen erhalten. Fünf Regler je Spalte mit38dp statt48dp Mittelpunktabstand (rund20% näher). Belagshinweis entfernt, semantische Strichelung bekannter unbefestigter Wege unverändert. Ortsfont-Editor und vier gemeinsame Aktionsicons bleiben. Zuletzt bearbeitetes Objekt weiter merken.

Bei expliziter Outlinebreite ist die resultierende äußere native Linie pro Zoom `Innenbasis×Innenfaktor + (Außenbasis−Innenbasis)×Outlinefaktor`. Dadurch ändert rechts die tatsächliche Randstärke, während links die Straße breiter/schmaler werden kann. Vergleich der Basen per Linien-ID und Zoomvorfahren vor jeder Änderung. Ohne explizite Outlineeinstellung bleibt die ältere gemeinsame Breitenskalierung erhalten; beim ersten Innenbreitenzug wird die bisherige Outline-Stärke festgehalten. Zoom ohne Innenlinie verwendet seine ursprüngliche äußere Linie als Basis.

Reine Innen-/Outlinefarbe oder Alpha bleiben auf dem bestehenden GL-Livepfad: VtmLiveTheme sammelt jetzt auch Casing-Handles, behält eigene Originale je Zoom und setzt beide Shaderfarben von Maskenlinien. compiledStyles entfernt auch Outlinefarben/Alpha, behält Geometrieparameter; canUpdateColorsFrom betrachtet Outlinebreite. Kein Tile-/Atlas-/Layer-Neuaufbau für reine Outlinefarben, native Referenzen bleiben identisch. Breitenänderungen weiter180ms gebündelt. Bright-JSON/XML/Sprites und 12/8-Belagsregel unverändert.

Drei gezielte Editorfälle grün, zwei davon nach Erweiterung für alle vier nativen Straßenklassen sowie Outline-Profil-/Backup-Roundtrip erneut grün. Unabhängige Farben/Alpha/Breiten auf mehreren Zooms, unveränderte Stylehandles bei Livefarben, strikte JSON-Grenzen, linke/rechte Regler und38dp-Höhe, Export/Reset/Wiederöffnen/Profilwechsel geprüft. Logs `/workspace/.tools/map-outline-editor-tests.log` und `map-outline-editor-native-final-tests.log`. Vorschau `app/build/reports/ui/map-editor-outline.png` visuell geprüft. Kein Gesamtlauf/Profiling; optischer Abgleich auf dem Radgerät bleibt offen.
