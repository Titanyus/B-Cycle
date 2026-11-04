# Dropdown-Rahmen

Neue opt-in Komponente `BicycleFrameDropdown`, zuerst in Wartung für die Bauteilgruppen eingesetzt. Kein Sticky-Header und keine automatische Umstellung anderer Dropdowns.

- Geschlossen gleicher Headerstil wie das bisherige normale Accordion: Body-/choice-Schrift, FOREGROUND, Fachicon links vor dem Titel, Chevron rechts danach und optional Schalter/Aktion ganz rechts; mindestens48dp Touchhöhe.
- Ein gemeinsamer `BicycleComponents.controlBackground` um Header und geöffneten Inhalt: dieselbe1dp-Kontur, FOREGROUND50%,6dp-Rundung und bisherige SECONDARY-Fläche. Keine zusätzliche Kopfkontur oder Trennlinie beim Öffnen; Rahmen wächst mit dem Inhalt. Gilt in Flat und Glow.
- Chevron über RotateDrawable passend zu ausgeklappt/eingeklappt drehen; gemeinsamer stateDescription. Header bleibt nativer Button mit Tastatur-/Accessibility-Bedienung. Eigene Headerrolle vor generischem Buttonstyling schützen, damit kein zweiter Rahmen entsteht.
- `content` ist der normale vertikale Inhaltscontainer. Zentrale Tokens `dropdownFramePadding=16dp` für links/rechts/unten und `dropdownFrameHeaderGap=24dp` für den Abstand nach dem Kopf. Bestehendes controlSpacing gehört außen an die gesamte Komponente, nicht zwischen Kopf und Inhalt.
- Optional `trailing`: rechts ein normaler bereits gestalteter Switch oder `BicycleComponents.specialFilled`. Deren native Listener/Checked-Zustand unabhängig vom Klappzustand erhalten. Der Switch bekommt bei fehlender Beschreibung den Headertitel als zugänglichen Namen; Label weiterhin links. Keine Aktion über Headerclick auslösen.
- `open` und `changed` steuern nur den bestehenden Klappzustand des jeweiligen Aufrufers. Kein eigenes Preferenceformat, keine neue Speicherung oder fachliche Nebenwirkung.

Beispiel:

```kotlin
val frame = BicycleFrameDropdown(context, themes, "Bauteil", open = expanded,
    leadingIcon = BicycleIcon.Work,
    trailing = BicycleComponents.specialFilled(context, themes, "Aktion") { runAction() }) {
    expanded = it
}
frame.content.addView(existingContent)
```

## Wartung

Bauteilgruppen liegen vollständig im Dropdown-Rahmen, Zuordnung/Reihenfolge/API- und Cacheverhalten unverändert. In jeder Wartungszeile stehen Intervall und „Zuletzt erledigt“ direkt untereinander in einem linken Textblock; dieser und die rechte Aktion sind vertikal mittig ausgerichtet. Aktion nutzt vorhandenen Spezial-Rahmen-Pillenbutton und bleibt wie bisher ohne schreibende Wartungs-API deaktiviert. Trennlinien zwischen mehreren Aufgaben innerhalb desselben Bauteils und sämtliche Fortschritts-/Fälligkeitswerte erhalten.

## Nachweis

Native Tests in `BicycleFrameDropdownTest` prüfen Rahmen/Farbe, wachsende Geometrie, sichtbare Chevrondrehung, generisches Styling, Flat/Glow und unabhängig bedienbare rechte Controls. Bestehende `MaintenancePresentationTest` und `DreeveMaintenanceTest` prüfen weiter Gruppierung, Status/Cache/Retry/Offenzustände; Textblock-/Buttonausrichtung und Rahmen für alle Bauteile gezielt ergänzt. Native Wartungsvorschauen unter app/build/reports/ui/maintenance-flat.png und maintenance-glow.png, mit synthetischen Daten. HTML-Atlas Abschnitt06 enthält alle drei Varianten mit Tokeninspektor. Kein vollständiger Gesamtlauf oder neuer APK-Build.

## Icon und Chevron nach Build 93

