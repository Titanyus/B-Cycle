# Dashboardweite Trainingshilfe

Die Visualisierung wird pro Dashboard konfiguriert, nicht mehr pro Kachel. TrainingHelpView wird identisch in Design & UI und dauerhaft in der Feldauswahl eingesetzt. Beide besitzen den gemeinsamen Sticky Header und erklären den Geltungsbereich „dieses Dashboard“. Langes Drücken auf jede obere Dashboard-Kachel öffnet einheitlich den Editor. Dort öffnet ein kurzer Tipp auf ein Feld die Feldauswahl; messwertabhängige Long-Press-Sonderfunktionen entfallen.

Die segmentierte Auswahl „Aus | Alle Zonen | Zone 2“ ersetzt die zwei Modusschalter. Genau ein Segment ist ausgewählt; ein erneuter Klick auf das aktive Segment deaktiviert es nicht. ACCENT hebt das aktive Segment hervor, die Schriftfarbe erhält ausreichenden Kontrast. Native RadioGroup-/RadioButton-Semantik gewährleistet Tastatur- und Accessibility-Bedienung ohne sichtbare Auswahlkreise. Ein separater Chevron nach unten rechts neben den Segmenten klappt die Auswahl für aktuelle Werte und Durchschnittswerte auf. Sie ist zunächst geschlossen; der Modus und das Aufklappen funktionieren unabhängig. Die Quelle ergibt sich aus dem Messwert: Puls verwendet Puls, Fahrerleistung verwendet Rider Power. Trainingszone/FTP-Auslastung/HF-Reserve zählen als aktuelle Felder. Keine Motor-, Maximal-, Kadenz- oder unsichtbaren Datenfelder einfärben. Neue Konfiguration: standardmäßig Aus, aktuelle Werte vorgewählt. Alte feldbezogene Quellen bleiben gespeichert und migrieren, solange keine neue Auswahl gespeichert ist, zu Alle Trainingszonen und den entsprechenden aktuellen/Ø Gruppen.

Alle Trainingszonen nutzt unverändert die vorhandenen fünf Puls-/sieben Coggan-Farben samt ±3-Prozentpunkte-Übergang. Zone 2 nutzt einheitlich das bestehende Grün/Rot der Pulszonen. Der Zielbereich basiert auf 55–75 % FTP bzw. 60–70 % HF-Reserve. Die mathematische Zonenzuordnung bleibt unverändert.

Die visuelle Untergrenze wird großzügig behandelt: Bis 10 Prozentpunkte unter der eigentlichen Z2-Untergrenze bleibt die Farbe grün. Weitere 10 Prozentpunkte nach unten bilden einen glatten Übergang nach Rot. An der Obergrenze reicht der Übergang nur von −0,5 bis +0,5 Prozentpunkten. Die Farbmischung ist monoton und stetig (smoothstep), ohne zusätzliche Einstellungen. Damit ist beispielsweise bei FTP 200 W die Leistung von 90 W noch grün, 70–90 W bildet den unteren Übergang; oberhalb 151 W ist Rot erreicht. Das ist eine Trainingsorientierung, keine Änderung der Leistungszone des Messwerts.

Bestehende zeitgewichtete Anzeigepuffer, Aktualitätskontrollen, Fahrtmittelwerte und ZoneAmbientGlow mit 1,5 Sekunden Überblendung werden wiederverwendet. Keine Änderung an Sensoren, FIT, Performance Engine oder Aufnahme. Ohne gültiges Profil oder frische Messwerte bleibt der Glow aus.

DashboardTrainingHelp speichert einen Modus und zwei boolesche Gruppen unter einer seitenbezogenen ID. SettingsBackup enthält und validiert dieses Objekt zusätzlich zu den lesbar erhaltenen Legacy-Quellen. DashboardPages.keys erfasst es für Slot-Bereinigung und den vorbereiteten fahrradbezogenen Design-Umfang.

In der Feldauswahl steht die Trainingshilfe außerhalb und unterhalb von Daten und Graph. Der Schnellzugriff bleibt sichtbar und wird weder bei „Keine Daten“ noch bei nicht unterstützten Messwerten oder Kartenfeldern ausgeblendet. Dieser Schnellzugriff konfiguriert weiterhin das aktuelle Dashboard, nicht die ausgewählte Kachel. Nur die frühere feldbezogene Visualisierungsfunktion bleibt verborgen.

Umstellung am 18.10.2026: identische Segmente in Design & UI und im Feldauswahl-Schnellzugriff. Der Modus bleibt OFF/ALL_ZONES/ZONE_TWO; keine Persistenzmigration. Aktuelle Werte und Durchschnittswerte behalten ihre vorhandenen Schalter und bleiben auch beim Abschalten des Modus gespeichert.

## Pulsmodelle ab 0.8.2

Pulszonen und Zone-2-Grenzen folgen der global im Fahrerprofil gewählten Methode (Karvonen, Maximalpuls oder Friel-Rad). HF-Reserve (%) bleibt unverändert. Siehe [Pulsmodelle](heart-rate-methods.md).
