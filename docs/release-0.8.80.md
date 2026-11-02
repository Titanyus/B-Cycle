# B|CYCLE0.8.80 – Vektorkarte, Liberty und Seitenwechsel

- Zusätzliche Online-Vektorkarte **OpenFreeMap · Liberty**, ohne API-Schlüssel. Flache Liberty-Adaption für VTM, optionale Radwegehervorhebung; Kartenauswahl/Option je Fahrradprofil und in der Einstellungssicherung. Bestehende Rasterkarten weiterhin wählbar.
- Karte immer in Draufsicht, native Neigung gesperrt; Gebäude nur als Grundrisse, keine Extrusionen. Nord-/Fahrtrichtungsdrehung erhalten. Liberty-Gestaltung für VTM übertragen; Linien, Symbole, Beschriftungen und Zoomstaffelung können von der MapLibre-Browserdarstellung abweichen.
- Gespeicherte Strecken, Livefahrt und Start-/Endmarker werden im selben VTM-GL-Frame wie die Grundkarte gezeichnet. Damit entfällt die bisherige getrennte Android-Zeichenfläche für diese Elemente; GPS-Informations-View bleibt separat. Farben, Breiten, Deckkraft, Livefahrt-Umrandung und echte Lücken erhalten.
- Dashboard-/HRV-Seiten bleiben bis zum Loslassen am Finger, auch bei langsamem vollständigem Hinausschieben. Danach entscheiden Weg und Geschwindigkeit; schnelle Richtungsumkehr kann den Wechsel abbrechen. Vorhandener weicher Übergang, eine lebende Karte und feste Footer erhalten.

[Liberty und Draufsicht](vtm-liberty-style.md), [Vektorquelle](vtm-vector-map.md), [Strecken-Synchronisierung](vtm-route-frame-sync.md), [Wischsteuerung](dashboard-page-transition.md). Nur betroffene Tests ausgeführt, kein Gesamtlauf. Gezielte JVM-/Native-/Shaderprüfungen und Release-R8/Ressourcenprüfung erfolgreich; Typografie/Atlas/Diff geprüft. Tatsächliche Kartendarstellung, schnelle Kartengesten und Wischgefühl auf dem Smartphone noch prüfen.

Minimal-Stil, allgemeiner Akkuschonmodus und Live-Stilimport sind vorgemerkt und nicht Teil dieser APK. Sensorerfassung, Aufzeichnung und Analyse nicht gedrosselt.

Buildnachweis und Download folgen nach erfolgreichem GitHub-Actions-Lauf.
