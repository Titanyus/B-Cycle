# Seiten, Kategorieabstände und Sensoridentität

Stand: 17.10.2026. Native Kotlin-Views, ausschließlich Hochformat.

## Zentrale Abstände

BicycleDesign.pageContentGap und sectionContentGap: jeweils 16 dp.
BicycleDesign.sectionEndGap: 32 dp vom Inhalt zum nächsten Sticky Header.
BicycleDesign.controlGap: unverändert 8 dp zwischen Bedienelementen.
BicycleContentSpacing.page wird im Fullscreen-Host auf den eigentlichen
Seiteninhalt angewendet. Vorhandene äußere Wrapper-Innenabstände werden entfernt;
Control-Margins werden in den Gesamtanfangsabstand einbezogen. Die Datenliste der Dashboard-Feldauswahl wächst vollständig innerhalb des gemeinsamen äußeren Scrollbereichs und ist kein eigener Seitenanfang.

BicycleContentSpacing.sections gewährleistet 16 dp nach einem Header und 32 dp vom Inhalt zum nächsten Header. Auch wenn dieser am Anfang eines verschachtelten Layouts steht, wird der Abstand am Ende der vorherigen Inhaltsgruppe gesetzt. Die Sensorliste bildet dafür eine gemeinsame Gruppe; interne Sensorabstände bleiben kleiner.
Das gilt für Text, Eingaben, Dropdowns, Buttons/Schalter, Accordion-Controls,
Listen und Kachelgruppen. Vorhandene Touchflächen und Innenabstände innerhalb
der Elemente bleiben erhalten. Wiederholte Anwendung addiert keine Abstände.
Der Sticky-Scrollcontainer aktualisiert diese Regeln vor dem Messen auch bei
verändertem Inhalt. Scrollposition und native Scrollmechanik bleiben erhalten.

## Sticky Header

TERTIARY als Headerhintergrund; Linien oben/unten in FOREGROUND mit 0,75 dp.
Kein Schatten, keine Außenmargins und kein oberer Leerstreifen. Die Header
berühren sich beim gegenseitigen Wegschieben. 56 dp Mindesthöhe, 12 dp
Innenpadding und zentrierte 23-sp-/900-Black-Schrift bleiben erhalten. Die
Z-Reihenfolge bleibt über erhöhten nativen Buttons, ohne nativen Schatten.
Horizontale/vertikale UI-Trenner, Dashboard-Raster und feine Feldrahmen verwenden
ebenfalls 0,75 dp über BicycleDesign.linePixels. Deckkraft bleibt 55 % für
Abschnittstrenner bzw. 100 % zwischen Strecken. Horizontale Abschnittstrenner
verwenden sectionDividerGap mit 12 dp oben und unten. Diagrammkurven bleiben
unverändert. Die zentrale Umrechnung unterstützt Bruchteile und rundet auf
ganze Pixel, mindestens einen Pixel für positive Linienbreiten.
Der Schatten des Hauptheaders bleibt 12 dp / maximal 40 %.

## Icon-Text-Buttons

BicycleDialogStyle.actionIcon verwendet BicycleTextIcon. Das Icon steht direkt
vor dem Text mit 8 dp Abstand; die Gruppe ist bei zentrierten Buttons gemeinsam
zentriert. Native Button-Texte und deren Accessibility bleiben unverändert,
keine künstlichen Icon-Zeichen im Text. Links ausgerichtete Accordion-Buttons
behalten ihre Ausrichtung. Icon-only-Controls und vertikale Footer sind eigene
Komponenten. Pixeltests prüfen die Nähe zum Text in LTR und RTL.

SensorIcons ordnet sensorbezogene Symbole zentral zu. GPS: satellite-dish,
E-Bike: bike, Pulsmesser: heart, Powermeter: zap. SourceIconSpan verwendet dieselbe
Zuordnung, einschließlich Dashboard-Quellenauswahl und Datenbeschriftungen;
keine separat gezeichnete Fahrradvariante mehr.

## Bikes und Streckendetails

Fahrradbilder: Android-Dokumentauswahl, Kopie je Profilslot unter bike-photos.
Import höchstens 25 MiB; ImageDecoder berücksichtigt die Bildorientierung und
verkleinert auf höchstens 1200 px längste Seite. Atomare WebP-Speicherung mit
85 % Qualität. Fehlgeschlagene Übernahme ersetzt kein bestehendes Foto.
Vorschau und Import im Hintergrund; kein dauerhafter Zugriff auf die Quelldatei.
Bildfeld und anklickbarer Platzhalter behalten immer dasselbe Format 16:9; Fotos werden mittig passend beschnitten. Abgerundete Ecken
und Löschsymbol. Fotos sind derzeit nicht Bestandteil der manuellen Sicherung.

