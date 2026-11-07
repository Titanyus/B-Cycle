# Fahrtnotiz und subjektive Anstrengung (07.11.2026, nach Build145)

## Daten und Bedienung

Fahrtbezogene Notiz und optionale ganzzahlige RPE0–10 unabhängig vom HRV-Tagesbefinden in `ride-annotations` nach unveränderlicher Fahrt-ID. Keine Bewertung ist nicht dasselbe wie0. Grobe CR10-Anker:0 keine Anstrengung,1 sehr leicht,2 leicht,3 moderat,4 etwas schwer,5–6 schwer,7–9 sehr schwer,10 maximal. Subjektiver Gesamteindruck, keine Berechnung aus Puls/Leistung. Kein Umskalieren oder Migrieren vorhandener HRV-Antworten.

HRV-Frage ausdrücklich „Wie anstrengend war dein Training gestern für dich? (RPE0–10)“. Bestehende Vortagssummen über alle Fahrten bleiben; die dortige Tagesbewertung wird weiterhin nur beim Tagesbefinden gespeichert. Keine Übernahme, Mittelung oder Verknüpfung mit Fahrt-RPE. Mittlerer RPE-Anker dort „Schwer“.

Zusammenfassung: Name der gespeicherten Fahrt als anklickbarer Titel, vorhandene automatische Umbenennung verwenden. Statistikwerte ohne Icons; sonstige Details weiterhin mit Icons. Über „Zu den Details“ gemeinsamer RPE-Slider und vierzeiliges Notizfeld in voller Inhaltsbreite, normale Body-Schrift, Platzhalter „Bemerkung oder Notiz eintragen“.

Details: RPE rechts neben Rad/Datum als schmale große Zahl/Bedeutung in derselben52dp-Höhe wie das Herkunfts-/Format-Symbol. Antippen öffnet Slider. Notiz mit horizontalem Trenner zwischen Identität und Wetter, normale Schrift/Größe wie Datum; ohne Text weder Notiz noch zusätzlicher Trenner. Erster Footerpunkt „Notiz“ öffnet die Inline-Eingabe oben, statt die Karte zu öffnen. Die Karte bleibt über ihr Vergrößern-Icon erreichbar. Fokuswechsel, Tastatur schließen oder Done beenden die Bearbeitung; Eingaben bleiben schon während des Schreibens gespeichert. Feldrahmen dann durch normalen Text ersetzt.

SharedPreferences.apply aktualisiert lokal sofort und schreibt atomar asynchron; Android wartet bei Lifecycle-Stop auf ausstehende Änderungen. Jeder Text-/Sliderwechsel speichert ohne Bestätigung. Beim Verlassen wird die Hintergrundarbeit zusätzlich angestoßen. Dauerhaftes Dirty-Merkmal, Wiederaufnahme beim Appstart, serieller Worker bündelt externe Übertragungen nach1,2s. Fehler beim Aktualisieren einer externen Datei löschen keine lokalen Eingaben und werden am Editor angezeigt. Notizen begrenzt auf6000 Zeichen. Löschen der Fahrt entfernt Feedback und zugehörige Aufgaben.

## Dateien und Sicherungen

- Interne Original-/Analyse-FIT/GPX-Dateien unverändert. Nur öffentliche Exportkopien erhalten Feedback; Identität, Rohdaten und Speicherfeldfilter bleiben erhalten.
- FIT: Session `workout_rpe` (Feld193) = CR10×10, ergänzende UTF-8-Notiz als mehrteilige `memo_glob`-Nachrichten mit Präfix `B|CYCLE note`. Bytewerte explizit unsigned; exakte Definition pro Teil verhindert ungültige FF-Füllbytes im letzten Teil. Ein externer FIT-Leser muss Memo-Nachrichten unterstützen, um den Text anzuzeigen.
- GPX-Einzelexport: Feedback als namensraumgebundene `https://bcycle.app/feedback/1`-Erweiterung, Originalgeometrie und vorhandene Erweiterungen erhalten. Zusammenfassungs-JSON: `bcycleFeedback`.
- Streckensicherung lokal/WebDAV: validierter optionaler `feedback`-Block in der Fahrt-Metadatei. Original-/Vollquellen bleiben unverändert, damit Wiederherstellung und Dublettenprüfung weiter bytegenau funktionieren. Alte Backups ohne Block weiterhin lesbar; Wiederherstellung startet keine Cloudänderungen. HRV-Sicherung separat unverändert.
- Einzelne Downloads/gewählter Ordner: Exporte enthalten Feedback. Seit dieser Änderung geschriebene Kopien werden über ihre konkrete Dokument-URI nachbearbeitet, statt pro Tastendruck neue Dateien anzulegen. Bereits aus älteren Builds exportierte Dateien haben keine gespeicherte URI; diese einmal erneut exportieren. URI-Zugriffe werden nicht als übertragbare Backupberechtigung behandelt.
- Bereits vorgemerkte/gesicherte WebDAV-Fahrten: geänderte öffentliche FIT-Kopie unter demselben Namen erneut vormerken, vorhandene Hash-/Generationsprüfungen erhalten. Es werden keine zusätzlichen Aktivitätsuploads zu Strava/Dreeve/Intervals ausgelöst.

