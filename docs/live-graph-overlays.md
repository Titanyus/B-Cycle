# Live-Graph-Overlays und SDK-Modernisierung

Aktueller Auswahlaufbau: [Daten, Graph und Visualisierung unabhängig](independent-dashboard-layers.md). Die frühere Graph-Darstellung-Auswahl wird nicht mehr angeboten.


Stand: 13.10.2026. Implementierung und Prüfung abgeschlossen und freigegeben.
Die signierte Test-APK 0.7.10 enthält sämtliche folgenden Änderungen und Korrekturen;
[Buildnachweis](milestone-0710.md).

## Bedienung und Darstellung

Die großen Einzelfelder von DATA, HERO, HERO_SEVEN und HERO_FIVE unterstützen bis zu
zwei Hintergrundgraphen. Die vorhandene Feldauswahl enthält unter den Messwerten den
Abschnitt „Graph-Overlay“. Auswahlreihenfolge: linke, dann rechte Skala. Bei zwei ausgewählten
Graphen sind weitere Optionen deaktiviert; Abwählen bleibt möglich. „Kein Graph“ leert die
Auswahl. „Speichern“ übernimmt ausschließlich die Graph-Auswahl, wenn der Hauptwert
unverändert bleiben soll. Die bisherige direkte Messwertauswahl bleibt erhalten und
übernimmt auch den Graph-Entwurf. „Abbrechen“ verwirft den Entwurf. Bearbeiten bleibt
während der Fahrt gesperrt, ohne Hinweisbox.

Die Auswahl wird separat pro Dashboardseite und Feld in den bestehenden `bike`-Einstellungen
aufbewahrt (`dashboardGraphs`, `dashboardGraphs_1` bis `_3`). Ein Wechsel zu einem kleinen
Feldlayout blendet die Graphen aus und bewahrt die Auswahl für das große Layout. Alte
Einstellungen starten ohne Graphen. Die Dashboard-Sicherung enthält die optionalen
`graphs`-Arrays je Seite; alte Sicherungen ohne diese Angabe lassen die aktuelle Auswahl
unverändert. Ungültige Sicherungen mit unbekannten, doppelten oder mehr als zwei Graphen
pro Feld werden abgelehnt.

Eine Drawable-Ebene liegt unter den vorhandenen Texten und Icons, über dem bisherigen
Hintergrund. Feldhöhe, Raster, Typografie und Touch-Flächen bleiben identisch. Alle
Graphbestandteile einschließlich Achsen sind auf die mittleren 80 Prozent des tatsächlichen
Innenbereichs begrenzt. Die Linie verwendet 225/255 Deckkraft bei 2,6 dp Stärke. Ein weicher Lichtsaum aus
zwei breiteren, transparenten Strichen umgibt die Linie. Die Füllung startet mit maximal
65/255 Deckkraft und läuft nach unten vollständig transparent aus. Die Farben stammen direkt aus `AnalysisChart`, derselben Definition wie in
den Streckendetails. Pro verfügbarer Datenreihe erscheinen zwei zurückhaltende Achsenwerte
mit einem kleinen farbigen Zuordnungsstrich. Vordergrundwerte bleiben unverändert.

## Datenfluss und Grenzen

`RideRuntime.appendSample()` reicht das bereits vorhandene, unveränderte `RideSample`
an den Anzeigeadapter weiter. Es gibt keine zusätzliche Sensorverbindung, Aufzeichnung
oder Performance-Berechnung. Geschwindigkeit und Distanz folgen den beim START eingefrorenen
Hauptquellen der bestehenden `RecordingConfig`: `SPEED`/`GPS_SPEED` und
`BIKE_DISTANCE`/`GPS_DISTANCE`. Geschwindigkeit wird nur für die Grafik von m/s in km/h
umgerechnet. Die übrigen Quellen sind `CADENCE`, `RIDER_POWER`, `HEART_RATE` und
`GPS_ALTITUDE`. Motorleistung wird nicht als Fahrerleistung verwendet. Position und
Zeitstempel stammen aus dem vorhandenen Sample; fehlende Distanz wird nicht erfunden.

