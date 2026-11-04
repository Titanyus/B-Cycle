# Aktueller Stand: Track-Sichtbarkeit und HRV-Slider angepasst; letzte APK 0.8.85 / Build 126

## Aktueller Auftrag abgeschlossen: Track-Schalter und HRV-Slider

Trackrahmen heißen „Aktiver Track“ und „User Track 1–3“; rechts jeweils unabhängiger Sichtbarkeitsschalter. Der bisherige separate mapTrackVisible-Schalter in den Kartenoptionen entfällt, Key und Aufnahmeverhalten bleiben erhalten. User-Tracks verwenden bestehende MapRouteSlots-Sichtbarkeiten (inklusive Masterzustand), keine neuen Einstellungen/Backupfelder. Die Augenbuttons bleiben gekoppelt, Ausblenden entfernt keine Zuordnung. Track-Komponente erst nach allgemeinem observeSettings-Binding anfügen, damit ihre Listener nicht überschrieben werden.

HRV: BicycleSettingSlider mit Zeit direkt im Zugpunkt und kurzen Labels „Ruhephase“/„Messphase“. Bereiche 15–60 s / 60–300 s, 15-s-Schritte, Sperre und Persistenz unverändert. [Details](dropdown-frame.md).

Drei gezielte Fälle grün: HRV-Bereiche/Sicherungsrundreise und Slider-Tastaturänderung; Sichtbarkeitsschalter in den echten Kartenoptionen samt unabhängigen geschlossenen Rahmen und drei Slotzuständen; Bibliotheksfall mit belegten Slots, Master-Sichtbarkeit und unveränderten Zuordnungen. Log /workspace/.tools/track-switch-hrv-slider-tests.log. Kompilation/Typografie/Atlas/Diff erfolgreich. Kein Gesamtlauf und keine neue APK; Gerätesichtprüfung offen.

## Vorheriger Auftrag abgeschlossen: übrige Rahmen-Dropdowns

Dashboardauswahl, alle vier Trackdarstellungen und „Benachrichtigung anzeigen“ verwenden BicycleFrameDropdown. Auswahl/Listener/Persistenz bleiben erhalten; Benachrichtigungsschalter und Aufklappen sind unabhängig. Anzeigedauer steht vor Position. App-Auswahl verwendet native animierte Switches am rechten Rand; ein horizontaler Trenner steht unter dem Messengerfilter.

Die fünf Cloud-Formulare verwenden allein die Standard-Innenabstände des Rahmens (16 dp unten). Leere Live-Statuszeilen beanspruchen keinen Platz; Meldungen werden bei Inhalt wieder sichtbar. Alle drei Wiederherstellungen haben direkt Lokal/WebDAV im Rahmen; bei HRV darunter getrennt der HRV4Training-CSV-Import. Bestehende SAF-/Passwort-/Bestätigungsabläufe bleiben erhalten. [Details](dropdown-frame.md).

Sieben gezielte Tests erfolgreich (Dashboardauswahl, Trackstil, zwei Benachrichtigungsfälle, drei Backup-/Cloudfälle), einschließlich Auswahlpersistenz, unabhängiger Schalter, direkter Wiederherstellung, Schutzbestätigung und tatsächlichem unteren Rahmenabstand aller fünf Dienste. Log: /workspace/.tools/framed-settings-tests.log. Kotlin-/Ressourcenkompilation, Typografie, Atlas und Diff-Prüfung erfolgreich. Kein Gesamtlauf, keine neue APK beauftragt oder gebaut; Sichtkontrolle auf dem Gerät steht aus.

## Erhaltener Stand aus Build 126: Vektorfehler nach Dashboardwechsel

Nutzer meldet nach Build 125 rechteckige Ortsnamen, fehlende/flimmernde Bereiche, scheinbar vertauschte Tiles. Umhängen der GLSurfaceView verliert den GL-Kontext, VTM hält teils alte Textur-IDs. DashboardMapHost ist jetzt dauerhafter Elterncontainer; Seiten enthalten nur Slots. Host folgt aktivem Slot per Layout UND Drag-/Animationsframe. Kein MapPanel-Reparent/Window-Detach auf Wechsel/Abbruch. Echte Footer-/Vollbild-/Zielgrößen bleiben; auslaufender Slot zeigt begrenzte Aufnahme, Softwarecapture delegiert an aktiven MapPanel. [Details](dashboard-page-transition.md).

