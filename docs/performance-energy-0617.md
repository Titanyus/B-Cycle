# Performance & Energy Engine – B-Cycle 0.6.17

## Ergebnis und Bedienung

Die vorhandene Seite **FIT-Aufzeichnung** enthält fünf zusätzliche, einzeln auswählbare Fahrtkennzahlen. Für vorhandene Installationen und bei Standardkonfiguration sind sie ausgeschaltet. Die bestehenden Auswahlmöglichkeiten und der SharedPreferences-Schlüssel `recordFields` bleiben erhalten. START friert die Auswahl und Quellen für diese Fahrt ein; Änderungen gelten erst für die nächste Fahrt.

Berechnung und Fahrtjournal laufen im bestehenden `RideRuntime`/`RecordingService` unabhängig von Dashboard und Exportauswahl. `runtime.performanceValues` stellt Zwischenstände bereit. Noch keine Dashboard-Feldauswahl oder Layoutänderung für diese neuen Kennzahlen. Eine deaktivierte NP-Checkbox verhindert beispielsweise nicht die interne NP-Berechnung für EF.

## Komponenten und geänderte Dateien

Neu: `PerformanceEngine.kt` enthält zeitgewichtete Leistungsauswertung, Messquellen-/Zeit-/Qualitätsmodelle und eine erweiterbare `EnergyEngine`. Keine neue Bibliothek oder Sensorverbindung.

Gezielte Integrationen:

- `RecordingData.kt`: gemeinsame Feldauswahl mit `RECORD`/`SUMMARY`, fünf neue IDs, GPS-Abhängigkeit und Ereignisliste pro vorhandener Probe.
- `RideRuntime.kt`: echte neue Sensorbeobachtungen und Empfangszeiten übernehmen, zentrale Engine pro START zurücksetzen, Ereignisse mit dem vorhandenen Sample an den bestehenden Speicherworker übergeben.
- `GpsRide.kt`: Zeit der letzten angenommenen GPS-Beobachtung bereitstellen; bestehende Filter/Berechnungen bleiben erhalten.
- `RideJournal.kt`: versioniertes V5-Journal mit Ereignissen; V4 bleibt lesbar.
- `FitWriter.kt`: dieselbe Engine aus dem Journal reproduzierbar auswerten; optionale Session-/Lap-Felder ergänzen.
- `MainActivity.kt`, `RecordingService.kt`: GPS-Berechtigung/Aktivierung berücksichtigt die gewählte Geschwindigkeitsquelle auch ohne aktivierte GPS-Record-Checkbox. Die vorhandene Checkbox-Seite wird über ihre gemeinsame Feldliste erweitert.
- `app/build.gradle.kts`: Versionsnummer 0.6.17 / Code 33.

Wiederverwendet: Bosch-MCSP-Decoder und Verbindung, HeartRateConnection, GpsRide/GpsSensor, START/STOP und Hintergrunddienst, RecordingStore und dessen Worker/Recovery, vorhandene FIT-Record-/Lap-/Session-Erzeugung, bestehende SharedPreferences, Checkboxen und Bicycle-Dialogdesign. Bereits erzeugte FIT-Dateien werden nicht nachträglich verändert.

Zusätzlich enthalten: die zuvor gewünschte Streckenmanager-Korrektur – zweite Datenzeile nur Strecke und Bewegungsdauer, ohne Durchschnittsgeschwindigkeit. Bei unbekannter Bewegungsdauer wird keine Gesamtzeit als Ersatz verwendet.

## Bosch-Messbasis

Fahrerleistung stammt aus der MCSP-Adresse `0x185b`, unsigned Leistungswert in W. Motorleistung `0x185d` ist eine getrennte Größe und geht in keine der fünf Kennzahlen ein. Die vorhandene Dekodierung bleibt unverändert.

In den implementierten skalaren Bosch-Nachrichten ist kein nutzbarer Sensor-Messzeitstempel enthalten. Deshalb protokolliert B-Cycle den tatsächlichen Empfang mit `SystemClock.elapsedRealtime()`. Ein neues `onSamples`-Ereignis zählt als Beobachtung, auch bei identischem Leistungswert. Zusammengeführte, zwischengespeicherte Werte in `onData` sind keine erneuten Beobachtungen.

