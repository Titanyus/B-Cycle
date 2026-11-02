# Aktueller Stand: Profil-Aufzeichnung und Dropdown-Abstände überarbeitet; letzte APK 0.8.83 / Build 124

## Aktueller Auftrag: Profil-Aufzeichnung zusammenfassen

Gemeinsamer Sticky-Abschnitt „Aufzeichnung“ statt FIT-/Strecken-Aufzeichnung: Fahrt benennen, Höhenkorrektur aus Karte, Rahmen-Accordion „Sensor-Hauptwerte für die .fit-Datei“ (bisherige Erklärung + sechs Quellenwahlen), Rahmen-Accordion „Zu speichernde Daten für die .fit-Datei“ (neue kurze Erklärung + bisherige innere Speichergruppen/Metadaten). Vorhandene Daten/Keys/Listener/Locks erhalten. Dropdownabstände überall bisheriger kleiner controlGap 8 dp: unsichtbare Sensortrenner entfernt, doppelte Quellenzeilen-Paddings durch einzelnen Zeilenabstand ersetzt. [Details](profile-recording-settings.md).

Zwei betroffene bestehende Tests erfolgreich: Profilansicht/Schalterpersistenz und echte Abstände in Flat/Glow × E-Bike/Indoor; Quellenwahl/Locks/Sicherungsrundreise. Offenen Profilfall nach Ergänzung wiederholt. Kein Gesamtlauf, Kompilation/Typografie/Atlas/Diff erfolgreich. Keine APK beauftragt/gebaut; Build 124 enthält diese Änderungen sowie die beiden folgenden Korrekturen noch nicht.

## Aktuelle Korrektur: Footer beim Dashboardwechsel

Nutzer bestätigt: Karte UND Bedienelemente kommen bei sichtbarem Footer zunächst wie ohne Footer herein, beim Loslassen springen sie um. Bisher CENTER_CROP-Aufnahme des gesamten alten MapPanel in neuer Kartengröße; zusätzlich erzwang showFullscreen beim Commit den Footer erneut. Jetzt wird die einzige reale Karte bereits in die Nachbarseite umgehängt und in deren echter Zielhöhe gemessen; die auslaufende Seite bekommt ein begrenztes Standbild. Bei Abbruch/Commit zurücksetzen und Bitmap freigeben. Footer-Zustand über Vorschau/Commit bewahren, normale Vollbildbedienung und erzwungener Footer bei kartenlosen Seiten erhalten. Sichtbare Controls-Ausblendtimer nach Resume wieder planen. Generation/aktive Wischrichtung/Seite/Profil/Lifecycle verwerfen verspätete Aufnahmen vor dem Umhängen.

Vier gezielte Fälle erfolgreich: neue Integration mit beiden Footer-Zuständen inklusive Zoom-Control-Bounds und Abbruch, bestehender Mapless-/echter Dashboardwechsel und verspätete Vorschau nach Abbruch. Nach späterem Guard nur zwei betroffene Fälle wiederholt. Neuer Test reproduzierte vor Fix das erzwungene Footereinblenden. Logs dashboard-footer-before-test.log, dashboard-footer-final-tests.log, dashboard-footer-late-result-tests.log in /workspace/.tools/. [Details](dashboard-page-transition.md). Kompilation/Typografie/Atlas/Diff erfolgreich. Keine neue APK beauftragt/gebaut, Build 124 enthält den Fix noch nicht. Tatsächlichen GL-Neuanschluss/Übergang am Smartphone prüfen.

## Aktuelle Korrektur nach Vergleichsbildern

