# Aktueller Stand: APK 0.8.84 / Build 125 veröffentlicht

## Neuer Auftrag nach Build 125

Nutzer meldet schwere Vektorfehler nach Dashboardwechsel: rechteckige Ortsnamen, fehlende/flimmernde Bereiche und scheinbar vertauschte Tiles. Umhängen der GLSurfaceView trennt sie vom Fenster und verliert den GL-Kontext, während VTM teils alte Textur-IDs hält. Jetzt DashboardMapHost als dauerhafter Elterncontainer; Seiten enthalten nur Slots. Host folgt aktivem Slot per Layout/Drag-/Animationsframe. Kein MapPanel-Reparent/Window-Detach auf Wechsel/Abbruch. Echter Footer-/Vollbild-/Zielgrößenbezug bleibt; auslaufender Slot zeigt begrenzte Aufnahme, Softwarecapture delegiert an aktiven MapPanel. [Details](dashboard-page-transition.md).

Vier gezielte Fälle grün: Vektorkarte beide Footerzustände + Cancel/Commit mit 0 Attach/Detach und gleicher Host-/Layer-/Theme-Identität; kartenlose Vorschau mit stabilem Elterncontainer; echte Dashboardintegration; Kartenpixel in auslaufender Softwareaufnahme + Bitmapfreigabe. Logs dashboard-stable-map-tests.log / dashboard-stable-map-picture-test.log in /workspace/.tools/. Kompilation/Typografie/Atlas/Diff grün, kein Gesamtlauf. Echte Geräte-GPU/Zoom-/Text-/Tileausgabe noch bestätigen. Neue APK ausdrücklich anschließend beauftragt: 0.8.85 / code 155, erwarteter Build 126, nur GitHub Actions.

## Aktueller Auftrag abgeschlossen

Neue APK ausdrücklich beauftragt, ausschließlich über GitHub Actions gebaut, geprüft und veröffentlicht. [Buildnachweis](build-125.md), [Änderungen](release-0.8.84.md).

- GL-Hintergrund korrigiert: VectorTileLayer.setTheme setzt nicht MapRenderers Clear-Farbe. VtmBaseMap gibt theme.mapBackground beim Initialisieren/Wiederaufnehmen weiter; neue MapView setzt gemeinsame Farbe sonst auf LTGRAY. JSON unverändert, keine eigenen Farbänderungen. Neuer Test tatsächlicher Renderer-Clear-Farbe vor Fix rot, danach inklusive Rückkehr von zweiter MapView grün; bestehender Quellenwechsel ebenfalls grün. [Details](vtm-bright-style.md).
- Footer beim Dashboardwechsel: reale einzige MapPanel schon in Zielseite messen, auslaufende Karte als begrenztes Standbild. Kein CENTER_CROP-Komplettbild als einlaufende Karte. Footer beim Resume/Commit erhalten, normale Vollbild-/Mapless-Bedienung erhalten. Abbruch/Generation/aktive Richtung/Seite/Profil/Lifecycle sichern späte Aufnahmen vor Umhängen. Vier gezielte Fälle grün, nach Guard nur zwei betroffen erneut. [Details](dashboard-page-transition.md). Physischer GL-Neuanschluss/Übergang noch prüfen.
- Fahrradprofile unter gemeinsamem Sticky „Aufzeichnung“: Fahrt benennen, Höhenkorrektur aus Karte, Rahmen-Accordion Hauptwerte (Erklärung + sechs Quellenwahlen), Rahmen-Accordion zu speichernde Daten (Erklärung + bisherige innere Gruppen/Metadaten). Bestehende Keys/Daten/Listener/Locks erhalten. Dropdowns kleiner controlGap 8 dp: unsichtbare Sensortrenner entfernt, doppelte Quellenzeilen-Paddings durch einzelnen Zeilenabstand ersetzt. Profilansicht/Schalterpersistenz/echte Abstände in Flat/Glow × E-Bike/Indoor sowie Quellenwahl/Sperre/Sicherungsrundreise grün. [Details](profile-recording-settings.md).
- Trainingshilfe-Erklärung nach Nutzerwortlaut mit typografischen Anführungszeichen ersetzt; keine Zonen-/Farblogik geändert. Ressourcenprüfung statt zusätzlicher Unit-Tests.

Acht eindeutige gezielte Fälle erfolgreich; nach Ergänzungen nur betroffene/offene Fälle wiederholt. Generator/Kompilation/Typografie/Atlas/Diff erfolgreich. Kein Gesamtlauf/keine lokale APK. Logs in /workspace/.tools/: bright-background-fixed-tests.log, dashboard-footer-final-tests.log, dashboard-footer-late-result-tests.log, recording-section-tests.log (Quellenfall), recording-section-final-test.log (Profilfall), training-help-copy-resources.log.

