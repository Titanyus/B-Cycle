# Übergabe an einen neuen Chat

Stand: 22.10.2026. Repository `Titanyus/B-Cycle`, Android/Kotlin, native Views.

Arbeitsstand 23.10.2026 nach Build 69 (noch keine neue APK beauftragt): Linien-Glow bei Dashboard-Graphen und Leistungs-/Pulszonen-Verteilungen entfernt, eigentliche Linien/Flächen erhalten. HRV-Tagesangebot wechselt beim Vordergrundstart zum Dashboard (auch aus erhaltenen Einstellungen) und bleibt bis zum Tap auf Herz/Hintergrund wiederkehrend. Neuer lokaler handledDay-Marker statt lastDay; Lifecycle/Zurück verbrauchen nichts. Laufende Aufnahme/Speicherung/HRV und Speicherfehler unterbrechen wir nicht. Details docs/hrv-entry.md. Elf gezielte Tests für Tagesangebot/Lifecycle/RR-Autostart, Sicherung/Signale, glowfreie Dashboard-Kurven und helle/dunkle Leistungs-/Pulszonen erfolgreich. Typografie-/Atlasprüfung und diff --check erfolgreich; kein Hardwaretest und keine neue APK.


## Aktueller Abschluss: APK 0.8.30 / Build 69

APK **0.8.30 / Build 69 / versionCode 100** aus `def820992a58bf09485dadcad38d31cbe695c8d7` über Actions gebaut und direkt veröffentlicht. [Download](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.30-release-69.apk), [Buildnachweis](build-69.md). 2.228.390 Bytes; SHA-256 `a2471bba1429267794ad11b68eb1a870d0f835b9565744472b1909fdf9b957d2`. Dauerhafte Signatur wie Build 68, Version, fehlende Debug-/Test-Flags und Ausrichtung geprüft. Enthält alle zuvor lokalen HRV-Änderungen: Löschen/Wiederimport, neuer Messbildschirm, Balkenbeschriftung und kontinuierliches Rand-Clipping. Sechs gezielte Tests erfolgreich, native Darstellung geprüft; kein Gesamtlauf oder Hardwaretest. Alles auf main und in APK enthalten, kein offener APK-Auftrag. Frühere Angaben zu lokalen Änderungen/fehlender APK unten sind Verlauf.


Aktueller Auftrag nach Build 68: sehr kleine Tageswerte oberhalb der HRV-Balken (8 sp, 3 dp Gap), gegen Überlappung/Clipping geschützt. Zahlenformat max. eine Nachkommastelle, Daten/Skalen unverändert. Ausdrücklich keine APK.


Neu danach (ohne APK): HRV-Übersicht mit Papierkorb je Messung und Bestätigung. HrvStore.delete entfernt die vollständige Messung per verstecktem Rename; Recovery räumt Löschreste auf. Kein Tombstone: CSV-Reimport ergänzt gelöschte Identitäten. Tagesregel bleibt: ältere Messung desselben Tages kann wieder erscheinen. Controller sperrt Löschen während Messung/ausstehender Speicherung und entfernt gelöschtes Live-Ergebnis. Messbildschirm: Puls 90 statt 72 sp, Zahl allein horizontal zentriert, bpm dahinter; Textblock 10 dp höher; zweizeilig „Atme ruhig / und entspanne dich.“ statt Bereit; Start-Pille horizontal 24 dp Padding und 8 dp vertikaler Rahmen-Inset bei unveränderter 48-dp-Touchhöhe. Einstellungen-Pillen bleiben unverändert. Geprüft: drei gezielte Tests (Löschen/Wiederimport inkl. Recovery, UI-Bestätigung/Abbrechen und Live-Ergebnis-Reset, natives Rendering inkl. Zahlenzentrierung/Themewechsel/Pille) erfolgreich; Typografie- und UI-Katalogprüfung ebenfalls erfolgreich. Keine APK erstellt.


## Aktueller Abschluss: APK 0.8.29 / Build 68

APK **0.8.29 / Build 68 / versionCode 99** aus `8fa9e77799b35e54ea4391e0672f69793c602072` erfolgreich über Actions gebaut und direkt veröffentlicht. [Download](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.29-release-68.apk), [Buildnachweis](build-68.md). 2.224.554 Bytes; SHA-256 `b4e92d17c61b302cd06e6eeff663b4785f26482059a3c692bf807cd01ad7f8c9`. Dauerhafte Signatur wie Build 66, Version, fehlende Debug-/Test-Flags und Ausrichtung geprüft. Enthält kombinierte Schlafzeiten, neue Schlafdauer, engeren Auswahlpunkt-/Textabstand sowie größeres HRV-Diagramm, feste Y-Achsen, neuen Farbsatz und gemeinsamen Bestand. 15 neue gezielte Tests und zuvor zehn Auswertungstests erfolgreich, kein Gesamtlauf/Hardwaretest. Alles auf main und in APK enthalten, kein offener APK-Auftrag. Ursache des konkret fehlenden Messwerts bleibt ohne App-Journal ungeklärt; keine Daten wiederhergestellt oder extrapoliert. Frühere Angaben unten zu fehlender APK oder nur lokalen Änderungen sind Verlauf.


