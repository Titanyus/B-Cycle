# Bestandsaufnahme: B|Cycle 0.6.20

Stand: 11.10.2026, Meilenstein `v0.6.20`, Commit
`726a813e754eec2dd081747505099d51bf798b96`.
Read-only-Analyse des App-Codes; keine Funktionen oder Build-Einstellungen geändert.
188 App-/Build-Dateien per Git-Blob-Hash mit dem Meilenstein verglichen: identisch.

## Ergebnis

Die Basis ist schlank. Sinnvolle erste Maßnahmen sind Versionsangaben korrigieren,
alte Icon-Implementierungen bereinigen und unnötige UI-Arbeit reduzieren.
Keine Begründung für einen Architekturwechsel oder zusätzliche Bibliotheken.
Es gibt hier keine Messung von Akkuverbrauch, CPU-Zeit oder Frametimes am Poco;
die entsprechenden Punkte sind Kandidaten aus dem Quellcode, keine bestätigten
Leistungsprobleme. Vollständige Funktionserhaltung muss durch Tests und Hardwarevergleich
abgesichert werden, nicht durch Größenvergleiche der APK allein.

## Ausgangsbasis

- Kotlin, native Views, eine Activity; 60 Kotlin-Quelldateien unter main.
- minSdk 33, target/compileSdk 35, Kotlin 2.1.20.
- Drei explizite Laufzeitbibliotheken: Mapsforge 0.25.0, Garmin FIT 21.218.0,
  OkHttp 4.12.0. Transitiv: Mapsforge Core/Map/Reader, AndroidSVG 1.4, Okio und
  Kotlin-Standardbibliothek. Keine Testbibliotheken in releaseRuntimeClasspath.
- R8-Minifizierung und Ressourcenbereinigung bereits aktiv.
- Signierte APK: 934.741 Byte. ZIP-Inhalte komprimiert: DEX 763.568 Byte,
  Ressourcen 57.959 Byte, Assets 36.545 Byte, übrige Einträge 62.119 Byte.
  Die Summe schließt ZIP-Verzeichnis und Signaturblock nicht ein.
- Letzter Meilenstein-Build: 211 erfolgreiche Tests, Release-Build erfolgreich.
  Für diese Bestandsaufnahme nicht unnötig wiederholt.
- Neu ausgeführt: `:app:lintDebug` und Dependency-Report für releaseRuntimeClasspath,
  erfolgreich in 31 Sekunden: 0 Fehler, 45 Warnungen.

## Priorisierte Vorschläge

