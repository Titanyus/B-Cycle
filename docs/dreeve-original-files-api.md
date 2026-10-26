# Dreeve-API: Originaldateien einschließlich FIT

Geprüft26.10.2026 gegen veröffentlichte **v5.7.0**, Quellstand `85638ed67f4e11c55caa247dc1358485c357da61`. Prüfung öffentlicher API-Dokumentation, Handler und Upstreamtests; keine Verbindung zum Nutzer-Server. Dessen installierte Version ist unbekannt.

Ja: Die aktuelle API bietet den unveränderten Download gespeicherter Originaldateien, auch FIT. Es ist keine aus GPX neu erzeugte FIT-Datei.

1. `GET /api/v1/file-imports?filters[activityId]=<activityId>` liefert passende Dateiimporte. Pagination beachten. Angaben: `id` als FileImport-ID, `filename`, `source`, `status`, `activityId`, `hasFile`, `importedOn`.
2. Bei passender Aktivität, erfolgreichem Import, Quelle `fitFile` und `hasFile=true`: `GET /api/v1/file-imports/{fileImportId}/file`.
3. Antwort200 ist `application/octet-stream` mit Originalbytes und Content-Disposition/Originaldateiname. Die API unterstützt Original-FIT, -TCX und -GPX.404 kann unbekannter Import oder fehlende gespeicherte Datei sein (`file_not_available`);401 ist ein Authentifizierungsfehler. Übliche API-Key-Authentifizierung.

Nicht jede Aktivität besitzt eine Original-FIT-Datei. GPX-Importe werden dadurch nicht zu Original-FITs; übersprungene Dubletten oder andere Quellen können keine gespeicherte Datei haben. Quelle, Aktivitäts-ID und hasFile prüfen; niemals allein auf Dateiendung oder einen fehlgeschlagenen Request vertrauen.

**App-Stand:** Integriert26.10.2026. Nur ausgewählte, noch nicht verknüpfte Fahrten prüfen den Dateiimport-Katalog (Filter Aktivität/fitFile/success, Pagination100). Download nach FileImport-ID, niemals nach Dateiname. Unveränderte Originalbytes gehen durch den bestehenden FIT-Reader (CRC, maximal100MiB/600000 Records) und die bestehende FIT-Bibliothek. Indoor-FIT ohne GPX/GPS ist möglich. Herkunft Importiert/Gefahren und bestätigte Profilzuordnung gelten unverändert; Profiltyp wird als lokales Metadatum geführt, nicht in Originalbytes umgeschrieben.

Keine verfügbare Datei, alte API ohne hasFile/404 oder fehlender Originaldownload404 → GPX. Ein defektes FIT führt mit sichtbarem Hinweis zu GPX; ohne GPX bleibt die bisherige Zusammenfassung. Authentifizierungs-, Netzwerk-, Server- und ungültige API-Antworten bleiben konkrete Fehler. Abbruch bereinigt temporäre Dateien. Kein automatischer Cloud-Upload.

Bestehende GPX-/Zusammenfassungsimporte werden nicht aufgewertet oder ersetzt. FIT behält seine Hash-ID; Dreeve-Link und neue Profil-/Namensmetadaten verwenden diese tatsächliche lokale ID. Bereits byteidentisches FIT wird nur verknüpft, nicht dupliziert; Name, Herkunft und Profil bleiben unverändert. Linkbeleg enthält den Hash des heruntergeladenen Originals, auch falls lokal bereits eine abgeleitete Version aktiv ist. Löschen entfernt wie bisher lokale Verknüpfungen und erlaubt erneuten Import.

45 gezielte Tests mit MockWebServer und Robolectric erfolgreich; sie prüfen API-Pagination/Filter/Datei-ID, Originalbytes, Indoor, Herkunft, Dubletten, Ersatzwege, klare Fehler und Abbruch. Kein Test gegen den privaten Nutzer-Server.

## Quellen

- [Release v5.7.0](https://github.com/dreeveapp/dreeve/releases/tag/v5.7.0)
- [OpenAPI-Dateiimporte und Originaldownload](https://github.com/dreeveapp/dreeve/blob/85638ed67f4e11c55caa247dc1358485c357da61/docs/src/schemas/file-imports.yaml)
- [Download-Handler](https://github.com/dreeveapp/dreeve/blob/85638ed67f4e11c55caa247dc1358485c357da61/src/Controller/Api/V1/FileImport/FileImportFileRequestHandler.php)
- [Aktivitätsfilter](https://github.com/dreeveapp/dreeve/blob/85638ed67f4e11c55caa247dc1358485c357da61/src/Domain/Import/Search/FileImportSearchFilters.php)
- [Upstreamtests zu Originalbytes und Fehlern](https://github.com/dreeveapp/dreeve/blob/85638ed67f4e11c55caa247dc1358485c357da61/tests/Controller/Api/V1/FileImport/FileImportFileRequestHandlerTest.php)

Der Endpoint wurde laut öffentlicher Dateihistorie mit Commit `c6fba32deb18620c937c9d628aee5cf1bdf029d0` hinzugefügt. Aus der Prüfung von v5.7.0 wird keine exakte früheste Mindestversion abgeleitet.
