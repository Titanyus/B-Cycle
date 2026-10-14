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

Gemeinsame Geometrie: ein 64-dp-Header mit Zurück-Chevron und einem zur gesamten
Fensterbreite zentrierten Titel, flexibler Content und nur bei fachlichen Aktionen ein
fixer Footer. `BicycleNavigation` vereinheitlicht Vollbild- und kompakte Dialogheader.
Speichern/Abbrechen/Schließen entfallen als Footer-Navigation. Ein leerer Footer wird
vollständig entfernt und reserviert keinen Platz. Gültige Einstellungsänderungen
werden unmittelbar über die bestehenden Speicherkomponenten übernommen.
DialogNavigation behält Elternfenster und Scrollpositionen unverändert.
Details: [UI-Navigation und Gestaltung](ui-navigation.md).

SettingsFullscreenTheme ist die Fensterkonfiguration desselben Builders, keine
zweite Dialogarchitektur. Fensterbreite/-höhe MATCH_PARENT, kein Dim-Rand, keine
äußeren Margins oder äußeren Radien. Header bleibt PRIMARY, Körper/Footer SECONDARY;
Texte/Icons, ACCENT und Eingaberadien behalten ihre bisherigen Zuordnungen.

## Sonderansichten und Scrolling

Hauptmenü und Erweitert verwenden getrennte Screens. Die sieben Hauptmenü-Zeilen
haben Icons und doppelte normale Touchhöhe; Untermenüs behalten die normale Höhe.
Der Streckenmanager-Header wird im gemeinsamen Headerplatz wiederverwendet.
Seine Liste füllt den Restplatz. Streckenmanager und Feldauswahl behalten eigene
ListView-/ScrollView-Bereiche. Bestehende ScrollViews werden wiederverwendet; nur
nicht scrollbare Inhalte wie Umbenennen erhalten einen Scrollcontainer.

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
Touchflächen mindestens 48 dp. Importieren im Manager und die fünf Aktionen in der
Detailansicht bleiben im Footer; Navigation erfolgt ausschließlich über den Header.

## Prüfungen und Grenzen

Layouttests auf API 33, 35 und 36 prüfen mehrere Hochformat-Größen, kurze/lange
Inhalte, fixe Header, funktionale Footer und deren vollständigen Wegfall bei leeren
Dialogen. Systemleisten/Cutout, simulierte IME-Insets, Elternansicht und Zurück,
kompakte Bestätigungen sowie Hauptmenü/Streckenmanager mit eigenen Listen werden
weiterhin geprüft. Theme-, WebDAV-Fokus-, Import-, Auswahl- und Navigationsprüfungen
wurden auf automatische Speicherung umgestellt.

Sensoren, FIT-Rohdaten und Performanceberechnung bleiben unverändert. Die
Insets-/IME-Prüfungen sind Simulationen; Gesten-/Drei-Tasten-Navigation und echte
Tastatur auf dem Telefon bleiben ein Praxistest.

Der Buildnachweis der aktuellen Migration steht in [UI-Navigation](ui-navigation.md).
