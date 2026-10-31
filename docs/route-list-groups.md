# Gruppierte Streckenübersicht – 31.10.2026

Die aktive Sortierung bestimmt die Gliederung. Bei Datum steht vor der ersten Fahrt jedes Monats Monat+Jahr in der Hauptheader-Schrift (League Gothic, Großbuchstaben). Bei Zahlenwerten stehen die einfachen Titel „Distanz bis25km“, „Fahrzeit bis1,5h“ bzw. „Ø-Tempo bis25km/h“. Gruppen folgen der auf-/absteigenden Sortierung; keine leeren Gruppen, fehlende Sortierwerte zuletzt als „Ohne Angabe“.

Exklusive Obergrenzen, wie besprochen: exakt25km liegt in der folgenden Gruppe. Distanz:10/25/50/75/100/125/150/200km, danach50-km-Schritte. Bewegungszeit:30/60/90/120min, danach Stunden. Tempo:20km/h, danach5-km/h-Schritte. Interne Grenzen bleiben eindeutig, angezeigt wird nur die Obergrenze.

Unter dem Titel vier Kennzahlen in derselben Wert-/Einheiten-/Label-Komponente wie die Fahrten: Aktivitätenzahl, Summe gültiger Distanzen, Summe gültiger Bewegungszeiten, gewichtetes Durchschnittstempo. Tempo ist Distanzsumme/Bewegungszeitsumme ausschließlich aus Fahrten, für die beide Angaben gültig sind und die Bewegungszeit positiv ist. Fehlende Angaben sind keine0; gültige echte0 bleiben erhalten. Alle Filter wirken vor Gruppenbildung und Summierung.

RouteListSections bereitet Grenzen, Labels und Summen gemeinsam vor. RouteManager hält diesen Stand, bis sich die gefilterten Fahrten oder Sortierung ändern; ein reiner Uploadstatus-/Busy-Wechsel baut die Summen nicht neu. Native ListView/Recycling bleiben bestehen. Gruppenkopf ist ein Präfix der ersten echten Fahrt der Gruppe, kein zusätzliches Adapterelement: Anzahl, Mehrfachauswahl, Augen-/Detailaktionen und Abschnittspositionen beziehen sich weiterhin auf echte Fahrten. Tippen auf den Gruppenkopf öffnet keine Fahrt. Kein Sticky-/Accordionverhalten hinzugefügt.

## Fast-Scroller

Nach der abschließenden Nutzerentscheidung nach dem Chat-Absturz gilt: **Gruppenweise springen**, nicht stufenlos am Zugpunkt. Nur die Streckenübersicht aktiviert sectionSteps. Jeder Abschnitt erhält einen gleich großen Bereich der Zugstrecke; Ziel ist der erste echte Fahrt-Eintrag mit seinem Gruppenkopf. Vorschau und Gruppenkopf verwenden dieselbe Beschriftung. Normales Scrollen bleibt stufenlos/kinetisch, der Thumb berücksichtigt die Lage innerhalb einer Gruppe. Andere Listen, insbesondere der Dreeve-Import, behalten den bisherigen stufenlosen Fast-Scroller.

## Prüfung

40 gezielte Tests erfolgreich: Grenzfälle/Filter/Gewichtung/fehlende Werte, native Header-/Einheitengeometrie in schmalen Breiten und beiden Designs, Recycling/Bedienung, tatsächliche Fast-Scroll-Sprünge und stabiler Thumb bei normalem Scrollen, bestehende Filter-/Intervals-Tests. Log `/workspace/.tools/route-groups-hrv-final-tests.log`. Native Vorschauen `app/build/reports/ui/route-groups-flat.png` und `route-groups-glow.png`; Flat visuell geprüft. Typografie/Atlas/Diff geprüft. Kein physischer Gerätetest und keine neue APK beauftragt.
