# Übernahme des HRV-Prototyps

Geprüft am 20.10.2026: Titanyus/B-Cycle-HRV, main,
`895de63a98828cc7a7ece813c618e2eb48321daa` (Testapp 0.3.2).
Umgesetzt ab 0.8.16: siehe [HRV-Messung](hrv-measurement.md).
Die folgenden Abschnitte halten den ursprünglichen Entwurf fest; die aktuelle
Abdeckungswarnung und der tatsächlich implementierte Umfang stehen im Umsetzungsdokument.

## Bestehende Basis

`hrv-core` ist ein reiner Kotlin/JVM-Kern mit JVM-Target 17, ohne zusätzliche
Produktivabhängigkeiten. Wiederverwendbar sind HeartRatePacket, MeasurementSession,
Metrics und ArtifactCorrection samt Tests, Referenzfixtures und Lizenzhinweisen.
Die Testapp verifiziert 30 Sekunden Ruhephase und 180 Sekunden Messung. Der Kern
liefert RMSSD, LnRMSSD, SDNN, pNN50 und mittleren Ruhepuls sowie getrennte rohe und
korrigierte Ergebnisse mit Qualitätsgründen. Baselines, LF/HF und Cloudfunktionen
sind nicht Teil dieses implementierten Funktionsumfangs.

B-Cycle besitzt bereits genau eine HeartRateConnection und einen Decoder für mehrere
RR-Werte im Standard-HR-Paket. RideRuntime verarbeitet derzeit bpm, aber nicht die
RR-Intervalle. `rrSeconds` allein reicht für die Übernahme nicht: Null-RR werden
entfernt, Kontaktzustand ist nicht explizit erhalten, Empfangszeit fehlt.

## Empfohlene erste Umsetzung: eigenständige Ruhemessung

1. Den geprüften hrv-core-Stand als lokales JVM-Modul übernehmen. Herkunftscommit,
   Referenzfixtures, Tests und Drittanbieterhinweise erhalten; kein Netzwerkabruf
   von main beim Appstart oder Build. Algorithmus nicht nebenbei verändern.
2. An der bestehenden GATT-Notification ein unveränderliches Empfangspaket ergänzen:
   Originalbytes, monotone Empfangszeit vor Handler-Dispatch, Sensor und
   Verbindungsgeneration. Die bisherige Pulsanzeige unverändert weiter bedienen.
   Die HRV-Sitzung erhält die ursprünglichen Pakete für ihren getesteten Decoder;
   kein zweiter GATT-Client, kein zweiter Scanner oder Reconnect-Mechanismus.
3. Einen begrenzten seriellen HRV-Worker für Messablauf, Journal und Auswertung
   anbinden. Empfangszeit nicht durch UI-Verzögerung ersetzen. Quellenwechsel,
   Kontaktverlust, fehlende RR und Verbindungsabbruch explizit behandeln.
4. Eigene native HRV-Messansicht mit bestehenden Design-/Dialogkomponenten:
   Start, 30 s Ruhephase, 180 s Messung, Fortschritt, Abbruch und Ergebnis.
   Während der Ruhemessung keine Fahrt starten, keinen Sensor/Profilwechsel
   vornehmen und keine RR-Fortsetzung über Verbindungsabbrüche hinweg erfinden.
   Kurze Qualitätsmeldung bei unbrauchbarer Messung statt freigegebenem Messwert.
5. Messungen unabhängig von Fahrrädern und Fahrten privat speichern, einschließlich
   Roh-RR, korrigierter Folge, Messprotokoll, Qualität und Algorithmusversion.
   MeasurementStore ist eine brauchbare Vorlage; Testapp-/BleTrace-Abhängigkeiten
   an B-Cycles Lebenszyklus und Speicher anpassen. Nicht in RideJournal/FIT-Felder
   oder Uploadwarteschlangen für Fahrten einbauen.
