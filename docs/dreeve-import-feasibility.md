# Dreeve: Import vollständiger Fahrten

Stand: 19.10.2026. Nutzeranforderung: vollständige Fahrt einschließlich Leistungs- und Pulsverlauf, keine reine geplante GPX-Strecke.

## Geprüfte Schnittstellen

Die [öffentliche API](https://docs.dreeve.app/api/openapi.yaml) bietet eine paginierte Aktivitätenliste, Aktivitätsdetails und `GET /api/v1/activities/{activityId}/gpx`. Ein Download der ursprünglichen FIT-Datei oder ein separater Messwert-Stream-Endpunkt ist dort nicht dokumentiert. Aktivitätsdetails enthalten Zusammenfassungen, keine vollständigen Zeitreihen.

Der [GPX-Serializer](https://github.com/dreeveapp/dreeve/blob/master/src/Domain/Activity/GpxSerializer.php) exportiert pro vorhandenem GPS-Punkt Zeit, Höhe und – soweit in Dreeve vorhanden – Leistung, Puls, Kadenz und Temperatur. Leistung steht als `extensions/power`, die übrigen Sensorwerte in Garmins TrackPointExtension. Die öffentliche [Export-Testdatei](https://github.com/dreeveapp/dreeve/blob/master/tests/Controller/File/__snapshots__/ActivityGpxRequestHandlerTest__testHandle__1.xml) wurde heruntergeladen und mit einem XML-Parser geprüft: zwei Trackpunkte mit jeweils Höhe, Leistung, Puls, Kadenz und Temperatur sowie Zeitstempeln.

## Grenzen

- Der Serializer setzt einen GPS-Koordinatenstream voraus. Ohne diesen gibt der API-Handler `404 gpx_not_available` zurück. Indoor-Fahrten ohne GPS sind damit nicht importierbar.
- Der Export läuft über GPS-Punkte; eine verlustfreie Übernahme aller ursprünglichen FIT-Messungen ist nicht zugesichert. Zusätzliche E-Bike-/Akkuwerte sind im Serializer nicht enthalten.
- B|CYCLE liest seit dem Importprototyp Position, Zeit, Höhe, Leistung, Puls, Kadenz und Temperatur aus den unterstützten GPX-Erweiterungen. Leistungs-/Puls-/Kadenzverläufe werden in der Analyse berücksichtigt; Temperatur bleibt im Original-GPX erhalten. Fehlende Messungen werden nicht erfunden.
- Keine Abfrage eines echten Nutzerkontos durchgeführt; Zugangsdaten liegen nur in der Android-App. Die Prüfung betrifft öffentliche Dokumentation, Quellcode und Testdatei, keinen Ende-zu-Ende-Import.

## Konsequenz

Ein Outdoor-Import mit den verfügbaren grundlegenden Sensorverläufen ist technisch vorbereitbar. Für den geforderten vollständigen Import einschließlich Indoor wird zuerst ein GPS-unabhängiger Stream- oder Original-FIT-Download in der Dreeve-API benötigt. Keine reine Streckenübernahme als vollständige Fahrt ausgeben und keine Werte aus Durchschnittsdaten erfinden.

Der schlanke Importprototyp ist jetzt unter Streckenmanager → Strecke importieren → Aus Dreeve importieren verfügbar. Er verwendet die bestehende Dreeve-Konfiguration, ordnet externe Aktivitäts-IDs stabil zu, verhindert bekannte Duplikate und lädt Importe nicht automatisch zu anderen Diensten hoch. Details und Testgrenzen: [Dreeve-Import](dreeve-import.md).
