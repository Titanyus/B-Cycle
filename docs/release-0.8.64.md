# B|CYCLE 0.8.64

Alle Änderungen seit Build104:

- Fahrradlicht bleibt während derselben E-Bike-Verbindung gültig, ohne bisherigen60s-Verfall. Nach Trennung/Wiederverbindung ist ein neuer Zustand erforderlich. Beide Lichtindikatoren und Aufzeichnung verwenden dieselbe Regel.
- Bosch-Akkuwarnung nur bei tatsächlich verbundenem E-Bike; gespeicherter Akkustand bleibt sichtbar. Record-Kreis10%kleiner (54dp),3er-Kacheln maximal45sp/Einheiten10sp, bei Platzmangel weiterhin gemeinsam kleiner.
- NP benötigt90% Leistungs- und vollständige30s-Fensterabdeckung. Arbeit/Kalorien/Ratio behalten ihre bisherigen95%-Grenzen. Die bisher untersuchte private FIT bleibt ohne Veränderung der Originaldatei nachberechenbar.
- Bestätigte E-Bike-Standpausen werden nur aus der NP-Auswertungszeit entfernt. Aufnahme/Gesamtzeit/FIT-Timer und Rohwerte laufen unverändert weiter bis zum manuellen STOP. Voraussetzung: keine Radverbindung/keine verfügbare Leistung sowie mindestens10s frischer, genauer GPS-Stillstand. Ohne zuverlässige Belege bleibt Zeit unbekannt. Keine zusätzliche GPS-Aktivierung. [Details und Grenzen](confirmed-np-pauses.md).
- Neue Journal-/FIT-Marker erhalten die NP-Pausenregel bei späterer Auswertung und explizitem Neuexport. Keine automatische Umdeutung alter/fremder FIT-Dateien ohne Belege, keine Nullwerte oder Fenster über Pausen erfinden. Performance-Modellversion5, bestehende Journale lesbar.
- Flat-Trainingsbalken im ModusNormal mit unverfälschten Zonenfarben; weiche Farbwechsel erhalten. Glow-Modus/Bosch-Fahrmoduslicht unverändert.
- Haupt-Einstellungsmenü mit League Gothic30sp,54dpZeilen und Icons auf75%(24dp/Chevron18dp), unveränderte Beschriftungen/Navigation/Gruppierung. Unterseite Erweitert unverändert.

Prüfung: gezielte Leistungs-/Pausen-/Journal-/FIT-/Neuexport-/GPS-/Runtime-/UI-Tests erfolgreich, inklusive identischer Live-/Replay-Ergebnisse, tatsächlicher NP-Nachberechnung aus FIT, unveränderter Gesamtzeit, Marker-Feldwahl und langer Aufnahme bei ausgeblendeter Activity aufAndroid13/15/16. Weitere bereits abgeschlossene Prüfungen zu Licht/Akku/Schrift/Kreis, Flat-Farbtreue und Einstellungsnavigation. Testgruppen überschneiden sich; keine vollständige Gesamtsuite, kein neuer physischer Rad-/Langzeit-/Cloudtest. Typografie/Atlas/Diff geprüft.

Buildnachweis folgt nach Veröffentlichung.
