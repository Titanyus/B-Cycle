# Sticky-Kategorien in den Einstellungen

`BicycleSectionHeader` ist die gemeinsame Kategoriekomponente: bestehende
23-sp-/900-Black-Schrift, zur vollen Fensterbreite zentriert, deckender
SECONDARY-Hintergrund und oben/unten je 0,75 dp FOREGROUND. Die Umrechnung
rundet anhand der tatsächlichen Android-Anzeigedichte auf volle Pixel
(mindestens einen Pixel). Bei Dichte 2,5–3 entspricht der Testwert 2 Pixeln.
Mindestens 56 dp Texthöhe und vertikal 12 dp Innenpadding bleiben erhalten.
Keine Außenabstände, kein oberer Leerstreifen, kein Sticky-Schatten. Abstände
gehören zum Inhalt: davor und danach 16 dp, siehe [zentrale Abstände](ui-content-spacing.md).

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
ihres gedrückten Zustands. Es wird kein Sticky-Schatten gezeichnet; der
Schatten des festen Hauptheaders bleibt eigenständig. Native Touchgesten,
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
Titelleiste beim Zeichnen, Schrift, Themewechsel, dichteabhängige Linien, volle Breite,
Auswahlrahmen und bestehende Bike-/FIT-/Themebedienung. Keine neue Bibliothek,
keine Änderung an Sensoren, Aufzeichnung oder FIT-Inhalten.

## Prüfung der erweiterten Anwendung

Aktuelle gezielte Prüfung umfasst Inhaltsabstände, Nachbar-Header, Sensor-Icons,
Fotopicker/-speicherung/-löschung, quadratische Detailkarte und bestehende
Navigation/Dashboard-/FIT-Bedienung. Testergebnis im Abschlussbericht und in
[den zentralen Konventionen](ui-content-spacing.md).

Accordion-Beschriftungen sind ausdrücklich keine Sticky-Abschnittstitel:
BicycleDialogStyle.accordion verwendet wie Dropdowns 16 sp, normale Systemschrift
und linksbündigen Text. Bedienung, Expand-Icon und Zustand bleiben erhalten.
