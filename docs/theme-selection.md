# Theme-Auswahl – Design & UI

## Geänderte Dateien

- `BicycleTheme.kt`: sieben unveränderliche Presets; bestehende Standard-ID bleibt erhalten.
- `BicycleDesign.kt`: gemeinsame, farbunabhängige Größen für Theme-Kacheln.
- `ThemePickerView.kt`: schlankes natives Raster ohne neue Bibliotheken.
- `SettingsMenuView.kt`, `MainActivity.kt`: sichtbare Umbenennung und Einstieg in die Theme-Auswahl.
- `BicycleDialog.kt`: berücksichtigt die eigene zentrale Gestaltung des Theme-Rasters, ohne seine Vorschaufarben oder Typografie zu überschreiben.
- `ThemePickerTest.kt`: zusätzliche Validierungs-, Integrations- und Layouttests.
- `ThemeManagerTest.kt`, `SettingsMenuTest.kt`: bestehende Prüfungen auf erweiterten Katalog bzw. zentrale Standardfarbe angepasst; keine Tests entfernt.

## Umbenennung und Integration

Der bisherige Hauptmenüeintrag und Einstellungsdialog heißen „Design & UI“. Das große Dashboard-Icon, interne Bezeichner und gespeicherte Dashboard-Schlüssel bleiben erhalten. Die acht Theme-Kacheln stehen direkt über den Dashboard-Presets. Der separate Farbthema-Button und Unterdialog wurden auf Wunsch entfernt. Das gemeinsame ScrollView scrollt den gesamten Inhalt bei Platzmangel. Dashboard-Presets und Karten-Datenfelder bleiben bedienbar; eine Farbauswahl verändert ihre Entwürfe nicht.

## Presets

| Theme | Primary | Secondary | Tertiary | Foreground | Niedrigster Textkontrast |
| --- | --- | --- | --- | --- | --- |
| BICYCLE RED | #BF281B | #89190E | #680E07 | #FFFFFF | 5,94:1 |
| EMBERS | #BF281B | #000000 | #292929 | #FFFFFF | 5,94:1 |
| FIRE | #FFB400 | #000000 | #292929 | #FFFFFF | 1,78:1 (ausdrücklich akzeptiert) |
| ELECTRIC BLUE | #1168B8 | #10508C | #09294D | #FFFFFF | 5,68:1 |
| ACID LIME | #587812 | #39510E | #203007 | #FFFFFF | 5,11:1 |
| ULTRAVIOLET | #8242C3 | #57238E | #2D114D | #FFFFFF | 5,97:1 |
| PETROL ATTACK | #117782 | #11525C | #092C33 | #FFFFFF | 5,27:1 |

FIRE ersetzt SOLAR FLARE unter derselben gespeicherten ID `preset:solar-flare`. ACCENT = #FFB400. Der Nutzer hat am 15.10.2026 die exakte optionale Palette einschließlich weißer Beschriftung auf Gelb ausdrücklich bestätigt. Die Ausnahme liegt als Daten-Metadatum `approvedLowContrastSurfaces = { PRIMARY }` am Preset, ohne namensabhängige UI-/Headerlogik. Kontrastprüfungen für SECONDARY/TERTIARY, ACCENT, Deckkraft, Flächenunterschiede und Custom-Themes bleiben erhalten; selbst erstellte Themes übernehmen diese Ausnahme nicht.

EMBERS ersetzt BLACK OPS unter derselben gespeicherten ID `preset:black-ops`. PRIMARY und ACCENT verwenden #BF281B; SECONDARY ist #000000 und TERTIARY #292929. FIRE verwendet dieselbe Flächenstruktur mit #FFB400 als PRIMARY und ACCENT. Die bestätigte Kontrastausnahme liegt daher bei Fire nun auf dem gelben Header.

## Speicherung und laufender Farbwechsel

Die bestehende `ThemeManager.activate()`-Funktion validiert, aktiviert und speichert die ID in den vorhandenen `bike`-SharedPreferences (`uiTheme.activeId`). Die Auswahl wirkt sofort, der Dialog bleibt geöffnet. „Schließen“ setzt sie nicht zurück. Unbekannte IDs fallen weiterhin auf BICYCLE RED zurück. Bestehende Custom-Theme-Speicherung und API bleiben erhalten; es gibt noch keinen Farbwähler.

Alle bereits angebundenen Dialoge, auch verdeckte Eltern, erhalten weiterhin Aktualisierungen über die vorhandenen Theme-Abonnements. Die Kachelflächen und Schriftfarben folgen dem aktiven Theme; nur die drei gleich hohen, deckenden Vorschau-Streifen zeigen die jeweilige Kandidaten-Palette. Die aktive Kachel erhält eine Foreground-Umrandung. Die reservierte achte Kachel hat eine gestrichelte Umrandung und ein Plus; sie ist deaktiviert.