Aktueller Auftrag nach Build 66: HRV-Diagramm mit fester Y-Skala aus gesamten geladenen Daten (Puls fünf bpm Reserve, andere Werte fünf Prozent), keine vertikale Änderung durch Zoom/Pan. Balken ACCENT, unter Normalbereich weiß; Mittel weiß, Korridor weiß 30 % und gepunktete Grenzen. Diagramm +40 % Höhe (504 dp), linken Platz ausnutzen ohne Skalentext-Clipping. Keine neue APK angefragt. Details hrv-trends.md.


## Aktueller Abschluss: APK 0.8.27 / Build 66

APK **0.8.27 / Build 66 / versionCode 97** aus `aea6b1ec24ba509ba1e85ffd954c84ec43917a2b` erfolgreich über Actions gebaut und direkt veröffentlicht. [Download](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.27-release-66.apk), [Buildnachweis](build-66.md). 2.221.754 Bytes; SHA-256 `44843f32ca2fa8ead0379890629a59ddb2669ff25f90c891823ccc66a35f35a1`. Dauerhafte Signatur wie Build 65, Version, fehlende Debug-/Test-Flags und Ausrichtung geprüft. Enthält elf Sliderstufen, gerundete HRV4-Antworten mit Ergänzung fehlender importierter Antworten beim Öffnen und Skalenmigration sowie weißen 40-%-Korridor mit gepunkteten 1-dp-Grenzen. 21 gezielte Tests erfolgreich, kein Gesamtlauf/Hardwaretest. Alles auf main und in APK enthalten; kein offener APK-Auftrag. Frühere Angaben unten zu fehlender APK oder nur lokalen Änderungen sind Verlauf.


Aktueller Auftrag nach Build 65: Tagesbefinden auf elf Stufen 0–10, gerundete HRV4-Antworten einschließlich Ergänzung fehlender Antworten vorhandener Importe beim Öffnen. Alte Schema-1-Werte proportional übertragen und in legacyRatings erhalten; neue Antworten Schema 2, unveränderte Ergebnisdateien/Originalfelder. Korridor weiß 40 %, oben/unten weiß gepunktet 1 dp. Keine APK angefragt. Details hrv-questionnaire.md / hrv-trends.md.


## Aktueller Abschluss: APK 0.8.26 / Build 65

APK **0.8.26 / Build 65 / versionCode 96** aus `2275327ce05806d5c44875d13efad4547af8dcd5` erfolgreich über Actions gebaut und direkt veröffentlicht. [Download](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.26-release-65.apk), [Buildnachweis](build-65.md). 2.219.870 Bytes; SHA-256 `40e220d8c4e08d8f362fbac3113144d6da53cb22fb748fc904c737b756540047`. Dauerhafte Signatur wie Build 64, Version, fehlende Debug-/Test-Flags und Ausrichtung geprüft. Enthält neue HRV-Auswertung mit beiden Einstiegen und CSV-Prüfanzeige-Korrektur. 27 gezielte Tests erfolgreich, kein Gesamtlauf oder Hardwaretest. Alles auf main und in APK enthalten; kein offener APK-Auftrag. Frühere Angaben unten zu „nur lokal“, „keine APK“ oder Build 64 sind Verlauf.


Aktueller Auftrag: eigene HRV-Auswertung mit Kennzahlwahl, Tagesbalken, 7-Tage-Mittel und dokumentiertem 60-Tage-Normalbereich; Zoom/Pan und Zugriff auf Messung/Tagesbefinden. Einstiege über HRV-Einstellungen und vorhandenes „Zur Auswertung“ nach erfolgreichem Fragebogenspeichern. Keine neue APK angefragt. Details hrv-trends.md; bisherige CSV-Hänger-Korrektur gehört zum Quellstand, noch nicht zu APK 0.8.25 / Build 64.


Aktuelle Korrektur nach Build 64: Endlose CSV-Prüfanzeige als View-Lebenszyklusfehler reproduziert (Ergebnis vor Attachment verworfen / Callback bei Vollbild-Reparenting invalidiert). Vorschau erst nach Attachment starten, unterbrochene Prüfung bei Reattach neu starten; fertige Vorschau behalten. Keine Daten-/Parseränderung und kein automatischer Import. Noch keine neue APK für diese Korrektur angefragt. Details hrv4training-import.md.


