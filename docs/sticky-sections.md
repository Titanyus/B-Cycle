# Sticky-Kategorien: erster Versuch auf Bikes

`BicycleSectionHeader` ist die gemeinsame Kategoriekomponente: bestehende
23-sp-/900-Black-Schrift, zur vollen Fensterbreite zentriert, deckender
TERTIARY-Hintergrund und oben/unten je eine physische 1-Pixel-Linie in FOREGROUND.
Zentrale Maße: mindestens 56 dp Texthöhe, vertikal 12 dp Innenabstand, außen
12 dp Abstand vor/nach dem Kasten. Zusätzlich bleibt oberhalb der Linien ein
12-dp-Streifen in SECONDARY auch im angedockten Zustand sichtbar. Keine neue Farbe, Schrift oder Nutzereinstellung.

`BicycleStickyScrollView` aktiviert das Andocken gezielt für solche Komponenten.
Im ersten Schritt verwendet nur Bikes den Scrollhost, mit Radtyp und Sensoren.
Andere Dialogüberschriften bleiben unverändert. Die FIT-Aufzeichnung bleibt ein
separates Accordion und beendet den Sensorenabschnitt, ohne selbst anzudocken.

Der ursprüngliche Header wird im nativen Inhaltsbaum verschoben und liegt beim
Scrollen oben unter der festen Dialogtitelleiste. Der nächste Abschnitt schiebt
ihn heraus. Keine zusätzlichen Headerkopien oder verschachtelten ScrollViews.
Die sichtbare Zeichenfläche wird auf den Scroll-Viewport begrenzt; erhöhte
Überschriften können die feste Titelleiste nicht übermalen. Angedockte Header
liegen mit zentral definierten 16 dp Z über nativen Buttons einschließlich
ihres gedrückten Zustands, ohne zusätzlichen Schatten. Native Touchgesten,
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
