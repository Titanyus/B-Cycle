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