| Prio | Befund und Nachweis | Vorschlag | Nutzen / Prüfbedarf |
| --- | --- | --- | --- |
| 1 | Versionsstrings noch 0.6.11: RideRuntime.kt (Diagnosekopf), MainActivity.kt (Diagnosedateiname), MapPanel.kt (Karten-User-Agent). FitWriter.kt schreibt zusätzlich eine feste Softwareversion 0.4. | Eine gemeinsame aktuelle Versionsquelle verwenden; FIT-Metadaten mit eindeutigem Versionsschema abbilden. | Verlässliche Diagnose und Herkunft, geringe Auswirkung auf Laufzeit. FIT-/Diagnosetests prüfen. |
| 1 | RouteActionIcon.kt hat keinen Aufrufer in main oder Tests. RouteSymbolDrawable/RouteSymbol nur noch in BicycleComponentsTest, keine produktive Verwendung. R8 entfernt beide Klassen schon aus Release. | Alten Renderer entfernen; Komponentenprüfung auf den vorhandenen Lucide-Renderer umstellen. | Weniger Pflege, kaum APK-Ersparnis zu erwarten. Gewünschte alte Einstellungsicon-Dateien behalten. |
| 2 | RideRuntime.notifyChanged liefert jedes BLE-/GPS-Ereignis plus 1-Hz-Tick an MainActivity; Observer ruft render, refreshSensorRows und refreshRouteRows auf. | UI-Updates pro Frame bündeln, dieselben GPS-/Bike-/Puls-Snapshots innerhalb einer Darstellung wiederverwenden; unveränderte Texte/Flags nicht neu setzen. | Potenzial für weniger Allokationen/Layoutarbeit. Roh-Sensorevents und FIT-Aufzeichnung ausdrücklich nicht drosseln. Profiling vor/nach Änderung. |
| 2 | MapPanel.refreshRoutes kopiert den ganzen Live-Track bei jeder Trackrevision; RouteTrackLayer projiziert beim Zeichnen jeden Punkt erneut. Bei längeren Fahrten bis rund 20.000 Punkte je angezeigtem Track. | Speicher-/CPU-Profil langer Tracks; erst danach Projektion/Display-Daten gezielt cachen oder vereinfachen. | Mögliches Hauptpotenzial beim Zoomen/Pannen. Unveränderliche Übergabe zwischen UI- und Layerthread, Segmentlücken und FIT-Originale erhalten. |
| 2 | FitRead prüft Integrität und decodiert die Datei, bei Import/anschließender Auswahl kann eine weitere Dekodierung erfolgen. Läuft bereits im Worker. | Große FIT-Dateien und Bibliotheken messen; bereits ermittelte Vorschau/Details wiederverwenden, wo Lebensdauer und Dateistempel passen. | Schnellere Imports/STOPP-Verarbeitung; geringe Priorität gegenüber laufender Fahrt. CRC-Prüfung nicht entfernen. |
| 3 | SolarWeather wird mehrfach über gpsValues innerhalb derselben Darstellung abgefragt; Sonnenereignisse sind bereits gecacht, Ortsvergleich und Snapshot-Erzeugung erfolgen erneut. | Mit gemeinsamen UI-Snapshots abdecken; keine eigene zusätzliche Timerarchitektur. | Kleines Potenzial; keine belegte teure sekündliche Sonnenberechnung. |

## Lint richtig einordnen

| Warnung | Anzahl | Einschätzung |
| --- | ---: | --- |
| StaticFieldLeak | 1 | RideRuntime.get erzeugt den Singleton ausdrücklich mit applicationContext. Kein Beleg für einen Activity-Leak. In einem Heap-Vergleich Activity-/View-Referenzen und Observer prüfen; nicht den Aufzeichnungssingleton blind entfernen. |
| UnusedResources | 17 | Alte Strings, Farben und Icons. Viele sind historische/vom Nutzer bewusst zu behaltende Ressourcen. R8/Resource Shrinker beseitigen unbenutzte Release-Ressourcen bereits. |
| ViewConstructor | 8 | Views werden programmatisch gebaut, nicht über XML inflation. Keine zusätzlichen Konstruktoren nur für die Warnungszahl einführen. |
| ClickableViewAccessibility | 5 | Gesten-/Touch-Listener prüfen. Mehrere geben false zurück und lassen native Klickbehandlung weiterlaufen. Nicht pauschal performClick ergänzen: Gefahr doppelter Klicks oder Aktionen beim Wischen. TalkBack-/Gestentests sinnvoll. |
| SetTextI18n | 14 | Zusammengesetzte Texte/Lokalisierung, keine Performancefehler. Bei späterer Mehrsprachigkeit in Stringressourcen überführen. |

Zusätzlich bekannte Compilerwarnung: MainActivity.onBackPressed ist veraltet.
Eine Migration auf Androids Back-Dispatcher ist eine eigene kleine Funktionsänderung:
Dialog-Rücknavigation und Karten-Vollbild dabei gezielt testen, keine Bibliothek nötig.

Unbenutzte Ressourcen umfassen beispielsweise ic_brand, ic_lucide_smartphone,
settings_header_background und mehrere alte ic_settings_*.
Die bisherigen Einstellungsicons werden entsprechend der Nutzeranweisung aufbewahrt.
Alle R8-usage-Einträge sind nicht automatisch Löschkandidaten: R8 meldet auch
Inlinings, synthetische Kotlin-Methoden und absichtlich vorbereitete Theme-/Energiemodelle.
Die Custom-Theme-Grundlage und künftige Energieprofile bleiben erhalten.

