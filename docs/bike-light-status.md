# Fahrradlicht-Position

Unter dem Bosch-Fahrmodus steht bei E-Bike-Profilen das Dropdown **Fahrradlicht** mit Unten/Oben. Standard unten. Oben nur bei aktiviertem Dashboard-Vollbild auswählbar. Schaltet man Vollbild aus, greift unten als Darstellungsrückfall; die gewählte obere Position bleibt für erneutes Vollbild gespeichert.

Die Anzeige folgt weiterhin ausschließlich dem tatsächlichen bestehenden BIKE_LIGHT-Sensorzustand; nur bei eingeschaltetem Licht sichtbar, keine neue Lichtsteuerung/Animation oder BLE-Abfrage. Unten bleibt sie in der mittleren Karten-Datenzeile, oben entfällt diese zusätzliche untere Markierung. Ein ausdrücklich gewähltes BIKE_LIGHT-Datenfeld bleibt unverändert.

Oben verwendet DashboardStatusStrip das neue horizontale Lucide-flashlight-Derivat mit drei Strahlen in zentralem Gelbgold. Rechter Symbolrand liegt15dp links vom gemeldeten Kamera-/Notchrand; ohne Kameraausschnitt15dp links von der Mitte. Die linke Statusgruppe hält Symbol und6dp Zwischenraum frei. Ohne sinnvollen sicheren Platz wird nicht über Systemflächen gezeichnet. Das Licht kann allein die benutzerdefinierte Statuszeile sichtbar machen, auch ohne belegte sechs Slots. Keine zusätzliche frei belegbare Slot-ID und keine doppelte Anzeige. Dialog-/Vollbild-Sichtbarkeit bleiben erhalten.

Die reine Positionspräferenz gehört zum Fahrrad-Designprofil, Profilübernahme sowie Design-Sicherung/-Wiederherstellung. Ungültige Sicherungswerte vor Anwendung ablehnen; Altstände ohne Feld behalten Standard/Bestand. Keine Live-Lichtwerte persistieren.

Zusätzliche Nutzerkorrektur nach Test am Poco: feste Außenabstände der Statusgruppen links8dp/rechts25dp. Gemeldete Systemindikator-/Kamerabounds mit bisherigen8dp Sicherheitsabstand weiterhin berücksichtigen. Keine zuverlässige Erkennung der tatsächlichen Punkt-Sichtbarkeit behaupten.


Veröffentlichung28.10.2026: Diese Änderungen sind in **APK0.8.59/Build100** enthalten. [Buildnachweis und Download](build-100.md). Frühere Aussagen zu fehlender neuer APK sind Umsetzungshistorie.
