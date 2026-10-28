# Statistiken

Einstellungen → Statistiken, unmittelbar nach Strecken. Eigener Vollbild-Einstellungsdialog mit normalem Zurück-Verhalten und vertikalem Scrollbereich.

## Gemeinsame Jahreswahl und Diagramm

Nutzerentscheidung28.10.2026: ein gemeinsames Distanzdiagramm, umschaltbar **Monat · Woche**, statt zwei untereinanderstehenden Diagrammen. Eine mittige Jahresauswahl darüber gilt für das Diagramm und die Fahrrad-Fahrzeitverteilung. Mehrere Jahre gleichzeitig auswählbar; der farbige Punkt steht direkt vor dem Jahr im Auswahlbutton. Keine doppelte Jahreslegende. Native Mehrfachauswahlbuttons und zentrale Zonenpalette wie bisher; dekorative Jahrespunkte ignorieren Button-Tint, damit Datenfarbe in aktivem/inaktivem Zustand erhalten bleibt.

- Monat: zwölf Monatsnamen und Monatssummen nach lokalem Kalenderjahr.
- Woche: ISO-Wochensummen ausgewählter Jahre als Kurven; nur Wochennummern auf der X-Achse, ohneKW. Ein53-Wochen-Jahr kann mit52-Wochen-Jahren verglichen werden; im52-Wochen-Jahr gibt es keinen erfundenen53.Wert. ISO-Jahresgrenzen bleiben korrekt, beispielsweise29.12.2025 gehört zur Woche1/2026 und1.1.2027 zur Woche53/2026. Die Jahreswahl meint bei Wochen das ISO-Wochenjahr, bei Monaten/Fahrradzeiten das Kalenderjahr.
- Beide Auflösungen: formtreue Kurven ohne Überschwingen (vorhandener MonotoneGraphPath), Punktmarkierungen und kleine Kilometerzahlen in jeweiliger Jahresfarbe. Zahlen nicht wegen Überschneidungen auslassen; vorzugsweise oberhalb, bei Platzkonflikten mit Skala/anderen Zahlen knapp unterhalb bzw. höher oberhalb. Bei extrem vielen nahen Werten bleiben Überlagerungen möglich; Zoom/Jahreswahl helfen.
- Y-Achsenwerte links **innerhalb** der vollen Diagrammbreite, jeweils oberhalb ihrer durchgehenden Rasterlinie, kein reservierter linker Achsenstreifen und kein0-km-Skalenwert. Dezimalstellen bei niedrigen Skalen erhalten, damit Viertelwerte nicht falsch ganzzahlig wirken. Keine hellen Abschnittstrenner unter den X-Achsen; auch die frühere Basisrasterlinie entfällt. Verbleibende horizontale Rasterlinien bleiben dezent.
- Keine gewählten Jahre: Hinweis statt Diagramm, keine veraltete Tortengrafik. Bewusst leere Auswahl bleibt bei Datenaktualisierung leer. Gleicher unveränderter Snapshot erhält Auswahl und Zoom.

## Wochenzoom

Native Zweifinger-Geste ohne Android-Mindestspanne; auch eng zusammengezogene Finger funktionieren bis zur tatsächlichen Bereichsgrenze. Pointerwechsel neu verankern, Nullspanne ignorieren. Zoom um Fingermitte, gemeinsames Verschieben der Finger ebenfalls berücksichtigt. Horizontaler Drag mit kinetischem Nachlauf; neuer Finger stoppt Fling. Vertikales Seitenscrollen bleibt möglich. Accessibility-Aktionen erlauben Zoom und Vor-/Zurückblättern.

Standard14Wochen, Zoomgrenze4 bis52/53Wochen des Vergleichs. Keine neue Einstellung für technische Grenzen. X-Achsentext bei engem Zoom in festen Nummernschritten ausdünnen, nie am Rand festhalten. Punktwerte bleiben gezeichnet und werden an den Plotgrenzen abgeschnitten. Monatsansicht zeigt das volle Jahr; Wochenansicht behält ihren Ausschnitt beim Umschalten und bei der Jahreswahl, begrenzt nur auf die tatsächlich vorhandenen52/53Wochen. Die Y-Skala folgt den kompletten gewählten Jahresreihen; Zoom/Pan verändern ausschließlichX, keine Neuberechnung aus dem sichtbaren Ausschnitt.

## Fahrradzeiten

