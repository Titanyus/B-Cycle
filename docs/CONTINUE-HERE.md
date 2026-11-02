# Aktueller Stand: GPS/Live-Strecke und Nachbarseitenvorschau lokal korrigiert

## Aktueller Auftrag nach Build121

Nutzer: gespeicherte Strecken bewegen sich perfekt mit; Standort/Höhe ruckeln, Live-Strecke prüfen. Lokal umgesetzt: alle GPS-Elemente in VtmGpsLayer im GL-Frame, wiederverwendete kleine Glyphen und aufrechtes Höhenlabel, keine zeichnende Android-Kartenoverlayfläche mehr. Live-Strecke behält vorbereitete Geometrie während Worker-Neuberechnung, mit eigener Aufnahme-/Profilgeneration gegen alte Linien. [Umfang/Prüfung](vtm-gps-frame-sync.md). Drei gezielte Fälle und echter Mesa/GLES-Shader-Pixeltest erfolgreich, nur offenen Worker-Test nach Synchronisierung wiederholt; Kompilation/Typografie/Atlas/Diff erfolgreich. Noch keine neue APK beauftragt; nicht pushen. Nächste APK **27 Shader**, einschließlich neuem `bcycle_gps.glsl`.

Zusätzlicher Auftrag ebenfalls umgesetzt: Dashboard-/HRV-Nachbarseite läuft schon während des Ziehens herein. Schreibgeschützte native Vorschau mit echter Nachbar-Konfiguration/-Tagesauswertung; Auswahl/Datum/Chart erst beim Commit. Eine Karteninstanz: aktuelle Karte als begrenzte unabhängige GL-Aufnahme oder aus kartenloser Seite umhängen, bei Abbruch/Commit zurücksetzen. Abbruch/Richtungswechsel/verspätete Antworten abgesichert. Sechs gezielte Fälle erfolgreich in `page-neighbor-tests.log`. [Umfang/Grenzen/Prüfung](dashboard-page-transition.md). Kompilation/Typografie/Atlas/Diff erfolgreich. Keine APK beauftragt; nicht pushen.

## Letzter APK-Auftrag02.11.2026 abgeschlossen

Neue APK ausdrücklich beauftragt, über GitHub Actions gebaut und geprüft. [Build121](build-121.md), [Änderungen](release-0.8.80.md).

- OpenFreeMap-Vektorkarte ohne API-Key, **OpenFreeMap · Liberty**. Liberty-Adaption für VTM, nicht pixelgleiche MapLibre-Ausführung; flache Gebäude ab Zoom13. Immer Draufsicht, native Tilt-Grenzen0 und keine Extrusionen. Nord-/Fahrtrichtung erhalten. [Stil](vtm-liberty-style.md).
- Radwegehervorhebung je Fahrradprofil, Backup und gemeinsame Dashboard-/Detailkarten. Vorhandene Rasterkarten erhalten. MVT-Metadaten/Datensatz-URL auf Ladethreads, begrenzter gemeinsamer HTTP-Cache. [Quelle](vtm-vector-map.md).
- Gespeicherte Strecken/Livefahrt/Start-/Endmarker im selben VTM-GL-Frame, gecachte Geometrien und vorhandene Stile/Lücken. GPS-Informations-View bleibt separat. [Nachlaufkorrektur](vtm-route-frame-sync.md).
- Dashboard-/HRV-Seiten bis UP am Finger; dann halbe Breite plus Geschwindigkeitsprojektion für schnelle Flicks. Richtungsumkehr möglich, keine Restanimation nach vollständig manuellem Hinausschieben. [Wischen](dashboard-page-transition.md).

Gezielte Tests je Änderung erfolgreich; nur betroffene/offene Fälle wiederholt, kein Gesamtlauf. Shader zusätzlich lokal mit Mesa/GLES geprüft. Release-R8/Ressourcenprüfung und CI erfolgreich. Keine lokale APK gebaut. Android-Darstellung/Lesbarkeit, schnelle Kartenbewegung, Wischgefühl, PixelCopy und App-/Dialogrückkehr noch am Smartphone prüfen. Sensoren/Aufzeichnung/Analyse unverändert.

## Letzte veröffentlichte APK

