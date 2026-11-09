Fortschreibung09.11.2026: Punkte1 und5 sowie gezielte Dashboard-Kachelaktualisierung sind umgesetzt und geprüft. [Aktueller Bericht](dashboard-efficiency-2026-11-09.md). Die nachfolgenden Befunde dokumentieren den vorherigen Durchgang.

# Gezielte Effizienz-Durchsicht vom 08.11.2026

Nutzerauftrag: die App auf unnötige Arbeit, Schwachstellen und Verschlankung prüfen; auch kleine vermeidbare Kosten beseitigen. Statische Prüfung statt Profiling/Benchmarks. Keine Prozentangaben zur Akkuersparnis, keine Änderung von Sensorfrequenzen, Aufzeichnung oder Analyseformeln. Keine APK beauftragt.

Durchgesehen wurden zentrale Aktualisierungspfade von MainActivity/Dashboard/Karte, Graphen, Kartenstilen, Profil-/Statistikanzeige, HRV, Autosave/Cloud, Foto-/Trainings-/Fitness-Workern sowie Release-Shrinking. Das ist ein gezielter Quellcode-Durchgang der Laufzeitpfade; keine Behauptung, jede Zeile oder jeden Gerätetreiber geprüft zu haben.

## Nachgewiesen und behoben

| Stelle | Vorher / Auslöser | Änderung und Nutzen |
| --- | --- | --- |
| `LiveGraphProvider.configureDisplay/append` | Im Normalmodus ignorierte die Veröffentlichung unsichtbare Diagramme. Bei wiederholtem identischem Anzeigezustand wurden trotzdem neue Demand-Maps/Sets gebaut. | Auch ohne Sparmodus nur sichtbare Graphen vorbereiten; identische Konfiguration sofort verlassen. Historie weiter aufnehmen, bei Rückkehr einmal vollständig aktualisieren. Weniger Workerarbeit, UI-Benachrichtigungen und Allokationen. Abschließender Flush beim Aufnahmeende bleibt. |
| `DashboardGraphDrawable.animation/draw` | Animationscallback baute Pfade neu, nachfolgendes Zeichnen bei laufender Skalierung nochmals. | Callback markiert nur den anstehenden Frame; Pfade einmal beim Zeichnen aufbauen. Letzten Frame ausdrücklich bis zum Ziel berechnen, danach keine weiteren Animationstermine. Darstellung und3-s-Rückskalierung unverändert. |
| `MapRouteStyles.read` | Bei Kartenbewegung, GPS-Updates und Vorschau wurde für jede einzelne Linie dasselbe JSON erneut dekodiert und vollständig validiert. | Bis8 unveränderliche Stilsnapshots nach exakten gespeicherten Rohwerten, Legacy-Stil und Nachtzustand. Alle6 Linienstile einmal je Variante aufbereiten. Direkte Edits, Profilwechsel, Restore und beschädigte Daten bleiben korrekt. Keine Context-/View-Referenzen im Cache. |
| `VtmBrightTheme` / `BrightFilter` / `MapRoadSurface` | Bright-JSON und identische Filter bei jedem Grund-/Vordergrundpass neu lesen/übersetzen; feste Mengen teilweise für jedes Kartenobjekt neu erzeugen. | Unveränderliche Filter einmal pro Prozess vorbereiten; feste Prüfsets wiederverwenden. Native Styles, Farben, Sichtbarkeit, Textprioritäten/Cache und GL-Ressourcen bleiben je Theme getrennt. Kein gemeinsam mutierbarer nativer Stil. |
| `MapPanel.refreshRouteButton` / feste Trackzuordnungen | Für die Ja/Nein-Frage „ist eine Strecke sichtbar?“ wurde eine Liste und ein Set erzeugt; feste Track-/Cycling-Listen wiederholt angelegt. | Direkte `any`-Prüfung und konstante Zuordnungen. Gleiche Sichtbarkeit, weniger kurzlebige Objekte bei häufigen Updates. |
| `RideAnnotations` | Abgeschlossene Autosave-Futures blieben pro bearbeiteter Fahrt in `pending`; aktueller Notizwert wurde bei jeder Änderung zweimal gelesen/dekodiert. | Erledigte Aufträge im finally entfernen, aber nur wenn sie noch der aktuelle Auftrag sind. Neuere Aufträge nicht versehentlich entfernen. Notizwert einmal lesen. Persistenz/Dirtymarker/Retry bei Fehlern erhalten. |
| `RideNames.annotated` | Notizen ohne irgendeine verknüpfte Cloud-Aktivität schrieben und planten trotzdem die Cloud-Warteschlange neu. | Ohne Links sofort zurückkehren. Spätere Verknüpfung liest das lokal vorhandene Feedback weiterhin in `confirmed`. Manuelle Fahrtnamen speichern bewusst auch vor der ersten Verknüpfung; deren Verhalten bleibt erhalten. |

