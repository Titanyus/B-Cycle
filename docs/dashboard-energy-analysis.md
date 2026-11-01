# Dashboard während der Fahrt: CPU/GPU und möglicher Akkuschonmodus

Stand01.11.2026, Quellprüfung nach Build118. Analyse beauftragt; anschließend nur die unten beschriebene Aufnahmeanimation bereits optimiert. Noch kein Akkuschonmodus implementiert. Die Bewertung ist eine begründete Schätzung, keine Messung. Annahme: Outdoorfahrt mit sichtbarem Dashboard, GPS-Folgen, eingeschaltetem Bildschirm, verbundenen Sensoren und gegebenenfalls Graphen/gespeicherten Strecken. Geräte, Kartengröße, Punktzahlen und Dashboardkonfiguration können die Rangfolge verändern. Prozentwerte einer CPU-/GPU-Auslastung oder Akkuersparnis wären ohne Messung nicht belastbar.

## Wahrscheinliche Rangfolge

| Bereich | CPU-Arbeit | GPU-/Kompositionsarbeit | Erwartete Bedeutung |
|---|---|---|---|
| Bewegte Karte einschließlich aller Strecken | Hoch: Kameralogik, Layerzeichnung, Projektion und Pfadaufbau | Hoch für Darstellung/Komposition der bewegten Kartenfläche; Mapsforge zeichnet Layer zum Teil zunächst in Bitmaps | Wahrscheinlich größter dauerhafter Grafikposten bei sichtbarer Karte |
| Häufige vollständige Dashboardaktualisierung | Mittel bis hoch, abhängig von BLE-Ereignissen und Layouts | Mittel für geänderte Werte/Flächen | Wichtigster allgemeiner UI-CPU-Kandidat; nicht fest auf1Hz begrenzt |
| Kurze Graphfenster mit gleitender Skalierung | Mittel bis hoch bei mehreren Kurven | Mittel; Kurven/Gradienten und wiederholte Frames | Kann bei mehreren Graphen zeitweise zum Hauptposten werden |
| Permanente Aufnahmeanimation | Wenig Arbeit pro Frame, aber sehr häufig | Kleine Fläche, hält trotzdem Frame-/Kompositionsarbeit aktiv | Besonders im sonst ruhigen Dashboard ohne Karte unnötig teuer |
| Trainings-/Bosch-Farbübergänge | Situativ mittel | Mittel durch transparente/additive Flächen | Statisch günstig; häufige Zonenwechsel oder Akkuwarnung machen sie dauerhaft relevant |
| Graphvorbereitung im Worker | Periodisch mittel | Keine direkte GPU-Arbeit | Schon begrenzt; weitere Einsparung durch bedarfsgerechte Vorbereitung möglich |
| GPS-/BLE-Erfassung, Fahrtberechnung, Speicherung | Laufende notwendige Basisarbeit, überwiegend unabhängig von Karte | Kaum direkte GPU-Arbeit | Nicht mit reiner Darstellungsdrosselung verwechseln |

CPU- und GPU-Zeiten sind nicht direkt addierbar. Eine Mapsforge-Layerzeichnung ist teilweise CPU-/Bitmaparbeit, auch wenn die abschließende Android-Darstellung GPU-beschleunigt ist. Eine reduzierte App-Zeichenrate senkt außerdem nicht automatisch die physische Displayfrequenz.

## Konkrete Befunde

### Karte und Strecken

`MapMotion.frameMs=33`: während aktiver Bewegung bzw. Ankerbewegung werden Kamera und Streckenlayer bis etwa30-mal/s aktualisiert (`MapPanel.animateCamera`). Der Standbildfall und verdeckte/pausierte Karten stoppen diese Schleife bereits. Unveränderte BLE-Werte lösen nicht direkt ein Neuzeichnen der Karte aus.

Bei jedem `refreshRoutes` wird ein Layer-Snapshot veröffentlicht und `requestRedraw()` aufgerufen. Die Punktliste der Livefahrt wird nur bei geänderter Revision kopiert; trotzdem durchläuft `RouteTrackLayer.draw` bei einer tatsächlichen Zeichnung erneut sämtliche Punkte sichtbarer gespeicherter Strecken und der Livefahrt und berechnet Pixelkoordinaten/Pfade. Es gibt dort keine sichtausschnittsbezogene Punktreduktion. Lange Fahrten und bis zu drei lange Vorlagen können daher besonders CPU-intensiv sein. Angeforderte Redraws können zusammenfallen:30Anforderungen/s beweisen keine30vollständigen Layerzeichnungen/s.

