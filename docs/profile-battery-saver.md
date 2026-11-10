# Akkusparmodus je Fahrradprofil (02.11.2026)

Nutzerauftrag: Schalter **Akkusparmodus** unmittelbar nach dem Radumfang, je Profil unabhängig. Standard aus; ausgeschaltet bisheriges Anzeige-/Animationsverhalten. Bestehenden Bright-Kartenstil unverändert lassen; Spar-Kartenstil und dessen spätere Dashboardoption noch nicht entscheiden. Diagramme frühestens alle5s aktualisieren, bereits längere Intervalle erhalten. Auch Chevrons ohne weiches Einblenden.

## Umsetzung

- `BikeProfile.powerSaving` ist ein Profilfeld, beim Lesen alter Profile standardmäßig false. Im Profil-JSON und in der Einstellungssicherung enthalten, als Boolean validiert. Kein globaler System-Akkusparschalter und keine neue Android-Berechtigung. Änderung/Profilwechsel greift ohne Neustart.
- `DisplayUpdateGate` bündelt reine Darstellung. Dashboard-/Sensorereignisse bei sichtbarem Dashboard höchstens1Hz; jeweils aktuellen Zustand lesen. Aufnahme-/Speicher-/Profil-/Sparmodusänderungen, Aufnahmefehlerstatus und Bike-Batteriestatus sofort behandeln. Einstellungen und andere sichtbare Verwaltungsansichten bleiben direkt; verdecktes Dashboard im Sparmodus nicht für jedes Sensorereignis rendern. Stopp entfernt wartende UI-Aktualisierung.
- Automatische Kartenbewegung, Standort/Höhe/Kompass und neue Trackanzeige über gemeinsamen1-s-Schritt. Keine33-ms-Interpolation oder animierter Vorausschauanker im Sparmodus. Native Karten-Gesten, Zoom und Seitenwischen bleiben unmittelbar. Kartendrehung/Folgenmodus des Nutzers bleiben erhalten, kein erzwungenes Norden-oben. Raster-/Vektor-Tile-Ladevorgänge und native Gesten dürfen zusätzliche notwendige GL-Frames auslösen; kein pauschales1-FPS-Limit der Engine.
- Kurze Diagrammfenster: Publikation5s statt1s. Gesamtverlauf behält10s; bei gemischten Fenstern keine zusätzliche5-s-Pfadaktualisierung eines unveränderten Gesamtverlaufs. Im Sparmodus nur sichtbare Metriken/Fenster vorbereiten, verdeckte Publikation aussetzen. Display-Historie nimmt trotzdem jeden bisherigen Punkt an; beim Wiederanzeigen aus vollständigem Displaypuffer aufholen. Auch Runtime ohne sichtbares Dashboard übermittelt den Spar-/Sichtbarkeitsstatus, damit Hintergrundaufnahmen keine unnötigen Graphdarstellungen aufbereiten.
- Diagrammskalen direkt statt3-s-Zwischenanimation. Training-/Bosch-Farben direkt statt Überblendung. Bike-Akkuwarnung als dauerhaft sichtbare Warnfarbe statt Atmen; bestehende Erkennung/Schwellen/Verbindungsvoraussetzung bleiben. Aufnahmesymbol als statischer roter Punkt, Stop-Aktion/Text erhalten. Chevrons in Datenkacheln und Kartenwerten sofort statt500-ms-Fade.

GPS-/BLE-Abfrage, Rohdatenannahme, Fahrt-/Leistungs-/Pulsberechnung, Zeitstempel, FIT/Journal und echte Streckenpunkte bleiben unverändert. Der Sparmodus verändert keine HRV-Messung/-Auswertung und keine Analyseformeln. Keine Bildschirmhelligkeits-/Displayfrequenzsteuerung, kein neuer Kartenstil. Keine gemessene Prozentersparnis behaupten; Wirksamkeit und Bediengefühl auf echter Fahrt prüfen.

## Prüfung

Neun gezielte Fälle erfolgreich: Profil-Schalterposition/Isolation/alte Standardwerte/echter Einstellungsbackup-Roundtrip; gebündelter letzter Zustand mit sofortigen Aktionen/Abbruch; gebündelte automatische Karte bei vollständig angenommenen GPS-Fixes und unmittelbarem Pan/Zoom;5-/10-s-Graphen, sichtbare Vorbereitung und vollständige Daten trotz unsichtbarer Publikation; statische Glows/Aufnahme/Chevrons/Graphskalen einschließlich sichtbarer Warnung. Bestehende echte Aufnahmeuhr mit künstlich langsamer UI im Sparmodus, normaler1-Hz-Rollingprovider und normale Bosch-Überblendung ebenfalls erfolgreich. Palette-Geometrie-/Helligkeitsfall erfolgreich.

Im Anschluss Karten-Chevrons und Runtime-Hintergrundpfad ergänzt; nur die drei betroffenen Karten-/Graph-/Aufzeichnungstaktfälle erneut ausgeführt, alle erfolgreich. Logs `/workspace/.tools/battery-saver-tests.log`, `battery-saver-final-tests.log`. Test-Aufräumaufruf vor Ausführung berichtigt; kein Gesamtlauf. Kompilation/Typografie/Atlas/Diff geprüft. Keine neue APK beauftragt und nicht auf main pushen.

## Farbauswahl

Unabhängiger kleiner Fix: ausgewählte Palettenfarbe immer mit FOREGROUND umranden; bisherige kontrastabhängige schwarze Alternative entfernt. Gilt über die gemeinsame Palette auch für Track3. Kein eigener Test für die kosmetische Einzeilenänderung; vorhandenen Palettenfall mit ausgeführt.

Allgemeine Effizienzkorrektur08.11.2026: unsichtbare Graphen werden auch im Normalmodus nicht mehr für die Anzeige vorbereitet; die vollständige Historie läuft weiter, Rückkehr wird sofort aktualisiert. Keine Änderung sichtbarer Normal-/Sparintervalle oder der Aufzeichnung. [Nachweis](efficiency-review-2026-11-08.md).

Korrektur10.11.2026: bei gemischten Kurz-/Gesamtfenstern bleiben Gesamtachse und Kurve als eigener Snapshot synchron, auch im Normalmodus und beim erneuten Binden. Intervalle unverändert. [Befund/Prüfung](mixed-graph-snapshots.md).