Bereits vorher korrigiert: Radwege-/Radrouten-Breite und Ein/Aus in Einstellungen bauen die verdeckte Karte nicht mehr pro Schritt neu auf. [Details](cycling-map-options.md).

## Nächste sinnvolle Schritte, geordnet

| Priorität | Fundstelle | Konkreter Folgeschritt | Aufwand / Risiko |
| --- | --- | --- | --- |
|1| `MainActivity` DisplayUpdateGate, Bedingung `forceDashboard || !saving || dialogs.empty` | Dashboard-Aufbereitung im Normalmodus hinter **vollständig deckenden** Seiten aussetzen. Rückkehr einmal frisch zeichnen. Sichtbare Sensor-/Strecken-/Statusseiten weiter versorgen; kompakte Dialoge mit sichtbarem Dashboard unterscheiden. | Mittel. Nicht nur `dialogs.empty` als frühes Return setzen: Aufnahmeende, Zusammenfassung, Warnungen und Profilwechsel laufen teilweise über denselben Renderpfad. |
|2| `RideTitleView.restart` + `TextScrollerMotion` | Lange Titel nur während Bewegung/Überblendung animieren; statische4-s-Anfangs-/Endhalte und2-s-Leerphase mit einem Termin überbrücken. | Klein bis mittel. Zeitablauf, Sichtbarkeit und Systemeinstellung für Animationen erhalten. Kein Messlauf nötig; deterministische Phasentests reichen. |
|3| `VtmCycleTheme.getRenderThemeAsStream` + `VtmBrightTheme.load` | Unveränderliches365457-Byte-Basis-XML und statische Vorverarbeitung wiederverwenden. Danach reine Textaufbereitung, soweit mit VTM-Lebenszyklus vereinbar, vom UI-Thread lösen. | Mittel. Native Themes/GPU-Handles niemals global teilen; Farb-/Font-/Geometrieänderungen sowie getrennte Maps beachten. Original-Bright-JSON66314Bytes bereits nicht mehr wiederholt gelesen/übersetzt. |
|4| `BikeProfileStore.all/current` → `legacy` → `RecordingConfig.decode` | Unveränderte aktive Legacy-/Quellenkonfiguration nach ihren eigenen Eingabewerten wiederverwenden. | Mittel. Vorhandener Cache der Profilliste reicht für diese Zusatzdekodierung nicht. Sämtliche Aufnahmequellen und Profil-/Restore-Änderungen benötigen gezielte Regressionen; keine Sensoränderung. |
|5| `MainActivity.render` Aufbau `graphDemand` | Gewünschte Diagramme nach unverändertem GraphOverlayConfig/Template wiederverwenden. | Klein. Zentraler Provider kopiert identische Nachfrage bereits nicht mehr. Profil-/Dashboard-/Fensterwechsel vollständig berücksichtigen. |

