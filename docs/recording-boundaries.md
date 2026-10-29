# Aufnahmegrenzen für App-Fahrtzähler

Vor START ist die Fahrtdauer 0. GPS darf Position, Geschwindigkeit, Höhe und
Fahrtrichtung für die Live-Anzeige liefern, sammelt aber noch keine Fahrtstrecke,
Bewegungs-/Standzeit, Höchstgeschwindigkeit oder Höhenmeter.

RideRuntime aktiviert GpsRide.collecting erst beim Aufnahmestart und deaktiviert
es nach der letzten Abschlussprobe. Die Fahrtdauer stammt immer aus der
Aufnahmeuhr, auch vor der ersten Aufnahme und ohne GPS. Nach STOPP bleiben die
Fahrtzähler eingefroren, bis START sie für die nächste Fahrt zurücksetzt.

Der GPS-Filter verwendet seine räumliche Kontinuität weiterhin für Live-Geschwindigkeit
und Steigung; diese internen Filteranker sind keine zusätzliche Aufzeichnung.
Aktuelle Position, Höhe und Geschwindigkeit bleiben unabhängig von der Aufnahme.

App-eigene Kadenz-/Pulsdurchschnitte, maximaler Puls und die Fahrtstrecke aus dem
Rad-Kilometerzähler werden ausschließlich während der Aufnahme aktualisiert.
Rohwerte vom Sensor bleiben erhalten. Vom Bosch-Gerät selbst gelieferte Kennzahlen
werden dadurch nicht umgerechnet oder inhaltlich verändert.

FIT-Journal, Karten-Fahrttrack, Graphhistorie und Performance Engine hatten bereits
eine Aufnahmebedingung und verwenden weiterhin denselben Aufnahme-/Abschlussweg.
Keine neuen Hintergrundtimer, keine zweite Sensorverbindung oder Aufzeichnung.

Verifikation: 43 gezielte Tests bestanden, inklusive realem Runtime-/Service-/FIT-
Abschlussweg auf API 33/35/36, GPS-Preview vor dem ersten START, Stillhalten aller
GPS-Tripwerte nach STOPP, Kadenz-/Puls-/Radstrecken-Grenzen und Karten-Livebetrieb.
Keine neue APK gebaut; der Fix ist eine Quellcodeänderung nach Version 0.7.15.

## FIT-Listenmetadaten

FitRead nutzt für fehlende Standard-Distanz/Geschwindigkeit dieselben eigenen Developer-Field-Fallbacks wie RouteAnalysisRead. Die gemeinsame Prüfung FitDeveloperValues akzeptiert nur definierte skalare Felder unserer App-UUID mit passender Einheit und endlichen nichtnegativen Werten. Der Bibliotheksindex erhält detailsVersion=1; ältere Indexeinträge werden im vorhandenen Worker einmalig neu gelesen, einschließlich GPS-Vorschau und hasRoute. Dateiname, Benutzertitel, ursprüngliches Listendatum, interne ID und FIT-Original bleiben erhalten.

Ohne aufgezeichnete GPS-Koordinaten kann keine echte Kartenstrecke rekonstruiert werden. Vorhandene Distanz- oder Geschwindigkeitssensoren sind kein Ersatz für Positionsdaten.

## Private Vollauswertung und begrenzter FIT-Export (20.10.2026)

Das BCY7-Journal bewahrt verfügbare Rohwerte während der Aufnahme unabhängig von der FIT-Feldauswahl. Die Auswahl wird gesondert erhalten und im normalen FitWriter vollständig angewandt, einschließlich Entwicklerfeldern und GPS. Private vollständige Auswertungsquelle und reduzierter Export sind getrennt. Die bestehenden START/STOPP-, Sensor- und GPS-Grenzen bleiben erhalten. Details: [Private Auswertung](dreeve-workflow-and-private-analysis.md). Ältere bereits verworfene Daten bleiben verloren.


## Eine Fahrt bis zum manuellen Stoppen

Ausdrückliche Nutzerregel29.10.2026: Abschalten/automatisches Einschlafen des Fahrrads oder eine längere Pause beenden und speichern keine laufende Aufnahme. Wiederverbindung liefert weitere Messwerte in dieselbe Sitzung, ohne Uhr/Journal/ID zurückzusetzen. Gesamtdauer umfasst die Pause; Bewegungszeit wächst nur aus gültigen Bewegungswerten. Fehlende Sensorwerte bleiben unbekannt, keine erfundenen Nullen oder Messwertfortschreibung über die bestehenden Grenzen.

Bereits im Produktcode vorhanden: active=visible||recording hält Runtime-Ticks und Wiederverbindungsregeln auch ohne sichtbare Activity aktiv. Die Aufnahme setzt connectionsRequested; bekannte und aktivierte Sensoren werden bei ungeplanter Trennung erneut verbunden (Versuche3s bis30s). Manuell in der App getrennte Sensoren bleiben absichtlich ausgeschlossen. RecordingService arbeitet als Vordergrunddienst mit PARTIAL_WAKE_LOCK, GPS läuft bei aktivierter Auswahl weiter. Rad-Aus ist kein STOP-Trigger. Erneutes START bei laufender Aufnahme ist wirkungslos.

Neuer Integrationstest longBikeShutdownWithScreenOffKeepsOneRideUntilManualStop:45min simulierte Trennung ohne sichtbare Activity, weiterhin Aufnahme/Foregroundservice/Retrybereitschaft, fehlende Wattwerte statt Nullen, neue Werte nach simuliertemReconnect, unverändertes Journal bei erneutemSTART und genau eine Sitzung/FIT nach manuellemSTOP. Zusammen mit bestehendem Hintergrund-/GPS-Test und AutoReconnectTest9Fälle grün (beide Integrationsfälle aufAPI33/35/36, Policy3Fälle). Log/workspace/.tools/recording-long-bike-pause-tests.log. KeineProduktionsänderung nötig, kein physischerLangzeittest amRad.

Betriebssystemseitiges Beenden/Force-Stop/Handyneustart ist kein bloßes Bildschirm-Aus und wird durch diese Regel nicht als garantiert unterbrechungsfrei fortsetzbar behauptet. Unabhängige Einschränkung: ohne explizite Pauseereignisse zählt unbekannte Leistung während Rad-Aus aktuell zur NP-Datenabdeckung; lange Pausen können NP trotz erhaltener Gesamtsitzung sperren. Die NP-/Pausenpolitik wurde hier nicht verändert.
