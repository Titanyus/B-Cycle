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

Aktionsfooter: bicycleFooter und BicycleFooterAction verwenden (64 dp Höhe, 24-dp-Icon, kleine 11-sp-Beschriftung zentriert darunter). Icons und Texte verwenden FOREGROUND, Erfolgs-/Busy-Zustände behalten ihre Semantik. Streckendetails: Anzeigen, Umbenennen, Cloudupload, Speichern, Löschen. Streckenmanager: Importicon mit „Strecke importieren“, links im gleichen Footer. Keine zusätzliche Footer-Navigation oder leere Footer einführen.

Sekundäre Kennzahlentexte: Einheiten und daran angebundene Dashboard-Beschriftungen verwenden BicycleDesign.dashboardUnitColor = Weiß mit 65 % Deckkraft (ARGB #A6FFFFFF), kein deckendes Grau. Dasselbe Token für Einheiten in Fahrerprofil, Streckenliste und Streckendetails verwenden. Graph-Skalen verwenden ebenfalls diesen Textfarbwert; strukturelle Linien und semantische Diagramm-/Zonenfarben bleiben davon unabhängig.

Dashboard-Kacheln: drei gemeinsame DashboardTileStyle-Varianten ONE (großes Einzelfeld), TWO und THREE. DashboardTemplate.tileStyle ordnet sie zu. Schriftgrenzen/feste Schriftgrößen, Innenpadding, Beschriftungs-/Wertgewichte, Einheitenabstand und Höhengewicht werden dort definiert; keine presetabhängigen Kopien derselben Werte ergänzen. Bestehende automatische Zahlengrößen für ONE/TWO und feste 45/10 sp für THREE erhalten.

Kartensteuerung: MapControlDesign für Button-/Iconmaße, Abstände, Radien und kompakte Anpassungsgrenzen verwenden. Dashboard-Controls bleiben 44 × 48 dp, Detailkarten-Controls 48 × 48 dp; Spezialrollen erhalten, keine globale Angleichung erzwingen. Zustandsicons/Positionen und 24–48-dp-Anpassung kurzer Kartenflächen bleiben erhalten. Der frei positionierte Editor-Beenden-Button bekommt keinen Formular-Außenabstand.

UI-Texte und Accessibility: sichtbare Beschriftungen, Überschriften, Auswahltexte und Bedienbeschreibungen über Android-Stringressourcen beziehen. DashboardTextResources übersetzt stabile Feld-/Quellen-IDs ausschließlich an der UI-Grenze; IDs, FIT-/BLE-Metadaten und Persistenz bleiben unverändert. Dynamische Nutzernamen, Dateinamen und Lizenz-/Urheberangaben erhalten. Keine Schriftgrößen oder Formulierungen beim Auslagern ändern.

Kleine Schriftrollen getrennt definieren: analysisUnitSp 13, analysisLabelSp 11, sensorStatusSp 13, mapStatusSp 11 und mapAttributionSp 11. Gemeinsame Definition nur für gleiche fachliche Rollen; keine pauschale Verkleinerung/Angleichung aller Texte. Rückmeldungen über BicycleFeedback: bestehende kurze/lange Toastdauer beibehalten, Inline-Status mit Theme-Textfarbe und höflicher Accessibility-Live-Region; identische Statusmeldungen nicht wiederholt setzen. Fehler-/Erfolgsauslöser, fehlende Werte, Retry-Aktionen und automatische Speicherung bleiben beim zuständigen Fachbereich.

Aufnahmegrenzen: Live-Sensoranzeige darf außerhalb der Aufnahme laufen, App-Fahrtzähler nicht. RideRuntime aktiviert/deaktiviert GPS-Tripstatistik beim tatsächlichen START/STOPP. Fahrtdauer vor START = 0, nach STOPP eingefroren; beim nächsten START zurücksetzen. App-eigene Puls-/Kadenzdurchschnitte, Pulsmaximum und Rad-Fahrtstrecke ebenfalls nur innerhalb der Aufnahme verändern. FIT-/Track-/Graph-/Performance-Pfade behalten ihre bestehende Aufnahmebedingung. Details: docs/recording-boundaries.md. Fahrradprofile sind als nächster Architekturauftrag in docs/bicycle-profiles-plan.md vorgemerkt.

Bikes: vier feste Profile über BikeProfileStore; erste Migration in Slot 0, bestehende aktive Runtime-/FIT-Preferences als Kompatibilitätsgrenze erhalten. Hauptmenüeintrag Bikes (Bike-Icon); alle Einstellungen auf einer scrollenden Seite mit antippbarem Namen, Kategorie-Dropdown, Sensoren und nachfolgendem festen Sticky-Abschnitt „FIT-Aufzeichnung“. Hauptquellen-Auswahl ist direkt sichtbar; die zusätzlichen FIT-Felder stehen je Datenquelle in einem eigenen Accordion. Deren Offen-Zustand bleibt bei Profilwechsel in derselben Activity erhalten; FIT-Änderungen werden weiterhin sofort im jeweiligen Bike gespeichert. Nur E-Bike aktiv, Bike/Indoor und zusätzliche Leistungs-/Trainerrollen vorbereitet. Profilwechsel bei Aufnahme/Speichern sperren; alte Links/Livewerte beim Wechsel entfernen. Disconnect bewahrt Zuordnung und blockiert automatische Wiederverbindung bis zur bewussten Verbindungsaktion. Sensoren anhand bestätigter BLE-Dienste zuordnen, nicht anhand von Namen. Details/Abgrenzung/Backup in docs/bicycle-profiles.md.

Sticky-Kategorien sind opt-in über BicycleSectionHeader und BicycleStickyScrollView. Angewandt auf Fahrer (Persönliche Daten, Trainingsparameter, Herzfrequenzzonen, Leistungszonen), Bikes (Radtyp, Sensoren, FIT-Aufzeichnung), Daten & Speicher (Globale Daten, Streckendaten) sowie Design & UI (Theme, Dashboard-Layout, Fahrmodus-Anzeige): Schrift unverändert 23 sp/900 Black, zentriert, TERTIARY, oben/unten 1 physischer Pixel FOREGROUND, min. 56 dp und 12 dp Innen-/Außenabstand. Oberhalb bleibt ein deckender 12-dp-SECONDARY-Streifen auch angedockt erhalten; Sticky-Z liegt über erhöhten nativen Buttons. Sensor-Aktivierungsschalter auf Bikes verwenden selbst die Abschnittstypografie, ohne doppelte Überschrift. Keine automatische Migration anderer Überschriften oder Accordions. Auswahlrahmen von Kacheln zentral über BicycleComponents.selectionBorder: aktiv ACCENT, sonst bestehendes FOREGROUND. Füllung und Geometrie erhalten. Details: docs/sticky-sections.md.

Formulare: BicycleForm.section und BicycleForm.numberRow/outlinedInput verwenden. Numerische Einstellungen auf Fahrer und Bikes stehen mit Beschriftung links, rechts Bebas-Neue-Wert in analysisMetricSp (28 sp), Einheit dahinter. Eingabehintergrund transparent, 1 physischer Pixel TERTIARY als Rahmen; Zahlenbreite zentral formNumberWidth. WebDAV-Eingaben verwenden denselben Rahmen bei normaler Systemschrift und voller Textbreite. Geburtsdatum, Namenbearbeitung, Dropdowns und Karten bleiben bei ihrer jeweiligen Bedienform.

Accordions: ausschließlich BicycleDialogStyle.accordion verwenden. Typografie wie Dropdowns (BicycleDesign.inputSp, 16 sp, Typeface.DEFAULT, normales Gewicht), Text linksbündig/START und vertikal mittig. Accordions sind bedienbare Buttons mit stateDescription für offen/geschlossen, keine 23-sp-/900-Black-Abschnittsüberschriften. Sticky-Kategorien bleiben eigenständige Abschnittsüberschriften. Graphenauswahl: erste Auswahlzeile nach finalem addView mit dem zentralen 8-dp-Control-Abstand versehen; Farbpunkte verwenden graphChoiceDotDiameter.

Header-Tiefe: BicycleHeaderShadow zeichnet einen gecachten, ausschließlich nach unten weich auslaufenden Schatten (6 dp, max. Alpha 55/255) unter Vollbild-PRIMARY-Headern und Kategorieheadern. Keine zusätzlichen Layoutabstände oder native große Z-Schatten einführen; Sticky-Z für korrektes Überdecken der Buttons bleibt erhalten.

Kartenquellen: OpenTopoMap mit maximal Zoom 17; OpenCycleMap/Thunderforest benötigt einen eigenen API-Schlüssel aus Karten. Ohne Schlüssel bisherige Auswahl beibehalten, bei wiederhergestellter OpenCycleMap-Konfiguration ohne lokalen Schlüssel auf CyclOSM zurückfallen. Schlüssel im privaten map-provider-Speicher, nicht in Backups/Diagnosen. Kein Offline-Prefetch. Höhenlinien/Schummerung sind bei diesen Rasterquellen Teil des Kartenbilds, keine unabhängigen Schalter ohne tatsächliche zusätzliche Ebenen anbieten. Details: docs/map-providers.md.
