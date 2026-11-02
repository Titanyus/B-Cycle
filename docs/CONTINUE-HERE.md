# Aktueller Stand: APK 0.8.82 / Build 123 veröffentlicht

## Aktueller Auftrag abgeschlossen

Neue APK ausdrücklich beauftragt, ausschließlich über GitHub Actions gebaut, Signatur/Prüfsumme/Assets geprüft und veröffentlicht. [Build 123](build-123.md), [Änderungen](release-0.8.82.md).

- Bright: deckender grüner Wald, hellgrüne Wiesen, gelbgrüne Felder; farmland-/scrub-Regeln und fehlenden wetland-Stil ergänzt. Zwei betroffene Vektorkartentests erfolgreich. [Details](vtm-bright-style.md).
- Alte Karte verschwand beim Loslassen: VTMs setWillNotDraw(true) übersprang beim Aufnehmen des Elternbaums die Software-Snapshot-Routine. BicycleMapView setzt jetzt false; vorhandene GL-Aufnahme wird ins ausgehende Seitenbild gezeichnet. Neuer Bildtest reproduzierte den Fehler; nach Fix drei betroffene Übergangs-/Abbruch-/Umhängfälle erfolgreich. Keine zusätzliche Karteninstanz oder Übergangsaufnahme. [Details](dashboard-page-transition.md).
- Gruppentrenner im Haupt-Einstellungsmenü auf ausdrücklichen Wunsch je 12 dp oben/unten (vorher je 32). Menüzeilen und erweitertes Menü unverändert. Ein bestehender Menütest erfolgreich.

Typografie/Atlas/Diff erfolgreich; kein Gesamtlauf. Tatsächlichen PixelCopy-/Compositor-Übergang und Kartenfarben am Smartphone noch bestätigen. Testlogs in /workspace/.tools/: vtm-bright-rural-tests.log, page-outgoing-map-fix-tests.log, settings-divider-12dp-test.log.

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

[APK 0.8.82 / Build 123](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.82-release-123.apk), code 152, Quelle `1730b8182c22e6931c919f6f1c97a7ad72eb9eea`, 4075100 Bytes, SHA-256 `298c19b2bd27f7a1c49f9755415a875908cb2320f5936d1b7b51d87f6a207a10`. Dauerhafte Signatur, CI-Prüfsumme, Version/Manifest, ZIP-/ARM64-16-KB-Ausrichtung, vier ABIs, 27 Shader und neue Bright-Flächenfarben geprüft. [Buildnachweis](build-123.md).

Vorige APK: [0.8.81 / Build 122](build-122.md), davor [0.8.80 / Build 121](build-121.md). Rückfallstand vor Enginewechsel: [Meilenstein 0.8.78 / Build 119](milestone-0.8.78.md), dauerhaft gesichert; nicht verändern.

## Weitere Planung

- Kartenstil im Akkusparmodus ausdrücklich noch nicht wechseln. Minimal: kleine Straßen/Feldwege ausblenden, große Straßen erhalten; Orte/Wasser/Wald/Position/gewählte Strecken bleiben. OLED-/Sparstil und mögliche Dashboardoption noch offen, nicht gebaut. Alle Karten vorerst Draufsicht.
- Maputnik als visueller Browsereditor besprochen; MapLibre-JSON nicht direkt VTM-XML-kompatibel. Kein Live-Stilimport beauftragt.
- Sensorfrequenz/weniger Aufzeichnung waren nur Verständnisfragen: niemals aus Akkuwünschen eine Drosselung von GPS/BLE, Rohdaten oder Analyse ableiten.
- Nächste APK nur auf ausdrücklichen Auftrag ausschließlich GitHub Actions, voraussichtlich 0.8.83 / code 153 / Build 124. Appcode nie mit [skip ci] überspringen. Dauerhafte Signatur, 27 Shader/Bright-Assets und echten direkten Download prüfen.
