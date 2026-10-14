# UI-Konventionen – Bestandsaufnahme

Stand: 14.10.2026. Native Hauptbranch-Oberfläche. Keine Compose-Migration.
Die Bestandsaufnahme verändert außer den angeforderten Formularabständen keine
weiteren Darstellungsregeln.

## Bereits zentral geregelt

- Fünf Theme-Farbrollen, Validierung, aktive Palette und semantischer Iconkatalog.
- Fullscreen-Dialograhmen, 64-dp-Header, Chevron-/Zurück-Navigation und unabhängig
  zentrierte Fenstertitel; leere Footer benötigen keinen Platz.
- Sofortiges Speichern gültiger Änderungen und Bestätigungen destruktiver Aktionen.
- Abschnittstypografie 23 sp / 900 Black, Standardtext- und Eingabegrößen.
- Einheitengrau, einige Kennzahlen-/Icongrößen, Radien und Touch-Mindestmaß.
- Kreisauswahl für Checkboxen; Schalter und Zustands-Icons.
- Hochformat-Konvention und getrennte Daten-/Graph-/Visualisierungs-Auswahl.

## Jetzt vereinheitlicht: Formularabstände

BicycleDesign.controlGap = 8 dp ist unabhängig vom Innenpadding. Zentraler Helper
BicycleComponents.controlSpacing wird für Formularbuttons (auch Switch, Radio und
CheckBox) sowie Spinner verwendet. Eingabefelder verwenden denselben Abstandswert
in ihrem bestehenden Stil. Buttons aus der gemeinsamen Action-Fabrik erhalten die
Regel ebenfalls.

Der Helper setzt mindestens 8 dp oberen Außenabstand und bei nebeneinanderliegenden
Formularcontrols mindestens 8 dp zwischen ihnen. Bestehende größere Abstände werden
erhalten; vorhandener Endabstand der linken Seite wird berücksichtigt. Breite, Höhe,
Gewicht und übrige Layoutparameter bleiben erhalten. Die Regel wird nach dem
Anfügen erneut angewendet, weil addView zuvor gesetzte Layoutparameter ersetzen kann.

Header-/Footer-Iconbuttons und die fest positionierten Dashboard-/Kartencontrols
verwenden ihre vorhandene Geometrie, sie sind keine Formularbuttons. Navigation und
Kartensteuerung werden durch diesen Auftrag nicht umgebaut. Spezialisierte
Eingabe-/Einheitenzeilen und Gruppenabstände bleiben Teil der folgenden Prüfung.

## Noch lokal oder nur teilweise zentral

| Bereich | Konkrete Fundstellen / heutige Regel |
| --- | --- |
| Überschriften-/Gruppenabstände | RiderProfileView.section verwendet 4/8 dp Padding, FieldPickerView 12/4 dp. Sensor-/FIT-Abschnitte in MainActivity haben eigene Kombinationen. Es fehlt ein gemeinsamer Aufbau aus Überschrift + nachfolgender Feldgruppe. |
| Kleine Zusatztexte und Kennzahlentypografie | AnalysisMetricTile enthält 13-sp-Einheiten und 11-sp-Labels samt lokalen Autosize-Grenzen. MapPanel/MapCreditView haben 11-sp-Texte. Hauptgrößen/Grauton sind zentral, die Unterrollen noch nicht vollständig. |
| Weitere Kennzahlen-/Chevron-Geometrie | Dashboard-Kacheln nutzen jetzt drei zentrale DashboardTileStyle-Varianten. AnalysisValueRow ist gemeinsam; das Dashboard behält seinen eigenen ValueRow und lokale Chevron-Größen. |
| Karten-/Toolbar-Geometrie | MapPanel enthält 44/48-dp-Controls, 2/6-dp-Abstände, 5-dp-Radius und lokale dunkle Farben. MainActivity setzt Toolbarhöhe 58 dp und Iconinsets. Funktionale Sonderlayouts sind sinnvoll; benannte Spezial-Tokens fehlen teilweise. |
| Dropdown-/Popup-Darstellung | BicycleDialogStyle.spinner ist gemeinsam. RouteManagerView implementiert Filterhintergrund und Adapter zusätzlich selbst, trotz weitgehend gleicher Theme-Rollen/Schriften. Popup-Menüs haben weitere eigene Aufbaupfade. |
| Anpassungsgrenzen außerhalb von Dialogen | Kartencontrols und Dashboard haben eigene Anpassungsformeln. Dialog-Scrolling und die Höhenbegrenzung der Feldauswahl sind jetzt zentral geregelt; Karten-Spezialmaße bleiben getrennt. |
| Sichtbare Texte und Accessibility | Neue Bereiche verwenden Strings-Ressourcen, ältere Labels und Beschreibungen in MainActivity/DashboardPresetPicker/FieldPickerView sind teilweise noch direkt im Code. Keine vollständige zentrale lokalisierbare Textbasis. |
| Rückmeldungen und Ladezustände | MainActivity verwendet verschiedene direkte Toast-Aufrufe; RouteExportView und RouteAnalysisView eigene Status-/Fehlerzeilen. Die Funktionen existieren, aber es gibt keine gemeinsame Darstellungskomponente für diese Zustände. |

