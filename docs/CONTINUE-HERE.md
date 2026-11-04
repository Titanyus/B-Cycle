# Aktueller Stand: einfache Auswahl-Dropdowns SECONDARY; letzte APK 0.8.86 / Build 127

## Aktueller Auftrag abgeschlossen: Dropdown-Hintergrund

Normale Spinner-Auswahllisten verwenden jetzt SECONDARY in Flat und Glow, zentral in BicycleDialogStyle.spinner. Geschlossener Auswahlkopf war bereits SECONDARY. Fahrradtyp/-untergruppe und alle anderen einfachen Spinner erhalten damit dieselbe Fläche. BicycleComponents.dropdownBackground nimmt einen optionalen Fill entgegen; andere Popup-Menüs behalten ihre bisherige TERTIARY-Fläche. Rahmen, Maße, Auswahl und Persistenz unverändert. Atlas neu generiert; keine neue APK beauftragt.

Ein bestehender gezielter Dropdown-/Abstandstest grün (secondary-spinner-test.log in /workspace/.tools/), Kompilation/Typografie/Atlas/Diff ebenfalls. Kein Gesamtlauf; Gerätesichtprüfung offen.

## Vorheriger Auftrag abgeschlossen: neue APK

Version 0.8.86 / Build 127 über GitHub Actions gebaut und geprüft. [Buildnachweis](build-127.md), [Änderungen](release-0.8.86.md). Enthält alle Änderungen seit Build 126 samt Flat-Designversuch; Gerätesichtprüfung offen.

## Flat-Sticky-Designversuch

Vorheriger Stand lokal durch annotierten Git-Tag `design-before-flat-sticky-lines-2026-11-04` gesichert, Ziel `10453de2d9119db0a878f899e07b51ba9b695cd8`. Er enthält alle bisherigen Änderungen einschließlich HRV-Menü und „Dashboards & UI“. Bei Rückkehr spätere fachliche Änderungen nicht pauschal zurücksetzen; gezielt den Designversuch rückgängig machen.

Flat: BicycleSectionHeader zeichnet oben eine 2-dp-Linie mit FOREGROUND/controlBorderAlpha (wie Dropdown-Rahmen). Keine zusätzliche Layout-/Touchhöhe. BicycleSectionCards zeichnet die Inhalts-/Zwischenflächen TERTIARY, einschließlich Accordion-Inhalten und Headergruppen ohne Accordion. Glow-Geometrie/-Farben und Sticky-/Accordion-Funktion bleiben erhalten. In Build 127 enthalten; Gerätesichtprüfung offen.

Zwei gezielte native Tests erfolgreich: Flat→Glow-Rückwechsel, tatsächliche Linien-/Flächenpixel, unveränderte Geometrie/Accordion und Sticky-Pinning/Push/Release/native Drag-Gesten. Log /workspace/.tools/flat-sticky-trial-tests.log. Kompilation/Typografie/Atlas/Diff erfolgreich; kein Gesamtlauf.

## Vorheriger Auftrag abgeschlossen: HRV-Menü und Bereichsname

Haupteinstellungen beginnen mit zentriertem ausgefülltem BicycleSpecialButton „HRV-Messung“ (Herzicon, bisherige Messnavigation). „HRV-Auswertung“ steht als normale Menüzeile im Design der übrigen Einträge zwischen Streckenvorlagen und Statistiken und öffnet die bestehende gemeinsame Tages-/Verlaufsauswertung. Beide Buttons sind aus „HRV & Tagesbefinden“ entfernt; dort bleiben nur Einstellungen. Neue SettingsAction-Werte hinten angehängt; erweiterte Menüauswahl auf ihren bisherigen Enum-Bereich begrenzt.

„Design & UI“ heißt jetzt „Dashboards & UI“, einschließlich zugehöriger Sicherungsbezeichnung und Hilfetext. Keys und Funktionen bleiben erhalten.

Drei gezielte Tests erfolgreich: kompakter Menüaufbau/Positionen/gefüllte Pille/erweitertes Menü, echter direkter Auswertungseinstieg samt Zurücknavigation sowie HRV-Einstellungen/Sicherungsrundreise. Log /workspace/.tools/hrv-menu-tests.log. Kompilation/Typografie/Atlas/Diff erfolgreich; in Build 127 enthalten. Gerätesichtprüfung offen.

## Vorheriger Auftrag abgeschlossen: Track-Schalter und HRV-Slider

Trackrahmen heißen „Aktiver Track“ und „User Track 1–3“; rechts jeweils unabhängiger Sichtbarkeitsschalter. Der bisherige separate mapTrackVisible-Schalter in den Kartenoptionen entfällt, Key und Aufnahmeverhalten bleiben erhalten. User-Tracks verwenden bestehende MapRouteSlots-Sichtbarkeiten (inklusive Masterzustand), keine neuen Einstellungen/Backupfelder. Die Augenbuttons bleiben gekoppelt, Ausblenden entfernt keine Zuordnung. Track-Komponente erst nach allgemeinem observeSettings-Binding anfügen, damit ihre Listener nicht überschrieben werden.

