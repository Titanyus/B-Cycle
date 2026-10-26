# Trainingszonen und Ambient-Glow je Dashboard-Kachel

## Persönliche Leistungszonen

Coggan verwendet ausschließlich den manuell eingegebenen persönlichen FTP.
Keine Übernahme von Estimated FTP oder Veränderung der Performance Engine.
Grenzen: 55 / 75 / 90 / 105 / 120 / 150 % FTP. Die unteren Zonen enthalten ihre
jeweilige obere Grenze; die nächste Zone beginnt beim nächsten ganzzahligen Watt.
Bei 250 W: 0–137, 138–187, 188–225, 226–262, 263–300, 301–375, ab 376 W.
Bruchteile aus Live-Mittelwerten werden gegen die exakten Prozentgrenzen geprüft.
Bei sehr kleinem FTP können Integer-Bereiche leer sein; sie zeigen einen Strich.

Das Fahrerprofil zeigt sieben Bereiche, Namen und semantische Intensitätsfarben,
analog zu den bestehenden fünf Karvonen-Zonen. FTP-Änderungen aktualisieren die
Vorschau sofort. Ungültige/leere FTP-Eingaben erzeugen keine Ersatz-Zonen.

## Dashboard und Feldauswahl

Die zusätzliche Kategorie „Leistung“ verwendet das vorhandene Lucide-zap-Asset.
Vier stabile Feld-IDs: TRAINING_POWER_ZONE, TRAINING_HEART_ZONE,
TRAINING_FTP_PERCENT und TRAINING_HRR_PERCENT. Alle sichtbaren neuen Texte liegen
in Android-String-Ressourcen. Bestehende Kategorien/Felder bleiben erhalten.

„Graph“ ist ein zunächst eingeklapptes Accordion. Direkt dahinter folgt
„Visualisierung“ mit einer unabhängigen Einfachauswahl: Aus, aktuelle Leistung,
Ø Leistung, aktueller Puls oder Ø Puls. Bei Pulsfeldern nur Aus/Puls aktuell/Puls Ø,
bei Leistungsfeldern nur Aus/Leistung aktuell/Leistung Ø. Die anderen bisherigen
Zuordnungen und datenlosen Visualisierungen bleiben erhalten. Unpassende gespeicherte
Zuordnungen werden bei Anzeige und Feldwechsel auf Aus gesetzt, pro Seite gespeichert.
Nur die großen graphfähigen Kacheln
bieten Graphen; der Zonen-Glow ist für alle Dashboard-Kacheln verfügbar.
Kartenfelder erhalten keine Trainings-/Glow-Auswahl.

## Datenverfügbarkeit

TrainingInputs liest bestehende Empfangszeiten, Rider Power, Puls und vorhandene
Fahrt-Durchschnitte aus RideRuntime. Keine neuen Sensorabonnements oder
Aufzeichnung. Zwei begrenzte UI-Puffer bilden zeitgewichtete Mittel: Leistung über fünf Sekunden,
Puls weiterhin über drei Sekunden;
gleiche Empfangszeit wird nicht erneut gewichtet. Echte Nullleistung bleibt
erhalten. Fehlende/ungültige Daten, getrennte Sensoren, Rücksprünge der
Empfangszeit und mehr als fünf Sekunden ohne aktuelle Daten leeren den Puffer.
Lücken werden nicht miteinander verbunden. Nach App-Rückkehr liefern alte
Puffer keinen scheinbar frischen Wert. Ø-Quellen verwenden ausschließlich den
vorhandenen Bosch-Leistungsdurchschnitt bzw. Pulsdurchschnitt und benötigen
weiterhin frische Daten der entsprechenden Quelle.

Karvonen bleibt bei 50–100 % HF-Reserve. Unterhalb der ersten Zone und oberhalb
des persönlichen Maximalpulses gibt es keine HF-Zone/keinen HF-Glow. HF-Reserve
selbst zeigt die berechnete Prozentzahl. Fehlende Profilparameter liefern „—“.

## Darstellung und Persistenz

