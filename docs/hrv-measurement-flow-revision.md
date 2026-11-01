# HRV-Ablauf und gemeinsame Auswertung – 01.11.2026

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
- Übergangsabstand24dp; der Button richtet den Verlauf am bestehenden oberen Seitenabstand aus. Tagesgestaltung gemäß Nutzermockup und bestätigten Präzisierungen.

Messung behält Start/Abbruch/Speicherwiederholung innerhalb des Kreises; nur der untere Navigations-/Ergebnisbereich entfällt. Automatischer Wechsel erst nach abgeschlossener und gespeicherter Messung, nicht bei Abbruch oder Speicherfehler und nicht erneut für ein bereits beim Öffnen vorhandenes Ergebnis. Messdetails/Qualitätshinweise bleiben über den Verlauf zugänglich.

Gemeinsamer nativer ScrollView: Der Tagesbereich bleibt in der freien Anfangsfläche stehen, bis der Verlauf mit24dp Abstand ankommt; danach wird er weggeschoben. Auf kleinen Displays bleibt der gesamte höhere Tagesinhalt scrollbar. Textbutton fährt den Verlauf bis auf den normalen oberen Seitenabstand. Ein gemeinsamer ausgewählter Tag gilt für Datumstitel, Tagesansicht und Tagesaktionen. Frischer Einstieg und Abschluss einer Messung starten mit dem aktuellen Kalendertag; fehlt dort eine Messung, keine ältere als Ersatz anzeigen. Verlaufsauswahl lädt auch Empfehlung, Puls und Indizes dieses Tages. Veraltete Workerantworten werden verworfen. Wochen-/Monatsdarstellung bleiben als klar beschriftete Aggregationen erhalten; eine Auswahl darin verwendet für die Tagesansicht den tatsächlichen repräsentativen Messtag. Tagesbefinden-Änderungen laden auch den Tagesbereich neu.

[Tagesscreen](hrv-day-screen-design.md), [bestätigte Indexformeln und Grenzen](hrv-autonomic-index-proposal.md).

## Footer und Titel – 01.11.2026

Redundante Aktion „Auswertung“ entfernt. Verbleiben Messergebnis, Tagesbefinden und Löschen. Zentraler BicycleFooterVisibility-Schalter im Kopf wie bei Strecken/Fahrradprofilen/-details, von Anfang an gebunden; standardmäßig verborgen, Tagesklick darf wie bisher temporär einblenden. Manueller Schalter und temporäre Sichtbarkeit verwenden denselben Controller. Titel ausschließlich Datum, ohne „Auswertung“/„Tagesauswertung“ und unabhängig von der vertikalen Scrollposition. Frisches Öffnen beginnt immer bei heute, keine Übernahme einer früheren Auswahl. Ohne gespeicherten Tag keine Aktionen auf einem anderen Tag anbieten. Kontext-only-Importtage bleiben editier-/löschbar.


## Horizontales Tageswischen nach Build117

Nur der obere Tagesbereich erhält denselben PageViewport wie das Dashboard. Horizontal jeweils einen Kalendertag wechseln; fehlende Messungen bleiben als fehlend sichtbar. Grenzen: frühester vorhandener Mess-/Antworttag bis heute, an Grenzen weich zurück. Datum, Empfehlung, Werte und Diagrammauswahl/Tagesaktionen wechseln gemeinsam; Diagrammzoom/-pan liegt außerhalb der Tages-Wischfläche. Footer fest. Erst fertige lokale Tagesansicht einschieben; bis dahin ausgehende Ansicht halten. Maximal16 besuchte Tagesmodelle im Dialog-RAM zwischenspeichern, nach Bearbeiten/Löschen neu laden und Cache leeren. Kein zusätzlicher Sensor-/Kartenbaum und keine neue Speicherung.

Native Integration prüft beide Richtungen, gemeinsame Auswahl/Datum/Diagramm, festen Footer, Snapshotfreigabe und obere Tagesgrenze. Bestehende Bearbeiten-/Löschen-/Header-/vertikale Scrollprüfungen weiterhin erfolgreich; siehe `hrv-day-swipe-final-test.log` und `hrv-day-swipe-smooth-pages-tests.log`.
