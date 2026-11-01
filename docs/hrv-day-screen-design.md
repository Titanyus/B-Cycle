# HRV-Tagesscreen: Nutzermockup – 01.11.2026

Gestaltungsvorgabe erhalten, aktuell Plausibilitäts-/Vollständigkeitsbesprechung. Keine offenen Indexformeln stillschweigend festlegen. Die Darstellung wurde nachfolgend präzisiert; Berechnung und bestehende Tagesempfehlung erhalten. Die vorgemerkte gemeinsame Tages-/Detailauswertung und danach die Dashboard-Gesten bleiben Teil der Aufgabenfolge.

## Zweigeteilter Kreis

- Form des Messkreises aufgreifen; oberer Bogen HRV, unterer Bogen Ruhepuls.
- Je150° Bogen: gegenüber dem vollen Halbkreis beide Enden um15° gekürzt. Keine vollen Halbkreise.
- Ruhepuls-Strichstärke wie bestehender Messkreis, HRV doppelte Strichstärke.
- Kurzer horizontaler Trenner mittig zwischen beiden Bereichen, bewusst innerhalb des Kreises.
- Große Werte etwa20% kleiner als der Pulswert auf dem Messbildschirm. Jeweils vertikal im eigenen Halbkreis zentrieren; bestehende Wertschrift verwenden.
- Auf Höhe der Bogenabschlüsse jeweils zentrierte Beschriftung „Herzfrequenzvariabilität“ oben bzw. „Ruhepuls“ unten, normale Roboto-Textrolle.
- Bestehenden persönlichen Normbereich je Messgröße unverändert übernehmen. Mittlere50% des Bogenwegs für den Normbereich, davor/danach je25% Puffer; Marker entsprechend bei einem Viertel und drei Vierteln, radial auf den Kreismittelpunkt ausgerichtet.
- Markierungen2dp FOREGROUND, beide Enden je3dp über die Bogenstrichstärke hinaus. Kleine Grenzwerte in Verlängerung der Linien.
- HRV-Farben und Funktionalität aus der bisherigen Auswertung übernehmen. Nutzer stellt ausdrücklich klar: Allein ein HRV-Wert oberhalb des Korridors wird vorerst nicht orange/gelb oder rot gedeutet. Keine neue beidseitige Warnregel oder neue Schwelle für „deutlich außerhalb“ erfinden. Die vorhandene Tagesempfehlung darf weiterhin bestehende Kriterien wie Befinden berücksichtigen.
- Ruhepulswert/Bogen ausschließlich weiß laut Nutzervorgabe; Grenzen als echte bpm-Werte, nicht die im Mockup wiederholten HRV-Beispielzahlen6,3/7,5 übernehmen.

## Gemeinsamer ruhender Glow

Zusätzliche ausdrückliche Nutzervorgabe: Den pulsierenden Glow innerhalb des Messkreises auch im Tagesscreen verwenden, dort dauerhaft und ohne Pulsieren. Dessen Maximalwert/Spitzenintensität aus dem Messscreen übernehmen, keine neue stärkere Intensität erfinden. Farbe entsprechend der HRV-Auswertung (bei gutem Wert leichtes Grün). Der Glow liegt gemeinsam im gesamten Kreis hinter beiden Bereichen; keine getrennte Einfärbung oder Intensität für HRV- und Pulshälfte. Ruhepulswert/-bogen weiterhin weiß. Bereits bestehende funktionale HRV-Glow-Darstellung in Flat/Glow berücksichtigen. Die präzisierte bisherige HRV-Einordnung verwenden, keine Warnfarbe allein wegen hohen HRV-Werts oberhalb des Korridors.

## PNS-/SNS-Balken

- Unter dem Kreis nach Mockup zuerstPNS, dannSNS; je links Indexname, rechts Zahlenwert.
- Ähnliche horizontale Bahn wie bisheriger HRV-Bereichsbalken, Dicke wie Messkreis/Ruhepulsbogen.
- Null liegt in der Mitte. Füllung von dort je nach Vorzeichen nach links oder rechts.
- Füllung am Nullpunkt gerade abschneiden, nur das äußere linke/rechte Ende abrunden.
- Mittige FOREGROUND-Linie steht3dp über; darunter kleine„0“.
- Skalierung vom Nutzer präzisiert: Auch hier mittlere50% für den persönlichen Normbereich, links/rechts je25% Puffer;0 bleibt in der Mitte. Als nachvollziehbare normierte Darstellung bietet sich Normalbereich−1…+1 innerhalb einer sichtbaren Skala−2…+2 an. Konkrete eigene Indexformel/Referenz weiterhin offen; diese Darstellungsdefinition nicht als bereits implementierte oder validierte Berechnung ausgeben. Werte außerhalb der sichtbaren Skala nicht rechnerisch begrenzen, tatsächlichen Zahlenwert erhalten. PNS/SNS benötigen begründete semantische Farben; siehe hrv-autonomic-index-proposal.md.

## Empfehlung und Ablauf

- Vorhandenen Empfehlungstext unter den Anzeigen frei auf der Seite darstellen, ohne Kasten, mit24dp Abstand.
- Textbutton„Zur genauen Auswertung“ bleibt ausdrücklich bestehen; er war nur im Mockup abgeschnitten. Präzisiertes Sticky-Handoff aus hrv-measurement-flow-revision.md erhalten.
- Datum/Navigation aus der bisherigen Tagesansicht berücksichtigen, historische Tagesauswahl muss erkennbar bleiben.
- Fehlende Werte/Referenzen neutral kenntlich machen, keine scheinbaren Null-/Normalwerte erfinden. Keine neue Alterskorrektur oder Kubios-Gleichheit behaupten.

## Bestätigung des Nutzers

HRV- und Puls-Grenzwerte sind genau die bereits aus den bestehenden Korridoren berechneten Werte. Nur die Darstellung ändern, keine neue Referenz-/Grenzwertberechnung, Trainingsempfehlung oder Annahmeregel einführen. Die Beispieldaten im Mockup sind keine neuen festen Grenzwerte.
