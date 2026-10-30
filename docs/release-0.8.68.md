# B|CYCLE 0.8.68

Änderungen seit Build 108:

- Fahrradprofile neu geordnet: Name, Sensoren, Design & UI, Trainingshilfe, Karten, FIT-Aufzeichnung, Strecken-Aufzeichnung. Automatischen Aufnahmestart zu Sensoren verschoben und Texte vereinfacht.
- Design & UI: Flat-Schalter, Theme, Vollbild als Rahmen-Accordion, Trenner, kleine Dashboard-Layout-Überschrift. Kontextfooter berücksichtigt die neue Abschnittsreihenfolge.
- Bosch-Fahrmodus immer oben. Fahrradlicht bei eingeschaltetem Licht automatisch oben: bei verborgenem Android-Status am bisherigen Statusplatz, sonst rechts oben im Dashboard. Untere automatische Lichtmarkierung und bisherige Positionsoptionen entfernt.
- Schaltwerk-Akkuwarnung beim Aufnahmestart unter 20 % immer aktiv; bisheriger Warnschalter entfällt.
- Sensoren, Fahrtbenennung mit Lucide map-pin-pen, Metadaten und Aufzeichnungsgruppen als Rahmen-Accordions. Native Dropdownlisten in Glow mit derselben einfachen Kontur wie in Flat.
- Normale Rahmen sowie nicht ausgewählte Auswahlkacheln mit 2 dp und FOREGROUND bei 25 % Deckkraft. Ausgewählte und besondere Glow-Konturen behalten ihren Stil.
- Trackfarbdialog bleibt nach Auswahl offen, markiert die Farbe und erhält die Auswahl beim Helligkeitswechsel. Speichern beim Verlassen.

[Details und Prüfstand](profile-settings-revision.md). 76 gezielte Fälle während der Profilrevision, danach 12 Prüfungen zur inaktiven Kachelkontur erfolgreich (fünf davon wiederholt). Native Vorschauen, Typografie, UI-Katalog und Diff geprüft. Keine vollständige Testsuite und kein neuer physischer Gerätetest.

[Buildnachweis und Download](build-109.md).
