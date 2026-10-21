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
| Weitere Spezialtypografie | Einheiten-/Beschriftungsgrößen der Analyse, Sensorstatus, Kartenstatus und Attribution sind jetzt eigene zentrale Rollen. Weitere spezielle Renderinggrößen, etwa Graphachsen, behalten ihre fachlichen Styledefinitionen. |
| Weitere Kennzahlen-/Chevron-Geometrie | Dashboard-Kacheln nutzen jetzt drei zentrale DashboardTileStyle-Varianten. AnalysisValueRow ist gemeinsam; das Dashboard behält seinen eigenen ValueRow und lokale Chevron-Größen. |
| Toolbar-Geometrie | Kartensteuerung und kompakte Anpassungsgrenzen liegen in MapControlDesign. MainActivity setzt weiterhin Toolbarhöhe 58 dp und Iconinsets; diese Dashboard-Spezialmaße bleiben lokal. |
| Dropdown-/Popup-Darstellung | BicycleDialogStyle.spinner ist gemeinsam. RouteManagerView implementiert Filterhintergrund und Adapter zusätzlich selbst, trotz weitgehend gleicher Theme-Rollen/Schriften. Popup-Menüs haben weitere eigene Aufbaupfade. |
| Anpassungsgrenzen außerhalb von Dialogen | Kartencontrols und Dashboard haben eigene Anpassungsformeln. Dialog-Scrolling und die Höhenbegrenzung der Feldauswahl sind jetzt zentral geregelt; Karten-Spezialmaße bleiben getrennt. |
| Fachliche Statusgeneratoren und Metadaten | UI-Beschriftungen/Bedienhinweise der bestehenden Dialoge, Dashboard-/Quellen-/Feldwahl und Kartensteuerung sind jetzt ressourcenbasiert. Bestehende rohe Protokoll-/Diagnosetexte und Statusgeneratoren außerhalb des UI bleiben erhalten; keine vollständige Übersetzung der gesamten App. |
| Rückmeldungslogik | Toasts und Inline-Statusdarstellung sind über BicycleFeedback gemeinsam. Fachliche Auslöser und Statusklassifizierung bleiben in ihren bestehenden Komponenten; keine zweite Statusmaschine. |

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

Die Dashboard-Feldauswahl verwendet einen einzigen äußeren Scrollbereich. Die Datenliste wächst mit WRAP_CONTENT vollständig; Graph und Trainingshilfe folgen darunter. Keine feste Listenhöhe oder verschachtelte ScrollView, damit die Trainingshilfe keine Werte abschneidet.

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

## Kartensteuerung, Texte, Schriftrollen und Rückmeldungen

`MapControlDesign` enthält die bestehenden Dashboard- und Detailkartenrollen.
Die Werte wurden erhalten, einschließlich des größeren Track-Auges und der
Anpassung kleiner Kartenflächen; der Wechsel zu zentralen Werten verschiebt keine
Controls. Der Editor-Beenden-Button ist eine Kartenaktion und kein Formularbutton.

Bestehende UI-Texte, Menütitel, Preset-/Quellen-/Feldnamen und Accessibility-Hinweise
liegen in Android-Ressourcen. `DashboardTextResources` bildet bestehende stabile
IDs auf diese Ressourcen ab. Die alten kontextfreien Fach-APIs bleiben für Daten,
FIT und Tests erhalten; UI verwendet die kontextbezogenen Ressourcen-APIs.
Sensor-/Quellen-IDs, Dateinamen, Gerätenamen, Lizenzinhalte und Protokolltexte werden
nicht übersetzt oder als persistente Schlüssel ersetzt.

Kleine Texte haben eigene Rollen: Analyse-Einheiten 13 sp (Autosize 8–13),
Analyse-Beschriftung 11 sp, Sensorstatus 13 sp, Kartenstatus und Attribution je
11 sp. Das bedeutet gemeinsame Definitionen, keine globale Einheitsgröße.