Die Online-Basiskarte besteht aktuell aus Rasterkacheln; Bewegung lädt nicht bei jedem Bild alle Kacheln neu. Neue Gebiete verursachen zusätzliche Netzwerk-/Dekodier-/Cachearbeit. Der Nachtmodus wandelt neue Kacheln pixelweise um, cached fertige Umwandlungen aber. Das ist ein Zusatzposten beim Nachladen, keine vollständige Umfärbung pro Frame.

Der Smartphone-Richtungspfeil verarbeitet höchstens10Sensormessungen/s; Änderungen können ebenfalls den gesamten Streckenlayer neu anfordern. Ein Kartenlimit muss deshalb alle Auslöser bündeln: Kamera, Position, Kompass, Anker und neue Trackpunkte. Nur den33-ms-Timer zu ändern reicht nicht.

### Dashboardwerte und Ereignisse

`RideRuntime.tick` läuft1-mal/s. Zusätzlich rufen GPS-, BLE-, Status- und Graphereignisse `notifyChanged()` auf. Der MainActivity-Beobachter startet daraufhin `render()` sowie Aktualisierungen gegebenenfalls offener Verwaltungsansichten. Es gibt in diesem Pfad keine zentrale UI-Drosselung oder Zusammenfassung mehrerer Sensorereignisse.

`render()` bereitet Training/Anzeige auf, durchläuft Datenkacheln, aktualisiert Graphansichten, Status und Kartendatenfelder. Bestehende Gleichheits-/Signaturprüfungen vermeiden bereits manches `setText`, Layout oder Path-Rebuild. Trotzdem bleiben Formatierung, Zustandsabfragen und Durchläufe pro Ereignis. Gemeinsame Schriftanpassung kann bei tatsächlichen Text-/Layoutänderungen mehrere Messdurchläufe auslösen. Hier ist ein gemeinsamer sichtbarer Snapshot im1-s-Takt sinnvoller als die Sensorerfassung zu verlangsamen.

### Graphen

`LiveGraphProvider`: Gesamtverlauf normalerweise alle10s, bei aktiven kurzen Fenstern Publikation1-mal/s; Gesamtserien weiterhin höchstens alle10s. Verarbeitung erfolgt in einem begrenzten Worker. `GraphHistoryBuffer.frame` bereitet derzeit alle kurzen Fenster und verfügbaren Metriken vor, auch wenn nur ein Teil angezeigt wird. Das könnte auf sichtbare Metriken/Fenster beschränkt oder bei unsichtbaren Graphen ausgesetzt werden; Aufzeichnung bleibt vollständig.

`DashboardGraphDrawable.update` verwendet bereits eine Frame-Identitätsprüfung. Für gleichbleibende Daten werden daher nicht bei jedem BLE-Ereignis neue Pfade gebaut. Anders während der3-s-Rückskalierung kurzer Fenster: Es werden etwa alle16ms neue Frames angefordert; dabei werden Kurvenpfade und Füllungen wieder aufgebaut. Häufig neue Zielbereiche können die Animation längere Zeit aktiv halten. Ein Sparmodus könnte neue Daten alle5–10s darstellen und Skalen ohne Zwischenanimation wechseln lassen.

### Animationen

`RecordingButtonIndicator`: endloser4-s-ValueAnimator. Er fordert pro Animationsschritt ein Redraw an, obwohl sein Aussehen innerhalb jedes Zyklus zweimal je1,5s konstant bleibt. Damit sind75% der Zyklusdauer optisch unverändert, trotzdem läuft die Animation mit dem Android-Animator-Takt (geräteabhängig, häufig60Hz). Eine statische, eindeutig erkennbare Aufnahmeanzeige wäre im Sparmodus günstig.

`ZoneAmbientGlow` animiert Farbwechsel1,5s mit33-ms-Schritten. `BoschModeGlow` animiert Moduswechsel1,5s mit16-ms-Schritten; bei niedriger Bike-Batterie pulsiert die Warnung fortlaufend mit33-ms-Schritten. Gleichbleibende Glows haben keinen eigenen endlosen Frame-Timer. Im Sparmodus könnten funktionale Farben/Warnungen erhalten bleiben, aber sofort/statisch erscheinen. Warnungen nicht entfernen.

## Vorschlag für einen einzelnen Akkuschonmodus

