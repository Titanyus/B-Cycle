# Dreeve-Wartung: schreibgeschützter Prototyp

Einstellungen → Wartung lädt beim Öffnen und über „Von Dreeve abrufen“ die vorhandenen Wartungsaufgaben. Bestehende Adresse und verschlüsselter API-Schlüssel aus Daten & Speicher → Dreeve werden verwendet; automatischer Fahrtupload muss nicht aktiviert sein. Keine zusätzlichen Kontodaten oder Einstellungen.

## API und Anzeige

GET /api/v1/gear/maintenance-tasks, Bearer-Authentifizierung, gemeinsame ActivityHttp-Grenzen/Timeouts/Redirectsperre. GET /api/v1/gear ergänzt Fahrradnamen; bei dessen Fehler bleiben Aufgaben sichtbar, IDs statt Namen und konkreter Warnhinweis. Mehrere Räder an einer gemeinsam genutzten Komponente und derzeit nicht zugeordnete Komponenten bleiben erhalten. Keine Vermutung eines lokalen Profils aus Namen.

Aufgabe, Komponente, Fahrradnamen/-IDs, Intervall (km, mi, Stunden, Kalendertage oder Nutzungstage), positiver Rest beziehungsweise Überschreitung, letzter Wartungstermin und Status Fällig/Überfällig/Anstehend. Dreeve berechnet Fortschritt und Reihenfolge; kein eigener Schwellenwert und keine fiktive Terminprognose. Auch noch nicht fällige Aufgaben sind sichtbar. Leere Antwort kann „keine Aufgaben“ oder „Wartung deaktiviert“ bedeuten; beides im Text benennen. Der geprüfte API-Handler liefert alle Aufgaben in einer Seite, keine zusätzliche Filterung/Pagination durch B-Cycle.

Unbekannter Endpunkt404: Versionshinweis. Authentifizierungs-, Netzwerk-, TLS-, Timeout- und Serverfehler verwenden ServiceProblems. Ungültige JSON-/Aufgabendaten als Antwortfehler behandeln. Abruf auf eigenem Worker, Sichtbarkeits-/Konfigurationsprüfung vor Anzeige; Schließen verwirft späte Antworten und beendet den Worker. Keine Pollingjobs, keine dauerhafte Wartungskopie und keine neuen Sicherungsfelder. Bearbeiten/Erledigtmelden/Upload ist nicht implementiert. Sichtbare IN/OUT-Texte und Hilfe wurden erweitert.

## Nachweise

Dreeve-Quellstand `93c214fd11cb0ff0d3fc2bad74fe2a0d910a0cce`:

- [API-Handler](https://github.com/dreeveapp/dreeve/blob/93c214fd11cb0ff0d3fc2bad74fe2a0d910a0cce/src/Controller/Api/V1/Gear/GearMaintenanceTaskSearchRequestHandler.php)
- [Intervall-Einheiten](https://github.com/dreeveapp/dreeve/blob/93c214fd11cb0ff0d3fc2bad74fe2a0d910a0cce/src/Domain/Gear/Maintenance/Task/IntervalUnit.php)
- [Öffentliche Testantwort](https://github.com/dreeveapp/dreeve/blob/93c214fd11cb0ff0d3fc2bad74fe2a0d910a0cce/tests/Controller/Api/V1/Gear/__snapshots__/GearMaintenanceTaskSearchRequestHandlerTest__testItListsMaintenanceTasksMostUrgentFirst__1.json), als API-Testfixture übernommen.

Sechs gezielte Tests erfolgreich: vier API-/UI-/Navigationstests, tatsächliches Einstellungsmenü und bestehende Dreeve-Pagination/Gear-Abfrage. Keine echte Verbindung zum Nutzer-Server und kein Gerätetest; installierte Dreeve-Version unbekannt. Keine neue APK angefordert.

## Gruppierte Darstellung, 26.10.2026 nach Build89

Fahrräder aus Dreeve sind echte, verschiebbare Sticky-Abschnitte im vorhandenen BicycleStickyScrollView. Darunter normale Bauteil-Accordions, gruppiert nach component.id, mit allen Aufgaben dieses Bauteils. Fahrrad-/Bauteilnamen sind keine Identität: gleiche Namen führen nicht zur Zusammenlegung. Bei fehlender Bauteil-ID werden Aufgaben vorsichtig einzeln angezeigt. Gemeinsame Bauteile stehen bei jedem zugeordneten Fahrrad, mit Hinweis; unzugeordnete Teile bleiben in einem eigenen Abschnitt. Die Anzahl zählt eindeutige API-Aufgaben, keine zusätzlichen Kopien geteilter Bauteile. Dreeves Dringlichkeitsreihenfolge bleibt innerhalb der Gruppen erhalten. Klappzustände bleiben beim manuellen Neuladen erhalten.

Je Aufgabe: Name und Intervall, letzte Wartung (bei null „nie“), vergangene Menge und verbleibende beziehungsweise überschrittene Menge, Fortschrittsbalken und bei Überschreitung Zielmarke samt Zielwert. Einheiten km/mi/h/Kalendertage/Nutzungstage wie vom Server. Fälligkeitsflags bleiben maßgeblich, keine eigene Fälligkeitsprognose oder zusätzliche Schweregradschwelle.

Solange nicht überfällig ist der Fortschritt grün. Bei überfälligen Aufgaben zeigt der Balken den Anteil bis zum Intervallziel heller rot und die Überschreitung dunkler rot, mit einer1dp-Zielmarke. Balkenproportion aus elapsed/interval, bei fehlendem elapsed aus remaining oder percentage; fehlende Textwerte bleiben Strich. Keine künstliche zweite Rotwarnschwelle. Gemeinsame Grundfarben für Pulszonen, FTP-Zonen und Wartung stehen jetzt in TrainingZoneColors.kt. Vorhandene Grundfarbwerte bleiben exakt erhalten; Wartungs-Rotabstufungen sind daraus abgeleitet. Künftige Änderungen an green/red wirken an allen drei Stellen.

Schnellaktionen sind normale App-Buttons mit deaktiviertem Zustand. Bekannte Aufgaben haben passende Beschriftungen wie Gemessen/Getauscht/Gereinigt; unbekannte Aufgaben Erledigt. Es gibt keine schreibenden API-Aufrufe. Verwaltung bleibt ausschließlich Dreeve. Navigation und Header entsprechen der App; kein zusätzlicher Footer oder Plus aus dem Mockup. Die bestehende Erläuterung ist weiterhin über(i) zugänglich, die Datenseite beginnt mit Abruf und Anzahl.

Sieben gezielte Tests zu API, Fehlern/Schließen, Gruppierung nach Identität, gemeinsamem/unzugeordnetem Bauteil, Akkordeon-Zustand, deaktivierten Aktionen, Farb-/Balkenpixeln und echtem Sticky-Pinning/Verdrängen in Flat/Glow erfolgreich. Native Vorschauen geprüft. Kein Test mit dem Nutzer-Server und keine neue APK für diesen Darstellungsschritt.

## Vorgeschlagene automatische Aktualisierung – noch nicht umgesetzt

Nutzer fragt nach Aktualisierung beim Appstart. Empfehlung: beim Appstart asynchron abrufen, beim Öffnen von Wartung aktualisieren und nach bestätigter Dreeve-Verarbeitung einer Fahrt erneut abrufen. Doppelte unmittelbar aufeinanderfolgende Abfragen zusammenfassen. Letzten erfolgreichen Stand kontogebunden lokal vorhalten und Zeitstempel zeigen; bei nicht erreichbarem Server den alten Stand ausdrücklich als solchen anzeigen. Manuellen Abruf erhalten. Dafür gibt es noch keinen Cache/automatischen Trigger: Aktuell unverändert nur Öffnen und manueller Abruf, ohne dauerhafte Wartungskopie oder Polling.


## Automatische Aktualisierung und lokaler Stand – umgesetzt26.10.2026

Ersetzt den unmittelbar vorherigen Vorschlagsstatus: MaintenanceRepository ist der gemeinsame, schreibgeschützte Hintergrundabruf. Auslöser: Öffnen/Vordergrundstart der App, Öffnen der Wartungsseite, manueller Button und bestätigte erfolgreiche Dreeve-Fahrtverarbeitung. Eine angenommene202-/Processing-Antwort löst noch keine Wartungsaktualisierung aus. Auch Änderungen an Adresse/API-Schlüssel verwerfen die vorherige UI-Zuordnung und laden die neue Verbindung.

Automatische Wiederholungen innerhalb60s werden zusammengefasst, auch nach einem fehlgeschlagenen Versuch; laufende normale Abrufe werden geteilt. Manueller Abruf kann die Pause umgehen. Ein Uploadereignis während eines älteren laufenden Abrufs merkt genau einen nötigen Folgeabruf vor. Ein privater dauerhafter Uploadmarker verhindert, dass ein frischer, aber noch vor der Fahrt geladener Cache nach Prozessende den nötigen Abruf unterdrückt. Kein periodischer Pollingtimer und kein zusätzlicher Android-Job.

Letzter erfolgreicher Datensatz in atomarer privater JSON unter noBackupFilesDir, begrenzt auf4MiB, mit Aktualisierungszeit und aus kanonischer Adresse plus API-Schlüssel abgeleiteter Identitätsprüfsumme. Keine Zugangsdaten im Cache. Konfiguration und Schlüssel werden zusammen unter dem vorhandenen Settings-Lock gelesen. Alte Ergebnisse nach Konto-/Schlüsselwechsel werden verworfen; alte Dateien mit anderer Identität nicht angezeigt. Abgeleiteter Cache und Uploadmarker sind keine Einstellungen und gehören nicht in die Einstellungssicherung. Dreeve bleibt einzige Verwaltung.

Die Seite hält vorhandene Zeilen/Klappzustände während Abruf und Fehlern sichtbar, zeigt Aktualisiert:Datum/Uhrzeit und konkrete Fehlermeldung mit Hinweis auf den letzten erfolgreichen Stand. Bei identischen Daten kein Neuaufbau der Liste, nur Zeit/Status ändern. Leere erfolgreiche Antwort ist ebenfalls ein gültiger Cache. Namensabruf darf einzeln scheitern: bekannte Namen erhalten, fehlende über IDs, mit Hinweis. Nach Dialogschluss endet nur die Beobachtung; der gemeinsame Abruf darf den Cache weiter aktualisieren. Wartungs-GETs haben separat5s Verbindungs-/10s Lese-/15s Gesamtlimit je Request; Uploads/andere Dienste behalten ihre bisherigen Limits.

16 unterschiedliche gezielte Tests erfolgreich: Repository-Neustart/Offline/Zeitstempel, korrupter/leerer Cache, Kontowechsel während Abruf, fehlende Konfiguration, direkte Wiederholungen/manueller/Uploadabruf, geteilter laufender Abruf/Folgeabruf, Beobachterabbau, echter App-Vordergrundtrigger und Prozessende mit vorgemerktem Upload, UI-Erhaltung sowie bestehende API-/Gruppen-/Stickyfälle. Echter Upload-Bestätigungstest belegt: während Processing kein Abruf, erst nach bestätigter Aktivitäts-ID. Ein Testharness-Hintergrundfehler beim noch laufenden Robolectric-Routenladen wurde durch Abwarten des Bibliotheksladens und native Testkonfiguration behoben; Abschlusslauf ohne Hintergrundexception. Kein Nutzer-Server-/Gerätetest und keine neue APK angefordert.

API-Recherche zu Original-FITs separat in [Dreeve-Originaldateien](dreeve-original-files-api.md). Dieser Downloadweg ist noch nicht im App-Import eingebaut.

Darstellungsänderung27.10.2026: Wartung beginnt mit den Fahrrad-Sticky-Abschnitten. „Von Dreeve abrufen“, Zeitstempel und Status-/Infotext stehen gemeinsam nach der gesamten Fahrradliste am Seitenende. Bei leerer Liste bleiben dieselben Controls sichtbar. Abruf, Cache, Fehleranzeige und Accordionzustände unverändert.

Darstellung27.10.2026, Dropdown-Rahmen: Bauteile verwenden nun [BicycleFrameDropdown](dropdown-frame.md), dessen bisherige Kopfkontur den gesamten geöffneten Inhalt umfasst.16dp Innenabstand/24dp zum Kopf, Chevron zustandsabhängig gedreht. Intervall und letzter Termin bilden direkt untereinander einen linken Block, vertikal mittig zur rechten Spezial-Rahmen-Pillenaktion. Gruppierung/Status/API/Cache/Offenzustände erhalten; keine neuen schreibenden Aktionen.

## Korrektur nach Build 93

Die gepaddete Wartungsliste schneidet überstehende Sticky-Header nicht mehr am Padding ab (clipToPadding=false), sodass Flat-Header bis zum Bildschirmrand gezeichnet werden. Glow-Inset und Klapp-/Sticky-Verhalten bleiben erhalten. Trenner zwischen Aktionen verwenden wieder den globalen sectionDividerGap von 16 dp oben/unten; lokale 8-dp-Ausnahme und zusätzliches unteres Zeilenpadding entfernt.

Dropdown-Rahmen jetzt mit globalem Aufbau: Work-Icon vor Bauteiltitel, danach drehendes Chevron rechts; optionaler Schalter/Aktionsbutton folgt ganz rechts. Gemeinsame Komponente verlangt ein passendes Icon. Fahrrad-Sticky-Header, Aufgaben, API-Aktionen und Daten unverändert.