`BicycleFeedback` bündelt Toasts und Inline-Statuszeilen. Bestehende Dauer,
Wortlaut, Erfolg-/Fehlerentscheidungen und Retry-Funktionen bleiben erhalten.
Inline-Updates nutzen höfliche Accessibility-Live-Regionen und setzen identischen
Text nicht wiederholt. Leerzustände behalten ihre bisherige Sichtbarkeitsregel.

Vollständige Abschlussprüfung für 0.7.15: 386 Tests, 0 Fehler/Fehlschläge,
0 übersprungen. Die Suite deckt auch die vorher zentralisierten Kachelstile,
transparente Einheiten, Footer und Scrollbereiche ab. Die zwei anfangs gemeldeten
Befunde wurden behoben: Formularspacing greift nicht auf den frei positionierten
Karten-Editorbutton über; der Theme-Test prüft den Ressourcen-Menütitel statt
des alten internen Literal-Felds.

## Schalterdarstellung

Die frühere dunkle Spur wird durch eine transparente Pille mit FOREGROUND-Kontur ersetzt. Der runde Knopf ist ausgeschaltet FOREGROUND und eingeschaltet ACCENT; deaktiviert beide mit 50 % Deckkraft. Zentrale Maße: 48 × 24 dp, Knopfdurchmesser 18 dp, Linienmaß linePixels. Native Switch-Bedienung und 48-dp-Touchfläche bleiben erhalten.


## Segmentierte Auswahl und Seitenanfang – 21.10.2026

Alle segmentierten Einfach-/Mehrfachauswahlen verwenden den gemeinsamen umlaufenden Formularrahmen über BicycleComponents.segmentedControlRow: FOREGROUND mit bestehender 50-%-Kontur, 1 dp und 6 dp Rundung. BicycleSegmentButton stellt ihn bei horizontalen Segmentgruppen zusätzlich beim Anhängen sicher. Beispiele: Trainingshilfe, Fahrtnamensbausteine, Graph-Zeitfenster, Dreeve-Zuordnung und Streckenfilter. Auswahlzustände, native Radio-/Checkbox-Semantik und vorhandene Listener bleiben erhalten.

Für Auswahlsegmente mit Icon oberhalb des kleinen Labels BicycleSegmentButton.stackedIcon nutzen. Icon und Text als gemeinsamer zentrierter Block, 2 dp Abstand über BicycleDesign.segmentIconTextGap; keine an den oberen Rand gezogenen Icons mit weit darunter zentriertem Text. Touchflächen bleiben erhalten.

Zwischen Eingabefeldern/Auswahlgruppen und Nachbarcontrols mindestens 8 dp. Innerhalb einer segmentierten Gruppe bleiben die Segmente ohne zusätzliche Formular-Außenabstände unter der gemeinsamen Kontur. Eingabefactory und generische EditText-Gestaltung registrieren controlSpacing, damit nachträglich ersetzte LayoutParams den Abstand nicht verlieren. Inline-Zahlenzeilen behalten ihre gemeinsame Zeilengeometrie.

Normale erste Inhalte beginnen 16 dp unter dem Haupt-Header, führende Sticky-Header bei 0 dp. Verschachtelte Seiten-/Scrollwrapper liefern keine zusätzlichen oberen Insets. Der gemeinsame Vollbildhost normalisiert diese führende Kette beim Messen, auch für nachträglich eingefügte Formulare. Horizontales Padding, innere Controlgeometrie, Footer und Scrollzustand erhalten. Bestehende Abstände innerhalb/zwischen Sticky-Abschnitten bleiben bestehen.

Gezielt geprüft: 11 erfolgreiche UI-Tests (4 neue SelectionSpacing, 6 bestehende ContentSpacing und ein bestehender RideNaming-Profil-/Mehrfachauswahltest). Native Pixelprüfung für den Icon-Text-Abstand und Gruppenrahmen, sieben tatsächliche Einstellungsseiten, nachträglich eingefügte Formulare/Sticky-Header sowie bisherige Profil-/Strecken-/Detailabstände. Kein Gesamttestlauf, keine APK.
