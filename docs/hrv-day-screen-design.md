# HRV-Tagesscreen: Nutzermockup – 01.11.2026

Am01.11.2026 umgesetzt. Bestehende HRV-/Puls-Korridore und Tagesempfehlung erhalten. Eigene persönliche Indizes ausdrücklich bestätigt: PNS aus lnRMSSD, zweiter Balken als Aktivierungsindex aus Ruhepuls; Details in hrv-autonomic-index-proposal.md. Gemeinsame Tages-/Verlaufsauswertung umgesetzt, Dashboard-Gesten danach als eigener Änderungsschritt.

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

## PNS-/Aktivierungsbalken

- Unter dem Kreis nach Mockup zuerstPNS, dannAktivierung; je links Indexname, rechts Zahlenwert.
- Ähnliche horizontale Bahn wie bisheriger HRV-Bereichsbalken, Dicke wie Messkreis/Ruhepulsbogen.
- Null liegt in der Mitte. Füllung von dort je nach Vorzeichen nach links oder rechts.
- Füllung am Nullpunkt gerade abschneiden, nur das äußere linke/rechte Ende abrunden.
- Mittige FOREGROUND-Linie steht3dp über; darunter kleine„0“.
- Mittlere50% zeigen persönliche Abweichungen−1…+1, außen25% Puffer bis−4/+4 mit gröberer Skalierung. Tatsächliche Zahlenwerte nicht begrenzen. Ohne ausreichende Referenz steht rechts neben dem Indexnamen „noch XX Tage bis zum ersten Wert“; fehlenden Tagesmesswert/fehlende Streuung kurz benennen. Beide Balken FOREGROUND ohne erfundene Stress-/Warnschwellen. Berechnung und wissenschaftliche Grenzen in hrv-autonomic-index-proposal.md und In-App-Hilfe erklärt.

## Empfehlung und Ablauf

- Vorhandenen Empfehlungstext unter den Anzeigen frei auf der Seite darstellen, ohne Kasten, mit24dp Abstand.
- Textbutton„Zur genauen Auswertung“ bleibt ausdrücklich bestehen; er war nur im Mockup abgeschnitten. Präzisiertes Sticky-Handoff aus hrv-measurement-flow-revision.md erhalten.
- Datum/Navigation aus der bisherigen Tagesansicht berücksichtigen, historische Tagesauswahl muss erkennbar bleiben.
- Fehlende Werte/Referenzen neutral kenntlich machen, keine scheinbaren Null-/Normalwerte erfinden. Keine neue Alterskorrektur oder Kubios-Gleichheit behaupten.

## Bestätigung des Nutzers

HRV- und Puls-Grenzwerte sind genau die bereits aus den bestehenden Korridoren berechneten Werte. Nur die Darstellung ändern, keine neue Referenz-/Grenzwertberechnung, Trainingsempfehlung oder Annahmeregel einführen. Die Beispieldaten im Mockup sind keine neuen festen Grenzwerte.


Nach Build117 kompakter: Empfehlung, HRV-Begründung und subjektives Befinden in einem durchgehenden Absatz, Empfehlung weiterhin hervorgehoben/funktional gefärbt. Zusätzliche Zeitraum-/„von4 Tagen“-Zeile entfällt; berechnete Daten/Empfehlungslogik unverändert. Textbutton „Zur genauen Auswertung“ bleibt. Obere Grenzwertbeschriftungen3dp höher, untere3dp tiefer; Markierungslinien unverändert.

Oberer Seitenabstand für Mess- und gemeinsame Auswertungsseite von16dp auf8dp reduziert. Im echten360-dp-Layout betrug der Abstand Header→sichtbarer Kreisrand zuvor ca.21,9dp (Messung) bzw.27,8dp (Tag), danach ca.13,9/19,8dp; der Unterschied stammt aus der Kreiszeichnung, nicht aus zusätzlichen Seitenrändern. Zwei gezielte native Fälle erfolgreich in `hrv-compact-header-label-tests.log` (echte Header-/Kreispositionen, kompakter Text/fehlende Werte und beide Designs), Vorschau visuell geprüft. Keine neue APK beauftragt.

### Verfeinerung nach Build118

Die hervorgehobene Kurzempfehlung (z.B. „Heute Intensität begrenzen.“) steht in einer eigenen Zeile. Danach folgen HRV-Begründung und subjektives Befinden zusammen ohne weiteren Absatz. Berechnung unverändert.
