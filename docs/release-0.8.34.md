# B|CYCLE 0.8.34

Änderungen seit Build 72:

- Neuer HRV-Einstiegsdialog mit Screen-Herzgrafik, Themefläche/Rand, normalem Startbutton und Später. Langer Druck auf den linken Dashboard-Streckenbutton öffnet ihn auch manuell; kurzer Tipp bleibt Streckenmanager.
- Dreeve-Verarbeitung ohne exponentiell wachsende Wartezeit für normale Bestätigungen. Erfolgreichen Uploadstatus 600 ms halten: Dreeve/intervals prüfen und bei bestätigtem Fehlen erneut übertragen, WebDAV dieselbe Datei prüfen/wiederherstellen, Strava bewusst erneut versuchen und lokal weitere Kopie speichern.
- Manueller Dreeve-Fahrtnamenabgleich unter Daten & Speicher; keine regelmäßigen Namensabfragen.
- Quellenwahl je Messgröße/Profil mit automatischer Ersatzquelle und Rückkehr bei frischen Daten. Smartphone-Barometer mit GPS-Referenz, kompatible Aufzeichnungen/Profile/Backups. Separate Powermeter weiterhin nicht angebunden.
- Optionaler Aufnahme-Autostart je Profil für verbundenes E-Bike/FTMS-Indoor-Bike, einschließlich Appöffnung bei bereits eingeschaltetem Gerät. Aufnahme/Speichern/aktive HRV behalten Vorrang.
- Rollierende Dashboardgraphen geglättet und weiche Rückskalierung über drei Sekunden bis zum Startbereich. Skalen innerhalb der Graphbreite und kompakte Farbpunkte.
- Fahrtenzusammenfassung mit Zu den Details. Explizit beendete Aufnahmen unter zehn Sekunden verwerfen, exakt zehn Sekunden speichern.
- Letzten E-Bike-Akkustand je Profil dauerhaft anzeigen. Unveränderte tatsächliche Bosch-Akkustände während derselben Live-Verbindung ohne Zeitlimit aufzeichnen; Offline-Anzeige bleibt getrennt.
- Windpfeil in Textgröße statt Himmelsrichtungsabkürzung in Streckendetails. Pfeil zeigt wohin Wind weht: Südwestwind nach Nordost.

Gezielte Tests in den jeweiligen Umsetzungsschritten erfolgreich; darunter Aufnahme auf Android 13/15/16, Quellenwechsel/Barometer/FIT/Backup, Upload-/Status-/Gestenprüfungen und native HRV-Darstellung samt Start/Lifecycle. Zwei alte Indoor-Testfahrten auf zwölf Sekunden verlängert, damit sie weiterhin FIT-Ausgaben oberhalb der neuen Zehnsekundengrenze prüfen. Kein kompletter Gesamtlauf, kein physischer Sensor-/Cloudkontentest.

Build und direkte APK-Veröffentlichung: [Buildnachweis](build-73.md). Vorheriger Stand: [Build 72](build-72.md).
