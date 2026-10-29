# Dashboard-Aufnahmebutton29.10.2026

Zentrale native DashboardRecordButton-Instanz mit unveränderten Click-/LongClick-/Enabled-/Accessibility-Pfaden. Record/Stop-Indikator zentriert an der oberen Kante des rechteckigen Körpers; darunter dieselbe solide PRIMARY-Fläche mit echtem Kreisabschnitt und kleinen verrundeten Anschlüssen. Fahrradicon und Profilname weiterhin im Körper, nur der Record-Indikator aus dem Textspan in eine eigene gezeichnete Position verschoben. Bestehende RecordingButtonIndicator-Animation einschließlich Stop/Pause/Wiederaufnahme erhalten.

DashboardRecordSurface cached eine einzige native Path-Kontur (kein Bitmap, Blur, SVG oder Union je Frame). Zentrale Tokens: Körper58dp, Wölbung14dp, Icon20dp, Rechteckradius12dp. Toolbargewichte0,8/2,5/0,8, damit Mitte etwas breiter. Die14dp Wölbung überlagert den Inhalt über negativen Toolbar-Topmargin; der rechteckige Footer verbraucht weiterhin58dp. Fahrradicon und Name gemeinsam mittig im unteren Körper, feste20sp und Rechtskürzung bei Überlänge; vollständiger Name bleibt zugänglich. Record-Indikator als eigener nativer Foreground, um den internen horizontalen TextView-Scroll kompensiert, damit er auch bei sehr langen Namen ortsfest bleibt. Seitenbuttons58dp, TERTIARY-Hintergrund beginnt erst am Körper. Gesamter Button inklusive Wölbung liegt im nativen Touchziel. Kein zusätzlicher Bedienknopf.

Fahrtzusammenfassung: vor Warn-/Uploadangaben ein zentraler sectionDivider mit unveränderten globalen Abständen; vorhandene E-Bike-Batterietrennung bleibt zusätzlich bestehen.


Veröffentlichung29.10.2026: Diese Änderungen sind in **APK0.8.61/Build102** enthalten. [Download und Buildnachweis](build-102.md). Frühere Angaben zu noch fehlender APK sind Umsetzungshistorie.


## Nach Build102: größerer Kreis, kein Dashboard-Footerschatten

Dashboard-Footer zeichnet in Flat und Glow keinen Schatten mehr. Normale Einstellungsfooter behalten ihre bisherige Darstellung. Der Record-Indikator bleibt20dp; der umgebende Kreis hat60dpDurchmesser (20dpAbstand +20dpIcon +20dpAbstand), entsprechend30dpRadius/Überstand. Die echte Kreisform und ihre kleinen verrundeten Anschlüsse bleiben erhalten. Footerfläche58dp, Iconposition auf der Körperoberkante, zentrierter Name und Bedienung unverändert; größerer Überstand wird weiterhin vollständig vom negativen Topmargin kompensiert.


Veröffentlichung29.10.2026: Schattenfreier Dashboard-Footer und größerer Kreis sind in **APK0.8.62/Build103** enthalten. [Download und Buildnachweis](build-103.md).


NachBuild104: Kreis aufNutzerwunsch10%kleiner:54dpDurchmesser/27dpÜberstand, Record-Symbol weiter20dp. Footerhöhe58dp und Profilnamenanordnung unverändert.
