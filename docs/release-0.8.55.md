# B|CYCLE 0.8.55

Enthält alle Änderungen seit APK0.8.54 / Build95.

- Dashboard-Auswahl nach Layouts mit und ohne Karte gruppiert. Drei weitere Layouts ohne Karte:1/1/2/2/2,1/1/1/1 und1/1/1/3/3. Neuntes frei belegbares Feld; bisherige Belegungen, Kartenlayouts und Sicherungen mit acht Feldern bleiben erhalten. Footer bei allen Layouts ohne Karte sichtbar.
- Gemeinsame dynamische Messwert- und Einheitengrößen je sichtbarer Zweier-/Dreiergruppe. Dreierwerte nicht mehr auf45sp festgelegt. Ausgeblendete Felder, Akku-Sammelfelder und andere Dashboardseiten begrenzen die Schrift nicht; Einzelfelder und Beschriftungen behalten ihre Rollen.
- Bei Dashboards ohne Karte bleibt die Kartenansicht pausiert; zusätzliche Layer-Sperre verhindert neue Kachelaufträge vor Aktivierung oder nach Pausieren. GPS, Radverbindung und Fahrtaufzeichnung bleiben unabhängig aktiv.
- Akku-Feldauswahl: führende Sensoricons erhalten den rechten Auswahlschalter. Eindeutige Auswahl über stabile Quellen-IDs, auch beim Wechsel zwischen Schaltwerk-Einzelfeld und Akku-Sammelauswahl.
- Neue Statistikseite unter Einstellungen nach Strecken: Monatskilometer mit Jahresvergleich, horizontal scrollbare ISO-Wochen des aktuellen Jahres und gesamte Fahrzeit je Fahrrad in dessen Profilfarbe. Als gefahren markierte Importe zählen mit; geplante Strecken nicht. Cached Berechnung aus vorhandenen Kurzkennzahlen im Bibliotheksworker.
- HRV-Auswertung mit kleinen kalendergebundenen Hauptmondphasen auf der oberen Rasterlinie. Offline berechnet, auch an Tagen ohne Messung. Header „Auswertung: DATUM“, frisch heutiger Tag und anschließend bewusste Tagesauswahl. Messung, Messergebnis, Tagesbefinden und Verlauf mit schlichten Secondary-Headern ohne Rahmen/Glow.
- Langer Druck auf lokale Speicherung/Upload: eigene Export-FIT anhand der aktuellen Feldauswahl des zugeordneten Fahrradprofils neu erzeugen, soweit vollständige interne Daten vorhanden sind. Danach vorhandene Prüf-/Uploadaktion; unveränderte Originale, vollständige Analyse, Fahrt-ID und Cloudverknüpfungen bleiben erhalten. Höhenkorrektur sowie Streckensicherung berücksichtigen die neue Exportversion. Keine automatische Cloudlöschung; Dienste können vorhandene Fahrten als Duplikat ablehnen.
- Kartenoptionen bereinigt: Ausrichtung und GPS-Folgen nur noch über Kartenbuttons einstellen. Vier übrige Optionen unter kleiner Überschrift „Kartenoptionen“.
- „Auf Karte angezeigt“ mit Standardrahmen. Spezialbuttons mit20% flacherer sichtbarer Pille bei gleicher Breite/Bedienfläche. Kleine Zeitanteile unter den farbigen Zonenmarkierungen der Leistungs-/Puls-Histogramme.

Gezielte Prüfungen während Umsetzung; keine Gesamtsuite oder privaten Cloudaufrufe. Einzelheiten in docs/CONTINUE-HERE.md, docs/ride-statistics.md und docs/fit-reexport.md. Prüfung mit synthetischen Daten, kein erneuter Test auf dem Nutzertelefon.

Buildnachweis folgt nach Veröffentlichung.
