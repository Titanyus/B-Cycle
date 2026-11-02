# Aktueller Stand: APK 0.8.83 / Build 124 mit Bright-JSON-Zuordnung veröffentlicht

## Aktueller Auftrag abgeschlossen: Bright als Basis und neue APK

Nutzer liefert zweimal dieselbe bright.json und bestätigt ausdrücklich: diese Vorlage möglichst genau übernehmen, vorerst keine eigenen Farb-/Breitenkorrekturen. Unverändert als `assets/styles/bright.json` gespeichert, SHA-256 cda8931095c5101cbad5d2a60fa129600a1a0ad3dea27d802be341fb5eae071f. [Aktuelle Zuordnung, Pflege und Grenzen](vtm-bright-style.md).

- Generator `python3 scripts/build-bright-vtm.py` und `--check`: native XML, 111 lokale Spritevarianten aus referenziertem Atlas, Bericht aller 116 Ebenen. Kein Live-Stileditor/neuer Kartendienst; OpenFreeMap bleibt. VTM-Theme nur über `VtmBrightTheme.load` laden, damit Originalfilter einschließlich numerischer Vergleiche gelten. Keine Änderungen am erzeugten XML von Hand.
- Farben/Deckkraft, Ebenenfolge, Straßenränder/Füllung, Tunnel/Brücken/Auffahrten, Zoomfunktionen, Breiten/Kappen/Dashes übernommen. Gleicher Maßstab: MapLibre-Zoom = VTM-Zoom − 1. Halbe VTM-Extrusionsbreite/feste Breiten statt zusätzlicher nativer Breitenmultiplikation. Schemaalias minor_road/minor und ramp→*_link. Flach bleiben.
- Vorlage selbst: Wald 10 % Deckkraft, keine eigene Ackerfarbe, track mit minor/service, path dünn/gestrichelt. Das ersetzt ausdrücklich die kräftigeren Flächenfarben aus Build 123; keine eigenen Overrides. Nutzer bestätigt nochmals JSON als verbindliche Basis, Mapnik-Vergleich nur Hinweis. Nutzer bietet App-/Maputnik-Screenshots an; gleicher Ausschnitt/Maßstab/Nord/Draufsicht erbeten, noch keine eingegangen.
- Grenzen dokumentiert: ganzzahlige Zoomstufen, native Mindestbreite/Antialiasing/Dashquantisierung, Android-Schriften/lokalisierte Namen/Kollision statt Noto-Remote-Glyphen; keine exakte MapLibre-Textplatzierung/Abstände/Halo-Blur. building-top und water-offset ausgelassen, Wasserpattern vorhanden. Originale Daten/Tracks/GPS/Rasterquellen/Profile erhalten; Radhervorhebung weiterhin getrennte Option, für Referenzvergleich aus.
- Vier gezielte Fälle erfolgreich; nach Ergänzungen nur zwei betroffene Fälle wiederholt. Generator/Kompilation/Typografie/Atlas/Diff erfolgreich. Logs bright-json-style-tests.log / bright-json-final-tests.log in /workspace/.tools/. Kein Gesamtlauf. Neue APK anschließend ausdrücklich beauftragt und über GitHub Actions gebaut/veröffentlicht: [0.8.83 / Build 124](build-124.md), code 153. Physischer Bildvergleich offen.

Voriger Stand Build 123: eigene Flächenfarben, ausgehende Karte im Dashboardbild korrigiert (setWillNotDraw(false)), Hauptmenü-Trenner je 12 dp. Diese Korrekturen erhalten. Build 124: neue JSON/XML/111 Spritevarianten/Referenzfarben bytegenau geprüft; die überholten kräftigen Flächenfarben aus Build 123 gelten nicht mehr.

## Erhaltener Stand aus Build 122

Neue APK ausdrücklich beauftragt, ausschließlich über GitHub Actions gebaut und geprüft. [Build 122](build-122.md), [Änderungen](release-0.8.81.md).

