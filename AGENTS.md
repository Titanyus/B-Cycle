# Entwicklungsregeln

1. Entwickle in Kotlin für Android 13+ (minSdk 33).
2. Priorisiere Performance, geringen Ressourcenverbrauch und schnelle Reaktionen.
3. Behalte die bestehende Single-Activity-Struktur mit nativen Kotlin-Views und getrennten Sensor-/Datenkomponenten bei, sofern keine wichtigen technischen Gründe dagegensprechen.
4. Implementiere neue Funktionen möglichst schlank.
5. Vermeide unnötige Bibliotheken, Abhängigkeiten und Refactorings.
6. Verwende vorhandene Komponenten und Funktionen wieder.
7. Prüfe nach Änderungen möglichst die Kompilierung und relevante Tests; nutze dafür die vorhandenen Gradle-Tasks.
8. Ändere funktionierende Bereiche nur, wenn es für die Aufgabe notwendig ist.
9. Arbeite schrittweise und konzentriere dich auf die angeforderte Funktion.
10. Schließe mit einer kurzen, verständlichen Zusammenfassung der Änderungen und Testergebnisse; benenne ungeprüfte Punkte.

UI: Verwende für neue Dialoge die zentralen BicycleDesign-/BicycleComponents-/BicycleDialog-Komponenten, ThemeManager und BicycleIcons; keine eigenen Farb- oder Geometrievarianten. Einstellungsansichten verwenden den Vollbild-Host mit fixem Header/Footer; kurze Bestätigungen bleiben kompakt. Navigation: jeder Dialog hat im Header links den Zurück-Chevron; Titel sind unabhängig davon zur gesamten Fensterbreite zentriert. Speichern/Abbrechen/Schließen entfallen als Footer-Navigation; nur fachliche Aktionen bleiben dort. Gültige Einstellungsänderungen werden automatisch gespeichert; ungültige bzw. unvollständige Eingaben überschreiben keine gültigen Einstellungen. Destruktive Aktionen behalten eine Bestätigung. Abschnittsüberschriften verwenden appweit 23 sp (BicycleDesign.titleSp), wie die Streckendetails, und den zentralen strongTypeface mit Gewicht 900 Black (Roboto über Android-Systemfont, soweit verfügbar). Alle bisherigen System-Fettschriften verwenden diesen zentralen Schnitt; Bebas-Neue-Messwerte und Fenstertitel bleiben erhalten. Funktionale Bosch-Modusfarben bleiben vom App-Theme getrennt.

Auswahlkreise (Checkboxen und RadioButtons) verwenden BicycleDialogStyle: äußere Kontur und Beschriftung FOREGROUND, aktive Mitte ACCENT, inaktive Mitte transparent. Aktive Schalter verwenden ACCENT für den Daumen und eine abgeschwächte ACCENT-Spur; ausgeschaltet FOREGROUND wie bisher. Deaktivierte Controls bleiben gedimmt. Mehrfach-/Einfachauswahl, Listener, Touchflächen und native Radio-Platzbreite erhalten. Farben müssen bei Themewechsel sofort folgen.

## Einheitliche Navigation und Zustands-Icons

Jeder Einstellungs-Menüpunkt öffnet einen eigenen Unterdialog. „Erweitert“ ist ein separater Screen, kein Accordion. Im Haupt-Einstellungsmenü verwenden die Zeilen 72 dp Mindesthöhe, die Menütexte die gemeinsame Abschnittstypografie (BicycleDesign.titleSp, 23 sp / 900 Black) und führende Icons 32 dp; Untermenüs verwenden die normale Listenhöhe und Typografie. Header-Chevron und Android-Zurück führen zum vorherigen Screen und erhalten dessen Scrollposition. Ein Footer ohne fachliche Aktionen wird vollständig entfernt, inklusive seines Platzbedarfs.

Umschalt-Icons zeigen **den aktuellen Zustand**, nicht die nächste Aktion. Beispiele: helle Karte = sun, dunkle Karte = moon; Norden oben = compass, Fahrtrichtung oben = arrow-up-from-dot; Passwort verborgen = eye-closed, sichtbar = eye. Reine Befehls-Icons (Import, Löschen usw.) bleiben Aktionssymbole. Accessibility-Beschreibungen dürfen zusätzlich die nächste Aktion erklären.

## Ausrichtung und Dashboard-Bearbeitung

Die App-Oberfläche wird ausschließlich im Hochformat verwendet. Layout- und UI-Tests prüfen unterschiedliche Hochformat-Größen; keine neuen Querformat-Fälle hinzufügen. Bestehende responsive Komponenten nicht ohne konkreten Auftrag umbauen.

Dashboardfelder werden nur im bewusst aktivierten Editor bearbeitet: Long-Press auf eine obere Kachel oder „Design & UI → Dashboard bearbeiten“. Kartenfelder haben keinen Long-Press-Einstieg. Der gemeinsame Editor verwendet PRIMARY-Rahmen und den zentralen Beenden-Button auf der Karte. Keine Stillstandssperre für diese Bearbeitung ergänzen; bestätigte Änderungen verwenden die bestehende Persistenz.

## Grundsätzliche UX- und Konfigurationsphilosophie

BICYCLE ist ein leistungsfähiger, individuell anpassbarer Fahrradcomputer. Seine Bedienung bleibt trotz umfangreicher Funktionen einfach, logisch und übersichtlich.