Bosch-Freilauf wird anhand des bereits von `PerformanceEngine` bestätigten Zuwachses
von `quality.continuedZeroMs` dargestellt. Ohne diese Bestätigung wird fehlende Leistung
nicht zu Null. Fehlende Kadenz wird nur bei bestätigtem Freilauf und einem tatsächlich
zuletzt empfangenen Kadenz-Nullwert fortgesetzt. Verbindungsabbrüche beziehungsweise
abgelaufene Kontinuität erzeugen Lücken. Die Engine selbst und die Rohdaten bleiben
unverändert.

Ein gemeinsamer Anzeige-Worker hält maximal 2.048 Punkte für alle fünf Arten. Die
32-Punkte-Verdichtung behält Endpunkte, Minima, Maxima, Nullphasen und Lückengrenzen.
Die Warteschlange ist auf 16 Aufgaben begrenzt; bei Überlast gehen ausschließlich
Anzeigepunkte verloren und die nächste Strecke bekommt eine Unterbrechungsmarkierung.
FIT-Schreiben läuft weiter auf seinem bestehenden eigenen Worker. Kein UI-Code liest
für die Overlays FIT-Dateien oder hält eine vollständige Rohdatenkopie je Kachel.

Die gesamte aktuelle Fahrtdistanz wird auf die Breite skaliert; beide Graphen haben
dieselbe Distanzachse. Bei identischer Distanz wird innerhalb eines zusammenhängenden
Abschnitts der letzte tatsächliche Punkt verwendet. Es entsteht kein künstlicher Fortschritt.
Neue Fahrten leeren Verlauf und Skalen. Nach STOP werden keine Live-Punkte mehr angenommen;
eine abschließende Anzeigeübernahme hält ebenfalls den Zehn-Sekunden-Abstand ein.
Dashboardwechsel und App-Wechsel unterbrechen den gemeinsamen Verlauf nicht.

## Skalierung und Glättung

Die Y-Skalen erweitern sich anhand plausibler Originalwerte und schrumpfen während einer
Fahrt nicht. Schritte: Geschwindigkeit 5 km/h, Kadenz 10 U/min, Leistung 30 W,
Puls 20 bpm, Höhe 25 m. Etwa zehn Prozent Sicherheitsabstand lösen mindestens einen
vollen Erweiterungsschritt aus. Große Sprünge werden vollständig umfasst. Höhe beginnt
um den auf die nächste 25-m-Stufe gerundeten ersten Messwert, zum Beispiel 475–525 m bei 500 m.

Nach distanz- und pixelbezogener Verdichtung wird eine leichte, lokale Medianmischung
verwendet: 75 Prozent Originalpunkt, 25 Prozent benachbarter Median, nur innerhalb eines
kleinen Fensters von etwa zwei Pixelbreiten. Nullpunkte und ihre Übergänge bleiben
unverändert; getrennte Abschnitte werden unabhängig bearbeitet. Die Mischung kann keine
Überschwinger erzeugen. Linien werden ohne überschwingende Spline-Interpolation mit
runden Verbindungen gezeichnet. Skalen verwenden weiterhin Originalextrema, nicht die
geglättete Kurve. Aktualisierung und Glättung erfolgen höchstens alle zehn Sekunden.
Die Zahlenwerte bleiben live. Glättung, Deckkraft, Intervall und Stufen sind interne
Standards ohne zusätzliche Einstellungen.

## Geänderte Komponenten

- `GraphOverlayConfig.kt`: geordnete, kompatible Auswahl je Seite/Feld.
- `GraphOverlayPicker.kt`, `FieldPickerView.kt`, `MainActivity.kt`: vorhandene Feldauswahl,
  Speichern/Abbrechen und Wiederladen der Konfiguration.
- `LiveGraphData.kt`: Adapter, begrenzter Verlauf, Skalen und Anzeige-Glättung.
- `LiveGraphProvider.kt`, `RideRuntime.kt`: gemeinsamer Anzeige-Worker und vorhandene Samples.
- `DashboardGraphDrawable.kt`, `DashboardView.kt`, `DashboardTemplate.kt`: Hintergrundebene
  und ausschließlich große Felder, ohne Layoutänderung.
- `SettingsBackup.kt`, `strings.xml`: Dashboard-Sicherung und zentrale lokalisierbare Texte.
- `app/build.gradle.kts`, `MainActivity.kt`: SDK 36 und native Zurück-Anbindung.
- `LiveGraphDataTest.kt`, `LiveGraphUiTest.kt`, `SdkNavigationTest.kt`: neue Regressionstests.
- `FullscreenDialogTest.kt`, `RecordingFlowTest.kt`, `HeartRateConnectionTest.kt`: API-36-Abdeckung;
  die BLE-Test-Shadow überschreibt die inzwischen in Robolectric vorhandene Methode korrekt.
