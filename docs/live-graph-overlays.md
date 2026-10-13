# Live-Graph-Overlays und SDK-Modernisierung

Stand: 13.10.2026. Implementierung und Prüfung abgeschlossen und freigegeben.
Die signierte Test-APK 0.7.9 enthält diese Änderungen; [Buildnachweis](milestone-079.md).

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
Innenbereichs begrenzt. Die zentralen Deckkräfte betragen 140/255 für Linien und 30/255
für Flächen; die Linienstärke beträgt 2,6 dp. Die Farben stammen direkt aus `AnalysisChart`, derselben Definition wie in
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
Fahrt nicht. Schritte: Geschwindigkeit 10 km/h, Kadenz 20 U/min, Leistung 50 W,
Puls 20 bpm, Höhe 25 m. Etwa zehn Prozent Sicherheitsabstand lösen mindestens einen
vollen Erweiterungsschritt aus. Große Sprünge werden vollständig umfasst. Höhe beginnt
um den ersten gültigen Messwert, zum Beispiel 450–550 m bei 500 m.

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
