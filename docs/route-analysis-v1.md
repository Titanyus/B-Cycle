# Streckendetails V1

## Integration und Gestaltung

Ein normaler Tap auf den gesamten Streckeneintrag öffnet die Details. Die Zuordnung verwendet die unveränderte Strecken-ID, nicht Name oder Listenposition. Bestehende Karten-, Cloud-, Speicher- und Menüaktionen bleiben separate Klickziele mit derselben Geschäftslogik. Die Detailansicht nutzt `BicycleDialogBuilder`, `BicycleFullscreen`, `DialogNavigation`, `BicycleComponents`, `BicycleDesign`, `ThemeManager` und den semantischen Lucide-Katalog. Schließen, Header-Zurück und Android-Zurück kehren zum erhaltenen Manager zurück; Filter und Listenobjekt bleiben erhalten.

Fester Header mit gekürztem Streckennamen und Datum, ein vertikaler Scrollbereich, bestehender fester Footer. Bei FIT und zeitgestempeltem GPX zeigt die Detailansicht das Datum der Aufzeichnung statt des Importdatums. Der native Android-Footer kann intern seinen eigenen Overflow-Scrollcontainer besitzen; es gibt keine verschachtelten Scrollbereiche im Analyseinhalt.

Die Kennzahlengruppen stehen untereinander: Ride Overview (maximal vier Kacheln), Power (drei), Cadence & Heart Rate, Energy und Performance (je zwei). Fehlende Kennzahlen entfallen zentral; leere Gruppen hinterlassen keine Abstände. Vorhandene Standardwerte haben Vorrang. Alternative Zeit-, Speed-, Höhen-, Kadenz- und Herzfrequenzwerte wechseln innerhalb derselben Kachel, sofern vorhanden. Wert, Einheit, Beschriftung und zugängliche Aktionsbeschreibung wechseln zusammen. Der Zustand bleibt lokal.

32-dp-Lucide-Icons stehen zentriert über den Werten und verwenden FOREGROUND. Werte verwenden Bebas Neue, kleine Einheiten stehen auf derselben Grundlinie; Beschriftungen nutzen den Systemfont. Neue Geometrie-Tokens stehen im vorhandenen `BicycleDesign`. Alle fünf Theme-Rollen bleiben verfügbar; keine Theme-Abhängigkeit der Geometrie. Standardtexte und Kennzahlen-Icons werden nicht beliebig mit ACCENT eingefärbt.

## Gemeinsame Datenquelle

`MapState.loadAnalysis` verwendet den vorhandenen Strecken-Worker und lädt anhand der ID die private Originaldatei. Es verändert weder Auswahl noch Indizes, Sensorzustand oder Dateiinhalte. Beim Verlassen kann die Anfrage abgebrochen werden; verspätete Ergebnisse werden nicht mehr in die Ansicht eingesetzt.

`RouteAnalysisRead` bereitet Metadaten, verfügbare Kennzahlen, Geometrie und alle fünf Zeitreihen zusammen vor. FIT wird mit Garmins vorhandenem Streaming-Decoder einmal für sämtliche Abschnitte decodiert; davor erfolgt die vorhandene Integritätsprüfung. Es gibt kein separates Einlesen je Diagramm. GPX nutzt den bestehenden Streaming-Parser, der über einen optionalen Beobachter die ursprünglichen Höhen-/Zeitpunkte vor der Kartenreduktion liefert. Bestehende Aufrufer und importierte Dateiinhalte bleiben unverändert.

Native FIT-Session-Werte werden bevorzugt, Lap-Werte nur ohne Session. Bei mehreren Abschnitten werden vollständige Summen und Maxima zusammengeführt; Durchschnittswerte nur mit vollständiger Zeitgewichtung. NP und Performance-Verhältnisse werden nicht aus Lap-Verhältnissen gemittelt. `total_work` (J) wird als kJ dargestellt, `total_calories` bleibt kcal. Bekannte B|Cycle-Developer-Fields werden über App-UUID, Namen und Einheit geprüft, insbesondere `EFFICIENCY_FACTOR` und `SPEED_POWER_RATIO`; unbekannte Felder werden nicht als gleichnamige B|Cycle-Kennzahl interpretiert.

