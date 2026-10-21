# Übergabe an einen neuen Chat

Stand: 21.10.2026. Repository `Titanyus/B-Cycle`, Android/Kotlin, native Views.

Aktueller Abschluss: Alle fünf Schritte des vereinbarten Architekturplans umgesetzt und separat geprüft. Letzte APK **0.8.21 / Build 60 / versionCode 91**, [direkter Download](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.21-release-60.apk), [Buildnachweis](build-60.md), einschließlich nachfolgender Optikkorrekturen. Keine offenen Implementierungsschritte aus diesem Plan. Frühere Aufträge und Zwischenstände unten sind Verlauf, nicht der aktuelle offene Auftrag.

Diese Datei ersetzt den fehlenden Chatverlauf als Arbeitsübergabe. Sie enthält keine persönlichen Fahrten, RR-Daten, Zugangsdaten oder Signierschlüssel. Zuerst `AGENTS.md` lesen, dann Git-Status und diese Datei prüfen. Uncommittete Änderungen können aus der bisherigen Umgebung stammen: erhalten und vor Weiterarbeit prüfen. Git überträgt nur committete und gepushte Dateien; ein anderer Chat kann lokale Änderungen der alten Umgebung nicht automatisch sehen.

## Aktueller Auftrag

Nach Build 60: Namensvorschau im Eingabefeld anhand der letzten gefahrenen Fahrt, Querstrich vor Trackaufzeichnung in Karten, automatisches Scrollen beim Aufklappen der Trainingshilfe sowie HRV-Optik (Achsbeschriftungen 50 %, Diagramm +30 %, Kreislinie +35 %, Endpunkt +40 %). Nur gezielte Tests. Anschließend neue APK beauftragt: Version 0.8.22 / versionCode 92 wird über GitHub Actions gebaut. Vorschau ausschließlich gezeichnet, kein Bestandteil des gespeicherten Grundnamens oder der tatsächlichen Fahrtbenennung. Messung, Berechnungen und Datenformate unverändert.

Umgesetzt und geprüft: fünf gezielte Robolectric-Tests erfolgreich (VisualFollowupTest: Vorschau/Editable/Speicherung einschließlich leerem Grundnamen und 60-Zeichen-Grenze, Trainingshilfe-Scrollen ohne Auswahländerung, Kartentrenner; vorhandener profilbezogener RideNaming-/Backup-Test; native HRV-Render-/Theme-Prüfung mit 286-dp-Graph und tatsächlicher 50-%-Textalpha). Native Vorschau und HRV-Ansicht visuell geprüft. Kein Geräte-/BLE-Test für diese optischen Änderungen; Neue APK 0.8.22 beauftragt; Build und Veröffentlichung laufen. Build 60 enthält diese Ergänzungen noch nicht.

## Entscheidungen und Änderungen im aktuellen Arbeitsstand

