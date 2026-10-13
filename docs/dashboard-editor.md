# Dashboard-Editor V2

Der Editor ist ein Zustand des vorhandenen Dashboards, kein zusätzlicher Screen.
`MainActivity.dashboardEditing` verwaltet NORMAL/EDIT zentral. DashboardView und
MapPanel erhalten daraus ihren Darstellungs- und Touchzustand. Die Oberfläche ist ausschließlich für Hochformat vorgesehen. Die Activity deklariert
`screenOrientation="portrait"`; für Target-SDK 36 setzt sie zusätzlich die Android-16-
Kompatibilitätseigenschaft für eingeschränkte Ausrichtung auf großen Displays.
Querformat-Prüfungen wurden durch unterschiedliche Hochformat-Größen ersetzt.
Es gibt keine neue Konfigurationsablage und keine neue Sensor- oder Graphverarbeitung.

## Bedienung

- NORMAL: kurzes Tippen öffnet keine Feldauswahl. Seitenwechsel und Karte bleiben bedienbar.
- Langer Druck auf eine obere Kachel aktiviert EDIT, ohne gleich einen Dialog zu öffnen.
- Alternativ: **Design & UI → Dashboard bearbeiten** mit Lucide `pencil`. Die vorhandenen
  Einstellungsfenster werden geschlossen und dieselbe Dashboardinstanz wird verwendet.
- EDIT: auf eine obere Kachel oder eines der drei Kartenfelder tippen, um die vorhandene
  Feldauswahl zu öffnen. Unterstützte große Kacheln behalten ihre Auswahl für bis zu zwei
  Graph-Overlays. Kartenfelder bleiben reine Messwertfelder, einschließlich „Leer“.
- **EDITOR BEENDEN** liegt mittig über der Karte. Er schließt die Bearbeitung ab.
- Die Dashboard-Bearbeitung hat keine Geschwindigkeits- oder Stillstandsprüfung mehr.
  Aufnahme und Live-Messwerte laufen auch im Editor weiter.

## Touch, Gestaltung und Navigation

Obere Kacheln verwenden Androids `setOnLongClickListener`; es gibt keinen eigenen
Long-Press-Timer. Der vorhandene GestureDetector meldet Scrollbewegungen und ruft
`cancelLongPress()` auf. Horizontales Wischen wechselt weiterhin die Dashboardseite.
Ein erkannter Long-Press öffnet nicht zusätzlich den Felddialog. Kartenfelder haben
keinen Long-Press-Einstieg und sind ausschließlich während EDIT anklickbar.

`DashboardEditOutline` ist der gemeinsame Foreground für obere Kacheln und Kartenfelder:
PRIMARY, 1,5 dp Strichstärke, gestrichelter Verlauf, 4 dp Innenversatz. Er ersetzt keinen
Hintergrund und beansprucht keinen Layoutplatz. Kachelgrößen, Raster, Messwerttypografie,
Glow und Graphen bleiben unverändert. Themewechsel aktualisieren die Rahmenfarbe.

Der Beenden-Button verwendet die vorhandene `BicycleComponents.action`-Gestaltung.
Karten-Pan/Zoom und Kartenbuttons sind während EDIT gesperrt; die drei Kartenfelder
und der Beenden-Button bleiben bedienbar. Das automatische Ausblenden wird während
EDIT ausgesetzt. Beim Beenden werden die normale Kartenbedienung und die bisherigen
Ausblendregeln wiederhergestellt. Ein Long-Press im normalen Dashboard blendet die
Bedienleiste nicht zusätzlich ein und verändert dadurch nicht die Kartenhöhe.

EDIT bleibt über Dashboardseitenwechsel und Felddialoge hinweg aktiv. Beim Wechsel
in andere Bereiche, etwa Einstellungen oder Streckenmanager, wird es beendet. Eine
vorübergehende App-Unterbrechung erhält EDIT in derselben Activity; eine neue Activity
startet in NORMAL. Bereits bestätigte Einstellungen bleiben erhalten.

## Speicherung

Die bisherigen Schlüssel bleiben erhalten:

- `dashboard` / `dashboard_<Seite>` für die Feldbelegungen;
- bestehende `GraphOverlayConfig.key(Seite)` für die Graph-Auswahl;
- `mapDataField_0` bis `mapDataField_2` für die globalen Kartenfelder.