Fahrerleistung kommt aus dem nativen `power` oder dem bekannten `RIDER_POWER`, niemals `motor_power`. Positive Kadenzwerte verwenden `PedalingAverage`; Nullwerte bleiben in den Diagrammdaten. Native Durchschnitts-Kadenz aus Fremddateien wird nicht ungeprüft übernommen, da sie Pedalierpausen enthalten kann. Bewegungszeit nutzt `MovingTime`; Höhenänderungen nutzen die bestehende 3-m-Hysterese von `RouteElevation` (für Höhenverlust über gespiegelte Höhen). Fehlende Performance-/Energiewerte dürfen ausschließlich aus der vorhandenen `PerformanceEngine`/`EnergyEngine` stammen. Bei unzureichender Qualität bleiben sie fehlend. Es gibt keine neue Formel in der UI. Eine unbekannte Geschwindigkeitsquelle wird nicht als Beleg für frische Bosch-Telemetrie verwendet.

## Karte und Diagramme

Die nicht interaktive Vorschau verwendet vorhandene Mapsforge-Technik: `MapSource`, `MapNightLayer`, `ViewportTileCache` und `RouteTrackLayer`. Sie passt die ausgewählte Geometrie ein, übernimmt Quelle und Tages-/Nachteinstellung, aber verändert die Dashboard-Kamera oder angezeigte Strecke nicht. Kartenquellen bleiben sichtbar. Vollbild bietet normale Kartenbedienung und kehrt zu denselben Details zurück. Der Detail-Kachelspeicher ist auf den sichtbaren Bereich begrenzt und hält keinen zweiten persistenten Downloadbestand. Damit braucht die Detailkarte ohne RAM-Kacheln Netz; sie ist keine neue Offline-Kartenfunktion. Verborgene Karten pausieren, geschlossene Karten geben ihre Ressourcen frei.

Die feste Reihenfolge lautet Speed (Gelb/Orange), Cadence (Grün), Rider Power (Blau), Heart Rate (Rot), Elevation (Neutralgrau). Nicht verfügbare Diagramme entfallen. Die semantischen Kurvenfarben sind unabhängig vom Theme; ihre Helligkeit wird zentral für mindestens 3:1 Flächenkontrast gewählt, auch für zukünftige Custom-Paletten. Achsen und Beschriftungen verwenden FOREGROUND mit reduzierter Deckkraft.

Native `Canvas`/`Path`, keine Chart-Bibliothek. Gleiche Breite, 120-dp-Höhe und Zeichenränder; kleine Systemfont-Bezeichnungen links um 90 Grad gedreht. Keine Überschriften oder Messwertblöcke neben den Kurven. Einheitliche Distanzachse in km für sämtliche Reihen; bei ungeeigneter Distanz konsistente Zeitachse in Minuten. Fehlende Distanzzuordnung erzeugt eine Lücke. Zeitsprünge und fehlende Sensorwerte werden markiert; echte Nullwerte bleiben Werte. Keine ungeprüfte Interpolation.

Pro Zeitreihe werden höchstens 4096 vorbereitete Punkte gehalten. Die fortlaufende Reduktion erhält Endpunkte, Minima/Maxima und Lückenmarker; bei stark fragmentierten Daten werden Verbindungen konservativ getrennt. Verlorene Zwischenpunkte sind ausschließlich Darstellungsreduktion, kein Eingriff in die FIT-Rohdaten. Der gemeinsame Achsenbereich bleibt unabhängig von Datenlücken. Pfade und Ticktexte werden bei Größenänderungen vorbereitet; beim Scrollen werden nur bestehende Pfade gezeichnet.

## Ressourcen und Dateien

Bestehend war nur eine kleine `res/values/strings.xml`, ohne weitere Sprachordner. Sämtliche neuen sichtbaren Bezeichnungen, Einheiten, Lade-/Fehlertexte, Buttons, Datums-/Zahlenformate und Accessibility-Texte sind dort zentral ergänzt. Bestehende Ressourcen wurden erhalten. Keine vollständige Übersetzung der alten App.

Neu:
- `RouteAnalysis.kt`: read-only Datenmodell, semantische Kennzahlen/Kurvenfarben und begrenzte Datenvorbereitung.
- `RouteAnalysisView.kt`: Kennzahlen, lokales Umschalten, Header und Scrollinhalt.
- `RouteChartView.kt`: kompakte Diagramme.
- `RouteAnalysisMap.kt`: streckenbezogene Vorschau/Vollbildkarte.
- `RouteAnalysisTest.kt`, `RouteAnalysisViewTest.kt`: Daten- und UI-Tests.
- Dieses Dokument sowie fünf lokale Lucide-Vektoren (`gauge`, `zap`, `heart`, `flame`, `repeat-2`).

