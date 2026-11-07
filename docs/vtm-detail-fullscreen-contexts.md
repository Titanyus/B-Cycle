# Detailkarte vergrößern: VTM-Ressourcen (07.11.2026, nach Build145)

## Befund

Dashboard-Vollbild verwendet dieselbe MapPanel-/BicycleMapView-Instanz. Details-Vollbild legte dagegen neben Dashboard und Vorschau eine dritte RouteAnalysisMap mit neuem GL-Kontext an. VTM0.25.0 verwaltet u.a. `MapRenderer.mQuadIndicesID`, `mQuadVerticesID`, NativeBufferPool, BufferObject-Pools, Bucket-Shader und GLState-Caches statisch. Unabhängige EGL-Kontexte haben unabhängige Objekt-IDs; ein zufällig gleicher Zahlenwert kann in der anderen Karte auf eine andere Textur oder einen anderen Puffer zeigen. Das passt zu kaputten Labels/Flächen und einem nach Rückkehr ebenfalls beschädigten Dashboard.

## Änderung

1. Details-Vollbild verwendet die vorhandene Vorschau einschließlich derselben BicycleMapView/Layer weiter. Vorübergehender begrenzter PixelCopy-Platzhalter im ursprünglichen Slot; Interaktion/Zoomcontrols einschalten, Kamera beim Zurückkehren wiederherstellen. Keine dritte Karteninstanz. PixelCopy-Fehler dürfen die Vergrößerung nicht blockieren. Doppelte Öffnungen und veraltete Capture-Antworten nach Verlassen der Details abweisen. Snapshot auf Rückkehr freigeben. Dashboard-Gestenpfad und permanenter Kartenhost unverändert.
2. Alle BicycleMapViews verwenden eine EGL-Share-Group. Somit sind VTM-weit gespeicherte Textur-/Puffer-/Programm-IDs für die verbleibenden Karten gültig. Bei gemeinsam genutzten Kontexten Zeichenabläufe und GL-Queue-Arbeit serialisieren; GLES2-glFinish stellt dabei auch die GPU-Reihenfolge sicher. CPU-Lock allein reicht bei getrennten GPU-Queues nicht. Diese Synchronisation ist nur bei mehr als einem existierenden Kontext nötig.
3. Bei Kontext-/Kartenwechsel GLState und die acht garantierten GLES2-Attribute zurücksetzen; Viewport und Hintergrund pro Karte einstellen. Zusätzliche Label-Fade-Attribute dürfen nicht in die andere Karte durchsickern.
4. Prozessweite VTM-Shader-/Pufferinitialisierung einmal pro lebender Share-Group; weitere/erneut angehängte Oberflächen nutzen bestehende gemeinsame Objekte. Letzten Kontext korrekt entfernen, Gruppe erst nach letzter Freigabe zurücksetzen; kein ungeteilter Ersatzkontext bei fehlgeschlagener Share-Erstellung.

Adapter BicycleGlContexts sitzt am bereits vorhandenen GLSurfaceView-Renderer-Hook, bevor dessen Thread gestartet wird. Native VTM-Klassen/Dateiformate/Shader unverändert; kleiner BCycleGlReset-Bridge für das paketlokale GLState.init. Kein Umhängen der Dashboardkarte während einer Wischgeste.

## Prüfung und Grenze

BicycleGlContextsTest verwendet aufgezeichnete EGL-/GL-Aufrufe: geteilte Kontextnamen, Freigabe und erneutes Teilen mit verbleibendem Kontext, unabhängige Viewports, Attributreset, GPU-Abschluss und serialisierte parallele Zeichenanforderungen. RouteDetailActionsTest prüft gleiche RouteAnalysisMap-/BicycleMapView-Identität vor/nach Vollbild und Rückkehr, zusätzlich Notiz-/Umbenennen-/Export-/Löschbedienung.

UI-JVM-Tests besitzen keinen Android-EGL-Treiber; expliziter Headless-GLSurfaceView-Shadow verhindert unkontrollierte native GL-Threads. Offizielle Khronos-EGL-Interfaces ausschließlich als Testabhängigkeit, nicht im APK. Die Adapterprüfung nutzt einen Recording-GL statt einer Geräte-GPU. Optische Bestätigung von Namen/Flächen beim Zoomen und anschließender Rückkehr zum Dashboard bleibt ein Gerätetest. Log `/workspace/.tools/ride-feedback-map-tests.log`.