**Sinnvolle Standards. Gezielte Individualisierung. Keine unnötige Konfiguration.**

- Für technische Darstellungs- und Berechnungsparameter sinnvolle Standardwerte verwenden.
- Keine zusätzlichen Einstellungen, Regler oder Schalter einführen, nur weil etwas technisch konfigurierbar wäre.
- Nur Optionen anbieten, bei denen unterschiedliche Nutzer tatsächlich unterschiedliche Anforderungen haben.
- Einstellungen unmittelbar dort platzieren, wo die zugehörige Funktion verwendet oder konfiguriert wird; keine tief verschachtelten Strukturen.
- Vorhandene Einstellungen wiederverwenden, statt ähnliche Optionen mehrfach anzubieten.
- Neue Funktionen müssen mit ihren Standardwerten unmittelbar sinnvoll funktionieren.
- Fortgeschrittene Möglichkeiten dürfen die alltägliche Bedienung nicht belasten.
- Graphenglättung, Rendering-Intervalle und Skalierungsstufen bleiben interne Implementierungsdetails.
- Standards anhand realer Fahrten verbessern, statt die Verantwortung für technische Feinabstimmung dem Nutzer zu übertragen.
- Keine vorhandenen Einstellungsmöglichkeiten eigenmächtig entfernen. Vereinfachungen zunächst vorschlagen und abstimmen.

Vor jeder neuen Konfigurationsoption prüfen: Hat sie klaren praktischen Nutzen? Gibt es unterschiedliche legitime Nutzungsbedürfnisse? Ist sie verständlich? Ist ihr Effekt unmittelbar nachvollziehbar? Ist sie logisch auffindbar? Wenn diese Kriterien nicht erfüllt sind, einen sinnvollen internen Standard bevorzugen.

Produktziel: Flexibilität eines leistungsfähigen Fahrradcomputers mit der Klarheit und unmittelbaren Bedienbarkeit einer durchdachten, reduzierten Oberfläche. **Nicht maximale Konfigurierbarkeit, sondern maximale Nutzbarkeit.**

Trainingszonen: Coggan aus persönlichem FTP und bestehende Karvonen-Zonen zentral berechnen. Dashboard-Trainingsfelder und Zonen-Glow sind ausschließlich Darstellung; keine automatischen FIT-Felder oder Änderungen an Performance-/Sensorverarbeitung daraus ableiten. Aktuelle Zonen verwenden den zeitgewichteten 3-Sekunden-Anzeigepuffer, Ø-Quellen die vorhandenen Durchschnittswerte. Glow-Auswahl je Seite/Kachel ist unabhängig von Hauptwert, Graphen, Bosch-Glow und Akkuwarnung; Details in docs/training-zones-glow.md.

Dashboard-Feldauswahl: getrennte Accordions Daten, Graph und Visualisierung. Sichtbarkeit des Hauptwerts ist unabhängig von Graphauswahl und Zonen-Glow; keine Graph-Darstellung-Auswahl mehr. Keine Daten blendet Wert/Einheit/Beschriftung aus und erhält die Feld-ID. Neuere Darstellungs-/Migrationsregeln stehen in docs/independent-dashboard-layers.md.

Formularabstände: BicycleDesign.controlGap (8 dp) über BicycleComponents.controlSpacing und BicycleDialogStyle verwenden. Formularbuttons inklusive Auswahl-/Schaltercontrols und Dropdowns bekommen mindestens diesen Außenabstand; nebeneinanderstehende Controls vorhandene End-/Startabstände berücksichtigen, keine doppelten Gaps. Spacing nach finalem addView erneut anwenden; Layoutmaße/Gewichte erhalten. Karten-/Toolbar-/Header-Iconcontrols behalten ihre Spezialgeometrie. Bestandsaufnahme weiterer dezentraler UI-Regeln: docs/ui-conventions-audit.md.

Scrollkonvention: BicycleComponents.scrollStyle und BicycleDesign-Scrolltokens verwenden. Header und fachliche Footer bleiben außerhalb des scrollenden Inhalts; vorhandene ScrollViews/ListViews nicht erneut umwickeln. Keine Scrollpositionen durch Styling zurücksetzen. Die Feldauswahl behält eine begrenzte innere Liste unter der Quellenwahl (55 % der verfügbaren Höhe, 144–360 dp; initial 320 dp); diese Maße liegen zentral in BicycleDesign. Adapter, Gesten, Kategorienwechsel und Tastatur-/Inset-Verhalten erhalten.

Aktionsfooter: bicycleFooter und BicycleFooterAction verwenden (64 dp Höhe, 24-dp-Icon, kleine 11-sp-Beschriftung zentriert darunter). Icons und Texte verwenden FOREGROUND, Erfolgs-/Busy-Zustände behalten ihre Semantik. Streckendetails: Anzeigen, Höhe korrigieren, Cloudupload, Speichern, Löschen. Umbenennen über antippbaren Titel und Stiftsymbol rechts im Header; Titel weiterhin zur gesamten Fensterbreite zentrieren. Manuelle Höhenkorrektur für eigene FIT-Fahrten mit GPS unabhängig vom Automatikschalter, während Aufnahme/Speichern/Verarbeitung gesperrt. Streckenmanager: Importicon mit „Strecke importieren“, links im gleichen Footer. Keine zusätzliche Footer-Navigation oder leere Footer einführen.

