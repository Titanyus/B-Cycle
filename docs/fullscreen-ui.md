# Globale Vollbild-Einstellungsansichten

## Verbindliche Ausrichtung

BICYCLE wird ausschließlich im **Hochformat** verwendet. Das gilt für Dashboard,
Karte, Einstellungsdialoge, Streckenmanager, Details und neue Ansichten. Querformat
ist derzeit kein unterstützter Nutzungsmodus und keine Anforderung an neue Layouts.

Die Single-Activity ist auf `screenOrientation="portrait"` festgelegt. Die Android-16-
Kompatibilitätseigenschaft erhält diese Vorgabe auch bei großen Displays unter Target-SDK 36.
UI-Tests prüfen mehrere Hochformat-Größen, Schrift-/Datenvarianten, Themes und
Scrollverhalten. Sie müssen keine Querformatdarstellung absichern. Rechteckige
Diagramm- und Kacheltests sind weiterhin sinnvoll; eine breite Kachel ist kein
Querformat-Screen.

Diese Konvention steht ebenfalls in `AGENTS.md` und gilt für künftige Erweiterungen.

## Bestandsaufnahme und Migration

Alle appinternen Einstellungs- und Verwaltungsfenster verwenden bereits
BicycleDialogBuilder und DialogNavigation. Umgestellt sind Hauptmenü/Erweitert,
Sensoren & Aufzeichnung mit FIT-Accordion, Sensor hinzufügen/Geräteauswahl,
Design & UI mit Themes/Dashboards, Feldauswahl im [Dashboard-Editor](dashboard-editor.md), Kartenmanager,
Streckenmanager, Streckeneinstellungen/WebDAV, Umbenennen, Wiederherstellung,
Diagnose, Standardbelegung, GPS-Informationen und Lizenzansichten.

Gemeinsame Geometrie: bestehender Header oben, flexibler Content dazwischen,
bestehender nativer Buttonbereich unten. BicycleFullscreen passt ausschließlich
Geometrie/Container an. AlertController-Buttons und Adapter bleiben dieselben
Objekte, samt Labels, Reihenfolge, Click-Handlern, Enabled-Zuständen und Validierung.
DialogNavigation behält Elternfenster, Drafts und Scrollpositionen unverändert.

SettingsFullscreenTheme ist die Fensterkonfiguration desselben Builders, keine
zweite Dialogarchitektur. Fensterbreite/-höhe MATCH_PARENT, kein Dim-Rand, keine
äußeren Margins oder äußeren Radien. Header bleibt PRIMARY, Körper/Footer SECONDARY;
Texte/Icons, ACCENT und Eingaberadien behalten ihre bisherigen Zuordnungen.

## Sonderansichten und Scrolling

Hauptmenü-Logo und Streckenmanager-Header werden aus dem Inhalt in den festen
Headerplatz verschoben. Kein neuer Titel/Logo/Button. Die bisherige Größenbegrenzung
des Streckenmanagers entfällt nur im Vollbildfenster; seine Liste füllt den Restplatz.
Streckenmanager und Feldauswahl behalten eigene ListView-/ScrollView-Bereiche.
Bestehende ScrollViews werden wiederverwendet; nur nicht scrollbare Inhalte wie
Umbenennen erhalten einen Scrollcontainer. Leerer Restplatz bleibt leer.

Kleine Löschbestätigungen verwenden ausdrücklich den kompakten Modus desselben
Builders und schweben über der weiterhin sichtbaren Vollbild-Elternansicht,
damit dahinter keine Karte erscheint. Stack/Zurück/Draft bleiben erhalten. Dropdowns/ListPopupWindow und Androids System-Dateiauswahl bleiben unverändert.
Keine Verlagerung oder Entfernung von Streckenaktionen.

## Systemleisten, IME und Footer

Nur das Einstellungsfenster wird für explizite Insets ausgerichtet; Dashboard-
Systemleisten/Root werden nicht geändert. Der Vollbild-Host verarbeitet Systembars
und DisplayCutout einmal. Unten gilt max(Navigations-Inset, IME-Inset), keine Summe.
Die Padding-Flächen für Systemleisten/Ausschnitte bleiben wie im Dashboard schwarz;
Header, Inhalt und Footer behalten ihre Theme-Flächen. Insets werden für untergeordnete Views konsumiert. Beim Öffnen der Tastatur schrumpft
der flexible Content; Footer liegt oberhalb der Tastatur und verdeckt ihn nicht.
Native adjustResize/Inset-APIs bleiben für Android 13+ kompatibel. Keine neuen Bibliotheken.
Header/Footer liegen außerhalb des Content-Scrolls; native Buttonbar behält ihr
vorhandenes Verhalten einschließlich Stapelung bei sehr schmalen Displays.
Touchflächen mindestens 48 dp. Keine neuen oder entfernten Footer-Aktionen.

## Prüfungen und Grenzen

Neue Layouttests auf API 33 und 35: kleine und große Hochformat-Flächen, kurze/lange Inhalte,
fixe Header/Footer, scrollbarer Rest, native Speicher-/Abbruchaktionen, ursprüngliche
Buttonanordnung, Systemleisten/Cutout, simulierte IME-Insets, Eltern-Draft und Zurück,
kompakte Bestätigung sowie Hauptmenü/Streckenmanager mit eigenen Headern/Listen.
Bestehende Theme-, WebDAV-Fokus-, Import-, Auswahl- und Navigationsprüfungen bleiben aktiv.
Der Theme-Geometrievergleich berücksichtigt sichtbare UI; versteckte native Spacer
besitzen nach Layoutpässen bedeutungslose alte Rechtecke und werden nicht verglichen.

Sensoren, FIT, Performanceberechnung, WebDAV, Speicherung, Dashboard und Kartenlogik
werden in dieser Migration nicht geändert. Die Insets-/IME-Prüfungen sind Simulationen;
Gesten-/Drei-Tasten-Navigation und echte Tastatur auf dem Poco bleiben ein Praxistest.

Abschlussprüfung: `:app:testDebugUnitTest :app:assembleRelease` erfolgreich in
1 Minute 26 Sekunden. 235 Tests, 0 Fehler, 0 Fehlschläge, 0 übersprungen.
Release-Lint-Vital erfolgreich. Die bekannte MainActivity-onBackPressed-Warnung
bleibt bestehen; kein neuer vollständiger Lint-Lauf. Keine veröffentlichte APK
oder GitHub-Änderung im Rahmen dieser Umstellung; Freigabe bleibt ausstehend.
