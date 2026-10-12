# Estimated FTP und Streckendetails-Update

## Zentrale Berechnung

`PowerDurationWindow` integriert tatsächlich gültige, zeitlich zusammenhängende Leistungsintervalle. Das Fenster der Performance Engine ist exakt 1.200.000 ms lang. Bei wechselnden Leistungen werden sowohl eingehende Intervallgrenzen als auch um 20 Minuten verschobene ausgehende Grenzen ausgewertet. Das Maximum ist deshalb nicht auf ganze Sekunden oder die Anzahl der Sensorpakete beschränkt. Null Watt zählen als echte Leistung; unbekannte Leistung und Aufzeichnungspausen unterbrechen die Kontinuität. Ein vorheriges gültiges Maximum bleibt erhalten, aber getrennte Abschnitte werden nie zusammengeschoben.

`PerformanceEngine` liefert `estimatedFtpW = bestAverageW × 0,95`. Es gilt die bereits vorhandene Sensorvalidierung: normale Messwerte höchstens 5 s halten; bestätigte Bosch-Nullzustände dürfen bei frischer E-Bike-Telemetrie weitergelten. Ein auswertbares Fenster benötigt vollständige gültige Intervallabdeckung. Weniger als 20 Minuten zusammenhängende Abdeckung liefert keinen Wert. Die Qualität wird am jeweiligen Kandidatenfenster bewertet; ungültige Abschnitte anderswo in der Fahrt löschen nicht ein zuvor vollständig gültiges 20-Minuten-Fenster.

Der zeitlich begrenzte Intervallpuffer hängt von der Fensterlänge ab, nicht von der gesamten Fahrtdauer. Gleiche benachbarte Leistungen werden zusammengefasst. Die interne Fensterkomponente nimmt eine Dauer entgegen und kann später weitere Power-Duration-Fenster tragen; jetzt ist ausschließlich das 20-Minuten-Fenster aktiv. Kein zusätzlicher Live-Graph und keine Einstellungsseite.

Das Ergebnis ist eine **Schätzung dieser Fahrt**, kein bestätigter persönlicher FTP. Ohne maximale 20-Minuten-Belastung kann es deutlich zu niedrig sein. Die Kennzahl wird nicht in ein Fahrerprofil übernommen. Diese Einschränkung steht auch in der zugänglichen Kachelbeschreibung.

## FIT

`ESTIMATED_FTP` ist ein unabhängig auswählbares Summary-Feld, standardmäßig ausgeschaltet. Die vorhandene Feldmengen-Speicherung persistiert die Auswahl; die Checkboxbezeichnung liegt in `strings.xml`.

Nur bei aktiver Auswahl und gültigem Ergebnis schreibt `FitWriter` einen Session-Developer-Field-Wert, Einheit W, float64, mit dem vorhandenen B|Cycle-App-Identifier. Kein Record- oder Lap-FTP und keine künstlichen Sekundenwerte. Vorhandene Felder, Qualitätsdaten und Developer-Indizes bleiben erhalten. Der native FIT-Validator prüft den Export unabhängig.

`RouteAnalysisRead` bevorzugt den gespeicherten bekannten B|Cycle-Session-Wert. Fehlt er, berechnet die vorhandene Engine ihn aus geeigneten Fahrerleistungs-Rohdaten. Anzeige und Exportauswahl bleiben unabhängig. Originaldateien werden weder geändert noch nachträglich ergänzt.

## Daten und UI

Reihenfolge: Ride Overview, E-Bike, Power, Cadence & Heart Rate, Energy, Performance, Map, Charts. Diagrammreihenfolge bleibt Speed, Cadence, Rider Power, Heart Rate, Elevation.

Header: bestehende globale 64-dp-Komponente, nur Streckenname und Zurück. Datum/Uhrzeit stehen in einer kleinen zentrierten Systemfont-Zeile direkt darunter im Scrollinhalt. Lange Namen werden gekürzt beziehungsweise innerhalb der vorhandenen Schriftgrenzen angepasst.

Kennzahlen stehen ohne Einzelkarten direkt auf SECONDARY. Zurückhaltende vertikale FOREGROUND-Divider trennen die Spalten. Haupticons bleiben 32 dp und FOREGROUND, Werte Bebas Neue, Beschriftungen Systemfont. Einheiten sind separate kleine TextViews, vertikal mittig zum Zahlenfeld und direkt hinter den tatsächlichen Glyphen; die gesamte Zahl-Einheit-Kombination ist zentriert. Das exakte bisherige Dashboard-Grau `RGB(163,163,163)` ist nun gemeinsam in `BicycleDesign.dashboardUnitColor` definiert und wird auch vom Hauptdashboard verwendet. Keine neue Graufarbe, keine Baseline-Spans. Geometrie ändert sich nicht beim Themewechsel.

