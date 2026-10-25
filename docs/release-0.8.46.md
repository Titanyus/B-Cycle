# B|CYCLE 0.8.46

- Neues App- und README-Logo aus dem bereitgestellten BClogo3.zip; unveränderte Original-PNG, dunkler Launcher-Hintergrund und vorhandene Android-Maskierung.
- Glow-Sticky-Kategorien: Header und geöffneter Inhalt bilden wieder eine nahtlose Box. Beim Hochschieben übernimmt der Header den gerundeten unteren Kartenabschluss, ohne offene Ecklücken oder eckige Reste.
- Der deckende Streifen unter dem Hauptheader entfällt. Inhalt scrollt sichtbar durch den 8-dp-Abstand unter den weiterhin transparent auslaufenden Headerschatten.
- Zwischen Radprofil-Kacheln und erster Sticky-Box gilt wieder der zentrale Abschnittsabstand von32dp. Zwischen Glow-Boxen bleiben24dp.
- HRV-Auswertung: Jeder vorhandene Balkenwert bleibt mittig über seinem Balken, auch bei engem Zoom. Keine Kollisions-/Breitenunterdrückung mehr; Beschriftungen dürfen sich überlappen und werden am Diagrammrand weiterhin abgeschnitten.

17 gezielte Tests zu HRV-Trend/Zoom/Pan, Sticky-Push/Glow-/Flat-Karten, nativer Pixelprüfung und Headerschatten erfolgreich. Zusätzlich zwei gezielte Prüfungen nach der Profilabstand-Korrektur erfolgreich. Ressourcen-Kompilierung und Prüfung der Original-PNG/XML-Verweise erfolgreich; Typografie-/Diffprüfung erfolgreich. Keine Änderungen an Messwerten, Datenformaten oder Aufzeichnung. Kein vollständiger Gesamtlauf und kein Gerätetest.
