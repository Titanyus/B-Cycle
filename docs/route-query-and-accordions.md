# Accordions, Dropdownflächen und Streckenfilter – Arbeitsstand nach 0.8.4

Kein APK-Build angefordert. Alle Sticky Header werden aufklappbar; der Fahrradtitel bleibt ein Umbenennen-Zugang. Bestehende Profile/Fahrer/Hilfe-Gruppen behalten ihre Bindungen, andere gemeinsame Sticky-Abschnitte werden einmalig vor der Messung gruppiert. Standard offen, außer ausdrücklich anfangs geschlossenen Auswahldialoggruppen/Hilfen. Native Eingaben und Listener bleiben erhalten.

Dropdown-Fenster TERTIARY, Kontur weiter 1 dp FOREGROUND 50 %, Rundung 6 dp. Die Basisbuttons behalten ihre eigene Fläche. Horizontale Abschnittstrenner 16 dp oben/unten; die zuvor bewusst kompakten Fahrdatenblöcke bleiben bei ihrem kleineren Abstand.

Benachrichtigungen: Schalter und Chevron unabhängig; im Inhalt Android-Zugriff, Erklärung und Status. Darunter Benachrichtigungsposition mit normaler Inhaltsüberschrift, kein zusätzlicher Sticky Header. Sekundenwert näher an der Sliderlinie. Die Appauswahl ist ein Sticky-Accordion.

Erweitert zeigt Wiederherstellung ungespeicherter Fahrten und GPS-Erklärung nicht mehr als Menüeinträge; ihre zugrundeliegenden Funktionen werden nicht entfernt.

Strecken: Footer mit Import, Filter, Sortieren. Kein oberes Filterdropdown, Auge bei fehlenden GPS-Daten vollständig ausgeblendet. Filter- und Sortierfenster über dem Footer öffnen, begrenzt und scrollbar. Die sechs Filtergruppen beginnen eingeklappt und zeigen eine Zusammenfassung. Sofortige Anwendung, keine Bestätigungs-/Abbrechen-Footer. Native Tastatur für Bereichsfelder. Ungültige Entwürfe ersetzen keine gültigen Filter. Datumsdialog übernimmt Änderungen unmittelbar. Bewegungszeit in h:mm, Distanz km, Geschwindigkeit km/h. Datum von/bis inklusive ganzer lokaler Tage, auch an Sommerzeitgrenzen. Schnellwahl Montag–Sonntag und vollständige Kalendermonate.

Importiert/Bike/E-Bike/Indoor mehrfach auswählbar; Fahrradfilter nach gespeicherter Profilzuordnung, zusätzliche Auswahl Ohne Zuordnung. Fehlende Werte bleiben ohne entsprechenden Filter sichtbar, werden bei aktiven Bereichen ausgeschlossen. Sortierung Datum/Distanz/Bewegungszeit/Ø Geschwindigkeit in beiden Richtungen, fehlende Werte stets am Ende. Sortierung stabil bei gleichen Werten. Filter zurücksetzen erhält die Sortierung. Filter-/Sortierzustand bleibt in der Manageransicht auch bei Rückkehr aus Details.

Detailkopf: links Herkunfts-/Radtyp-Icon und Dateiformat; rechts zentrierte Textzeilen Fahrradname/Datum. Der Zweizeilenplatz bleibt auch bei älteren Fahrten ohne bekannte Zuordnung erhalten. Keine künstlichen Radzuordnungen. Wettertrenner verwendet 16-dp-Abstände, kompakte Kennzahlengruppen bleiben erhalten.

Gezielte Tests decken Bereichskombinationen, Mehrfachauswahl, unbekannte Werte, inklusive Datumsgrenzen, Sortierfelder/-richtung, Reset, Popup-Eingaben/Entwürfe, h:mm, Wochenwahl, Accordion-Zustand/Erhalt der Eingaben, Benachrichtigungen, Auswahlmenü und Listen-/Detail-Navigation ab. Kein realer Gerätetest.

## Kompakte Filterbedienung ab 0.8.6 (19.10.2026)

Filter und Sortierung verwenden normale Accordion-Buttons wie die Dashboard-Feldauswahl, keine großen Sticky-Header. Fensterhöhe nach Inhalt bis maximal 75 % der verfügbaren Höhe, Sortierung maximal 320 dp; bei Ausklappen oberhalb des Footers neu positionieren. Kein Zurückbutton im Popup, außerhalb tippen oder Android-Zurück schließt es. TERTIARY und gemeinsamer Rahmen bleiben erhalten.

Streckenart: unabhängig schaltbare Segmente im Stil der Trainingshilfe, keine erzwungene Einfachauswahl. Keine Auswahl bedeutet alle. Fahrräder: mehrfache Auswahl über Profilkacheln in drei Spalten, zusätzliche Auswahl Ohne Zuordnung erhalten.

Distanz, Bewegungszeit und Geschwindigkeit kombinieren native schmale Zahleneingaben mit einem gemeinsamen Zwei-Griff-Slider. Schritte ab 0.8.54: 5 km / fünf Minuten / 5 km/h; Skala aus den vollständigen vorhandenen Fahrten, nach oben auf 10 km / eine Stunde / 5 km/h gerundet. Ohne Fahrtdaten 100 km / 8 h / 50 km/h. Größere gültige Texteingaben erweitern die Skala; ungültige Entwürfe verändern keine gültige Query. Slider und Felder synchron, keine kreuzenden Griffe. Tastatur und getrennte Accessibility-Aktionen für beide Griffe. Keine neue Datenaufzeichnung oder Statistik.

