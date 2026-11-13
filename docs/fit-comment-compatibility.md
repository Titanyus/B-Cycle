# FIT-Höhenkommentar und Viewer-Kompatibilität · 13.11.2026

Eine private Nutzer-FIT wird vom Garmin-SDK gelesen und besteht dessen ActivityFileValidationPlugin ohne fehlgeschlagene Prüfungen; Header-/Datei-CRC gültig. FITFileViewer wirft dagegen rawFieldValue in Bi/Ci beim Verarbeiten des memo_glob. Exakt mit dessen öffentlichem Originaldecoder reproduziert; vor Übergabe der privaten Dateibytes war sämtliche Browser-Netzwerkkommunikation gesperrt.

Ursache ist der von FitElevationCorrection angehängte Höhenquellenkommentar über das von Garmin als veraltet markierte MemoGlobMesg.memo-Feld. Der Viewer erwartet stattdessen data und field_num und dereferenziert sie ungeprüft. Das ist ein Kompatibilitätsfehler und kein Nachweis beschädigter Fahrdaten. Nutzerauftrag: Höhenkommentar weglassen, Lizenz-/Quellenangaben weiter in Erweitert erhalten.

FitElevationCorrection schreibt den Kommentar nicht mehr und entfernt beim erneuten Korrigieren nur die eigene alte Mapterhorn-Attributionsnachricht. Andere Kommentare/Fahrtnotizen und moderne MemoGlob-Nachrichten bleiben erhalten. Garmin-Validierung und Regression für unveränderte andere Messfelder, eigene Altkommentare und fremde Kommentare in fit-memo-and-zone-strip-guard-tests.log.

Private bereinigte Kopie liegt ausschließlich unter workspace/shared/downloads/B-Cycle-bereinigt.fit. Binär genau die eine Attributions-Datennachricht entfernt, Datenlänge/CRCs angepasst; alle anderen Original-Datensatzbytes erhalten. Zusätzlich sämtliche übrigen dekodierten Standard-/Entwicklerfelder gegen das Original verglichen. Original und Kopie bestehen Garmin-Aktivitätsvalidierung; Kopie vom unveränderten FITFileViewer-Decoder erfolgreich gelesen. Keine privaten FITs/Standorte/Sampletabellen ins Repository übernehmen. Private Nachweise in .tools/np-nov13.

Die Bereinigung ergänzt keine NP und ändert keine Messwerte. Die separate Live-Pausenerkennung ist in ConfirmedBikePause korrigiert; fehlende historische Pausenbelege werden nicht stillschweigend aus der FIT erfunden. Andere veraltete memo_glob-Schreibpfade für Fahrtnotizen bleiben funktional erhalten; ihre eventuelle Viewer-Kompatibilität ist unabhängig vom konkreten Höhenkommentar.
