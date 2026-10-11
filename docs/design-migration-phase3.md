# Phase 3 – Einstellungsdialoge

## Umfang und Analyse

Die App verwendet native Kotlin-Views, eine Activity und `AlertDialog`. Phase 3
verwendet weiterhin `ThemeManager`, `ThemeTokens`, `BicycleDesign`,
`BicycleComponents`, `BicycleIcons` und `DialogNavigation`; keine neue UI-Bibliothek
oder zusätzliche Abhängigkeit wurde eingeführt.

Vor der Migration hatten die Dialoge eigene weiße/graue Texte, statische rote
Hintergründe, Standard-Spinner, Android-Buttonflächen und teilweise eine dritte
Schriftvariante. Die Hauptdialoge sind in `MainActivity.kt` aufgebaut; die
Spezialinhalte liegen in `SettingsMenuView`, `FieldPickerView`,
`DashboardPresetPicker` und `RouteExportView`.

Die vorhandenen ScrollViews, die feste Sensorquellen-Auswahl über der scrollbaren
Feldliste sowie der native ListView des Streckenmanagers bleiben bestehen.

## 1. Migrierte Hauptdialoge

| Dialog | Umsetzung |
| --- | --- |
| Haupt-Einstellungen | Bestehende sechs großen Icons und Markenbereich; dynamische Primary-/Secondary-Flächen, neutraler Systemfont, themenabhängige Icons und Trennlinie |
| Sensoren | Einheitlicher Header, Sensorbereiche mit Trennlinien, Lucide für Auswahl/Verbinden/Trennen |
| FIT-Aufzeichnung | Themed Dropdowns für beide Hauptquellen; alle Speicherfelder bleiben auswählbar; themenabhängige Checkboxen |
| Kartenmanager | Einheitliche Quelle, Checkboxen, Aktionen und Bereichstrennung |
| Dashboards | Bestehende vier Seiten und sechs grafischen Vorlagen; Auswahlzustände und Vorschaugrafiken folgen dem Theme |
| Erweitert | Bestehender aufklappbarer Bereich im Hauptmenü, einschließlich bisheriger Navigation |
| Ungespeicherte Fahrten | Bestehender Status und Wiederherstellungsaktion; gemeinsamer Dialograhmen |
| Diagnose | Exportaktion und gemeinsamer Dialograhmen |
| Standardbelegung | Bestehende Reset-Aktion und gemeinsamer Dialograhmen |
| GPS-Messung erklärt | Vorhandener Hilfetext, Systemfont und gemeinsamer Dialograhmen |
| Schrift-/Bibliothekslizenzen | Vollständige Lizenzinhalte, Systemfont und gemeinsamer Dialograhmen |

Die langen einleitenden Erklärtexte in den Formularen wurden auf zusätzlichen
Wunsch entfernt. Feldbezeichnungen, Status- und Fehlertexte, Hilfe und Lizenzen
bleiben erhalten.

## 2. Migrierte Unterdialoge

- Sensor hinzufügen und Auswahl von Fahrrad bzw. Pulsmesser; Scanlogik unverändert.
- Feldauswahl für Dashboard und Karte, einschließlich dynamisch neu erzeugter
  Messwertzeilen nach Wechsel der Sensorquelle.
- Karten-Datenfelder im Dashboard-Menü.
- Streckeneinstellungen mit lokalem Zielordner und WebDAV-Konfiguration.
- Strecke umbenennen, einschließlich bestehender Eingabeprüfung.
- Bestätigung zum Löschen einer Strecke, einschließlich bestehendem Sicherheitstext.
- Vorhandene Import- und Streckenaktions-Dropdowns bleiben auf ihrer bereits
  migrierten Phase-2-Darstellung.

## 3. Gemeinsame Komponenten

`BicycleDialogBuilder` ergänzt den vorhandenen AlertDialog um den zentralen Header.
`BicycleDialogStyle` bindet native Eingaben, Auswahlfelder, Checkboxen, Listen und
Footer an die bestehenden Theme-Komponenten. Beide sind Adapter des bestehenden
Designsystems, ohne eigene Theme-Schnittstelle oder Speicherlogik.