- `SerialNativeRuntimeLoader.kt` und dessen Test-SPI: Schriftinitialisierung verschiedener
  SDK-Sandboxes serialisiert, um eine JVM-ZIP-Kollision zu verhindern. Ausschließlich Tests.
- `RouteAnalysisUpdateTest.kt`: alte Erwartung an die bereits gewünschte Gruppenreihenfolge
  korrigiert (Kadenz & Puls vor Leistung). Keine Änderung an der Detailansicht.
- `AGENTS.md`, `README.md`, `docs/sdk-modernization.md`: UX-Regeln und aktueller Nachweis.

## Prüfung

Vollständiger abschließender Lauf:

```sh
./gradlew :app:testDebugUnitTest :app:assembleRelease --console=plain
```

**336 Tests bestanden, 0 Fehler, 0 übersprungen**, einschließlich Android 13/15/16 für
Fullscreen/IME, Zurück-Navigation, Pulsmesser-GATT und den Aufzeichnungslebenszyklus.
Release-Build inklusive R8, Resource-Shrinking und Release-Lint-Vital erfolgreich.
Der abschließende Lauf mit erneuter Release-Kompilierung und Paketierung dauerte 2m 11s;
der vorherige inkrementelle Lauf 1m 35s. Die unsignierte Release-APK hat 1.059.710 Byte.

Die neuen Tests prüfen Auswahlgrenzen/Reihenfolge/Persistenz, Legacy-Backup, fehlende
Quellen, Freilaufbestätigung, Nullwerte, Datenlücken, Stillstand, Skalenwachstum,
Glättungsgrenzen, fünf Stunden synthetischen Verlauf, Worker-Überlast, Zehn-Sekunden-Takt,
STOP/Neustart, Pixel-Clipping und unveränderte Geometrie/Schriften. Ein Test vergleicht
Journal und FIT-Export vor/nach der Graphvorbereitung bytegenau. Die bestehenden
Performance-, FIT-/Garmin- und UI-Tests bleiben erhalten.

Eine reale Android-16-Fahrt mit Bosch/Pulsmesser, Display-Off, Dateiprovidern und
WebDAV-Job-Verhalten ist in dieser Cloud-Umgebung nicht möglich und bleibt Teil des
Gerätetests. Die signierte APK 0.7.9 ist auf GitHub veröffentlicht.

## Korrektur nach APK 0.7.9 (13.10.2026)

Die Direktwert-Chevrons für Bike-Tempo, GPS-Tempo, Fahrerleistung, Kadenz und Puls
verwenden jetzt direkt die vorhandene Berechnung und denselben Verlauf wie ihre
zugehörigen Durchschnittsfelder. GPS-Tempo folgt dem Durchschnitt in Bewegung.
Der getrennte Vergleich von momentanem Wert gegen Durchschnitt wurde entfernt.
Es gibt keine zusätzliche Trendberechnung. Insbesondere folgt Fahrerleistung weiterhin
dem bereits vorhandenen geglätteten Watt-Durchschnittstrend, auch bei kurzen Tretpausen.

Graphlinien: 1,3 → 2,6 dp. Kräftigere Darstellung durch zentral erhöhte Deckkraft:
Linien 100 → 140/255, Flächen 22 → 30/255. Die semantischen Farben aus den
Streckendetails werden weiterhin unverändert wiederverwendet.

Nur gezielt geprüft: sieben `RideAveragesTest`-Tests plus der native Pixel-/Clipping-Test
`LiveGraphUiTest.axesAndEveryGraphPixelStayInsideEightyPercentOfInnerHeight`.
**8 Tests bestanden**, Release-Kotlin-Kompilierung erfolgreich; zusammen 10 Sekunden.
Keine vollständige Testsuite und keine neue APK für diese Korrektur.
Die oben beschriebenen Änderungen sind noch nicht in der veröffentlichten APK 0.7.9 enthalten.

## Größere Skalen, Overlay-Punkte und Graph-Leuchtsaum (13.10.2026)