- HRV: Ruhephase 15–60 s, Messphase 60–300 s, beide in 15-s-Schritten, Standard 30/180 s. Öffentliche Einstellungen vollständig sichern/wiederherstellen.
- HRV-Artefakte: neue Messkorrektur übernimmt nur bessernde Durchläufe. Mehr als 5 % veränderte Rohintervalle allein blockieren bei vollständig bereinigter Folge nicht mehr; orange Warnung. Restauffälligkeiten, Kontakt-/Empfangsabbruch und technische Fehler bleiben getrennte Ablehnungsgründe. Ältere Ergebnisse nicht rückwirkend verändern. Rohdaten erhalten. Wissenschaftliche Quellen und Grenzen: `hrv-artifact-research.md`.
- Nutzer berichtete nur eine erfolgreiche von sechs Messungen mit H10/Cycplus H2. Fehlgeschlagene Rohmessungen liegen noch nicht zur Prüfung vor; keine Erfolgsquote versprechen.
- HRV-Kreis: groß, Ruhe FOREGROUND/Endpunkt ACCENT, Messung umgekehrt, Endpunkt 1,3 × Linienbreite. Monotones kontinuierliches Fortschreiten. ACCENT-Glow 30 → 10 % in 1,5 s, Neustart im Pulsrhythmus; Visualisierung verändert niemals RR. Phase als kleine Beschriftung im Kreis über Puls, Zeit darunter, bpm wie Dashboardeinheit. Rahmenloser Textbutton im Kreis unter Zeit. Keine übrigen Textinfos während Messung. Detaillierte Zusammenfassung danach, einschließlich Pulsmesser, Protokoll, Zeit-/Frequenzwerten und Qualität. Nur HRV-Mess-/Ergebnisheader in Fensterfarbe.
- HRV-Graph: 60-s-Fenster, erst links füllen, anschließend kontinuierlich verschieben, 10-s-Ticks. Vollständige Daten speichern. LF/HF neu berechnen, Methode/versionierte Parameter speichern und Schätzgrenzen erklären.
- Daten & Speicher: Sticky-Abschnitt HRV-Pulsdaten, manueller ZIP-Export mit einer vollständigen JSON je Messung (auch abgelehnte), Rohpaketen, RR/NN, Korrekturen, Qualitätsgründen und Versionsangaben. Kein automatischer Upload.
- Dreeve: vier Zeilen pro Fahrt (Titel; Datum · km · Zeit; Fahrradname (Typ); GPX-Verfügbarkeit). Oben nur geladene Anzahl, Beschreibung im Info-Dialog. Oberster Importfilter Alle Tracks / Noch nicht importiert. Keine 50-Fahrten-Grenze.
- Dreeve als gefahren importierte GPX behalten bestätigte Dreeve-Verknüpfung und können bewusst als echte FIT-Aktivität zu anderen Diensten übertragen werden. Geplante Tracks/Zusammenfassungen bleiben davon ausgeschlossen. Keine automatische Übertragung beim Import.
- Aktueller Typ des zugeordneten lokalen Fahrradprofils gilt. Nach Profillöschung letzten Typ über stabile UUID erhalten; Slotwiederverwendung darf alte Fahrten nicht neu zuordnen. Originaldateien nicht überschreiben; Übertragung/Sicherung mit aktuellem Typ ableiten. Keine automatische nachträgliche Änderung schon vorhandener Remote-Aktivitäten behaupten.
- Normierte Leistung: reale Bosch-Flow/Dreeve-GPX enthielt 19 stationäre Pausen, insgesamt 26 min. Erkannte Pausen werden aus aktiver Auswertung herausgerechnet, keine globale Lockerung der Power-Qualitätsgrenzen. Lokale Prüfung: NP ca. 155,15 W. Private Originaldatei nicht ins Repo übernehmen.
- ZIP-Import: Auswahlfenster sofort öffnen, während Entpacken mittig Verarbeitungshinweis; danach gleiche Filter/Sortierung/Mehrfachauswahl wie Dreeve. Nur gewählte Fahrten importieren, Originale erhalten, temporäre Daten bereinigen.
- Gemeinsamer Fastscroller für Strecken-/Importlisten: kompakter Infokasten, SECONDARY-Schrift, Themewechsel unterstützen.
- Textscroller: 3 s einblenden, 4 s Halt, 4 s beschleunigen, 18 dp/s, 4 s abbremsen, 4 s Halt, 3 s ausblenden, 2 s leer, Loop. Gemeinsame Konvention: `text-scrollers.md`.

## Abschlusscheck

Diese Übergabe nach Implementierung und Prüfung um tatsächliche Test-/Build-Ergebnisse und den APK-Link ergänzen. Noch offene Punkte ausdrücklich nennen, nicht als erledigt darstellen. `git diff` und relevante Tests sind maßgeblich; diese Liste allein ist kein Umsetzungsnachweis. Sichtbare IN-/OUT-Angaben, Hilfen, Backup und Projektdokumentation bei geänderten Wegen aktualisieren.

Bei jedem zukünftigen größeren Auftrag und vor einem Umgebungswechsel diese Datei aktualisieren: Ziel, Entscheidungen, erledigt/offen, Tests, Build-/Commit-IDs. Keine vollständigen privaten Chatprotokolle ins öffentliche Repository kopieren.

## Nachweis im Arbeitsstand 0.8.17

Implementierung der oben aufgeführten Punkte abgeschlossen. 144 unterschiedliche Android-Hosttests und 19 HRV-Kerntests bestanden (einschließlich 62 Referenzfällen innerhalb der Kerntests). Native HRV-Messansicht gerendert und visuell kontrolliert. Die bereitgestellte private Flow-GPX wurde nur lokal geprüft; keine persönlichen Daten übernommen.

