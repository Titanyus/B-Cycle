# Gemeinsame Dialoggestaltung

`BicycleDialogSurface` übernimmt die abgerundete HRV-Angebotskarte als gemeinsame Basis. Normale Dialoghintergründe und ihre optionalen Header verwenden TERTIARY. Header sind schlicht, ohne die frühere Chromatic-Dekoration; Titel, Zurück und bestehende Infoaktionen bleiben bedienbar. Ohne Titel entsteht kein zusätzlicher Header. Die bestehende Hell-/Dunkel-Einstellung wird berücksichtigt, einschließlich lesbarer Titel im hellen TERTIARY-Header.

`dialogRadius = 18 dp`, `dialogShadow = 24 dp`, schwarze native Ambient-/Spot-Schatten. Der Schatten gilt in Flat und Glow, ausdrücklich abweichend von der allgemeinen Flat-Regel. HRV-Grafik, Inhalt und Aktionen bleiben erhalten.

Die Alert-Variante verwendet PRIMARY für Fläche und gegebenenfalls Header (`BicycleDialogBuilder(..., headerRole=BicycleDialogSurface.alert)` beziehungsweise `BicycleDialogSurface.bind` für Dashboardkarten). DrinkReminderPanel nutzt sie ohne Header statt des früheren SECONDARY-Panels mit breitem PRIMARY-Rand. Icon, Text, eingestellte Position, Dauer, Antippen und Zeitplan bleiben unverändert.

Kompakte Dialoge behalten ihre Fenstergröße; Einstellungsseiten bleiben aus Gründen der Bedienkontinuität im bisherigen Vollbild-Host. Dessen Fensterschatten liegt außerhalb des Displays; es werden keine zusätzlichen Seitenränder oder neue Größenregeln eingeführt. Android-Systemleisten, Tastatur-Inset, Scrollposition, feste Aktionsfooter und Statusleisten-Ausnahmen bleiben erhalten. Gemeinsame Seitensurfaces wechseln auf TERTIARY; Bedienelemente und fachliche Farben behalten ihren Stil.

Gezielte Regressionen: gemeinsame Farben/Rundung/Schatten in beiden Designs, helle Einstellungen, native Bestätigungsaktionen, headerlose Variante, Vollbild-Layout/Insets/Scrollen, HRV-Tageskarte und Trink-Timer/-Positionen. Native Schattenstärke auf echter GPU nicht durch Robolectric messbar.
