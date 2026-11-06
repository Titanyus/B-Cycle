# Trainingssammlung (06.11.2026, nach Build143)

## Umfang und Bedienung

Eigener Einstellungsmenüpunkt „Training“ nach den Streckenvorlagen. Zunächst ausschließlich Vorlagen sammeln, keine Kalender-/Wochenplanung oder Live-Ausführung. Gruppierung nach Trainingsart: Ausdauer, Intervalle, Schwelle/Sweet Spot, Regeneration, weitere Trainings. Quellnamen/Tags und eindeutige relative Leistungsstrukturen helfen bei der Zuordnung; uneindeutige Vorlagen bleiben unter „Weitere Trainings“.

Native ListView mit wiederverwendeten Karten, SectionIndexer/Fastscroll und gepinnten Gruppenüberschriften. Karte: Titel, kurze Beschreibung (maximal drei sichtbare Zeilen), zeitproportionales farbiges Stufendiagramm einschließlich Rampen, darunter Dauer, Abschnittszahl, Zieltyp, Sport, Quelle und Format. Volltonflächen ohne Farbverlauf in Liste und Detailansicht, Rampengeometrie unverändert. Feste funktionale TrainingZoneColors; relative FTP-/explizite Zonenziele unmittelbar, absolute Watt-/Pulsziele anhand des Fahrerprofils. Fehlende Referenz: neutrale Farbe mit kompaktem Hinweis, Verlauf bleibt sichtbar. Offene/distanzgesteuerte Abschnitte erfinden keine Gesamtzeit; Diagramm als Abschnittsfolge kenntlich.

Fixer Footer wie beim Streckenmanager: Import (Intervals/Datei), Filter/Sortieren vorerst deaktiviert, Löschen. Tippen öffnet die Detailansicht; Longpress beginnt die Mehrfachauswahl, danach schaltet Tippen die Auswahl um. Löschen fragt vor dem Entfernen nach; Zurück hebt zunächst eine Auswahl auf. Gepinnte Köpfe blockieren Klicks auf verdeckte Trainings, vertikales Scrollen bleibt möglich.

## Gemeinsame Importpunkte

Backup & Restore → Importieren enthält wieder zwei Rahmen-Accordions: „Strecken importieren“ mit unveränderten vier RouteImportAction-Aktionen und darunter „Trainings importieren“. Dessen Intervals-/Dateieinträge stammen aus TrainingImportAction.entries, ebenso das Importmenü im Trainingsfooter. Bestehende Sicherungen, HRV-CSV, Streckenmanager und Dreeve-Freigabe unverändert. Es wurde kein neuer Trainings-Backupumfang in die bestehenden Sicherungsformate eingebaut.

## Intervals

Bestehende IntervalsSettings inklusive Athleten-ID/verschlüsseltem API-Schlüssel und WLAN-Präferenz; manueller Trainingsabruf hängt nicht vom automatischen Upload-Schalter ab. Keine Zugangsdaten in Trainingsdateien oder Logs.

