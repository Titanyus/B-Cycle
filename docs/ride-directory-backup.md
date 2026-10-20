# Fahrten und Einstellungen als Verzeichnis sichern

Unter **Daten & Speicher → Fahrten sichern** den Schalter einschalten und das
Accordion öffnen. **Lokal** oder **WebDAV** auswählen und **Sicherung starten**
antippen. Der Schalter allein startet keine Übertragung. WebDAV verwendet die bereits
konfigurierte HTTPS-Verbindung und ist ohne gültige Konfiguration deaktiviert;
der automatische Fahrtupload muss dafür nicht eingeschaltet sein.

Jeder Start erzeugt einen neuen Ordner `Bicycle-Backup-…`. Lokal wird der Android-
Verzeichnisdialog verwendet. Die Sicherung läuft auf dem bestehenden Bibliotheksworker;
während Aufnahme, Speichern oder anderer Bibliotheksverarbeitung ist sie gesperrt.

## Inhalt

| Bestand | Dateien |
| --- | --- |
| FIT-Fahrt | `<id>.fit` mit allen noch vorhandenen privaten Messwerten; `<id>.original.fit` unverändert für Original und stabile ID; ggf. `<id>.corrected.fit` |
| GPX-Fahrt | Unverändertes `<id>.gpx` und daraus erzeugtes `<id>.fit` mit tatsächlich verfügbaren unterstützten GPX-Werten |
| Dreeve-Zusammenfassung ohne Track | `<id>.summary.json`, keine erfundenen FIT-Messpunkte |
| Jede Fahrt | `<id>.ride.json` mit Name, Herkunft, Details, bestätigten Dienstverknüpfungen, Fahrradzuordnung und Dateiprüfsummen |
| Einstellungen | `settings.json` mit allen sechs bisherigen Sicherungsbereichen und ergänzter öffentlicher Konfiguration |
| Fahrradbilder | `bike-<uuid>.webp` |
| HRV-Messung | `hrv-<uuid>.json` mit Rohpaketen, RR-/NN-Folgen, Ergebnis und Qualitätsangaben |
| Abschluss | `bicycle-backup.json` als Inhaltsverzeichnis, einschließlich fehlgeschlagener Fahrten |

FIT eignet sich zum Austausch tatsächlicher Messwerte. JSON erhält die appbezogenen
Metadaten, die FIT allein nicht zuverlässig abbildet. Für die Wiederherstellung den
gesamten Ordner unverändert aufbewahren. Frühere verlorene oder nie aufgezeichnete
Messwerte können auch durch diese Sicherung nicht ergänzt werden. Normale Exporte und
Dienstuploads verwenden weiterhin die bisherige gefilterte FIT-Version.

## Wiederherstellen

1. **Einstellungen aus Sicherungsordner wiederherstellen** wählen und die gewünschten
   Bereiche im bestehenden Bestätigungsdialog übernehmen.
2. Zugangsdaten und notwendige Android-Berechtigungen erneut einrichten.
3. **Fahrten aus Sicherungsordner importieren** wählen und bestätigen.

Direkt einen Sicherungsordner oder dessen Elternordner auswählen. Bei mehreren
Sicherungen wird eine Auswahl angeboten. Derselbe Ablauf funktioniert über WebDAV.
Fahrten werden nicht automatisch hochgeladen und es werden keine Namensabgleich-Jobs
durch den Import gestartet.

Jedes Fahrrad hat eine portable UUID. Fahrten werden ausschließlich diesem Profil
zugeordnet; gleicher Name oder gleiche Slotnummer reichen nicht. Ohne passendes Profil
bleibt die Fahrt unzugeordnet, der gesicherte Name/Farbwert bleibt als Darstellung
vorhanden. Nach Wiederherstellung des passenden Profils ist die Zuordnung auflösbar.
Vor dem Ersetzen von Profilen werden bestehende lokale Altzuordnungen und Bilder an
deren bisherige UUID gebunden, damit sie keinem neuen Fahrrad im gleichen Slot folgen.

## Prüfung der Einstellungssicherung

Die bestehende bereichsweise JSON-Sicherung wurde um folgende zuvor fehlende Daten ergänzt:

- Alle Fahrradprofile einschließlich Indoor-/Aufnahmekonfiguration, aktiver Auswahl,
  nächster Slotnummer, portabler Identitäten und profilbezogener Design-/Kartenkonfiguration.
- Bestätigte Dreeve-Gear-Zuordnungen.
- Benachrichtigungsauswahl, Position und Dauer sowie Schalter/Ziel der Fahrten-Sicherung.
- Öffentliche intervals.icu-, Dreeve- und Strava-Konfiguration.

Diese Ergänzungen sind optional im bestehenden Format; alte Backups bleiben lesbar.
Profilbilder sind binäre Dateien und nur in der vollständigen Verzeichnissicherung
enthalten. Passwörter, API-/OAuth-Schlüssel, Android-Keystore-Geheimnisse und
Berechtigungsfreigaben werden bewusst nicht exportiert. Dienste ohne passende lokal
vorhandene Zugangsdaten bleiben nach der Wiederherstellung ausgeschaltet. Bei geändertem
Konto/Ziel werden alte Zugangsdaten nicht übernommen. Historische Uploadwarteschlangen,
Live-Sensorzustand und flüchtige Kartenansichten gehören nicht zur Sicherung.

## Integrität und Grenzen

Dateigrößen, sichere Dateinamen, Format und SHA-256 werden vor dem Import geprüft.
Bestehende identische Fahrten werden übersprungen; abweichende Daten mit derselben ID
werden als Fehler gemeldet und nicht überschrieben. Metadatenfehler rollen eine neu
installierte Fahrt zurück. Fehlgeschlagene Fahrten werden gezählt; ein Teilbackup wird
auch beim Import nicht als vollständig ausgegeben. Die Originalbibliothek wird nicht
gelöscht.

Das Manifest wird zuletzt geschrieben. Ein abgebrochener Lauf ohne gültiges Manifest
ist keine abgeschlossene Sicherung. Auf dem gewählten Ziel können unvollständige
Ordner verbleiben. WebDAV benötigt MKCOL, PROPFIND, HEAD, GET und bedingtes PUT sowie
Lesezugriff zur Prüfung hochgeladener Dateien.

Gezielte JVM-/Robolectric-Tests prüfen den FIT-/GPX-/Zusammenfassungs-Rundlauf,
Fahrradidentitäten/Bilder, beschädigte und abgebrochene Sicherungen, Wiederholungsimporte,
Einstellungsauswahl, Android-Verzeichniszugriffe und einen lokalen HTTPS-WebDAV-Testserver.
Ein echter Android-Dokumentanbieter und der persönliche WebDAV-Server benötigen noch
praktische Geräteprüfung.

Ab 0.8.16 enthält der vollständige Ordner auch gespeicherte HRV-Messungen einschließlich
Rohdaten. Die Bilanz führt HRV getrennt auf; alte Sicherungen ohne HRV bleiben lesbar.
