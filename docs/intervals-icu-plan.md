# intervals.icu – geplante Anbindung

Stand: 15.10.2026. Nutzerziel: zunächst Trainings-/Fahrtdaten als FIT hochladen,
später strukturierte Trainingsprogramme austauschen. Noch nicht implementiert.

## Erster Ausbau: FIT-Upload

- Native Views und vorhandene Einstellungen unter Daten & Speicher verwenden.
- Zugangsdaten ausschließlich in der App eingeben; keine Schlüssel im Quellcode,
  in Diagnosen oder in normalen Einstellungssicherungen veröffentlichen.
- Authentifizierungsverfahren anhand offizieller API-Dokumentation festlegen;
  Athleten-ID/API-Key ist ein möglicher Einstieg, kein bereits bestätigter Auftrag
  für eine bestimmte Anmeldelösung.
- Automatischen Upload nach Aufnahmeende und manuelles Senden in den bestehenden
  Streckenaktionen vorsehen. Konkrete Bedienung noch abstimmen.
- Nur eigene aufgezeichnete FIT-Fahrten; aktive, gegebenenfalls höhenkorrigierte
  Dateiversion verwenden. Original erhalten.
- Eigene Konfiguration, Warteschlangen- und Erfolgsstatus pro Dienst und Dateiversion.
  Nextcloud bleibt unabhängig aktivierbar; Erfolg bei einem Dienst bedeutet nicht
  Erfolg beim anderen. Bestehende Nextcloud-Funktionen erhalten.
- Bestehende Hintergrundjobs, HTTP-Anbindung, Wiederholungs- und Dateifingerprint-
  Konzepte wiederverwenden. Keine unnötigen Abhängigkeiten hinzufügen.

## Später: strukturierte Trainingsprogramme

Der Nutzer meint ausdrücklich Trainingsprogramme austauschen. Ziel sind geplante,
strukturierte Einheiten mit Abschnitten, Dauer und Zielintensität. Keine automatische
Übernahme von FTP-/Profilwerten allein aus dieser Anbindung ableiten.

Abruf und Übertragung von Trainingsprogrammen als getrennte Fähigkeiten neben
Aktivitätsuploads strukturieren. Genaue API-Unterstützung für Einzeltrainings,
mehrwöchige Pläne und Dateiformate sowie notwendige Berechtigungen vor Umsetzung
prüfen. Noch keine Trainingssteuerung, neue Ausführungsoberfläche oder vollständige
Synchronisierung implementieren.

## Offene Verifikation

Der direkte Zugriff auf https://intervals.icu/api/v1/docs ist in dieser
Entwicklungsumgebung durch die Netzwerk-Domainrichtlinie blockiert (403 beim
Proxy-Verbindungsaufbau). Endpunkte, Anmeldung und Austauschformate deshalb noch
nicht anhand aktueller Anbieterdokumentation verifiziert. Kein produktiver Upload
oder Datenabruf durchgeführt.