| Darstellung | Vorgeschlagene Einstellung |
|---|---|
| Automatische Karte während Fahrt | Start mit einem vollständigen Anzeigeschritt pro1s, keine Zwischenanimation; optional später2s nach Fahrtest |
| Kartendrehung | Zusammen mit Kartenframe, zusätzlicher Winkel-/Bewegungsschwellwert; Norden oben spart weitere Rotationsarbeit |
| Smartphone-Kompasspfeil | Änderungen mit Kartenframe bündeln, Sensorverarbeitung bei nicht benötigter Anzeige pausieren |
| Dashboardzahlen | Sensorereignisse sammeln und maximal1-mal/s einen gemeinsamen Snapshot anzeigen |
| Graphen | Neue Darstellung alle5–10s, ohne animierte Skalenrückstellung; sichtbare Inhalte gezielt vorbereiten |
| Aufnahme-/Zonen-/Akkuanzeige | Statisch, funktionale Farben und Warnungen erhalten |
| Direkte Gesten, Dashboardwechsel | Unmittelbar bedienbar lassen; automatisches1-s-Limit nur außerhalb aktiver Interaktion |
| Unsichtbare Bereiche | Keine UI-Workerpublikationen/Animationen allein für unsichtbare Inhalte; Datenerfassung unverändert weiter |

30→1 automatische Kartenaktualisierung/s entspricht rechnerisch rund97% weniger angeforderten Bewegungsschritten, nicht97% weniger Kartenenergie und erst recht nicht97% weniger Geräteenergie. Manche Arbeit fällt weiterhin für echte Daten, Kacheln, Gesten und Komposition an. Bei25km/h legt das Rad in1s etwa7m zurück; in5s etwa35m. Deshalb würde ich5s für die gesamte Fahrkarte nicht als Standard wählen. Eine grobe Übersicht könnte so sparsam sein, nahe Abzweigungen wirkt die Anzeige aber deutlich veraltet.

Erfassung und Aufzeichnung ausdrücklich unverändert: GPS-Abtastung aktuell1s, BLE-Verarbeitung, Zeitstempel, Distanz, Leistungs-/Pulsberechnungen, FIT, Streckenpunkte und Warnungslogik bleiben vollständig. Ein langsameres Dashboard darf keine langsamere Datenerfassung erzeugen. Auch eine sichtbare Zahlenanzeige darf nicht unnötig neu formatiert werden, nur weil ein Kartenframe läuft.

## Gesamtakku und VTM

Bildschirmhelligkeit und große helle OLED-Flächen sind unabhängig von CPU/GPU wahrscheinlich einer der größten Gesamtverbraucher, bei hoher Außenhelligkeit häufig der größte. GNSS, Funk und Mobilfunk beim Nachladen bleiben als Sockelverbrauch. Offline-/gecachte Karten vermeiden laufendes Nachladen; es ist keine pauschale Aussage möglich, dass Vektorkarten immer weniger Netzwerk oder Energie brauchen. Ein OLED-Stil mit dunklen Flächen und gut lesbaren Wegen ergänzt die reduzierte Zeichenrate.

VTM kann vorbereitete Vektorgeometrie effizient auf der GPU bewegen. Das beseitigt nicht automatisch unnötige UI-Ereignisse, Graphanimationen oder die Aufnahmeanimation. Bei einem Wechsel sollten Streckengeometrien bei Datenänderung aktualisiert und anschließend durch die Kamera transformiert werden; keine Neuübertragung sämtlicher Punkte pro Kameraframe. VTM nutzt standardmäßig bedarfsgesteuertes Rendering, aber unsere Animationen könnten weiterhin dauernd neue Bilder verlangen. Akkuschonmodus deshalb als gemeinsame Darstellungsrichtlinie planen, unabhängig von der Engine.

## Priorität und spätere knappe Bestätigung

1. Permanente Aufnahmeanimation im Sparmodus statisch; Karte mit allen Auslösern auf1Hz bündeln.
2. Sensorereignisse von UI-Publikation trennen, gemeinsame1-Hz-Dashboardanzeige.
3. Animierte Graphskalen/Zonenübergänge aus und Graphvorbereitung auf sichtbaren Bedarf begrenzen.
4. Dunkler Vektorstil, offlinefähige Quelle und vorbereitete Streckengeometrie im Rahmen einer möglichen VTM-Umstellung.

Erwartung: hoher Effekt auf die Grafiklast mit bewegter Karte, mittlerer bis hoher Effekt auf unnötige UI-CPU-Arbeit. Ohne Karte fallen Aufnahme-/Graphanimationen und Ereignisbündelung stärker ins Gewicht. Eine seriöse Prozentzahl für die gesamte Akkulaufzeit lässt sich aus dem Code nicht ableiten. Für spätere Bestätigung genügen zunächst identische kurze Fahr-/GPS-Replay-Szenarien mit gleicher Helligkeit: Normal/Sparmodus, mit/ohne Karte, mit/ohne kurze Graphfenster; CPU-/Frameprofil und Energieindikatoren getrennt betrachten. Keine Tests oder Produktänderungen allein für diese Analyse durchgeführt.

