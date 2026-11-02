# B|CYCLE0.8.81 – Akkusparmodus, Bright und Karten-/Wischkorrekturen

- **Akkusparmodus je Fahrradprofil** nach dem Radumfang, standardmäßig aus. Automatische Karte/Dashboard etwa1Hz, kurze Diagramme5s, Gesamtverlauf weiter10s. Nur sichtbare Graphen vorbereiten; Aufnahme-/Trainings-/Warnanzeigen und Chevrons statisch. Manuelle Gesten direkt, Sensoren/Aufzeichnung/Analyse unverändert. Profilspeicherung und Backup enthalten. Kartenstil wird vom Sparmodus nicht umgestellt.
- Vektor-Basisstil jetzt **OpenFreeMap · Bright**, flache VTM-Adaption. Immer Draufsicht, keine3D-Gebäude; Nord-/Fahrtrichtung und Radwegehervorhebung erhalten.
- Standort, Genauigkeitskreis, Kompass und Höhenlabel gemeinsam im GL-Kartenframe. Kleine wiederverwendete Glyphen statt separater Android-Zeichenfläche. Live-Strecke bleibt sichtbar, während neue Punktgeometrie vorbereitet wird; neue Aufnahme/Profilwechsel/Ausblenden verwerfen alte Zwischengeometrie.
- Dashboard-/HRV-Nachbarseite schon während des Ziehens sichtbar, Auswahl weiterhin erst beim Loslassen. Richtungswechsel, Abbruch und verspätete Antworten abgesichert; weiterhin nur eine lebende Karte.
- **Eine Stressfrage mit acht Sliderpositionen** statt Regelmäßigkeitsfrage plus separater Vierfachauswahl. Je zwei Positionen entsprechen einer Intervals-Stufe. Bisherige Regelmäßigkeitswerte auf ausdrücklichen Wunsch umgekehrt; Originale erhalten, keine doppelte Umkehr. Neue Antworten haben Vorrang; Backup kompatibel erweitert.
- Ausgewählte Farbe der Trackpalette immer mit FOREGROUND-Rahmen, auch bei Track3.

[Akku](profile-battery-saver.md), [Bright](vtm-bright-style.md), [Standort/Live-Strecke](vtm-gps-frame-sync.md), [Seitenwechsel](dashboard-page-transition.md), [Stress](hrv-eight-step-stress.md). Nur gezielte betroffene Tests, kein Gesamtlauf; nach Ergänzungen nur betroffene/offene Fälle wiederholt. GPS-/Streckenshader zusätzlich mit echtem lokalem Mesa/GLES geprüft. Kompilation/Typografie/Atlas/Diff erfolgreich.

Tatsächliche Android-GPU-Darstellung, Umhängen/Snapshot verschiedener Kartenhöhen, Wischgefühl und Akkueffekt am Smartphone noch prüfen. Keine gemessene Akkuersparnis. Minimal-/OLED-Kartenstil, Dashboard-Stilumschalter und Live-Stilimport sind nicht Teil dieser APK.

Buildnachweis und Download folgen nach erfolgreichem GitHub-Actions-Lauf.
