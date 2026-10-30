# Gemeinsame Diagramm-Navigation in den Streckendetails

Tempo, Kadenz, Leistung, Puls und Höhe teilen eine RouteChartNavigation mit normalisiertem X-Ausschnitt. Anfangs gesamte Fahrt, stufenloser X-Zoom bis64×; Distanz-/Zeitachse bleibt wie in der Analyse gewählt. Horizontales Verschieben erst nach Vergrößern, begrenzt auf den vollständigen Datenbereich. Y-Skalen bleiben stabil. Zonenverteilungen und Karte nicht Teil dieser Steuerung.

Zwei-Finger-Pinch über direkte Distanz der Pointer-IDs wie HrvTrendChart, kein Android-Mindestabstand. Fingerwechsel startet nur die Referenzdistanz neu, keine Sprünge. Native Quick-Scale erhalten. Horizontales Schieben mit GestureDetector und OverScroller mit derselben Standardträgheit wie HRV; eine gemeinsame Bewegung für alle Diagramme. Neue Berührung auch auf einem anderen Diagramm, Rand, CANCEL, Entfernen oder Schließen beenden den Nachlauf. Vertikales Seitenscrollen bleibt möglich, horizontale/Pinch-Gesten sperren das Abfangen nur während der Geste.

Vorbereitete Kurvenpfade bleiben zwischengespeichert, nur die X-Transformation ändert sich. Kurven clippen auf die Zeichenfläche, Beschriftungen/Grid bleiben im Plotrahmen; fünf passende X-Werte pro Ansicht. Keine Datei-/FIT-Dekodierung, Y-Neuskalierung oder Kurvenanalyse pro Bewegungsframe. Ausreichende Darstellung hängt weiterhin von den begrenzten vorbereiteten Analysedaten ab. Accessibility-Aktionen für gemeinsamen Zoom und Seitenschritt ohne neue sichtbare Buttons.

## Kadenzdarstellung

Fehler in AnalysisSeriesBuffer: Ein einziger fehlender Wert in einer Verdichtungsgruppe trennte bisher alle ausgewählten Werte dieser Gruppe künstlich voneinander. Gerade Kadenz mit häufigen echten Tretpausen erschien dadurch lichter als die Leistung. Die Verdichtung behält jetzt pro tatsächlichem zusammenhängendem Abschnitt Endpunkte und Extrema, mit Lücken nur zwischen den echten Abschnitten. Keine Umdeutung fehlender Kadenz in0 oder erfundene Tretwerte; Rohdaten, Kennzahlen, FIT und Leistungsauswertung unverändert.

Maximal4096 Diagrammpunkte bleiben erhalten. Extrem fragmentierte Daten werden bei Bedarf durch Ausdünnen ganzer Abschnitte begrenzt; globale Extrema/Anfang/Ende bleiben erhalten, keine zusätzlichen Löcher innerhalb eines behaltenen Abschnitts. Regression mit16000 Messpunkten und80 Tretabschnitten erhält sämtliche Abschnittsgrenzen, außerdem vorhandener100000-Punkte-/Peakt-Test.

## Prüfungen

RouteChartNavigationTest: enge Finger bis15px, 3.Pointer/Ersatz, synchroner Ausschnitt aller5 Diagramme, Distanz/Zeit, unveränderte Y-Skalen/Rohdaten, Nachlauf und Stopp durch anderen Chart, Grenzen/Vollansicht/vertikales Scrollen, Detach/Accessibility sowie native Pixelprüfung von gezoomter Fläche/Lücken/Clipping. RouteAnalysis-/Kurven-/Skalen-/native Detailtests und Filtertests erfolgreich. Alte Detailtests an bereits eingeführte2-dp-Trenner, GPS-lose Höhenzeilen und Sichtbarkeitsänderung durch das Auge angepasst; keine Produktänderung zur Erfüllung alter Tests.

Logs `/workspace/.tools/route-chart-filter-final-tests.log` (41Tests) und `/workspace/.tools/route-chart-pixels-final-tests.log` (4Tests). Keine vollständige Gesamtsuite, kein physischer Gerätetest und kein Vergleich mit dem Original der abgebildeten61,5-km-Fahrt (nur Screenshot bereitgestellt).