## Bereits für Build119 umgesetzt

Auf Nutzerauftrag ist die Aufnahmeanzeige nun schon im Normalbetrieb optimiert: Zwei500-ms-Übergangsanimatoren, dazwischen je1,5s Ruhe ohne laufenden Animator oder Frame-Callbacks. Ein geplanter Handler-Weckzeitpunkt startet den nächsten Übergang, Aus-/Appwechsel entfernt ihn. Vier-Sekunden-Zyklus und Kosinusüberblendung bleiben erhalten, wiederholte Render-Aufrufe starten den Zyklus nicht neu.75% der Zyklusdauer benötigen damit keine eigenen Animationsframes; das ist keine Aussage über die Gesamtersparnis.

Ausdrückliche Entscheidung: Trainingshilfe bleibt aktuell weich überblendet. Harte Farbwechsel erst im späteren Akkuschonmodus. Karte, Datenanzeige und Graphen bislang unverändert.

## Ergänzung: Sensorfrequenz und weniger Aufzeichnung

Nutzer wünscht zunächst nur einen groben Entscheidungsüberblick; noch keine Drosselung beauftragt. Reguläre Fahrtabtastung und GPS-Anfrage bereits1Hz. Pulsgurt und Powermeter verwenden BLE-Notifications; selteneres Verarbeiten/Speichern senkt deren Sendefrequenz nicht automatisch. Bosch-/Trainerprotokolle getrennt prüfen, keine pauschale Einstellbarkeit der Sensorfrequenz behaupten.

- Gemeinsame Dashboardanzeige1Hz bündelt die etwa halbsekündlich eintreffenden Geschwindigkeitsmeldungen und weitere Sensorereignisse. Konkret bei0,5-s-Geschwindigkeitsmeldungen halbiert sich die sichtbare Rate; nicht zwangsläufig die gesamte UI- oder Geräteenergie. Gemeinsamer letzter Snapshot, maximal knapp1s Anzeigeverzögerung, Erfassung und Fahrtberechnung unverändert.
- Weniger echte aktive Sensorabfragen können Funk/CPU sparen. Langsame Geräte-/Akkustatuswerte eignen sich eher als Leistung/Puls/Kadenz. Bereits seltene Abfragen bringen entsprechend wenig Zusatzgewinn. Keine willkürliche Anpassung von Verbindungs-/Frischegrenzen ohne Protokollprüfung.
- Notifications nur verwerfen: eher geringe Funkersparnis, weil Sender und Empfang weiterlaufen. Zur echten Drosselung wäre Unterstützung durch Sensor/Protokoll bzw. angepasste Verbindungsparameter nötig; bei BLE nicht allgemein garantiert.
- Weniger Fahrtpunkte speichern: kleinere Dateien und weniger Speicherarbeit, aber gewöhnlich deutlich weniger Potenzial als sichtbare Karte/Display. Sensorenergie bleibt oft gleich. Spitzen, kurze Pausen und Zeit-/Leistungsberechnungen können bei veränderter Datengrundlage verlieren. Reduktion nur des Exports spart während der Messung nochmals weniger.
- GPS seltener erfassen: potenziell wirksamer, aber Geräte können GNSS trotzdem aktiv halten. Längeres Intervall allein garantiert keine entsprechende Energieersparnis. Schlechtere Kurven-/Positionsauflösung und spätere Bewegungs-/Pausenerkennung; deshalb vorerst vollständige1-Hz-Erfassung behalten.

Entscheidungsreihenfolge: unnötige Darstellung und Daueranimationen reduzieren, passive/aktive Statusarbeit prüfen, erst danach bei ausdrücklichem Bedarf Messauflösung ändern. Zusätzliche Hauptbefunde der Quellprüfung sind wiederholte Projektion aller sichtbaren Streckenpunkte, Kompass als weiterer Redraw-Auslöser und Vorbereitung unsichtbarer Graphfenster/Metriken; sie gehören bereits zur Rangfolge oben.

## Verbindliche Nutzerpräzisierung nach Abschluss der Analyse

Sensorfrequenz und weniger Aufzeichnung waren ausschließlich eine Verständnisfrage, kein Optimierungsauftrag. Die App lebt von zuverlässiger vollständiger Erfassung und anschließender Analyse. Für die spätere Akkuoptimierung daher Darstellung/Animationen/gebündelte UI-Updates optimieren; Sensorfrequenz, Rohdatenannahme, reguläre Aufzeichnung und Analyseberechnungen beibehalten. Keine Sensor- oder Datendrosselung aus den vorangegangenen Beispielen ableiten.
