# Gleitender Dashboardwechsel

Beim bestehenden horizontalen Wischen auf den Dashboard-Datenfeldern gleitet der gesamte Dashboardbereich einschließlich Karte und Kartenbuttons in Wischrichtung. Der Footer bleibt außerhalb des Viewports fest stehen; Statuszeile und funktionaler Fenster-Glow bleiben ebenfalls am Fenster. Dauer 300 ms bei normaler Android-Animationsskala, weiches Abbremsen ohne Zoom-/Bounceeffekt.

DashboardPageViewport enthält genau einen lebenden Dashboard-/Kartenbaum. Vor dem Wechsel wird ein kurzlebiges transparentes ARGB-Bild des bisherigen Bereichs erfasst, maximal vier Millionen Pixel (16 MB). Die bestehende Seite wird mit ihren neuen Einstellungen und ihrer passenden Kartenhöhe aufgebaut. Nach deren Layout bewegen ein gemeinsamer ValueAnimator und eine gemeinsame Zeitachse den alten Bildausschnitt hinaus und den lebenden Bereich hinein. Keine zweite MapView, Sensorverbindung, Kartenquelle oder Dateidekodierung. Transparente Bildteile erhalten den funktionalen Fensterhintergrund.

Der Zwischenstand und Bitmap bleiben nur im RAM. Am Ende/Abbruch werden Listener entfernt, die Transformation zurückgesetzt und Bild/Bitmap freigegeben. Appwechsel, Fokusverlust, Ablösen des Viewports und Breitenänderungen brechen sauber auf die bereits gewählte neue Seite ab. Weitere schnelle Flings während der 300 ms werden nicht aufgestaut; der feste Footer bleibt bedienbar. Kartengesten außerhalb des Übergangs unverändert.

Bei deaktivierten Android-Animationen, vor dem ersten Layout oder wenn keine sichere Bildaufnahme möglich ist, erfolgt der Wechsel unmittelbar. Editor-, Konfigurations- und Profilaktualisierungen mit Delta 0 bleiben sofort; Animation nur bei einer tatsächlichen anderen Dashboardseite.

Gezielte Native-Tests prüfen Richtung, Zwischenpositionen mit gemeinsamem Abstand, 300-ms-Dauer, festen Footer, Freigabe nach Ende/Cancel/Detach, keine Bitmap bei deaktivierter Animation und den tatsächlichen Activity-Wechsel mit/ohne Karte bei unveränderter MapPanel-Instanz. Editor-/Konfigurationsspeicherung ebenfalls geprüft. Der Test taktet den tatsächlichen ValueAnimator gezielt, statt sich auf automatische Robolectric-Vsync-Zeitsprünge zu verlassen. Kein neuer physischer Gerätetest.

[Native Vorschau eines Zwischenstands](previews/dashboard-page-transition.png): synthetische Daten, in der Testumgebung keine geladenen Kartenkacheln.


Veröffentlicht in [APK 0.8.69 / Build 110](build-110.md). Ältere Angaben zu noch fehlender APK sind Umsetzungshistorie.
