# Accordiontest

**Archivierte Designvariante:** Die aktuelle Profilseite verwendet eine lange Seite ohne Haupt-Accordions. Diese Referenz bleibt für eine spätere Rückkehr erhalten.

Referenzstand vom **17.10.2026**, Test-Build **25**, Version **0.7.31**.
Designversuch für die **Profilseite**, keine automatische Umstellung anderer Screens oder verschachtelter FIT-Accordions.

## Gestaltung der Haupt-Accordions

Die fünf Bereiche Bike, FIT-Aufzeichnung, Karten, Design und Trainingshilfe verwenden `ProfileAccordion`.

| Eigenschaft | Festgelegter Wert |
| --- | --- |
| Headerhintergrund | TERTIARY des aktiven Themes, deckend |
| Inhaltshintergrund | SECONDARY des aktiven Themes |
| Schrift | Zentrale Abschnittsschrift, 23 sp / Roboto 900 Black, linksbündig |
| Kategorie-Icon | 32 dp, links vor dem Text, FOREGROUND |
| Zustands-Chevron | Rechts, nach unten geschlossen / nach oben geöffnet |
| Headerhöhe | Mindestens 64 dp; lange Titel dürfen bei gleicher Schriftgröße umbrechen |
| Eckenradius | 10 dp |
| Umlaufender Rahmen | **Keiner**, weder am Header noch am Inhalt |
| Linker Akzent | Geschwungene, 3 dp breite FOREGROUND-Kante mit runden Enden |
| Inhaltseinrückung | Links 4 dp gegenüber dem Header |
| Begleitlinie | Vertikal, FOREGROUND, zentrale Linienbreite 0,75 dp |
| Abstand der Hauptbereiche | 8 dp; bei geöffnetem Bereich von der Begleitlinie überbrückt |

Die helle linke Kante schwingt oben und unten leicht in den Header hinein. Unter einem geöffneten Header setzt die dünne Begleitlinie an der unteren Kurve an, begleitet den Inhalt und verbindet sich ohne Lücke mit der oberen Kurve des nächsten Headers – auch wenn dieser geschlossen ist. Beim letzten geöffneten Bereich endet die Linie am Inhaltsende. Kein unterer oder rechter Abschlussrahmen und kein geschlossener Kasten um den Inhalt.

Der Inhalt überlappt den Header oben um dessen Radius und liegt dahinter. Innenpadding: 12 dp links/rechts/unten, oben Radius plus 8 dp. Dadurch bleiben Rundung und Inhalt verbunden, ohne den Header zu überdecken.

## Unterüberschriften und Sticky-Verhalten

- Zentrierte Überschrift, ebenfalls 23 sp / 900 Black; links und rechts horizontale FOREGROUND-Linien auf halber Texthöhe, 0,75 dp stark.
- SECONDARY als deckender Hintergrund; kein Schatten.
- Mindestens 56 dp Titelhöhe, 12 dp vertikales und 8 dp horizontales Titelpadding.
- Titelbreite maximal 78 % des verfügbaren Bereichs, damit die seitlichen Linien sichtbar bleiben. Lange Titel umbrechen, nicht verkleinern.
- Sticky: Radtyp, Sensoren, FIT-Hauptquelle, Aufzuzeichnende Werte sowie die bestehenden Design-Unterüberschriften.
- Jeder Sticky Header endet an der Grenze seines eigenen Haupt-Accordions (`sectionEnd`). Er darf folgende geschlossene oder geöffnete Hauptbereiche niemals überlagern. Verdeckte Bedienelemente dürfen keine Klicks durch den Sticky Header erhalten.

## Bedienung und Umsetzung

Auf-/Zuklappen, sofortiges Speichern, Profilwechsel, Sensorzuordnung und Footeraktionen behalten ihre Funktion. Der Fahrradname bleibt eine normale antippbare Überschrift zum Umbenennen, kein Sticky Header.

Native Kotlin-Views verwenden: `ProfileAccordion`, `BicycleSectionHeader.inlineRules`, `BicycleStickyScrollView` und die zentrale Geometrie in `BicycleDesign`. Themefarben übernehmen, kein festes Weiß oder Schwarz hinterlegen. Die durchgehende Begleitlinie zeichnet `BikesView` über die Kindansichten; keine zusätzlichen Rahmen pro Inhaltsblock ergänzen.

Untergeordnete FIT-Accordions und andere Seiten behalten ihre bisherige Dropdown-Typografie und Abschnittsoptik. „Accordiontest“ ist eine benannte Designreferenz, kein zusätzlicher Schalter für Nutzer.

## Dauerhafte Referenz und Prüfung

Die folgenden Links sind auf den Tag `test-build-25-apk` festgelegt und bleiben auch bei späteren Änderungen als Vergleich erhalten:

- [Design-Vorschau](https://github.com/Titanyus/B-Cycle/blob/test-build-25-apk/docs/previews/profile-accordions.png)
- [FIT-Vorschau](https://github.com/Titanyus/B-Cycle/blob/test-build-25-apk/docs/previews/profile-fit-accordion.png)
- [Quellstand](https://github.com/Titanyus/B-Cycle/tree/8dd0388a346808b6cf0a430e2e0172a7aa035646)
- [Test-APK Build 25](https://raw.githubusercontent.com/Titanyus/B-Cycle/test-build-25-apk/downloads/B-Cycle-0.7.31-test-build-25.apk)
- [Buildbericht](test-build-25.md)

`ProfileAccordionTest` prüft unter anderem fehlende Außenkonturen, die lückenlose Verbindung zur nächsten Kante, Themewechsel, lange Titel und die Sticky-Begrenzung. Für Build 25 bestanden insgesamt 19 gezielte Tests; native Vorschauen wurden visuell geprüft. Kein zusätzlicher Gerätetest in der Cloud.