Aus dem bereitgestellten Diagnoseprotokoll B-Cycle 0.2.0: 25 Leistungsbeobachtungen in 91,622 s. Für die 23 vollständig vorliegenden Abstände der letzten 24 Beobachtungen: Minimum 509 ms, Median 2.041 ms, Maximum 12.777 ms. Das ist eine Beobachtung dieses Tests, keine feste Bosch-Abtastrate. Interne Motorabtastung, Glättung und absolute Leistungsgenauigkeit können daraus nicht abgeleitet werden. Keine automatische Korrektur der Bosch-Wattwerte.

## Zeit- und Qualitätsregeln

Alle Berechnungen nutzen eine monotone, auf START bezogene Zeitbasis. Zusätzlich bleiben Empfangszeit und, falls tatsächlich vorhanden, Sensorzeit erhalten. GPS übernimmt den Zeitpunkt der angenommenen GNSS-Beobachtung. Eine bei START noch verwendbare Beobachtung erhält kein künstlich erneuertes Alter.

Zwischen zwei Beobachtungen wird der letzte gültige Wert stückweise konstant verwendet. **Maximal fünf Sekunden** seit Empfang bzw. älterer Sensorzeit, danach unbekannt. Diese Haltezeit ist eine explizite Modellannahme, keine Behauptung, dass währenddessen wirklich erneut gemessen wurde. Ein erkannter Disconnect invalidiert sofort. Keine lineare Interpolation oder unbegrenzte Fortschreibung.

- 0 W ist ein gültiger Messwert, einschließlich echter Tretpausen.
- Fehlend, NaN, Infinity, negative Leistung, zukünftige Zeitstempel und ungültige Werte liefern keinen Null-Ersatz.
- Leistung gültig 0…65.535 W (Protokollbereich), Puls 1…255 bpm, Geschwindigkeit 0…35 m/s.
- Exportierbare Arbeit/Ratio: mindestens 95 % zeitliche Leistungsabdeckung. Unbekannte Abschnitte werden nicht ergänzt; eine einzelne längere Lücke sperrt eine ansonsten ausreichend belegte Fahrt nicht pauschal.
- Arbeit ist auch dann ausschließlich das Integral der bekannten Abschnitte. Bis zu 5 % fehlende Zeit wird **nicht extrapoliert**. Der Wert kann daher den vollständigen Fahrtwert unterschätzen. Qualitätsfelder machen dies nachvollziehbar.
- NP: mindestens90% zeitliche Leistungsabdeckung, mindestens ein vollständiges30-s-Fenster und mindestens90% nutzbare vollständige Fenster im aktiven Auswertungszeitraum. Eine Lücke verwirft alle über sie laufenden Fenster.
- EF erfordert vollständige Pulsabdeckung aller tatsächlich ausgewerteten Leistungsabschnitte. Fehlender Puls macht EF unverfügbar, nicht aber gültige Leistungskennzahlen.
- Sehr kurze Fahrten können Arbeit/Kalorien liefern, aber vor 30 vollständigen Sekunden keine NP/EF.

Abdeckung ist **keine Genauigkeit**. Es gibt keine erfundenen Sensor-Genauigkeitsprozente. `knownMechanicalWorkKj` stellt intern auch bei zu schlechter Abdeckung nur die bekannte Teil-Arbeit bereit; diese wird dann nicht als gültige Gesamt-Arbeit exportiert.

Die App hat weiterhin START/STOP, keine neue Pausenbedienung. Stillstand oder 0 W sind keine Aufzeichnungspause. Das Modell unterstützt explizite PAUSE/RESUME: pausierte Zeit zählt nicht, neue 30-s-Fenster beginnen nach RESUME; kein Fenster verbindet beide Abschnitte. Journallücken ohne entsprechende Ereignisse bleiben unbekannte aktive Zeit.