## Aktueller Stand nach Build 64

APK **0.8.25 / Build 64 / versionCode 95** aus `f9e8e5ab67f12e86eb755f048909c52fd2055026` über Actions erfolgreich gebaut und direkt veröffentlicht. [Download](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.25-release-64.apk), [Buildnachweis](build-64.md). 2.201.366 Bytes; SHA-256 `3419ada2ea11cf1158cf57357ff674e7141e0b702404edbd11b63da36e8a0dbc`. Dauerhafte Signatur wie Build 63, Version, fehlende Debug-/Test-Flags und Ausrichtung geprüft. Enthält CSV-Import, spätere Slider-Abstandskorrektur und zentrale HRV-Tagesregel. Alle betreffenden Commits jetzt auf main, kein offener APK-Auftrag. 16 gezielte HRV-Tests erfolgreich; kein Gesamtlauf/Hardwaretest.

Tagesregel: nur letzte Messung pro gespeichertem lokalem Tag anzeigen/auswerten; ältere bleiben unverändert archiviert. Auswahl vor Qualitätsprüfung, kein Rückfall auf ältere erfolgreiche Messung. Export vollständig mit dailySelected. Details hrv-daily-selection.md. Score-Diskussion: persönliche Umrechnung aus Export passt, allgemeiner Alterseinfluss unbekannt. Nutzer möchte aktive HRV4Training-Installation nicht für Altersversuche verändern; kein neuer Score implementiert. Frühere Aussagen zu fehlender APK/Tagesregel oder nur lokalen Änderungen unten sind Verlauf.


Aktueller Auftrag: HRV4Training-CSV-Import unter Daten & Speicher → HRV-Pulsdaten umgesetzt. Schreibfreie Vorschau mit Datumsformat/Zeitzone, bewusstes Importieren auf HRV-Worker, externe Kennzahlen und vollständige Originalfelder/Tagesdaten, stabile Dublettenidentität ohne Überschreiben, atomare neue Einträge, eigene Herkunft/Detailansicht und vorhandener Backup-/JSON-Export. Originaldatei privat außerhalb Repo, nur synthetische Fixtures committen. 18 unterschiedliche gezielte Prüfungen erfolgreich, einschließlich temporärer Originaldateiprüfung; tatsächlicher Dateiauswahlpfad und native Vorschau zusätzlich geprüft. Details docs/hrv4training-import.md. Keine neue APK angefragt, Änderungen lokal sichern.

Währenddessen Score-Diskussion: HRV4Training-Recovery-Points zunächst gewünscht, dann vom Nutzer ausdrücklich als unwichtig bezeichnet. Aktuelle Frage ist wissenschaftlicher Log+Alter-Kennwert. lnRMSSD ist verbreitet und bereits in nativen Metriken enthalten; kein universeller Log+Alter-Readinessscore aus Quellen belegt. Empfohlen persönliche Baseline statt erfundener Altersformel. Keine exportkalibrierte Formel, Alterskorrektur, neue Baseline oder Scoreberechnung implementiert; Originalscore nur erhalten. Quellen und Abgrenzung docs/hrv-score-plan.md.

Aktuelle Korrektur nach APK 0.8.24 / Build 63: gemeinsame 32-dp-Gruppenregel schließt nun Übergänge von Dropdown/Auswahl zu Slider und optionale Trainingsdetails ein, ohne Einzelabstände je Frage oder doppelte Margins. Distanz/Dauer nur noch als geghostete Platzhalter in nebeneinanderstehenden Feldern, 8 dp Abstand vom Vortagshinweis. Eingabe/Vorbefüllung/Validierung/Speicherung unverändert. Neue APK wurde für diese Korrektur nicht angefragt; Änderungen lokal sichern, HTML-Vorschau separat aktualisieren.

Aktueller Abschluss: APK **0.8.24 / Build 63 / versionCode 94** veröffentlicht. [Direkter Download](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.24-release-63.apk), [Buildnachweis](build-63.md). Quellstand 0bd848c55b3ea15c1308f9cd847afeb8a27de698 über Actions erfolgreich gebaut, 2.176.486 Bytes. Signatur wie Build 62, Version, Prüfsumme, fehlende Debug-/Test-Flags und ZIP-Ausrichtung geprüft. Finale Sliderwerte 32/0/−8 dp, Textfeld-Labelabstand weiterhin 4 dp. Alle zuvor lokalen Schrift-/UI-Änderungen jetzt auf main und in APK enthalten. HTML-Atlas auf main und Vorschau-Branch synchron. 11 gezielte Tests und Browsermessungen erfolgreich; keine vollständige Suite/Hardwareprüfung. Frühere Angaben zu „nur lokal“, „keine APK“ oder Build 62 unten sind Verlauf.

