# Gleitender Dashboardwechsel

Der gesamte Dashboardbereich samt Karte und Kartenbuttons bewegt sich; Footer, Statuszeile und funktionaler Fenster-Glow bleiben fest. Genau eine lebende MapView, keine zusätzlichen Sensorverbindungen oder Kartenquellen.

## Direkte Gesten – Stand02.11.2026

Nach dem HRV-Umbau umgesetzt: Schon ein kurzer horizontaler Drag auf den Dashboard-Datenfeldern verschiebt den kompletten Bereich direkt um die Fingerbewegung. Die erste vom Elterncontainer abgefangene Bewegung wirkt sofort; nicht erst auf das nächste Move-Ereignis warten. Aktivierung nach Android-Touchslop, klare horizontale Richtung erforderlich. Vertikale Gesten, Kartengesten, normale Klicks, Longpress/Editor und Footer bleiben erhalten. Im Editor oder bei offenem Dialog kein Seitenwischen.

Die Seite bleibt bis zum Loslassen vollständig unter Fingerkontrolle, ohne automatische Übernahme während MOVE. Langsames Ziehen darf die Seite bis zu einer ganzen Breite hinausschieben; auch Zurückziehen bleibt möglich. Erst UP entscheidet: mindestens24dp tatsächlicher Drag und mindestens eine halbe Seitenbreite als Summe aus Drag und Geschwindigkeitsprojektion. Bei einem schnellen Wischer (mindestens400dp/s bzw. Android-Mindestfling, Geschwindigkeit auf Android-Maximum begrenzt) werden200ms Bewegung in Wischrichtung vorausgerechnet. Bei langsamen Gesten zählt nur der tatsächliche Weg. Ein schneller Wischer zurück zur Ausgangsposition bricht den Wechsel auch jenseits der halben Breite ab. Die letzte UP-Position wird übernommen. Nicht ausreichender Drag, Cancel oder Mehrfingerbeginn geht in180ms auf die Ausgangsposition zurück; kein Seitenwechsel. Keine Folgewisch-Warteschlange während des Übergangs.

Bei vollständig manuell hinausgeschobener Seite ist nach Bereitstellung der Folgeseite keine Restanimation nötig. Ansonsten setzt die anschließende Animation exakt an der sichtbaren Dragposition fort. Dauer proportional zum verbleibenden Weg, maximal300ms, weiches Abbremsen ohne Zoom/Bounce. Programmatische Wechsel ohne Drag weiterhin300ms. Systemanimation aus: Wechsel unmittelbar, ohne Snapshot; abgebrochener Drag sofort zurücksetzen.

## Darstellung und Lebenszyklus

DashboardPageViewport enthält einen lebenden Dashboard-/Kartenbaum. Erst bei Übernahme des Seitenwechsels wird ein kurzlebiges transparentes ARGB-Bild der bisherigen Seite erfasst, maximal vier Millionen Pixel (16MB). Bei reinem Fingerfolgen oder zurückgenommenem Drag kein Bitmap. Neue Seite mit passenden Datenfeldern und Kartenhöhe aufbauen; ein gemeinsamer ValueAnimator bewegt altes Bild und lebenden Bereich mit einem Seitenabstand hinaus/hinein. Transparente Bildteile erhalten den funktionalen Fensterhintergrund.

Am Ende/Abbruch Listener, Transformation, Bild und Bitmap freigeben. App-/Fokuswechsel, Detach und Breitenänderung setzen sauber auf die bereits ausgewählte Seite zurück. Ohne Layout oder sichere Aufnahme unmittelbar wechseln. Editor-/Konfigurationsaktualisierungen mit Delta0 weiterhin sofort.

## Gezielte Prüfung

Native Tests: erste kurze Bewegung folgt direkt; Übernahme vor Loslassen; Fortsetzung ohne Sprung und maximal300ms; keine zusätzlichen Wechsel während Bewegung; kurzer/abgebrochener Drag ohne Snapshot zurück; vertikale/Kartenbereiche unverändert; Freigabe/Detach/Animation aus. Tatsächlicher Activity-Wechsel per Geste mit unveränderter MapPanel-Instanz und wechselnder Kartenhöhe, Footer fest. Bestehende Longpress-/Editor- und Kartenbedienung gezielt geprüft. Kein physischer Gerätetest.

