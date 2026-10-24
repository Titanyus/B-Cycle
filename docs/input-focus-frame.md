# Gemeinsamer Fokusrahmen und ACCENT-Flächen

Stand 24.10.2026. Eingabefelder verwenden BicycleInputFrame an den drei gemeinsamen Stileinstiegen. Normalzustand, Feldmaße, Rundung, Padding, Text, Masken, Einheiten, Validierung, Tastatur und Autosave bleiben erhalten.

Aktivierte fokussierte Felder erhalten eine 1,5-dp-Kontur. Fünf Stops bei 0/44/50/56/100 % ergeben dunkel → ACCENT → hell → ACCENT → dunkel. Die Ableitungen mischen ACCENT zu 35 % mit Schwarz beziehungsweise Weiß. Die schmale helle Spitze hat längere dunkle Anläufe. Achse +20°; 0° zeigt horizontal rechts, positive Winkel drehen im Uhrzeigersinn.

Aus genau dieser farbigen Kontur entstehen zwei echte Blur-Bitmaps: nahe 2,5 dp innen und außen (75 % Deckkraft), weit 15 dp ausschließlich außen (55 %). Beide werden mit BlendMode.SCREEN gezeichnet. Die RGB-Kanäle werden premultipliziert gemeinsam mit Alpha über drei separierbare Box-Pässe geglättet, als schnelle Gauß-Näherung. Helle Stellen erzeugen dadurch einen helleren Schein. Keine gestapelten Umrisslinien und kein Graphen-Blur.

Die Berechnung erfolgt nur bei Cache-Änderung, nicht in jedem Zeichenframe. Fokusverlust, disabled, Themewechsel und Detach geben den Cache frei; große Raster sind auf 512.000 Pixel begrenzt. Zeichnen im Parent-Overlay verhindert Abschneiden an Formularzeilen; Scrollviewport und Sticky-Header bleiben ausgespart. Vorhandene Fokuslistener werden nicht ersetzt.

Aktive ACCENT-Flächen und Segmentbuttons verwenden zentral BicycleAccentGradient: ACCENT nach dunkler ACCENT bei -115°. Damit zeigt die Achse nach oben links. Die Abdunklung wird bei Bedarf reduziert, damit der vorhandene Vordergrund mindestens 4,5:1 Kontrast behält. Kein neuer Profilparameter oder App-Schalter.

[Native Rahmenvorschau](previews/input-focus-frame.png) · [Fahrerprofil](previews/rider-profile-focus-preview.png) · [Interaktiver Designer](input-designer.md). Der Browser verwendet seine eigene Blur-Implementierung; native Darstellung ist maßgeblich.

Gezielte Tests erfolgreich: Blur-Farbverteilung und Quellbild-Erhalt, Farben einschließlich Weiß/Schwarz, nativer Rahmen/innerer und äußerer Schein, Scrollen, Sticky-Clipping, Cache-Wiederverwendung/-Freigabe, Theme/disabled, Geometrie/Werte/Listener und ausgewählte Formular-/HRV-Regressionsfälle. Füllwinkel und Textkontrast über alle Theme-Presets geprüft. Native Vorschauen visuell geprüft; kein Hardware-Benchmark oder Gesamtlauf.


## Scrollkorrektur 24.10.2026

Der native ScrollView-Overlay-Canvas trägt beim Zeichnen über einen Elterncontainer bereits den negativen Scrollversatz. Die aus Fensterpositionen abgeleitete Feldposition ist dagegen schon viewportbezogen. Den Canvas-Versatz deshalb genau einmal neutralisieren, bevor Kontur-Blurs und Viewport-/Sticky-Clips gezeichnet werden. Verhindert doppelte Scrollgeschwindigkeit, ändert weder Blur noch Feld-/Scrollfunktion.

Regression zeichnet über den tatsächlichen Elterncontainer statt ScrollView.draw isoliert. So ist der frühere Fehler reproduzierbar; Farbwerte direkt neben dem Feld müssen nach Scrollen identisch mitwandern. Sticky-Header weiterhin aussparen.

## Gemeinsame Modi und PRIMARY, 24.10.2026

Die oben als ACCENT bezeichnete Stilbasis ist jetzt PRIMARY. ACCENT bleibt eigenständig gespeichert. Aktive Segmente und Fokusfelder verwenden dieselbe Kontur-/Blur-Dekoration. Flat-Design zeichnet die PRIMARY-Kontur einfarbig und unterdrückt beide Glows; Glow-Design (Standard) ergänzt die bisherigen Ableitungen auf derselben Grundgeometrie. Details design-modes.md.