Am05./06.11.2026 geprüfte öffentliche [OpenAPI](https://intervals.icu/api/v1/docs): GET `/athlete/{id}/workouts` liest ausschließlich die Trainingsbibliothek; GET `/athlete/{id}/workouts/{workoutId}` lädt für jeden Eintrag das vollständige Original. `workout_doc.steps` wird bevorzugt direkt übernommen: explizite Rampen samt Richtung, Zielbereiche, verschachtelte feste Wiederholungen, Zeit-/Distanz-/offene Abschnitte und Zieleinheiten. POST `/athlete/{id}/download-workout.fit` ist die dokumentierte zustandslose Dateikonvertierung mit Athleten-Referenzwerten; keine Erzeugung/Änderung von Bibliothekseinträgen, Kalender-/Aktivitäts-/Wellness-Uploads. Die Konvertierung wird nur bei fehlender nativer Struktur verwendet und benutzt denselben Leser wie lokale Importe. Ein FIT-Zielbereich gilt niemals automatisch als Rampe: die Konvertierung kann Rampenrichtung verlieren. Stabiler SHA-256-Schlüssel aus Dienst/Athlet/Remote-ID aktualisiert bei erneutem Abruf dieselbe Vorlage. Alte lokale Vorlagen werden nicht durch fehlende Remoteeinträge gelöscht.

Alle Vorlagen einer Abrufrunde werden zunächst geprüft, erst danach atomar in die Sammlung übernommen. Abbrechen, Kontowechsel, Netz-/Datenfehler lassen die vorhandene Sammlung unverändert. Ein serieller Worker, begrenzte Antworten (Bibliothek/Einzelantwort8MiB, Datei4MiB, Runde32MiB), maximal500 Vorlagen/50000 expandierte Abschnitte in der Sammlung. Kein Polling/Hintergrundjob und keine Auswirkung auf BLE/GPS/Aufzeichnung.

## Dateiimport und Datenmodell

Android-Dateiauswahl Request21, anschließend URI-/Parser-/Speicherarbeit auf dem Worker. Lokale Inhalts-Hashes verhindern Dubletten bei identischem Import. Sammlung unabhängig vom Fahrradprofil unter files/training-workouts/workouts.json, versioniertes Modell und AtomicFile, gemeinsamer Lock auch über Activity-Neustarts. Gültige Daten behalten Zielbereiche, Rampen, feste/offene Dauersteuerung und primäre/sekundäre FIT-Ziele; keine Datumsterminierung.

- ZWO: Warmup/Cooldown/Ramp, SteadyState, IntervalsT mit festen Wiederholungen, FreeRide und Kadenzangaben. DTD/Entities abweisen, externe Auflösung gesperrt.
- ERG: Minuten/Watt (auch Minuten/Prozent), gestufte/doppelte Zeitpunkte und lineare Rampen. Zeitachse muss bei0 starten und monoton sein.
- FIT: offizieller vorhandener Garmin-Decoder mit CRC; Workout/WorkoutStep statt aufgezeichneter Aktivitäts-FIT. Zeit-/Distanz-/offene Abschnitte, Wiederholungsblöcke mit fester Anzahl, Watt/FTP-Prozent/Leistungszone und bpm/Maximalpuls-Prozent/Pulszone, sekundäre Ziele. Bedingte Wiederholungsschleifen und unbekannte ZWO-Strukturen werden ausdrücklich mit Hinweis abgelehnt, statt falsche Zeitabläufe zu erfinden.

Keine neue Abhängigkeit. Normalisierte Sammlung, kein eigener Trainingsplayer/Dateiexport und keine Remote-Synchronisation von Löschungen.

## Prüfung

TrainingLibraryTest: echte Garmin-Workoutdateien einschließlich Wiederholungen, HR-Bereich und distanzabhängigem Abschnitt; ZWO65min/12Abschnitte mit Rampen; ERG-Sprung/Rampe und neutrale Darstellung ohne FTP; ungültige Dateien/AtomicFile/Dubletten/Löschen; MockWebServer prüft GET Bibliothek plus POST Konverter ohne Kalender-/Bibliothekswrites; vollständiger Abruf, fehlerhafte Runde und Abbruch; echter MainActivity-Weg Menü→Footer→Dateiimport→bestätigtes Löschen; native Diagramm-/Footer-/Gruppen-/Pinned-Klickprüfung. Zusätzlich DataSettingsNavigationTest für beide Importframes und unveränderte Strecken-/HRV-Aktionen sowie aktualisierte SettingsMenuTest-Geometrie.

Logs: training-collection-final-tests.log (Trainings-/Importfälle grün; alte Menüindex-Erwartung danach angepasst), training-sticky-and-menu-final-tests.log (Sticky-Klicks und aktualisiertes Menü grün). Keine unnötigen Gesamtläufe. Öffentliche API-Struktur/Testserver geprüft; kein Zugriff auf das persönliche Intervals-Konto im Entwicklungsprozess. Gerätevergleich und echter Kontotest offen; keine neue APK beauftragt.

## Detailansicht und Rampenkorrektur nach Build144

Die Detailansicht enthält Übersicht mit Diagramm und vollständiger Beschreibung, übernommene Abschnitte mit Dauer/Zielen und alle originalen Intervals-Felder. Die vollständige Einzelantwort wird unabhängig von der normalisierten Diagrammstruktur als `intervalsData` gespeichert; unbekannte Felder bleiben erhalten. Verschachtelte Objekte/Listen werden bei Bedarf geöffnet, große Listen und Texte vollständig in weiteren Portionen angezeigt. Null/false/0 bleiben sichtbar. Parsing auf dem Bibliotheksworker, kein Netzabruf beim Öffnen der Details.

Mögliche Originalfelder sind unter anderem ID, Name, Beschreibung, Sportart, Indoor, Tags, Farbe, Ordner, Dauer, Distanz, Intensität/Trainingslast, Energie, Zielvorgaben, strukturierte Schritte und Anhänge. Die Anzeige verwendet die tatsächliche Antwort statt einer festen Feldliste. Zusätzliche Zielreferenzen `%lthr` und `%hrr` nutzen die vorhandenen Fahrerwerte; fehlende Referenzen bleiben neutral.

Bereits gespeicherte Vorlagen bleiben lesbar. Zum Nachladen der Originaldaten und zur Rampenkorrektur einmal erneut von Intervals importieren; derselbe Remote-Schlüssel aktualisiert den bestehenden Eintrag. Lokale Dateien haben keine Intervals-Originalfelder. Kein Kalenderabruf, Trainingsplayer oder neuer Export-/Backupumfang.

Gezielt geprüft: TrainingDetailsTest und TrainingLibraryTest, zusammen10 Tests grün (`/workspace/.tools/training-original-data-final-tests.log`). MockWebServer prüft Liste→vollständiger Einzelabruf ohne FIT-Konvertierung für native Rampen; gespeicherte Auf-/Abwärtsrampen zusätzlich per Diagrammpixel geprüft. Normale Zielbereiche bleiben flach; Wiederholungen, HF-Einheiten, unbekannte verschachtelte Felder und Pagination sowie echter MainActivity-Tap→Details/Longpress→Löschen geprüft. FIT-Fallback weiterhin geprüft. Typografie/UI-Katalog/Diff grün. Screenshot `app/build/reports/ui/training-details.png`. Kein persönlicher Kontozugriff und kein neuer APK-Build.

## Automatische Leistungstags nach Build144

Nutzerbestätigte Grenzen: genau ein automatisch abgeleiteter Tag anhand der höchsten vorgegebenen Leistung über alle Abschnitte: unter70%FTP „Locker“, 88 bis unter95% „Sweet Spot“, ab95% „Hart“. Zwischen70 und88% kein solcher Tag. Keine zeitliche Mittelung: kurze Sprints/An-Aus-Intervalle bleiben „Hart“ trotz langer Pausen; Rampen und Zielbereiche berücksichtigen beide Enden.

Anzeige in Übersicht und Detailkarte, auch für bestehende Importe ohne erneuten Abruf. Ableitung aus normalisierten Leistungszielen beim Binden, Watt mit aktueller Fahrer-FTP; originale Quelltags und Gruppierung bleiben erhalten, keine Datenmigration/Remoteänderung. Ohne Leistungsziele, bei Watt ohne FTP oder reinen Zonenzielen mit ungenauer Prozentgrenze kein erfundener Tag. Trainings ausschließlich nach Puls werden nicht als Leistungsintensität eingeordnet.

TrainingIntensityTest prüft exakte Grenzwerte, unklassifizierten Zwischenbereich, Rampen/Zielbereiche, harte Spitzen trotz Erholung, Wattumrechnung und fehlende Referenzen. Gezielter Nachweis `/workspace/.tools/training-intensity-tests.log`; zentrale Typografie/Katalog/Diff-Prüfungen.

## Detaildiagramm mit Zeit-/Wattachsen

Nur die Detailkarte verwendet das größere180dp-Diagramm mit Achsen; Listenkarten bleiben96dp ohne Skalen. X zeigt bei vollständig zeitdefiniertem Training runde gleichmäßig verteilte Fahrzeitmarken plus exaktem Anfang/Ende, Abstände nach Dauer/Breite mit Kollisionsprüfung. Offene/Distanzabschnitte bekommen keine erfundene Fahrzeitachse.

Y zeigt tatsächliche Watt, von0 mit runden50/100/200/500…-Schritten nach maximaler Leistung; auch Rampen-Enden bestimmen die Skala. FTP-Prozentziele werden mit Fahrer-FTP umgerechnet, Wattziele direkt; gemischte Watt-/FTP-Ziele teilen eine physikalisch gemeinsame Skala. Normale Zielbereiche bleiben auf ihrem Mittelwert flach, Rampen zeigen den tatsächlichen Verlauf. Keine falschen Wattangaben für Puls-/Zonenziele oder FTP-Ziele ohne Referenz; bei fehlender FTP kompakter Hinweis. Volltonfarben erhalten.

Gezielte Tests TrainingChartAxesTest + TrainingDetailsTest: Watt-/FTP-Mischung, Rampenrichtung, exakte/runde Zeitmarken, fehlende Referenzen, native Diagrammpixel und bisheriger Detailablauf. Log `/workspace/.tools/training-chart-axes-tests.log`.

## Vereinbarte spätere Varianten

Basistraining mit eindeutigem wiederholtem Hauptblock: Varianten Q+, Q, S, E, E+ entsprechen minus2/minus1/Standard/plus1/plus2 Wiederholungen. Aufwärmen/Ausrollen erhalten; Dauer und sämtliche weiteren berechenbaren Kennzahlen ausschließlich aus tatsächlich gewählter Struktur. Orientierung Quick/Extended wie polarized.cc; noch keine Variantenfunktion implementiert. Wiederholungsstruktur muss zusätzlich zur bisher expandierten Abschnittsliste erhalten werden.