Die Skalenschrift der Live-Graphen ist um 80 Prozent vergrößert: 9 → 16,2 dp.
Direkt rechts neben der Bezeichnung eines großen Feldes erscheinen kleine farbige Punkte
für dessen aktivierte Overlays, in Auswahlreihenfolge und den bestehenden Graphfarben.
Die Punkte verwenden dieselbe Kreis-Darstellung wie die Aufnahmeanzeige; ihre Größe ist
an die kleinere Feldbezeichnung angepasst. Bei deaktivierten Overlays und auf kleinen
Feldern gibt es keine Punkte. Der rote Aufnahmepunkt bleibt unverändert.

Die Linie erhält einen dezenten Lichtsaum aus zwei transparenten, breiteren Strichen
und einen kräftigeren Kern. Unter der Kurve wird ein vertikaler Farbverlauf gezeichnet,
der vollständig nach unten ausläuft. Pfade und Farbverläufe werden nur bei einem neuen
Graphbild oder einer Größenänderung vorbereitet. Keine Blur-Filter, zusätzlichen
Bitmaps, Animationen oder Timer. Der Lichtsaum bleibt mit der gesamten Graphdarstellung
auf die mittleren 80 Prozent des Feldinnenbereichs begrenzt. Datenlücken bleiben offen.
Die Bosch-/Akku-Randglüheffekte sind hiervon unabhängig und werden nicht geändert.

Nur fünf gezielte native Tests: Skalen-/Pixel-Clipping, unveränderte Feldgeometrie und
Typografie, Auswahl-/Farbfolge der Punkte, Leuchtsaum/Farbverlauf/Datenlücken sowie der
bestehende rote Aufnahmepunkt. **5 Tests bestanden**, Release-Kotlin-Kompilierung
bestanden; zusammen 11 Sekunden. Keine vollständige Testsuite und keine neue APK.
Diese Änderungen sind noch nicht in APK 0.7.9 enthalten.

## Auslaufende Grenzlinien (13.10.2026)

Links und rechts an der eigentlichen Kurvenfläche stehen dünne neutrale Linien im
bestehenden Dashboard-Grauton. Ihre Deckkraft nimmt zur Mitte zu und an beiden
vertikalen Enden bis Null ab. Beide verwenden einen gemeinsam vorbereiteten Farbverlauf.
Kurven, Füllung und Lichtsaum werden horizontal an diesen Grenzen abgeschnitten;
Skalen bleiben außerhalb der Kurvenfläche und innerhalb des vorhandenen 80-Prozent-Bereichs.
Keine Änderung an Daten, Skalierung, Hauptmesswerten, Touch-Flächen oder Timern.

Drei gezielte native Tests für die beiden symmetrischen Fade-Linien, Pixel-Clipping
und Leuchtsaum/Füllung/Datenlücken sowie Release-Kotlin-Kompilierung: erfolgreich in
6 Sekunden. Die zuvor geprüften Punkte-/Geometrie-/Aufnahmepunkt-Tests bleiben gültig.
Zusätzlich wurde ein alter Kartentest an die bereits freigegebene Chevron-Regel angepasst:
Auch der Direktwert über der Karte folgt dem Verlauf seines nicht angezeigten Durchschnitts,
statt den aktuellen Wert mit dem Durchschnitt zu vergleichen. Dieser einzelne UI-Test
bestand ebenfalls. Keine Änderung an Produktivcode oder Trendberechnung für diese Testkorrektur.

Insgesamt wurden für diese Darstellungsanpassungen **sieben unterschiedliche gezielte
Tests** geprüft; keine vollständige Suite und keine neue APK.

## APK-Veröffentlichung

Alle oben beschriebenen Korrekturen einschließlich Grenzlinien sind in der signierten
Test-APK **0.7.10** enthalten. Die Hinweise „keine neue APK“ in den einzelnen
Änderungsschritten beziehen sich auf deren damalige gezielte Prüfung.
[Release-Nachweis](milestone-0710.md).

## Farbpunkte und Quellen-Symbole in der Feldauswahl (13.10.2026)

Jede der fünf Graph-Checkboxen enthält einen kleinen Punkt in exakt derselben Farbe
wie ihr Gegenstück neben der Dashboard-Feldbezeichnung. Wiederverwendung der vorhandenen
Kreis-Darstellung und `AnalysisChart`-Palette. „Kein Graph“ bleibt ohne Punkt.
Auswahlreihenfolge, Begrenzung auf zwei, Speichern/Abbrechen und persistente IDs bleiben gleich.