## Bereits sinnvolle Schutzmaßnahmen

- Keine dauerhafte BLE-Suche: zeitlich begrenzt; serielle begrenzte Write-Queue.
- Activity meldet Runtime-Observer bei onStop ab; Karte pausiert/destroyed beim Lifecycle.
  Eine gestartete Aufzeichnung behält gemeinsam genutzte Sensoren.
- FIT-Journal auf separatem Worker, begrenzte Samplequeue und Sensor-Eventbatches.
  Flush pro Sample, fsync alle fünf Samples: Schutz vor Datenverlust, nicht einfach entfernen.
- MapTrack mit Punktlimit und Coarsening; FIT behält Originaldaten.
- ViewportTileCache hält den Mapsforge-Arbeitsbereich vor. Diese Korrektur hat das
  Kachelflimmern behoben; kein pauschales Verkleinern des Caches.
- Karten-Layer redraw nur bei GPS-/Track-/Routenänderung; unveränderte Wertetexte
  teilweise bereits gecacht. Paint/Path und Glow-Shaders werden wiederverwendet.
- Glow läuft nur während 1,5 Sekunden Zustandsübergang mit Animationstimer.
- JobScheduler statt dauerhafter Uploadschleife; Uploadstatus gecacht und Dateistempel
  genutzt. Passwort nicht Bestandteil der Fahrt-/Uploadindizes.

## Messplan auf dem Poco

Gleiche Route/Datei, Helligkeit, Sensoren, Einstellungen und vergleichbare Empfangsbedingungen
für die Meilenstein-APK und spätere optimierte APK verwenden:

1. Fünf Minuten ohne gestartete Fahrt, Sensoren/GPS aus: CPU und unerwartete Timer prüfen.
2. Fahrt mit GPS, Bosch und Puls, Karte sichtbar: Frametimes/Jank, Allokationen und CPU.
3. Gleiche Aufzeichnung bei ausgeschaltetem Display: Wake-Lock, GPS, CPU und Akku vergleichen.
4. Langen GPX-/FIT-Track zoomen/pannen: Frametimes, Layerthread und Heap messen.
5. Mehrfach Dialoge öffnen/schließen und Activity neu erstellen: Heap/Observer vergleichen.
6. STOPP, lokale Speicherung und Upload/Wiederholung: Dauer, korrektes FIT und Datenumfang.

Android Studio Profiler/Perfetto, adb gfxinfo und batterystats sind geeignete Werkzeuge.
Kurze Profilersamples messen Rechenarbeit; Energievergleiche brauchen längere wiederholte
Läufe. Profiling möglichst nahe am Release-Verhalten; APK-Größe ist kein Akkuindikator.
Die BLE-Sensorrate, GPS-Frequenz, Wake-Lock und Journal-Synchronisierung bleiben zunächst
unverändert. Eine Änderung daran benötigt eigene Datenqualitäts- und Hintergrundtests.

## Empfohlene Durchführung

1. Kleine Bereinigung: Versionsangaben und nachgewiesene alte Renderer; kein neues Feature.
2. Bestehende relevante Tests, anschließend komplette Suite und signierter Vergleichsbuild.
3. Hardwaremessung der UI/Track-Hotspots; einzelne, rücknehmbare Optimierungsschritte.
4. Vor/nach Vergleich: Messwerte, Fit-Garmin-Validierung, Hintergrundfahrt, Karte und Bedienung.
5. Ergebnis und verbleibende Unsicherheiten dokumentieren; v0.6.20 bleibt Rückfallpunkt.

Keine Bereinigung oder Optimierung wurde im Rahmen dieser Analyse ausgeführt.
