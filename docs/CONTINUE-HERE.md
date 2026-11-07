# Aktueller Entwicklungsstand: gemeinsame Mapterhorn-Höhenquelle (noch nicht veröffentlicht)

Auftrag07.11.2026 umgesetzt: Höhenlinien von Mapzen/AWS auf Mapterhorn umgestellt, gemeinsamer Downloader/validierter512-Pixel-WebP-Decoder/Cache mit der Fahrtkorrektur. Linien nativ10–14, darüber vorhandene Geometrie; regionale Detail-404 fallen auf12 zurück. Korrektur unverändert bilinear14→12 und bisherige Grenzen. Nur Darstellung/Quellenzugriff vereinheitlicht, keine Änderung von Aufzeichnung oder Höhenmeterzählung. Quellenanzeige/Attribution samt offiziellen regionalen Datenlizenzen aktualisiert. [Details, Auflösung und Schummerungsprüfung](mapterhorn-unified-dem.md).

16 gezielte Tests aus MapterhornElevationsTest, MapVectorLayersTest, MapPresentationSettingsTest und FitElevationCorrectionTest bestanden; zusätzlich ungültige Bildantwort geprüft. Kachelanschlüsse, gemeinsamer Cache,404-Fallback, Abbruch/Dispose, FIT-Korrektur und Quellenanzeige; zentrale Typografie-/UI-Katalog-/Bright-Prüfungen grün. Zwei veraltete UI-Testannahmen an die bereits bestehenden Aufzeichnungs-/Detailseiten angepasst. Echte Mapterhorn-Kacheln aus Bayern auf12/14 erfolgreich als512×512WebP abgerufen. Kein Gesamtlauf, keine APK angefordert/gebaut; Geräteprüfung der Höhenlinien über Zoomwechsel offen.

Fertige externe Schummerung nur geprüft, noch nicht integriert: Esri World Hillshade256JPEG technisch/optisch geeignet, echte Probe erfolgreich; Esri-Lizenz und Offline-/Cache-Grenzen vor Einbau klären. Mapterhorn-Endpunkt liefert DEM, keine vorberechnete Schummerung. Kein neuer aktiver Anbieter. Trainingsvarianten und spätere Intervallerkennung bleiben nur vorgemerkt.

# Aktueller Stand: APK0.8.101 / Build146 veröffentlicht

[APK](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.101-release-146.apk) · [Buildnachweis](build-146.md) · [Änderungen](release-0.8.101.md)

Fahrtfeedback enthalten: Zusammenfassung mit umbenennbarem Fahrtnamen, Statistik ohne Icons, eigener Fahrt-RPE0–10 und vierzeilige autospeichernde Notiz in normaler Schrift. Detailkopf rechts RPE/Meaning, Notiz vor Wetter, Footer „Notiz“ für Inline-Edit. HRV-Vortagssummen und eigener Gesamteindruck separat, keine Übernahme von Fahrt-RPE. Dateien/Streckensicherung enthalten Feedback; alte lokale Exporte ohne URI einmal neu exportieren. Intervals eigenes RPE-Feld, Strava/Dreeve Beschreibung mit markiertem RPE-Text, vorhandene Beschreibungen erhalten. [Umfang/Grenzen](ride-feedback.md).

Kartenkorrektur enthalten: vorhandene Detailkarte für Vollbild wiederverwenden, keine dritte MapView. Gemeinsame EGL-Share-Group/GL-State-Abgrenzung/GPU-Reihenfolge für VTM-weit geteilte Puffer-/Shader-IDs. [Befund](vtm-detail-fullscreen-contexts.md). 21 gezielte Tests und zentrale Quellenprüfungen grün, Actions-Build/Artefaktprüfung erfolgreich. Kein Gesamtlauf oder lokale APK. Optischer Geräte-GPU-Test des Kartenwechsels und persönliche Cloud-Kontoprüfung offen.  Trainingsvarianten und spätere Intervallerkennung weiterhin nur vorgemerkt.

Direkten veröffentlichten Raw-Download tatsächlich abgerufen: 4387299 Bytes, bytegenau identisch zum geprüften Actions-Artefakt; SHA-256 b91de0ff6202c68d25bae2f5770b48c9043e22e8f7dfa86253edc813fce20c7b ebenfalls identisch.