Das bestehende Quellen-Dropdown zeigt dieselben GPS-/Fahrradglyphen wie das Dashboard,
ein Lucide-Herz für Pulsmesser und `cloud-sun` für Wetter. Symbole stehen vor dem Text
und werden in der Theme-Vordergrundfarbe gezeichnet, sowohl bei der aktuellen Auswahl
als auch in der geöffneten Liste. Die ursprünglichen String-Einträge des Dropdown-Modells
bleiben erhalten. Die zentrale Spinner-Darstellung unterstützt dafür optional formatierte
Labels; andere Dropdowns behalten ihre bisherige Darstellung. Recycelte Zeilen behalten
die richtige Quelle; Theme-Bindings verwenden deren jeweils aktuelle Quelle.

`cloud-sun` ist ein lokaler VectorDrawable aus der bereits verwendeten Lucide-Version
0.468.0. Keine zusätzliche Bibliothek. Das vorhandene Importskript enthält das Symbol
zur Reproduzierbarkeit; die bestehende Lizenz bleibt unverändert.

Vier gezielte Tests: bestehendes Quellen-Umschalten/Scrollen, Quellen-Icons in beiden
Dropdown-Darstellungen einschließlich Tint und Zeilen-Recycling, Graph-Punktfarben sowie
bestehende Auswahlgrenzen/Persistenz. **4 Tests bestanden**, Release-Kotlin-Kompilierung
erfolgreich; zusammen 12 Sekunden. Keine vollständige Suite und keine neue APK.
Diese Änderungen sind noch nicht in APK 0.7.10 enthalten.

## Kleinere Anfangsbereiche (13.10.2026)

| Graph | Anfangsbereich | Erweiterungsschritt |
| --- | --- | --- |
| Tempo | 0–20 km/h | 5 km/h |
| Kadenz | 0–60 U/min | 10 U/min |
| Fahrerleistung | 0–120 W | 30 W |
| Puls | 50–100 bpm | 20 bpm |
| Höhe | ungefähr ±25 m um die erste Höhe | 25 m |

Für Höhe wird die Ausgangsmitte auf die nächstgelegene 25-m-Stufe gerundet, damit
nahezu gleiche erste Messwerte nicht direkt eine unnötige Erweiterung auslösen.
Die Skalen wachsen weiterhin mit etwa zehn Prozent Sicherheitsabstand und schrumpfen
während einer Fahrt nicht. Werte nahe oder außerhalb der Startgrenzen erweitern die
Skala bereits beim ersten Messwert. Keine Änderung an Kurvenzeichnung oder Sensordaten.

Zwei gezielte Skalierungstests prüfen die neuen Anfangswerte und Schritte, Sicherheitsabstand,
große Sprünge, unabhängige Skalen und ausbleibende Verkleinerung. Beide bestanden;
Release-Kotlin-Kompilierung erfolgreich. Zusammen 2 Sekunden. Keine neue APK.

## Darstellung ab 0.7.12

Design & UI enthält eine globale Auswahl „Graph-Darstellung“ mit „Graph mit Daten“
(Standard) und „Nur Graph“. Die Auswahl wird sofort unter `dashboardGraphDisplay`
gespeichert und bei der Design-&-UI-Sicherung mitgeführt. Ältere Sicherungen ohne
diesen Wert lassen die aktuelle Auswahl bestehen. Unbekannte gespeicherte Modi
fallen auf den Standard zurück; ungültige Sicherungswerte werden abgelehnt.

„Nur Graph“ blendet bei Kacheln mit aktiven Graphen die Messwert-/Einheitenzeile
inklusive Chevron aus. Feldbezeichnung, Farbpunkte und Skalen bleiben sichtbar.
Die Zeile ist unsichtbar statt entfernt: Raster, Schriftgrößen, Kachelhöhe und
Kartenplatz verändern sich nicht. Ohne aktive Graphen bleiben normale Werte
sichtbar. Die zugeordneten Felder und aktuellen Sensorwerte bleiben erhalten.

