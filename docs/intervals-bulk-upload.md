# intervals.icu: alle Fahrten prüfen und fehlende übertragen

26.10.2026 nach Build90. Daten & Speicher → intervals.icu → Alle Fahrten zu intervals.icu hochladen. Vorhandene Dienstkonfiguration/API-Key/WLAN-Vorgabe nutzen; kein neues Setting. Ein Button wechselt während des Laufs zu Abbrechen. Fortschritt und Bilanz hochgeladen/vorhanden/übersprungen/unklar/fehlgeschlagen, konkrete dienstweite Fehlermeldung und bis20 problematische Fahrtnamen. Dialogschließen beendet weitere Verarbeitung; bei erneuter Ausführung erneut prüfen. Bei aktiver Aufnahme/Speichern/HRV oder beschäftigter Bibliothek nicht starten. Keine APK angefordert.

## Umfang und Dateien

Eigene RECORDED-Fahrten plus bestätigte Dreeve-Importe mit lokalen FIT- oder vollständig zeitgestempelten GPX-Dateien, auch Herkunft IMPORTED. Keine Umklassifizierung oder Veränderung von Originalen. Reine JSON-Zusammenfassungen und nicht als gefahren belegte geplante Tracks überspringen. Für diesen expliziten Auftrag kann RouteTransferPayload.file(allowDreeveImport=true) bestätigte Dreeve-GPX mit echten Messzeiten konvertieren; alle anderen Aufrufer und automatischen Uploads behalten die vorherige Berechtigungsprüfung. Keine Weiterleitung an andere Dienste.

Der bestehende MapState-Bibliotheksworker hält einen stabilen Katalogsnapshot. IntervalsUploads.serial teilt den Upload-Lock mit dem automatischen Dienst, verhindert parallele POSTs. Ausstehende/fehlgeschlagene Uploadaufträge derselben Fahrt werden vor deren Prüfung vom expliziten Sammellauf übernommen; sie dürfen nach einem bereits gefundenen oder unklaren Treffer nicht blind im Hintergrund erneut senden. Bei Abbruch/Fehler erneut den Button verwenden; noch nicht bearbeitete Fahrten bleiben unangetastet.

## Dublettenprüfung

1. Bereits gespeicherte intervals-Verknüpfung zum aktuellen Athleten über GET /activity/{id} prüfen. Nur bestätigtes404 darf den fehlenden Link entfernen; Netzwerk-/Anmeldefehler bedeuten nicht fehlend.
2. Ohne vorhandene Aktivität FIT-Identität aus Session-start_time/total_elapsed_time/total_distance, ersatzweise echten Recordzeiten lesen; niemals file_id.time_created mit Fahrtbeginn verwechseln. CRC/Größe/Recordanzahl/Zeitgrenzen prüfen.
3. GET /athlete/{id}/activities mit oldest/newest/limit1000 und gezielten Feldern, pro UTC-Kalendermonat einmal gecacht einschließlich je einem Randtag. Antworten ab1000 Einträgen oder ungültige Daten als Fehler behandeln; keine Abwesenheit aus möglicherweise abgeschnittener Liste ableiten. API-UTC verwenden, sonst start_date_local plus Zeitzone, zuletzt Gerätezeitzone. Fahrradtypen berücksichtigen.
4. Stabile external_id oder eindeutiger Treffer mit Start±2s, verstrichener Dauer±2s und Distanz±max(100m,1%). Fehlende Distanz nur passend, wenn beide Seiten keine haben. Bei mehreren Treffern oder gleichem Start ohne ausreichende Übereinstimmung: unklar, kein Upload. Andere Exporte/Zeitzonen können so eine manuelle Prüfung erfordern; keine absolute Dublettengarantie für fehlerhafte/veränderte Quelldaten.
5. Nur fehlende Fahrt als FIT senden: external_id=bcycle:<stabile lokale ID>, Anzeigename als URL-Parameter, vorhandene Original-/Transferdatei. Neben älteren Einzel-/Arrayantworten auch aktuellen UploadResponse-Wrapper mit activities[0].id verstehen; dessen Root-id kann Uploadkennung sein. Kein Überschreiben vorhandener Remote-Namen beim GET-Treffer.

Erkannte Präsenz ist eine RideServiceLink-Verknüpfung, kein erfundener Dateiversions-Erfolgsbeleg. Nur bestätigter POST erzeugt exakten Uploadbeleg. Existiert eine Remote-ID bereits an einer anderen lokalen Dublette, nicht still umhängen. Bestätigte neue Uploads in den internen Trefferbestand aufnehmen. Datei-/Profiltyp-/Konfigurations-/Schlüssel-/WLAN-/Abbruchkontrollen vor/nach Netzoperationen. Wiederholung nach verlorener Uploadantwort ist über external_id und erneute Prüfung abgesichert.

## Dreeve-Haken

Recorded-FIT-Importe aus Dreeve hatten bereits eine bestätigte Herkunftsverknüpfung, aber keinen eigenen Uploadbeleg. RouteTransferRows übersah den Link nur bei RECORDED+FIT und bot erneuten Upload an. Diese Kombination zeigt nun ebenfalls Mit Dreeve verknüpft/Haken, auch bei ausgeschaltetem Upload. Kontobindung bleibt, laufende oder gescheiterte eigene Dateiversionsaufträge behalten ihren Status. intervals zeigt bestätigte Sammelimport-Verknüpfungen entsprechend; keine Änderung an Dateien/Herkunft/Remote-Aktivitäten.

## API-Nachweise und Grenzen

Öffentliche intervals.icu-Dokumentations-URLs antworteten in dieser Umgebung mit403; kein privates Nutzerkonto abgefragt und keine Zugangsdaten verwendet. Parameter/Antworttypen anhand öffentlicher API-Clients gegengeprüft, keine fremden Implementierungen übernommen:

- https://github.com/paladini/node-intervals-icu/blob/main/src/services/activity.service.ts
- https://github.com/paladini/node-intervals-icu/blob/main/src/types/activity.ts
- https://github.com/futureweb/garmin-intervals-bridge/blob/main/src/garmin_intervals_bridge/intervals.py

Reale intervals-/Dreeve-Daten müssen am Gerät geprüft werden. Gezielte MockWebServer/Robolectric-Tests statt Gesamtsuite.

## Verifikation

44 unterschiedliche gezielte Tests erfolgreich:7 Sammelupload-/Zuordnungsfälle,15 Sensorscannerfälle (SDK33/35/36),9 bestehende intervals-Fälle,5 Uploadmenü-,6 Nachprüfungs- und1 Transferpayload-Fall sowie1 Einstellungsnavigationstest. Keine Gesamtsuite. Logs /workspace/.tools/intervals-bulk-final-tests.log (bestehende Regressionen), intervals-bulk-sram-verified.log (Sammelupload/Scanner abschließend grün) und intervals-bulk-ui-check.log (Sessionidentität/echter Einstellungsbutton). Ein Groß-/Kleinschreibungsfehler einer synthetischen Bluetooth-Adresse im neuen Test korrigiert. Kotlin-/Typografie-/UI-Katalog-/Diffprüfung erfolgreich.
