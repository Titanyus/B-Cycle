# BICYCLE 0.7.27 – Benachrichtigungen und Indoor-Korrektur

Build: 16.10.2026 13:09:45 CEST; versionName 0.7.27, versionCode 65.
Quellstand: `1c055fd425418f5c131699a19a0b4a3313f378f1`. Tag: `v0.7.27`.

## Enthalten

- Benachrichtigungen direkt nach Design & UI: Hauptschalter, Android-Zugriff,
  einzelne App-Auswahl mit Messenger-Filter. Keine automatische App-Auswahl.
- Drei Positionskacheln im Dashboard-Layout-Stil (unten, mittig, oben), Sekunden-
  Slider 5–180 s, Default 60 s. Sofortige Speicherung der Konfiguration.
- Abgerundetes In-App-Fenster mit SECONDARY und 3-dp-ACCENT-Rahmen, farbigem
  App-Icon und Absender links oben. Scrollbarer erweiterter Nachrichtentext,
  maximal Kartenoberkante unten bzw. 320 dp oben/mittig; Systemleisten aussparen.
- Manuelles Schließen, nacheinander angezeigte Folgemeldungen, Touch pausiert Timer.
  Appwechsel/Deaktivierung verwerfen Nachrichten; keine Inhalte in FIT, Diagnosen
  oder dauerhafter Speicherung. Kein Overlay über anderen Apps.
- Indoor-Messwertkorrektur im Bikeprofil: unabhängige Faktoren für Leistung und
  Geschwindigkeit/Distanz, 0,0–3,0 in 0,1er-Schritten. Gleiche korrigierte Werte
  für Dashboard, Graphen, Training, Performance und neue FIT-Dateien. Faktorwechsel
  während Aufnahme/Speicherung gesperrt. Bestehende Fahrten bleiben unverändert.
- ZIP-Archivimport und alle Funktionen aus [0.7.26](milestone-0726.md) enthalten.

Details: [Benachrichtigungen](notifications-preparation.md),
[Indoor-Messwertkorrektur](indoor-ftms.md#messwertkorrektur-je-indoor-profil).

## Verifikation

Vollständige Testsuite: **529 Tests, keine Fehler oder übersprungenen Tests**.
Native Hochformat-UI einschließlich Menüreihenfolge, Appauswahl, Android-Freigabeintent,
Slider, drei Positionsanker/Kartenoberkante, Nachrichtentext, Deduplizierung,
Warteschlange/Timeout/Verwerfen und Erhalt offener Einstellungen geprüft.
Indoor-Korrektur einschließlich Persistenz, Live-Anzeige, Performance und FIT-Roundtrip.

Release-Build mit R8, Resource-Shrinking und Lint Vital erfolgreich. Bestehender
Testschlüssel, Version/minSdk/targetSdk und 16-KiB-ZIP-Ausrichtung geprüft.

Noch am Gerät prüfen: Android-Benachrichtigungszugriff und echte Zustellung aus
WhatsApp/Element usw., tatsächliche App-Icons und Lesbarkeit der drei Positionen.
Android/Quell-Apps können Texte kürzen oder sensible Inhalte verbergen; BICYCLE
kann nur den bereitgestellten Inhalt anzeigen. Indoor-Grundverbindung wurde vom
Nutzer bestätigt; Korrekturfaktoren und Langzeitverhalten bleiben praktisch zu prüfen.

## APK

[B-Cycle-0.7.27-test.apk](../downloads/B-Cycle-0.7.27-test.apk)

- 1,641,019 Byte
- SHA-256: `44d3337c56dc48c954a6bc0b8e8c71571ae3ca6772d9b65176fd6a4cb278364d`
- Als Update mit bestehendem Android-Testschlüssel installierbar.
- Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.
