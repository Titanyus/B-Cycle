# B|CYCLE 0.8.83 – Bright nach gelieferter JSON

Die hochgeladene Bright-Datei ist jetzt die verbindliche Basis der Vektorkarte. Die bisherige handgeschriebene Adaption wurde durch eine nachvollziehbare Zuordnung aus der unveränderten JSON ersetzt.

- Farben und Deckkraft, Ebenenreihenfolge, Straßenränder und Fahrbahnen, Tunnel, Brücken, Auffahrten, Linienbreiten, Kappen und Strichmuster nach Vorlage. Zoomabhängige Werte berücksichtigen den unterschiedlichen Maßstab von VTM und MapLibre.
- Originalfilter einschließlich Geometrietyp, numerischen Vergleichen und UND/ODER-Verknüpfungen. Kompatibilität zu aktuellen OpenMapTiles-Klassen und Auffahrten. Keine Änderung der Kartendaten oder Aufzeichnungen.
- Referenzsymbole und Wassermuster lokal enthalten; keine neue Symbol-/Glyphen-HTTP-Abhängigkeit, Kartenquelle weiterhin OpenFreeMap. Radhervorhebung bleibt eine getrennte Profiloption.
- 116 Ebenen inventarisiert, 114 einschließlich Hintergrund abgebildet. Verschobene Gebäudeoberseiten und Wasserkopie ausgelassen; weiterhin Draufsicht und flache Gebäude. Keine zusätzlichen eigenen Farben/Breiten. Wald ist laut Vorlage dezent, Ackerflächen haben dort keine eigene Füllung.

[Zuordnung, Pflege und technische Grenzen](vtm-bright-style.md), [Bericht je Ebene](bright-vtm-mapping.json). Zwischen ganzzahligen Zoomstufen, bei sehr dünnen Linien und bei Schriften/Beschriftungsplatzierung bleibt VTM eine Annäherung an MapLibre. Kein Versprechen pixelgleicher Darstellung. Für den nächsten optischen Abgleich sind Screenshots mit gleichem Ausschnitt/Maßstab vorgesehen.

Vier gezielte Stil-/Daten-/Linien-/Quellenwechseltests erfolgreich; nur zwei betroffene Fälle nach Ergänzung wiederholt. Generator-Reproduzierbarkeit, Kompilation, Typografie/Atlas/Diff geprüft; kein Gesamtlauf. Physischer Bildvergleich noch offen.

[Buildnachweis und Download](build-124.md).