Die Performance-Gruppe ordnet Estimated FTP, Efficiency Factor, Speed-to-Power Ratio in drei Spalten an, soweit Werte vorliegen. Es gibt keine eigene Best-20-min-Kachel. Power zeigt Average, Max und Normalized Power gleichzeitig; fehlende Teilwerte erscheinen als zentraler Gedankenstrich, statt die Gruppe auf eine unvollständige Einzelkennzahl zu reduzieren. Vollständig fehlende Gruppen bleiben verborgen.

### E-Bike

`RideAnalysisMeans` ist eine read-only Berechnungskomponente außerhalb der UI:

- Motorleistung: zeitgewichteter Durchschnitt tatsächlich verfügbarer Motorleistungsintervalle; Nullwerte bleiben gültig.
- Rider/Bike Ratio: Integral der Fahrerleistung geteilt durch Integral der Motorleistung über **dieselben gültigen gepaarten Intervalle**. Darstellung als Verhältniszahl, keine Prozentzahl. Ohne positive Motorenergie kein Verhältnis. Dadurch erzeugen unterschiedliche Sensorlücken kein irreführendes Verhältnis separat ausgewerteter Zeiträume.
- Battery Usage: Anfangsstand minus Endstand in Prozentpunkten der Akkuladung. Anfangs-/Endmessung müssen innerhalb jeweils 60 s des aufgezeichneten Zeitraums liegen. Ohne beide Messungen, bei fehlender Zeitzuordnung oder erkanntem Anstieg/Nachladen wird kein Verbrauch erfunden. Ein Anstieg wird konservativ als nicht eindeutig auswertbar behandelt.

Intervalle über 5 s und fehlende Werte werden nicht beliebig fortgeführt. Der Fahrerleistungsdurchschnitt ist auch ohne gespeichertes Session-Average aus geeigneten Rohdaten verfügbar. Native gültige FIT-Summary-Werte bleiben bevorzugt. NP/Energie/FTP kommen weiterhin aus gespeicherten Werten oder der bestehenden zentralen Performance-/Energy-Engine.

### Vollbildkarte

Die eigentliche Ursache der leeren Kartenansicht war neben dem bereits abgesicherten View-Lifecycle die automatische ScrollView-Hülle des Vollbild-Hosts: eine reine Kartenfläche bekam dadurch keine nutzbare Höhe. `BicycleFullscreen` nimmt die Detailkarte jetzt direkt als gewichtete Inhaltsfläche auf. Track-Layer, Quelle und Tages-/Nachteinstellung bleiben identisch. Native Gesten erlauben Verschieben/Pinch-Zoom; lokale Lucide-Buttons erlauben Zoom hinein/hinaus und erneutes Einpassen der Strecke. Schließen kehrt zu derselben Detailseite zurück. Keine Änderung an der Dashboard-Kartensteuerung.

## Prüfungen

Neue Tests: exakte Zeitgewichtung und verschobene Fenstergrenzen, echte Nullen, zu kurze Abschnitte, Datenlücken, Pausen, fremde Leistungsquellen, abgelaufene Messwerte, Session-only-Export, Garmin-Validator, Standardauswahl aus, gespeicherter Wert und unabhängige Rohdatenberechnung. Bestehende Checkbox-/Persistenztests prüfen zusätzlich das neue Feld.

Daten-/UI-Tests: native Motor-/Akkudaten, gepaarte Verhältnisse, fehlende Randdaten/Nachladen, neue Gruppenreihenfolge, Power-Platzhalter, keine Einzelhintergründe, vertikale Divider, tatsächliche Zahl-/Einheitenzentren und das identische Dashboard-Grau bei schmalen Feldern und 1,5-facher Schriftgröße. Der Vollbildkartentest prüft nutzbare Höhe, Track-Layer, Zoomen, Verschieben, Einpassen und Freigabe beim Schließen. Bestehende Theme-, Diagramm- und Navigationstests bleiben erhalten.

Vollständige Testsuite und Release-Build erfolgreich am 12.10.2026 in **1m 15s**: **259 Tests, 0 Fehler, 0 Fehlschläge, 0 übersprungen**. Release-Lint-Vital erfolgreich. Nur die bekannte Produktionswarnung zu `onBackPressed()` bleibt. Kein zusätzlicher vollständiger Lint-Lauf. Auf anschließenden Nutzerwunsch als signierte Test-APK 0.7.4 bereitgestellt. Reale Karten-Netzdownloads und physische Gerätebedienung bleiben praktisch zu prüfen.
