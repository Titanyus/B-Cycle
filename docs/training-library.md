# Trainingssammlung (06.11.2026, nach Build143)

## Umfang und Bedienung

Eigener Einstellungsmenüpunkt „Training“ nach den Streckenvorlagen. Zunächst ausschließlich Vorlagen sammeln, keine Kalender-/Wochenplanung oder Live-Ausführung. Gruppierung nach Trainingsart: Ausdauer, Intervalle, Schwelle/Sweet Spot, Regeneration, weitere Trainings. Quellnamen/Tags und eindeutige relative Leistungsstrukturen helfen bei der Zuordnung; uneindeutige Vorlagen bleiben unter „Weitere Trainings“.

Native ListView mit wiederverwendeten Karten, SectionIndexer/Fastscroll und gepinnten Gruppenüberschriften. Karte: Titel, kurze Beschreibung (maximal drei sichtbare Zeilen), zeitproportionales farbiges Stufendiagramm einschließlich Rampen, darunter Dauer, Abschnittszahl, Zieltyp, Sport, Quelle und Format. Feste funktionale TrainingZoneColors; relative FTP-/explizite Zonenziele unmittelbar, absolute Watt-/Pulsziele anhand des Fahrerprofils. Fehlende Referenz: neutrale Farbe mit kompaktem Hinweis, Verlauf bleibt sichtbar. Offene/distanzgesteuerte Abschnitte erfinden keine Gesamtzeit; Diagramm als Abschnittsfolge kenntlich.

Fixer Footer wie beim Streckenmanager: Import (Intervals/Datei), Filter/Sortieren vorerst deaktiviert, Löschen. Tippen/Longpress wählt Trainings, Löschen fragt vor dem Entfernen nach; Zurück hebt zunächst eine Auswahl auf. Gepinnte Köpfe blockieren Klicks auf verdeckte Trainings, vertikales Scrollen bleibt möglich.

## Gemeinsame Importpunkte

Backup & Restore → Importieren enthält wieder zwei Rahmen-Accordions: „Strecken importieren“ mit unveränderten vier RouteImportAction-Aktionen und darunter „Trainings importieren“. Dessen Intervals-/Dateieinträge stammen aus TrainingImportAction.entries, ebenso das Importmenü im Trainingsfooter. Bestehende Sicherungen, HRV-CSV, Streckenmanager und Dreeve-Freigabe unverändert. Es wurde kein neuer Trainings-Backupumfang in die bestehenden Sicherungsformate eingebaut.

## Intervals

Bestehende IntervalsSettings inklusive Athleten-ID/verschlüsseltem API-Schlüssel und WLAN-Präferenz; manueller Trainingsabruf hängt nicht vom automatischen Upload-Schalter ab. Keine Zugangsdaten in Trainingsdateien oder Logs.

Am05./06.11.2026 geprüfte öffentliche [OpenAPI](https://intervals.icu/api/v1/docs): GET `/athlete/{id}/workouts` liest ausschließlich die Trainingsbibliothek. POST `/athlete/{id}/download-workout.fit` ist die dokumentierte zustandslose Dateikonvertierung mit Athleten-Referenzwerten; keine Erzeugung/Änderung von Bibliothekseinträgen, Kalender-/Aktivitäts-/Wellness-Uploads. Die konvertierte FIT-Datei benutzt denselben Leser wie lokale Importe. Stabiler SHA-256-Schlüssel aus Dienst/Athlet/Remote-ID aktualisiert bei erneutem Abruf dieselbe Vorlage. Alte lokale Vorlagen werden nicht durch fehlende Remoteeinträge gelöscht.

Alle Konvertierungen einer Abrufrunde werden zunächst geprüft, erst danach atomar in die Sammlung übernommen. Abbrechen, Kontowechsel, Netz-/Datenfehler lassen die vorhandene Sammlung unverändert. Ein serieller Worker, begrenzte Antworten (Bibliothek8MiB, Datei4MiB, Runde32MiB), maximal500 Vorlagen/50000 expandierte Abschnitte in der Sammlung. Kein Polling/Hintergrundjob und keine Auswirkung auf BLE/GPS/Aufzeichnung.

## Dateiimport und Datenmodell

Android-Dateiauswahl Request21, anschließend URI-/Parser-/Speicherarbeit auf dem Worker. Lokale Inhalts-Hashes verhindern Dubletten bei identischem Import. Sammlung unabhängig vom Fahrradprofil unter files/training-workouts/workouts.json, versioniertes Modell und AtomicFile, gemeinsamer Lock auch über Activity-Neustarts. Gültige Daten behalten Zielbereiche, Rampen, feste/offene Dauersteuerung und primäre/sekundäre FIT-Ziele; keine Datumsterminierung.

- ZWO: Warmup/Cooldown/Ramp, SteadyState, IntervalsT mit festen Wiederholungen, FreeRide und Kadenzangaben. DTD/Entities abweisen, externe Auflösung gesperrt.
- ERG: Minuten/Watt (auch Minuten/Prozent), gestufte/doppelte Zeitpunkte und lineare Rampen. Zeitachse muss bei0 starten und monoton sein.
- FIT: offizieller vorhandener Garmin-Decoder mit CRC; Workout/WorkoutStep statt aufgezeichneter Aktivitäts-FIT. Zeit-/Distanz-/offene Abschnitte, Wiederholungsblöcke mit fester Anzahl, Watt/FTP-Prozent/Leistungszone und bpm/Maximalpuls-Prozent/Pulszone, sekundäre Ziele. Bedingte Wiederholungsschleifen und unbekannte ZWO-Strukturen werden ausdrücklich mit Hinweis abgelehnt, statt falsche Zeitabläufe zu erfinden.

Keine neue Abhängigkeit. Normalisierte Sammlung, kein eigener Trainingsplayer/Dateiexport und keine Remote-Synchronisation von Löschungen.

## Prüfung

TrainingLibraryTest: echte Garmin-Workoutdateien einschließlich Wiederholungen, HR-Bereich und distanzabhängigem Abschnitt; ZWO65min/12Abschnitte mit Rampen; ERG-Sprung/Rampe und neutrale Darstellung ohne FTP; ungültige Dateien/AtomicFile/Dubletten/Löschen; MockWebServer prüft GET Bibliothek plus POST Konverter ohne Kalender-/Bibliothekswrites; vollständiger Abruf, fehlerhafte Runde und Abbruch; echter MainActivity-Weg Menü→Footer→Dateiimport→bestätigtes Löschen; native Diagramm-/Footer-/Gruppen-/Pinned-Klickprüfung. Zusätzlich DataSettingsNavigationTest für beide Importframes und unveränderte Strecken-/HRV-Aktionen sowie aktualisierte SettingsMenuTest-Geometrie.

Logs: training-collection-final-tests.log (Trainings-/Importfälle grün; alte Menüindex-Erwartung danach angepasst), training-sticky-and-menu-final-tests.log (Sticky-Klicks und aktualisiertes Menü grün). Keine unnötigen Gesamtläufe. Öffentliche API-Struktur/Testserver geprüft; kein Zugriff auf das persönliche Intervals-Konto im Entwicklungsprozess. Gerätevergleich und echter Kontotest offen; keine neue APK beauftragt.
