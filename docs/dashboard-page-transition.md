# Gleitender Dashboardwechsel

Der gesamte Dashboardbereich samt Karte und Kartenbuttons bewegt sich; Footer, Statuszeile und funktionaler Fenster-Glow bleiben fest. Genau eine lebende MapView, keine zusätzlichen Sensorverbindungen oder Kartenquellen.

## Direkte Gesten – 01.11.2026

Nach dem HRV-Umbau umgesetzt: Schon ein kurzer horizontaler Drag auf den Dashboard-Datenfeldern verschiebt den kompletten Bereich direkt um die Fingerbewegung. Die erste vom Elterncontainer abgefangene Bewegung wirkt sofort; nicht erst auf das nächste Move-Ereignis warten. Aktivierung nach Android-Touchslop, klare horizontale Richtung erforderlich. Vertikale Gesten, Kartengesten, normale Klicks, Longpress/Editor und Footer bleiben erhalten. Im Editor oder bei offenem Dialog kein Seitenwischen.

Nach48dp bzw.15% der Viewportbreite (der kleinere Wert, mindestens dreifacher Touchslop) übernimmt die Automatik bereits während der Bewegung und vollendet den Wechsel. Ein kürzerer schneller Flick kann beim Loslassen ebenfalls wechseln (mindestens24dp, mindestens400dp/s in Wischrichtung). Nicht ausreichender Drag oder Abbruch geht in180ms auf die Ausgangsposition zurück; kein Seitenwechsel. Mehrfingerbeginn beendet einen noch nicht übernommenen Drag. Keine Folgewisch-Warteschlange während des Übergangs.

Die anschließende Animation setzt exakt an der sichtbaren Dragposition fort. Dauer proportional zum verbleibenden Weg, maximal300ms, weiches Abbremsen ohne Zoom/Bounce. Programmatische Wechsel ohne Drag weiterhin300ms. Systemanimation aus: Wechsel unmittelbar, ohne Snapshot; abgebrochener Drag sofort zurücksetzen.

## Darstellung und Lebenszyklus

DashboardPageViewport enthält einen lebenden Dashboard-/Kartenbaum. Erst bei Übernahme des Seitenwechsels wird ein kurzlebiges transparentes ARGB-Bild der bisherigen Seite erfasst, maximal vier Millionen Pixel (16MB). Bei reinem Fingerfolgen oder zurückgenommenem Drag kein Bitmap. Neue Seite mit passenden Datenfeldern und Kartenhöhe aufbauen; ein gemeinsamer ValueAnimator bewegt altes Bild und lebenden Bereich mit einem Seitenabstand hinaus/hinein. Transparente Bildteile erhalten den funktionalen Fensterhintergrund.

Am Ende/Abbruch Listener, Transformation, Bild und Bitmap freigeben. App-/Fokuswechsel, Detach und Breitenänderung setzen sauber auf die bereits ausgewählte Seite zurück. Ohne Layout oder sichere Aufnahme unmittelbar wechseln. Editor-/Konfigurationsaktualisierungen mit Delta0 weiterhin sofort.

## Gezielte Prüfung

Native Tests: erste kurze Bewegung folgt direkt; Übernahme vor Loslassen; Fortsetzung ohne Sprung und maximal300ms; keine zusätzlichen Wechsel während Bewegung; kurzer/abgebrochener Drag ohne Snapshot zurück; vertikale/Kartenbereiche unverändert; Freigabe/Detach/Animation aus. Tatsächlicher Activity-Wechsel per Geste mit unveränderter MapPanel-Instanz und wechselnder Kartenhöhe, Footer fest. Bestehende Longpress-/Editor- und Kartenbedienung gezielt geprüft. Kein physischer Gerätetest.

Erster Lauf neun relevante Fälle: sechs bestanden, drei neue Dragfälle machten die verzögerte erste Move-Verarbeitung sichtbar. Nur diese drei nach Korrektur erneut geprüft; Ergebnis in `dashboard-direct-drag-final-tests.log`. Kein Gesamtlauf.

[Native Zwischenansicht](previews/dashboard-page-transition.png) zeigt den bisherigen Übergang mit synthetischen Daten ohne geladene Kartenkacheln. Ursprüngliche Animation veröffentlicht in [Build110](build-110.md); direktes Dragverhalten lokal nach Build116.
