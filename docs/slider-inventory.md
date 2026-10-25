# Slider-Bestand und Abstände

Stand: 25.10.2026, Quellstand nach APK 0.8.41. Bestandsaufnahme ohne Änderung der Android-App. Alle Maße dp, Schriften sp. Außenabstände beziehen sich auf View-/Containergrenzen, nicht auf die sichtbare Linie. Bei 48 dp Höhe liegt deren Mitte normalerweise etwa 24 dp unter der View-Oberkante. Textzeilenhöhe und Android-Schrift-/Anzeigegröße kommen hinzu; die Gesamtblockhöhe ist deshalb nicht pauschal fest.

[Native Vorschau der Trink-Regler](https://github.com/Titanyus/B-Cycle/blob/01a01bf66045dc4c260bbbc1222c6a4004b5601c/docs/previews/setting-slider-pills.png)

## Neuer gemeinsamer Einstellungsregler

Trink-Erinnerung (Intervall und Dauer), Trackdarstellung (Deckkraft und Breite) und Benachrichtigungsdauer verwenden jetzt BicycleSettingSlider und settingSliderRow. Titel links, Schieberegler rechts, kein Zahlenwert mehr darüber. Der native Griff ist eine solide PRIMARY-Pille mit FOREGROUND-Wert einschließlich bisheriger Einheit. Schrift 10,5 sp = bisherige bodySp 14 × 0,75. Pillenhöhe mindestens 24 dp, Breite mindestens 56 dp, bei Bedarf für Werte/Schriftvergrößerung breiter. Maximalbreite des gesamten Wertebereichs einmal vorab messen, damit sich die Griffbreite beim Ziehen nicht ändert. Gemeinsame 3-dp-Spur und native SeekBar-Gesten/Tastatur/Accessibility bleiben erhalten.

Bedienfläche 48 dp, Zeilenhöhe mindestens 64 dp mit 8 dp vertikalem Innenabstand. Dadurch liegen die sichtbaren Linien zweier aufeinanderfolgender Regler bei normaler Schriftgröße **64 dp** auseinander, durch nativen Layouttest gegen die Mitte zweier RecordingSourcesView-Dropdownzeilen geprüft. Titel und Slider stehen in einer Zeile; zwischen ihnen 8 dp, Breitenverhältnis 1:2. Keine separaten Zahlenzeilen oder negativen Margins in diesen drei Bereichen. Wertebereiche, Schritte, Profil-/Sicherungsfelder und Speicher-Listener unverändert. Tagesbefinden, HRV-Messzeiten, Indoor-Korrektur, Farbhelligkeit und Bereichsfilter nicht umstellen.

## Vorheriger Stand zur Ursachenanalyse

Die folgende Bestandsaufnahme beschreibt die Varianten vor dieser Vereinheitlichung. Für die drei oben genannten Bereiche gilt jetzt die gemeinsame Komponente.

## Drei technische Komponenten

| Typ | Verwendung | Höhe / Darstellung | Hervorhebung |
| --- | --- | --- | --- |
| Native `SeekBar` | HRV-Messzeiten, Indoor-Korrektur, Kartenlinien, Trinkintervall, Benachrichtigungsdauer | Meist 48 dp fest oder als Mindesthöhe; Spur/Griff stammen aus dem Android-Theme, deren Maße sind nicht appweit festgelegt | Vom Minimum nach rechts, aktive Spur PRIMARY. Griff meist PRIMARY; Kartenlinien explizit FOREGROUND |
| `BicycleRatingSlider` (von SeekBar abgeleitet) | Tagesbefinden, Trink-Anzeigedauer, Helligkeit der Karten-Farbpalette | 48 dp an den Einsatzstellen; selbst gezeichnete Spur 3 dp, rundes Linienende; nativer Griff, PRIMARY | Zwischen Ursprung und Auswahl. Tagesbefinden Ursprung Mitte 5/10, Muskelkater/Müdigkeit 0; technische Regler Ursprung 0. Tagesbefinden 11 Werte 0–10 und 2-dp-Rastpunkte |
| `BicycleRangeSlider` | Streckenfilter Distanz/Fahrtzeit/Tempo | 48 dp Mindesthöhe; Spur 3 dp; zwei Griffe mit Außenradius 8 dp (10 bei Tastaturfokus), Innenradius 5 dp. Track endet je 20 dp vom Rand | PRIMARY nur zwischen Unter- und Obergrenze |

## Tatsächliche Layoutvarianten

| Einsatz | Sliderhöhe | Oberhalb | Unterhalb / nächster Block |
| --- | --- | --- | --- |
| Tagesbefinden | 48 | Frage → Slider: **0** | Slider → Skala: **−8**. Nach Skala bis nächster Fragegruppe: **32**. Die gemeinsame Spacing-Normalisierung berücksichtigt vorhandene Margins und dynamische Gruppen |
| HRV-Einstellungen: Ruhe-/Messzeit | 48 | Beschriftung/Wert stehen in einer Zeile über dem Slider; Slider-topMargin **0** | Slider-bottomMargin **8**. Kein 32-dp-Fragenabstand |
| Indoor-Korrekturfaktoren | mindestens 48 | Beschriftung/Wert-Zeile hat topMargin **8**; zwischen dieser Zeile und Slider **8** durch controlSpacing | Eigener Slider-bottomMargin **0**; folgende Beschriftungszeile bringt wieder **8** mit |
| Kartenlinien: Deckkraft/Breite | mindestens 48 | Beschriftung/Wert-Zeile hat topMargin **8**; danach Slider-topMargin **0** | Slider-bottomMargin **0**; folgende Label-/Wert-Zeile hat wieder **8** |
| Trink-Erinnerung: Intervall | 48 | Label links neben Slider; Wert darüber im rechten Grid-Feld. Ganze Gruppe topMargin **8**, zwischen Wertzeile und Slider keine weitere Margin | Eigene bottomMargin **0**, nächste Gruppe topMargin **8** |
| Trink-Erinnerung: Anzeigedauer | 48 | Derselbe Grid-Aufbau wie Intervall; Gruppe topMargin **8**, interne Zusatzmargin **0** | Danach Positionsüberschrift mit topMargin **16** |
| Benachrichtigungen: Anzeigedauer | 48 | Label links; Wert über rechter Sliderhälfte. Ganze Gruppe topMargin **8**, aber Wertzelle bottomMargin **−8** | Eigene Slider-bottomMargin **0**, danach Sticky-Appauswahl mit eigenen Abschnittsabständen |
| Karten-Farbpalette: Helligkeit | 48 | Überschrift mit paddingTop **8** zum Farbraster; Überschrift → Slider **0** | Slider → Skala **−8**; äußerer Palettencontainer unten **8** |
| Streckenfilter: Bereich | 48 | Oberhalb zwei Eingabefelder; Slider-topMargin **0** | Slider-bottomMargin **0**; weitere Abstände kommen vom Filtercontainer |

Skalenbeschriftungen verwenden global 50 % Deckkraft. Tagesbefinden hat keinen separaten ausgewählten Wert oder Resettext; die technischen Einstellungsregler zeigen ihren Zahlenwert weiterhin.

## Was zentral ist – und was nicht

BicycleDesign: touchSize 48, controlGap 8, sliderLabelGap 0, sliderScaleLabelGap −8, sliderQuestionGap 32, sliderScaleLabelAlpha 0,5. BicycleForm.sliderQuestionSpacing normalisiert die markierten Tagesbefinden-Fragegruppen; es ist keine allgemeine Regel für jede SeekBar in jedem Bildschirm.

Die nativen Slider sind an mehreren Stellen direkt angelegt. Grid-/Zeilenaufbau und manche Margins stehen lokal in HrvSettingsView, BikesView, MapLineStyleView, DrinkReminderSettingsView und MessageNotificationSettingsView. Deshalb unterscheiden sich unter anderem Wertposition und Abstand oberhalb der Linie, obwohl fast überall 48 dp Höhe vorgesehen sind. Eine weitere Gestaltungsvereinheitlichung wurde hier nicht umgesetzt.

Quellen: app/src/main/java/de/bcycle/app/BicycleDesign.kt, BicycleForm.kt, BicycleRatingSlider.kt, BicycleRangeSlider.kt und die oben genannten Views sowie HrvQuestionnaireView.kt, FixedColorPalette.kt und RouteQueryPopup.kt. Quellcodeprüfung; keine pauschale Zusage identischer gerenderter Maße auf unterschiedlichen Android-Themes/Schriftskalierungen.
