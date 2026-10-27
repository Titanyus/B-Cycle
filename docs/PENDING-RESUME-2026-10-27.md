# Abschluss nach Wiederverbindung

Alle unten gesammelten Änderungsaufträge sind umgesetzt und gezielt geprüft. Neue APK 0.8.55 ausdrücklich angefordert; Veröffentlichung wird in CONTINUE-HERE.md dokumentiert. Der folgende Text ist die historische Übergabenotiz.

# B|CYCLE – Übergabe 27.10.2026

## Arbeitsstand und Umgebung
- Projekt /workspace/B-Cycle, Branch work. Arbeitsumgebung ist offline; zuletzt kein Zugriff auf Projektdateien möglich. Nicht behaupten, Änderungen seien veröffentlicht.
- Letzte veröffentlichte APK: 0.8.54 / Code124 / Build95. Publishercommit64cf4db, Buildquelle62d1ec2764ecf72041110b2c998a49ea7ce62469. GitHub main wurde danach nicht aktualisiert.
- Lokale Folgecommits: b613ae4 (Rahmen Kartenfilter), a1ccc6a (Spezialbuttons20%flacher),81a18de (Zonenprozente),d7641c2 (Statistikseite).
- Nach d7641c2 liegen weitere HRV-/FIT-/Kartenänderungen im Arbeitsverzeichnis, noch nicht abschließend committed. Bei Wiederzugriff zuerst git status/diff und vorhandene Logs prüfen, keine Änderungen verwerfen.
- Keine neue APK beauftragt. Push main startet automatisch einen APK-Build: daher derzeit nicht pushen.

## Noch NICHT umgesetzt – aktueller Dashboard-Auftrag
1. Ohne sichtbare Karte Kartenaktualisierung, Rendering, Kamerafolgen und Kacheldownloads pausieren; beim Wechsel zurück wieder aufnehmen. GPS, Radverbindung und Fahrtaufzeichnung müssen weiterlaufen. Beim Appstart existiert bereits ein hasMap-Guard für mapPanel.resume; der vollständige Lebenszyklus/Wechsel wurde noch nicht geprüft.
2. Dashboard-Auswahl gruppieren: kleine Überschrift „Dashboards mit Karte“ über den neun Kartenlayouts; „Dashboards ohne Karte“ über bestehendem und neuen Layouts ohne Karte.
3. Drei weitere Layouts ohne Karte (Kacheln je Zeile):
   - 1 / 1 / 2 / 2 / 2 = acht Kacheln
   - 1 / 1 / 1 / 1 = vier Kacheln
   - 1 / 1 / 1 / 3 / 3 = neun Kacheln
   Bestehendes Layout ohne Karte erhalten. Footer bleibt bei allen Dashboards ohne Karte sichtbar.
4. Schriftgrößen sichtbar dargestellter Kacheln synchronisieren:
   - 3er-Kacheln wieder dynamisch statt fest.
   - 2er-Kacheln bleiben dynamisch, ebenfalls synchronisieren.
   - Je Gruppe die größte passende Größe jeder sichtbaren Kachel ermitteln und deren Minimum für alle Kacheln derselben Gruppe verwenden. 2er und3er getrennt; 1er nicht ungefragt angleichen.
   - Tatsächliche Textbreite und verfügbarer Platz zählen, nicht nur Zeichenanzahl.
   - Ausgeblendete Kacheln und andere, nicht sichtbare Dashboardseiten dürfen die gemeinsame Größe nicht beeinflussen.
   - Bei Wert-, Feldbelegungs- und Größenänderung neu bestimmen. Zentral über vorhandene DashboardTileStyle-/Textgrößenlogik, keine funktionalen Änderungen am Dashboard.

## Bereits umgesetzt, lokal – für nächste APK erhalten
- Kartenfilter „Auf Karte angezeigt“: inaktiv gleicher zentraler Rahmen wie „Filter zurücksetzen“ (standalone=true).
- Spezial Füllung/Rahmen: sichtbare Pillenhöhe20% geringer bei gleicher Breite; Kontur/Füllung/Glow gemeinsam eingerückt, native Bedienfläche und Padding erhalten. Native Prüfung grün.
- Strecken-Histogramme: Zeitanteile pro Zone unter den Farblinien,9sp statt11sp, schmale Zonen passen sich an. Berechnung aus ursprünglichem Integer-Histogramm vor5W/2bpm-Klassenbildung; acht Prüfungen grün.
- Statistikseite unter Einstellungen nach Strecken: Monats-km mit mehrfacher Jahresauswahl, aktuelle ISO-Wochen horizontal (auch53-Wochen-Jahre), gesamte Fahrzeit je Fahrrad. Bewegungszeit, ersatzweise Fahrtdauer. Verbindlich Profilfarben für die Tortengrafik, unbekannt neutral. Nur origin RECORDED, auch entsprechend markierte Importe. Cache auf vorhandenen Kurzkennzahlen und bestehendem Bibliotheksworker; relevante Änderungen aktualisieren, Zeichnen/Scrollen aggregiert nicht neu. Sechs gezielte Prüfungen erfolgreich. docs/ride-statistics.md und docs/previews/statistics.png, TODO-Punkt erledigt, Commitd7641c2.

