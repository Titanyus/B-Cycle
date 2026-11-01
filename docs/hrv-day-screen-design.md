# HRV-Tagesscreen: Nutzermockup – 01.11.2026

Gestaltungsvorgabe erhalten, aktuell Plausibilitäts-/Vollständigkeitsbesprechung. Noch nicht implementieren oder offene Farb-/Skalen-/Indexregeln stillschweigend festlegen. Die vorgemerkte gemeinsame Tages-/Detailauswertung und danach die Dashboard-Gesten bleiben Teil der Aufgabenfolge.

## Zweigeteilter Kreis

- Form des Messkreises aufgreifen; oberer Bogen HRV, unterer Bogen Ruhepuls.
- Je150° Bogen: gegenüber dem vollen Halbkreis beide Enden um15° gekürzt. Keine vollen Halbkreise.
- Ruhepuls-Strichstärke wie bestehender Messkreis, HRV doppelte Strichstärke.
- Kurzer horizontaler Trenner mittig zwischen beiden Bereichen, bewusst innerhalb des Kreises.
- Große Werte etwa20% kleiner als der Pulswert auf dem Messbildschirm. Jeweils vertikal im eigenen Halbkreis zentrieren; bestehende Wertschrift verwenden.
- Auf Höhe der Bogenabschlüsse jeweils zentrierte Beschriftung „Herzfrequenzvariabilität“ oben bzw. „Ruhepuls“ unten, normale Roboto-Textrolle.
- Bestehenden persönlichen Normbereich je Messgröße markieren. Als Vorschlag Marker bei einem Viertel und drei Vierteln des jeweiligen Bogenwegs, radial auf den Kreismittelpunkt ausgerichtet.
- Markierungen2dp FOREGROUND, beide Enden je3dp über die Bogenstrichstärke hinaus. Kleine Grenzwerte in Verlängerung der Linien.
- HRV-Farben nach Nutzer: Grün im Korridor, Gelb außerhalb, Rot deutlich außerhalb. Noch klären, wie „deutlich“ definiert wird und ob auch hohe HRV oberhalb rot werden soll: bisherige Tagesempfehlung behandelt oberhalb wie innerhalb und hängt zusätzlich vom Befinden ab. Farbentscheidung nicht versehentlich in eine neue Empfehlungsregel umdeuten.
- Ruhepulswert/Bogen ausschließlich weiß laut Nutzervorgabe; Grenzen als echte bpm-Werte, nicht die im Mockup wiederholten HRV-Beispielzahlen6,3/7,5 übernehmen.

## PNS-/SNS-Balken

- Unter dem Kreis nach Mockup zuerstPNS, dannSNS; je links Indexname, rechts Zahlenwert.
- Ähnliche horizontale Bahn wie bisheriger HRV-Bereichsbalken, Dicke wie Messkreis/Ruhepulsbogen.
- Null liegt in der Mitte. Füllung von dort je nach Vorzeichen nach links oder rechts.
- Füllung am Nullpunkt gerade abschneiden, nur das äußere linke/rechte Ende abrunden.
- Mittige FOREGROUND-Linie steht3dp über; darunter kleine„0“.
- Feste sichtbare Skalenspanne und Umgang mit Überlauf sowie semantische Farben noch festlegen. Indexwerte nicht rechnerisch auf±2 begrenzen. PNS und SNS brauchen eine begründete, unterschiedliche Interpretation; Indexformel weiter offen, siehe hrv-autonomic-index-proposal.md.

## Empfehlung und Ablauf

- Vorhandenen Empfehlungstext unter den Anzeigen frei auf der Seite darstellen, ohne Kasten, mit24dp Abstand.
- Textbutton„Zur genauen Auswertung“ und präzisiertes Sticky-Handoff aus hrv-measurement-flow-revision.md weiterhin einplanen.
- Datum/Navigation aus der bisherigen Tagesansicht berücksichtigen, historische Tagesauswahl muss erkennbar bleiben.
- Fehlende Werte/Referenzen neutral kenntlich machen, keine scheinbaren Null-/Normalwerte erfinden. Keine neue Alterskorrektur oder Kubios-Gleichheit behaupten.
