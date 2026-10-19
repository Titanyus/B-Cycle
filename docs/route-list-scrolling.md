# Streckenliste: Scrollbereich und Schnellnavigation

## Abstand unter dem Header

Der anfängliche pageContentGap gehört zum oberen Innenpadding der nativen ListView, nicht zum festen RouteManagerView-Container. Der Container hat kein oberes Padding. clipToPadding ist false, damit Einträge beim Scrollen den anfänglichen Abstand durchlaufen und direkt unter dem festen Header verschwinden. Der gemeinsame Vollbild-Host ruft dafür über BicycleContentSpacing.page die spezialisierte Anwendung auf. Andere Einstellungsseiten behalten ihre bisherigen Regeln.

Importstatus und leere Liste bleiben als vorhandene Rückmeldung erhalten. Kein alter Filterkasten wird vor die Liste gesetzt; Filter und Sortierung bleiben im Footer.

## Native Schnellnavigation

ListView verwendet den nativen Android-Fast-Scroller am rechten Rand. Er wird bei längeren Listen beim Scrollen sichtbar und lässt sich ziehen; keine eigene Overlay-Geste oder zweite ScrollView. Der Adapter implementiert SectionIndexer und aktualisiert die Sektionen mit dem bereits gefilterten und sortierten Bestand.

- Datum: Monat plus Jahr, lokal formatiert. Gleiche Monate verschiedener Jahre bleiben getrennt.
- Distanz: 10-km-Bereiche.
- Bewegungszeit: Stundenbereiche.
- Durchschnittsgeschwindigkeit: 5-km/h-Bereiche.
- Fehlende Werte: eigener beschrifteter Abschnitt.

Die Reihenfolge folgt exakt der aktuellen Sortierung, auf- wie absteigend. Sprünge führen zur ersten Fahrt des jeweiligen Bereichs. Leere Listen und native Indexanfragen außerhalb des gültigen Bereichs sicher behandeln. Keine neue Sortierart: A–Z wäre eine spätere Namenssortierung.

Gezielte Tests prüfen Abschnittslabels/-positionen, Sortierrichtung, gefilterte Teilmengen, Zahlenbereiche und fehlende Werte sowie Seitenabstand und Vollbild-Layout. Physische Bedienung/Optik des nativen Fast-Scrollers auf dem Gerät noch zu testen. Keine APK für diese Änderung angefordert.