App-/Maputnik-Screenshots erhalten: ungefüllte Flächen in App grau, Grünflächen ähnlich. Ursache bestätigt: VectorTileLayer.setTheme übernimmt nicht MapRenderers Clear-Farbe; Standard LTGRAY (#cccccc) blieb statt Bright #f8f4f0. VtmBaseMap setzt jetzt theme.mapBackground beim Initialisieren und Wiederaufnehmen, weil neue MapView den gemeinsamen Renderer-Hintergrund zurücksetzt. JSON/Stilfarben unverändert. Neuer Test prüft tatsächliche GL-Clear-Farbe plus Rückkehr nach weiterer MapView: vorher rot, danach grün. Bestehender Raster-/Vektorwechsel ebenfalls grün (2 Fälle). [Details](vtm-bright-style.md). Logs bright-background-before-test.log / bright-background-fixed-tests.log. Kein Gesamtlauf, keine neue APK beauftragt/gebaut. Build 124 enthält den Fix noch nicht. Optischen Abgleich nach Fix am Smartphone bestätigen.

## Aktueller Auftrag abgeschlossen: Bright als Basis und neue APK

Nutzer liefert zweimal dieselbe bright.json und bestätigt ausdrücklich: diese Vorlage möglichst genau übernehmen, vorerst keine eigenen Farb-/Breitenkorrekturen. Unverändert als `assets/styles/bright.json` gespeichert, SHA-256 cda8931095c5101cbad5d2a60fa129600a1a0ad3dea27d802be341fb5eae071f. [Aktuelle Zuordnung, Pflege und Grenzen](vtm-bright-style.md).

- Generator `python3 scripts/build-bright-vtm.py` und `--check`: native XML, 111 lokale Spritevarianten aus referenziertem Atlas, Bericht aller 116 Ebenen. Kein Live-Stileditor/neuer Kartendienst; OpenFreeMap bleibt. VTM-Theme nur über `VtmBrightTheme.load` laden, damit Originalfilter einschließlich numerischer Vergleiche gelten. Keine Änderungen am erzeugten XML von Hand.
- Farben/Deckkraft, Ebenenfolge, Straßenränder/Füllung, Tunnel/Brücken/Auffahrten, Zoomfunktionen, Breiten/Kappen/Dashes übernommen. Gleicher Maßstab: MapLibre-Zoom = VTM-Zoom − 1. Halbe VTM-Extrusionsbreite/feste Breiten statt zusätzlicher nativer Breitenmultiplikation. Schemaalias minor_road/minor und ramp→*_link. Flach bleiben.
- Vorlage selbst: Wald 10 % Deckkraft, keine eigene Ackerfarbe, track mit minor/service, path dünn/gestrichelt. Das ersetzt ausdrücklich die kräftigeren Flächenfarben aus Build 123; keine eigenen Overrides. Nutzer bestätigt nochmals JSON als verbindliche Basis, Mapnik-Vergleich nur Hinweis. App-/Maputnik-Screenshots jetzt erhalten; bestätigter Hintergrundfehler oben korrigiert.
- Grenzen dokumentiert: ganzzahlige Zoomstufen, native Mindestbreite/Antialiasing/Dashquantisierung, Android-Schriften/lokalisierte Namen/Kollision statt Noto-Remote-Glyphen; keine exakte MapLibre-Textplatzierung/Abstände/Halo-Blur. building-top und water-offset ausgelassen, Wasserpattern vorhanden. Originale Daten/Tracks/GPS/Rasterquellen/Profile erhalten; Radhervorhebung weiterhin getrennte Option, für Referenzvergleich aus.
- Vier gezielte Fälle erfolgreich; nach Ergänzungen nur zwei betroffene Fälle wiederholt. Generator/Kompilation/Typografie/Atlas/Diff erfolgreich. Logs bright-json-style-tests.log / bright-json-final-tests.log in /workspace/.tools/. Kein Gesamtlauf. Neue APK anschließend ausdrücklich beauftragt und über GitHub Actions gebaut/veröffentlicht: [0.8.83 / Build 124](build-124.md), code 153. Physischer Bildvergleich offen.

Voriger Stand Build 123: eigene Flächenfarben, ausgehende Karte im Dashboardbild korrigiert (setWillNotDraw(false)), Hauptmenü-Trenner je 12 dp. Diese Korrekturen erhalten. Build 124: neue JSON/XML/111 Spritevarianten/Referenzfarben bytegenau geprüft; die überholten kräftigen Flächenfarben aus Build 123 gelten nicht mehr.

## Erhaltener Stand aus Build 122

Neue APK ausdrücklich beauftragt, ausschließlich über GitHub Actions gebaut und geprüft. [Build 122](build-122.md), [Änderungen](release-0.8.81.md).

- Akkusparmodus je Fahrradprofil direkt nach Radumfang, Standard aus; Profil-JSON/Backup/Booleanvalidierung. Automatische Karte/Dashboard 1 Hz, kurze Diagramme 5 s, Gesamtverlauf weiter 10 s. Sichtbare Graphen vorbereiten; statische Aufnahme-/Zonen-/Warn-/Chevron-Darstellung. Manuelle Gesten, Sensoren/Rohdaten/Aufzeichnung/Analyse unverändert. Bestehenden Bright-Stil nicht umschalten. Neun gezielte Fälle erfolgreich, nach Ergänzung nur drei betroffene Fälle erneut erfolgreich. [Details](profile-battery-saver.md).
- Bright als flache VTM-Adaption, keine direkte MapLibre-JSON-Ausführung; Kennung VECTOR erhalten. Immer Draufsicht, keine Extrusionen; Nord-/Fahrtrichtung/Radwege erhalten. Stil-/Draufsichttest erfolgreich. [Details](vtm-bright-style.md).
- Standort/Genauigkeit/Kompass/Höhe im gleichen GL-Frame wie Karte/Strecken; keine zeichnende Android-Kartenoverlayfläche. Kleine wiederverwendete Texturen. Live-Strecken-Fallback bis neue Worker-Geometrie bereit, bei Aufnahme-/Profilgeneration/Ausblenden verwerfen. Drei gezielte Fälle und echter Mesa/GLES-Pixeltest erfolgreich. [Details](vtm-gps-frame-sync.md).
- Dashboard-/HRV-Nachbarseite beim Ziehen sichtbar, Auswahl/Datum/Chart erst beim Commit. Eine Karteninstanz: unabhängige begrenzte GL-Aufnahme oder aus kartenloser Seite umhängen. Richtungswechsel/Abbruch/späte Antworten abgesichert. Sechs gezielte Fälle erfolgreich. [Details](dashboard-page-transition.md).
- Eine Stressfrage mit acht Positionen 0–7 statt Doppelung. Je zwei Stufen→Intervals 1–4. Bestätigte alte Regelmäßigkeitsrichtung invertieren, Originale erhalten; neue Antwort zuerst, sonst inverse Regelmäßigkeit, sonst alte Vierfachantwort. Fragebogen-/Auswahlschema 3, alte 1/2 lesen. HRV/Indizes/Empfehlungsgewichtung unverändert. Sechs gezielte Fälle inklusive echtem Backup erfolgreich. [Details](hrv-eight-step-stress.md).
- Gemeinsame Farbpalette immer mit FOREGROUND-Auswahlrahmen; schwarze Kontrastalternative entfernt, auch Track3.

Kompilation/Typografie/Atlas/Diff erfolgreich. Kein Gesamtlauf, keine lokale APK. Wirkliche Android-GPU-Ausgabe, schnelle Gesten, Map-Snapshot/Umhängen, Wischgefühl und Akkueffekt noch am Smartphone prüfen. Keine gemessene Akkuersparnis.

## Letzte veröffentlichte APK

[APK 0.8.83 / Build 124](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.83-release-124.apk), code 153, Quelle `f088f6bdc9f95ced2c18643bfc33cfe705034115`, 4204369 Bytes, SHA-256 `ba5fdf923684083fe0f6c067de96b8a84ba1567c81377b1643e07ae2e308e69d`. Dauerhafte Signatur, CI-Prüfsumme, Version/Manifest, ZIP-/ARM64-16-KB-Ausrichtung, vier ABIs, 27 Shader sowie Original-JSON/generierte XML/111 Spritevarianten/Referenzfarben geprüft. [Buildnachweis](build-124.md).

Vorige APK: [0.8.82 / Build 123](build-123.md), davor [0.8.81 / Build 122](build-122.md). Rückfallstand vor Enginewechsel: [Meilenstein 0.8.78 / Build 119](milestone-0.8.78.md), dauerhaft gesichert; nicht verändern.

## Weitere Planung

- Kartenstil im Akkusparmodus ausdrücklich noch nicht wechseln. Minimal: kleine Straßen/Feldwege ausblenden, große Straßen erhalten; Orte/Wasser/Wald/Position/gewählte Strecken bleiben. OLED-/Sparstil und mögliche Dashboardoption noch offen, nicht gebaut. Alle Karten vorerst Draufsicht.
- Maputnik als visueller Browsereditor besprochen; MapLibre-JSON nicht direkt VTM-XML-kompatibel. Kein Live-Stilimport beauftragt.
- Sensorfrequenz/weniger Aufzeichnung waren nur Verständnisfragen: niemals aus Akkuwünschen eine Drosselung von GPS/BLE, Rohdaten oder Analyse ableiten.
- Nächste APK nur auf ausdrücklichen Auftrag ausschließlich GitHub Actions, voraussichtlich 0.8.84 / code 154 / Build 125. Appcode nie mit [skip ci] überspringen. Dauerhafte Signatur, 27 Shader/Bright-Assets und echten direkten Download prüfen.

Downloadnachweis: veröffentlichter direkter Raw-Link abgerufen, 4204369 Bytes, bytegenau identisch zum geprüften Actions-Artefakt; SHA-256 ebenfalls identisch.