APK-Auftrag 0.8.24 (versionCode 94): Finale Sliderabstände 32 dp Gruppen / 0 dp Label → Slider / −8 dp Slider → Skala. Negative Margin am Skalencontainer, Text vollständig sichtbar, Touchhöhe unverändert. Text-/Dropdown-Labelabstand bleibt 4 dp. 11 relevante Hosttests erfolgreich; native Ansicht visuell geprüft. APK jetzt ausdrücklich beauftragt, über Actions bauen und direkt als APK veröffentlichen, dauerhafte Signatur prüfen. Bisherige lokale Schrift-/UI-Commits gehören dazu.

Aktuelle Ergänzung: Beschriftungen über gestapelten Controls zentral 4 dp über fieldLabelGap und BicycleComponents.fieldLabel / explizites labelFor. Tagesbefinden und Kartenproviderfeld verwenden die Rolle; normale Controlabstände, horizontale Zahlenzeilen und Header unverändert. HTML-Eingabebeispiele/Tagesbefinden/Rückmeldungen jetzt einheitlich, unabhängiger zusätzlicher Entwurfsregler Slider → Skalenbeschriftung (0–24 dp). Nur Vorschau-HTML auf preview/ui-catalog veröffentlichen, App-Code lokal ohne neue APK.

Vorherige Ergänzung (8 gezielte Tests und Browser-Abstandsmessungen erfolgreich): Tagesbefinden-Slider ohne sichtbare Auswahl-/Unbeantwortet-Zeile und ohne Zurücksetzen pro Slider. Skalenbeschriftungen unter Slidern global über sliderScaleLabel mit 50 % Deckkraft, auch im HTML-Atlas. Speicherung, fehlende unberührte Werte und Accessibility bleiben erhalten. Keine APK beauftragt.

Aktueller Abschluss: League Gothic ersetzt Bebas Neue in der App. Alle Schriftfamilien stehen zentral in BicycleDesign; Screens verwenden nur benannte Rollen. Texteingaben einschließlich Tagesbefinden/Fahrtbenennung verwenden die gemeinsame schmale Schrift mit Groß-/Kleinschreibung, Passwortmasken bleiben erhalten. Nur Hauptheader erzwingen Großschreibung zur Anzeige; gespeicherte Namen bleiben unverändert. Titellauftext misst den transformierten Anzeigetext. Quellprüfung verhindert neue lokale Schriftfamilien; Lizenzdialog aktualisiert. 56 unterschiedliche gezielte Android-Hosttests bestanden, kein Gesamtlauf, Hardwaretest oder APK. Letzte veröffentlichte APK bleibt Build 62 und enthält diesen Wechsel noch nicht.

HTML-UI-Atlas unter docs/previews/ui-catalog.html ist alleinstehend/offline: 20 Bauteilgruppen, 14 tatsächliche Themes, Schrift-/Breitenvergleich, Abstandsmarkierungen und Quelleninspektor. Browserprüfung für 320/360/412 sowie Bedienung erfolgreich, keine externen Requests. Mobilansicht visuell geprüft. Anleitung docs/ui-catalog.md, Schriftregeln docs/fonts.md. **Die allgemeine native Formular-Abstandsbereinigung bleibt ausstehend. Die danach ausdrücklich angefragte konkrete Slidergruppierung ist inzwischen in Tagesbefinden und Atlas umgesetzt: 24 dp nach dem Block, 4 dp zur Skala.** Vorlage über scripts/build-ui-catalog.py erzeugen; Aktualitätsprüfung und Typografieprüfung im begrenzten Testskript. Aktueller Quellstand ohne neue APK veröffentlichen [skip ci].

Vorheriger Abschluss: Dashboard Strecken links / Start-Stop mittig / Menü rechts, permanenter HRV-Button entfernt. Manueller HRV-Einstieg als zentrierter Herz-Pillenbutton oben in HRV-Einstellungen, ohne Start. Optionaler erster Tagesstart mit TERTIARY-Kreis, ACCENT-Herz und 14-px-Dashboard-Blur; Herz startet normales Ruhe-/Messprotokoll erst mit frischen RR und nur in sichtbarer Vordergrundansicht. Außen-Tap/Zurück schließt; standardmäßig aus. Vibration/Signalton getrennt unter Zeitslidern, ebenfalls aus und in Profil-Einstellungssicherung enthalten. Tagesbefinden mit Zeiten nebeneinander, ohne Sportart; mittige Slider-Hervorhebung, Muskelkater/Müdigkeit ab 0, kein sichtbarer RPE-Zusatz, „Zur Auswertung“ wartet auf erfolgreiches Speichern und führt vorerst zurück. Details [hrv-entry.md](hrv-entry.md). 22 relevante Hosttests gezielt geprüft; kein vollständiger Lauf, neuer Hardwaretest oder APK-Build. HRV-Algorithmus/Rohdaten/Qualität unverändert. Letzte APK bleibt Build 62.

