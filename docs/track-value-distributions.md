# Streckendetails: zeitgewichtete Zonen-Histogramme

Unter den bisherigen Streckenverlaufsdiagrammen folgen zwei native quadratische Diagramme: Leistung und Puls. Seit der Änderung nach Build93 ersetzen feine Balken die bisherige geglättete Kurve und deren farbigen Hintergrund. Die fünf Verlaufsgrafiken darüber und die bestehenden distanzgewichteten Zonenbalken bleiben unverändert.

## Daten und Einteilung

RouteZoneAccumulator verwendet dieselben bekannten Zeitintervalle wie zuvor (vorheriger gültiger Messwert, maximal fünf Sekunden; keine Überbrückung von Datenlücken, Pausen oder Segmentwechseln). Fehlende Messwerte werden nicht als null gewertet; echte null Watt bleiben erhalten. Der bestehende Integer-Histogramm-Akkumulator erhält die Zeiten, bevor die Diagrammdaten auf dem Bibliotheksworker reduziert werden. Keine Änderungen an Sensorwerten, FIT-Aufzeichnung, Importdateien oder gespeicherten Fahrtkennzahlen.

Verbindliche Nutzerentscheidung: **gleich breite Balken mit festen Watt-/bpm-Schritten, unterschiedlich viele Balken je Zone**. Standard 5 W bzw. 2 bpm, aus zentralen BicycleDesign-Tokens. Bei sehr großen Messspitzen wird die feste Schrittweite um ganze Basiseinheiten verbreitert, sodass höchstens128Balken entstehen. Achsengrenzen werden auf diese Schrittweite ausgerichtet; sämtliche Balken haben dieselbe numerische und sichtbare Breite. Die X-Achse bleibt linear. Keine Kurvenglättung und kein Verschieben der Zeitsummen zwischen Histogrammbereichen. Klassen berücksichtigen inklusive Leistungs-Obergrenzen und den maximalen Puls der letzten Zone, damit exakte Grenzwerte nicht fälschlich im anschließenden Zonenbereich erscheinen.

Leistung zeigt alle sieben Coggan-Zonen und die gemessenen Spitzen, mindestens bis175%FTP für ausreichend Platz in der offenen letzten Zone. Puls folgt der im Fahrerprofil eingestellten Methode und den Messspitzen. Werte außerhalb definierter Pulszonen bleiben als neutrale Balken sichtbar, statt als eine falsche Zone markiert zu werden.

## Darstellung

Balken haben kleine Lücken und abgerundete Ecken. Ihre Höhe stellt die erfasste Zeit im jeweiligen Werteintervall dar, relativ zum höchsten Balken; fünf Prozent Platz oberhalb des Peaks. Die Werte- und Zeitverteilungen bleiben unmodifiziert.

Farben stammen ausschließlich aus der bestehenden gemeinsamen Leistungs-/Pulszonenpalette (ZoneGlowColors.powerColors / TrainingZoneColors.values). Harte Farbübergänge liegen exakt an den Profil-Zonengrenzen. Trifft eine Grenze einen Balken, ändert dieser genau dort seine Farbe; seine Breite bleibt gleich. Der verwendete Shader hat doppelte Stopppositionen und erzeugt keinen weichen Farbverlauf und keine Zwischenfarben.

Oberhalb stehen Z1…Z7 bzw. Z1…Z5 mit kurzen farbigen Linien. Direkt darunter stehen seit der Ergänzung nach Build95 die Zeitanteile mit einer Nachkommastelle, kleiner (9sp statt11sp) und bei sehr schmalen Zonen weiter an deren Breite angepasst. Die zusätzliche Legendenzeile erhält16dp Platz innerhalb der unverändert quadratischen Diagrammfläche. Prozentwerte werden einmalig formatiert und auch zugänglich vorgelesen. Ihre Grundlage sind die ursprünglichen Integer-Werte und die echten Profil-Zonengrenzen vor Zusammenfassung in Darstellungsbalken; Bezugsgröße ist die gesamte gültige Messzeit des jeweiligen Sensors. Außerhalb der Pulszonen liegende Messzeiten bleiben neutral und werden keiner Zone zugeschlagen, daher können deren Anteile zusammen unter100% liegen. Feine gestrichelte senkrechte Linien trennen die echten Zonen zusätzlich. Seitenbeschriftung, Watt-/bpm-Skala, Theme-Typografie und ungefähr quadratische Größe bleiben. Darstellung in Flat und Glow identisch; neutrale Balken/Schrift folgen dem hellen/dunklen Theme, Zonenfarben bleiben semantisch gleich.

Balkengeometrie wird beim Größenwechsel gecacht, Farbstopps bei Größe/Theme aktualisiert. draw() verarbeitet keine Rohdaten, glättet nicht und erzeugt keine neue Histogramm-Auswertung. Speicher ist auf maximal128Balken und wenige Farbstopps begrenzt.

## Nachweis

RouteValueDistributionTest prüft Zeitgewichtung, Lücken/Pausen, exakte Summe, feste Watt-/bpm-Breiten, Zonengrenzen, Messspitzen und begrenzte Größe. Native RouteDistributionViewTest-Fälle prüfen echte Balken/Lücken, Farbwechsel innerhalb eines Balkens, neutrale Werte, Themewechsel, Zonenmarkierungen und Hochformat-Größen in allen Theme-Presets. Keine echte Fahrt für Vorschauen verwendet.

[Vorschau aus synthetischen Daten](previews/track-distributions.png).