## Responsives Raster und Design

Normalerweise vier gleich breite Spalten und zwei Zeilen. Die Spaltenzahl berechnet sich aus der verfügbaren Breite, zentralen Mindestbreite und Android-Schriftgrößenskalierung. Bei Platzmangel entstehen weniger Spalten, mit vertikalem Scrollen. Es gibt keine festen Bildschirmkoordinaten und keine Layoutvarianten je Farbthema. Bei großer Schrift wachsen die Kacheln; Namen dürfen zweizeilig werden.

Der vorhandene „Design & UI“-Header, zentrale Rundungen und Footer werden verwendet. Gegenüber dem Mockup dürfen lange Namen umbrechen und auf kleinen Displays weniger Spalten erscheinen. Kein zusätzlicher Theme-Header und keine verschachtelte Logofläche. Die drei Farbrollen ersetzen keine Dashboard-, Karten- oder Messwertdarstellung außerhalb der angebundenen Einstellungsoberflächen.

## Prüfung

Die neuen Tests prüfen alle sieben validen, eindeutigen Presets, Speicherung nach Neuerstellung des Managers, Fallback sowie Erhalt anderer Einstellungen. Ein echter MainActivity-Dialogablauf prüft sofortige Farben und Auswahl, unveränderte Geometrie, Kachelhintergründe, Originalfarben der Streifen, Aktualisierung verdeckter Eltern, deaktivierte reservierte Kachel und Erhalt des unveränderten Dashboard-Entwurfs. Ein kurzer Viewport prüft zusätzlich das gemeinsame Scrollen bis zu den Dashboard-Einstellungen. Weitere Layoutprüfungen verwenden kleine Breiten und doppelte Schriftgröße und prüfen vollständige Namen sowie Touch-Flächen.

Testvorschau: `app/build/reports/settings/theme-picker.png` (native Robolectric-Zeichnung, kein Hardware-Screenshot).

Vollständiger Testlauf und Release-Build: **148 Tests, keine Fehler oder übersprungenen Tests; BUILD SUCCESSFUL in 48 Sekunden.** Bestehende Kotlin-Warnung: `onBackPressed()` ist veraltet; keine neue Kotlin-Warnung durch die Theme-Auswahl. Für die angeforderte installierbare APK wird die Version anschließend auf 0.6.13 (Code 29) erhöht und der Release-Build erneut ausgeführt. Sensor-, GPS-, FIT/GPX-, WebDAV- und Kartenfunktionen wurden nicht verändert. Eine Prüfung auf dem Smartphone steht noch aus.

## APK-Bereitstellung

Version 0.6.13 (Code 29), Android 13+, Release mit Ressourcen-/Code-Shrinking. Erneuter Release-Build nach Versionsanhebung erfolgreich in 23 Sekunden. Installierbare Test-APK: 889.181 Bytes, mit demselben Testzertifikat wie zuvor signiert und erfolgreich geprüft. APK und SHA-256-Prüfsumme auf GitHub veröffentlicht; Download bytegenau geprüft.

## Dashboard-Buttons

Auch die untere Dashboard-Leiste verwendet jetzt das aktive Theme: START/STOPP nutzt Primary, Einstellungen Secondary, Text und Hamburger Foreground, die feine Umrandung Tertiary. Damit bleibt Einstellungen farblich von START abgesetzt. Die bestehenden Theme-Abonnements aktualisieren die Buttons sofort und bei Wiederherstellung der Activity. Button-Geometrie, Aufzeichnungsaktionen und automatisches Ausblenden bleiben unverändert. Der Integrationstest prüft beide Buttons für alle sieben Paletten einschließlich stabiler Geometrie.

## Einheitliche Footer-Icons

Abbrechen verwendet ausschließlich `BicycleIcon.Cancel` (Lucide x), Schließen `BicycleIcon.Close` (Lucide log-out / Tür mit Pfeil, ausdrücklich ausgewählt). Die zentrale Dialoggestaltung setzt die Icons anhand der Aktion. Der bereits eigenständig gestaltete Streckenmanager bindet seinen Schließen-Button ebenfalls über die gemeinsame Icon-Funktion an. Die Geometrie des lokalen 24dp-Vektors stammt aus Lucide 0.468.0; keine neue Bibliothek. Die Wiederverwendung ist in AGENTS.md dauerhaft festgehalten.
