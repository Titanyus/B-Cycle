# Aktueller Stand: Online-Vektorkarte ergänzt; Strecken-Nachlauf korrigiert

## Aktueller Auftrag02.11.2026

Nutzer: echte Vektorkarte ergänzen; Frage nach OSM-Grundkarte mit Fahrrad-Layer. Lokal umgesetzt: zusätzliche Quelle **OpenFreeMap · Vektorkarte** ohne API-Key, optional **Radwege hervorheben** aus denselben Vektordaten. Rasterquellen bleiben wählbar; bestehende Auswahl unverändert. Fahrradprofilumfang/Backup, Beschriftungen, Dashboard und Streckenvorschau/-vollbild gemeinsam angebunden. Ausgeschilderte Radroutennetze sind noch keine vollständige zusätzliche Ebene. [Umfang/Prüfung](vtm-vector-map.md).

Sechs betroffene Fälle gezielt geprüft (echte MVT-Kachel/Theme, Metadaten/URL, Vektor-Raster-UI/Lebenszyklus/Backup, drei bestehende Raster-/Anbietertests). Label-Zoomlimit-Konflikt korrigiert, betroffenen Fall erfolgreich wiederholt. Release-R8 und Java-Ressourcenzusammenführung erfolgreich; fehlende SLF4J-Anbindung durch stille Bindung behoben. Typografie/Atlas/Diff erfolgreich. Keine APK beauftragt, nicht auf main pushen. Nächste APK enthält zusätzlich Theme-/MVT-Ressourcen und weiter26 Shader einschließlich `shaders/bcycle_route.glsl`; Publikationsprüfung anpassen. Smartphone-Darstellung noch prüfen.

## Ebenfalls lokal fertig: Strecken-Nachlauf

Nutzer: VTM funktioniert grundsätzlich, Strecken hinken beim schnellen Ziehen hinterher; danach „bitte weitermachen“. Ursache getrennte GL-/Android-Zeichenflächen. Strecken/Livefahrt und Start-/Endmarker jetzt im gemeinsamen VTM-GL-Frame mit gecachten Vertexdaten. Eigener kleiner Shader erhält Breite/Farbe/Deckkraft/weiße Livefahrt-Umrandung; GPS-Informations-View bleibt separat. Raster beim Anbieterwechsel unter Strecken einfügen. [Umfang/Prüfung](vtm-route-frame-sync.md).

Fünf betroffene Fälle und lokaler Mesa/GLES-Shader-Pixeltest erfolgreich; finale Debug-Kompilation/Typografie/Atlas/Diff. Kein Gesamtlauf, keine neue APK beauftragt, nicht auf main pushen. Nächste APK enthält26 statt25 Shader einschließlich `shaders/bcycle_route.glsl`; Prüfschema anpassen. Schnelles Ziehen auf Android-GPU noch bestätigen.

## Bereits veröffentlicht: VTM-Umstellung


Nutzer hat nach Sicherung des Meilensteins den Austausch Mapsforge → Mapsforge-VTM beauftragt: alle Dashboardkartenfunktionen erhalten, zunächst dieselben Raster-Tiles; invertierte Nachtkarte vorerst unwichtig. Implementierung abgeschlossen, gezielt geprüft. APK-Auftrag0.8.79/code149 erfolgreich über GitHub Actions erfüllt; [Build120](build-120.md). [Vollständiger Umfang/Prüfnachweise](vtm-migration.md).

- Stabile VTM0.25.0-Android-/HTTP-Abhängigkeiten und vier native ABIs, OpenGL ES2. Alte Mapsforge-Map-/Core-Abhängigkeit/Imports entfernt. Anbieter/URLs/Keys/Attributionen unverändert, begrenzter geteilter HTTP-Cache.
- Kamera, bestehende Präferenzschlüssel und Zoom6–19/0,5-Schritte; OpenTopoMap17; gemeinsame Pinch-/Pan-/Nachlaufsteuerung. GPS-Folgen, Norden/Fahrtrichtung, Vorausschauanker und33-ms-Interpolation erhalten.
- Drei gespeicherte Strecken plus Livefahrt als native transparente Overlays mit VTM-Projektion, unabhängigem Stil und echten Lücken. Geometrie auf Worker nur bei Punktlistenänderung vorbereiten, Generationsschutz/Profile erhalten. GPS/Genauigkeit/Kompass/Höhe/Windrose sowie Bedienelemente/Editor/Vollbild erhalten.
- Dashboardübergang: PixelCopy der GL-Fläche höchstens4MB/200ms, anschließend bestehendes Seitenbild höchstens16MB. Abbruch/verspätete Antwort abgesichert; bei fehlendem Bild sofortiger Wechsel. Weiterhin eine lebende Dashboardkarte. Detailvorschau/-vollbild ebenfalls VTM, Lebenszyklus beim Wiederanfügen erhalten.
- Nacht-Invertierung/Button deaktiviert, gespeicherte Option und Stilbezug erhalten. Kein allgemeiner Sparmodus, keine Vektorquelle und keine Sensor-/Aufzeichnungsänderung.