## Mathematische Verfahren

**NP:** Leistungsintegral je vollständig abgedeckter Sekunde / 1 s ergibt mittlere W. Daraus vollständige gleitende 30-s-Mittel, vierte Potenz, Mittel dieser Potenzen, vierte Wurzel. Die ersten 29 s und ein unvollständiger Schlussblock erzeugen keine künstlichen Fenster. Alle vollständigen Fenster werden gleich gewichtet; echte Nullleistung bleibt enthalten. Mathematische Übereinstimmung mit diesen dokumentierten Randbedingungen, keine Zusicherung bitgleicher Ergebnisse zu allen Fremdanwendungen mit anderen Start-/Lückenregeln.

**Mechanical Work:** `Σ P(W) × Δt(s) / 1000 = kJ`. Subsekündliche Wechsel werden mit ihren echten Zeitintervallen integriert. Beispiel 100 W für 250 ms, 300 W für 1.650 ms, 0 W für 100 ms: 0,520 kJ.

**Calories:** ausschließlich leistungsbasiert in dieser Version: `Arbeit(kJ) / (4,184 × 0,24) = kcal`. 0,24 ist ein angenommener Brutto-Wirkungsgrad, keine persönliche Kalibrierung. Modell-ID `power-gross-efficiency-0.24-v1`. Ergebnis geschätzt, keine zusätzliche Ruheenergie oder doppelt addierte Herzfrequenzschätzung. 720 kJ mechanische Arbeit entsprechen ca. 717 kcal. Bei Wirkungsgrad 0,18 statt 0,24 wären es ca. 956 kcal; bei 0,25 ca. 688 kcal. Diese Spannweite illustriert die Modellabhängigkeit, kein individuelles Konfidenzintervall. Auch Erholung bei echter Nullleistung bildet dieses reine Arbeitsmodell nicht gesondert ab.

`EnergyModel`/`EnergyInput` erlauben später validierte Alternativen; `EnergyProfile` bereitet Gewicht, Alter, Größe, Ruhe-/Maximalpuls, FTP und Kalibrierung als nullable Werte vor. Keine neuen persönlichen Daten werden erhoben, gespeichert oder erfunden. Kein aktiver HR-/Bewegungsfallback: die notwendigen persönlichen Angaben und ein freigegebenes Modell fehlen. Genau ein geeignetes Modell liefert die Schätzung, keine Addition verschiedener Modelle.

**Speed-to-Power Ratio:** zeitgewichtete mittlere Geschwindigkeit in km/h / zeitgewichtete mittlere Fahrerleistung in W über exakt dieselben gültigen gemeinsamen Abschnitte. Geschwindigkeit aus der bei START gewählten FIT-Hauptquelle (GPS oder Rad), nicht aus formatierten Dashboardwerten. Die Distanz-Auswahl für FIT bleibt unabhängig erhalten. Gemeinsame Abdeckung mindestens 95 %, keine Division durch 0. 36 km/h und 200 W → 0,18 km/h/W. Dieser Vergleichsindikator ist bei E-Bikes von Unterstützung, Wind und Gelände abhängig; kein physikalischer Wirkungsgrad.

**EF:** zentrale NP / zeitgewichteter Puls über dieselben gültigen Leistungsintervalle. 200 W und 150 bpm → 1,333… W/bpm. Keine Verwendung des bestehenden nach Nachrichtenanzahl gebildeten Live-Pulsmittels. Trainingsvergleich unter ähnlichen Bedingungen, keine direkte Fitnessmessung.

Bestehende Puls-/Kadenzmittel bleiben im Rahmen dieser Aufgabe funktional unverändert. Eine spätere Umstellung dieser allgemeinen Mittel auf Zeitgewichtung ist separat sinnvoll; sie beeinflusst die fünf neuen zeitgewichteten Kennzahlen nicht.

## FIT-Zuordnung (offizielles Garmin-Profil)

Verwendetes SDK/Profil **21.218.0**, Header-Profilwert **21218**, FIT-Protokoll **2.0**. Feldnummern, Datentypen, Einheiten, Skalierung und Offset wurden gegen Garmins tatsächlich eingebundene `Factory`/SessionMesg/LapMesg geprüft.