- Primary: Header; Secondary: Dialog und Inhalt; Tertiary: Eingaben und Dropdowns.
- Foreground: Text, Icons, Auswahl-/Fokusrahmen und Trennlinien.
- Header: Bebas Neue, 32 sp; lange Titel können innerhalb desselben 64-dp-Headers
  bis 18 sp verkleinert werden. Alle übrigen Texte: Systemfont.
- Texte: 14 sp; Eingaben und Bereichsüberschriften: 16 sp.
- Standard-Icons: 24 dp; normale Aktionen: mindestens 48 dp Höhe.
- Der Streckenmanager behält seine zuletzt freigegebene kompakte Geometrie mit
  36 × 48 dp Aktionsflächen; die Phase-3-Dialoganpassung überspringt ihn vollständig.
- Trennlinien: 1 physischer Pixel, Foreground mit 55 % Deckkraft.
- Die Geometrie bleibt unabhängig von den Theme-Farbwerten.
- Theme-Abonnements verwenden weiterhin die bestehenden Attach-/Detach-Bindings.

## 4. Lucide-Umstellung

Neue lokale Lucide-0.468.0-Vektoren: Bluetooth, Unplug, Map, Save, Folder Open,
Rotate CCW, Plus, Link und Info. Bestehende Lucide-Icons für Schließen, Import,
Export, Löschen und Dropdown werden wiederverwendet. Semantische Zuordnung in
`BicycleIcons.kt`; Importskript erweitert; vorhandene Lizenz erhalten.

Beispiele: Sensor hinzufügen/auswählen/verbinden/trennen, Diagnoseexport,
Wiederherstellung, Reset, Kartenaktionen, Ordnerauswahl, Verbindungstest und
Dialog-Footer. Die bereits migrierten Streckenmanager-Icons bleiben unverändert.

## 5. Erhaltene individuelle Icons

Unverändert erhalten: `ic_settings_sensors`, `ic_settings_recording`,
`ic_settings_map`, `ic_settings_dashboards`, `ic_settings_routes`,
`ic_settings_advanced` und `ic_settings_brand`. Die Farbe wird zur Darstellung
an Foreground gebunden. Kein zusätzlicher Logo-Bereich wurde eingeführt.

## 6. Ersetzte Styles

- Statische Textfarben und die Condensed-Systemschrift des Hauptmenüs.
- Statischer Header-Hintergrund des Hauptmenüs.
- Android-Standarddarstellung für Dropdowns, Eingaben, Checkboxen und Buttons.
- Eigene feste Auswahlfarben der Dashboard-Vorlagen.

Die XML-Styles bleiben als Framework-Fallback vorhanden. Sichtbare
Einstellungsflächen werden dynamisch vom aktiven Theme gestaltet.
Dashboard, Karte und START-/Hamburger-Leiste außerhalb der Dialoge werden nicht
auf das Einstellungs-Theme umgestellt.

## 7. Sonderfälle und Grenzen

- Das Hauptmenü behält seinen bestehenden Marken-Header und das große Icon-Raster.
- Der Streckenmanager und seine vorhandenen DropDowns sind die Referenz und
  behalten ihre bereits freigegebene Darstellung.
- Android-Berechtigungs-, Datei- und Ordnerdialoge werden vom Betriebssystem
  gezeichnet und behalten das Systemdesign.
- Es existieren keine Slider in den aktuellen Einstellungsdialogen.
- Keine Änderungen an BLE, GPS, Aufzeichnung, FIT, Upload, Speicherformaten oder
  Konfigurationsauswertung. Vorhandene Handler, Drafts und Navigation bleiben bestehen.
- Tests zur kurzen Bildschirmhöhe simulieren den verfügbaren Platz bei geöffneter
  Tastatur; eine reale Xiaomi-Tastatur/IME ist damit nicht getestet.

## 8–10. Prüfungen

