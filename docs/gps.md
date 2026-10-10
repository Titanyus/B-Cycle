# GPS-Fahrtwerte ab 0.4.0

Native `LocationManager.GPS_PROVIDER`, ungefähr eine Messung je Sekunde. Kein
Google-Play-Services-SDK, kein Netzwerkstandort, keine Internetberechtigung.
Android fragt bei Auswahl eines GPS-Felds oder beim Aktivieren im Menü nach
präzisem Standort während der App-Nutzung. Bluetooth funktioniert ohne GPS-Freigabe.

Außerhalb einer gestarteten Fahrt läuft die Messung bei sichtbarer Activity.
Nach START hält der Aufzeichnungsdienst GPS auch bei ausgeschaltetem Display
und beim Wechsel in andere Apps aktiv. STOPP beendet und speichert die Fahrt.
START setzt die Fahrtzähler zurück. GPS-Positionen werden in FIT gespeichert,
wenn „GPS-Route / Position“ im Aufzeichnungsmanager ausgewählt ist.
Es gibt keinen GPX-Export; Details: [FIT-Aufzeichnung](fit.md).

## Quellen und Zeiten

GPS und E-Bike haben getrennte Geschwindigkeits- und Distanzfelder; kein Sensorwert
ersetzt stillschweigend einen anderen. E-Bike-Fahrdistanz summiert positive
Differenzen des empfangenen Bosch-Kilometerstands. Wiederholte Teilupdates zählen
keine zusätzliche Strecke. Nach einer Unterbrechung beginnt eine neue Referenz;
der Gesamtkilometerstand bleibt ein separater Wert. Ein Radwechsel setzt nur den
E-Bike-Fahrtzähler zurück. START setzt beide Quellen zurück.

- **Bewegungsdurchschnitt:** GPS-Strecke / bestätigte GPS-Bewegungszeit.
- **Gesamtdurchschnitt:** GPS-Strecke / aktive GPS-Fahrtdauer, einschließlich
  Stillstand und Empfangswarten nach der ersten brauchbaren Position.
- **Standzeit:** Intervalle mit bestätigtem Stillstand. Empfangslücken sind keine
  bestätigte Standzeit und erscheinen deshalb nicht automatisch darin.
- **Fahrtdauer:** aktive GPS-Zeit ab erster brauchbarer Position, inklusive
  Empfangswarten. Bei gestarteter Fahrt zählt auch Offscreen-Zeit; manuelles GPS-Trennen zählt nicht.
- **Maximaltempo:** Maximum der gefilterten Geschwindigkeit aus zusammenhängenden
  Messungen. Eine einzelne erste Position erzeugt noch kein Maximum.

Es handelt sich um geschätzte Intervalle zwischen Positionen. Die Aufzeichnung
im Hintergrund beginnt ausschließlich durch START. Bei kurzen Aussetzern können GPS und Radstrecke
abweichen. Beide Distanzen beginnen beim jeweils ersten brauchbaren Quellenwert.

## Filter und Grenzen

Die unabhängige Kotlin-Verarbeitung verwendet monotone `elapsedRealtime`-Zeit:

1. Nur endliche Koordinaten und horizontale Genauigkeit bis 20 m. Keine doppelten,
   rückwärts laufenden, zukünftigen oder mehr als fünf Sekunden alten Messungen.
2. Positionssprünge über `35 m/s × Zeitabstand + Genauigkeit + 5 m` werden verworfen.
   Nach schlechten Messungen wird neu verankert. Bei mehr als zehn Sekunden Abstand
   wird keine Strecke zwischen den Punkten ergänzt. Live-Speed/Höhe/Steigung sind
   nach fünf Sekunden ohne brauchbare Messung nicht mehr sichtbar.
3. GNSS-Geschwindigkeit wird bevorzugt: 0–35 m/s, gemeldete Speed-Ungenauigkeit
   höchstens 1,5 m/s; falls nicht mitgeliefert, greifen die anderen Prüfungen.
   Unplausible Geschwindigkeitswechsel über `max(4 m/s, 6 m/s² × Zeitabstand)`
   fallen auf den Positionsweg zurück. Exponentielle Glättung, Zeitkonstante 1,5 s.
4. Unter 0,7 m/s, etwa 2,5 km/h, gilt Stillstand. Stillstandspositionen addieren
   keine Distanz. Bewegte Punkte zählen ab `max(2,5 m, 0,35 × Summe der Genauigkeiten)`
   zum letzten Distanzanker. Dadurch können sehr langsame Fahrt und enge Kurven
   unterschätzt werden. Auch brauchbare Genauigkeitsangaben garantieren keine
   fehlerfreie Position in Häuserschluchten oder unter dichtem Wald.
5. Ohne brauchbaren GNSS-Speed wird Geschwindigkeit erst aus einer Verschiebung
   über `max(4 m, Summe der Genauigkeiten)` und der zugehörigen Zeit abgeleitet.
   Zwischen diesen Schritten bleibt der gefilterte Wert erhalten; nach fünf Sekunden
   ohne ausreichend große Verschiebung wird Stillstand angenommen. Dieser Ersatzweg
   reagiert langsamer und kann die ersten Bewegungssekunden unterschätzen.
6. Höhe nur mit gemeldeter vertikaler Genauigkeit bis 12 m. Bevorzugt von Android
   gelieferte Meereshöhe (MSL, ab API 34), sonst GPS-Ellipsoidhöhe; die Feldbeschriftung
   nennt den Bezug. Es gibt keinen geratenen pauschalen Geoid-Offset.
7. Höhenfilter: Median der letzten fünf Höhen und exponentielle Glättung mit 6 s
   Zeitkonstante. Auf-/Abstieg erst bei `max(3 m, halbe vertikale Genauigkeit)`
   Änderung zur Höhenreferenz, während Bewegung. Stillstand, Empfangslücken und
   ein Wechsel des Höhenbezugs setzen die Referenz, keinen Höhenzuwachs.
8. Steigung aus geglätteter Höhendifferenz und etwa 30–40 m Strecke; erst ab 30 m,
   im Stand unbekannt. Werte außerhalb ±50 % werden nicht angezeigt. Die Anzeige
   reagiert verzögert, kurze Rampen können geglättet werden.

Diese Startwerte sind konservative Filter, keine Vermessung. GPS-Höhe, Höhenmeter
und Steigung brauchen einen Vergleich auf einer echten Fahrt. Für genauere Höhen
könnte später ein vorhandenes Barometer mit GPS-Korrektur ergänzt werden.

Nur ein Positionsanker, die letzte Messung, fünf Höhen und höchstens 100 Paare
aus Wegstrecke/Höhe liegen für die GPS-Berechnung im Speicher. Während einer
Aufzeichnung liegen ausgewählte Positionen im privaten Journal und später in FIT,
aber nicht im Diagnosebericht. Der Bericht enthält Zähler und aggregierte
Fahrtwerte ausschließlich nach ausdrücklich gewähltem Export.