| Kennzahl | Session (18) | Lap (19) | Typ / Einheit / Skalierung |
|---|---:|---:|---|
| NP | normalized_power 34 | normalized_power 33 | uint16, W (`watts`), 1, Offset 0 |
| Mechanical Work | total_work 48 | total_work 41 | uint32, **J**, 1, Offset 0; interne kJ × 1000 |
| Calories | total_calories 11 | total_calories 11 | uint16, kcal, 1, Offset 0 |
| Speed-to-Power Ratio | Developer Field | Developer Field | float64, km/h/W, 1, Offset 0 |
| EF | Developer Field | Developer Field | float64, W/bpm, 1, Offset 0 |

Es bleibt exakt die bestehende einzelne Lap über die gesamte Fahrt. Daher sind die neuen Lap-/Session-Ergebnisse identisch. Keine zusätzliche Lap und keine künstlichen neuen Kennzahl-Records.

Developer Data ID: vorhandene App-ID `a4d3c076-05cc-4b59-b1ab-c4963f348248`. FieldDescription meldet korrekte Einheiten, Basisdatentyp und `native_mesg_num` 18/19; separate Definitionindizes für Lap und Session. Zeitbezogene bestehende Developer Fields bleiben Record 20 zugeordnet.

Wenn mindestens eine gültige neue Kennzahl ausgewählt/exportiert wird, ergänzen kleine Summary-Developer-Felder: `POWER_DATA_COVERAGE` (%), `POWER_MISSING_TIME` (s), `POWER_MAX_GAP` (s), `PERFORMANCE_MODEL_VERSION`. Bei Calories zusätzlich `ENERGY_GROSS_EFFICIENCY` (0,24). Keine Qualitätswerte im Record-Datenstrom. Anwendungen ohne Developer-Field-Anzeige können die üblichen Standarddaten weiter importieren.

Deaktivierte oder ungültige optionale Kennzahlen fehlen vollständig; keine willkürlichen Ersatzzahlen, NaN oder Infinity. Native Felder werden nur im gültigen Wertebereich geschrieben. NP/Calories gemäß Profil in ganzen Einheiten, Work in ganzen J; Developer-Ergebnisse float64.

## Journal, Speicher und spätere Sensorvergleiche

V5 ergänzt Ereignisse an den vorhandenen Sample, einschließlich Kanal, relativer Empfangs-/Sensorzeit, Quelle und Zustand. Bei unabhängig deaktivierten Rohdaten-Checkboxen bleiben für die Kennzahlen benötigte Eingangsdaten im privaten Fahrtjournal reproduzierbar verfügbar; sie werden nicht dadurch automatisch als zusätzliche FIT-Records exportiert.

V4-Journale bleiben exportierbar. Da dort keine tatsächlichen Beobachtungszeiten vorliegen, werden nachträglich keine neuen Kennzahlen aus vermeintlich frischen 1-Hz-Snapshots erfunden. Nach Abbruch wird nur eine unvollständige Schlussprobe verworfen, zuvor vollständige Beobachtungen bleiben nutzbar. Vorhandene Recovery-/Exportarchitektur bleibt bestehen.

Speicherbegrenzung: 30 NP-Werte, aktuelle Kanal-/Quellenwerte und maximal 256 noch nicht journalisierte Ereignisse. Bei übergroßer Ereignischarge werden Datenverlust und ungültige Eingänge explizit markiert statt verdeckt extrapoliert. Kein im RAM wachsendes Leistungsarchiv. Lange Abschnitte ohne Leistung werden ohne Schleife für jede einzelne fehlende Sekunde verarbeitet. Export auf dem bestehenden Speicherworker; keine neue BLE-/GPS-Verbindung, kein neuer Hintergrundtimer.

