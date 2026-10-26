# B|CYCLE 0.8.48

- Fahrtabschluss heißt **Zusammenfassung**. E-Bike-Fahrten zeigen zusätzlich den letzten gespeicherten Akkustand und die letzte gespeicherte Akkutemperatur. Keine Livewerte als Ersatz, fehlende Werte bleiben Strich. Negative gespeicherte Akkutemperaturen werden korrekt gelesen.
- Speicher-/Uploadstatus kompakt als gemeinsame Dienstsymbole mit Haken, X oder Pending, zentriert und umbrechend. Angenommene Uploads bleiben ausstehend bis zur Bestätigung. Warnungen und lokale Teilfehler bleiben sichtbar; „Zu den Details“ bleibt erhalten.
- Dashboard hinter Zusammenfassung und täglichem HRV-Dialog weichgezeichnet; beide Dialoge mit diffusem Schatten in Flat und Glow. Einstellungen behalten ihre bisherigen Farben und Dialogkonventionen.
- Dashboardgraphen und Trainingshilfe unabhängig von Flat/Glow; ursprüngliche Verlaufsfüllung in beiden Designs. Zuordnungsmarker passend zur linken/rechten Graphachse neben der Beschriftung, zusätzlicher mittlerer Skalenwert bei allen Graphen/Zeitfenstern.
- Dashboardeditor endet nach5s ohne offene Feldauswahl beziehungsweise12s mit offener Auswahl. Bedienung setzt Frist zurück, Sensordaten nicht. Trendindikator-Auswahl bei deaktiviertem Datenfeld gesperrt, Konfiguration bleibt erhalten.
- HRV-Messheader blendet Texte/Icons während Ruhe-/Messphase über2s aus und danach wieder ein. Hintergrund reicht in den ausgeblendeten Statusleistenbereich.
- Karte: Track, Positionspunkt und Höhenlabel bleiben bei halben Zoomstufen maßhaltig. Pinch kann gleichzeitig die Karte verschieben; bestehende Zoomgrenzen und Nachlauf bleiben.
- Neue schreibgeschützte **Wartung** in den Einstellungen: Dreeve-Wartungsintervalle über die bestehende Dienstkonfiguration abrufen. Proof of concept, keine Erledigen-/Bearbeitungsfunktion. API-Verfügbarkeit hängt von der Dreeve-Version ab.
- Verwaiste eigene Fahrten nach Profillöschung ausdrücklich neu zuordnen: Button unter Profile und gemeinsamer Hinweis im Streckenmanager. Gruppierung nach ehemaliger Profilidentität; Namen/Typ nur als Vorschlag. Fahrtdateien, Titel, Links und Uploadbelege erhalten.
- Nutzerdateien BClogo5.zip und Header2.zip als neues App-Logo und Einstellungsmotiv übernommen.

Gezielte Tests zu jedem Umsetzungsschritt, native UI-Vorschauen und zentrale Typografie-/Atlas-/Diffprüfungen. Keine neuen externen Abhängigkeiten oder Änderungen bestehender Aufnahme-/Profil-/FIT-Formate. Kein vollständiger Gesamttestlauf, kein Hardwaretest oder Test gegen einen echten Dreeve-Server. Signierter optimierter Release über GitHub Actions; [Buildnachweis und Download](build-88.md).