Vorheriger Abschluss: Vier technische Prioritätsoptimierungen umgesetzt: kleine gestreamte HRV-Historie mit nachgeladenen Details, Fahrten-/Profil-Snapshots mit direktem ID-Lookup, Dreeve-/Strava-Statusindex mit weiterhin aktuellen Konto-/Typ-/Dateiprüfungen sowie eigener manueller Testworkflow ohne APK/Signierschlüssel. Lokal 52 Android-Hosttests + 19 HRV-Kerntests bestanden, einschließlich der drei gezielt reparierten historischen UI-Prüfungen. Auch alle 71 Tests auf frischem GitHub-Runner bestanden. Erfolgreicher manueller reiner CI-Testlauf: https://github.com/Titanyus/B-Cycle/actions/runs/39720300130 (Codestand a91dcd5). Details [priority-optimizations.md](priority-optimizations.md). Tägliche Readiness als Ziel; keine zusätzliche Tagesaggregation und keine Änderungen an gut funktionierender HRV-Messung, Korrektur oder Bewertung. Keine neue APK beauftragt.

Vorheriger Abschluss: Alle fünf Schritte des vereinbarten Architekturplans umgesetzt und separat geprüft. Letzte APK **0.8.23 / Build 62 / versionCode 93**, [direkter Download](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.23-release-62.apk), [Buildnachweis](build-62.md), einschließlich Tagesbefinden, flexibler Fragenauswahl, Fahrerprofil-Geschlecht und weicher Dashboard-Kurven. Keine offenen Implementierungsschritte aus diesem Plan. Frühere Aufträge und Zwischenstände unten sind Verlauf, nicht der aktuelle offene Auftrag.

Diese Datei ersetzt den fehlenden Chatverlauf als Arbeitsübergabe. Sie enthält keine persönlichen Fahrten, RR-Daten, Zugangsdaten oder Signierschlüssel. Zuerst `AGENTS.md` lesen, dann Git-Status und diese Datei prüfen. Uncommittete Änderungen können aus der bisherigen Umgebung stammen: erhalten und vor Weiterarbeit prüfen. Git überträgt nur committete und gepushte Dateien; ein anderer Chat kann lokale Änderungen der alten Umgebung nicht automatisch sehen.

## Vorheriger abgeschlossener Auftrag

Tagesbefinden und flexible Fragenauswahl umgesetzt: Button nach gespeicherter HRV-Messung und im historischen Ergebnis, eigener deutscher Screen in B-Cycle-UI, neun diskrete Stufen bei Qualitätsfragen (unberührte Mitte bedeutet fehlend), RPE 0–10. Globale Auswahl im HRV-Einstellungs-Accordion, drei benennbare numerische Angaben, dynamische Gruppen ohne leere Platzhalter. TSS ausdrücklich weggelassen. Vortagsfahrten in gespeicherter Messungszeitzone über alle Profile summiert, nur gefahrene Tracks; km und Fahrtzeit nebeneinander und manuell änderbar. Fahrerprofil um optional Mann/Frau ergänzt; Zyklusfrage nur bei Frau aktivierbar. Profil-/Konfigurationssicherung und Antwortdateien in vollständiger Sicherung/Analyseexport erhalten, result.json bleibt bytegleich. Details docs/hrv-questionnaire.md.

Dashboard-Graphen verwenden formtreue kubische Hermite-Kurven ohne neue Extrema zwischen Punkten. Kein Daten-/Aufzeichnungs-/Skalenwechsel; Lücken, gleiche Distanz, Linie und Füllung sowie Voll/30/60/120 s berücksichtigt. Keine neue Einstellung.

15 unterschiedliche gezielte Tests erfolgreich: sechs Tagesbefinden-Tests, bestehende HRV-Speicherung/alt-neu-Backup, vollständige Verzeichnissicherung mit Antworten, Fahrerprofil-Rundlauf, HRV-Einstellungsbackup, drei Kurvenprüfungen (Formtreue, gleiche Distanz/Lücken, alle Zeitfenster) sowie bestehende Halo/Lücken- und Zeichenbereichsprüfungen. Nur betroffene UI-Prüfungen nach letzten Abstandsänderungen erneut ausgeführt. Formular einschließlich echtem Dialog und Dashboard-Kurve nativ gerendert und visuell geprüft. Kein vollständiger Gesamtlauf oder neuer Geräte-/BLE-Test. Neue APK 0.8.23 / Build 62 / versionCode 93 aus `0a5dc9abb418320dbc82044c9eed59558a7d1e9f` über GitHub Actions gebaut und direkt veröffentlicht. 2.154.363 Bytes; SHA-256 `71afb280b703842d0608e3af083bfda1884e790eea045dece34677394d917acd`. Dauerhafte Signatur wie Build 61, Version, fehlende Debug-/Test-Flags und Ausrichtung geprüft. README aktualisiert, Veröffentlichung mit [skip ci].

