# B|CYCLE 0.8.88

- Weniger Ortsnamen in Übersichtszoomstufen: gemeinsame Größenklassen mit Zoomschwellen/Hysterese und größeren Abständen. Bereits sichtbare gleichklassige Namen werden nicht durch wechselnde Tile-Feinränge verdrängt.
- Ortsnamen und andere Beschriftungen werden über 240 ms weich ein-/ausgeblendet. Gruppenwechsel laufen zeitlich gemeinsam, auch bei Zoomsprüngen. Kurz wiederkehrende Namen setzen mit ihrer aktuellen Deckkraft und ursprünglichem Sichtbarkeitsalter fort. GPU-Alpha mit nativer Glyph-/Atlasgeometrie, keine neue Karteninstanz.
- Gemeinde-/Ortsteilgrenzen (admin_level ≥8) ausgeblendet; übergeordnete Grenzen bleiben. Bright-JSON und generierte Stilassets unverändert.
- Feldauswahl: tatsächlicher Vollbild-Seitenhintergrund TERTIARY. Globaler Abstand von 16 dp bleibt; nur das frühere zusätzliche Außenpadding entfällt.
- Glow-Sticky-Kästen außen links/rechts 8 statt 16 dp. Abstand zwischen Kästen weiterhin 24 dp.

Acht gezielte native Fälle für Labelplatzierung/Fades, Grenzen, Feldauswahl, Boxgeometrie und Layerwechsel erfolgreich. GLES-Shader-Compile/Link und Alpha-Pixel für Ein-/Ausblendung und Umkehr geprüft. Kompilation/Typografie/Atlas/Bright-/Diff-Prüfung erfolgreich. Kein Gesamtlauf, keine lokale APK. Smartphone-GPU und subjektiver Zoom-Eindruck noch zu beurteilen.

[Buildnachweis und Download](build-129.md).