Drei ältere UI-Tests schlagen im breiteren Lauf fehl und wurden am unveränderten Ausgangscommit `734d5e3` identisch reproduziert: `SettingsDesignMigrationTest.fieldDropdownAndNewRowsUseActiveThemeWhileSourceSelectionRemainsFixed`, `SettingsDesignMigrationTest.mainScreensAndAdvancedDialogsRecolorWithoutChangingGeometryOrNavigation`, `SettingsSectionsTest.newSectionsUseOneScrollHostAndNumericRowsStayInlineAcrossThemes`. Veraltete Annahmen zu Dropdownhintergrund, Schriften und Bosch-Abschnitt; in diesem Auftrag nicht korrigiert. Diese Fehler nicht als neue Regression oder als bestandene Tests ausgeben.

Offen für die Praxis: neue H10-/H2-Messungen auswerten, insbesondere zuvor abgelehnte Rohfolgen. Echte WebDAV-/intervals-/Dreeve-/Strava-Konten und physische Bedienung am Telefon sind durch Mock-/Hosttests nicht abgedeckt. Die fünf fehlgeschlagenen Messungen des Nutzers liegen nicht als RR-Rohdaten vor. Kein bestimmter Erfolg der neuen Korrektur zugesichert.

Abgeschlossen: signierte Release-APK 0.8.17 / Build 56 / versionCode 87 über GitHub Actions erfolgreich gebaut und geprüft. Quellcommit `280bd90a6b4a60791ebe95c6298d77737767aec7`. [Direkte APK](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.17-release-56.apk), [Build-/Testnachweis](build-56.md), [Änderungen](release-0.8.17.md). Dauerhafte Signatur wie Builds 53/55, APK 2.082.919 Bytes, kein Debug-Flag. Keine offenen Implementierungspunkte aus diesem Auftrag; oben benannte Praxistests und bekannte ältere UI-Testfehler bleiben offen. Die letzte Ergänzung der Paket-/Capture-Diagnostik wurde nochmals gezielt erfolgreich geprüft.

## Kleine Folgeänderung nach Build 56

Dreeve-/gemeinsame ZIP-Auswahlliste kompakter: vertikales Blockpadding 4 statt 8 dp und kein zusätzliches Fontpadding der vier Textzeilen. Streckenname im zentralen 900-Black-Schnitt bei unveränderter Textgröße; Datum zusätzlich mit Startzeit HH:mm. Nur vorhandenen Zeilen-UI-Test gezielt prüfen. Nutzer ausdrücklich: minimale Tests, keine APK. Build 56 enthält diese Folgeänderung noch nicht.

Dashboard-Fahrradauswahl zusätzlich als 3-Spalten-/2-Reihen-Raster bei sechs Profilen, gleiche volle Kachelhöhe, Bild- und Schriftgröße wie im Profilmanager. Bestehende Auswahl-/Aufnahmesperren erhalten; weiterhin keine APK.

Pulsmesser-Folgekorrektur: Beim bewussten Profilwechsel bestehende bestätigte Live-Verbindung weiterverwenden; Ziel ohne Pulsmesser übernimmt Adresse/Name dauerhaft. Ziel mit eigenem anderem Sensor oder deaktiviertem Puls gewinnt. Fahrtmittelwerte zurücksetzen, keine zweite GATT-Verbindung. Nutzerbeobachtung: erneutes Android-Koppeln machte H10 wieder auffindbar; hardwareseitige Ursache nicht abschließend belegt. Nur gezielte Profilwechseltests, weiterhin keine APK.

Abschluss der Folgeänderungen: 9 gezielte Tests bestanden (1 Dreeve-Zeile, 3 BikeQuickPicker, 1 Dashboard-Einstieg, 2 Pulsmesser-Wiederverwendung und 2 bestehende Profil-Runtime-Tests). Keine APK erstellt; Quelländerungen mit `[skip ci]` sichern, damit kein automatischer APK-Build ausgelöst wird. Build 56 bleibt der letzte veröffentlichte APK-Stand. H10-Verhalten am realen Gerät noch prüfen.

## Rückmeldung am BLE-Suchbutton