Anschließende Optimierungsanalyse in docs/optimization-review-2026-10-21.md ist die Grundlage der nun umgesetzten vier technischen Prioritäten. Die vorgeschlagene zusätzliche Tagesregel wurde vom Nutzer ausdrücklich ausgenommen; eine tägliche Readiness-Messung genügt als Produktziel. Kein Geräteprofiling oder vollständiger Gesamttestlauf. Nutzer bestätigt inzwischen die gut funktionierende HRV-Messung.

## Vorheriger HRV-Abschluss

Nach Build 61 HRV-Optik und kurze Graphlücken korrigiert, keine neue APK beauftragt: Glow-Start 45 % statt 30 %, Ende weiter 10 % nach 1,5 s; bpm vertikal zur sichtbaren Zahl zentriert; Start/Erneut messen 900 Black mit transparentem ACCENT-Pillenrahmen 1 dp und Innenabstand. Abbruch/Speicherwiederholung weiterhin schlicht. Grafische Lückenerkennung zieht beobachtetes RR vom Punktabstand ab, bevor zusätzliche fehlende Zeit bewertet wird. Längere Lücken bleiben sichtbar. Keine Änderung an Rohdaten, HRV-Berechnung, Artefaktkorrektur, Datenformat oder Messqualität.

Vier gezielte Tests erfolgreich (Glow; native HRV-Ansicht mit Ink-Zentrierung, Start-/Abbruch-Styling, Rahmen und Themewechsel; Controller mit kurzer Empfangsverzögerung und längerem Ausfall; Graph mit verbundenen Punkten und expliziter Lücke). Nach letztem Button-Innenabstand nur den einen nativen UI-Test erneut ausgeführt, erfolgreich. Ansichten visuell geprüft; kein echter Gurt-/Gerätetest. Keine APK, Version bleibt 0.8.22/92; Veröffentlichung mit [skip ci].

Währenddessen fragt Nutzer nach einem leicht verständlichen HRV-/Erholungswert und wünscht alle Detailwerte als Langzeitgraphen. HRV4Training/Kubios gefallen; Elite HRV bewertet trotz ähnlicher Messwerte teils anders. Recherche und Vorschlag in docs/hrv-score-plan.md festgehalten. Kein neuer Score, keine Baseline-Logik oder Verlaufs-UI implementiert; konkrete Formel/Grenzen noch nicht vereinbart. Die Beratungsfrage ersetzt nicht den abgeschlossenen UI-Auftrag.

## Vorheriger Abschluss bis Build 61

Nach Build 60: Namensvorschau im Eingabefeld anhand der letzten gefahrenen Fahrt, Querstrich vor Trackaufzeichnung in Karten, automatisches Scrollen beim Aufklappen der Trainingshilfe sowie HRV-Optik (Achsbeschriftungen 50 %, Diagramm +30 %, Kreislinie +35 %, Endpunkt +40 %). Nur gezielte Tests. Anschließend neue APK beauftragt: Version 0.8.22 / Build 61 / versionCode 92 über GitHub Actions erfolgreich gebaut und direkt veröffentlicht. Vorschau ausschließlich gezeichnet, kein Bestandteil des gespeicherten Grundnamens oder der tatsächlichen Fahrtbenennung. Messung, Berechnungen und Datenformate unverändert.

Umgesetzt und geprüft: fünf gezielte Robolectric-Tests erfolgreich (VisualFollowupTest: Vorschau/Editable/Speicherung einschließlich leerem Grundnamen und 60-Zeichen-Grenze, Trainingshilfe-Scrollen ohne Auswahländerung, Kartentrenner; vorhandener profilbezogener RideNaming-/Backup-Test; native HRV-Render-/Theme-Prüfung mit 286-dp-Graph und tatsächlicher 50-%-Textalpha). Native Vorschau und HRV-Ansicht visuell geprüft. Kein Geräte-/BLE-Test für diese optischen Änderungen; Alle Ergänzungen in APK 0.8.22 / Build 61 enthalten. 2,112,787 Bytes; SHA-256 `81411756c0b51c79415de70274c637ffa336f51cc0e9aa75ee0dbeff2b542e75`. Dauerhafte Signatur, Version, fehlende Debug-/Test-Flags und zipalign geprüft.

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