## Noch nicht abschließend committed – HRV
- LunarPhases.kt berechnet vier echte Hauptphasen offline nach Meeus Kapitel49 mit Delta-T; lokale Kalendertage, kein Einfluss auf Readiness.
- HrvTrendChart: kleine Neumond-/Vollmond-/zunehmender und abnehmender Halbmond-Symbole auf oberster Rasterlinie; auch bei fehlender Messung, kalendergebunden, mit Scroll/Zoom und Rand-Clipping; gecachte Ereignisse.
- Auswertungsheader „Auswertung: dd.MM.yyyy“: frisch heutiges Datum, erst bewusste Tagesauswahl aktualisiert ihn. Automatische Initialauswahl und Metrikwechsel überschreiben ihn nicht. Chart/View onDaySelected-Callbacks.
- Schlichte Secondary-Header ohne Rahmen/Glow für Auswertung, Messergebnis, Tagesbefinden und HRV-Verlauf. Messheader war bereits schlicht. Statusleistenregel erhalten.
- SettingsHelp.resolve erkennt HRV-Auswertung über Host-/View statt veränderlichen Titel, damit Hilfe erhalten bleibt.
- Tests: LunarPhasesTest2 (April2024-Referenzen/Zeitzonen), HrvMoonChartTest1 (vier Symbole, Flat/Glow, fehlende Tage/Tagesauswahl), HrvChartScrollingTest3, erweiterte HRV-Navigation und Tagesbefinden-/Ergebnisheader erfolgreich.
- Logs /workspace/.tools/hrv-moons-tests.log, hrv-dated-header-tests.log (zunächst Hilfeauflösung fehlgeschlagen, danach korrigiert), hrv-plain-headers-final-tests.log (abschließende beiden Fälle grün). Vorschau app/build/reports/ui/hrv-moons.png noch ins docs-Verzeichnis übernehmen und Dokumentation ergänzen.

## Noch nicht abschließend committed – FIT-Neuerzeugung
- Benutzer will KEINEN neuen Button. Langer Druck (>600ms) auf einen Dienst im Upload-Dialog soll bei eigenen Aufnahmen die Export-FIT mit aktueller Feldauswahl neu erzeugen, danach bisherige Prüfung/erneuter Upload. Auch lokal und bei noch nicht hochgeladenen, startbaren Zeilen.
- User akzeptiert Cloud-Dubletten-/Ersetzungsgrenzen und löscht bestehende Fahrt im Zweifel manuell. Niemals automatisch Cloudfahrten löschen.
- Neue FitReexport.kt: nur eigene recorded-IDs mit vollständiger interner Analyse-FIT; aktuelle Konfiguration des zugeordneten Fahrradprofils (nicht ungefragt das aktive andere Rad). Ohne passende Profilzuordnung verständlicher Fehler. Importe/unverfügbare volle Analyse bleiben beim bestehenden Übertragungsweg.
- Eigene derivative Datei fit-library/reexported/ID.fit atomar validiert; Original/Analyse/ID bleiben unverändert. Aktive FIT priorisiert diesen Export, Datenauswertung bleibt vollständig.
- Standard- und Developer-Felder nach Feldauswahl filtern; keine Messwerte erfinden. Aufgezeichnete Primärdaten bleiben die historische Datenbasis. Vorhandene Höhenkorrektur überlagern; spätere Höhenkorrektur aktualisiert Export, ohne dessen Feldmaske zu verlieren.
- FitLibrary: neue aktive Exportversion, importierte identische Rückläufer erkennen, beim Löschen Export mitentfernen/aktuellen Hash ausschließen. Analyse und Höhen-Summen nicht aus reduziertem Export beziehen.
- RideDirectoryBackup sichert/restauriert optional diese Exportversion zusätzlich zu Original/voller Analyse/echter Höhenkorrektur.
- MapState.recheckTransfer erzeugt Export vor bestehender Übertragungsaktion neu; lokaler Export nutzt denselben Pfad. Bestehende Job-/Kontoverknüpfungen erhalten.
- RouteTransferRecheck: bei geändertem Dateihash trotz vorhandener Remote-Datei expliziten neuen Upload anstoßen, vorhandenen Link behalten; nur nach bestätigtem Fehlen Link entfernen. Netzwerkfehler sind kein Löschbeleg. WebDAV bestehende Konfliktsicherheit erhalten.
- 25 gezielte Prüfungen grün: FitReexportTest3, RouteTransferRecheckTest7, RouteTransferMenuTest5, RideDirectoryBackupTest9, MapProviderSettingsTest1; Log /workspace/.tools/fit-reexport-map-tests.log.
- Zusätzlicher letzter Auftrag lief beim Umgebungsausfall: RouteTransferMenuTest.completedRowsOfferRecheckWithoutChangingTheirNormalTapActionAndRespectBusyState erweitert um Langdruck vor erstem Upload, dazu FitLibraryTest und RouteTransferPayloadTest. Ob Shell-Erweiterung/Testlauf ausgeführt wurde, ist unbekannt. /workspace/.tools/fit-reexport-library-final-tests.log prüfen; ggf. gezielt nachholen. Keine Gesamtsuite.
- FIT-Dokumentation/CONTINUE-HERE aktualisieren, Diff prüfen und abschließenden Commit erstellen.