Nutzer klärte die scheinbar wirkungslose Suche selbst auf: Eine Aufnahme lief, deshalb war die Sensorsuche absichtlich deaktiviert. Nur Darstellung verbessern: während Aufnahme bzw. Speichern klarer Sperrtext auf dem Button, Alpha 40 %, passende Accessibility-Zustandsbeschreibung; anschließend normale Beschriftung/volle Deckung. Keine Änderungen am BLE-Suchalgorithmus, keine APK. Weitergehende Suchprobleme nur prüfen, falls sie ohne Aufnahmesperre erneut auftreten. Gerät ist Matrix ICR50 / JFICycle, im Projekt über FTMS unterstützt; aktives Profil muss Indoor sein.

Gezielter UI-Test für Aufnahme-/Speichersperre und anschließende normale Darstellung erfolgreich. Quelländerung mit `[skip ci]`, kein APK-Build.

## Aktueller Folgeauftrag: Trend, Graphfenster und Fahrtnamen

Umsetzung: zusätzliche Trendchevrons positiv oberhalb / negativ unterhalb des festen ersten; Graph-Accordion mit Voll/30s/60s/120s je Seite/Feld; fünf Optionen sensor-/quellenabhängig gedimmt; rollierende Aufnahmezeit auch im Stand, jüngste 120 s gesondert erhalten. Profil → FIT-Aufzeichnung → Fahrten benennen mit Text und fünf Mehrfachsegmenten (Datum/Startzeit/Distanz/Bewegungszeit/Ø Tempo). Format exakt `Feierabendrunde · 21.10.2026 17:30h · 32,4km · 1:15:20h · 25,8km/h`. Vorlage/Zeitzone/Sprache beim START fixiert, beim Speichern reale verfügbare Kennzahlen verwenden, manuelle Retry-Namen erhalten. Neue Einstellungen in Profil-/Dashboard-/FIT-Sicherung; Wiederherstellungspläne derselben Preference-Datei zusammenführen. Details docs/ride-naming.md und docs/live-graph-overlays.md. Keine APK beauftragt.

Zwischenfrage zur Trink-Erinnerung beantwortet: bei vorheriger Aktivierung ab Aufnahmestart, bei Aktivierung/Intervalländerung während Aufnahme ab dann neues volles Intervall; Stillstand zählt, Fenster 15 s oder bis Antippen, höchstens eine verdeckt fällige Erinnerung nachholen. Keine Funktionsänderung daran.

Abschluss: 67 gezielte Tests erfolgreich über die betroffenen Graph-/Trend-/Namens-/Profil-/Backup-Suiten. Enthalten: exaktes Namensformat, fehlende Kennzahlen, Startvorlage statt später geänderter Vorlage, manueller Name bei Retry, gleichzeitige Wiederherstellung alter FIT-Einstellungen und neuer Vorlage; Quellverfügbarkeit, zeitabhängige Kurvenpositionen auch im Stand, 1-Hz-Fenster, abschließender vollständiger Gesamtgraph bei STOPP, Backup/Profile und Chevron-Deckkraft/-Geometrie. Native Graphdarstellung geprüft. Keine APK erstellt. Letzte veröffentlichte APK bleibt Build 56; diese Quelländerungen mit `[skip ci]` sichern.


## APK-Folgeauftrag 0.8.18 und App-To-dos

Nutzer hat anschließend eine APK beauftragt. Version 0.8.18 / versionCode 88, GitHub Actions Build 57 aus Quellcommit `9e3a29cc5c0475e2bee8046ba5df2c75368f858d`. Erfolgreich gebaut und geprüft: 2.098.575 Bytes, dauerhafte Signatur wie Build 56, Version 0.8.18/88 ohne Debug-/Test-Flag, SHA-256 und zipalign korrekt. [Direkte APK](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.18-release-57.apk), [Buildnachweis](build-57.md). GitHub-Startseite aktualisiert. Wartungsanbindung Dreeve ↔ App und „Trainer einbauen“ in [App-To-dos](../TODO.md) festgehalten; nur vorgemerkt, nicht implementieren.


## Architekturentkopplung – Schritte 1 und 2

Beauftragt sind ausschließlich Referenztests und interne typisierte Speicherergebnisse. Callback `(String, Boolean)` zu RideRuntime/UI vorerst unverändert. Kein Feldkatalog-Umbau, keine Messdaten-/Sensorumstellung, keine APK.

