# Dashboard: gezielte Effizienzkorrekturen09.11.2026

Nutzerauftrag nach Bewertung einer externen Optimierungsspezifikation: ausschließlich Dashboard-Aufbereitung hinter vollständig deckenden Seiten aussetzen, Graph-Nachfrage wiederverwenden und einzelne Anzeigebereiche nur bei relevanten Änderungen erneuern. Keine Sensor-/GPS-Abfrageraten, WakeLocks, Aufzeichnungsdaten oder neuen Kartenlimits ändern. Statische Prüfung und gezielte Funktionsregressionen, kein Profiling/Benchmark und keine neue APK beauftragt.

## Vollbildseiten und kompakte Dialoge

DialogNavigation kennt zusätzlich zur bisherigen Dialogliste die vollständig deckenden Seiten (derselbe fullscreen/overlay-Vertrag des bestehenden BicycleDialogBuilder). Ein kleiner Dialog allein verdeckt das Dashboard nicht vollständig; über einer Einstellungsseite bleibt es verdeckt. Show/Dismiss meldet den neuen Zustand nach dem tatsächlichen Android-Dialogereignis an MainActivity. Bei Rückkehr einmal den aktuellen Zustand anzeigen, ohne auf ein Sensorpaket oder das Sparintervall zu warten. Elternseiten, Cleanup und Rückkehr zum Dashboard bleiben erhalten; beim Stop/Destroy keine neue Darstellung aus Dialogcallbacks.

MainActivity trennt den Darstellungsblock von weiterhin notwendigen Aktionen: hinter vollständig deckenden Seiten keine GPS-Anzeigeaufbereitung, Trainings-/Zahlen-/Trend-/Graph-Kachelupdates, Bosch-Glow-Updates oder MapPanel.refresh. Live-Graphdarstellung ausgeblendet, Historie läuft weiter und wird bei Rückkehr aktualisiert. Sichtbare Sensor-/Strecken-/Statistikseiten behalten ihre eigenen Refreshpfade. Speicherstatus, Fehlerhinweise, Fahrtabschluss/Zusammenfassung, Warnungslogik, Profilwechsel und Bildschirmwachhaltung werden weiterhin verarbeitet. Die kurzen zeitgewichteten Trainings-Anzeigefenster laufen bewusst weiter, damit Rückkehr keine fiktive Messlücke oder geänderte Glättung erzeugt.

Kein Umbau am GL-/MapView-Lebenszyklus: native Gesten, Tiles, verbleibende Animationen und notwendige Engineframes bleiben unabhängig. Das ist kein pauschales Anhalten jeder Aktivität im Hintergrund. Profilwechsel darf die notwendige neue Dashboard-Konfiguration weiterhin einmal aufbauen.

Auch im Sparmodus betrifft die Abdeckung nur den Darstellungsblock. Kleine Dialoge über dem Dashboard behalten sichtbare Livewerte/Graphen mit dem vorhandenen1-Hz-Anzeigelimit; wichtige Statusänderungen weiter sofort. Deckende Verwaltungsseiten werden weiterhin direkt aktualisiert.

## Diagrammkonfiguration

DashboardGraphDemand hält genau einen Snapshot nach vollständigem GraphOverlayConfig und DashboardTemplate. Gleichwertige unveränderliche Konfigurationen liefern dieselbe Map zurück; andere Metriken, Fenster, Layoutfähigkeiten und leere Auswahl werden berücksichtigt. Keine zusammengesetzte String-/Hashsignatur und kein zusätzlicher unbegrenzter Profilcache. Die vorhandene Provider-Invalidierung und Rohdatenhistorie bleiben erhalten.

## Einzelne Dashboardbereiche

DashboardView bindet Zahlen nur für die tatsächlich im Template enthaltenen Slots. Jede Kachel merkt ihren eigenen Rohwert-/Quelleneingang und die Anzeigeabhängigkeiten (Feld, Indoorquelle, Modusnamen, Höhenreferenz, Sichtbarkeit, Trendrichtung, Graphauswahl, Locale). Beispielsweise verursacht ein Leistungspaket oder eine fortlaufende GPS-Zeit keine neue Geschwindigkeitsformatierung, solange sich deren Eingang nicht ändert. GPS-Rohwertprojektion und Formatierung nutzen dieselbe Zuordnung; fehlende Werte und Zeitfelder bleiben echte Änderungen. Keine neue Rundungs-/Glättungsregel und keine zentrale Sperre anhand nur von Puls/Leistung/Tempo.

Unveränderte Graphkonfiguration plus derselbe unveränderliche Graphframe werden nicht erneut an die Overlays gebunden; gleiche Trainingskonfiguration/-snapshots nicht erneut auf alle Glows verteilt. Ein Designwechsel invalidiert den Trainingsfarbzustand ausdrücklich, auch bei identischen Werten. Nach Trendreset und neuem Template neue Kacheln frisch aufbauen. Graphframes, Feldeditor, Akku-Sammelfelder und Accessibility bleiben unabhängig aktualisierbar. Kleine Korrektur innerhalb dieses Cachepfads: Graph-Updates dürfen ausgeblendete Zahlenzeilen eines Akku-Sammelfelds nicht wieder sichtbar setzen.

## Gezielte Prüfung

16 unterschiedliche relevante Fälle erfolgreich, keine Gesamtsuite:

- Neue DashboardEfficiencyTest-Fälle: exakter Graph-Cache/Template/Fenster/Metrikwechsel; irrelevante Pakete und relevante Feld-/Quellen-/Zeit-/Sichtbarkeitsänderungen; verschachtelte Vollbild-/Overlayseiten, Normal-/Sparmodus, Graph-Sichtbarkeit und sofortige Rückkehr; Speichern/Fehler/Zusammenfassung trotz verdecktem Dashboard; identischer Trainingssnapshot bei Flat/Glow-Wechsel.
- Bestehende RecordingUpdateUiTest-Fälle3, DisplayUpdateGate und Graphhistorie/Sparintervalle2, Trendänderung ohne sonstigen Feldwechsel1, echter Feld-/Graph-/Training-Editor1, sichtbare gemeinsame Schriftgrößen1, Akku-Sammelfelder1, Seiten-/Kartenlayoutwechsel1, nativer Flat-/Glow-Diagrammvergleich1.
- Logs unter /workspace/.tools: dashboard-efficiency-final-tests.log (11/12; neue Testannahme zum absichtlichen Schließen der Vollbildseite beim Fahrtabschluss korrigiert), dashboard-efficiency-coverage-tests.log (5/6; Graphprüfung musste zunächst das reale asynchrone OnShow abwarten), dashboard-efficiency-graph-coverage-test.log (korrigierter Fall grün), dashboard-efficiency-appearance-tests.log (Vollbild-/Speicherpfade grün; unmittelbare zusätzliche Farbumrechnung beim Themewechsel verletzte den bestehenden Pixelvergleich), dashboard-efficiency-appearance-final-tests.log (bestehender Pixelvergleich und Kachelfall grün nach expliziter Cacheinvalidierung statt zusätzlicher Umrechnung), dashboard-efficiency-theme-cache-test.log (ergänzter Theme-Cachefall grün).
- Erster Testcompile hatte einen fehlenden isPush-Parameter im synthetischen MCSP-Testpaket; Testfixture korrigiert, keine Produkt-/Protokolländerung.
- Zentrale Typografie/UI-Katalog/Bright/Diff-Prüfungen bestanden. Keine Prozentangaben zur Akkuersparnis; Bediengefühl auf echtem Gerät bleibt praktische Kontrolle.