## Noch nicht abschließend committed – Einstellungen/Karte
- Im gemeinsamen mapControls() die Schalter „Fahrtrichtung oben“ und „GPS automatisch folgen“ entfernt, ebenso den folgenden Trenner map_track_divider.
- Kleine Überschrift „Kartenoptionen“, dann die vier bestehenden Optionen: eigene Strecke, importierte GPX, Höhe am aktuellen Punkt, Höhenkorrektur. Späteren Trackdarstellungs-Trenner erhalten.
- mapHeading/mapFollow nicht mehr durch diese Einstellungsseite schreiben; Werte/Funktionen/Backup/Kartenbuttons erhalten.
- Erweiterten vorhandenen MapProviderSettingsTest erfolgreich geprüft.

## Abschluss bei wieder erreichbarer Umgebung
- Zuerst lokalen Stand/Logs prüfen, fehlenden Abschluss der bereits geschriebenen Änderungen erledigen und dokumentieren.
- Danach neue Dashboard-Aufträge umsetzen, zentrale Layout-/Schrift-/Karten-Lifecycle-Komponenten verwenden; passende gezielte Prüfungen, keine unnötige Gesamtsuite.
- Typografie: python3 scripts/check-ui-typography.py; Atlas: python3 scripts/build-ui-catalog.py [--check]; git diff --check.
- Offline-Tests: JDK21 /workspace/.tools/jdk-21, ANDROID_HOME /workspace/.tools/android-sdk, ANDROID_USER_HOME /workspace/.tools/android-user, GRADLE_USER_HOME /workspace/.tools/gradle; Gradle --offline --no-daemon und passende --tests.
- Akku-Diagnosemodus wurde nur besprochen, noch NICHT beauftragt/implementieren. Vergleichsfahrten geplant mit B|Cycle/Bosch Flow/Locus Map jeweils mit Radverbindung. Neuer verbindlicher Auftrag ist allein das Pausieren unsichtbarer Karten bei weiterlaufendem GPS.

## Neuer Fehlerauftrag: Dashboard-Feldauswahl → Daten → Akku (hohe Priorität)
- Seit Hinzufügen des Schaltwerk-Akkus fehlen in der Akku-Kategorie die Auswahlschalter.
- Bei Auswahlversuchen wird teilweise ein Feld aus einer darunterliegenden Kategorie aktiviert.
- Noch NICHT untersucht oder behoben, da Umgebung weiter offline. Keine bestätigte Ursache.
- Zuerst die Render-/Adapter-/Listener-Zuordnung in FieldPicker prüfen: sichtbare Zeilen und stabile Feld-IDs müssen übereinstimmen, insbesondere nach Kategorieüberschriften, Akku-Sammelfeld und Schaltwerk-Eintrag. Keine feste Indexkorrektur ohne Ursachenprüfung.
- Schalterdarstellung und Zuordnung bei allen Akku-Einträgen sowie angrenzender Kategorie prüfen; bestehende Ein-/Mehrfachauswahl erhalten.
- Passende gezielte Regression: jeden sichtbaren Akku-Eintrag und Einträge danach auswählen und erwartete Feld-ID/Schalterzustände prüfen. Beide Designs berücksichtigen.
- Diesen Funktionsfehler vor den neuen Dashboard-Layouts/Schriftanpassungen beheben. Andere offenen Aufträge bleiben erhalten.