Erster Lauf neun relevante Fälle: sechs bestanden, drei neue Dragfälle machten die verzögerte erste Move-Verarbeitung sichtbar. Nur diese drei nach Korrektur erneut geprüft; Ergebnis in `dashboard-direct-drag-final-tests.log`. Kein Gesamtlauf.

[Native Zwischenansicht](previews/dashboard-page-transition.png) zeigt den bisherigen Übergang mit synthetischen Daten ohne geladene Kartenkacheln. Ursprüngliche Animation veröffentlicht in [Build110](build-110.md); direktes Dragverhalten lokal nach Build116.


## Nach Build117: weicher Gestenbeginn und Geschwindigkeitsübergang

Beim Erkennen einer horizontalen Geste den Android-Touchslop vom sichtbaren Weg abziehen. Die Seite beginnt dadurch an der Schwelle bei0, statt um den gesamten seit Touch-Down gesammelten Weg zu springen. Übernahme speichert die aktuelle Fingergeschwindigkeit. Eine monotone kubische Hermite-Kurve setzt diese anfängliche Geschwindigkeit fort und kommt ohne Überschwingen mit Geschwindigkeit0 an; Dauer an Restweg/Geschwindigkeit angepasst, weiterhin höchstens300ms. Programmatische Wechsel ohne Drag behalten ihre bisherige Kurve. Abbruch weiterhin180ms zurück.

Die Komponente wird auch für die HRV-Tagesansicht mit Wrap-Content verwendet. Nicht klickbare Kreisflächen müssen die Bewegung im eigenen Touchhandler erkennen, da Android nach eigenem DOWN keine weitere Elterninterzeption ausführt. Lokale Datenvorbereitung kann die ausgehende Seite kurz halten, statt einen Ladehinweis hereinzuschieben. Focusverlust/Detach/Cancel räumt auf.

Drei gezielte Übergangsfälle erfolgreich in `page-smooth-handoff-tests.log`: Geschwindigkeit am Start, ruhendes Ende/Monotonie, tatsächlicher Fingerübergang und Abbruch. Fünf der sechs integrierten HRV-/Dashboard-/Header-/Scrollfälle sofort erfolgreich in `hrv-day-swipe-smooth-pages-tests.log`; der neue HRV-Gestenfall nach Korrektur der nicht klickbaren Fläche und passender Testfenstergröße erfolgreich in `hrv-day-swipe-final-test.log`. Kein Gesamtlauf oder physischer Gerätetest.

## VTM-Anbindung nach Build119

Die GL-Fläche wird vor dem bisherigen Seitenbild asynchron über PixelCopy vorbereitet: begrenzt auf1Mio.Pixel/4MB und200ms, keine zweite lebende Dashboardkarte. Danach zeichnet die GL-View ihre vorbereitete Aufnahme in das bestehende Software-Seitenbild (weiterhin4Mio.Pixel/16MB), und die GL-Aufnahme wird freigegeben. Alte Seite während Vorbereitung halten; nach Cancel/Fokusverlust verspätete Resultate ignorieren; bei fehlender Aufnahme sofort wechseln. HRV-Seiten ohne GL-Fläche verwenden weiterhin den direkten bestehenden Weg. [Umstellung](vtm-migration.md).


## Prüfung der Freigabegeste02.11.2026

Gezielt: Fingerkontrolle ohne frühen Commit/Geschwindigkeitsübergang; langsames vollständiges Hinausschieben ohne Restanimation; langsamer/rascher identischer kurzer Weg, langsamer Weg über die Hälfte und schnelle Richtungsumkehr; kurze/abgebrochene/vertikale/Kartengesten; echte Dashboard- und HRV-Integration. Ergebnisse in `page-release-gesture-tests.log`, `page-release-integration-tests.log` und `page-release-hrv-final.log` unter `/workspace/.tools/`. Alle sechs Fälle nach Anpassung der beiden Integrationstests erfolgreich; Kompilation/Typografie/Atlas/Diff ebenfalls. Die HRV-Testdaten geben ISO-Datumsreihenfolge explizit an, damit die Monatsanfangsdaten nicht gemäß HRV4-Standard als Jahr-Tag-Monat gelesen werden; Produktionsimport unverändert. Kein Gesamtlauf, keine APK. Geräteeindruck noch prüfen. Dashboardintegration simuliert nur die erfolgreiche GL-Aufnahmebereitschaft, da Robolectric keine echte PixelCopy-Surface hat; die separate bestehende Snapshot-Prüfung bleibt erhalten.

