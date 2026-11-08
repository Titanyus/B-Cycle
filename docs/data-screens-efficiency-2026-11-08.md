# Datenreiche Einstellungs- und Detailseiten: Effizienzprüfung08.11.2026

Nutzerauftrag als Ergänzung zum [allgemeinen Durchgang](efficiency-review-2026-11-08.md): insbesondere Streckendetails mit vielen Diagrammen, HRV-Auswertung, Statistiken und Trainingsansichten statisch prüfen und unnötige Arbeit verringern. Ohne Profiling/Benchmarks, nur gezielte Funktionsregressionen. Keine neue APK beauftragt.

## Streckendetails

`RouteChartView` bereitete bisher bei jeder Größenänderung sämtliche Kurven bereits beim Layout vor, einschließlich noch nicht gezeichneter Diagramme. Jetzt markiert Layout nur die Geometrie als geändert; Glättung, Tangenten und Pfade entstehen erst beim ersten Zeichnen dieser View und nach tatsächlicher Größenänderung. Kleine View-/Layoutobjekte entstehen weiterhin regulär. Kein Versprechen einer vollständig virtualisierten Scrollseite; Android kann selbst Zeicheninhalte vorbereiten.

Die unveränderte Gesamtskala wird einmal je Datenreihe bestimmt statt bei jeder Größenänderung. Pan/Zoom verwendet weiterhin vorhandene Pfade und eine Canvas-Transformation. Die gemeinsame `RouteChartNavigation` hält nun auch X-Achsenwerte und ihre formatierten Labels nach Achsenende/Entfernungs- oder Zeitachse/Viewport vor; die5 Diagramme formatieren denselben Ausschnitt nicht mehr separat. Endwerte, runde Rasterwerte, Mitscrollen unter den Rand und Synchronisation unverändert. Keine neue Interpretation der Fahrtdaten oder Glättungsparameter.

Geprüft: Layout allein bereitet Kurven nicht vor; Zeichnen eines Diagramms lässt die anderen Geometrien unvorbereitet. Gemeinsamer Labelsnapshot bis zum nächsten Viewport, feste Y-Skalen, wiederverwendete Pfade bei Pan/Zoom, Pinch/Pointerwechsel/Inertie/Accessibility/Detach, echte Datenlücken und Randbeschriftung.

## Detaillierte HRV-Auswertung

`HrvTrendChart.onDraw` lief bisher mehrfach durch die gesamte Historie: sichtbare Balken filtern, jeden Korridorabschnitt aufbauen, Mittelwerte prüfen und sämtliche Beschriftungen messen. Jetzt werden Kalenderindizes beim Datensatzwechsel vorbereitet. Für sortierte Daten begrenzen binäre Bereichsgrenzen die Zeichenarbeit auf das sichtbare Fenster. Je ein Nachbar außerhalb der Ränder erhält übergreifende Korridor-/Mittelwertsegmente. Balkenbeschriftungen berücksichtigen ihre eigene Breite, damit angeschnittene Labels weiterhin in den Bildrand reichen. Ein kleiner numerischer Puffer berücksichtigt Floatprojektion. Unsortierte Eingangsdaten behalten den bisherigen vollständigen Durchlauf statt stillschweigend umgeordnet/abgeschnitten zu werden.

Y-Achsenbeschriftungen, Balkentext/-breiten und gestrichelter Linieneffekt werden beim Datenwechsel statt pro Zeichenframe vorbereitet. Die7 Kennzahlentexte in HrvTrendsView formatieren ihren Zahlenwert nur einmal für sichtbaren Text und Accessibility.

Korridor-/Mittelwertberechnung nutzt unverändert die **vollständige** Historie; nur das Zeichnen wird eingegrenzt. Tages-/Wochen-/Monatsaggregationen, persönliche Referenzen, Auswahl und Farben unverändert. Neue Indexdaten gehören zur aktuellen Chartinstanz, kein global wachsender Cache.

Geprüft: Darstellung aus1000Tagen und Tages-/Wochen-/Monatsaggregation pixelgenau gegen denselben vollständigen Zeichendurchlauf verglichen, einschließlich Korridor, Mittelwert, Beschriftung, Lücke und Randsegmenten. Der Tagesframe greift nur auf die benötigten Punkte zu. Bestehende unabhängige Skalen je Auflösung und ihre Stabilität während Navigation erhalten.

## Training: Liste und Details

