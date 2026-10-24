# Fokusrahmen für Eingabefelder – Testentwurf

24.10.2026, auf Nutzerwunsch. Ausschließlich Kontur und äußerer Schein; keine Änderung an Innenfläche, Rundung, Feldgröße, Padding, Eingabewert, Tastatur, Einheiten, Auswahl, Validierung oder Autosave. Keine zusätzliche Abhängigkeit, Konfiguration oder Daueranimation.

## Gemeinsame Regeln

`BicycleInputFrame` ist an den drei vorhandenen Eingabestil-Einstiegen angebunden (`BicycleComponents.input`, `BicycleDialogStyle.inputBackground`, `BicycleForm.outlinedInput`). Normale Controls/Buttons behalten ihre bisherige Gestaltung; das als Eingabe gestaltete Datumsfeld verwendet denselben Rahmen. Die normalen Eingabekonturen bleiben unverändert. Aktiviert und fokussiert:

- 1-dp-Kontur mit fünf gleichmäßig verteilten Farbstopps: dunkel → ACCENT → hell → ACCENT → dunkel.
- Dunkel = ACCENT mit 35 % Schwarz gemischt, hell = ACCENT mit 35 % Weiß, je RGB-Kanal. Deckende Themefarben bleiben deckend, auch Weiß/Schwarz als ACCENT sind gültig. Keine festen Rotwerte oder neuen Paletteinträge.
- Verlaufachse 20° gegen die Horizontale. Die Shadergeometrie wird bei Größenänderung erneuert, keine Rotation des eigentlichen Feldes.
- Weicher statischer ACCENT-Schein bis 8 dp nach außen. Zwölf abgestufte Konturen mit gaußähnlichem Abfall; maximale Deckkraft pro Kontur 18 %. Überlagerung benachbarter Konturen ergibt einen weichen Verlauf. Keine unzuverlässige Hardware-BlurMaskFilter-Schattierung, kein Softwarelayer, kein Render-Loop.
- Ein Clip auf die abgerundete Feldfläche hält den Glow vollständig außerhalb. Zeichnen im übergeordneten Scrollviewport verhindert Abschneiden an schmalen Formularzeilen. Viewport und fixe Header werden respektiert; Sticky-Kategorien werden explizit ausgespart. Sichtbarkeit, Fokus, enabled, Scrollposition, Themewechsel und Detach berücksichtigen.

Alle Werte stehen in `BicycleDesign` (`inputFocus…`). Der Hintergrund bleibt eine `GradientDrawable`-Unterklasse für bestehende Rahmen-/Transparenzprüfungen. Fokus kommt aus dem nativen Drawable-Zustand; vorhandene Fokuslistener und Eingabecallbacks werden nicht ersetzt oder ausgelöst.

Native Vorschau: [input-focus-frame.png](previews/input-focus-frame.png). Die HTML-Atlasdarstellung ist eine CSS-Näherung und separat als solche beschriftet. Noch keine neue APK angefragt oder erstellt.

Gezielte Tests: Farbenableitung über mehrere Grundfarben einschließlich Weiß/Schwarz, nativer Rahmen/Innen-Transparenz/äußerer Glow, Scrollposition, Themewechsel, disabled, Listener-/Wert-/Geometrie-Erhalt sowie Einheiten-/Profil- und vorhandene Formular-/Scrollregressionen.
