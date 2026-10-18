# Pulsmodelle ab 0.8.2

Unter Fahrer → Trainingsparameter steht Methode mit Erklärung und drei Möglichkeiten. Das globale Fahrerprofil bleibt unabhängig vom Fahrradprofil.

| Methode | Erforderliche Eingaben | Fünf Zonen |
| --- | --- | --- |
| Karvonen (Standard) | Ruhe- und Maximalpuls | 50–60–70–80–90–100 % Herzfrequenzreserve, jeweils plus Ruhepuls |
| Maximalpuls | Maximalpuls | 50–60–70–80–90–100 % Maximalpuls |
| Schwellenpuls · Friel (Rad) | Schwellenpuls | unter 81 %, 81–90 %, 90–94 %, 94–100 %, ab 100 % Schwellenpuls |

Bei Friel werden 5a–5c zur fünften Hauptzone zusammengefasst. Schwellenpuls ist ein selbst ermittelter Trainingsparameter, keine automatische Schätzung aus einer Fahrt. Sein gültiger Eingabebereich beträgt 60–250 bpm. Die technische Sensorobergrenze von 250 bpm begrenzt gültige Messwerte, nicht die Diagrammachse.

Nur die vom Modell benötigten Pulsfelder sind aktiv. Inaktive Werte bleiben gespeichert und werden beim Zurückwechseln wiederverwendet. FTP ist unabhängig und immer aktiv. Ungültige Entwürfe überschreiben keine gültigen Werte. Die Beziehung Ruhepuls < Maximalpuls ist nur für Karvonen erforderlich. Es gibt keine Altersformel, die einen persönlichen Maximalpuls ungefragt ersetzt.

Ganzzahlige Zonengrenzen werden aufgerundet; Intervalle sind unten inklusive und oben exklusiv. Karvonen und Maximalpuls schließen den Maximalpuls in Z5 ein. Friel zeigt für Z1 „unter“ und für Z5 „ab“. Kontinuierliche Mittelwerte werden an den ungerundeten mathematischen Grenzen eingeordnet.

HeartRateZones ist die gemeinsame Berechnung für Vorschau, Dashboard, Trainingshilfe, Zonenanteile und Verteilung. Zone-2-Hilfe verwendet die modellabhängigen Grenzen: 60–70 % bei Karvonen/Maximalpuls beziehungsweise 81–90 % bei Friel. Farbblenden folgen diesen Grenzen; bei schmalen benachbarten Bereichen wird ihre Breite begrenzt, damit keine Sprünge durch überlappende Blenden entstehen. Die eigentliche Zonenzuordnung bleibt exakt. HF-Reserve (%) behält ausdrücklich ihre bisherige Bedeutung aus Ruhe- und Maximalpuls.

RiderProfile speichert heartRateMethod und thresholdHeartRate optional im vorhandenen riderProfile.v1. Alte Profile ohne Methode verwenden Karvonen. Einstellungssicherung erhält beide Angaben; bestehende Sicherungen bleiben lesbar. Keine FIT-, BLE- oder Performance-Engine-Änderung.

Die lokale Hilfe erklärt Formeln, Beispiele, erforderliche Eingaben und Grenzen ausführlicher unter Trainingsparameter. Ihre Sticky-Accordions entsprechen den Hauptabschnitten der Ursprungsseite, darunter stehen normale Unterüberschriften mit Erklärungen.