Schritt 1: Unveränderten Feldkatalog (IDs, Reihenfolge, Einheiten, Scope, Labels, Gruppen und Defaults) und synthetisches Journal aus `3e7db0e` als Testreferenzen festgehalten. Zusätzliche Prüfungen für Reichweitenzuordnung, Speichererfolg, Teilerfolg mit Originalerhalt/Retry, gemischte Wiederherstellung, ungültiges Journal und Schreibfehler. Neue Referenztests sowie bestehende RecordingFlow-/RouteExport-Tests erfolgreich, bevor Produktionscode geändert wird.

Schritt 2: `RecordingResult.kt` definiert Fortschritt/Fehler sowie Completed/LocallyRetained/NotSaved mit bekannter Fahrt-ID, Fehlerphase, Zielvormerkungen und Warnungen. `RecordingStore` verwendet diese intern, Wiederherstellung zählt Ergebnistypen. `RecordingStatusText` rendert unveränderte Meldungen aus Stringressourcen; äußerer Callback und RideRuntime/MainActivity bleiben unverändert. Ziele bedeuten Vormerkung, keine Remote-Bestätigung. Worker/Reihenfolge/Dateiformate/Retry beibehalten. Details: [Speicherergebnisse](recording-results.md). Schritt 1 separat in `c4a7aa0` gesichert.

Abschluss Schritte 1/2: vor Produktionsänderung 36 Referenz-/RecordingFlow-/RouteExport-Tests erfolgreich; nach Umstellung abschließend 72 gezielte Tests erfolgreich (RecordingFields, RecordingStoreBaseline, RecordingResult, RecordingFlow, RouteExport, RideNaming, Intervals, FitWriter, PerformanceJournal, PerformanceFit, IndoorFit). Enthalten: typisierte tatsächliche Exportergebnisse/Fehlerphasen, lokale Originale/Retry, gemischte Wiederherstellung, Reihenfolge aller Meldungszusätze und unveränderte Zähler bei anderer Zahlensprache. Keine APK, keine neuen Abhängigkeiten. Schritte 3–5 des Plans bleiben offen.


## Schritt 3 mit eigener APK

Nutzer beauftragt den typisierten Callback bis RideRuntime/MainActivity und anschließend eine neue signierte APK. RecordingStore liefert RecordingUpdate auf dem Main-Thread; Laufzeit steuert saving/Bibliotheksaktualisierung nach Typen und bietet recordingStatus als abgeleiteten Text. UI bestimmt Feedback nach Ereignis und behält textbasierte Unterdrückung identischer aufeinanderfolgender Anzeigen. START setzt altes Ergebnis zurück, STOPP setzt sofort SAVING_FIT. Foreground-Service verwendet unverändert den abgeleiteten Text und die Aufnahmesperren. Feldkatalog-/Messdatenumbau (Schritte 4/5) nicht beauftragt und unverändert.

Schritt 3 geprüft: 62 gezielte Tests erfolgreich, einschließlich neuer Runtime-/Activity-Prüfungen, bestehender Aufzeichnung auf SDK 33/35/36, Service im Hintergrund, Retry, Referenzkatalog/Altjournal, Fahrtnamen, intervals.icu, Indoor-Runtime und Sensorsuch-Sperren. Kein vollständiger Gesamttestlauf. Neue Version 0.8.19 / versionCode 89 wird über Actions gebaut.

Abschluss Schritt 3: signierter optimierter Release 0.8.19 / Build 58 / versionCode 89, Quellcommit `265f3f0a1d16bd0baf0459c2b52d7e16173a790e`. APK 2.104.847 Bytes, SHA-256 `b96af0ac19d47e2865bca89745adf29a949b7286d26a9db76df452928bd146b8`, Signatur wie Build 57, Version/kein Debug-/Test-Flag/zipalign geprüft. [Direkte APK](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.19-release-58.apk), [Buildnachweis](build-58.md). README aktualisiert. Reine Veröffentlichung mit [skip ci], damit kein weiterer Build startet. Schritte 4/5 weiterhin offen.


## Schritte 4/5 mit APK beauftragt