Gruppenweise bestehende Tests für Navigation, Feldauswahl, Sensor-/Dashboard-Menüs,
Streckenmanager und Export ausgeführt. Zusätzlich drei UI-Tests ergänzt:

1. Alle Haupt-/Erweitert-Dialoge: dynamische Farbwechsel bei unveränderter Geometrie
   und Rückkehr zum vorherigen Dialog.
2. WebDAV-Eingaben: Draft, Fokus und Scrollverhalten bei kurzer Bildschirmhöhe;
   Abbrechen speichert keine Änderungen.
3. Feldauswahl: aktive Theme-Farben auch bei neuen Sensorquellen-Zeilen,
   Dropdown-Darstellung und Auswahlfunktion bei langem Titel/kleinem Display.

Abschließende gemeinsame Prüfung:

```text
:app:testDebugUnitTest :app:assembleRelease :app:lintDebug
BUILD SUCCESSFUL in 1m 11s
145 Tests, 0 Fehler, 0 Fehlschläge, 0 übersprungen
```

Alle 142 bisherigen Tests bleiben erhalten; hinzugekommen sind die drei oben
beschriebenen UI-Tests. Release-Build inklusive R8 und Lint-Vital erfolgreich.
Lint-Debug: 0 Fehler, 36 Warnungen. Davon eine zusätzliche Warnung zur jetzt
ungenutzten `settings_header_background.xml`; übrige Kategorien sind die
bestehenden StaticFieldLeak-, ViewConstructor-, ClickableViewAccessibility-,
SetTextI18n- und UnusedResources-Hinweise. Die bestehende Kotlin-Warnung zu
`onBackPressed` bleibt außerhalb dieses visuellen Auftrags unverändert.

Native Android-Screenshots: `app/build/reports/settings/phase3-*.png` für Sensoren,
Aufzeichnung, Kartenmanager, Dashboards, WebDAV und kleine Feldauswahl.

## 11. Altlasten für spätere Cleanup-Phase

Nicht entfernt:

- `RouteActionIcon.kt` und `RouteSymbol.kt`: ältere individuell gezeichnete
  Aktionsicons, keine aktuellen Aufrufstellen gefunden.
- `settings_header_background.xml`: bisheriger statischer Marken-Header.
- `settings_background.xml` und feste Farbwerte in `SettingsDialogTheme`:
  Framework-Fallbacks; erst nach gesonderter Prüfung aufräumen.
- Semantische Datei-/Höhen-/Zeit-Icons und ihre Vektoren, die der kompakte
  Streckenmanager nicht mehr anzeigt; vor Löschung übrige Nutzungen prüfen.

Phase 3 endet mit der visuellen Prüfung; keine Cleanup-Phase oder neue
Farbwahl-Oberfläche wird begonnen.

## Nachkorrektur der visuellen Prüfung

Dunkle Aktionsbuttons erhalten nun wie die WebDAV-Eingaben 8 dp oberen Abstand.
Footer und Streckenmanager bleiben davon ausgenommen. Der UI-Test vergleicht
explizit die Button- und Eingabeabstände.

Die kleine Feldauswahl-Vorschau war ein Belastungstest mit 720 Pixel Breite und
einem absichtlich langen Titel. Die übrigen Vorschauen waren 1080 Pixel breit.
Beim Skalieren auf dieselbe sichtbare Breite wirkten die Texte deshalb größer,
während die Titel-Autoskalierung den langen Testtitel verkleinerte.

Zusätzlich wird jetzt die tatsächliche Feldauswahl über ein Dashboard geöffnet
und bei denselben 1080 × 1800 Pixeln wie die anderen Dialoge dargestellt.
Der Test prüft 32 sp für den normalen Header und 14 sp für die Messwertauswahl.
Vorschau: `phase3-field-picker.png`; der kleine Belastungstest bleibt erhalten.

Nachkorrektur geprüft: alle 145 Tests bestanden; Release-Build erfolgreich (gemeinsamer Lauf: 1m 6s).
