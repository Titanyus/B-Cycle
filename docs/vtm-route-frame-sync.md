# Strecken im selben VTM-Kartenbild – Korrektur nach Build120

Auftrag02.11.2026: In der ersten VTM-APK funktioniert die Karte grundsätzlich, eingeblendete Strecken folgen beim schnellen manuellen Ziehen sichtbar verzögert. Ursache: Rasterkarte auf der GL-Surface, Strecken separat im Android-View-Zeichenlauf. Gleiche mathematische Projektion garantiert dort keinen gemeinsamen sichtbaren Frame.

## Änderung

- Gespeicherte Strecken, Livefahrt und Start-/Endmarker liegen nun in einem echten `VtmRouteLayer` über der Rasterebene. Die native View `RouteTrackLayer` publiziert vorbereitete Geometrie und zeichnet nur noch GPS-/Genauigkeits-/Kompass-/Höheninformationen. Kein zusätzliches Android-View-Redraw nötig, um Strecken zu bewegen.
- Linien- und Punktprimitive werden mit genau dem `GLViewport` des aktuellen VTM-Frames gezeichnet. Ein bereits weitergelaufener UI-Kamerastand wird nicht dafür abgefragt. Die vollständige Streckenaufnahme ist damit auch im bestehenden PixelCopy-Bild enthalten; keine doppelte Canvas-Streckenzeichnung im Dashboard-Snapshot.
- Weltkoordinaten werden weiter nur bei geänderten Punktlisten auf dem begrenzten Worker vorbereitet und unveränderlich publiziert. GL-Vertices bleiben bei normalem Pan/Rotation/kleinen Zoomänderungen gespeichert; GPU-Matrix bewegt sie im jeweiligen Kartenframe. Bei Daten-/Stiländerung, größerem Zoomverhältnis oder mehr als512Referenzpixeln Verschiebung wird neu aufgebaut. Kein hochauflösendes Streckenbild pro Frame und kein zusätzlicher Animationstimer.
- Sichtbereichs-Clipping hält Koordinaten im nativen Short-Vertexbereich. Segmentanfänge und Datumsgrenze bleiben berücksichtigt; keine Brücke über GPS-Lücken. Generationsschutz/Profilwechsel/Sichtbarkeit erhalten.
- Eigener kleiner Shader auf Basis des VTM-Linienformats: Linienbreiten und runde Enden, Farbe/Deckkraft sowie weiße Livefahrt-Umrandung in einem Durchgang. Dadurch keine doppelte Alpha-Verrechnung von Kern und Umrandung. Antialias-Rand außerhalb der konfigurierten Breite; transparente Selbstüberlappung mit dem VTM-üblichen Tiefenpuffer-Schutz. Start-/Endpunkte bleiben feste Bildschirmgrößen mit weißem Rand.
- Beim Anbieterwechsel neue Rasterebene ausdrücklich unter der Streckenebene einfügen. Der bestehende GL-Streckenlayer bleibt erhalten und wird nicht von später angefügten Tiles verdeckt.
- GPS-Daten, Aufnahme, Analyse, Kartenquelle und Kamerasteuerung unverändert. Die GPS-Informations-View ist weiterhin separat; dieser Auftrag verschiebt die Strecken und Endmarker in den gemeinsamen GL-Frame.

## Gezielte Verifikation

Nur fünf betroffene JVM-/Robolectric-Fälle, nach Änderungen betroffene Fälle erneut geprüft; keine Gesamtsuite:

1. Drei unabhängige Stile/Deckkraft plus separate Livefahrt mit echter GPS-Lücke werden an GL publiziert; Android-View zeichnet die Linien nicht mehr.
2. Eingefrorener GL-Frame behält seine Projektion, obwohl UI-Kamera bereits300/-180Pixel weiter ist. Nächster GL-Frame transformiert dieselben gecachten Vertices passend zur Karte. Kleine Bewegung ohne Neuaufbau, große mit Rebase.
3. Unveränderte GPS-/Kompass-/Höhenpixel über Rotation/Zwischenzoom.
4. Anbieter-/Keywechsel behält denselben Streckenlayer oberhalb der Rasterquelle.
5. Offscreen-Clipping, getrennte GPS-Segmente und Linie über die Datumsgrenze.

Logs: `/workspace/.tools/vtm-route-frame-tests.log`, `vtm-route-frame-final-tests.log`, `vtm-route-clip-test.log`, `vtm-route-final-compile.log`.

Zusätzlich tatsächlichen GLES-Shader in einem lokalen Mesa/EGL-Kontext kompiliert/gelinkt und Pixel geprüft:40%-roter Kern auf grauem Grund, weiße Umrandung, unveränderter Außenbereich und grüner Endmarker mit weißem Rand. Prüfskript `scripts/check-vtm-route-shader.py`, Log `/workspace/.tools/vtm-route-shader-check.log`. Das ist ein Software-GLES-Nachweis, kein Test auf dem Smartphone. Shader nutzt explizit übereinstimmende Integerpräzision in Vertex-/Fragmentstufe. Optional ausführen mit beschreibbarem `MESA_SHADER_CACHE_DIR`; System-EGL/Mesa erforderlich, keine APK wird gebaut.

Debug-Kompilation, Typografie/Atlas und Diff geprüft. Tatsächliches schnelles Ziehen/Nachlaufen, Pinch und Quellenwechsel mit der Android-GPU anschließend am Gerät bestätigen. Noch keine neue APK beauftragt/gebaut; veröffentlicht bleibt0.8.79/Build120. Neuer APK-Build ausschließlich auf ausdrücklichen Auftrag über GitHub Actions. Es gibt jetzt26 Shader einschließlich des neuen App-Shaders; bei der nächsten APK-Prüfung berücksichtigen.
