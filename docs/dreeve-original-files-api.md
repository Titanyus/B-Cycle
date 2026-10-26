# Dreeve-API: Originaldateien einschließlich FIT

Geprüft26.10.2026 gegen veröffentlichte **v5.7.0**, Quellstand `85638ed67f4e11c55caa247dc1358485c357da61`. Prüfung öffentlicher API-Dokumentation, Handler und Upstreamtests; keine Verbindung zum Nutzer-Server. Dessen installierte Version ist unbekannt.

Ja: Die aktuelle API bietet den unveränderten Download gespeicherter Originaldateien, auch FIT. Es ist keine aus GPX neu erzeugte FIT-Datei.

1. `GET /api/v1/file-imports?filters[activityId]=<activityId>` liefert passende Dateiimporte. Pagination beachten. Angaben: `id` als FileImport-ID, `filename`, `source`, `status`, `activityId`, `hasFile`, `importedOn`.
2. Bei passender Aktivität, erfolgreichem Import, Quelle `fitFile` und `hasFile=true`: `GET /api/v1/file-imports/{fileImportId}/file`.
3. Antwort200 ist `application/octet-stream` mit Originalbytes und Content-Disposition/Originaldateiname. Die API unterstützt Original-FIT, -TCX und -GPX.404 kann unbekannter Import oder fehlende gespeicherte Datei sein (`file_not_available`);401 ist ein Authentifizierungsfehler. Übliche API-Key-Authentifizierung.

Nicht jede Aktivität besitzt eine Original-FIT-Datei. GPX-Importe werden dadurch nicht zu Original-FITs; übersprungene Dubletten oder andere Quellen können keine gespeicherte Datei haben. Quelle, Aktivitäts-ID und hasFile prüfen; niemals allein auf Dateiendung oder einen fehlgeschlagenen Request vertrauen.

**App-Stand:** B|Cycle hat diesen Downloadweg noch nicht in den Dreeve-Import integriert. Bestehender GPX-/Zusammenfassungsimport bleibt unverändert. Eine spätere Integration kann Original-FIT bevorzugen und GPX bei fehlendem FIT verwenden. Dabei bestehende Fahrtenidentitäten, Dreeve-Verknüpfungen, Profilzuordnung, Filter/Dublettenregeln und Fehlersemantik erhalten; FIT-Integrität/Größe prüfen. Kein Auftrag für diese weitergehende Importänderung in diesem Schritt.

## Quellen

- [Release v5.7.0](https://github.com/dreeveapp/dreeve/releases/tag/v5.7.0)
- [OpenAPI-Dateiimporte und Originaldownload](https://github.com/dreeveapp/dreeve/blob/85638ed67f4e11c55caa247dc1358485c357da61/docs/src/schemas/file-imports.yaml)
- [Download-Handler](https://github.com/dreeveapp/dreeve/blob/85638ed67f4e11c55caa247dc1358485c357da61/src/Controller/Api/V1/FileImport/FileImportFileRequestHandler.php)
- [Aktivitätsfilter](https://github.com/dreeveapp/dreeve/blob/85638ed67f4e11c55caa247dc1358485c357da61/src/Domain/Import/Search/FileImportSearchFilters.php)
- [Upstreamtests zu Originalbytes und Fehlern](https://github.com/dreeveapp/dreeve/blob/85638ed67f4e11c55caa247dc1358485c357da61/tests/Controller/Api/V1/FileImport/FileImportFileRequestHandlerTest.php)

Der Endpoint wurde laut öffentlicher Dateihistorie mit Commit `c6fba32deb18620c937c9d628aee5cf1bdf029d0` hinzugefügt. Aus der Prüfung von v5.7.0 wird keine exakte früheste Mindestversion abgeleitet.