## Prüfnachweise

19 betroffene Fälle gezielt geprüft; nach gefundenen Abweichungen nur offene Fälle wiederholt. Kein Gesamtlauf. Native VTM-Projektion mit echtem JNI, Pixel-/Gesten-/Lebenszyklus-/Anbietertests; GL-Aufnahmeablauf getestet. Alte Cache-/Framebuffer-Tests an neue Engine angepasst. `vtm-*.log` unter `/workspace/.tools/`, Details in [Migration](vtm-migration.md).

Debug-Kompilation und Release-Native-/Java-Ressourcenzusammenführung erfolgreich (kein APK-Build): vier ABIs und25 Shader, ARM64 mit16-KB-Alignment. Typografie/Atlas/Diff erfolgreich. JNI für Robolectric pro Sandbox über identische Linuxbibliothek isoliert; Shadow passt nur das Laden und die Android-Testumgebung an.

Noch am Smartphone prüfen: tatsächliche GL-Tilezeichnung, SurfaceView-Clipping/Überlagerungen/PixelCopy beim Dashboardwechsel, Wischgefühl, mehrere Kartenansichten und App-/Dialogrückkehr. Lokale Tests sind kein GPU-/Akkunachweis.

## Letzte veröffentlichte APK

[APK0.8.79/Build120](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.79-release-120.apk), code149, Quelle`66a487b5f490df3834644a21b96d97caac309a96`, 2870449Bytes, SHA256`b8e9daffb07e871c836a33d501943328a97170333d589c6b306a0d31aabe980e`. Signatur/Version/Manifest/CI-Prüfsumme/16-KB-ZIP-Ausrichtung geprüft; vier native ABIs und25 Shader in APK vorhanden, JNI-Klassenname nach R8 erhalten. Tatsächlicher Raw-Direktdownload erneut geladen und binär identisch mit dem Artefakt bestätigt. [Buildnachweis](build-120.md).

## Gesicherter Rückfallstand

[APK0.8.78/Build119](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.78-release-119.apk), code148, Quelle`521e012a3e37ed9cf1dd85fb8e9c856b8ef57bb9`, SHA256`998fa47f4765b1a0079ff78160c9c99d3ec4ec70cf7a52fc09fece9592103977`. Dauerhafte Signatur, Version, Manifest, Ausrichtung und tatsächlicher Raw-Download geprüft. [Buildnachweis](build-119.md). Enthält noch Mapsforge, keine VTM-Migration.

Auf Nutzerwunsch als [Meilensteinv0.8.78](milestone-0.8.78.md) per annotiertem Tag/GitHub-Release gesichert. APK/Prüfsumme und R8-Mapping dauerhaft versioniert/revisionsgebunden verlinkt; direkter Release-Asset-Upload war am Upload-EndpunktHTTP401 gescheitert, Repository-Sicherung erfolgreich.

Build119 enthält HRV-Dropdown/volle Kacheln/kompakte Empfehlung/höheres Messdiagramm, aufgeräumte HRV-/Fahrradeinstellungen, Sensoranzeige nach Zuordnung/Suchfund und Aufnahmeanzeige mit ruhenden Zwischenphasen. Build118: Sticky-Summen, aktualisierender manueller Intervals-HRV-Sammelupload, größere Windrose, weiche Tages-/Dashboardgesten, gemeinsame Indexhistorie mit14-Tage-Hinweisen,±4 und HRV-Verlauf. [Build118](build-118.md).

## Festgehaltene spätere Planung

[Akkuanalyse](dashboard-energy-analysis.md): später ein gemeinsamer Sparmodus für Anzeige/Animationen – Karte1–2s, Zahlen1Hz, Graphen5–10s/keine Skalenanimation, statische funktionale Farben/Warnungen, OLED-Vektorstil. Trainingshilfe ausdrücklich aktuell weich, harte Farben erst im Sparmodus. Nutzer ausdrücklich: Sensorfragen nur aus Neugier; Sensorerfassung, Aufzeichnung und Analyse bleiben vollständig/zuverlässig. VTM jetzt beauftragt/umgesetzt, Online-Vektorquelle noch offen. Keine privaten Messdaten ins Repo.

Nächste APK nur auf Auftrag ausschließlichGitHubActions, voraussichtlich0.8.80/code150/Build121. Dauerhafte Signatur und tatsächlichen Download prüfen; Appcode nie mit[skip ci] überspringen.
