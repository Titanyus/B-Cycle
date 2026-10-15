# Sticky-Kategorien in den Einstellungen

`BicycleSectionHeader` ist die gemeinsame Kategoriekomponente: bestehende
23-sp-/900-Black-Schrift, zur vollen Fensterbreite zentriert, deckender
TERTIARY-Hintergrund und oben/unten je eine physische 1-Pixel-Linie in FOREGROUND.
Zentrale Maße: mindestens 56 dp Texthöhe, vertikal 12 dp Innenabstand, außen
12 dp Abstand vor/nach dem Kasten. Zusätzlich bleibt oberhalb der Linien ein
12-dp-Streifen in SECONDARY auch im angedockten Zustand sichtbar. Keine neue Farbe, Schrift oder Nutzereinstellung.

`BicycleStickyScrollView` aktiviert das Andocken gezielt für solche Komponenten.
Aktiviert auf Fahrer (vier Kategorien), Bikes (Radtyp, Sensoren, FIT-Aufzeichnung),
Daten & Speicher (Globale Daten, Streckendaten) und Design & UI (Theme,
Dashboard-Layout, Fahrmodus-Anzeige). Karten bleiben unverändert. Die
FIT-Aufzeichnung ist ein normaler Abschnitt; nur ihre zusätzlichen Datenquellen
stehen jeweils in einem Accordion. Hauptquellen für Tempo/Distanz bleiben direkt
sichtbar. Vorhandene Feldauswahl-Accordions bleiben erhalten.

Der ursprüngliche Header wird im nativen Inhaltsbaum verschoben und liegt beim
Scrollen oben unter der festen Dialogtitelleiste. Der nächste Abschnitt schiebt
ihn heraus. Keine zusätzlichen Headerkopien oder verschachtelten ScrollViews.
Die sichtbare Zeichenfläche wird auf den Scroll-Viewport begrenzt; erhöhte
Überschriften können die feste Titelleiste nicht übermalen. Angedockte Header
liegen mit zentral definierten 16 dp Z über nativen Buttons einschließlich
ihres gedrückten Zustands. Ein gemeinsam gecachter 12-dp-Verlaufsschatten
(maximal 102/255 Schwarz, 40 % Deckkraft) wird ausschließlich unterhalb der Header gezeichnet;
keine Änderung der Geometrie und kein großer nativer Z-Schatten. Native Touchgesten,
Fokus, Accessibility-Überschriften, Profilwechsel und Scrollpositionen bleiben
erhalten. Positionssuche nur beim Layout; beim Scrollen nur Positionsänderungen
für die wenigen registrierten Kategorieheader.

Die Sensor-Schalter GPS, E-Bike, Powermeter und Pulsmesser übernehmen selbst
die Abschnittstypografie (23 sp / 900 Black), statt zusätzliche Überschriften
zu duplizieren. Schaltergrafik, Status und Funktionen bleiben erhalten.

## Auswahlrahmen

`BicycleComponents.selectionBorder` liefert für ausgewählte Kacheln ACCENT,
ansonsten FOREGROUND. Angewandt auf Bike-, Theme- und Dashboard-Preset-Kacheln
sowie ausgewählte Formularbuttons. Füllfarben, Rahmenbreiten und Geometrie
bleiben erhalten. Nicht ausgewählte Theme-Kacheln behalten ihren bisherigen
abgeschwächten Rahmen.

## Vorschau und Prüfung

Native Android-Ansichten, EMBERS mit synthetischem Bike-Profil:
[Seitenanfang](previews/bikes-sections.png) · [angedockt](previews/bikes-sticky.png).

Gezielte native Tests prüfen zwei Hochformatbreiten, Andocken, Verdrängen,
Abschnittsende, Rückwärtsscrollen, Drag direkt auf dem Header, vollständige
Titelleiste beim Zeichnen, Schrift, Themewechsel, 1-Pixel-Linien, volle Breite,
Auswahlrahmen und bestehende Bike-/FIT-/Themebedienung. Keine neue Bibliothek,
keine Änderung an Sensoren, Aufzeichnung oder FIT-Inhalten.

## Prüfung der erweiterten Anwendung

25 gezielte Tests aus SettingsSectionsTest, BicycleStickySectionsTest,
BikesViewTest, PerformanceSettingsTest, RiderProfileViewTest,
SettingsDesignMigrationTest, RouteExportNavigationTest,
DataSettingsNavigationTest und ThemePickerTest bestanden. Geprüft werden unter
anderem Abschnittsreihenfolge, ein einzelner Scrollhost, Zahlenzeilen auf
320/360-dp-Displays, Themewechsel, Profilvalidierung, Navigation sowie FIT-Auswahl
und Accordion-Zustand nach Profilwechsel. Release-Kotlin-Kompilierung erfolgreich.
Keine vollständige Testsuite und keine neue APK für diese Umstellung.

Accordion-Beschriftungen sind ausdrücklich keine Sticky-Abschnittstitel:
BicycleDialogStyle.accordion verwendet wie Dropdowns 16 sp, normale Systemschrift
und linksbündigen Text. Bedienung, Expand-Icon und Zustand bleiben erhalten.
