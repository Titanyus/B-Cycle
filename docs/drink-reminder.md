# Trink-Erinnerung

Letzter Punkt innerhalb der Trainingshilfe, mit horizontalem Abschnittstrenner davor und ohne eigenen Sticky-Header. In der Feldauswahl unabhängig von der Datenquelle und in den Fahrradprofileinstellungen dieselbe DrinkReminderSettingsView, keine kachel-/dashboardbezogene Kopie. Schalter rechts, Beschriftung links, separater Accordion-Chevron wie Daten/Trendindikator. Aufklappen aktiviert nichts.

Unterpunkte: Intervall 10–60 Minuten in 5-Minuten-Schritten; Anzeigedauer 5–60 Sekunden in 5-Sekunden-Schritten; Position mit denselben drei nativen Vorschaukacheln wie App-Benachrichtigungen: Unten/Mitte/Oben. Gemeinsame MessagePositionChoices/MessagePositionPreview. Standards: aus, 30 Minuten, 15 Sekunden, Mitte. Ungültige gespeicherte Werte fallen auf die jeweiligen Vorgaben zurück. Alte Konfigurationen ohne neue Werte bleiben kompatibel.

Alle vier Preferencekeys gehören zum BikeUiConfigurationScope. Dauer und Position werden mit Profilbank/Profilkopie und DESIGN-Sicherung erhalten; alte Sicherungen ohne zusätzliche Felder bleiben lesbar. Abgelöste Profil-Controls dürfen nicht in andere aktive Profile schreiben.

DrinkReminder zählt ausschließlich monotone Aufnahmezeit inklusive Stillstand. Bei neuer Aufnahme neu starten, bei Stopp zurücksetzen. Aktivierung/Intervalländerung startet das neue Intervall ab dieser Änderung. Änderungen nur an Dauer/Position starten den Timer nicht neu. Keine Trinkmenge, FIT-Felder oder Meldung außerhalb der Aufnahme. Timer gehört zum Runtime, Hintergrundzeit zählt mit; beim Zurückkehren höchstens einen fälligen Hinweis nachholen, keine Warteschlange. Während Einstellungen/Dashboard-Editor keine Anzeige.

Dashboard-Hinweis mit Flaschensymbol und „Trinken nicht vergessen!“ an der gewählten Position. Wie App-Benachrichtigungen innerhalb des Dashboard-Fensters platzieren, Systemleisten-/Cutout-Inset-Sicherheit erhalten. SECONDARY und bestehende Rundung/3-dp-PRIMARY-Rahmen, Flat ohne Schatten. Nach eingestellter Dauer oder Antippen schließen. Schließen bestätigt keine Menge und setzt das periodische Intervall nicht zurück. Bei Aufnahmeende, Deaktivierung und Activity-Verlassen Timeout/Panel entfernen. Keine zusätzliche Systembenachrichtigung oder Hintergrundaufgabe.

Gezielte Tests: Intervall und Messende, Dauer-Endpunkte/Schritte/Timeout, unabhängiges Accordion, Positionen, Integration als letzter Trainingshilfepunkt ohne eigenen Sticky-Header, Profile/Kopie/Backup und unveränderter Benachrichtigungsdialog. Keine Trinkmengenbestätigung im Umfang.


NachBuild103: Trinkerinnerung oben/unten48dp zusätzlicherVERTIKALERAbstand zum sicherenDashboardrand; Mitte und seitlicheAbstände unverändert. Gemeinsamer BicycleDesign.dashboardMessageEdgeInset mit App-Nachrichten. Zeitplan, Anzeigedauer und Berührungsaktion unverändert.