Der verpflichtende Parameter leadingIcon setzt ein führendes Fachicon vor den linksbündigen Titel und ein separat bedienbares drehendes Chevron rechts daneben. Eine zusätzliche rechte Aktion oder ein Schalter folgt danach, also Icon · Name · Chevron · Schalter. Eine Variante ohne Icon oder mit Chevron links vom Titel gibt es für Dropdown-Rahmen nicht mehr. Öffnungszustand, rechte Zusatzaktion, Rahmen, Theme-Bindung und Inhaltspadding bleiben erhalten. Backup & Restore nutzt diese Variante für die drei Sicherungen und die HRV-Wiederherstellung; die lokalen/Cloud-Speicherziele nutzen zusätzlich ihre unverändert übernommenen Schalter. Die beiden direkten Wiederherstellungsbuttons verwenden das zentrale RestoreSettings-Icon und linksbündigen Text.

Die Reihenfolge gilt global für diesen Komponententyp, einschließlich der Wartungs-Bauteilgruppen. Wartung verwendet das vorhandene Work-Symbol. Gewöhnliche Sticky-Header und andere Dropdown-Typen werden nicht umgestellt. Native Rand-/Kontur-/Drehungsprüfungen für Flat und Glow sowie unabhängige Controls erfolgreich; HTML-Atlas Abschnitt06 zeigt ebenfalls die neue Reihenfolge.

## Weitere Umstellung nach Build 126

Auf ausdrücklichen Nutzerauftrag verwenden auch die Dashboard-Layoutauswahl, alle vier Trackdarstellungen und „Benachrichtigung anzeigen“ den gemeinsamen Rahmen. Layoutvorschau, Farbauswahl, HEX, Deckkraft, Breite und gespeicherte Auswahl bleiben erhalten. Der Benachrichtigungsschalter bleibt unabhängig vom Chevron/Label. Anzeigedauer steht über der Positionsauswahl; der Messengerfilter ist durch eine horizontale Linie von der App-Liste getrennt. App-Zeilen verwenden rechts native animierte Switches ohne zusätzlichen äußeren Innenabstand.

Die fünf installierten Cloud-Formulare geben ihre eigenen äußeren Paddings an den Rahmen ab; dadurch gelten dessen 16 dp unten statt addierter Innenabstände. Leere POLITE-Live-Statuszeilen sind GONE und werden durch Textänderungen wieder sichtbar, damit Fehler und Fortschritt erhalten bleiben.

Alle drei Wiederherstellungsbereiche sind nun Rahmen-Accordions mit direkt eingebetteter Zielauswahl „Lokal“/„WebDAV“, analog zu den Sicherungen. Der alte zusätzliche Zielauswahldialog entfällt im Bedienweg. Unter der HRV-Zielauswahl folgen Trenner und „HRV4Training-CSV importieren“. Die bestehenden Datei-/WebDAV-Aktionen einschließlich Passwort und Wiederherstellungsbestätigung werden weiterverwendet; Aufklappen löst keine Aktion aus.

Nachweis: sieben gezielte bestehende Tests aus DashboardPagesTest, MapRouteStyleTest, MessageNotificationUiTest und DataSettingsNavigationTest erfolgreich. Sie prüfen Auswahl/Persistenz, unabhängige Aktivierung, Reihenfolge, direkte Zielaktionen, Bestätigung sowie den gemessenen unteren Rahmenabstand und das Wiedereinblenden von Statusmeldungen bei allen fünf Cloud-Formularen. Kein Gesamtlauf oder APK-Build; Gerätesichtprüfung offen.

## Track-Sichtbarkeit

Die vier Trackrahmen heißen „Aktiver Track“ und „User Track 1–3“. Jeder Kopf enthält einen unabhängig vom Aufklappen bedienbaren Sichtbarkeitsschalter. Aktiver Track verwendet unverändert mapTrackVisible; der separate Schalter in den Kartenoptionen entfällt. User-Tracks verwenden die vorhandenen MapRouteSlots-Sichtbarkeiten samt mapGpxVisible-Masterzustand und bleiben damit mit den Augenbuttons gekoppelt. Ausblenden entfernt keine Zuordnung und beeinflusst die Aufzeichnung nicht. Bei leerem Trackplatz kann der Zustand ebenfalls gesetzt werden; das spätere bewusste Zuweisen einer Strecke zeigt sie wie bisher an. Keine neuen Preference-/Backupfelder.

Die Track-Komponente wird nach dem allgemeinen observeSettings-Binding angefügt, damit dieses ihre eigenen Schalter-/HEX-Listener nicht überschreibt. HRV-Einstellungen verwenden für Ruhephase/Messphase BicycleSettingSlider und settingSliderRow; Zeit im Zugpunkt, bisherige Bereiche, 15-Sekunden-Schritte, Bearbeitungssperre und Speicherung bleiben erhalten.
