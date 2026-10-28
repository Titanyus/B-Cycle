# B|CYCLE 0.8.56

Alle Änderungen seit APK0.8.55/Build96.

- HRV-Auswertung mit Tag/Woche/Monat. Periodenmittel aus ausgewählten Tageswerten; tägliche7-/60-Tage-Trend-/Referenzgrundlage bleibt unverändert und wird für Wochen/Monate zusammengefasst. Datenanzahl/bisher anzeigen, kurze AchsenKW5/Okt und vollständiger Zeitraum. Durchgehende Trenner links am Balken; festeY-Skala über Auflösungen. Keine neue HRV-Zoombegrenzung. Details hrv-periods.md.
- Dashboardauswahl neu sortiert: vier Vorlagen ohne Karte zuerst, danach neun mit Karte. Zweite Vorlage bleibt ausdrücklich1/1/1/2. Belegungen und bestehende IDs unverändert. Zentral16dp Abstand nach Standard-Accordionkopf/zu kleinen Überschriften, ohne vorhandene Abstände doppelt zu zählen.
- Eigene Dashboard-Statuszeile: Vollbild-Schalter als Accordion, Vorschau, drei zunächst leere/sortierbare Symbolslots je Seite. Uhrzeit, erstes gewähltes Messengericon mit aktivem Android-Meldungszähler, VPN-Text bei aktivem Android-VPN, Smartphone-Akkusymbol und Mobilfunkbalken der Standard-Daten-SIM. Keine Serverprüfung, neue gefährliche Berechtigung oder Polling. Android-Insets/Kameraausschnitte berücksichtigen; Slotbelegung/erste App-Reihenfolge vollständig sichern. Details dashboard-status.md.
- Kartenoptionen wieder zentral kleine14sp/900-Black-Überschrift mit Abstand; fachliche Viewtags überschreiben die Typografierolle nicht mehr.
- Statistik mit einer gemeinsamen mittigen Jahresauswahl (Farbpunkt im Button) und einem umschaltbaren Monats-/Wochendiagramm. Jahreskurven mit Punktwerten, Wochenzahlen ohneKW, Y-Skala rechts innerhalb der vollen Breite ohne0-km-Wert, keine hellen Abschnittstrenner. Wochen-Pinch/Drag/Fling samt Accessibility,52/53Wochen korrekt vergleichen. Torte folgt gewählten Jahren, Farben bleiben Fahrradprofilfarben. Cache proJahr, keine neuen Fahrtdatei-Lesevorgänge. Details ride-statistics.md.

Gezielte Umsetzungstests und native Vorschauen erfolgreich; keine Gesamtsuite und kein erneuter Telefon-/privater Cloudtest. Neue Status-/WireGuard-/Mobilfunkdarstellung noch am realen Gerät prüfen. Bekannte Darstellungsgrenze: bei sehr vielen dicht liegenden Jahreskurven können Punktwerte überlappen; keine Werte werden absichtlich unterdrückt. Benachrichtigungszähler zählt aktive passende Android-Meldungen, keine garantierte Zahl ungelesener Nachrichten.

[Buildnachweis und Download](build-97.md).