Der Farbverlauf wurde über seine Deckkraft um Faktor 1,8 verstärkt: oberer
Alpha-Wert 65 → 117 von 255, weiterhin nach unten auf transparent auslaufend.
Beim Modus „Graph mit Daten“ wird die Hauptlinie auf 70 % und der Farbverlauf
auf 85 % der neuen vollen Deckkraft reduziert. Skalen, Farbpunkte und Grenzlinien
bleiben unverändert. Der innere Leuchtsaum verwendet jetzt **11,2 dp**, der äußere
**19,2 dp** Gesamtstrichbreite; beide sind doppelt so breit wie zuvor. Die
Hauptlinie bleibt **2,6 dp**. Leuchtsaum-Deckkräfte bleiben 60/255 und 24/255.
Es gibt weiterhin keinen Blur-Filter oder zusätzliche Bildpuffer.

Die Tests vergleichen tatsächliche gerenderte Pixel zwischen beiden Modi,
Leuchtsaumgrenzen, Clipping und Datenlücken; sie prüfen außerdem alle Preset-
Geometrien, sichtbare Beschriftungen, Wiederherstellung normaler Werte und
sofortige persistente Auswahl inklusive kompatibler Settings-Sicherung.
Sensorverarbeitung, Graphdaten, Zehn-Sekunden-Takt, FIT und Performanceberechnung
werden durch die Darstellung nicht verändert.

## Kachelbezogene Auswahl nach APK 0.7.12

Die globale Auswahl in Design & UI entfällt. Stattdessen enthält der vorhandene
Datenauswahl-Dialog jeder graphfähigen Kachel direkt bei der Graph-Auswahl
„Graph-Darstellung“. „Nur Graph“ bzw. „Graph mit Daten“ gelten unabhängig pro
Dashboardseite und Slot. Änderungen werden sofort gespeichert; Quellwechsel
im Dialog erhalten Graphauswahl und Darstellungsmodus.

GraphOverlayConfig verwaltet die Modi gemeinsam mit der bestehenden Auswahl.
Die bisherigen Graph-Schlüssel/Graphnamen bleiben unverändert. Modi liegen in
`dashboardGraphDisplay_<Seite>` als Slot-Liste. Ohne gespeicherten Slotmodus wird
die bisherige globale Einstellung als Ausgangswert übernommen; neue Installationen
verwenden „Graph mit Daten“. Die Renderkomponente verwendet jeweils den Modus
der betroffenen Kachel. Helligkeiten, Leuchtsaum und Geometrie bleiben unverändert.

Settings-Sicherungen speichern die Modi bei den Dashboardseiten unter
`graphDisplayModes`, nicht mehr als globale Designoption. Ältere Sicherungen ohne
Slotmodi erhalten die aktuelle Kachelbelegung; ungültige Modi werden abgelehnt.
Die alte globale Designangabe bleibt beim Einlesen alter Sicherungen unterstützt.

## Zeitfenster und verfügbare Quellen nach Build 56

Oben im Graph-Accordion vier zentrale Auswahlsegmente: Voll / 30s / 60s / 120s. Pro Dashboardseite und Feld gespeichert (`dashboardGraphWindows_<page>`), Standard Voll; Profilsicherung und Dashboard-Sicherung berücksichtigen die Auswahl. Alte Sicherungen starten für neue Fenster mit Voll. Die fünf Graphoptionen bleiben sichtbar, sind ohne passende konfigurierte und aktivierte Sensorquelle gedimmt/deaktiviert. Temporärer Empfangsausfall löscht keine Auswahl. Höhe benötigt Outdoor-GPS; Leistung/Kadenz einen konfigurierten E-Bike-/FTMS-Sensor; Puls einen Pulsmesser oder den möglichen FTMS-Pulskanal. Tempo folgt der eingestellten primären Tempoquelle. Keine Abhängigkeit von gerade empfangenen Livewerten.

Voll behält die bisherige Gesamtansicht über die Distanz; ohne jegliche Distanzdaten ist eine vollständige Zeitachse möglich. Rollierende Fenster zeigen tatsächliche Aufnahmezeit, unabhängig von Bewegung/Distanz: erst links füllen, danach den gewählten Zeitraum nachführen. Ein separater begrenzter Rohpuffer hält die letzten 120 Sekunden plus Randpunkt; lange Fahrten verlieren dadurch im sichtbaren Fenster keine jüngsten Samples durch die Gesamtverlaufsreduktion. Echte Nullwerte und Empfangslücken bleiben erhalten. Die Gesamtansicht wird weiter höchstens alle zehn Sekunden neu vorbereitet; bei sichtbarer rollierender Auswahl entstehen zusätzliche Zeitfensterframes höchstens einmal pro Sekunde auf demselben Worker. Keine zusätzlichen Sensorsubskriptionen oder Änderungen an Journal/FIT/Performance.

