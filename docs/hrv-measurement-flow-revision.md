# Vorgemerkt: HRV-Ablauf und gemeinsame Auswertung – 01.11.2026

Nutzerauftrag festhalten, noch nicht umsetzen: Als Nächstes kommen Gestaltungsangaben für den Tagesscreen. Zuerst dessen Darstellung festlegen, dann den Ablauf und die gemeinsame Auswertungsseite passend umsetzen. Messberechnung, Tagesempfehlungen und Diagrammfunktionen erhalten; beauftragt sind Darstellung und Navigation.

## Messung und Tagesfragen

- Messbildschirm zeigt nur Messkreis und Diagramm. Dazwischen24dp zusätzlicher Abstand.
- Unteren Button auf dem Messbildschirm vollständig entfernen.
- Nach abgeschlossener Messung automatisch zu den Tagesfragen wechseln.
- Nach den Tagesfragen führt der untere Button weiter zur Auswertung.

## Auswertung: Tageswert und Verlauf auf einer gemeinsamen Seite

- Anfangs ausschließlich Tageswert und Tagesempfehlung sichtbar. Deren neues Layout folgt noch vom Nutzer.
- Unten reiner Textbutton „Zur genauen Auswertung“.
- Betätigung bringt die ausführliche Verlaufsauswertung von unten ins Bild.
- Wichtige Nutzerpräzisierung: Die Tagesauswertung bleibt zunächst an ihrer Position stehen. Die Detailauswertung scrollt dazu, bis sich beide Abschnitte mit dem vorgesehenen Abstand berühren.
- Erst ab diesem Kontakt schiebt die weiter nach oben kommende Detailauswertung den Tagesbereich nach oben aus dem sichtbaren Bildschirm, sinngemäß wie das Ablösen eines Sticky-Headers. Der Tagesbereich darf also nicht schon zu Beginn gemeinsam mit dem Detailbereich wegscrollen.
- Die Bewegung stoppt mit dem vorgesehenen Abstand oben so, dass die detaillierte Auswertung vollständig wie bei direktem Aufruf sichtbar ist.
- Danach können beide Abschnitte als eine gemeinsame Seite hoch und herunter gescrollt werden. Keine getrennten Screens für Tages- und Detailauswertung.
- Konkrete Maße über die genannten24dp und den bestehenden sinnvollen oberen Abstand hinaus sind noch nicht festgelegt. Kein neues Tageslayout erfinden; Nutzervorlage abwarten.

Zusätzlicher Wunsch für den Tagesscreen: nachvollziehbare eigene PNS-/SNS-Aktivierungsindikatoren zur Belastungseinschätzung. Keine exakte Kubios-Nachbildung. Fachliche Grenzen und noch festzulegende Berechnung siehe [Vorschlag](hrv-autonomic-index-proposal.md). Noch keine Layout- oder Rechenänderung umgesetzt.