[APK0.8.80/Build121](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.80-release-121.apk), code150, Quelle`6061cc6379cdce87c5125463bf56cd377b7f8327`, 4074378Bytes, SHA256`44daf0c56e46b407459e18df420757376e022e3d3e24b702ee22aa458096b554`. Signatur/Version/Manifest/CI-Prüfsumme/ZIP-/ARM64-16-KB-Ausrichtung geprüft; vier ABIs,26 Shader, Liberty-XML/Lizenz und MVT-Schema nach R8 nachgewiesen. Tatsächlichen Raw-Direktdownload geladen und binär identisch mit Actions-Artefakt bestätigt. [Buildnachweis](build-121.md).

## Vorige APK: VTM mit Rasterkarten

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

## Voriger Build120

[APK0.8.79/Build120](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.79-release-120.apk), code149, Quelle`66a487b5f490df3834644a21b96d97caac309a96`, 2870449Bytes, SHA256`b8e9daffb07e871c836a33d501943328a97170333d589c6b306a0d31aabe980e`. Signatur/Version/Manifest/CI-Prüfsumme/16-KB-ZIP-Ausrichtung geprüft; vier native ABIs und25 Shader in APK vorhanden, JNI-Klassenname nach R8 erhalten. Tatsächlicher Raw-Direktdownload erneut geladen und binär identisch mit dem Artefakt bestätigt. [Buildnachweis](build-120.md).

## Gesicherter Rückfallstand

[APK0.8.78/Build119](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.78-release-119.apk), code148, Quelle`521e012a3e37ed9cf1dd85fb8e9c856b8ef57bb9`, SHA256`998fa47f4765b1a0079ff78160c9c99d3ec4ec70cf7a52fc09fece9592103977`. Dauerhafte Signatur, Version, Manifest, Ausrichtung und tatsächlicher Raw-Download geprüft. [Buildnachweis](build-119.md). Enthält noch Mapsforge, keine VTM-Migration.

Auf Nutzerwunsch als [Meilensteinv0.8.78](milestone-0.8.78.md) per annotiertem Tag/GitHub-Release gesichert. APK/Prüfsumme und R8-Mapping dauerhaft versioniert/revisionsgebunden verlinkt; direkter Release-Asset-Upload war am Upload-EndpunktHTTP401 gescheitert, Repository-Sicherung erfolgreich.

Build119 enthält HRV-Dropdown/volle Kacheln/kompakte Empfehlung/höheres Messdiagramm, aufgeräumte HRV-/Fahrradeinstellungen, Sensoranzeige nach Zuordnung/Suchfund und Aufnahmeanzeige mit ruhenden Zwischenphasen. Build118: Sticky-Summen, aktualisierender manueller Intervals-HRV-Sammelupload, größere Windrose, weiche Tages-/Dashboardgesten, gemeinsame Indexhistorie mit14-Tage-Hinweisen,±4 und HRV-Verlauf. [Build118](build-118.md).

## Festgehaltene spätere Planung

[Akkuanalyse](dashboard-energy-analysis.md): später ein gemeinsamer Sparmodus für Anzeige/Animationen – Karte1–2s, Zahlen1Hz, Graphen5–10s/keine Skalenanimation, statische funktionale Farben/Warnungen, OLED-Vektorstil. Trainingshilfe ausdrücklich aktuell weich, harte Farben erst im Sparmodus. Nutzer ausdrücklich: Sensorfragen nur aus Neugier; Sensorerfassung, Aufzeichnung und Analyse bleiben vollständig/zuverlässig. VTM jetzt beauftragt/umgesetzt, Online-Vektorquelle noch offen. Keine privaten Messdaten ins Repo.

Nächste APK nur auf Auftrag ausschließlichGitHubActions, voraussichtlich0.8.81/code151/Build122. Dauerhafte Signatur und tatsächlichen Download prüfen; Appcode nie mit[skip ci] überspringen.


## Weitere vorgemerkte Kartenstile

Minimal: kleine Straßen/Feldwege ausblenden, große Straßen erhalten, Ortschaften/Wasser/Wald/Position/Strecken bleiben. Noch nicht gebaut. Maputnik als visueller Browsereditor besprochen; MapLibre-JSON nicht direkt VTM-XML-kompatibel, kein Live-Stilimport beauftragt. Alle Karten vorerst in Draufsicht.