HRV: BicycleSettingSlider mit Zeit direkt im Zugpunkt und kurzen Labels „Ruhephase“/„Messphase“. Bereiche 15–60 s / 60–300 s, 15-s-Schritte, Sperre und Persistenz unverändert. [Details](dropdown-frame.md).

Drei gezielte Fälle grün: HRV-Bereiche/Sicherungsrundreise und Slider-Tastaturänderung; Sichtbarkeitsschalter in den echten Kartenoptionen samt unabhängigen geschlossenen Rahmen und drei Slotzuständen; Bibliotheksfall mit belegten Slots, Master-Sichtbarkeit und unveränderten Zuordnungen. Log /workspace/.tools/track-switch-hrv-slider-tests.log. Kompilation/Typografie/Atlas/Diff erfolgreich. Kein Gesamtlauf; in Build 127 enthalten, Gerätesichtprüfung offen.

## Vorheriger Auftrag abgeschlossen: übrige Rahmen-Dropdowns

Dashboardauswahl, alle vier Trackdarstellungen und „Benachrichtigung anzeigen“ verwenden BicycleFrameDropdown. Auswahl/Listener/Persistenz bleiben erhalten; Benachrichtigungsschalter und Aufklappen sind unabhängig. Anzeigedauer steht vor Position. App-Auswahl verwendet native animierte Switches am rechten Rand; ein horizontaler Trenner steht unter dem Messengerfilter.

Die fünf Cloud-Formulare verwenden allein die Standard-Innenabstände des Rahmens (16 dp unten). Leere Live-Statuszeilen beanspruchen keinen Platz; Meldungen werden bei Inhalt wieder sichtbar. Alle drei Wiederherstellungen haben direkt Lokal/WebDAV im Rahmen; bei HRV darunter getrennt der HRV4Training-CSV-Import. Bestehende SAF-/Passwort-/Bestätigungsabläufe bleiben erhalten. [Details](dropdown-frame.md).

Sieben gezielte Tests erfolgreich (Dashboardauswahl, Trackstil, zwei Benachrichtigungsfälle, drei Backup-/Cloudfälle), einschließlich Auswahlpersistenz, unabhängiger Schalter, direkter Wiederherstellung, Schutzbestätigung und tatsächlichem unteren Rahmenabstand aller fünf Dienste. Log: /workspace/.tools/framed-settings-tests.log. Kotlin-/Ressourcenkompilation, Typografie, Atlas und Diff-Prüfung erfolgreich. Kein Gesamtlauf; in Build 127 enthalten, Sichtkontrolle auf dem Gerät steht aus.

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

[APK 0.8.86 / Build 127](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.86-release-127.apk), code 156, Quelle `fc7bd1c6a8505ceaf301e9370732b0a3cb9c73e6`, 4204677 Bytes, SHA-256 `9751401de637b7e104394904e768e455bddeaf578d402e1dbe59887973e2de5d`. Signatur/CI-Prüfsumme/Version/Manifest/ZIP-/ARM64-16-KB-Ausrichtung, vier ABIs, 27 Shader, Bright-Ressourcen und neue Texte geprüft. [Nachweis](build-127.md).

Vorige APK: [0.8.85 / Build 126](build-126.md). Meilenstein vor Enginewechsel [0.8.78 / Build 119](milestone-0.8.78.md) unverändert erhalten. Rückkehr-Tag für Flat-Designversuch siehe oben.

## Weitere Planung

- Keine automatische Stiländerung im Akkusparmodus. Minimalstil: kleine Straßen/Feldwege ausblenden, große Straßen/Orte/Wasser/Wald/Position/gewählte Tracks erhalten. OLED-/Sparstil und mögliche Dashboardoption noch offen, nicht gebaut.
- Maputnik als Browsereditor; kein Live-JSON-Stilimport beauftragt. Generator ist Entwicklungswerkzeug.
- Sensorfrequenz/weniger Aufzeichnung waren nur Verständnisfragen: keine Drosselung von GPS/BLE/Rohdaten/Analyse aus Akkuwünschen ableiten.
- Nächste APK nur auf ausdrücklichen Auftrag ausschließlich GitHub Actions, voraussichtlich 0.8.87 / code 157 / Build 128. Appcode nie mit [skip ci] überspringen. Dauerhafte Signatur/27 Shader/Bright-Ressourcen und echten direkten Download prüfen.

Downloadnachweis: veröffentlichter direkter Raw-Link abgerufen, 4204677 Bytes, bytegenau identisch zum geprüften Actions-Artefakt; SHA-256 ebenfalls identisch.