6. RR-TXT-Export und vollständigen Diagnoseexport bewusst anbieten. Die bestehende
   Fahrten-/Einstellungssicherung deckt neue HRV-Messdateien nicht automatisch ab:
   HRV-Sicherungs-/Wiederherstellungsumfang ausdrücklich ergänzen, bevor eine
   vollständige Gesamtsicherung behauptet wird. IN-/OUT-Texte mitpflegen.

Der Kern benötigt echte RR-Ausgabe des Gurts. Puls in bpm allein kann fehlende
RR-Intervalle nicht rekonstruieren. Ruhewerte und eine spätere Auswertung während
Belastung sind unterschiedliche Messprotokolle; der bestehende Ruhetest darf
nicht unverändert als täglicher Erholungswert aus einer Fahrt verwendet werden.

## Abnahme

Vorhandene Kern-/Referenztests übernehmen. Hosttests prüfen unveränderte Pulsanzeige,
mehrere RR pro Paket, präzise Empfangszeit, Kontaktverlust, Null-RR, Datenlücken,
Appwechsel, Abbruch und sichere Wiederherstellung nach Prozessende. Kein Start oder
Verändern von Fahrtzählern/FIT-Aufnahmen durch die HRV-Messung. Danach echte Gurt-/
Telefonprüfung mit demselben Messprotokoll wie im Prototyp. Eine neue APK erst bei
konkretem Buildauftrag, über GitHub Actions.

Anschließend getrennt ausbauen: persönliche Messhistorie und Referenzbereich,
Trainingskontext, optional intervals.icu-Wellness. Diese Teile sind im Testprojekt
überwiegend Architekturentwürfe und werden nicht als bereits implementiert übernommen.

## Vorgemerkter Messbildschirm – Nutzervorgabe vom 20.10.2026

Die bisherige spartanische Datenausgabe des Prototyps dient Testzwecken. Für die
spätere Integration ist folgender Messbildschirm vorgesehen; diese Vorgabe ist
festgehalten, noch nicht als Oberfläche implementiert.

- Oben ein großer kreisförmiger Zeitindikator. Der Kreis visualisiert den Fortschritt
  der Messung und die bereits verstrichene bzw. noch verbleibende Messzeit.
- In der Mitte der aktuelle Puls, groß und gut ablesbar, mit Kennzeichnung bpm.
  Darunter die Messzeit im Format mm:ss.
- Unter Puls und Zeit eine eindeutig erreichbare Aktion **Abbruch**, wie in der
  Referenz unter den zentralen Werten. Sie beendet die laufende Messung.
- Im unteren Bildschirmbereich ein Live-Liniengraph der Zeit zwischen zwei
  aufeinanderfolgenden Herzschlägen, beschriftet **RR (ms)**. Horizontale Achse:
  Messverlauf/Zeit; vertikale Achse: RR-Intervall in Millisekunden. Jeder Wert stammt
  aus tatsächlich empfangenen RR-Intervallen, nicht aus 60.000 / angezeigtem Puls.
  Datenlücken dürfen nicht durch erfundene Werte oder durchgehende Linien verdeckt
  werden. Mehrere RR-Intervalle pro Sensorpaket bleiben einzelne Messwerte.

Die vom Nutzer im Chat gezeigte READINESS-Ansicht ist die visuelle Referenz für
Aufteilung und Hierarchie: Zeitkreis oben, zentrale Messwerte und Abbruch, RR-Graph
unten. Deren Produktname, blaue Farbe und sonstige Gestaltung werden nicht als
B-Cycle-Vorgaben übernommen. B-Cycles vorhandene Themefarben, Typografie, Icons,
Header-/Zurücknavigation und Komponenten verwenden. Die Messansicht bleibt im
Hochformat. Technische Rohdaten-/Diagnoseausgaben gehören nicht in die zentrale
Messdarstellung.

Farbvorgabe des Nutzers: Die farbige Darstellung, insbesondere Fortschrittsbogen
und RR-Graph, verwendet ausschließlich die vorhandenen Farbtokens des aktuell
gewählten B-Cycle-Themes. Keine fest codierte blaue Referenzfarbe und keine eigene
HRV-Farbpalette. Ein Themewechsel aktualisiert auch diese Elemente unmittelbar.
