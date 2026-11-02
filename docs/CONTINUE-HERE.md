# Aktueller Stand: Flächenfarben nach APK 0.8.81/Build 122 korrigiert

## Aktuelle Korrektur: ausgehende Karte beim Loslassen

Nutzer: Neue Karte scrollt herein, alte verschwindet beim UP. Reproduzierter Bildtest: VTMs `setWillNotDraw(true)` überspringt im Elternbaum unsere Software-Snapshot-Routine. `BicycleMapView` setzt jetzt false; vorhandene PixelCopy-Aufnahme wird dadurch ins ausgehende Seitenbild gezeichnet. Keine neue Vorschau-/Animationsarchitektur. Neuer Bildtest vor Änderung fehlgeschlagen, nach Änderung erfolgreich; dazu zwei bestehende Abbruch-/Umhäng-Fälle erfolgreich (3/3). [Details](dashboard-page-transition.md), Log `/workspace/.tools/page-outgoing-map-fix-tests.log`. Noch keine neue APK.

Zusatzfrage: Haupt-Einstellungsmenü hat bei seinen Trennern derzeit je 32 dp oben/unten (2 × sectionDividerGap 16); Menüzeilen je 6 dp vertikales Padding, Mindesthöhe 54 dp. Je 16 dp vorgeschlagen, noch nicht geändert; Nutzer fragt zunächst nach aktuellem Maß.

## Aktuelle Änderung nach Build 122

Bright: Wald jetzt deckend grün, Wiesen hellgrün, Ackerflächen gelbgrün; fehlende farmland-/scrub-Regeln und undefinierten wetland-Stil ergänzt. Ursache: Wald bisher nur 10 % Deckkraft, Ackerflächen ohne Regel. [Details](vtm-bright-style.md). Zwei betroffene Vektorkartentests erfolgreich (0 Fehler), ebenso Typografie/Atlas/Diff. Log: `/workspace/.tools/vtm-bright-rural-tests.log`. Noch keine neue APK beauftragt/gebaut; letzter veröffentlichter Build bleibt 122.

## Auftrag 02.11.2026 abgeschlossen

Neue APK ausdrücklich beauftragt, ausschließlich über GitHub Actions gebaut und geprüft. [Build 122](build-122.md), [Änderungen](release-0.8.81.md).

- Akkusparmodus je Fahrradprofil direkt nach Radumfang, Standard aus; Profil-JSON/Backup/Booleanvalidierung. Automatische Karte/Dashboard 1 Hz, kurze Diagramme 5 s, Gesamtverlauf weiter 10 s. Sichtbare Graphen vorbereiten; statische Aufnahme-/Zonen-/Warn-/Chevron-Darstellung. Manuelle Gesten, Sensoren/Rohdaten/Aufzeichnung/Analyse unverändert. Bestehenden Bright-Stil nicht umschalten. Neun gezielte Fälle erfolgreich, nach Ergänzung nur drei betroffene Fälle erneut erfolgreich. [Details](profile-battery-saver.md).
- Bright als flache VTM-Adaption, keine direkte MapLibre-JSON-Ausführung; Kennung VECTOR erhalten. Immer Draufsicht, keine Extrusionen; Nord-/Fahrtrichtung/Radwege erhalten. Stil-/Draufsichttest erfolgreich. [Details](vtm-bright-style.md).
- Standort/Genauigkeit/Kompass/Höhe im gleichen GL-Frame wie Karte/Strecken; keine zeichnende Android-Kartenoverlayfläche. Kleine wiederverwendete Texturen. Live-Strecken-Fallback bis neue Worker-Geometrie bereit, bei Aufnahme-/Profilgeneration/Ausblenden verwerfen. Drei gezielte Fälle und echter Mesa/GLES-Pixeltest erfolgreich. [Details](vtm-gps-frame-sync.md).
- Dashboard-/HRV-Nachbarseite beim Ziehen sichtbar, Auswahl/Datum/Chart erst beim Commit. Eine Karteninstanz: unabhängige begrenzte GL-Aufnahme oder aus kartenloser Seite umhängen. Richtungswechsel/Abbruch/späte Antworten abgesichert. Sechs gezielte Fälle erfolgreich. [Details](dashboard-page-transition.md).
- Eine Stressfrage mit acht Positionen 0–7 statt Doppelung. Je zwei Stufen→Intervals 1–4. Bestätigte alte Regelmäßigkeitsrichtung invertieren, Originale erhalten; neue Antwort zuerst, sonst inverse Regelmäßigkeit, sonst alte Vierfachantwort. Fragebogen-/Auswahlschema 3, alte 1/2 lesen. HRV/Indizes/Empfehlungsgewichtung unverändert. Sechs gezielte Fälle inklusive echtem Backup erfolgreich. [Details](hrv-eight-step-stress.md).
- Gemeinsame Farbpalette immer mit FOREGROUND-Auswahlrahmen; schwarze Kontrastalternative entfernt, auch Track3.

Kompilation/Typografie/Atlas/Diff erfolgreich. Kein Gesamtlauf, keine lokale APK. Wirkliche Android-GPU-Ausgabe, schnelle Gesten, Map-Snapshot/Umhängen, Wischgefühl und Akkueffekt noch am Smartphone prüfen. Keine gemessene Akkuersparnis.

## Letzte veröffentlichte APK

[APK 0.8.81/Build 122](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.81-release-122.apk), code151, Quelle `6ecbd52ae58a0296fe15d08348e237a7cfe79ba2`, 4075040 Bytes, SHA256 `8301a0ac5a7a88f55157deda4aeba35db37b08517c9989c3afce042788ad3b0e`. Signatur/Version/Manifest/CI-Prüfsumme/ZIP-/ARM64-16-KB-Ausrichtung geprüft; vier ABIs, 27 Shader, Bright-XML/Lizenz und MVT-Schema nach R8 nachgewiesen. [Buildnachweis](build-122.md).

Vorige APK 0.8.80/Build 121: [Nachweis](build-121.md), noch Liberty und kein allgemeiner Akkusparmodus. Build 120 war die erste VTM-APK mit Rasterkarten. Rückfallstand vor Enginewechsel: [Meilenstein0.8.78/Build 119](milestone-0.8.78.md), dauerhaft per annotiertem Tag/GitHub-Release und revisionsgebundenen APK-/Mapping-Links gesichert. Nicht verändern.

## Weitere Planung

- Kartenstil im Akkusparmodus ausdrücklich noch nicht wechseln. Minimal: kleine Straßen/Feldwege ausblenden, große Straßen erhalten; Orte/Wasser/Wald/Position/gewählte Strecken bleiben. OLED-/Sparstil und mögliche Dashboardoption noch offen, nicht gebaut. Alle Karten vorerst Draufsicht.
- Maputnik als visueller Browsereditor besprochen; MapLibre-JSON nicht direkt VTM-XML-kompatibel. Kein Live-Stilimport beauftragt.
- Sensorfrequenz/weniger Aufzeichnung waren nur Verständnisfragen: niemals aus Akkuwünschen eine Drosselung von GPS/BLE, Rohdaten oder Analyse ableiten.
- Nächste APK nur auf ausdrücklichen Auftrag ausschließlich GitHub Actions, voraussichtlich 0.8.82 / code 152 / Build 123. Appcode nie mit [skip ci] überspringen. Dauerhafte Signatur,27 Shader/Bright-Assets und echten direkten Download prüfen.

Downloadnachweis: öffentlicher Raw-Link nach Veröffentlichung abgerufen, 4075040 Bytes und SHA-256 bytegenau identisch zum Actions-Artefakt.
