# Trink-Erinnerung

Schnellzugriff in der Dashboard-Feldauswahl als eigener Abschnitt unterhalb von Daten/Graph und Trainingshilfe. Unabhängig von der ausgewählten Feldquelle immer sichtbar; dieselbe DrinkReminderSettingsView und dieselben Profilwerte wie in den Profileinstellungen, keine kachel- oder dashboardbezogene Kopie.

Profilbezogener Schalter mit separatem Accordion in Design & UI, unterhalb des Bosch-Fahrmodus. Auch bei Bike/Indoor ohne Bosch-Schalter vorhanden. Aufklappen aktiviert nichts, allein der äußere Switch schaltet. Intervall über SeekBar mit elf Positionen: 10–60 Minuten, 5-Minuten-Schritte. Standard ausgeschaltet, Intervall 30 Minuten. Ungültige gespeicherte Intervalle fallen auf 30 zurück. Alle Texte in Stringressourcen, eigenes Flaschen-Vektoricon zentral über BicycleIcon.DrinkReminder.

Die beiden Preferencekeys sind Teil des bestehenden BikeUiConfigurationScope. Dadurch je Profil gespeichert sowie über Profil übernehmen und Profilbank-Sicherung kopiert. Die Design-Sicherung enthält beide Werte ebenfalls; alte Sicherungen ohne diese Felder bleiben kompatibel. Alte/detachierte Profil-Controls dürfen keine Einstellungen eines anderen aktiven Profils ändern.

DrinkReminder zählt ausschließlich die verstrichene Aufnahmezeit inklusive Stillstand. Bei Aufnahmebeginn neu starten, bei Stopp zurücksetzen. Aktivieren/Intervallwechsel während Aufnahme startet das neue Intervall ab dieser Änderung. Zeitquelle ist monotone Aufnahmedauer, keine Uhrzeit. Keine Trinkmenge, FIT-Felder, Sensorverarbeitung oder Meldung außerhalb der Aufnahme.

Laufender Timer gehört zum RideRuntime, nicht zur Activity. Hintergrundzeit zählt mit. Bei Rückkehr aus Hintergrund/Einstellungen wird höchstens ein fälliger Hinweis nachgeholt, keine Warteschlange für versäumte Intervalle. Vor erneutem Hinweis gilt die nächste reguläre Intervallgrenze. Während Einstellungen oder Dashboard-Editor geöffnet sind, keine Anzeige.

Hinweis mittig über der Karte, bei kartenlosem Layout mittig im Dashboardbereich. Flaschensymbol und „Trinken nicht vergessen!“. SECONDARY, vorhandene Rundung und 3-dp-ACCENT-Rahmen wie die Sondermeldungen. Automatisch nach 15 Sekunden oder durch Antippen schließen. Schließen gilt nicht als Trinkbestätigung und setzt den periodischen Timer nicht zurück. Bei Aufnahmeende, Deaktivierung oder Verlassen der Activity den Hinweis und seinen Timeout entfernen. Keine neue Systembenachrichtigung oder Hintergrundaufgabe.

Diese Umsetzung ersetzt den früher nur vorgemerkten einfachen Intervallhinweis; eine ausdrücklich bestätigte Trinkmenge oder Intervallrücksetzung per „Getrunken“ bleibt außerhalb des aktuellen Umfangs.
