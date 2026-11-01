# B|CYCLE 0.8.77

- Monats-/Gruppenköpfe im Streckenmanager behalten beim Anheften alle Summenwerte samt Einheiten sichtbar.
- Manueller HRV-Sammelupload aktualisiert vorhandene Intervals-Mess- und Tageswerte, einschließlich Ruhepuls. Identische Werte zählen als unverändert. Der automatische Upload behält seine bisherigen Eigentumsregeln.
- Windrose80% größer, Nordpfeil dynamisch in FOREGROUND; korrigierter Mittelpunkt, schwarze übrige Grafik,70% Deckkraft und Sichtbarkeitsregeln erhalten.
- Kompakter HRV-Empfehlungstext ohne Absatz zum subjektiven Befinden und ohne zusätzliche Zeitraumzeile. Oberer Seitenabstand für Messung/Tagesauswertung8 statt16dp. Obere Grenzwerttexte3dp höher, untere3dp tiefer.
- Weicherer Dashboard-Wechsel: kein Sprung um den Touchschwellenweg, Übernahme der Fingergeschwindigkeit mit sanftem Abbremsen. Derselbe Übergang für horizontale Kalendertagswechsel im HRV-Tagesbereich; Datum, Empfehlung, Werte und Diagrammauswahl bleiben synchron, Footer fest.
- Eine gemeinsame persönliche Historie für PNS und Aktivierung aus eigenen und passend importierten Messungen, ohne Neustart nach Quellen-/Protokollwechsel. Mindestzahl14 frühere gültige Tage innerhalb60 Tagen bleibt; kompakte Hinweise rechts neben dem Indexnamen erklären fehlende Werte.
- Indexbalken bis±4, persönliche−1…+1-Zone weiterhin mittlere50%. Berechnete Zahlen nicht begrenzt; keine Änderung der Indexformeln oder Angleichung an proprietäre Kubios-Zahlen.
- HRV als erste Kennzahl der Detailauswertung:2×lnRMSSD mit passendem Tageskorridor und Mittelwert aus den tatsächlichen HRV-Tageswerten. Erste Reihe HRV/Puls/SDNN/RMSSD/Weitere; weitere Kachelreihe pNN50/LF/HF. Weitere bleibt bei deren Auswahl hervorgehoben, auch eingeklappt.

Gezielt geprüft: echte Ziffern im angehefteten Kopf, aktualisierender Sammelupload und automatischer Schutz, Windrose/Themewechsel, HRV-Abstände/Text/Indizes, gemeinsame Historie und Hinweise, monotone Geschwindigkeitsübergänge, tatsächliche Tageswechsel, fester Footer, neue HRV-Reihe und Monatsnavigation. Nach Korrekturen nur betroffene Fälle erneut ausgeführt; native Vorschauen320/360dp in Flat/Glow visuell geprüft. Typografie/Atlas/Diff geprüft. Kein Gesamtlauf oder physischer Gerätetest; das Wischgefühl ist am Smartphone noch zu beurteilen.

[Buildnachweis und Download](build-118.md).
