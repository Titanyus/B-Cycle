# Offline-Sonnenwerte

Die gemeinsame Feldauswahl enthält „Wetter → Sonne & Tageslicht“: Sonnenaufgang,
Sonnenuntergang, Zeit bis Sonnenuntergang und verbleibendes Tageslicht. Alle vier
sind in Dashboard- und Kartenfeldern belegbar und werden wie andere Felder dauerhaft
über stabile IDs gespeichert. Es gibt keinen zusätzlichen Sensor und keinen Webdienst.

SolarCalculator berechnet mit den NOAA-Sonnengleichungen den sichtbaren Sonnenhorizont
(Sonnenzentrum −0,833°: übliche Refraktion und Sonnenradius). Datum und angezeigte
Uhrzeit verwenden die Android-Systemzeitzone einschließlich Sommerzeit. Es werden
benachbarte UTC-Tage geprüft, damit der ausgewählte lokale Tag auch an der Datumsgrenze
stimmt. Erwartete Genauigkeit: wenige Minuten, keine lokale Horizont-/Geländekorrektur,
keine reale Wetter-/Refraktionsmessung. Bei Polartag/-nacht ohne Ereignis: „—“.

RideRuntime ergänzt den vorhandenen GPS-Snapshot; frische, vom bestehenden GPS-Filter
angenommene Koordinaten sind Voraussetzung. Ohne GPS, bei ausgeschaltetem GPS oder
nach Ablauf der bestehenden 5-Sekunden-Freshness gibt es keine Sonnenwerte.
Keine Position wird zusätzlich gespeichert. Der kleine In-Memory-Cache wird nach einem
lokalen Tageswechsel, einem Zeitzonenwechsel, 25 km Positionsänderung oder einer Stunde
erneuert; bei einer rückwärts verstellten Uhr ebenfalls. Sekündlich wird nur die Differenz
zum bereits berechneten Sonnenuntergang gebildet; kein eigener Timer und keine zusätzliche
GPS-Abfrage. Restzeit HH:mm wird auf volle Minuten abgerundet; nach Sonnenuntergang
zeigen beide Restzeitfelder 00:00. Vor Sonnenaufgang ist es die Dauer bis zum heutigen
Sonnenuntergang, nicht ausschließlich die Dauer der noch hellen Stunden.

Die vier Werte sind Anzeigegrößen und verändern keine FIT-Aufzeichnung.
Lokale Lucide-Sunrise/Sunset-Vektoren, keine zusätzliche Bibliothek.

Tests: Berlin Sommer-/Wintersonnenwende mit Referenzzeiten (Toleranz vier Minuten),
Sommerzeit, lokale Tagesgrenzen/Datumsgrenze, Polartag/-nacht, fehlende/ungültige GPS-Daten,
Cache und Countdown, dauerhafte Feldwahl und Auswahl im bestehenden Dropdown.
