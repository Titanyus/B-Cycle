# Aktueller Stand: APK0.8.78 / Build119 veröffentlicht –01.11.2026

[APK direkt](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.78-release-119.apk) · [Buildnachweis](build-119.md) · [Änderungen](release-0.8.78.md). Code148, Quelle`521e012a3e37ed9cf1dd85fb8e9c856b8ef57bb9`, 2592945Bytes, SHA-256`998fa47f4765b1a0079ff78160c9c99d3ec4ec70cf7a52fc09fece9592103977`. GitHubActions erfolgreich, Version/Manifest/ZIP-Ausrichtung/CI-Prüfsumme/dauerhafte Signatur geprüft.

## In Build119 enthalten

- Kurzempfehlung eigener Absatz, restlicher HRV-Text zusammenhängend. Messdiagramm315dp hoch,36dp Abstand zum Kreis.
- HRV-Kacheln füllen die gemeinsame Höhe, Textblock zentriert. Weitere öffnet rechtsbündiges schwebendes Dropdown über drei Hauptspalten, unveränderte Diagrammposition. Auswahl schließt, Weitere bleibt bei Zusatzkennzahl markiert.
- HRV & Tagesbefinden direkt nach Fahrradprofile; Erststartangebot einfacher Schalter nach Trenner vor Vibrieren, ohne Erklärung.
- Fahrradprofile/Karten: redundanter GPX-Anzeigeschalter entfernt, Dashboardsteuerung/gespeicherte Sichtbarkeit erhalten.
- Sensorübersicht: Bluetooth-Zeilen einschließlich Abstand nur bei Zuordnung oder aktuellem Suchfund. Ausgeschaltet/getrennt weiterhin sichtbar, erst Entfernen löscht Anzeige inklusive Suchfund. GPS bleibt immer. Alle freigeschalteten Profiltypen, keine Änderung an Erfassung/Verbindung.
- Aufnahmeanzeige: nur zwei500-ms-Übergänge animiert, dazwischen je1,5s Ruhe ohne Frame-Callbacks. Vier-Sekunden-Rhythmus und Kosinusüberblendung erhalten, Abbruch/Neustart und deaktivierte Systemanimation berücksichtigt. Trainingshilfe bleibt weich.

## Analyse und spätere Planung

[Dashboard-Energieanalyse](dashboard-energy-analysis.md) auf Nutzerwunsch dauerhaft festgehalten. Schätzung aus Code, keine Leistungsmessung. Karte bis30Bewegungsschritte/s einschließlich vollständiger Streckenpfade wahrscheinlich größter Grafikposten; Dashboard render pro Sensor-/GPS-/Tickereignis ohne zentrale Drossel; kurze Graphfenster1Hz mit möglichen16-ms-Skalenanimationen, Gesamtgraphen schon10s. Späterer einzelner Akkuschonmodus: Karte alle1–2s statt Zwischenframes, alle Auslöser bündeln; Zahlen1Hz; Graphen5–10s und keine Skalenanimation; statische funktionale Farbwechsel/Warnungen; OLED-Vektorstil und bedarfsgerechte unsichtbare Verarbeitung. Erfassung/FIT/GPS-Sampling unverändert.5s-Kartenstandard wegen35m Weg bei25km/h nicht empfohlen. Keine Akku-Prozentzusage.

VTM bislang nur analysiert, noch kein Umbau beauftragt. Mapsforge-Datenkompatibilität ist keine MapView-/Layer-API-Gleichheit; eigener GL-Surface und Dashboard-Snapshot prüfen. Trainingshilfe ausdrücklich erst im künftigen Sparmodus auf harte Farben umstellen. Kein allgemeiner Sparmodus implementiert.

## Gezielte Prüfnachweise

`hrv-refinement-tests.log`: fünf Fälle erfolgreich. Menüfall in `profile-sensor-settings-tests.log`, Sensorfall in `profile-sensor-visibility-tests.log` erfolgreich. `recording-cue-idle-tests.log`: Kurve, Indoor und Outdoor erfolgreich, neuer Zeitablauffall zunächst nicht wegen ungesteuerter Robolectric-Frames. Letzterer mit expliziten Frames in `recording-cue-idle-final-test.log` erfolgreich. Native HRV-Vorschau320dp visuell geprüft; Typografie/Atlas/Diff erfolgreich. Kein Gesamtlauf oder physischer Gerätetest.

## Bestehende Grundlagen und offene Gerätebestätigung

Build118: sichtbare Sticky-Summen, aktualisierender manueller Intervals-HRV-Sammelupload, größere Windrose, weiche Tages-/Dashboardgesten, eine gemeinsame Indexhistorie mit14-Tage-Hinweisen,±4-Indexdarstellung und HRV-Verlauf. [Build118](build-118.md). Build117: gefahrene Strecken/Vorlagen und28-Tage-Fahrzeitschätzung, gemeinsame Kartenplätze und gemeinsamer HRV-Tag/Verlauf. [Build117](build-117.md).

Physisches Wischgefühl, neue Dropdownbedienung/Animation und Intervals-Sammelaktualisierung am Smartphone noch bestätigen. Alte422-Bestandsmeldung laut Nutzer erledigt; Ursprung früherer Remote-Pulsabweichungen ohne Remote-Historie nicht belegt. Keine privaten Messdaten ins Repo. Weitere APK nur auf Auftrag ausschließlichGitHubActions; nächste Version voraussichtlich0.8.79/code149/Build120. Dauerhafte Signatur und tatsächlichen Raw-Download prüfen; Appcode nie mit[skip ci] überspringen.