Diese Punkte sind in TODO.md aufgenommen. Reihenfolge aus der Häufigkeit/Art der nachgewiesenen Arbeit abgeleitet, keine gemessene Rangliste und keine Voraussetzung für eine Messkampagne. Hier keine großflächige Änderung des Bildschirm-/GL-Lebenszyklus vorgenommen.

## Bereits zweckmäßig umgesetzt

- MapState hält Streckengeometrien/Statistiken auf Worker-/Cachepfaden; RideStatisticsView ignoriert bereits dieselbe Ergebnisinstanz. `mapSlots` liegt in MapState schon als Snapshot vor; kein zusätzlicher Slots-JSON-Cache nötig.
- RideProfileCatalogCache und BikeProfileStore.parseStored haben bereits explizite Cacheeingänge; kein blindes Duplizieren dieser Caches.
- GraphHistoryBuffer begrenzt Historie und erhält Spitzen/Datenlücken; Aufzeichnungsdaten unabhängig vom Zeichenworker. RecordingStore verarbeitet die Dateiarbeit seriell. Die Änderung betrifft nur Graph-Frames.
- HRV-Mess-Tick läuft nur bei aktiver Messung; Rohdaten und Auswertung auf eigenem begrenztem Worker. Kein dauerhaft laufender250-ms-Leerlauf-Timer gefunden.
- Fotoimport/-dekodierung und Trainingsimport laufen auf Workern. BikePhotoView verwirft überholte Antworten; IntervalsFitnessView beendet seinen Executor bei Detach. Keine wiederholte Fotodekodierung bei jedem Sensor-Refresh in BikesView gefunden.
- Theme-Bindings melden sich bei Detach ab. Keine pauschale Listener-Leak-Behauptung aus bloßem Registrieren.
- Karte reagiert bereits auf geänderte Position-/Trackrevisionen; GPS-/Trackgeometrien bewegen sich im selben GL-Frame. Gemeinsame EGL-Ressourcen-/GPU-Abgrenzung nach den jüngsten Zeichenfehlern unverändert.
- Release nutzt bereits R8-Minifizierung und Ressourcen-Shrinking. Testbibliotheken bleiben testImplementation; keine wahllose Entfernung benötigter VTM-/FIT-/WebDAV-/Backup-Bibliotheken oder Lizenzen.
- Neue Höhendaten-Caches begrenzt, Schummerung ohne persistente Offline-Kacheln. Keine zusätzlichen Downloads als „Optimierung“.

## Gezielt geprüft

Keine Gesamtsuite, keine zusätzliche Profiling-/Benchmark-Infrastruktur und keine APK.

- Stilcache/Profilkopie/Sicherung/Legacy-/Nacht-/Rohwertänderungen, Vorschau/Palette; Diagrammgeometrie/Endzustand und einmaliger Pfadaufbau; bestehende verzögerte Cycling-Übernahme: `/workspace/.tools/efficiency-review-tests.log`.
- Native Bright-Filter und Profilsicherung, Belag/Dashes, unabhängige Live-Farbstile/Reset: `/workspace/.tools/efficiency-map-rules-tests.log`.
- Notiz-/RPE-/FIT-/GPX-/Cloudverhalten, Freigabe erledigter Aufträge und keine Cloudqueue-Writes für lokale Notizen; vorhandene Namens-/Kontowechselregeln: `/workspace/.tools/efficiency-feedback-tests.log`.
- Unsichtbare Graphen in beiden Modi, lückenlose erhaltene Historie und sofortige Rückkehr, unveränderte FIT-/Journalbytes sowie bisheriger sichtbarer1-Hz-Rollingprovider: `/workspace/.tools/efficiency-hidden-graphs-tests.log`.
- Zentrale Typografie-/UI-Katalog-/Bright-Generator-/Diff-Prüfung.

Tests zählen notwendige Aufbereitungen bzw. vergleichen Ergebnisse; das sind deterministische Funktionsregressionen, keine Laufzeit- oder Akku-Messungen. Visuelle Darstellungen/Bediengefühl auf realem Gerät bleiben die praktische Abschlusskontrolle.
