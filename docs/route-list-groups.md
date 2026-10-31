# Gruppierte Streckenübersicht – 31.10.2026

Die aktive Sortierung bestimmt die Gliederung. Bei Datum steht vor der ersten Fahrt jedes Monats Monat+Jahr in der Hauptheader-Schrift (League Gothic, Großbuchstaben). Bei Zahlenwerten stehen die einfachen Titel „Distanz bis25km“, „Fahrzeit bis1,5h“ bzw. „Ø-Tempo bis25km/h“. Gruppen folgen der auf-/absteigenden Sortierung; keine leeren Gruppen, fehlende Sortierwerte zuletzt als „Ohne Angabe“.

Exklusive Obergrenzen, wie besprochen: exakt25km liegt in der folgenden Gruppe. Distanz:10/25/50/75/100/125/150/200km, danach50-km-Schritte. Bewegungszeit:30/60/90/120min, danach Stunden. Tempo:20km/h, danach5-km/h-Schritte. Interne Grenzen bleiben eindeutig, angezeigt wird nur die Obergrenze.

Unter dem Titel vier Kennzahlen in derselben Wert-/Einheiten-/Label-Komponente wie die Fahrten: Aktivitätenzahl, Summe gültiger Distanzen, Summe gültiger Bewegungszeiten, gewichtetes Durchschnittstempo. Tempo ist Distanzsumme/Bewegungszeitsumme ausschließlich aus Fahrten, für die beide Angaben gültig sind und die Bewegungszeit positiv ist. Fehlende Angaben sind keine0; gültige echte0 bleiben erhalten. Alle Filter wirken vor Gruppenbildung und Summierung.

RouteListSections bereitet Grenzen, Labels und Summen gemeinsam vor. RouteManager hält diesen Stand, bis sich die gefilterten Fahrten oder Sortierung ändern; ein reiner Uploadstatus-/Busy-Wechsel baut die Summen nicht neu.

Gruppenköpfe sind seit der Nutzerkorrektur nachBuild112 eigenständige, nicht auswählbare Adaptereinträge mit eigenem Viewtyp. Fahrtkarten enthalten ausschließlich die Fahrt; Köpfe können nicht durch Longpress oder Recycling mitmarkiert werden. Indexerpositionen berücksichtigen die zusätzlichen Kopfzeilen; fachliche Auswahl und Aktionen verwenden weiterhin stabile Fahrt-IDs.

Der aktuelle Gruppenkopf einschließlich Kennzahlen bleibt als Sticky-Header unter dem Hauptheader stehen. Der nächste Kopf schiebt ihn nach oben weg. Der anfängliche Seitenabstand scrollt weg. Gecachte Kopf-View ohne Datei-/Summenberechnung im Zeichenpfad, deckender SECONDARY-Hintergrund; Fast-Scroll-Zugpunkt/Label liegen darüber. Touch auf dem angehefteten Kopf wird an die native Scroll-/Flingbehandlung weitergegeben, ohne die verdeckte Fahrt zu öffnen/selektieren.

## Footer und Augenfarben

Standardmäßig ist der Footer verborgen. Ein Panel-bottom-Symbol links neben Info blendet ihn manuell ein/aus. Longpress auf eine Fahrt blendet ihn vorübergehend für die Mehrfachauswahl ein. Endet die Auswahl, endet die temporäre Einblendung; eine manuelle Einblendung bleibt erhalten. Manuelles Ausblenden ist auch während Auswahl möglich. Beim erneuten Öffnen der Seite beginnt der Footer verborgen.

Sortieren verwendet ein seitenbreites Popup mit fünf gleich hohen Icon-/Textkacheln in einer Zeile, im Stil der Fahrtbenennung. Vier Kriterien einzeln auswählbar; fünfte Kachel schaltet die Richtung mit aktuellem Text um. Keine Unterdropdowns, Änderungen sofort, vorhandene Filter bleiben erhalten.

Farbige Augen bekommen ihre aktuelle Trackplatzfarbe auch bei Theme-Neuanbindung nach Detach/Reattach zurück; die generische Icontönung darf die fachliche Farbe nicht überschreiben. Farben bleiben deckend, Sichtbarkeitszustand und Platzzuordnung unverändert.

## Fast-Scroller

Nach der abschließenden Nutzerentscheidung nach dem Chat-Absturz gilt: **Gruppenweise springen**, nicht stufenlos am Zugpunkt. Nur die Streckenübersicht aktiviert sectionSteps. Jeder Abschnitt erhält einen gleich großen Bereich der Zugstrecke; Ziel ist der eigenständige Gruppenkopf. Vorschau und Gruppenkopf verwenden dieselbe Beschriftung. Normales Scrollen bleibt stufenlos/kinetisch, der Thumb berücksichtigt die Lage innerhalb einer Gruppe. Andere Listen, insbesondere der Dreeve-Import, behalten den bisherigen stufenlosen Fast-Scroller.

## Prüfung

40 gezielte Tests erfolgreich: Grenzfälle/Filter/Gewichtung/fehlende Werte, native Header-/Einheitengeometrie in schmalen Breiten und beiden Designs, Recycling/Bedienung, tatsächliche Fast-Scroll-Sprünge und stabiler Thumb bei normalem Scrollen, bestehende Filter-/Intervals-Tests. Log `/workspace/.tools/route-groups-hrv-final-tests.log`. Native Vorschauen `app/build/reports/ui/route-groups-flat.png` und `route-groups-glow.png`; Flat visuell geprüft. Typografie/Atlas/Diff geprüft. Kein physischer Gerätetest. Veröffentlicht in[0.8.71/Build112](build-112.md).

NachBuild112:67 gezielte Listen-/Farb-/Sticky-/Dialogtests erfolgreich, abschließend81 relevante Tests über Gesamtlauf und8 erfolgreiche Nachprüfungen der finalen Popupgeometrie. Logs`route-sticky-colour-footer-final-tests.log`, `route-sticky-sort-release-tests.log`, `route-sort-geometry-final-tests.log`. 320/360/480-dp-Kachelreihe beider Designs nativ geprüft und Vorschau visuell gesichtet; keine Geräteprüfung.