`PowerSource` unterscheidet BOSCH_RIDER, CYCLING_POWER_BLE, ANT_PLUS und PEDAL; `PowerMeasurement` enthält Quelle, Empfangs-/Sensorzeit, Gültigkeit, Alter und vorherige Lücke. Unabhängige Quellen überschreiben Bosch nicht. Journale können die getrennten Beobachtungen auf einer gemeinsamen monotonen Zeitbasis erhalten. Damit sind später Fenstervergleich, Verzögerung und Abweichungsanalyse möglich. Noch kein zusätzlicher Sensoranschluss, ANT+-Support oder Vergleichsalgorithmus. Keine angenommene Pedal-Korrektur.

## Tests und Prüfung

Neue Tests:

- `PerformanceEngineTest`: unabhängige konstante/Wechsel-/Spitzen-/HIIT-/Nullleistungs-Referenzen, kurze Dauer, Puls-/Geschwindigkeitsabdeckung, Division durch 0, unterschiedliche Raten, echte Subsekundenintervalle, veraltete Werte, Disconnect/Reconnect, Pausen, ungültige Zeit/Werte und unabhängige Quellen.
- `EnergyEngineTest`: Umrechnung, optionale persönliche Eingaben, Modellpriorität, kein erfundener HR-Fallback, keine Doppelzählung.
- `PerformanceFitTest`: Profileigenschaften, richtige Session/Lap-Felder, gleiche bestehende Records, unabhängige Exportcheckboxen und interne EF→NP-Abhängigkeit, unbrauchbare Daten, V4-Kompatibilität. Erzeugte Dateien durch offiziellen Garmin FitDecoder **und ActivityFileValidationPlugin** geprüft.
- `PerformanceJournalTest`: tatsächliche subsekündliche Ereignisse und Alter verlustfrei wiederherstellbar, Live-/Replaygleichheit, abgebrochene letzte Probe.
- `PerformanceSettingsTest`: fünf vorhandene unabhängige Checkboxen, bestehende Auswahl unverändert, gemeinsame Speicherung und Wiederherstellung, keine zusätzlichen Dashboardänderungen/abhängigen Exportfelder.
- Erweiterter `RecordingFlowTest` (API 33/35): echte Runtime/Journalspeicherung/Export ohne sichtbare Activity und mit deaktiviertem Power-Record-Feld.

Unabhängige Referenzwerte beruhen auf Python-Standardbibliothek und physikalischer Integration, nicht einer Kopie der Produktionsklasse. Beispiel-FIT: `app/build/reports/fit/performance-garmin-validated.fit`.

Abschließender Lauf: **198 Tests, 0 Fehler, 0 fehlgeschlagene, 0 übersprungene** (45 Klassen); `:app:testDebugUnitTest :app:assembleRelease` erfolgreich in **1 min 16 s** inklusive Release-Minifizierung/Ressourcenoptimierung. Offizieller Garmin-Decoder und Activity Validator in den FIT-Tests erfolgreich. Keine bisherigen Tests entfernt oder deaktiviert. Bekannte Compilerwarnung: bestehendes `onBackPressed()` in MainActivity (Deprecated); keine neue Warnung dieser Engine. APK mit dem bisherigen Testzertifikat signiert und mit `apksigner verify` geprüft.

## Referenzen und praktische Grenzen

Garmin FIT SDK / Profile / Developer Fields: https://developer.garmin.com/fit/overview/ und https://developer.garmin.com/fit/protocol/ . Technisch maßgeblich geprüft wurde das lokal verfügbare offizielle SDK 21.218.0, einschließlich Decoder und Activity Validator.

NP-Hintergrund: TrainingPeaks, https://www.trainingpeaks.com/learn/articles/normalized-power-intensity-factor-training-stress/ . Effizienz-Hintergrund: Ettema/Lorås (2009), https://doi.org/10.1007/s00421-009-1008-7 . Die Online-Seiten waren aus dieser Umgebung beim Abruf nicht lesbar (HTTP 403); deshalb keine Behauptung einer vollständigen Literaturprüfung oder individuellen Validierung. Die dokumentierte Berechnung und Referenzfixtures sind unabhängig nachvollziehbar.