Vier gezielte Fälle grün: Vektorkarte beide Footerzustände + Cancel/Commit mit 0 Attach/Detach und gleicher Host-/Layer-/Theme-Identität; kartenlose Vorschau mit stabilem Elterncontainer; echte Dashboardintegration; Kartenpixel in auslaufender Softwareaufnahme + Bitmapfreigabe. Logs dashboard-stable-map-tests.log / dashboard-stable-map-picture-test.log in /workspace/.tools/. Kompilation/Typografie/Atlas/Diff grün, kein Gesamtlauf. Echte Geräte-GPU/Zoom-/Text-/Tileausgabe noch bestätigen. Neue APK ausdrücklich beauftragt, gebaut und veröffentlicht: [0.8.85 / Build 126](build-126.md), code 155.

## Erhaltener Stand aus Build 125

Neue APK ausdrücklich beauftragt, ausschließlich über GitHub Actions gebaut, geprüft und veröffentlicht. [Buildnachweis](build-125.md), [Änderungen](release-0.8.84.md).

- GL-Hintergrund korrigiert: VectorTileLayer.setTheme setzt nicht MapRenderers Clear-Farbe. VtmBaseMap gibt theme.mapBackground beim Initialisieren/Wiederaufnehmen weiter; neue MapView setzt gemeinsame Farbe sonst auf LTGRAY. JSON unverändert, keine eigenen Farbänderungen. Neuer Test tatsächlicher Renderer-Clear-Farbe vor Fix rot, danach inklusive Rückkehr von zweiter MapView grün; bestehender Quellenwechsel ebenfalls grün. [Details](vtm-bright-style.md).
- Footer beim Dashboardwechsel: Echte Kartenmaße der Zielseite und auslaufendes Standbild (seit Build 126 durch Slots und permanenten Host, kein Umhängen mehr). Kein CENTER_CROP-Komplettbild als einlaufende Karte. Footer beim Resume/Commit erhalten, normale Vollbild-/Mapless-Bedienung erhalten. Abbruch/Generation/aktive Richtung/Seite/Profil/Lifecycle sichern späte Aufnahmen vor Slotwechsel. Vier gezielte Fälle grün, nach Guard nur zwei betroffen erneut. [Details](dashboard-page-transition.md). Physischer GL-Neuanschluss/Übergang noch prüfen.
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

[APK 0.8.85 / Build 126](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.85-release-126.apk), code 155, Quelle `73dae6f21d7339e31c2f6d3e5cbb9e89e9dc9279`, 4204773 Bytes, SHA-256 `adb2bd9e24b4ca51fa2ced9af061e8687634448a6ac7a30b82f0155946d2646d`. Signatur/CI-Prüfsumme/Version/Manifest/ZIP-/ARM64-16-KB-Ausrichtung, vier ABIs, 27 Shader, JSON/XML/111 Spritevarianten/Referenzfarben und bestehende neue Texte geprüft. [Nachweis](build-126.md).

Vorige APK: [0.8.84 / Build 125](build-125.md). Rückfallstand vor Enginewechsel: [Meilenstein 0.8.78 / Build 119](milestone-0.8.78.md), dauerhaft gesichert; nicht verändern.

## Weitere Planung

- Keine automatische Stiländerung im Akkusparmodus. Minimalstil: kleine Straßen/Feldwege ausblenden, große Straßen/Orte/Wasser/Wald/Position/gewählte Tracks erhalten. OLED-/Sparstil und mögliche Dashboardoption noch offen, nicht gebaut.
- Maputnik als Browsereditor; kein Live-JSON-Stilimport beauftragt. Generator ist Entwicklungswerkzeug.
- Sensorfrequenz/weniger Aufzeichnung waren nur Verständnisfragen: keine Drosselung von GPS/BLE/Rohdaten/Analyse aus Akkuwünschen ableiten.
- Nächste APK nur auf ausdrücklichen Auftrag ausschließlich GitHub Actions, voraussichtlich 0.8.86 / code 156 / Build 127. Appcode nie mit [skip ci] überspringen. Dauerhafte Signatur/27 Shader/Bright-Ressourcen und echten direkten Download prüfen.

Downloadnachweis: veröffentlichter direkter Raw-Link abgerufen, 4204773 Bytes, bytegenau identisch zum geprüften Actions-Artefakt; SHA-256 ebenfalls identisch.