Tortengrafik und Stunden-/Prozentlegende verwenden **die Summe der ausgewählten Kalenderjahre**. Bewegungszeit, bei fehlendem/ungültigem Wert gespeicherte Fahrtdauer. Farben bleiben ausdrücklich die Fahrradprofilfarben aus RideProfileCatalog, keine Jahresfarben. Unzugeordnet neutral; stabile gespeicherte Fahrradidentitäten/Metadaten bleiben bei gelöschten Profilen erhalten. Keine Zeit ohne bekanntes Datum einem erfundenen Jahr zuordnen.

## Daten, Cache und Aktualisierung

Es zählen nur Einträge mit TrackOrigin.RECORDED: eigene Fahrten sowie ausdrücklich als gefahren markierte Importe, einschließlich Dreeve-Zusammenfassungen. Geplante/importierte Routen zählen nicht automatisch. Fehlende Distanz/Zeit wird nicht geschätzt. Es werden ausschließlich vorhandene Bibliothekskurzkennzahlen verwendet; keine neuen FIT-/GPX-Lesevorgänge und keine Änderung an Aufnahme, Fahrtkennzahlen, Uploads oder Sicherungen.

MapState nutzt den vorhandenen Bibliotheksworker. RideStatisticsCache hält einen unveränderlichen Snapshot mit Monatssummen je Kalenderjahr, Wochensummen je ISO-Jahr und Fahrradzeiten je Kalenderjahr. Cache-Eingaben: statistikrelevante Kurzkennzahlen, stabile Radidentitäten/Namen/Farben, aktuelles Jahr und Zeitzone. Keine privaten Daten in öffentlichen Dateien. Gleiche Daten liefern dieselbe Snapshotreferenz; reine Fahrtumbenennung/Kartensichtbarkeit erzeugen keine neue Aggregation. Bestands-, Kennzahlen-, Zuordnungs-, Profilmetadaten- und Kalender-/Zeitzonenänderungen aktualisieren den Snapshot. Veraltete Workerantworten werden über Generationen verworfen; Schließen entfernt den Callback.

Jahreswahl summiert nur fertige kleine Jahres-/Fahrrad-Buckets, liest keine Fahrten erneut. Gesten ändern nur gecachte Canvas-Geometrie; Werttexte werden beim Serienwechsel vorformatiert, keine Datenaggregation beim Zeichnen/Scrollen. Kein Dateicache, keine neue Bibliothek oder dauerhafte Arbeitsqueue.

## Prüfung

Zehn unterschiedliche gezielte Fälle grün: RideStatisticsTest4, RideStatisticsViewTest1 und StatisticsDistanceChartTest5. Modell: lokale Monate/ISO-Jahre,52/53Wochen, Ausschluss geplanter/undatierter Fahrten, Zeitfallback und Profilfarben, Summierung ausgewählter Jahre sowie Cachewiederverwendung. Native Flat/Glow: gemeinsame Jahreswahl wirkt auf Diagramm und Torte, Farbpunkte bleiben in Buttons, nur ein Diagramm, Auflösungswechsel erhält Wochenzoom, bewusst leere Auswahl bleibt auch bei aktualisierten Daten leer. Canvas/Gesten: innere rechte Y-Skala ohne0, Linien statt Balken, Monats-/Wochenpunktwerte in Jahresfarben, enger Pinch/Pointerwechsel, feste Y-Skala bei Gesten, Fling/Stop, Bereichsgrenzen und keine verdeckten Punktwerte unter der inneren Skala.

[Synthetische Monatsvorschau](previews/statistics.png) · [Wochenvorschau](previews/statistics-week.png), beide nativ gerendert und visuell geprüft. Keine privaten Fahrten/Cloudaufrufe, keine Gesamtsuite und kein erneuter Telefon-/APK-Test. Die früheren Menü-/Navigationsprüfungen bleiben bestehen. Noch nicht in der zuletzt veröffentlichten APK0.8.55/Build96.


Veröffentlichung28.10.2026: Diese Änderungen sind in **APK0.8.56/Build97** enthalten. [Buildnachweis und Download](build-97.md). Frühere Aussagen zu fehlender neuer APK sind Umsetzungshistorie.


Veröffentlichung28.10.2026: Die zuletzt nach Build98 ergänzten Änderungen sind in **APK0.8.58/Build99** enthalten. [Buildnachweis und Download](build-99.md). Frühere Aussagen zu fehlender neuer APK sind Umsetzungshistorie.