Offen für echten Hardwaretest: Bosch-Zeit-/Glättungsverhalten unter Fahrtbedingungen, absolute Genauigkeit gegen unabhängigen Leistungsmesser, individuelle Kalorienmodellabweichung und Anzeige der Developer Fields in Dreeve/anderen Fremdprogrammen. Der Garmin-Validator ersetzt diese Prüfungen nicht. Bei sporadischen Bosch-Lücken kann NP/EF bewusst fehlen. Vollständige Sensorabdeckung beweist keine Sensor-Genauigkeit.

## Bosch-Rollphasen mit ausbleibenden Nullwert-Updates (Modell 2)

Ein ausdrücklich empfangener Fahrerleistungswert 0 W bleibt für die Berechnung
gültig, solange frische Geschwindigkeitswerte vom E-Bike dessen Verbindung bestätigen.
Damit sind auch ein bis zwei Minuten ohne neue Leistungs-/Kadenzwerte während
des Rollens möglich. Die letzten Nichtnullwerte bleiben auf fünf Sekunden begrenzt.
GPS-Geschwindigkeit allein, Verbindungstrennung, ungültige Messungen oder ein
Geschwindigkeitsausfall verlängern den Nullzustand nicht. Nach unterbrochener
Kontinuität ist wieder eine neue Leistungsmessung erforderlich.

Kadenzmittel berücksichtigen weiterhin nur tatsächlich empfangene positive Werte;
Null und fehlende Werte verwässern den Tretgeschwindigkeitsdurchschnitt nicht.
FIT-Record-Rohwerte und Sensortimestamps werden nicht aufgefüllt. NP, Arbeit,
Kalorien, Speed-to-Power und EF werden aus dem gemeinsamen Modell berechnet.
`POWER_ZERO_STATE_TIME` in Lap/Session (Developer Field, Sekunden) kennzeichnet
die Zeit, in der eine explizite Null durch frische Bike-Telemetrie fortgeführt wurde.
Diese Zeit ist in der berechneten Leistungsabdeckung enthalten. Die Grenzen von
Für Arbeit/Ratio/Kalorien bleiben95% Leistungsabdeckung erforderlich; NP verwendet seit29.10.2026 jeweils90% Leistungs- und vollständige30-Sekunden-Fensterabdeckung. Die fünf Sekunden begrenzen die Fortschreibung einzelner Messungen; sie sind kein pauschales Ausschlusskriterium für eine ganze lange Fahrt. EF erfordert Puls über alle ausgewerteten Leistungsabschnitte.

Das neue, am Ende des Enums ergänzte BIKE_ACTIVITY-Ereignis hält Rad-Telemetrie
auch bei ausgewählter GPS-Hauptquelle separat fest. Journal-Version 5 bleibt lesbar;
Modellversion 1 und 2 werden akzeptiert. Alte Modell-1-Journale können bestätigende
SPEED-Ereignisse verwenden, soweit deren Quelle tatsächlich das Rad ist.
Bestehende exportierte FIT-Dateien werden nicht nachträglich verändert.

Tests vergleichen zwei Minuten schweigende Nullzustände mit laufend empfangenen
Nullwerten, prüfen GPS-only, Geschwindigkeitsausfall und Nichtnull-Veraltung sowie
FIT-Metadaten, fehlende Rohwerte und den offiziellen Garmin Activity Validator.

## FIT-Neuberechnung und lange Rollphasen (Modell 3)

Die Detailansicht berechnet Kennzahlen bei fehlendem Session-Export aus den Rohdaten.
FIT-Checkboxen steuern die Speicherung der abgeleiteten Werte, nicht ihre Berechenbarkeit in der Detailansicht.

