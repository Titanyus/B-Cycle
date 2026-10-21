# Konvention für Text-Scroller

Überlange Streckentitel und zukünftige Text-Scroller verwenden dieselbe gemeinsame
Zeitleiste in `TextScrollerMotion`. Texte, die vollständig passen, bleiben statisch.

1. 3 Sekunden weich einblenden.
2. 4 Sekunden am Anfang stehen lassen.
3. Innerhalb von 4 Sekunden weich auf die Scrollgeschwindigkeit beschleunigen.
4. Mit 18 dp/s weiterfahren: 60 % der bisherigen nominalen 30 dp/s.
5. Innerhalb von 4 Sekunden bis zum vollständigen Textende abbremsen.
6. 4 Sekunden am Ende stehen lassen.
7. 3 Sekunden weich ausblenden.
8. 2 Sekunden ohne Text pausieren, dann wieder bei Schritt 1 beginnen.

Beschleunigungs-/Bremsverlauf: halbe Kosinuswelle der Geschwindigkeit; die Strecke
ist deren Integral. Dadurch sind Position und Geschwindigkeit an den Übergängen
stetig. Die Deckkraft folgt ebenfalls einer weichen Kosinuskurve. Die Länge der
Phase mit konstanter Geschwindigkeit ergibt sich aus dem tatsächlich verdeckten
Text. Bei weniger als 72 dp Überlauf entfällt diese Phase und die maximale
Geschwindigkeit sinkt; die beiden viersekündigen Rampen bleiben erhalten und
überschreiten das Textende nicht.

Das vollständige geformte Textlayout wird hinter einem festen Clip verschoben;
kein Verschieben eines bereits abgeschnittenen TextView-Ausschnitts. Schriftgröße,
Farbe, zugänglicher vollständiger Text und der sichtbare Titelbereich bleiben
erhalten. Bei deaktivierten Android-Animationen statisch mit Ellipse anzeigen.
Animation beim Verlassen/Verbergen abbauen; bei geändertem Text oder verfügbarer
Breite die tatsächliche Überlaufstrecke neu bestimmen.

Native Renderprüfung deckt das anfangs unsichtbare Textende ab. Weitere Prüfungen
decken die feste Phasenfolge, die Geschwindigkeit und kurze Überläufe ab.