„Nicht zentral“ heißt nicht automatisch „falsch“. Karten und Diagramme benötigen
passende Spezialmaße; diese können als eigene zentrale Rollen dokumentiert werden,
ohne sie auf Formularmaße zu zwingen. Eine weitere Vereinheitlichung sollte zuerst
Überschrift-/Feldgruppen und die kleinen Schriftrollen betreffen, anschließend
Dropdown-Aufbau und benannte Spezialmaße. Keine Einstellungen oder Funktionen
werden aus dieser Bestandsaufnahme eigenmächtig entfernt.

## Verifikation der Abstandsänderung

Gezielte Prüfungen: BicycleComponentsTest (einschließlich spätem addView,
horizontalem Abstand und erneutem Anfügen), SettingsDesignMigrationTest,
FieldPickerTest, DashboardPresentationTest, UiConventionsTest und RiderProfileViewTest.
Sie prüfen Stil-/Themewechsel, Dropdowns, Hochformat, neue Accordions, Profile und
bestehende Navigation. 23 gezielte Tests bestanden, zusätzlich zwei Streckenmanager-Tests nach Anbindung
des eigenen Filters; Kotlin-Kompilierung erfolgreich. Keine neue APK erstellt.

## Scrollverhalten und Aktionsfooter zentralisiert

`BicycleComponents.scrollStyle` verwendet die vorhandenen Scrolltokens: kein
Aufdehnen kurzer Inhalte (`fillViewport=false`), kein Abschneiden am Innenpadding
(`clipToPadding=false`) und 2-dp-Scrollbar. Fullscreen-Host, generische Dialogstile,
Profil, Feldauswahl, Detailansicht und Streckenliste verwenden dieselbe Geometrie.
Native Gesten, Adapter und Scrollpositionen werden nicht neu initialisiert.
Vorhandene Listen werden weiterhin nicht zusätzlich in eine ScrollView gesetzt.
Header/Footer bleiben fest; Insets und Tastaturanpassung bleiben beim Fullscreen-Host.

Die bisherige Höhenregel der inneren Feldauswahl wurde unverändert als benannte
Designrolle übernommen: 55 % der verfügbaren Höhe, mindestens 144 und höchstens
360 dp, initial 320 dp. Quellenwahl bleibt außerhalb der inneren Liste.

Aktionsfooter verwenden `bicycleFooter` und `BicycleFooterAction`: 64 dp Höhe,
24-dp-Icon und 11-sp-Systemfont darunter, zentriert und bei knapper Breite leicht
verkleinerbar. Die gesamte Kachel ist eine einzige zugängliche Touchfläche.
Import links im Streckenmanager und die fünf Detailaktionen teilen diesen Aufbau.
Neue Texte liegen in Android-Stringressourcen. Themewechsel verändern keine Geometrie;
Cloud-/Speicher-Erfolg und Busy-Sperren bleiben erhalten.

Gezielte Verifikation dieser Änderung: 24 Tests bestanden (Feldauswahl,
Fullscreen-Dialoge auf API 33/35/36, Tastatur/Insets, Themewechsel, Import-Dropdown
und Detailaktionen inklusive persistenter Erfolgsicons). Debug-Kotlin-Kompilierung
erfolgreich; keine vollständige Testsuite und keine APK gebaut.

## Drei Dashboard-Kachelstile (Darstellung erhalten)

`DashboardTileStyle` definiert ONE, TWO und THREE. Alle Presets verwenden diese
Varianten über `DashboardTemplate.tileStyle`. Bestehende Werte wurden übernommen:

| Eigenschaft | ONE (groß) | TWO | THREE |
| --- | --- | --- | --- |
| Zahl | Auto 10–240 sp | Auto 10–240 sp | Fest 45 sp |
| Einheit | Auto 5–16 sp | Auto 5–16 sp | Fest 10 sp |
| Innenpadding horizontal | 10 dp | 10 dp | 3 dp |
| Innenpadding oben/unten | 8/4 dp | 8/4 dp | 8/4 dp |
| Zahl/Einheit-Abstand | 6 dp | 6 dp | 4 dp |
| Beschriftungsgewicht | 0,10 | 0,20 | 0,20 |
| Wertgewicht | 0,65 | 0,65 | 0,65 |
| Höhe relativ zur kleinen Zeile | 2 × 0,75/0,85 | 1 | 1 |

Beschreibung weiterhin 13,5 sp; Schriftfamilien und Messwert-/Einheiten-Ausrichtung
bleiben erhalten. Vorhandene Höhenkalibrierung und Spezialfall kurzer Viewports
bleiben unverändert. Diese Zentralisierung gleicht nicht die Schriftgrößen
zwischen den drei verschiedenen Varianten an.

Sekundäre Schrift verwendet jetzt Weiß mit 65 % Deckkraft (`#A6FFFFFF`) statt
deckendem Grau: Einheiten, Dashboard-Beschriftungen, Graph-Skalentexte und fehlende
Dashboard-Werte. Das gemeinsame Token bindet auch Profil, Streckenliste und
Streckendetails an. Strukturelle Graph-Grenzlinien und Zonenfarben bleiben erhalten.

Verifikation: vier gezielte Prüfungen für transparente Einheiten, Profil und
Streckenliste sowie vier Dashboard-Prüfungen für gemeinsame Höhen/Schriftregeln,
Dreierfeld-Breiten, lange Werte und mittige Einheiten bestanden. Keine vollständige
Testsuite und keine APK erstellt.
