# Dialoggestaltung: Rücknahme der allgemeinen Umstellung

Die allgemeine Dialogbasis aus Build82 wurde auf Nutzerwunsch wieder entfernt. Es gibt keine globale TERTIARY-Umfärbung oder gemeinsame neue Schattenregel für normale Dialoge oder Einstellungsseiten. Die ursprünglichen Implementierungen von BicycleDialogBuilder/BicycleDialogStyle, BicycleFullscreen, BicycleNavigation, HRV-Tageskarte und ihren Aufrufern sind wiederhergestellt.

Damit gelten erneut SECONDARY für Seiten-/Dialoginhalte sowie PRIMARY für normale Seiten-/Dialogheader mit bisheriger Glow-Dekoration. Vorhandene HRV-/Fahrtabschluss-Ausnahmen bleiben wie vor der Umstellung SECONDARY, der Messheader schlicht. HRV-Tageskarte wieder SECONDARY mit ursprünglicher Kontur und ursprünglicher Flat-/Glow-Schattenbehandlung. Farben, Vollbildgrößen, Systemleisten/IME, Navigation und Funktionen entsprechen dem Zustand vor der allgemeinen Umstellung.

**Erhalten bleibt der Trink-Alert:** BicycleAlertSurface ist ausschließlich eine PRIMARY-Fläche für den vorhandenen Dashboard-Trinkhinweis. Kein Header, dialogRadius18dp, diffuser schwarzer alertShadow24dp auch in Flat. Keine Bindung an normale Dialoge oder Seiten; kein rekursives Umfärben anderer Inhalte. Position, Dauer, Antippen und Erinnerungsplan unverändert.

Die später separat eingeführten [Glow-Sticky-Karten](sticky-sections.md) bleiben erhalten. Sie sind unabhängig von der zurückgenommenen allgemeinen Dialoggestaltung.

Gezielte Regressionen für Wiederherstellung der PRIMARY-/SECONDARY-Rollen, ursprüngliche HRV-Karte, erhaltenen Alert in Flat/Glow und beim Themewechsel, native Dialogaktionen, Vollbild/Insets, HRV-Einstieg, Trink-Timer/-Position und Glow-Sticky-Karten. Keine neue APK ohne gesonderten Auftrag.