## Korrigierte Dropdown-Hierarchie nach 0.8.8 (19.10.2026)

Diese Regel ersetzt die vorherigen Flächen-/Accordion-Versuche für Filter/Sortierung: Das Hauptfenster über dem Footer und seine Auswahlbuttons verwenden SECONDARY. Jede Gruppe öffnet ein eigenes natives, am Auswahlbutton verankertes Dropdown mit TERTIARY-Fläche und dem gemeinsamen Rahmen/Rundungswert. Normale Buttons und Eingaben darin verwenden SECONDARY; semantische Auswahlzustände bleiben erhalten. Native Dropdownpositionierung unterhalb oder oberhalb des Auslösers statt über dessen Titel.

Gruppen behalten ihre Eingaben/Listener und Entwürfe bis zum Schließen des Hauptfensters. Außerhalb tippen bzw. Zurück schließt zunächst das aktive Unterfenster. Beim Schließen des Hauptfensters wird das Unterfenster ebenfalls entfernt. Keine neue Bestätigungsaktion.

Keine feste 320-dp-Grenze für Sortierung. Hauptmenühöhe aus Inhalt (maximal 75 % der verfügbaren Fläche), Unterfenster aus Inhalt und nativ verfügbarem Platz. Zentrale Controlabstände vor dem Messen an den endgültigen LayoutParams anwenden; spätes Styling darf keinen künstlichen Scrollbedarf erzeugen. 8-dp-Innenabstand der Gruppen bleibt erhalten.

Der Segmentzeile für Importiert/Bike/E-Bike/Indoor liegt kein eigener gefüllter/umrahmter Kasten mehr zugrunde; nur die eigentlichen Segmente zeichnen ihre Flächen. Dadurch kein heller Saum hinter den vier Feldern.

## Getrennte Herkunfts-/Radtypfilter und gemeinsamer Segmentrahmen

Herkunft/Streckenart bietet nur „Importiert“ und „Gefahren“, der separate Filter Radtyp „Bike“, „E-Bike“ und „Indoor“. Beide Gruppen erlauben Mehrfachauswahl (ODER innerhalb, UND zwischen Gruppen). Leere Gruppen begrenzen nichts. Herkunft richtet sich ausschließlich nach TrackOrigin, unabhängig vom Radtyp: selbst aufgezeichnete Altfahrten ohne Radtyp gehören zu Gefahren. Importierte Fahrten können nach Radtyp gefiltert werden, sofern dieser bekannt ist; unbekannte Typen fallen bei einem aktiven Typfilter heraus. Reset und Filterzähler berücksichtigen die neue Gruppe. Dynamische Verfügbarkeit siehe folgenden Abschnitt.

Die vorige rahmenlose Segmentdarstellung wird durch den zentralen Helfer BicycleComponents.segmentedControlRow ersetzt: derselbe dezente 1-dp-Rahmen in FOREGROUND 50 %, gemeinsame Rundung und SECONDARY-Fläche wie bei der Zonenauswahl der Trainingshilfe. Innenpadding exakt Rahmenbreite, damit der Rahmen sichtbar bleibt und keine zusätzliche Fläche hinter den Segmenten hervorsteht. Trainingshilfe und beide Filtergruppen verwenden denselben Helfer; Auswahlzustände behalten ihre ACCENT-Füllung und native Semantik.

## Dynamische Verfügbarkeit der ersten drei Filter

RouteQuery.availability bestimmt aus dem vollständigen Fahrtenbestand, welche Herkunftsarten, Radtypen und Profilzuordnungen unter den übrigen Filtern vorkommen. Die jeweils eigene Gruppe wird dabei ignoriert, damit Mehrfachauswahl erweitert werden kann. Datum, Distanz, Bewegungszeit und Geschwindigkeit wirken bei dieser Prüfung mit. Das Prädikat wird mit der normalen Ergebnisliste geteilt; für Verfügbarkeit wird nicht sortiert und die Query nicht verändert. Profilzuordnungen pro geöffnetem Filterfenster zwischenspeichern.

Nur die Optionen der ersten drei Gruppen werden bei fehlenden Treffern deaktiviert und gedimmt. Ausgewählte Optionen bleiben immer bedienbar, auch bei null Treffern. Auswahl niemals automatisch entfernen. Datums-/Bereichsgruppen, Slider und Gruppenöffner bleiben jederzeit aktiv. „Ohne Zuordnung“ wird wie ein weiteres Profil geprüft. Nach gültigen Eingabeänderungen und vor dem Öffnen eines Unterfensters aktualisieren.

## Kartenfilter und gröberes Raster ab 0.8.54

Im Streckenmanager steht vor Datum ein unabhängig umschaltbarer Button „Auf Karte angezeigt“. Er filtert auf die tatsächlich sichtbare Strecke, nicht nur auf eine geladene, ausgeblendete Strecke. Ohne sichtbare Strecke bleibt die Ergebnisliste bei aktivem Button leer. Kombination mit anderen Filtern, Verfügbarkeitsberechnung und Zurücksetzen sind integriert. Dreeve-/Archivimport bekommen keinen Kartenfilter.

Die drei Bereichsslider verwenden 5 km, 5 km/h und fünf Minuten als Schritte; direkte Texteingaben bleiben unverändert möglich. Zwölf gezielte Modell-/UI-Prüfungen erfolgreich, einschließlich Schrittraster und Accessibility.