Sekundäre Kennzahlentexte: Einheiten und daran angebundene Dashboard-Beschriftungen verwenden BicycleDesign.dashboardUnitColor = Weiß mit 65 % Deckkraft (ARGB #A6FFFFFF), kein deckendes Grau. Dasselbe Token für Einheiten in Fahrerprofil, Streckenliste und Streckendetails verwenden. Graph-Skalen verwenden ebenfalls diesen Textfarbwert; strukturelle Linien und semantische Diagramm-/Zonenfarben bleiben davon unabhängig.

Dashboard-Kacheln: drei gemeinsame DashboardTileStyle-Varianten ONE (großes Einzelfeld), TWO und THREE. DashboardTemplate.tileStyle ordnet sie zu. Schriftgrenzen/feste Schriftgrößen, Innenpadding, Beschriftungs-/Wertgewichte, Einheitenabstand und Höhengewicht werden dort definiert; keine presetabhängigen Kopien derselben Werte ergänzen. Bestehende automatische Zahlengrößen für ONE/TWO und feste 45/10 sp für THREE erhalten.

Kartensteuerung: MapControlDesign für Button-/Iconmaße, Abstände, Radien und kompakte Anpassungsgrenzen verwenden. Dashboard-Controls bleiben 44 × 48 dp, Detailkarten-Controls 48 × 48 dp; Spezialrollen erhalten, keine globale Angleichung erzwingen. Zustandsicons/Positionen und 24–48-dp-Anpassung kurzer Kartenflächen bleiben erhalten. Der frei positionierte Editor-Beenden-Button bekommt keinen Formular-Außenabstand.

UI-Texte und Accessibility: sichtbare Beschriftungen, Überschriften, Auswahltexte und Bedienbeschreibungen über Android-Stringressourcen beziehen. DashboardTextResources übersetzt stabile Feld-/Quellen-IDs ausschließlich an der UI-Grenze; IDs, FIT-/BLE-Metadaten und Persistenz bleiben unverändert. Dynamische Nutzernamen, Dateinamen und Lizenz-/Urheberangaben erhalten. Keine Schriftgrößen oder Formulierungen beim Auslagern ändern.

Kleine Schriftrollen getrennt definieren: analysisUnitSp 13, analysisLabelSp 11, sensorStatusSp 13, mapStatusSp 11 und mapAttributionSp 11. Gemeinsame Definition nur für gleiche fachliche Rollen; keine pauschale Verkleinerung/Angleichung aller Texte. Rückmeldungen über BicycleFeedback: bestehende kurze/lange Toastdauer beibehalten, Inline-Status mit Theme-Textfarbe und höflicher Accessibility-Live-Region; identische Statusmeldungen nicht wiederholt setzen. Fehler-/Erfolgsauslöser, fehlende Werte, Retry-Aktionen und automatische Speicherung bleiben beim zuständigen Fachbereich.

Aufnahmegrenzen: Live-Sensoranzeige darf außerhalb der Aufnahme laufen, App-Fahrtzähler nicht. RideRuntime aktiviert/deaktiviert GPS-Tripstatistik beim tatsächlichen START/STOPP. Fahrtdauer vor START = 0, nach STOPP eingefroren; beim nächsten START zurücksetzen. App-eigene Puls-/Kadenzdurchschnitte, Pulsmaximum und Rad-Fahrtstrecke ebenfalls nur innerhalb der Aufnahme verändern. FIT-/Track-/Graph-/Performance-Pfade behalten ihre bestehende Aufnahmebedingung. Details: docs/recording-boundaries.md. Fahrradprofile sind als nächster Architekturauftrag in docs/bicycle-profiles-plan.md vorgemerkt.

Bikes: ein bis sechs dynamische Profile über BikeProfileStore; erste Migration in Slot 0, bestehende aktive Runtime-/FIT-Preferences als Kompatibilitätsgrenze erhalten. Hauptmenüeintrag Bikes (Bike-Icon); alle Einstellungen auf einer scrollenden Seite mit antippbarem Namen, Kategorie-Dropdown, Sensoren und nachfolgendem festen Sticky-Abschnitt „FIT-Aufzeichnung“. Hauptquellen-Auswahl ist direkt sichtbar; die zusätzlichen FIT-Felder stehen je Datenquelle in einem eigenen Accordion. Deren Offen-Zustand bleibt bei Profilwechsel in derselben Activity erhalten; FIT-Änderungen werden weiterhin sofort im jeweiligen Bike gespeichert. E-Bike und Indoor aktiv, normales Bike und separate Powermeter-Rolle vorbereitet. Profilwechsel bei Aufnahme/Speichern sperren; alte Links/Livewerte beim Wechsel entfernen. Disconnect bewahrt Zuordnung und blockiert automatische Wiederverbindung bis zur bewussten Verbindungsaktion. Sensoren anhand bestätigter BLE-Dienste zuordnen, nicht anhand von Namen. Details/Abgrenzung/Backup in docs/bicycle-profiles.md.

Sticky-Kategorien sind opt-in über BicycleSectionHeader und BicycleStickyScrollView. Fahrer, Bikes, Daten & Speicher und Design & UI verwenden 23 sp/900 Black, zentriert, SECONDARY als Seitenhintergrund, oben/unten 0,75 dp FOREGROUND, min. 56 dp und 12 dp Innenpadding. Keine äußeren Headerabstände, kein oberer Leerstreifen und kein Sticky-Schatten; aufeinanderfolgende Header berühren sich beim Scrollen. Sticky-Z bleibt über nativen Buttons, ohne Elevation-Schatten. Inhalte bekommen davor/danach zentral 16 dp über BicycleContentSpacing; kein doppeltes Addieren vorhandener Control-Abstände. Keine automatische Umstellung von Accordions. Sensor-Schalter auf Bikes behalten die Abschnittstypografie. Auswahlrahmen von Kacheln bleiben aktiv ACCENT. Details: docs/sticky-sections.md und docs/ui-content-spacing.md.

Formulare: BicycleForm.section und BicycleForm.numberRow/outlinedInput verwenden. Numerische Einstellungen auf Fahrer und Bikes stehen mit Beschriftung links, rechts Bebas-Neue-Wert in analysisMetricSp (28 sp), Einheit dahinter. Eingabehintergrund transparent, 0,75 dp TERTIARY als Rahmen; Zahlenbreite zentral formNumberWidth. WebDAV-Eingaben verwenden denselben Rahmen bei normaler Systemschrift und voller Textbreite. Geburtsdatum, Namenbearbeitung, Dropdowns und Karten bleiben bei ihrer jeweiligen Bedienform.

Accordions: ausschließlich BicycleDialogStyle.accordion verwenden. Typografie wie Dropdowns (BicycleDesign.inputSp, 16 sp, Typeface.DEFAULT, normales Gewicht), Text linksbündig/START und vertikal mittig. Accordions sind bedienbare Buttons mit stateDescription für offen/geschlossen, keine 23-sp-/900-Black-Abschnittsüberschriften. Sticky-Kategorien bleiben eigenständige Abschnittsüberschriften. Graphenauswahl: erste Auswahlzeile nach finalem addView mit dem zentralen 8-dp-Control-Abstand versehen; Farbpunkte verwenden graphChoiceDotDiameter.

Header-Tiefe: BicycleHeaderShadow zeichnet einen gecachten, ausschließlich nach unten weich auslaufenden Schatten (12 dp, max. Alpha 102/255 = 40 % Deckkraft) nur unter Vollbild-PRIMARY-Headern. Keine zusätzlichen Layoutabstände oder native große Z-Schatten einführen; Sticky-Z für korrektes Überdecken der Buttons bleibt erhalten; Kategorieheader zeichnen keinen Schatten mehr.

Kartenquellen: OpenTopoMap mit maximal Zoom 17; OpenCycleMap/Thunderforest benötigt einen eigenen API-Schlüssel aus Karten. Ohne Schlüssel bisherige Auswahl beibehalten, bei wiederhergestellter OpenCycleMap-Konfiguration ohne lokalen Schlüssel auf CyclOSM zurückfallen. Schlüssel im privaten map-provider-Speicher, nicht in Backups/Diagnosen. Kein Offline-Prefetch. Höhenlinien/Schummerung sind bei diesen Rasterquellen Teil des Kartenbilds, keine unabhängigen Schalter ohne tatsächliche zusätzliche Ebenen anbieten. Details: docs/map-providers.md.

Höhenmeter: gemeinsame RouteElevation-Zählung mit bestätigten Extrema und 3-m-Umkehrschwelle verwenden; nach Bestätigung ganze Flanken zählen, keine Restmeter an jeder Kuppe/Senke abschneiden. Gleiche Logik für Auf-/Abstieg, keine kalibrierten Summenfaktoren. Bestehende Live-Glättung und Qualitäts-/Stillstands-/Aufnahmegrenzen erhalten. Alte FIT-Dateien nicht automatisch ändern. Optionale DEM-Korrektur über Mapterhorn-Z/X/Y-Höhenkacheln nach Aufnahme; standardmäßig aus. Unverändertes Original in fit-library behalten, korrigierte FIT atomar als aktive Version in corrected ablegen. Höhen/Gradient und zugehörige Summen konsistent ersetzen; andere Sensor-/Performancefelder erhalten. Keine FIT-Datei hochladen, nur benötigte Höhenkacheln abrufen und begrenzt cachen. Details: docs/elevation-gain.md.

Ab 0.7.23: Nutzermotiv als settings_brand.webp statt PNG, Ressourcenname und Darstellung/Build-Anzeige erhalten. Neues Nutzer-App-Icon als launcher_cyclist mit adaptiver Maske; vorheriger Vektor bleibt im Repository.

Geburtsdatumsfeld im Fahrerprofil: wie Körpergröße als Zeile mit Beschriftung links, Datumswert rechts in Bebas Neue / analysisMetricSp (28 sp), transparentem Hintergrund und 0,75-dp-TERTIARY-Rahmen. Gemeinsames BicycleForm.outlinedInput, Datumsauswahl und Speicherung erhalten; Breite formDateWidth für längere Datumswerte.

Kompakte Bestätigungsdialoge mit Beschreibungstext (z. B. Strecke aus Sammlung entfernen, Zurücksetzen): über BicycleDialogBuilder/BicycleDialogStyle.messageSpacing mindestens den zentralen controlGap (8 dp) zwischen Header und Nachrichtentext gewährleisten. Größere vorhandene Innenabstände erhalten, keine Einzelfall-Abstände direkt in Fachdialogen eintragen.

intervals.icu: FIT-Fahrtuploads unter Daten & Speicher → Streckendaten, getrennt von WebDAV. Nur eigene Fahrten, aktive FIT-Version, eigenständige Jobs/Erfolgsbelege je Konto/Dateistand. API-Schlüssel über StoredSecret verschlüsseln; keine Geheimnisse in Backups/Diagnosen. Cloud-Aktion bedient aktivierte Dienste, Haken erst bei Erfolg aller aktivierten Ziele. Später Austausch und Integration strukturierter Trainingsprogramme, jetzt keine Trainingssteuerung. Umfang und offene Online-Verifikation in docs/intervals-icu-plan.md.

Wetter in Streckendetails: zweite Zeile unter Datum, Open-Meteo für zeitliche Tourmitte und nahe GPS-Position. Anzeige ausschließlich aus verfügbaren Werten, keine Nullersatzwerte. Erfolgreiche Wetterdaten dauerhaft pro Fahrt unter filesDir/ride-weather-v1 speichern, nie als wegwerfbaren/ablaufenden Cache; offline zuerst diese Metadaten verwenden. Löschen mit der Fahrt. FIT-/Sensor-/Performance-Daten unverändert lassen. Quellen und Dienste zentral unter Erweitert → Quellen & Dienste; bestehende erforderliche Kartenattribution und FIT-Provenienz erhalten. Details: docs/ride-weather.md.

Activity-Lebenszyklus: Bei normalem App-Wechsel (onStop/onStart) DialogNavigation und offene Dialoge erhalten, einschließlich verschachtelter Navigation, Scrollposition und Eingabeentwürfen. Kein dialogs.closeAll in onStop. Hintergrund-Ressourcen wie Kartenrenderer und BLE-Suche weiterhin pausieren; Dialoge beim tatsächlichen onDestroy bereinigen. Bewusste Navigation zum Dashboard/Editor darf sie weiterhin schließen.

BLE-Status in Bikes: Anzahl unterschiedlicher tatsächlich empfangener Messwertarten derselben Verbindung in der Statuszeile anzeigen, keinen Nachrichten-/Sekundenzähler. Wiederholungen nicht mitzählen, beim Trennen zurücksetzen, GPS ausnehmen. Neue BLE-Clients müssen dieselbe Empfangssemantik anbieten. Fahrt-/Cloud-Zuordnung: unveränderliche interne FIT-ID erhalten; bestätigte externe Aktivitäts-IDs über RideServiceLinks getrennt nach Dienst/Konto und unabhängig von Dateiversions-Erfolgsbelegen speichern. Uploadziel-Fehler dürfen das Vormerken anderer Dienste nicht verhindern. Strava/Dreeve nutzen unabhängige asynchrone Jobs; angenommene Uploads sind noch kein Erfolg. Dreeve per Importstatus/Dateiname, Strava per persistierter Upload-ID bestätigen. Persönliche Strava-API-App mit OAuth/verschlüsselten Tokens, kein gemeinsames Secret in der APK. Namensabgleich/Wartung bleiben später; Einrichtung und reale Verifikationsgrenzen in docs/cloud-activity-uploads.md.

Für später vorgemerkt: zentrale BICYCLE-Strava-App mit Backend für Codeaustausch/Token-Erneuerung und einfachem „Mit Strava verbinden“ ohne persönliche API-Registrierung der Nutzer. Noch nicht umsetzen oder die aktuelle Anbindung entfernen. Details und erforderliche Strava-Freigaben in docs/cloud-activity-uploads.md, Abschnitt „Vorgemerkt: zentrale Strava-Anmeldung“.


Seitenabstände: BicycleContentSpacing.page setzt vor dem ersten Inhalt insgesamt 16 dp, berücksichtigt vorhandene Control-Margins und entfernt zusätzliche äußere Wrapper-Insets. Erste Sticky-Kategorie beginnt direkt unter dem Seitenheader; deren Inhalt folgt mit 16 dp. BicycleContentSpacing.sections sichert 16 dp vor/nach Kategorieinhalten für Texte, Felder, Dropdowns, Buttons/Schalter, Accordions, Listen und Kachelgruppen. Control-Gap bleibt 8 dp. Header selbst haben keine Außenmargins. Inhaltspadding/Touchflächen innerhalb von Controls erhalten.

Textbuttons mit führendem Icon: BicycleDialogStyle.actionIcon/BicycleTextIcon verwenden. Icon unmittelbar vor dem Text mit 8 dp Abstand, bei zentrierten Buttons als gemeinsam zentrierte Gruppe; kein abgesetztes Icon am linken Buttonrand. Native Texte, Accessibility und Touchflächen erhalten. Links ausgerichtete Accordions behalten ihre Ausrichtung; fachliche Icon-only-Buttons und vertikale Aktionsfooter behalten ihre eigene Geometrie.

Sensoridentität: SensorIcons ist die gemeinsame Zuordnung für Bikes-Schalter, Quellenwahl und Dashboard-Beschriftungen: GPS satellite-dish, E-Bike bike, Puls heart, Powermeter zap. Keine separat gezeichneten Fahrrad-/Sensorvarianten ergänzen. Neue Sensorquellen hier registrieren und dieselben Icons an jeder Verwendungsstelle nutzen.

Bikes: Typ über dem Icon der Profilkacheln, gleiche Schrift wie der Name darunter; Name immer mit zwei reservierten Zeilen. Fahrradbild zwischen Profilname und Radtyp über Android-Dokumentauswahl; private, atomare WebP-Kopie je Slot, maximal 1200 px längste Seite, begrenzter Import und Verarbeitung im Hintergrund. Löschen nur des jeweiligen Fotos, ungültiger Import erhält das alte Bild. Kein dauerhafter Zugriff auf das Original-URI erforderlich. Fotos derzeit nicht Teil der manuellen Einstellungssicherung. FIT-Bereich: eigene Überschrift FIT-Hauptquelle über Erklärung, darunter Geschwindigkeit und Distanz; vor Aufzuzeichnende Werte eine Abschnittslinie. Abstände verwenden die zentralen Tokens.

Streckendetails: Datum/Uhrzeit und Wetter verwenden rowTitleSp und normale Systemschrift wie die Streckentitel der Liste. Wetter-Icon direkt vor der jeweiligen Angabe; Gruppen auf schmalen Screens umbrechen statt die Schrift zu verkleinern. Eingebettete Karte quadratisch (1:1). Keine Mapterhorn-Zusatzzeile im Höhenbereich; zentrale Quellenangabe unter Erweitert und technische FIT-Provenienz erhalten.

Linien allgemein: BicycleDesign.dividerDp und sectionRuleDp sind 0,75 dp als Testwert (typisch 2 physische Pixel bei Dichte 2,5–3). BicycleDesign.linePixels rechnet über die Context-DisplayMetrics um, rundet auf ganze Pixel und erhält mindestens einen Pixel. Keine Ableitung aus der Displayhöhe. Horizontale Abschnittstrenner verwenden zentral sectionDividerGap = 12 dp oben und unten, Streckendetails verwenden für Kennzahlenbereiche abweichend analysisDividerGap = 2 dp; zusätzliche Control-Margins direkt danach nicht addieren. FOREGROUND/Deckkraft der Rollen erhalten, Feldrahmen bleiben TERTIARY.

Dynamische Bikes: maximal sechs Profile, drei gleich breite Kacheln sichtbar, native horizontale Scrollauswahl. Plus-Kachel „Neues Rad“ am Ende nur unter sechs Profilen. Bestehende vier Profile migrieren ohne Verlust; frische Installation beginnt mit einem Rad. Stabile, nicht wiederverwendete slot-ID unabhängig von Anzeigereihenfolge/Fotos; keine Arrayindizes zur Profilauswahl verwenden. Langes Drücken startet Drag-and-drop zum Umsortieren, Reihenfolge persistent; ergänzende Accessibility-Verschiebeaktionen. Fachlicher Footer mit „Umbenennen“ und „Rad löschen“ mit Bestätigung; bei einem Rad nur Löschen ausblenden, Umbenennen bleibt sichtbar. Fahrradname verwendet die gemeinsame Sticky-Header-Darstellung und bleibt antippbar zum Umbenennen. Profilwechsel, Hinzufügen, Löschen und Verschieben während Aufnahme/Speichern sperren; Fahrten beim Profillöschen erhalten. Indoor nur TRAINER und optional HEART, kein GPS/EBIKE/separates POWER; FTMS-/Matrix-Briefing verwenden, reiner Datenempfang. Details: docs/bicycle-profiles.md.

Streckendetails: Kennzahlenbereiche (Allgemeine Daten, Höhe, E-Bike, Kadenz/Puls usw.) behalten kompakte Abschnittstrenner mit analysisDividerGap = 2 dp oben/unten. Globale Abschnittstrenner sonst weiterhin 12 dp. Wetterzeile horizontal zentrieren, bei Umbruch jede Zeile separat; mittige Punkte nur zwischen den drei Gruppen, nicht zwischen Himmel und Temperatur.

Streckenliste: native ListView-Trenner verwenden wie die Abschnittstrenner der Streckendetails FOREGROUND mit BicycleDesign.dividerAlpha (55 % Deckkraft); dünne zentrale Linienstärke erhalten. Kein eigener abweichender Alpha-Wert.

Indoor ab 0.7.25: TrainerConnection/TrainerMeasurement für FTMS 1826/2AD2 verwenden. Nur Datenabo (CCCD) und optional Battery-Read, keine Trainersteuerung. Pro Feld 5 s Frische, echte Nullen bewahren, ungültige/fehlende Felder nicht wiederbeleben, Main-Thread und GATT-Generationen prüfen. Bestehende LiveData-/Record-IDs normalisiert weiterverwenden; Quellenwahl/Icons profilabhängig Indoor-Bike. Allgemeine Fahrtzeit-IDs mit GPS_-Präfix sind kompatible App-Fahrtwerte, keine GPS-Daten; GPS-Koordinaten/Geschwindigkeit/Höhe nicht anbieten. Kein GPS-Start im Indoor-Profil. Traineradresse/-Name/-Schalter pro Profil separat, externe Pulsmesser bevorzugen. PerformanceEngine mit primärer PowerSource.FTMS, keine Bosch-Nullfortschreibung für FTMS. Journal V6 mit Indoor-Merkmal, V4/V5 lesbar halten; FIT Indoor Cycling und keine GPS-/Motorwerte erfinden. Details: docs/indoor-ftms.md.

Archivimport: RouteArchiveImport auf dem vorhandenen MapState-Worker verwenden; FIT/GPX und deren GZIP-Dateien innerhalb einer ZIP, keine vollständige Entpackung oder ZIP-Pfade als Dateipfade. Originalbytes, Fahrtdatum, bestehende Auswahl und Herkunft erhalten. Byteidentische Duplikate per Hash überspringen, temporäre Dateien und Inflater freigeben, Größen-/Anzahlgrenzen und Teilimportbilanz erhalten. Archivimporte bleiben rein lokal, keine automatische Cloud-Weiterleitung oder erfundene externe IDs; dienstselektiver Massenexport separat zu entwerfen. Details: docs/route-archive-import.md.

Indoor-Messwertkorrektur: IndoorCorrection im Bikeprofil speichert Leistungs- und gemeinsamen Geschwindigkeits-/Distanzfaktor als Zehntel 0–30, Default 10. Nur Indoor zeigt zwei Slider zwischen Sensoren und FIT. RideRuntime normalisiert vor allen Verbrauchern; Roh-FTMS-Puffer/Zeitstempel erhalten, Kadenz/Puls unverändert. Korrigierte Werte gelten ausdrücklich auch für Journal/FIT und Performance. Nicht beim Export/Einlesen erneut anwenden. Faktoren während Aufnahme/Speicherung sperren; alte Fahrten und eingefrorene Fahrtzähler nicht rückwirkend verändern. Details: docs/indoor-ftms.md.

Benachrichtigungen: Hauptmenü nach Design & UI, eigener Fullscreen-Screen. Android NotificationListenerService/BIND_NOTIFICATION_LISTENER_SERVICE; nur ausdrücklich aktivierte und ausgewählte Apps, keine Altbenachrichtigungen nachladen. Appauswahl plus Messenger-Filter, Position Unten/Mitte/Oben als Dashboard-artige Vorschaukacheln, Dauer 5–180 s/Schritt 1/Default 60. Sonderfenster in der Dashboard-Activity: SECONDARY, 3 dp ACCENT-Rahmen, dezente Rundung, farbiges Original-App-Icon vor Absender, scrollbarer Text. Unten maximal Kartenoberkante, Mitte/Oben max. 320 dp und verfügbare Höhe, Systemleisten aussparen. Kein Systemoverlay oder zusätzlicher Ton; X/Timeout/Folgemeldungen. Inhalte nur RAM, nach Appwechsel/Deaktivierung verwerfen, niemals in FIT/Diagnosen/Prefs. Details: docs/notifications-preparation.md.

Akku-/Smartphone-Anzeige: BatteryFields/BatterySnapshot und einen gemeinsamen kompakten Canvas-Renderer verwenden. Kategorie Akku sammelt Füllstände, Smartphone bietet lokale Uhrzeit HH:mm; Smartphone-Akku in der Akku-Mehrfachauswahl. Kombifelder speichern ein bis vier eindeutige Quellen im stabilen DashboardField-ID-Codec; pro Feld, Seite, Preset, Karte und Backup erhalten. 2×2, nur Sensoricon/vertikaler Balken/Prozent, kein Titel. Direkte Akku-Mehrfachauswahl bis vier, kein Kombischalter und keine separaten Prozent-Auswahlfelder. Nur verfügbare Akkustände angeschlossener Geräte passend zum Bikeprofil anbieten, plus Smartphone. Letzter Akku bleibt abwählbar. Alte Einzelprozent-IDs lesbar erhalten. Bosch-Batteriewert bleibt für die laufende LIVE-Verbindung erhalten, ohne Zeitverfall; bei Trennung/Profilwechsel Verbindungspuffer leeren. Fehlende/stale Quellen behalten ihren Platz mit Strich, echte Null bleibt Null. Unter 20 orange/gelb, unter 10 rot, Grenzwerte genau beachten; Icons neutral. Dashboard hell, Karte dunkel. Kartenzeile nur bei drei/vier Quellen 76 statt 62 dp, Record/Licht und Zoom-Abstände erhalten. PhoneBattery nur über Systembroadcast abonnieren, kein Polling; keine neuen FIT-Felder oder BLE-Messwertzahlen. Details: docs/battery-dashboard.md.

Dashboard 10 FULL_DATA: 1/1/1/2, fünf Felder, keine Karte; ONE/ONE/ONE/TWO als bestehende Stile und gesamte verfügbare Höhe. Untere Bedienleiste sichtbar, Editor-Beenden separat erreichbar, Kartenlebenszyklus pausieren, GPS/FIT unabhängig. Vorschau/Persistenz/Backup wie bisher. Wetterfelder auch bei Indoor auswählbar, ohne GPS weiter fehlend und kein GPS-Start.

Dashboard-Vollbild: Android-Statusleiste standardmäßig per WindowInsetsController ausblenden, über Design & UI abschaltbar und im Design-Backup erhalten. Android-Navigation und Cutout-Sicherheitsabstände beibehalten. Dialogfenster zeigen Statusleiste; Rückkehr/Appwechsel berücksichtigt gespeicherte Wahl, transientes Einblenden per Randgeste. Nicht mit Karten-Vollbild verwechseln.

Fahrmodus-Anzeige: Seitlich/Oben pro App in modeGlowPlacement, Standard Seitlich. Oben nur entlang der oberen Dashboardkante. Akkuwarnung folgt ebenfalls der Position; Übergänge/Breathing nicht neu starten. Zonenvisualisierung verwendet dieselbe zentrale Lichtintensität (modeGlowAlpha/ambientGlowPasses) wie der Rand-Glow, eigene Zonenfarben und Geometrie erhalten.

## Zentrale APK-Builds über GitHub Actions

APK-Builds standardmäßig über `.github/workflows/android-debug.yml`, nicht lokal in
der Codex-Cloud. Push auf main startet automatisch einen Debug-Build; workflow_dispatch
erlaubt manuelle Builds. Keine zusätzliche manuelle Auslösung, wenn ein Push bereits
einen passenden Lauf gestartet hat. Nach Änderungen relevante Tests auswählen, keine
unnötigen lokalen APK-Builds oder vollständigen Testsuiten. GitHub-Lauf bis Abschluss
prüfen; bei Fehlern nur betroffene Schritte und relevante Fehlermeldungen auswerten.
Nur nach notwendiger Korrektur erneut bauen. Erfolgreichen Lauf und APK-Artefakt verlinken.
Artifact enthält APK und SHA-256, 30 Tage Aufbewahrung. Optionales Repository-Secret
ANDROID_DEBUG_KEYSTORE_BASE64 stellt den bestehenden Testschlüssel bereit (Standard-
Android-Debug-Passwörter); ohne Secret temporärer Schlüssel, keine Update-Kompatibilität
versprechen. Schlüssel niemals ins Repository oder Chat schreiben. Details: docs/github-actions.md.

Oberer Fahrmodus-Glow gehört an den gesamten Activity-Fensterhintergrund, einschließlich
ausgeblendeter Statusleisten-/Cutout-Fläche. Dashboard-Inhalte behalten sichere Insets.
Einen Drawable zwischen Dashboard (Seitlich) und Fensterhost (Oben) umhängen, keine
zweite Animation; Bounds nur vom aktuellen Besitzer ändern. Zonen-Quellenauswahl bei
Pulsfeldern nur Puls aktuell/Ø, bei Leistungsfeldern nur Leistung aktuell/Ø, jeweils Aus.
Falsche gespeicherte Zuordnungen und Feldwechsel zentral über ZoneGlowCompatibility
absichern; übrige freie und datenlose Zuordnungen erhalten. Leistungsanzeige zeitgewichtet
5 Sekunden, Puls 3 Sekunden; Farb-Übergang ±3 Prozentpunkte und 1,5 Sekunden erhalten.
Keine Änderungen an Rohwerten/FIT/Performance.

Solange der feste Testschlüssel noch nicht als Actions-Secret eingerichtet ist, kann
die von Actions gebaute Debug-APK heruntergeladen und mit dem vorhandenen geschützten
Testschlüssel nachsigniert werden. Das ist kein lokaler APK-Build. Signatur, SHA-256,
Version und ZIP-Ausrichtung prüfen; bestehenden Nutzern keinen fremd signierten
Update-Build empfehlen. Reine Veröffentlichung fertiger APKs/Prüfsummen/Buildnachweise
mit [skip ci] kennzeichnen, damit kein unnötiger weiterer Build entsteht. Änderungen
an App/Build-Konfiguration niemals so überspringen. Bevorzugt Actions-Artefakte, für
kompatibel nachsignierte Downloads bei fehlendem Release-Upload Repo-Downloads verwenden.

Radtyp-Identität: BikeTypeIcons zentral verwenden: Bike = Fahrrad, E-Bike = Fahrrad mit Blitz, Indoor = stationärer Trainer. SensorIcons verwendet dieselben Varianten in Sensoren, Quellenwahl und Akkuanzeigen. Streckenherkunft zeigt den in FIT gespeicherten Radtyp, niemals das aktuell gewählte Profil; unbekannte alte Fahrten behalten das Fahrrad. Generische Bikes-Menüs und Fahrradkategorie-Vorschauen bleiben unabhängig. Die Dashboard-Kategorie Akku gehört nicht zur FIT-Feldauswahl; einzelne Akkuwerte bleiben bei ihren Sensorquellen.

Dashboards: DashboardPages verwaltet 1–6 benannte Seiten mit stabilen Slots, keine feste Vier-Seiten-Navigation. Kacheln wie Bikes, drei sichtbar und horizontal scrollbar; Plus entfällt bei sechs. Layoutwahl als Dropdown mit den zehn DashboardPreview-Geometrien, Vollbild danach; Fahrmodus-Position als Dropdown. Fachlicher Footer Umbenennen/Löschen, letztes Dashboard nicht löschbar. Backup erhält Katalog, Namen, aktive Seite und alle Slot-Daten; keine Umnummerierung beim Löschen.

Fahrradbezogene UI-Konfiguration ist vorbereitet, noch nicht aktiv: künftig sämtliche Einstellungen von Design & UI inklusive Farbtheme sowie Karten pro Bike. BikeUiConfigurationScope als zentralen Umfang pflegen, neue Optionen dort aufnehmen; keine spontane Profilumschaltung oder Migration ohne Auftrag. Bestehende globale Konfiguration bleibt aktiv. Beim Aktivieren GPX-Auswahl aus dem Bibliotheksindex zusätzlich als Referenz übernehmen; Konten/Zugangsdaten nicht duplizieren. Details: docs/dynamic-dashboards-and-bike-settings.md.

Aufnahmebutton zeigt Bike-Name und Radtyp mit Record-/Stop-Symbol. Native Long-Press-Dauer wie Dashboard-Editor; Long-Press öffnet Bike-Auswahl und startet keine Aufnahme. Wechsel während Aufnahme/Speichern sperren.
