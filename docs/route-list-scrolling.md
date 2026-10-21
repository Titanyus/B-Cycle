# Streckenliste: Scrollbereich und Schnellnavigation

## Abstand unter dem Header

Der anfängliche pageContentGap gehört zum oberen Innenpadding der nativen ListView, nicht zum festen RouteManagerView-Container. Der Container hat kein oberes Padding. clipToPadding ist false, damit Einträge beim Scrollen den anfänglichen Abstand durchlaufen und direkt unter dem festen Header verschwinden. Der gemeinsame Vollbild-Host ruft dafür über BicycleContentSpacing.page die spezialisierte Anwendung auf. Andere Einstellungsseiten behalten ihre bisherigen Regeln.

Importstatus und leere Liste bleiben als vorhandene Rückmeldung erhalten. Kein alter Filterkasten wird vor die Liste gesetzt; Filter und Sortierung bleiben im Footer.

## Gemeinsame Schnellnavigation

`BicycleFastScrollList` erhält die native ListView samt Recycling und Accessibility und ergänzt eine gemeinsame Schnellnavigation am rechten 24-dp-Touchstreifen. Sie wird in Streckenmanager, Dreeve- und ZIP-Auswahllisten verwendet. Der kompakte Vorschaukasten ist maximal 180 × 40 dp groß, Hintergrund FOREGROUND, Schrift SECONDARY; Themewechsel wirken unmittelbar. Kein Zugriff auf private Android-Fastscroller-APIs und keine zusätzliche ScrollView. SectionIndexer bleibt auch nach dem zentralen Adapter-Styling erhalten.

- Datum: Monat plus Jahr, lokal formatiert. Gleiche Monate verschiedener Jahre bleiben getrennt.
- Distanz: 10-km-Bereiche.
- Bewegungszeit: Stundenbereiche.
- Durchschnittsgeschwindigkeit: 5-km/h-Bereiche.
- Fehlende Werte: eigener beschrifteter Abschnitt.

Die Reihenfolge folgt exakt der aktuellen Sortierung, auf- wie absteigend. Der Griff springt proportional in den sichtbaren Bestand; die Vorschau nennt den zugehörigen Bereich. Leere Listen und native Indexanfragen außerhalb des gültigen Bereichs sicher behandeln. Keine neue Sortierart: A–Z wäre eine spätere Namenssortierung.

Gezielte Tests prüfen Abschnitte, Filter-/Sortieraktualisierung, Griffnavigation und native Vorschaugröße/Farben bei Themewechsel. Physische Bedienung am Gerät zusätzlich prüfen.
