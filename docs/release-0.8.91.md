# B|CYCLE 0.8.91

- Maximalzoom der Vektorkarte: übergroße, beim nativen Overzoom unbeschnittene Gebäudegeometrie vor der 16-Bit-Vertexpackung auf die gepufferte Zielkachel beschneiden. Verhindert den reproduzierten Koordinatenüberlauf, der falsche/invertierte Flächen erzeugen kann. Kamerazoom, Kartendaten und Stil bleiben erhalten.
- Hauptstraßennummern/-namen: deutlich größere, richtungsunabhängige Wiederholungsabstände. Gleiche Kennungen über Leerzeichen- und Stilvarianten erkennen. Neue Kandidaten gemeinsam für den sichtbaren Kartenausschnitt auswählen und größere Lücken bevorzugt auffüllen; bestehende sichtbare Namen bleiben bevorzugt.

Gezielte native Overzoomprüfung für Stufen 15–19, vorhandene Label-/Fadefälle und gleichmäßige Verteilung bei umgekehrter Eingabereihenfolge/90° Kartenrotation erfolgreich. Typografie/Bright-Generator/Diff grün; kein Gesamtlauf, keine lokale APK. Gerätevergleich der konkret gemeldeten Szene noch offen. [Befund und Open-Source-Referenzen](vtm-maxzoom-and-road-labels.md).

[Buildnachweis und Download](build-133.md).