`TrainingWorkoutChart` gruppierte Ziele, bestimmte Maxima/FTP-Bezüge/Watt-Skala und rechnete Rampenanfang/-ende bei jedem Zeichnen neu aus. Diese Zielwerte und funktionalen Blockfarben werden jetzt einmal beim ersten Zeichnen der jeweiligen Workout-/Fahrerprofil-Kombination vorbereitet und wiederverwendet. Neues Workout oder geändertes Fahrerprofil verwirft das Modell; identisches erneutes Binden nicht. Vor dem Zeichnen keine Zielaufbereitung durch die Chart. Dimensionen, Canvaspfade und Achsenpositionen bleiben an die tatsächliche Viewgröße gebunden.

Gilt sowohl für Listendiagramme als auch die Detailansicht mit Watt-/Zeitachsen. Keine Trainingsmetrik-/Zonengrenzen-/Rampenänderung. Vorhandene32er-Paginierung der Trainingsabschnitte und nach Bedarf aufklappbare/paginierte Original-JSON-Felder bereits sinnvoll; unverändert.

Geprüft: zweites Zeichnen liest keine Trainingsziele erneut und liefert dasselbe Bild; Profil-/Workoutwechsel invalidiert. Bestehende Watt-/FTP-Höhen, Achsenendwerte und native Intervals-Rampe erhalten. Die Validierung des Workout-Datenmodells ist unabhängig davon und bleibt bestehen.

## Statistiken / Fitness

- Klassische RideStatisticsView überspringt bereits dieselbe Ergebnisinstanz; StatisticsDistanceChart hält Pfade und Zahlenlabels außerhalb von onDraw vor. Kein zusätzlicher Chart-/Workerumbau.
- Verteilungsdiagramme in den Streckendetails bereiten Balken/Labels anhand ihrer Größe vor; beim Scrollen keine erneute Verteilungsberechnung gefunden. Belassen.
- Fitnesschart bekam beim normalen Cachetreffer aus IntervalsFitnessView dasselbe Objekt zweimal: zunächst Cachevorschau, danach identisches fertiges Ergebnis. `IntervalsFitnessChart.show` ignoriert nun dasselbe bereits gezeigte Objekt und spart einen erneuten Index-/Kurvenaufbau.
- Bereits veraltete, im seriellen Fitnessworker noch wartende Ladeaufträge werden **vor** ihrem Cache-/Dateizugriff verworfen. Bestehende Konto-/Generations-/Abbruchprüfungen für Downloads bleiben.
- Aktuelle Fitnessfelder/Datumswahl,7/30/90-Tage-Fenster und Quellen unverändert. Das ist kein regelmäßiger Poller und kein Anlass für neue Netzwerkintervalle.

Geprüft: identisches Ergebnis hält dieselben Kurven; Scrollen, Tagesauswahl und Zeichnen in beiden Designs sowie Statistik ohne lokale Fahrten bestehen weiter.

## Grenzen und Folgearbeiten

Native Detailkarte und Vollbild-/GL-Lebenszyklus bleiben unverändert. Keine neue MapView, zusätzliche Datenbank, zusätzlichen Sensorabos oder Threads. Bestehende Worker liefern die Fahrtdaten weiterhin einmal an die Detailseite. Keine Rohdatenausdünnung, Messfrequenzänderung oder geänderte Bewertung.

Noch mögliche größere Schritte: komplexe Seiten vollständig virtualisieren oder reine, größenunabhängige Aufbereitung stärker auf Worker vorziehen. Dafür müssten Scrollposition, Accessibility und Abbruch beim Bildschirmwechsel eigens abgesichert werden; kein pauschaler View-/Recycler-Umbau in diesem Durchgang. Die bereits priorisierten allgemeinen Lebenszyklus-Themen stehen weiterhin in TODO.md.

## Prüfnachweis

14 unterschiedliche gezielte Fälle im aktuellen Stand erfolgreich: RouteChartNavigationTest6; HRV-Zeichenvergleich und stabile Auflösungsskalen2; TrainingChartAxesTest4; bestehende native Rampendarstellung1; Fitness-Statistik-/Navigationsfall1. Keine Gesamtsuite und keine Messkampagne.

- `/workspace/.tools/data-screens-tests.log`: erste13 Fälle erfolgreich; ein neuer Cachetest zählte versehentlich die vorgelagerte Workout-Konstruktorvalidierung mit. Testeingrenzung korrigiert.
- `/workspace/.tools/data-screens-cache-tests.log`: betroffene Trainingsfälle plus ergänzte Fitness-Cacheprüfung5/5 erfolgreich. Keine weiteren Produktänderungen nach dem ersten Testlauf.
- Zentrale Typografie-/UI-Katalog-/Bright-Generator-/Diff-Prüfungen.

Der Punktzugriffstest prüft die Eingrenzung der Arbeit und vergleicht Bildresultate; keine Laufzeit-/CPU-/Akkumessung. Bediengefühl und Darstellung auf dem Smartphone bleiben die praktische Abschlusskontrolle.
