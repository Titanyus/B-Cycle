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
Horizontale/vertikale UI-Trenner und Dashboard-Raster verwenden
ebenfalls 0,75 dp über BicycleDesign.linePixels. Deckkraft bleibt 55 % für
Abschnittstrenner bzw. 100 % zwischen Strecken. Horizontale Abschnittstrenner
verwenden sectionDividerGap mit 16 dp oben und unten. Diagrammkurven bleiben
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
Zeilen. FIT-Aufzeichnung enthält Geschwindigkeit/Distanz als normale Titel mit Dropdown in einer Zeile; Strecken-Aufzeichnung enthält die Feldgruppen. Native Dropdown-Schriften auf der Profilseite: 14 sp, Theme-Namen dagegen 21 sp / League Gothic gemäß zentraler schmaler Schriftrolle. Sensorzeilen sind so hoch wie ein einzeiliger Sticky Header; Schalter, Text und Chevron werden ohne zusätzliche vertikale Formular-Margins zentriert.

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
bei 16 dp. Wettergruppen werden als Gesamtzeile bzw. pro umgebrochener Zeile zentriert.

Die native ListView der Strecken übernimmt dividerAlpha = 55 % für ihre
FOREGROUND-Trenner, identisch zur Deckkraft der Abschnittstrenner in Details.

## Aufklappbare Profilabschnitte und Formularkontrollen

Auf der Profilseite sind die gleichrangigen Sticky-Abschnitte auf-/zuklappbar. Sie bleiben im geöffneten Zustand sticky; der Chevron und ein gleich breiter unsichtbarer Platz links erhalten die exakte Titelzentrierung. Der Fahrradname ist ein eigener Sticky Header ausschließlich zum Umbenennen. FIT-Aufzeichnung und Strecken-Aufzeichnung stehen in voller Seitenbreite auf der Hauptebene, nicht in einer eingerückten gemeinsamen Gruppe. Einklappen verändert keine gespeicherten Werte.

Formularbuttons und Dropdowns verwenden zentral SECONDARY mit **1 dp FOREGROUND-Kontur bei 50 % Deckkraft** und **6 dp Radius**. Popup-Flächen übernehmen dieselben Rollen. Eingabefelder (auch Radumfang und Fahrerprofil) erhalten dieselbe Kontur und Rundung; ihre bisherige transparente bzw. TERTIARY-Fläche und Typografie bleiben erhalten. Bestehende Ripple-/Disabled-Zustände und fachliche Auswahlmarkierungen bleiben bestehen.

Die zunächst vorgemerkte Änderung wurde am 17.10.2026 nach Build 30 umgesetzt. Die letzte Nutzervorgabe setzt die Rahmen-Deckkraft ausdrücklich auf 50 %, unabhängig von den weiterhin 55 % deckenden Sensor-Trennlinien. Nur die Kontur verwendet diese Deckkraft, nicht Text, Icons oder das gesamte Bedienelement. Andere Linien und fachliche Auswahlrahmen sind nicht betroffen.

Aktuelle sichtbare Referenz: [UI-Atlas](previews/ui-catalog.html), generiert aus BicycleDesign. Schriftrollen: [fonts.md](fonts.md). 24-dp-Gruppen-/4-dp-Labelabstände im Atlas sind ausdrücklich nur ein HTML-Entwurf und ändern keine bestehenden App-Abstände.

Feldbezogene Inline-Rückmeldungen direkt unter Eingabefeldern: fieldFeedbackGap=controlGap/2 (4 statt 8 dp), für Erfolg und Fehler gleichermaßen. BicycleFeedback.status setzt die Margin nach finaler Anfügung unter EditText; andere Statusrollen/Toasts/Validierungsregeln unverändert. HTML-Atlas zeigt dieselbe Regel.

## Beschriftung zum Eingabefeld (22.10.2026)

Bild 2 des UI-Vergleichs ist der gemeinsame Standard: 4 dp zwischen Beschriftung und zugehörigem Control über fieldLabelGap. Gestapelte Beschriftungen mit BicycleComponents.fieldLabel erzeugen beziehungsweise über labelFor explizit zuordnen; normale Fließtexte/Überschriften nicht implizit als Feldlabel behandeln. controlSpacing greift auch nach finaler LayoutParams-Zuweisung und bei erneutem Stylen; horizontale Zahlenzeilen und normale 8-dp-Controlabstände bleiben erhalten. Tagesbefinden-Inputs/Dropdowns/Slider und das beschriftete Kartenprovider-Schlüsselfeld verwenden die Rolle. Der HTML-Atlas zeigt den gleichen Abstand in Eingabebeispielen, Tagesbefinden und Rückmeldungen. Der zusätzliche Entwurfsregler Slider → Skalenbeschriftung wirkt ausschließlich in der HTML-Vergleichsgruppe und unabhängig von Label → Control.

## Finale Sliderwerte für 0.8.24

32 dp zwischen Slider-Fragenblöcken; 0 dp zwischen Label und Slider-Touchfläche; −8 dp von Slider-Touchfläche zur Skalenzeile, bei weiterhin 50 % Deckkraft. Der negative Abstand gehört zur ganzen Skalenzeile, deren Textkinder keine negativen Margins erhalten. So bleibt die Schrift vollständig sichtbar. Texteingaben/Dropdowns behalten 4 dp zwischen Label und Control. HTML-Vergleichsregler starten bei denselben Werten; Skalenregler erlaubt −16 bis 24 dp.
