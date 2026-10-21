# Typisierte Speicherergebnisse

Erste interne Entkopplung des Fahrtabschlusses: `RecordingStore` liefert aus `export()` ein `RideCompletion` und verwendet für Fortschritt/Fehler `RecordingUpdate`. Die Typen sind ausschließlich im Speicher; es gibt keine neue Datei oder Formatversion.

- `Completed`: bisheriger vollständiger Abschluss einschließlich aktivierter Nacharbeiten. Ziele beschreiben lokalen Export bzw. Upload-Vormerkung, keinen bestätigten Remote-Erfolg. Warnungen für unvollständige Aufzeichnung und fehlgeschlagene optionale Höhenkorrektur bleiben erhalten.
- `LocallyRetained`: Original-FIT wurde bereits privat gesichert, ein späterer Schritt schlug fehl. Bekannte Fahrt-ID, Fehlerphase und bekannte Warnungen bleiben verfügbar. Journal-/Retry-Verhalten unverändert.
- `NotSaved`: Fehler vor erfolgreicher privater FIT-Sicherung; keine bestätigte Fahrt-ID.

`Recovered` zählt nur `Completed` als vollständig abgeschlossen. Ein lokal erhaltener Teilerfolg wird weiterhin nicht im Zähler „Fahrten gespeichert“ mitgezählt. Beide Exportziele werden wie bisher unabhängig versucht, bevor Fehler ausgewertet werden; fehlgeschlagener Ordnerexport verhindert keine Upload-Vormerkung.

`RecordingStore` meldet `RecordingUpdate` über den Main-Thread-Callback an `RideRuntime`. Die Laufzeit übernimmt den Busy-Zustand und aktualisiert die Bibliothek anhand von Finished/Recovered; Meldungstexte werden nicht ausgewertet. `recordingStatus` bleibt als abgeleiteter Anzeigetext für Einstellungen und Foreground-Benachrichtigung erhalten. Bei einer neuen Aufnahme wird das vorherige Speicherereignis zurückgesetzt.

`RecordingStatusText` erzeugt die bisherigen Meldungen über Stringressourcen und entscheidet anhand des Ereignistyps über die bisherige lange Rückmeldung. `MainActivity` unterdrückt weiterhin identische aufeinanderfolgende Meldungen; Fortschritt und Hinweise auf fehlende Journale/offene Fahrten bleiben ohne Toast. Keine fachliche Entscheidung hängt von einem Textpräfix ab.

Unverändert: Verarbeitungsschritte/Reihenfolge, Worker, Aufnahmetakte, Speichergrenzen, Journal-/FIT-Schema, Profile, Namensvorlagen, Uploadwarteschlangen und Benutzeroberfläche. Aufzeichnungsfeldkatalog und Messwert-/Dashboard-Entkopplung sind noch nicht umgesetzt.

## Referenztests

`RecordingFieldsTest` prüft den geordneten Feldkatalog einschließlich Einheiten, Scope, Beschriftungen, Gruppen und Standardauswahl sowie die Reichweitenzuordnung. `recording-fields.txt` und `legacy-recording.ride` wurden vor der Produktionsänderung aus `3e7db0e` erzeugt; das Journal enthält ausschließlich synthetische Messwerte. Vorhandene Altdefinitionen werden bewusst erhalten.

`RecordingStoreBaselineTest` prüft exakte Meldungen und Busy-Flags, vollständiges Speichern, Teilerfolg/Retry mit unveränderten Originalbytes, gemischte Wiederherstellung, ungültiges Journal und Schreibfehler mit Erhalt vollständiger Samples. `RecordingResultTest` prüft zusätzlich tatsächliche typisierte Exportergebnisse und Fehlerphasen, lokale Identität, Upload-Vormerkung statt Remote-Erfolg und die unveränderte Darstellung.

Schritt 3 ergänzt `RecordingUpdateUiTest`: reale Activity mit gesperrtem START-/Profilwechsel während Speichern, lange Fehlermeldung, keine mehrfachen Toasts bei weiteren Render-Aufrufen, stille Hinweise sowie vollständige/teilweise/fehlgeschlagene Abschlüsse und Sammelwiederherstellung. Runtime-Tests prüfen gezielte Bibliotheksaktualisierung. Vorhandener Service-/Hintergrundtest prüft zusätzlich das typisierte endgültige Ergebnis.