Geändert:
- `MainActivity.kt`, `RouteManagerView.kt`: Einstieg, Navigation, erhaltene Aktionsziele.
- `MapState.kt`: read-only Anfrage auf dem bestehenden Worker.
- `GpxImport.kt`: optionaler Datenbeobachter ohne Änderung des bisherigen Importergebnisses.
- `BicycleDesign.kt`, `BicycleDialog.kt`, `BicycleIcons.kt`: gemeinsame Tokens, Erhaltung der Detailtypografie, zusätzliche semantische Icons.
- `res/values/strings.xml`, `scripts/import_lucide.py`: neue Ressourcen und erlaubte lokale Icons.

Keine Änderungen an BLE, GPS-Verarbeitung, Aufzeichnung, FIT-Erzeugung, Performance-/Energy-Engine, WebDAV, Dashboard-Kartensteuerung oder Dashboard-Konfiguration.

## Prüfung und verbleibende Grenzen

Neue Tests prüfen native und Developer-Summaries, Watt/Motor-Trennung, positive Kadenz, echte Nullen/Lücken, unveränderte Originalbytes, GPX ohne Sensoren, gemeinsame Zeit-/Distanzachsen, große Datenreihen mit Spitzen und begrenztem Speicher, Abbruch und beschädigte Dateien. UI-Tests prüfen alle Presets und mittlere Custom-Hintergründe, schmale/breite Ansichten, maximal vier gleich hohe Kacheln, alle Umschalter, konstante Geometrie, feste Diagrammreihenfolge/Zeichenachsen, Ressourcen, ID-Zuordnung trotz gleicher Namen, getrennte Aktionen, Vollbild und Rückkehr zum erhaltenen Filter/ListView.

Die Kartenbedienung/Netzdownloads, Android-Schriftgrößenskalierung am Gerät (1,5-fach zusätzlich im Layouttest geprüft), sehr große reale Fremd-FIT-Dateien, Multisport-Dateien und extreme geografische Fälle benötigen zusätzlich praktische Tests. Fremde Developer-Fields ohne bekannte Semantik werden nicht automatisch verwendet. Fehlende oder unzureichend belegte Kennzahlen bleiben fehlend. Herzfrequenzzonen und die vollständige Verlagerung der Manageraktionen sind bewusst nicht Bestandteil dieser Phase.

Vollständige Testsuite und Release-Build am 12.10.2026 erfolgreich: **250 Tests, 0 Fehler, 0 Fehlschläge, 0 übersprungen**, inklusive 11 neuer Analyse-/UI-Tests. Letzter gemeinsamer Lauf: **1m 8s**. Release-Lint-Vital erfolgreich. Die bekannte Produktionswarnung `onBackPressed()` bleibt; der neue Android-Zurück-Test verwendet dieselbe veraltete native API gezielt. Kein zusätzlicher vollständiger Lint-Lauf.

Danach: Dashboard-Kartenänderung mit zwei minimalen Kartentests geprüft; zusätzlicher Lifecycle-Test stellt sicher, dass die Vollbildkarte das Umsetzen in den globalen Host übersteht und erst beim Schließen freigegeben wird. Abschließender Lauf: **3 Tests erfolgreich und Release-Build erfolgreich in 48s**. Die vollständige Suite wurde nach diesen anschließenden Änderungen nicht erneut ausgeführt. Version 0.7.3/41, signierte Test-APK und Quellcode auf Wunsch des Nutzers veröffentlicht; vorherige Stände bleiben erhalten.

## Deutsche Bezeichnungen und Gruppenfolge

Sichtbare Kennzahlen, Diagrammbezeichnungen und die zugehörigen Accessibility-Texte
verwenden weiterhin zentrale Android-Textressourcen, jetzt mit deutschen Bezeichnungen:
Tempo, Fahrerleistung, Kadenz, Puls, Höhe, Bewegungszeit/Gesamtzeit, Aufstieg/Abstieg,
Normalisierte Leistung, Mechanische Arbeit, Kalorien, Effizienzfaktor, Tempo/Leistung,
Geschätzte FTP, Ø Motorleistung, Fahrer/Motor und Akkuverbrauch. Die Kadenz-Einheit
lautet wie im Hauptdashboard `U/min`; Rohdaten und FIT-Einheiten bleiben unverändert.

Gruppenfolge: Allgemeine Daten, E-Bike, Kadenz & Puls, Leistung, Energie, Performance.
Der Tausch betrifft nur die Kennzahlengruppen. Die feste Diagrammreihenfolge bleibt
Tempo, Kadenz, Fahrerleistung, Puls, Höhe. Zahlenformate, Berechnung, Farben und Aktionen
bleiben erhalten.

Nur drei gezielte Darstellungstests für Datumsformat/Versatz, deutsche Ressourcen
und Gruppenfolge einschließlich schmaler Theme-Layouts ausgeführt; keine vollständige
Suite und keine neue APK für diese kosmetische Änderung.