Historischer Befund aus Build 56 (inzwischen gezielt repariert und grün, siehe priority-optimizations.md): Drei ältere UI-Tests schlugen im breiteren Lauf fehl und wurden am unveränderten Ausgangscommit `734d5e3` identisch reproduziert: `SettingsDesignMigrationTest.fieldDropdownAndNewRowsUseActiveThemeWhileSourceSelectionRemainsFixed`, `SettingsDesignMigrationTest.mainScreensAndAdvancedDialogsRecolorWithoutChangingGeometryOrNavigation`, `SettingsSectionsTest.newSectionsUseOneScrollHostAndNumericRowsStayInlineAcrossThemes`. Veraltete Annahmen zu Dropdownhintergrund, Schriften und Bosch-Abschnitt; in diesem Auftrag nicht korrigiert. Diese Fehler nicht als neue Regression oder als bestandene Tests ausgeben.

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

## APK nach den UI-Ergänzungen

0.8.22 / Build 61 / versionCode 92 aus `f0dfbec5f3955c93f1c2f42bbab3c83b0742773b`. Alle Schritte erfolgreich im [Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/39615540854), direkte APK und README aktualisiert. Dauerhafte Signatur wie Build 60; 2,112,787 Bytes; SHA-256 `81411756c0b51c79415de70274c637ffa336f51cc0e9aa75ee0dbeff2b542e75`. Veröffentlichung mit [skip ci], kein zusätzlicher Build.

Bei Veröffentlichung meldet die GitHub-API den Gesamtstatus trotz aller 16 erfolgreich abgeschlossenen Schritte noch als in_progress (letzter Schritt 14:20:43 UTC). Kein Build-/Prüffehler. APK unabhängig geprüft; kein erneuter Build nötig.

## APK 0.8.23 / Build 62

[Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/39652351462), Quellstand `0a5dc9abb418320dbc82044c9eed59558a7d1e9f`. 2.154.363 Bytes; SHA-256 `71afb280b703842d0608e3af083bfda1884e790eea045dece34677394d917acd`; permanenter Signierschlüssel wie Build 61. Direkte APK, Prüfsumme, Release-/Buildnachweis und README veröffentlicht. Optimierungsanalyse nur Empfehlungen; kein zusätzlicher Score/keine Langzeitdiagramme.

Slider-Skalenbegriffe: Abstand nach oben zentral sliderScaleLabelGap = controlGap / 2 (4 statt 8 dp). Keine nachträgliche normale Control-Margin auf diese Beschriftungsrolle anwenden; Touchhöhe des Sliders bleibt unverändert.

Slider-Fragen bilden eine Gruppe: Frage, 48-dp-Slider, Skalenbegriffe mit 4 dp Abstand/50 % Deckkraft. Danach 24 dp Gruppenabstand (sliderQuestionGap). Diese konkrete Gruppierung ist jetzt in Tagesbefinden und Atlas umgesetzt; der allgemeine Formular-Abstandsentwurf bleibt separat.

Feldbezogene Inline-Rückmeldungen direkt unter Eingabefeldern: fieldFeedbackGap=controlGap/2 (4 statt 8 dp), für Erfolg und Fehler gleichermaßen. BicycleFeedback.status setzt die Margin nach finaler Anfügung unter EditText; andere Statusrollen/Toasts/Validierungsregeln unverändert. HTML-Atlas zeigt dieselbe Regel.

Veröffentlichung dieser Ergänzungen: App-Änderungen lokal committet, kein Push nach main ohne ausdrückliche Freigabe. Nur die alleinstehende HTML-Datei auf preview/ui-catalog aktualisieren. Vorschau-Download https://raw.githubusercontent.com/Titanyus/B-Cycle/preview/ui-catalog/docs/previews/ui-catalog.html . Keine APK.

Prüfstand Beschriftungsabstand/Skalenregler: 11 gezielte Android-Tests erfolgreich (6 Tagesbefinden, 5 Formular-/Seitenauswahl-Abstände). Browser prüft tatsächliche 4 dp in allen drei gezeigten Feldbeispielen und unabhängige Verstellung von Label- und Skalenabstand, zusätzlich bestehende Atlas-Bedienung/320–412-Breiten. Kein Gesamtlauf oder APK.

Prüfung dieser Korrektur: 7 gezielte bestehende/ergänzte Tagesbefinden-Tests erfolgreich, einschließlich optionaler Trainingphase, ein-/ausgeblendeter RPE-/Mengenfelder, exakt 32 dp ohne Doppelabstand, Platzhalter, manueller Eingabe und vorhandener Vorbefüllung/Backup. Browserprüfung des aktualisierten Atlas erfolgreich (Platzhalter, 8-dp-Hinweisabstand und bisherige Regler/320–412-Breiten). Native Ansicht visuell geprüft. Kein Gesamtlauf und keine neue APK.

