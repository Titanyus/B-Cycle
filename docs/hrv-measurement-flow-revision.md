# Vorgemerkt: HRV-Ablauf und gemeinsame Auswertung – 01.11.2026

Am01.11.2026 umgesetzt, inklusive bestätigtem Tagesscreen und persönlichen PNS-/Aktivierungsindikatoren. Messberechnung, Tagesempfehlungen und Diagrammfunktionen bleiben erhalten.

## Messung und Tagesfragen

- Messbildschirm zeigt nur Messkreis und Diagramm. Dazwischen24dp zusätzlicher Abstand.
- Unteren Button auf dem Messbildschirm vollständig entfernen.
- Nach abgeschlossener Messung automatisch zu den Tagesfragen wechseln.
- Nach den Tagesfragen führt der untere Button weiter zur Auswertung.

## Auswertung: Tageswert und Verlauf auf einer gemeinsamen Seite

- Anfangs ausschließlich Tageswert und Tagesempfehlung sichtbar. Layout gemäß [Nutzermockup](hrv-day-screen-design.md).
- Unten reiner Textbutton „Zur genauen Auswertung“.
- Betätigung bringt die ausführliche Verlaufsauswertung von unten ins Bild.
- Wichtige Nutzerpräzisierung: Die Tagesauswertung bleibt zunächst an ihrer Position stehen. Die Detailauswertung scrollt dazu, bis sich beide Abschnitte mit dem vorgesehenen Abstand berühren.
- Erst ab diesem Kontakt schiebt die weiter nach oben kommende Detailauswertung den Tagesbereich nach oben aus dem sichtbaren Bildschirm, sinngemäß wie das Ablösen eines Sticky-Headers. Der Tagesbereich darf also nicht schon zu Beginn gemeinsam mit dem Detailbereich wegscrollen.
- Die Bewegung stoppt mit dem vorgesehenen Abstand oben so, dass die detaillierte Auswertung vollständig wie bei direktem Aufruf sichtbar ist.
- Danach können beide Abschnitte als eine gemeinsame Seite hoch und herunter gescrollt werden. Keine getrennten Screens für Tages- und Detailauswertung.
- Konkrete Maße über die genannten24dp und den bestehenden sinnvollen oberen Abstand hinaus sind noch nicht festgelegt. Kein neues Tageslayout erfinden; vorliegende Nutzervorlage und die dort offenen Regeln beachten.

Messung behält Start/Abbruch/Speicherwiederholung innerhalb des Kreises; nur der untere Navigations-/Ergebnisbereich entfällt. Automatischer Wechsel erst nach abgeschlossener und gespeicherter Messung, nicht bei Abbruch oder Speicherfehler und nicht erneut für ein bereits beim Öffnen vorhandenes Ergebnis. Messdetails/Qualitätshinweise bleiben über den Verlauf zugänglich.

Gemeinsamer nativer ScrollView: Der Tagesbereich bleibt in der freien Anfangsfläche stehen, bis der Verlauf mit24dp Abstand ankommt; danach wird er weggeschoben. Auf kleinen Displays bleibt der gesamte höhere Tagesinhalt scrollbar. Textbutton fährt den Verlauf bis auf den normalen oberen Seitenabstand. Historischer Tageskopf bleibt bis zum Verlaufsbereich datiert, dort übernimmt der gewählte Verlaufstag/-zeitraum. Tagesbefinden-Änderungen laden auch den Tagesbereich neu.

[Tagesscreen](hrv-day-screen-design.md), [bestätigte Indexformeln und Grenzen](hrv-autonomic-index-proposal.md).