Beim Lesen eigener FIT-Dateien bestätigt ausschließlich das nach BICYCLE-App-UUID,
Feldname und Einheit geprüfte Bike-`SPEED`-Developer-Feld die Bike-Telemetrie. Es wird
als BIKE_ACTIVITY an die vorhandene Performance Engine übergeben. Ein fehlender
Power-Record hebt einen vorher ausdrücklich gemeldeten Nullzustand nicht sofort auf.
Die Engine hält ihn nur während frischer Bike-Telemetrie; nach abgerissener Kontinuität
kann ein späteres Geschwindigkeitssignal keinen alten Nullzustand wiederbeleben.
Fehlende Leistung nach Nichtnullwerten, GPS allein, Timerpausen und echte Verbindungs-
beziehungsweise Aufzeichnungslücken bleiben unbekannt oder unterbrechen das Fenster.

Diese Interpretation ändert keine FIT-Records, keine Dateien und keine Rohkurven.
Geschwindigkeit und echte Nullwerte werden weder in UI-Kurven noch im Journal erfunden.
Arbeit/Ratio/Kalorien nutzen weiterhin95% zeitliche Leistungsabdeckung; NP verlangt seit29.10.2026 jeweils90% Leistungsabdeckung und90% vollständige30-s-Fenster. Dadurch entwertet ein kurzer Akku-Ausfall
nicht automatisch eine sonst gut belegte lange Fahrt. Arbeit bleibt ausschließlich
beobachtete Arbeit ohne Hochrechnung. EF verwendet vollständig gepaarte Pulsdaten
über die gültigen Leistungsintervalle. FTP bleibt an exakt 20 Minuten unterbrechungs-
freien, auswertbaren Leistungsverlauf gebunden; unbekannte Abschnitte werden nicht verkettet.

Regressionen prüfen lange stille Nullphasen, eine zweistündige Fahrt mit kurzer echter
Lücke, unabhängige 30-s-Referenzrechnung, EF-Paarung, GPS ohne Bike-Beleg, fehlende
Nichtnullleistung, FIT-Roundtrip und unveränderte Rohdateien. Zusätzlich wurde eine
hochgeladene reale FIT-Datei lokal mit dem produktiven Detail-Leser neu ausgewertet.
Die private Datei und ihre Messwerte werden nicht im Repository gespeichert.

## Abschlussprüfung dieser Änderungen

Vollständige Suite: **307 Tests bestanden**, keine Fehler, Fehlschläge oder übersprungenen
Tests; Release-Build und Release-Lint-Vital erfolgreich in **2m 6s**. Nach der abschließenden
kleinen Dreier-Kachel-Dauer-/Einheitenanpassung wurden **43 gezielte UI-/Trend-/Glow-Tests**
und der Release-Build erneut erfolgreich ausgeführt (**57s**). Keine erneute vollständige
Suite nach dieser reinen Darstellungskorrektur. Bestehende `onBackPressed()`-Warnung bleibt;
kein zusätzlicher vollständiger Lint-Lauf oder Gerätetest. Keine neue signierte APK veröffentlicht.


NachBuild104,29.10.2026: ausdrückliche Nutzerentscheidung reduziert ausschließlich die NP-Daten-/Fensterqualitätsgrenzen von95% auf90% (MIN_NP_COVERAGE). Gemeinsame95%-Grenzen für Arbeit/Kalorien/Ratio bleiben erhalten; FTP95%-Faktor bleibt erhalten. Vollständige30sFenster weiter ohne Lückenfüllung/Verkettung, EF wiebisher vollständig gepaartePulsintervalle. Performance-MODEL_VERSION4 kennzeichnet geänderteFreigabepolitik bei künftigen Exporten; Rohdaten/Dateiformat unverändert, vorhandeneFITnichtautomatischüberschreiben. Fehlerhinweise verwenden90%.


NachfolgendeNutzerentscheidung29.10.2026: bestätigte E-Bike-Standpausen werden ausschließlich aus NP-Zeit/Fensterabdeckung entfernt. Aufnahme/Gesamtzeit bleiben durchgehend; keine Wattwerte erfinden.90%-NP-Grenzen beziehen sich aufNP-Auswertungszeit. Andere95%-Kennzahlenregeln bleiben unverändert. Modellversion5, benannte Journal-/FIT-Marker, keine automatische Änderung alterFIT. [Nachweise und Grenzen](confirmed-np-pauses.md).