## Nach Build121: sichtbare Nachbarseite während des Ziehens

Nutzer: Wischgefühl deutlich besser, aber bis UP bleibt neben der hinausgeschobenen Seite Schwarz. Gemeinsame Komponente ergänzt um `PagePreview`: Die Nachbaransicht steht bereits beim Drag um eine Seitenbreite versetzt bereit und bewegt sich im gleichen Maß. Richtungswechsel tauscht die Vorschau aus. Beim Zurückgleiten bewegt sie sich mit; nach Abbruch, Fokusverlust, Detach oder Abschluss entfernen/freigeben. Verspätete Vorschauantworten über eigene Generation verwerfen. Aktive Auswahl/Seitenpräferenz, HRV-Datum und Diagrammauswahl ändern sich weiterhin erst beim Loslassen/Commit.

Dashboard: Native schreibgeschützte Kopie der Nachbarseite mit deren Layout/Feldern/Diagramm-/Trainingshilfe-Konfiguration und aktuellen gemeinsamen Laufzeitwerten. Keine zweite Sensor-/Runtime-/Karteninstanz. Hat die aktuelle Seite eine Karte, wird deren GL-Inhalt plus Bedienelementen als getrennt besessene Aufnahme (höchstens1Mio.Pixel/4MB) in der Nachbaransicht gezeigt; bei anderer Kartenhöhe vorübergehend CENTER_CROP. Die Vorschau ist damit eine Momentaufnahme, keine zweite gleichzeitig bedienbare Karte. Hat die aktuelle Seite keine Karte, wird die einzige bestehende Karte für die Vorschau umgehängt und bei Abbruch/Commit zuverlässig zurückgesetzt. Map-Folgen/Anzeige darf währenddessen weiterlaufen; Sensoraufzeichnung bleibt unabhängig.

HRV: Nachbartag auf bestehendem HRV-Worker laden, im begrenzten Tagescache behalten, mit derselben Tagesauswertungs-View darstellen. Keine Änderung von Header, ausgewähltem Tag oder Verlaufsdiagramm beim bloßen Ziehen. Außerhalb erster Tag/heute keine Vorschau.

GL-Vorschauaufnahme besitzt ihr Bitmap unabhängig von der ausgehenden Übergangsaufnahme. Rasche Richtungswechsel bündeln Vorschauaufnahmen auf eine laufende PixelCopy-Anfrage mit neuestem Empfänger. Übergangsaufnahme zusätzlich per Generation gegen verspätete alte Antworten geschützt. Nach erfolgreicher Aufnahme die Vorschau entfernen/freigeben, dann bestehenden Übergang ab exakt derselben Position ausführen. Vorschau für Accessibility ausgeblendet, Footer/Status bleiben außerhalb.

Sechs gezielte Fälle erfolgreich in `/workspace/.tools/page-neighbor-tests.log`: sichtbare farbige Nachbarseite vor UP mit gemeinsamer Rückkehr/Richtungswechsel; verspätete Vorschau nach Abbruch; genau eine umgehängte Karte bei Abbruch und Commit aus einer Seite ohne Karte; echte Dashboardintegration ohne vorzeitige Präferenzänderung; bisheriger asynchroner GL-Snapshot-Abbruch; HRV-Nachbarvorschau bei unverändertem aktiven Tag/Chart bis UP. Kompilation/Typografie/Atlas/Diff erfolgreich. Kein Gesamtlauf/keine APK. Echtes GL-Umhängen, Snapshot-Darstellung bei verschiedenen Kartenhöhen und Gefühl auf Android noch prüfen.

## Nach Build 122: ausgehende Karte nach Loslassen erhalten

Nutzerpräzisierung: Die neue Karte gleitet herein, aber die alte verschwindet beim Loslassen. Ursache im Bildtest reproduziert: VTM setzt auf seiner SurfaceView `setWillNotDraw(true)`. Beim Zeichnen des übergeordneten Dashboardbaums überspringt Android deshalb `BicycleMapView.draw(Canvas)` und damit das bereits per PixelCopy vorbereitete Kartenbild. Die ausgehende Seite enthielt im Kartenbereich kein Bild.

Minimale Korrektur: `BicycleMapView` setzt `setWillNotDraw(false)`, sodass die bestehende Software-Snapshot-Zeichenroutine auch innerhalb des Elternbaums aufgerufen wird. Hardwarezeichnung bleibt bei SurfaceView/OpenGL; keine zweite Karte, keine zusätzliche Aufnahme/Animation oder längere Bitmaphaltung. Die alte Karte ist damit Teil desselben ausgleitenden Seitenbilds wie die Datenfelder.

