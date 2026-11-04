# B|CYCLE 0.8.89

- Fehlende Orts-/Straßennamen korrigiert: der Textshader aktiviert alle drei tatsächlichen Attributpositionen, unabhängig von der Vergabe des Gerätetreibers.
- Straßennummer und Schildhintergrund gemeinsam in derselben Atlaszelle und mit derselben Ein-/Ausblendung; keine separat flimmernden weißen Schildflächen.
- Fahrradprofil → Karten → Kartenebenen: große/kleine Straßen, Wege, Gebäude, Naturflächen, Gewässer, Orts-/Straßennamen, POIs, Gemeindegrenzen und Höhenlinien schaltbar. Auswahl je Profil mit Kopie und Sicherung. Bisherige Voreinstellungen erhalten.
- Höhenlinien optional aus öffentlichen Mapzen/AWS-Terrain-Höhendaten, zunächst aus. Ein Worker, begrenzte Caches, verbundene Vektorlinien mit passenden Kachelanschlüssen; zusätzliche Quellenhinweise. Keine Änderungen an GPS-/Aufzeichnungsdaten.

Drei gezielte Beschriftungs-/Schildprüfungen und drei Layer-/Profil-/Terrain-/UI-Prüfungen erfolgreich. Echte GLES-Pixelprüfung mit vier erzwungenen Attributbelegungen reproduziert den alten Glyphenfehler und bestätigt die Korrektur. Reale Terrainprobe plausibel; Typografie, UI-Katalog, Bright-Generator und Diff-Prüfung grün. Kein Gesamtlauf, keine lokale APK. Gerätebild und Smartphone-GPU/Zoom-Lesbarkeit noch beurteilen.

[Buildnachweis und Download](build-130.md).
