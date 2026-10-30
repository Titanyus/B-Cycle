# Fahrradprofil-Einstellungen nach Build 108

## Reihenfolge und Texte

Name bleibt zuerst. Danach Sensoren, Design & UI, Trainingshilfe, Karten (bei GPS-Profilen), FIT-Aufzeichnung und Strecken-Aufzeichnung. Die bestehende Indoor-Korrektur bleibt erhalten. Unter Sensoren steht „Aufnahme automatisch starten“ zuerst, danach „Sensoren automatisch verbinden“. Der frühere Trenner unter Radumfang entfällt. Autostart-Verhalten unverändert.

Design & UI: Flat-Schalter, Theme-Auswahl, Vollbild als Dropdown-Rahmen mit Schalter, bestehender Trenner, kleine zentrale 900-Black-Überschrift „Dashboard-Layout“, bestehende Layoutauswahl. Keine zusätzlichen Trenner zwischen den ersten drei Punkten. Kontextfooter bleibt nur innerhalb des Designabschnitts auf Dashboard-Aktionen; nachfolgende Profilabschnitte zeigen wieder die Profilaktionen.

FIT oben: „Diese Sensoren/Datenquellen werden als Hauptwerte in die .fit-Datei übernommen.“ Unterer Fallback-Erklärungstext entfällt, automatische Quellenersetzung bleibt erhalten. Strecken-Aufzeichnung: „Wähle, welche Daten in der .fit-Datei gespeichert werden sollen.“

## Obligatorische Anzeigen und Warnung

Bosch-Modus immer aktiv und oben, einschließlich Cutout-/Statusregion. Funktionale Farben, Quellen-/Frischeregeln und verbundene Akkuwarnung bleiben erhalten. Keine Auswahl für Aktivierung oder Seitenposition mehr. Alte gespeicherte Werte bleiben zur Sicherungskompatibilität erhalten, beeinflussen die Anzeige nicht.

Fahrradlicht folgt dem bestätigten Lichtzustand derselben Bosch-Verbindung. Bei verborgenem Android-Status in der bisherigen goldenen Statusposition links der Notch. Bei sichtbarem Android-Status rechts oben im Dashboard: derselbe zentrale Taschenlampen-/Strahlen-Glyph, Größe aus den bestehenden Lichttokens, 16 dp oberhalb/rechts innerhalb der System-/Cutout-Inset-Fläche. Licht aus/Verbindung getrennt entfernt die Anzeige. Keine Einstellung für Position mehr, automatische untere Lichtmarkierung über der Karte entfällt. Frei belegbares BIKE_LIGHT-Datenfeld bleibt fachlich unverändert.

Schaltwerk-Warnschalter entfernt. Der vorhandene Hinweis beim Aufnahmestart unter 20 % ist stets aktiv (weiterhin nur mit vorhandenem Schaltwerk-Akkuwert und aktivierter Quelle). Auch ein historisch gespeichertes `shiftBatteryWarning=false` unterdrückt ihn nicht mehr. Schwelle, zehn Sekunden, gespeicherte Akkustände und Sensorverbindungen unverändert.

## Rahmen

Normalrahmen zentral über `BicycleDesign.controlBorder=2` dp und `controlBorderAlpha=.25f`: FOREGROUND mit 25 % seiner Deckkraft, Radius weiterhin 6 dp. Wirkt auch auf neutrale Eingaberahmen und normale segmentierte Controls, die diese Tokens verwenden. Die nachfolgende Nutzerergänzung gilt auch für alle nicht ausgewählten Auswahlkacheln: Fahrradprofile, Dashboardseiten/-layouts, Benachrichtigungs-/Trinkpositionen und Fahrradfilter sowie gestrichelte Hinzufügen-Kacheln. Inaktive Strichbreite 2 dp unabhängig von der Inhaltsskalierung. Besondere ausgewählte/Glow-Konturen und Spezialbutton-Stile bleiben unverändert (aktive Kontur 1,5 dp, Preset-Farbverlauf/Deckkräfte).