Schritt 4 abgeschlossen: fachlicher RecordingFields-Katalog unabhängig von DashboardField; RecordField nur noch UI-Adapter in RecordingFieldLabels. RecordingConfig, Journal, FIT, Indoor und beide Backup-Validierungen verwenden die fachlichen Definitionen. Alle 46 Definitionen einschließlich Smartphone-Altdefinitionen, Reihenfolge/Labels/Einheiten/Defaults erhalten. Synthetische FIT-Referenz vor Produktionsänderung aus `ae5bf89` erzeugt. 48 gezielte Tests erfolgreich; danach letzte Backup-Adapter-Abhängigkeit entfernt und Katalog-/Supplement-Tests erneut erfolgreich. Altjournal bytegleich, FIT-Records/Summaries/Developer-Definitionen gleich; reine Appversionsfelder separat geprüft. Details docs/recording-fields.md. Schritt 5 und neue APK folgen innerhalb desselben Auftrags.

Schritt 5 abgeschlossen: numerische Reichweite gemeinsam über RideMeasurements, keine Messwerte aus Dashboardtexten. Bisherige LiveData-/LiveMetric-Formatierung unverändert als Erweiterungsfunktionen in LiveDataPresentation ausgelagert. Keine Sensor-/Quellen-/Frische-/Berechnungsänderungen. Abschließend 105 gezielte Testausführungen erfolgreich, einschließlich JVM-Messwertprüfung ohne Android/Dashboard, Katalogreferenz, bytegleichem Journal, FIT-Referenz trotz neuer Appversion, UI/Indoor, Backup/Profilen, Fahrtnamen und Aufnahme/Service auf SDK 33/35/36. Alle fünf geplanten Schritte sind umgesetzt. Version 0.8.20 / versionCode 90 für neue APK vorbereitet; gezielte Tests, kein Gesamttestlauf.

Abschluss APK: Build 59 erfolgreich aus `4b696c209d888ea8552e3758b6718c2e99f7a636`, optimiert und dauerhaft signiert wie Build 58. 2.107.835 Bytes, SHA-256 `52204fb6cb6b23470656ac8868710abf83ac03d38cf02bad7782d180f968bafc`. Version/kein Debug-/Test-Flag/Signatur/zipalign geprüft. README und direkte APK aktualisiert. Veröffentlichung mit [skip ci], kein zusätzlicher Build.


## Optikkorrekturen nach 0.8.20

Nutzer ausdrücklich: nur Optik, Funktionalität vollständig erhalten, nur nötige Tests. Fahrtnamenslabel „Fahrtzeit“ statt „Bewegungszeit“, berechnete Bewegungsdauer/IDs unverändert. Segmentierte Auswahlen global mit umlaufendem gemeinsamen Rahmen; zusätzlich Graph-Zeitfenster und Fahrtnamensauswahl auf gemeinsamen Helper umgestellt. Stacked-Iconsegmente zeichnen Icon/Text als Block mit 2 dp Abstand; zentrale 8-dp-Abstände für Inputfactory und generische EditTexts auch nach LayoutParams-Ersatz. Seitenanfang 16 dp, führende Sticky-Header 0 dp, verschachtelte/dynamisch eingefügte Wrapper zentral normalisiert. Reines Styling, keine Daten-/Auswahl-/Navigationsänderungen. 11 gezielte UI-Tests erfolgreich, nativ gerendert/visuell kontrolliert. Regeln in AGENTS.md und docs/ui-conventions-audit.md festgehalten. Keine APK beauftragt/erstellt; Build 59 enthält diese Folgeänderungen noch nicht. Quelländerungen mit [skip ci] sichern.

Nutzer hat anschließend neue APK beauftragt: 0.8.21 / Build 60 / versionCode 91 erfolgreich über GitHub Actions aus `8b29f37ef3a3ac36a9601dbe87498e6177d692bd` gebaut. Optikkorrekturen enthalten. 2.108.343 Bytes, SHA-256 `ee4514500142449e4ebd80ecbe9cb491bde8fdf370224232f88bd9d7b270ba8c`, permanente Signatur wie Build 59, Version/kein Debug-/Test-Flag/zipalign geprüft. Direkte APK und README aktualisiert; Veröffentlichung mit [skip ci], kein zusätzlicher Build.
