# B|CYCLE 0.8.45

- Neue headerlose TERTIARY-Startkarte bei fehlender E-Bike-/Trainerzuordnung im gewählten Profil. „Sensoren suchen“ öffnet die vorhandene Sensorverwaltung und Suche, „Jetzt nicht“ pausiert pro Profil bis zum nächsten lokalen Kalendertag. Gespeicherte, nur getrennte Geräte lösen keinen Hinweis aus; HRV, Aufnahme, Speichern und offene Dialoge haben Vorrang.
- HRV: Verbleibende RR-Detektorauffälligkeiten allein führen bei sonst auswertbaren neuen Messungen zu einer orangefarbenen Warnung mit Anzahl statt zur Ablehnung. Korrektur und Rohdaten unverändert; technische Abbrüche/Konsistenzregeln bleiben erhalten. Alte Messungen werden nicht still neu bewertet.
- Globale Positions-Auswahlkacheln für Benachrichtigungen und Trink-Erinnerung entsprechen dem Fahrradprofil-Stil.
- Fahrtbenennung: fünf einzelne Karten, inaktiv1dp/50%-Rahmen, aktiv bisheriger Stil. Per Drag-and-drop sortierbar; Datum und Zeit unabhängig aktivierbar, aber als gemeinsamer Block verschiebbar. Reihenfolge in Vorschau, Namen, Profilvorlagen, START-Snapshots und Sicherung berücksichtigt; ältere Vorlagen kompatibel.
- Glow-Sticky-Header rundum gerundet und auch angeheftet8dp Abstand zum Hauptheader; Flat bleibt bündig. Boxen schieben sich weiterhin direkt weg.
- Footer in beiden Designs TERTIARY ohne Rahmen, mit bisherigen Aktionen und Glow-Schatten. Start/Stop behält seine PRIMARY-Hervorhebung.

Gezielte Prüfungen zu Sensor-Karte/Profil/Tag/Lifecycle/Suche/HRV-Priorität, RR-Policy/Korrektur/Konsistenz/Frequenz/Speicherung, Positionskacheln, Drag/Abbruch/Lock, Namen/Profilen/Sicherung, Sticky-Push/Abstand und Footer erfolgreich. Native Vorschauen visuell geprüft. Kein vollständiger Gesamtlauf und kein BLE-/Gerätetest.
