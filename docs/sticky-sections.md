## Aktualisierung: Nahtloser Glow-Kasten und 8dp angehefteter Abstand

Geschlossene Glow-Sticky-Header haben vier runde Ecken. Geöffnet bilden Header und Inhalt eine durchgehende Box: oben runder Header, unten runder gemeinsamer Abschluss. Beim Scrollen übernimmt der Header exakt den unteren Bogen der gemeinsamen Karte, sobald deren Abschluss unter ihn fährt; keine offenen Ecklücken und kein eckiger Rest. Angeheftet bleiben8dp Abstand zum Hauptheader; Nutzerbestätigung vom25.10.2026. Führende Kategorien beginnen ebenfalls mit8dp, normale Elemente weiter16dp. Flat bleibt bei bisheriger bündiger/rechteckiger Anheftung. Pin-Linie = scrollY+8dp inGlow, Push-Grenze weiterhin nächste Headeroberkante; Abstand24dp zwischen Hintergrundboxen bleibt unabhängig vom direkten Headerkontakt beim Wegschieben.

SECONDARY-Matte unter der Headerkontur hält die oberen Rundungen und den beim Scrollen erreichten unteren Kartenbogen frei. Die Kontur wird als Ausschnitt derselben verlängerten Rundung gezeichnet, statt die unteren Ecken dauerhaft abzurunden. Nur bei geänderter Anschlussgeometrie wird der Header neu gezeichnet; keine neuen Zeichenobjekte pro Scrollschritt. Der 8dp-Abstand bleibt beim Anheften bestehen, erhält aber keine deckende Maske: Seiteninhalt scrollt sichtbar bis unter den Hauptheader und seinen transparent auslaufenden Schatten. Native Touch-/Scrollbehandlung bleibt bestehen. Keine neuen Headerkopien oder Scrollcontainer.

## Glow-Kartenversuch ab 25.10.2026

Aufklappbare Sticky-Kategorien bilden in Glow einen TERTIARY-Kasten für Header und geöffneten Inhalt. Geschlossen vier runde Ecken, geöffnet runder Header oben und gemeinsame Karte unten, Radius18dp. Keine Rahmen und keine horizontalen Headerlinien. SECONDARY zwischen den Karten macht ihre Form sichtbar. Glow-Karten bekommen16dp seitlichen Randabstand, weitere16dp Einzug für den Inhalt und16dp Luft unten nach dem Body. Zwischen Boxen24dp, sowohl bei geöffneten als auch geschlossenen Abschnitten. Flat behält seine bisherigen Maße und Abstände.

BicycleSectionCards zeichnet den Verbund auf dem vorhandenen Parent-Hintergrund; keine neuen Container oder Reparentings. Header und Body bleiben Geschwister, alle IDs/Listener/Entwürfe sowie die bestehende native Sticky-/Push-Mechanik erhalten. Der Body ist in Glow transparent über der gemeinsamen Karte; Flat nutzt seinen bisherigen Hintergrund. Theme-/Designwechsel aktualisieren die Darstellung. BicycleSectionCards.spacing setzt Glow-Margins über BicycleContentSpacing idempotent und stellt beim Flat-Wechsel die vorherige Geometrie wieder her. Die16dp unten werden im Hintergrund und reservierten Layoutbereich ergänzt; kein Reparenting oder neue Container. Die Sticky-Push-Grenze bleibt an der nächsten Headeroberkante, unabhängig vom Ende der Hintergrundkarte und dem24dp-Seitenabstand: angeheftete Header stoßen direkt aneinander und schieben sich ohne Zwischenspalt weg. Normale Dropdowns/Accordions bleiben unverändert. Der Profilnamenabschnitt ist nun in beiden Designs ein reguläres aufklappbares Sticky-Accordion; in Glow dieselbe Box. Der Titel erlaubt weiterhin Umbenennen, die Chevronfläche klappt separat. Der Editor erscheint im Header, sodass Box und Offen-Zustand erhalten bleiben. Zustandskennung bike_profile ist unabhängig vom editierbaren Profilnamen.

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

Abstand vor erster Glow-Karte: Stehen davor eigenständige Inhalte (z. B. Radprofil-Kacheln), gilt auch in Glow der zentrale Abschnittsabstand sectionEndGap=32dp. Der frühere vorzeitige Rücksprung der Karten-Geometrie darf diesen Abstand nicht überspringen. Zwischen Header/Body-Karten weiterhin24dp, keine doppelten Abstände und kein Aufsummieren bei erneutem Layout.