- Akkusparmodus je Fahrradprofil direkt nach Radumfang, Standard aus; Profil-JSON/Backup/Booleanvalidierung. Automatische Karte/Dashboard 1 Hz, kurze Diagramme 5 s, Gesamtverlauf weiter 10 s. Sichtbare Graphen vorbereiten; statische Aufnahme-/Zonen-/Warn-/Chevron-Darstellung. Manuelle Gesten, Sensoren/Rohdaten/Aufzeichnung/Analyse unverändert. Bestehenden Bright-Stil nicht umschalten. Neun gezielte Fälle erfolgreich, nach Ergänzung nur drei betroffene Fälle erneut erfolgreich. [Details](profile-battery-saver.md).
- Bright als flache VTM-Adaption, keine direkte MapLibre-JSON-Ausführung; Kennung VECTOR erhalten. Immer Draufsicht, keine Extrusionen; Nord-/Fahrtrichtung/Radwege erhalten. Stil-/Draufsichttest erfolgreich. [Details](vtm-bright-style.md).
- Standort/Genauigkeit/Kompass/Höhe im gleichen GL-Frame wie Karte/Strecken; keine zeichnende Android-Kartenoverlayfläche. Kleine wiederverwendete Texturen. Live-Strecken-Fallback bis neue Worker-Geometrie bereit, bei Aufnahme-/Profilgeneration/Ausblenden verwerfen. Drei gezielte Fälle und echter Mesa/GLES-Pixeltest erfolgreich. [Details](vtm-gps-frame-sync.md).
- Dashboard-/HRV-Nachbarseite beim Ziehen sichtbar, Auswahl/Datum/Chart erst beim Commit. Eine Karteninstanz: unabhängige begrenzte GL-Aufnahme oder aus kartenloser Seite umhängen. Richtungswechsel/Abbruch/späte Antworten abgesichert. Sechs gezielte Fälle erfolgreich. [Details](dashboard-page-transition.md).
- Eine Stressfrage mit acht Positionen 0–7 statt Doppelung. Je zwei Stufen→Intervals 1–4. Bestätigte alte Regelmäßigkeitsrichtung invertieren, Originale erhalten; neue Antwort zuerst, sonst inverse Regelmäßigkeit, sonst alte Vierfachantwort. Fragebogen-/Auswahlschema 3, alte 1/2 lesen. HRV/Indizes/Empfehlungsgewichtung unverändert. Sechs gezielte Fälle inklusive echtem Backup erfolgreich. [Details](hrv-eight-step-stress.md).
- Gemeinsame Farbpalette immer mit FOREGROUND-Auswahlrahmen; schwarze Kontrastalternative entfernt, auch Track3.

Kompilation/Typografie/Atlas/Diff erfolgreich. Kein Gesamtlauf, keine lokale APK. Wirkliche Android-GPU-Ausgabe, schnelle Gesten, Map-Snapshot/Umhängen, Wischgefühl und Akkueffekt noch am Smartphone prüfen. Keine gemessene Akkuersparnis.

## Letzte veröffentlichte APK

[APK 0.8.83 / Build 124](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.83-release-124.apk), code 153, Quelle `f088f6bdc9f95ced2c18643bfc33cfe705034115`, 4204369 Bytes, SHA-256 `ba5fdf923684083fe0f6c067de96b8a84ba1567c81377b1643e07ae2e308e69d`. Dauerhafte Signatur, CI-Prüfsumme, Version/Manifest, ZIP-/ARM64-16-KB-Ausrichtung, vier ABIs, 27 Shader sowie Original-JSON/generierte XML/111 Spritevarianten/Referenzfarben geprüft. [Buildnachweis](build-124.md).

Vorige APK: [0.8.82 / Build 123](build-123.md), davor [0.8.81 / Build 122](build-122.md). Rückfallstand vor Enginewechsel: [Meilenstein 0.8.78 / Build 119](milestone-0.8.78.md), dauerhaft gesichert; nicht verändern.

## Weitere Planung

- Kartenstil im Akkusparmodus ausdrücklich noch nicht wechseln. Minimal: kleine Straßen/Feldwege ausblenden, große Straßen erhalten; Orte/Wasser/Wald/Position/gewählte Strecken bleiben. OLED-/Sparstil und mögliche Dashboardoption noch offen, nicht gebaut. Alle Karten vorerst Draufsicht.
- Maputnik als visueller Browsereditor besprochen; MapLibre-JSON nicht direkt VTM-XML-kompatibel. Kein Live-Stilimport beauftragt.
- Sensorfrequenz/weniger Aufzeichnung waren nur Verständnisfragen: niemals aus Akkuwünschen eine Drosselung von GPS/BLE, Rohdaten oder Analyse ableiten.
- Nächste APK nur auf ausdrücklichen Auftrag ausschließlich GitHub Actions, voraussichtlich 0.8.84 / code 154 / Build 125. Appcode nie mit [skip ci] überspringen. Dauerhafte Signatur, 27 Shader/Bright-Assets und echten direkten Download prüfen.