Gezielter neuer Bildtest zunächst mit fehlendem Kartenpixel fehlgeschlagen, nach Korrektur erfolgreich: verschachtelte reale BicycleMapView, injizierte GL-Aufnahme, Finger-UP, Aufnahmefreigabe und sichtbarer Kartenpixel während der Restanimation, Bitmapfreigabe am Ende. Zusätzlich bestehende asynchrone Snapshot-/Abbruchprüfung sowie Umhängen aus kartenloser Seite bei Abbruch/Commit erfolgreich. Drei Fälle, 0 Fehler; Log `/workspace/.tools/page-outgoing-map-fix-tests.log`. Robolectric prüft die Android-Zeichenroutine mit vorbereiteten Pixeln, keinen echten Geräte-PixelCopy/Compositor. Keine neue APK gebaut.

## Nach Build 124: Footer und echte Kartenmaße bereits in der Vorschau

Nutzerpräzisierung: Bei eingeblendetem Footer kommen neue Karte/Bedienelemente in falscher Größe herein und springen beim Loslassen in die footerberücksichtigende Anordnung. Bisher enthielt die Nachbarseite bei Kartenwechseln ein CENTER_CROP-Bild des gesamten alten MapPanel einschließlich Bedienelementen. Bei anderer Zielkartenhöhe passt diese Aufnahme nicht zu den nativen Zielmaßen. Zusätzlich erzwang showDashboardPage über showFullscreen/revealControls beim Loslassen erneut einen sichtbaren Footer; ein verborgener Footer wurde unerwartet eingeblendet.

Jetzt ist bei jeder Nachbarseite mit Karte die einzige vorhandene MapPanel bereits Teil der einlaufenden Seite und wird dort nativ in deren Zielhöhe gemessen. Die bisherige Kartenfläche wird währenddessen durch eine begrenzte Aufnahme an ihrer ursprünglichen Position ersetzt; kein zugeschnittenes Komplettbild als einlaufende Karte. Bei Commit/Abbruch die reale Karte zurücksetzen, Standbild entfernen und dessen Bitmap freigeben. Keine zweite Karten-/Sensorinstanz. Der bestehende Footer-/Viewportabzug bestimmt weiter die Daten- und Kartenmaße. Beim Wiederaufnehmen während der Vorschau und beim normalen Dashboardcommit Footer-Zustand erhalten; Vollbildaktionen zeigen Bedienelemente weiterhin wie bisher. Sichtbare Controls bekommen ihre bisherige automatische Ausblendplanung zurück. Dashboards ohne Karte behalten den erzwungen sichtbaren Footer.

Vor dem Umhängen verspätete Aufnahmen über Anfragegeneration, aktive Wischrichtung, aktuelle Seite/Profil, Fenster-/Runtime-/Dialogzustand verwerfen. Nach Loslassen/Abbruch darf eine ausstehende Aufnahme keine Karte neu umhängen. Ausgehende Seitenaufnahme verwendet bei umgehängter Karte das bereits vorhandene Standbild statt einer zweiten Aufnahme der einlaufenden GL-Fläche.

Vier eindeutige gezielte Fälle erfolgreich: echte Karten-/Zoom-Control-Maße vor/nach Commit mit sichtbarem und verborgenem Footer, jeweils auch Abbruch/Restaurierung und genau einer Karteninstanz; bestehender Wechsel aus kartenloser Seite bei Abbruch/Commit; bestehende echte Dashboard-/Footerintegration; verspätete Vorschau nach Abbruch. Der neue Integrationstest hat das unerwartete Footereinblenden vor der Korrektur reproduziert. Logs `/workspace/.tools/dashboard-footer-before-test.log`, `dashboard-footer-final-tests.log` und `dashboard-footer-late-result-tests.log`. Nach dem Schutz gegen verspätete Ergebnisse nur zwei betroffene Fälle wiederholt. Kompilation/Typografie/Atlas/Diff erfolgreich, kein Gesamtlauf. In [APK 0.8.84 / Build 125](build-125.md) enthalten. Physischer GL-Neuanschluss und Übergang am Smartphone nach nächstem Build noch prüfen; Robolectric prüft Layout/Lebenszyklus und native Bedienelementmaße, keine laufende Geräte-GL-Surface.