ZoneAmbientGlow verwendet wie BoschModeGlow radial auslaufendes Ambient-Licht.
Es liegt als separate Hintergrundebene unter Graph und Text. Etwa 40 % Höhe,
wie der Rand-Glow 140/255 Alpha mit drei additiven Lichtbeiträgen, an den Seiten und nach oben transparent. Keine Balken,
Rahmen, Layoutänderungen, farbigen Texte oder Icons. Aus ist Standard und lässt
den bisherigen Kachelhintergrund unverändert. Bosch-Glow und Akkuwarnung bleiben
unabhängig und funktionieren auch ohne Zonen-Glow.

Um jede innere Zonengrenze werden die beiden benachbarten Farben innerhalb
von ±3 Prozentpunkten FTP/HF-Reserve kontinuierlich interpoliert. Zusätzlich
animiert ein 1,5-Sekunden-Übergang mit weichen Enden die Farbänderung; laufende
Übergänge beginnen beim aktuell sichtbaren Farbwert. Diese Darstellung ändert
die exakte Zonenzuordnung nicht. Es gibt keine zusätzlichen Regler.

ZoneGlowConfig speichert acht Quellen pro Seite in dashboardZoneGlow_0 bis _3.
Presets verändern die Kachelgeometrie und erhalten die Auswahl je stabilem Slot.
BackupSection.DASHBOARD exportiert und validiert zoneGlowSources je Seite.
Alte Backups ohne den Eintrag ändern vorhandene Glow-Auswahlen nicht. Neue
Installationen/fehlende oder defekte lokale Einträge fallen auf Aus zurück.

DashboardField.groups bietet Trainingsfelder nur auf ausdrückliche Anforderung
der Dashboard-Feldauswahl. Die bestehende FIT-Feldgenerierung bleibt dadurch
unverändert. Keine FIT-Felder, Rohdaten oder Developer Fields werden ergänzt.

## Prüfung

TrainingZonesTest und TrainingZoneUiTest prüfen Grenzen/Rundung, Nullwerte,
Zeitgewichtung, veraltete Daten, vorhandene Mittelwerte, Profilabhängigkeit,
weiche Farben, transparente Renderfläche, Accordions, unabhängige Auswahl,
echte Editor-Persistenz/erneutes Öffnen und alle vier Backup-Seiten einschließlich
älterer und ungültiger Einträge. Die komplette Testsuite (374 Tests, keine Fehler oder übersprungenen Fälle) und
der optimierte Release-Build inklusive R8 und Lint-Vital sind bestanden. Die visuelle Wirkung während einer realen Fahrt
bleibt auf dem Gerät zu prüfen.

## Stabilität und Zuordnung

Aktuelle Puls- und Leistungsquellen verwenden voneinander unabhängige, zeitgewichtete
Anzeigepuffer. Änderungen der Fahrt-Durchschnitte oder zusätzliche Render-Aufrufe
ändern bei konstanten Live-Werten nicht die aktuelle Zone/Farbe. ±3 Prozentpunkte
um die fachlichen Zonengrenzen und der 1,5-Sekunden-Farbübergang bleiben erhalten.
Die stärkere Leistungsglättung betrifft ausschließlich Trainingsanzeige/-Glow, nicht
Live-Wattwert, FIT-Rohdaten oder Performance Engine.

## Pulsmodelle ab 0.8.2

Pulszonen und Zone-2-Grenzen folgen der global im Fahrerprofil gewählten Methode (Karvonen, Maximalpuls oder Friel-Rad). HF-Reserve (%) bleibt unverändert. Siehe [Pulsmodelle](heart-rate-methods.md).


Trainingshilfe ab25.10.2026: Der Erklärungstext steht innerhalb des gemeinsamen Detail-Accordions, nicht davor. „Trainingshilfe für“ nutzt die kleine zentrale contentHeading14/900Black. Auf-/Zuklappen, automatisches Sichtbar-Scrollen, Modus-/Aktuell-/Durchschnittsauswahl und Trink-Erinnerung bleiben unverändert. Der zusätzlich erwähnte horizontale Strich war ein vom Nutzer zurückgenommener Tippfehler und wird nicht ergänzt.

26.10.2026: Trainingshilfe-/Zonen-Glow auf dem Dashboard ist vom Flat/Glow-UI-Schalter unabhängig und bleibt in beiden Modi in derselben Ebene unter dem Graphen sichtbar. Dashboard-Graphfüllungen nutzen immer den nach unten transparenten Verlauf. Keine Änderung an Zonenberechnung, Quellen, Farben oder Speicherung.