Prüfstand HRV-Auswertung: 27 unterschiedliche gezielte Tests erfolgreich; native Testdaten-Vorschau docs/previews/hrv-trends.png visuell geprüft. Alle sechs Kennzahlen, Statistik-/Tages-/Protokollregeln, Touch-Pinch/Pan/Tagesauswahl, beide echten Einstiege, Speichern vor Weiterleitung, Zurück zu Einstellungen, unveränderte Sicherung und CSV-Import geprüft. Typografie und UI-Atlas konsistent. Quellstand lokal gesichert, kein Push/APK für diesen Auftrag. APK 0.8.25 / Build 64 enthält weder diese Auswertung noch die danach korrigierte CSV-Prüfanzeige.

Prüfstand elf Stufen/HRV4-Antworten/Korridor: 21 gezielte Tests erfolgreich. Schema 1 → 2 einmalig mit legacyRatings, .5-Rundung, Quellenbereich/fehlende Daten, Schutz manueller Antworten, unveränderte Originaldateien und Backup-Rundlauf geprüft. Korridor nativ visuell geprüft. HTML-Atlas auf elf Stufen aktualisiert, nur lokal; keine APK und kein Push.

Prüfstand feste Achsen/Farben/Größe: acht gezielte HRV-Auswertungstests erfolgreich; native Vorschau mit 504 dp Höhe und linkem Platzgewinn visuell geprüft. Neue Folgefrage: Linie und Korridor sollen bis heute reichen; derzeit kein absichtlich zentrierter Mittelwert (schon trailing 7 Tage), mögliche Ursache sind Quellen-/Protokollgruppen oder Mindestdaten. Noch keine nachträgliche Kurvenverlängerung implementiert, Diagnose läuft. Keine APK angefragt.

Quellenklärung zur scheinbar drei Tage verzögerten Kurve: Nutzer hat zuletzt Messungen aus beiden Apps und eine fehlende vorgestrige Messung. Einzelne Lücke im trailing 7-Tage-Mittel reproduziert keinen Versatz. Harte Quellentrennung konnte beim Wechsel der pro Tag letzten Quelle eine zu kurze neue Historie erzeugen. Zeitkennzahlen Puls/RMSSD/SDNN/pNN50 jetzt mit gemeinsamer Referenzhistorie (Einheit bekannt), LF/HF weiterhin protokollgetrennt/nativ. Keine Doppelzählung/Extrapolation, Datum und Herkunft erhalten. Noch keine APK.

Abschließende Klarstellung: Nutzer möchte einen gemeinsamen Bestand ohne Trennung beider Messreihen. Referenzhistorie jetzt für alle verfügbaren kompatiblen Kennzahlen auch über Protokollgrenzen gemeinsam; Quellen/Protokolle nur Metadaten. Tagesregel/Qualitätsfilter bleiben. CSV-LF/HF ohne Einheit unverändert nur Originalfeld, keine erfundene Konversion.

Abschluss gemeinsamer Bestand: zehn gezielte Tests erfolgreich. Mittelwert/Korridor nutzen vorhandene persönliche Historie beider Apps und sind bei ausreichenden Daten auch nach Quellenwechsel bis zur neuesten Messung berechenbar. Lücke vorgestern allein erzeugt keinen Dreitagesversatz. Grund des konkret fehlenden Messwerts bleibt ohne aktuelles App-Journal unbekannt; keine Daten wiederhergestellt/gelöscht. Keine neue APK, alle Folgeänderungen nach Build 66 lokal.

Während APK-Auftrag Build 67 (0.8.28 / Code 98) erfolgreich, aber noch nicht direkt veröffentlicht: Nutzer ergänzt Schlafzeiten als einen Einstellungs-Auswahlpunkt, neue optionale Schlafdauer (Dezimalstunden, 0–24), globale Halbierung Auswahlkreis/Text auf 4 dp. Änderungen werden in aktualisierte APK 0.8.29 / Code 99 aufgenommen; Zwischenbuild 67 nicht als neuester Download veröffentlichen. Details hrv-questionnaire.md und ui-conventions-audit.md.

Folgekorrektur nach Nutzerhinweis: horizontaler rechter Rand blendete Balken/Linien teils erst bei Eintritt des Tagesmittelpunkts ein. Tatsächliche Balkenausdehnung und randüberschreitende Mittelwertsegmente jetzt geclippt zeichnen; kleine Beschriftungen bewegen sich mit statt am Rand festzukleben. Keine APK.

Prüfstand Balkenwerte/Rand: drei gezielte Tests erfolgreich (native Ansicht/Tagesauswahl, echte Pinch-/Wischgesten, Pixelregression für Teilbalken vor Eintritt seines Mittelpunkts am rechten Rand). Native Beschriftung visuell geprüft. Typografie und generierter Atlas konsistent. Keine APK erstellt.