Der bestehende Auswahldialog speichert bestätigte Änderungen sofort mit
SharedPreferences `apply()`. Sie erscheinen direkt und gehen bei Seitenwechseln nicht
verloren. Abbrechen übernimmt keinen neuen Wert bzw. Graphentwurf. Beim expliziten
Beenden wartet ein abschließendes `commit()` auf diese Schreibvorgänge. Meldet dieses
Speichern einen Fehler, bleiben Editor und Markierungen aktiv und ein lokalisierter
Fehlerhinweis erscheint. Es wird keine falsche Erfolgsmeldung angezeigt.

Das bisherige separate Kartenfelder-Menü wurde durch den gemeinsamen Editor-Einstieg
ersetzt. Gespeicherte Belegungen benötigen keine Migration. Unbestätigte Entwürfe in
„Design & UI“ werden beim Wechsel zum Dashboard nicht automatisch übernommen; dafür
bleibt dort der vorhandene Speichern-Button zuständig.

## Dateien

- `MainActivity.kt`: zentraler Zustand, Einstieg, bestehende Auswahl, Abschluss und Navigation.
- `DashboardView.kt`: native Long-Press-Erkennung, Scroll-Abbruch, editierbare obere Kacheln.
- `MapPanel.kt`: zentraler Beenden-Button, Kartengesten und globaler Kartenfelder-Editor.
- `MapDataFieldsView.kt`: drei einzeln anklickbare Felder; leere Felder sichtbar während EDIT.
- `DashboardEditOutline.kt`: einheitlicher gestrichelter Foreground ohne Layoutänderung.
- `BicycleIcons.kt`: semantisches EditDashboard-Icon aus dem vorhandenen pencil-Asset.
- `DialogNavigation.kt`: vollständiges Schließen registrierter Einstellungsfenster, auch vor
  einem noch ausstehenden OnShow-Callback.
- `RideRuntime.kt`: ausschließlich die unbenutzte alte Dashboard-Stillstandsbedingung entfernt.
- `res/values/strings.xml`: zentrale neue Texte und Accessibility-Beschreibung.

## Prüfungen

`DashboardEditorTest` ersetzt die überholten Stillstandssperren-Tests. Er prüft native
Long-Press-Gesten, Scroll-/Swipe-Abbruch, Aktivierung bei positiver Geschwindigkeit,
normalen Tap, obere und Karten-Feldauswahl, alle Preset-Geometrien, PRIMARY und gestrichelte
Innenrahmen, Position des Beenden-Buttons, Seitenwechsel, Graph-Persistenz, Abbrechen,
App-Unterbrechung, Verlassen des Dashboards, Kartensperre und Wiederherstellung,
Live-Werte und Speicherfehler.

Die bestehenden `DashboardViewTest`, `MapDataFieldsTest` und
`SettingsDesignMigrationTest` wurden auf den neuen Einstieg angepasst. Sie prüfen
weiterhin Feldzuordnung, dauerhafte Belegungen einschließlich Neustart, Theme-Dialoge,
Typografie, Presets, Attribution und Sichtbarkeit. Bestehende Sensor-, FIT-, Performance-,
Graph- und Glow-Tests bleiben Bestandteil der vollständigen Suite.

Ein bestehender Test für den Aufnahmetakt wartet jetzt auf die initiale Bibliotheksladung
und unterscheidet sekundengenaue Taktnachrichten von zusätzlichen asynchronen Graph-
Benachrichtigungen. Sonst konnte eine reguläre Zwischenbenachrichtigung den Test sporadisch
scheitern lassen. Die produktive Aufnahmezeitberechnung wurde nicht verändert.

Ein Praxistest auf dem Telefon, insbesondere während einer realen Fahrt, steht noch aus.

## Abschlussprüfung – 13.10.2026

Vollständige Suite und Release-Build: **354 Tests, 0 Fehler**, einschließlich API 33/35/36,
Release-Lint und R8. Abschließender Lauf: **1 Minute 57 Sekunden**. Build-Aufruf:

```sh
./gradlew :app:testDebugUnitTest :app:assembleRelease --console=plain
```

Die ehemaligen Stillstandssperren-Tests wurden durch neun Editor-Tests ersetzt,
zusätzlich kamen drei reine Interpolations-Tests und ein Karten-Lebenszyklustest hinzu.
Vorhandene Bedienungs- und Layouttests nutzen den neuen Editor-Einstieg beziehungsweise
unterschiedliche Hochformat-Größen. Sensorverarbeitung, FIT-Rohdaten und Performance-
Berechnungen wurden nicht geändert. Release-Artefakt liegt im lokalen Build-Verzeichnis;
die veröffentlichte Test-APK 0.7.10 enthält diesen Entwicklungsstand noch nicht.