Profilkacheln: Radtyp oberhalb des Symbols; Name darunter mit zwei reservierten
Zeilen. FIT-Aufzeichnung enthält Geschwindigkeit/Distanz als normale Titel mit Dropdown in einer Zeile; Strecken-Aufzeichnung enthält die Feldgruppen. Native Dropdown-Schriften auf der Profilseite: 14 sp, Theme-Namen dagegen 28 sp / Bebas Neue wie schmale Streckendetailwerte. Sensorzeilen sind so hoch wie ein einzeiliger Sticky Header; Schalter, Text und Chevron werden ohne zusätzliche vertikale Formular-Margins zentriert.

Streckendetails: Datum und Wetter verwenden die normale Systemschrift mit
rowTitleSp (16 sp). Wettergruppen umfassen jeweils das unmittelbar vorangestellte
Icon und die Angabe; bei Platzmangel Umbruch ganzer Gruppen. Die eingebettete
Karte misst Breite = Höhe. Mapterhorn bleibt zentral unter Quellen & Dienste,
mit unveränderter technischer Höhenkorrektur/FIT-Provenienz.

Linien-Testwert für das Poco F7 Ultra: 0,75 dp (bei Dichte 2,5–3 gerundet 2 physische Pixel). Die tatsächliche Android-Anzeigeskalierung entscheidet, nicht die Pixelhöhe allein. Abschnittstrenner übernehmen den Außenabstand; unmittelbar folgende Controls addieren keinen zusätzlichen Top-Margin.

## Verifikation

61 gezielte Tests ohne Fehler oder übersprungene Fälle: Inhaltsabstände,
dichteabhängige Linien, Icon-Text-Gruppen in LTR/RTL, Sticky-Verhalten, Bike-Fotos
inklusive Fehlerfall/Picker/Löschen, Wettergruppen, quadratische Karte und
bestehende Dashboard-/Quellen-/Einstellungsnavigation. Release-Build mit R8,
Resource-Shrinking und lintVital erfolgreich. Die tatsächliche optische Wirkung
auf dem Poco F7 Ultra sowie die Android-Anzeigedichte bleiben auf dem Gerät zu
prüfen; die Tests simulieren mehrere Dichten einschließlich 2,75.

## Footer-Wrapper und Strecken

BicycleFullscreen normalisiert bei einem fachlichen Footer die tatsächliche erste
Seitenansicht (ScrollView/Streckenmanager), nicht zusätzlich deren äußere
LinearLayout-Hülle. So bleibt der Abstand zu Bike-Kacheln, Streckenfilter und
Fahrtdatum identisch pageContentGap (16 dp); Wrapper-Padding wird nicht addiert.
Wettergruppen verwenden mittige Punkte nur zwischen Himmel/Temperatur, Feuchte
und Wind. Innerhalb von Himmel und Temperatur steht kein Punkt.

Streckendetails verwenden für die Trenner ihrer Kennzahlenbereiche zentral
analysisDividerGap = 2 dp oben und unten. Alle anderen Abschnittstrenner bleiben
bei 12 dp. Wettergruppen werden als Gesamtzeile bzw. pro umgebrochener Zeile zentriert.

Die native ListView der Strecken übernimmt dividerAlpha = 55 % für ihre
FOREGROUND-Trenner, identisch zur Deckkraft der Abschnittstrenner in Details.

## Aufklappbare Profilabschnitte und Formularkontrollen

Auf der Profilseite sind die gleichrangigen Sticky-Abschnitte auf-/zuklappbar. Sie bleiben im geöffneten Zustand sticky; der Chevron und ein gleich breiter unsichtbarer Platz links erhalten die exakte Titelzentrierung. Der Fahrradname ist ein eigener Sticky Header ausschließlich zum Umbenennen. FIT-Aufzeichnung und Strecken-Aufzeichnung stehen in voller Seitenbreite auf der Hauptebene, nicht in einer eingerückten gemeinsamen Gruppe. Einklappen verändert keine gespeicherten Werte.

Formularbuttons und Dropdowns verwenden zentral SECONDARY mit 2 dp FOREGROUND-Kontur und 6 dp Radius; keine eigene graue Fläche. Popup-Flächen übernehmen dieselben Rollen. Bestehende Ripple-/Disabled-Zustände und fachliche Auswahlmarkierungen bleiben erhalten. Eingabefelder behalten ihre eigene dezente Rahmenkonvention.

## Vorgemerkt: Button- und Dropdown-Rahmen auf 1 dp

Nutzerwunsch vom 17.10.2026: Die zuletzt auf 2 dp umgestellten Rahmen der Buttons und Dropdowns künftig auf **1 dp** reduzieren. Rahmenfarbe wie die Sensor-Abschnittstrenner: **FOREGROUND mit 55 % Deckkraft** (dividerAlpha), statt volldeckendem Weiß; nur die Kontur erhält diese Deckkraft. SECONDARY-Fläche und 6-dp-Rundung bleiben erhalten; andere Linien und fachliche Auswahlrahmen sind davon nicht betroffen. Ausdrücklich zunächst nur vorgemerkt, in Build 30 noch 2 dp.