Native Spinner-Popups in Flat und Glow mit derselben einfachen TERTIARY-Fläche und Normalrahmen, ohne zusätzliche Chromatic-Surface/Glow-Inset. Gleiche Auswahl, deaktivierte Quellen, Adapter und Listener.

Vollbild, Sensorfelder, Fahrtbenennung, Metadaten und die Gruppen der Strecken-Aufzeichnung verwenden den bestehenden `BicycleFrameDropdown`: Icon/Titel links, Chevron rechts vor optionalem Schalter. Inhalt im selben Rahmen, mit zentral 16 dp Seiten-/Bodenpadding und 24 dp Abstand zum Header. Bestehende Controls werden weiterverwendet. Lucide `map-pin-pen` bei Fahrtbenennung: aktuelle Upstream-Geometrie unter bestehender ISC-Attribution.

## Trackfarbauswahl

Farbauswahl bleibt offen, genau eine Zelle markiert. Gewählte Zelle bleibt beim Helligkeitswechsel und bei Schwarz erhalten. Entwurf speichert den tatsächlichen Farbwert; initiale HEX-Farbe bleibt ohne Änderung exakt erhalten. Helligkeitsänderung einer ursprünglichen HEX-Farbe erhält deren Farbton/Sättigung. Erst beim Verlassen über Zurück, Außentipp oder reguläres Schließen wird der Entwurf auf denselben aktiven Profilplatz geschrieben. Aktuelle Breite/Deckkraft beim Speichern übernehmen, keine alten Werte zurückschreiben. Reine HEX-Direkteingabe bleibt bisherige sofortige Speicherung.

## Prüfstand

76 eindeutige gezielte Fälle über mehrere Läufe erfolgreich (Wiederholungen nicht doppelt gezählt): BikesView 3, RecordingSources 7, RideNaming 4, RideNamingReorder 1, FitMetadata 6, DashboardStatus 10, BikeLightStatus 5, BoschModeGlow 11, MapRouteStyle 4, ShiftBatteryDashboard 5, ProfileSettingsRevision 2, ProfileContextFooter 2, BicycleComponents 7, BicycleFrameDropdown 2, StickyAccordionSettings 2, RouteQueryUi 5.

Neue Integration prüft tatsächliche Reihenfolge in Flat/Glow und E-Bike/Indoor, unabhängig bedienbare Schalter/Aufklappen, erhaltene Aufklappzustände, den Footer nach dem Designabschnitt sowie Status-/Dashboard-Lichtposition mit System-Inset, Licht aus und Disconnect. Warnung trotz altem false-Flag, offener Farbdialog mit unveränderter Speicherung bis zum Schließen und erhaltene Auswahl beim Helligkeitswechsel geprüft. Native 2-dp-/25%-Rahmenzeichnung auf verschiedenen Themes geprüft. Typografie, UI-Katalog und Diff erfolgreich.

Ältere UI-Tests erwarteten noch den entfernten Bosch-Schalter, „Profile“ statt „Fahrradprofile“, Glow-Header ohne Seitenabstand und einen nicht aufklappbaren Namensabschnitt. Erwartungen an den bestehenden bzw. ausdrücklich angeforderten Zustand angepasst; keine Produktänderung nur zur Erfüllung solcher Altannahmen. Ein neuer Test ging bei erneutem Öffnen fälschlich von geschlossenen Metadaten aus; er prüft nun korrekt den erhaltenen Zustand. Logs: `/workspace/.tools/profile-settings-{revision-tests,revision-integration-tests,revision-final-ui-tests,border-tests,border-native-tests,border-final-tests}.log`.

Native Ansichten mit synthetischen Testdaten: [Profil](previews/profile-revision-top.png), [Sensoren](previews/profile-revision-sensors.png), [FIT-Aufzeichnung](previews/profile-revision-fit.png). FIT- und Sensoransicht visuell geprüft. Kein neuer physischer Gerätetest. Veröffentlicht in [APK 0.8.68 / Build 109](build-109.md), einschließlich der Ergänzung zu inaktiven Kachelrahmen.