## Weiter gültige Basis

- Bright-JSON verbindlich, unverändert assets/styles/bright.json, SHA-256 cda8931095c5101cbad5d2a60fa129600a1a0ad3dea27d802be341fb5eae071f. Generator scripts/build-bright-vtm.py + --check: XML/111 Spritevarianten/Ebenenbericht. Laden über VtmBrightTheme.load für Originalfilter. MapLibre-Zoom = VTM-Zoom − 1. Keine eigenen Farben/Breiten ohne Auftrag, keine direkten Änderungen erzeugter Dateien. VTM-Grenzen/Schriften/Quantisierung dokumentiert; immer Draufsicht, keine versetzten Gebäudeoberseiten. Vergleichsbilder eingegangen, sie haben den Hintergrundfehler gezeigt. Weitere optische Kontrolle offen.
- VTM 0.25.0: Rasterquellen weiterhin verfügbar, OpenFreeMap-Vektor optional; GPS/Höhe/Strecken im gemeinsamen GL-Frame, Live-Strecke bei Worker-Geometriewechsel erhalten. Nur eine Dashboardkarte. [Migration](vtm-migration.md), [GPS](vtm-gps-frame-sync.md).
- Akkusparmodus je Fahrradprofil nach Radumfang, Standard aus: automatische Karte/Dashboard 1 Hz, kurze Diagramme 5 s, Gesamtverlauf 10 s. Statische Aufnahme-/Zonen-/Warn-/Chevron-Anzeigen. Nur Darstellung; Sensoren/Rohdaten/Aufzeichnung/Analyse unverändert. Bestehenden Bright-Stil nicht umschalten. [Details](profile-battery-saver.md).
- Achtstufige Stressfrage statt Doppelung: neue direkte Antwort zuerst, sonst bestätigte inverse alte Regelmäßigkeit, sonst alte Vierfachantwort. Originale erhalten; Intervals je zwei Positionen→1–4, JSON-Schema 3 kompatibel zu 1/2. [Details](hrv-eight-step-stress.md).
- Hauptmenü-Trenner je 12 dp; Track-Farbauswahl FOREGROUND-Rahmen. HRV-/Strecken-/Sicherungsfunktionen gemäß AGENTS/Fachdokumenten erhalten.

## Letzte veröffentlichte APK

[APK 0.8.84 / Build 125](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.84-release-125.apk), code 154, Quelle `daabf8c8aa69fd0b3e2db8f3a9303a474f622f71`, 4204773 Bytes, SHA-256 `eeeb19933205f7f11a4c04f07249951e37fb69abab9fc7d3ffa3787568a67bf4`. Signatur/CI-Prüfsumme/Version/Manifest/ZIP-/ARM64-16-KB-Ausrichtung, vier ABIs, 27 Shader, JSON/XML/111 Spritevarianten/Referenzfarben und neue Texte geprüft. [Nachweis](build-125.md).

Vorige APK: [0.8.83 / Build 124](build-124.md). Rückfallstand vor Enginewechsel: [Meilenstein 0.8.78 / Build 119](milestone-0.8.78.md), dauerhaft gesichert; nicht verändern.

## Weitere Planung

- Keine automatische Stiländerung im Akkusparmodus. Minimalstil: kleine Straßen/Feldwege ausblenden, große Straßen/Orte/Wasser/Wald/Position/gewählte Tracks erhalten. OLED-/Sparstil und mögliche Dashboardoption noch offen, nicht gebaut.
- Maputnik als Browsereditor; kein Live-JSON-Stilimport beauftragt. Generator ist Entwicklungswerkzeug.
- Sensorfrequenz/weniger Aufzeichnung waren nur Verständnisfragen: keine Drosselung von GPS/BLE/Rohdaten/Analyse aus Akkuwünschen ableiten.
- Nächste APK nur auf ausdrücklichen Auftrag ausschließlich GitHub Actions, voraussichtlich 0.8.85 / code 155 / Build 126. Appcode nie mit [skip ci] überspringen. Dauerhafte Signatur/27 Shader/Bright-Ressourcen und echten direkten Download prüfen.

Downloadnachweis: veröffentlichter direkter Raw-Link abgerufen, 4204773 Bytes, bytegenau identisch zum geprüften Actions-Artefakt; SHA-256 ebenfalls identisch.