## Cloud-Felder und Wiederholung

Bestehende kontogebundene RideNames-/JobService-Warteschlange um getrennte Annotation-Aufgaben erweitert. Neue bestätigte Remote-Verknüpfungen erhalten vorhandenes Feedback, spätere Änderungen aktualisieren bereits verknüpfte Aktivitäten. Eigene Aufgabenkennung/Token verhindert Überschreiben neuerer Aufgaben; Kontowechsel, WLAN-Präferenz und bestehende Wiederholungsregeln beachten. Keine Änderung von Sensordaten, keine neuen Fahrten nur wegen einer Notiz.

Vor dem Schreiben Beschreibung lesen; eigenen `[B|CYCLE]…[/B|CYCLE]`-Block ersetzen, sonstigen Text erhalten. Reservierte Marker im Notiztext bei der Cloud-Darstellung entschärfen; lokaler Text bleibt unverändert. Entfernen der Notiz entfernt den eigenen Notizblock. Unbeantwortetes RPE überschreibt kein fremdes RPE-Feld.

- **Intervals.icu:** PUT `/api/v1/activity/{id}` mit `description` und vorhandenem `icu_rpe`. `session_rpe` ist kein Ersatz für die subjektive Eingabe und wird nicht geschrieben.
- **Strava:** PUT `/api/v3/activities/{id}` mit `description`. Die App hat eine Anstrengungsbewertung, das öffentliche `UpdatableActivity`-Modell stellt dafür aktuell kein schreibbares Feld bereit. CR10 als gekennzeichneter Text „RPE: n/10“ im eigenen Beschreibungsblock, keine Umdeutung anderer Felder.
- **Dreeve:** PATCH `/api/v1/activities/{id}` unterstützt `description`, aber kein separates RPE-Feld. Notiz und „RPE: n/10“ im eigenen Block. Ältere Server müssen die Beschreibungsänderung unterstützen; API-Fehler bleiben sichtbar, lokal nichts verwerfen. Ein Feature-Request für ein separates RPE-Feld wäre sinnvoll; es wurde keiner veröffentlicht.

Geprüfte öffentliche Quellen am07.11.2026:

- [Intervals OpenAPI](https://intervals.icu/api/v1/docs): Activity.description/icu_rpe sowie GET/PUT activity.
- [Strava UpdatableActivity](https://developers.strava.com/swagger/activity.json): description, kein schreibbares perceived_exertion.
- [Dreeve ActivityUpdateRequest](https://github.com/dreeveapp/dreeve/blob/HEAD/src/Controller/Api/V1/Activity/Update/ActivityUpdateRequest.php): name/description/sportType/gearId.
- [GoldenCheetah FIT-Leser](https://github.com/GoldenCheetah/GoldenCheetah/blob/master/src/FileIO/FitRideFile.cpp): Session-Feld193 als RPE/10; offizielles Garmin-SDK21.218.0 für Encoding/CRC.

## Gezielte Prüfung

RideFeedbackTest: sofortiges Speichern/0 statt unbewertet/Inline-Edit/Normaltext, FIT-RPE und Unicode-Memo mit erhaltenen Messwerten/Originalbytes, MockWebServer für alle drei Cloudadapter, Beschreibungen erhalten und idempotent ersetzen/entfernen. Vorhandener RideDirectoryBackupTest um Feedback-Roundtrip ergänzt; RideSummaryTest und RouteDetailActionsTest für Zusammenfassung, Notizfooter und Kartenwiederverwendung; RideNameSyncTest für bestehende Queue-/Kontowechselregeln; gezielter HRV-Vortagssummenfall. 21 gezielte Fälle insgesamt grün; Nachweis `/workspace/.tools/ride-feedback-map-tests.log`. Native Zusammenfassungsvorschau `app/build/reports/ui/ride-summary.png` kontrolliert, normale Schrift für Notizen gegen globale Neuformatierung abgesichert. Keine Gesamtläufe, kein persönlicher Cloud-Kontozugriff, keine neue APK ohne Auftrag.