## Formtreue Kurven nach Build 61

Dashboard-Linien werden innerhalb bestehender zusammenhängender Abschnitte mit einer formtreuen kubischen Hermite-Interpolation verbunden. Gewichtete harmonische Tangenten, Nullsteigung an Richtungswechseln/Plateaus und Begrenzung der Tangenten verhindern neue Extremwerte zwischen benachbarten dargestellten Punkten. Die Kurve geht durch die Punkte; keine zusätzliche Mittelwertbildung und keine Änderung von Messwerten, Skalen, FIT oder Aufzeichnung. Vorhandene Anzeigeaufbereitung bleibt bestehen.

`MonotoneGraphPath` erzeugt nur beim bisherigen Pfad-Rebuild die gemeinsame Geometrie für Linie, Halo und Füllung. Nullwerte/Break-Markierungen schließen einen Abschnitt; sie werden nicht überbrückt. Gleiche Distanz/zusammenfallende X-Koordinaten behalten ihre vertikale Kante, statt durch Null zu teilen oder neue Werte zu erfinden. Voll- und alle rollierenden Zeitfenster (30/60/120 s) nutzen denselben Renderer. Keine zusätzliche Nutzereinstellung oder externe Abhängigkeit.

Drei gezielte neue Prüfungen für Punktdurchgang/fehlendes Überschwingen bei ungleichmäßigen Abständen und Spitzen, gleiche Distanz/getrennte Abschnitte sowie native Darstellung in allen Zeitfenstern. Bestehende Prüfungen für weichen Halo/Füllung ohne Lückenüberbrückung und unveränderte Zeichengrenzen ebenfalls erfolgreich. Natives Kurvenbild visuell geprüft. Veröffentlicht in APK 0.8.23 / Build 62.

## Linien ohne Glow (23.10.2026)
Dashboard-Verlaufslinien und die Leistungs-/Pulszonen-Verteilungen der Streckendetails zeichnen nur den eigentlichen Linienstrich, ohne breite halbtransparente Halo-Striche. Flächenverlauf, Linienfarbe/-stärke, Glättung, Achsen und Daten bleiben erhalten. Fachliche Zonen-/Fahrmodus-Hintergrundglows sind davon unabhängig.

## Rollierende Rückskalierung nach Build 72

30-/60-/120-Sekunden-Fenster ermitteln ihren Zielbereich ausschließlich aus den gültigen Werten im aktuellen Zeitfenster, mindestens mit dem ursprünglichen Startbereich der Kennzahl (bei Höhe um den ersten Höhenwert). Der eine für kontinuierliche Randsegmente mitgeführte Punkt vor dem Fenster hält keinen alten Peak in der Skala fest. Voll bleibt nach oben/unten erweiternd wie bisher.
Neue Peaks erweitern den Bereich sofort; Rückkehr zum kleineren Zielbereich über 3000 ms mit weichem Ein-/Auslauf. Übergang ist nur Darstellung, keine Messwertänderung. Pro Drawable/Kennzahl getrennt, beim Fenster-/Fahrtwechsel zurückgesetzt. Markergröße gemeinsam mit den 7-dp-Farbpunkten hinter den Dashboard-Beschriftungen definiert. Skalenwerte stehen innerhalb der Begrenzungslinien; beide Graphen nutzen unabhängig von einer/zweier Skalen dieselbe volle Breite.

Alle Modi verwenden MonotoneGraphPath ohne Überschwingen. Voll hat davor zusätzlich Pixel-/Distanzreduktion und einen konvexen 75/25-Medianblend; rollierende Fenster behalten ihre zeitgenauen dichteren Punkte. Das erklärt die glattere Wahrnehmung von Voll; diese Datenaufbereitung wurde in diesem Schritt nicht geändert. Noch keine APK.

Prüfstand nach Build 72: 32 gezielte Tests in den Umsetzungsschritten erfolgreich (Aufnahmeregression auf Android 13/15/16, Detailsnavigation, Skalenrückkehr/Fenstergrenzen, Graphgeometrie, Akku-Persistenz/Rohdatentrennung und Zehnsekundengrenze); vier betroffene Tests nach letzter Absicherung nochmals erfolgreich. Native Summary geprüft. Typografie-/Atlasprüfung erfolgreich, kein Hardware-/Diensttest oder neuer APK-Build.
