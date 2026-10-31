# Aktuelle Entwicklungsregeln

Dies ist die aktuelle Arbeitsgrundlage. Die datierten Entwicklungsnotizen stehen unverändert in [der Historie](docs/archive/AGENTS-vor-bereinigung-2026-10-30.md); dortige alte Zwischenstände sind keine aktuellen Vorgaben. Fachdetails in den verlinkten Dokumenten beachten, bei Konflikten gelten die neueren ausdrücklichen Nutzerentscheidungen.

## Arbeitsweise

- Kotlin, Android 13+ (minSdk 33), native Views, bestehende Single-Activity-Struktur.
- Schlanke, ressourcensparende Umsetzung. Bestehende Komponenten wiederverwenden, keine unnötigen Bibliotheken oder Refactorings.
- Bei Designaufträgen nur Darstellung ändern. Sensoren, Aufzeichnung, Auswahl, Navigation und Persistenz erhalten, sofern nicht ausdrücklich anders beauftragt.
- Neue Konfiguration nur bei klarem Nutzwert; keine zusätzlichen Schalter für technische Implementierungsparameter.
- Bei einer offenen Gestaltungsentscheidung zwischen bisheriger Darstellung und einer anderen UI-Konvention den Nutzer fragen. Keine eigenmächtigen Funktions-/Layoutänderungen als Nebenprodukt eines Stylings.
- Fachlich unabhängige Änderungen nachvollziehbar committen. Relevante Tests und zentrale Prüfskripte ausführen; keine unnötigen Gesamtläufe oder neuen Tests für triviale kosmetische Änderungen.
- `docs/CONTINUE-HERE.md` knapp aktuell halten: Auftrag, Stand, offene Punkte, Prüfnachweise, letzter veröffentlichter Build. Keine unbegrenzt angehängten Kopien alter Übergaben.

## Zentrale UI-Konventionen

- BicycleDesign, BicycleComponents, BicycleDialogStyle, ThemeManager und BicycleIcons verwenden. PRIMARY für UI-Hervorhebungen, ACCENT bleibt separate Highlightfarbe. Funktionale Zonen-/Boschfarben nicht an Themefarben binden.
- Flat/Glow ändern die Einstellungsdekoration auf derselben Bedienungs-/Geometriebasis. Standard Glow. Aktive Chromatic-Konturen/-Flächen aus BicycleInputStyle, keine neuen Farbkopien oder alternativen Glows. Flat ohne dekorative Effekte; ausdrücklich funktionale HRV-/Bosch-Effekte erhalten.
- Normale Rahmen und inaktive Auswahlkacheln: 2 dp, FOREGROUND mit 25 % Deckkraft, 6-dp-Radius. Aktive Auswahl-/Spezialkonturen bleiben unabhängig. Horizontale UI-Trenner verwenden dieselben Rahmenwerte.
- Globale Auswahlkacheln über BicycleSelectionCard, einschließlich Fahrradfilter. Positionen für Benachrichtigung/Trinkhinweis über MessagePositionChoices. Keine eigene parallele Konturdefinition.
- Schalter immer rechts, Label links; keine Radio-Punkte in Auswahllisten, keine RTL-Positionierungstricks. Native Auswahl-/Mehrfachlogik und Accessibility erhalten.
- Rahmen-Accordion über BicycleFrameDropdown: Icon/Titel links, Chevron vor optionalem rechtem Schalter/Action. Derselbe Rahmen um Kopf und Inhalt, innen 16 dp seitlich/unten, 24 dp nach dem Header. Titel-/Chevron-Druck hebt die gesamte Rahmenfläche hervor; Schalter bleibt unabhängig.
- Normale native Spinner-Popups in beiden Designs einfacher Rahmen und TERTIARY, kein zusätzlicher Popup-Glow. Touchhöhe mindestens 48 dp.
- Formularabstände über controlGap/controlSpacing und BicycleContentSpacing; kleine Überschriften vor Kachelgruppen mit bestehendem Standardabstand. Späte Layoutparameter und Wiederanfügen berücksichtigen, keine doppelten Margins.
- Eingaben: Label außerhalb links, Feld rechts; Einheiten gedimmt innerhalb des Felds. Alle Zeiteingaben mit fester Zeitmaske, Einheiten nicht editierbar. Native Cursor-/Passwort-/Validierungsfunktionen erhalten.
- League Gothic für Haupt-/Sticky-Header und Messwerte, zentrale strongTypeface/900 für kleine Überschriften. Keine eigenen Typeface-Erzeugungen. Sticky-Header großgeschrieben; Schriften/Abstände aus zentralen Rollen.
- Glow-Sticky-Boxen mit Seiteninset, Rundungen und Bodenpadding, 24 dp Abstand zwischen Gruppen. Beim Hochfahren bis an den Hauptheader, anfänglichen Seitenabstand erhalten. Flat behält seine flächige Darstellung ohne zusätzliche weiße Regeln.
- Unterseiten als bestehender Vollbild-Dialoghost mit fixem Header/Footer. Kurze Bestätigungen kompakt. Links Zurück, Titel unabhängig zentriert, Info rechts. Kein leerer Navigationsfooter.
- Fachliche Footer über bicycleFooter/BicycleFooterAction. Dashboard-Footer TERTIARY, ohne Rahmen/Schatten in beiden Designs. Footer bei Layouts ohne Karte immer sichtbar.
- Bestehende Dialoggestaltung nicht pauschal neu einfärben. Der Trinkhinweis bleibt PRIMARY-Alert ohne Header. HRV-Messung/-Ergebnis/-Tagesbefinden/-Auswertung: schlichte SECONDARY-Header ohne Rahmen/Glow, Android-Statusleiste verborgen.
- UI-Typografie und Atlas: `python3 scripts/check-ui-typography.py`, bei Tokenänderungen `python3 scripts/build-ui-catalog.py`, anschließend `--check`.

## Profile, Dashboard und Karten

- Stabile Profil-UUIDs und Dashboard-IDs erhalten. Kein automatischer Profilwechsel bei Sensorfund. Fahrradprofil umfasst Sensoren, FIT-Konfiguration, Design, Karten; Fahrerprofil, Uploadkonten und Benachrichtigungen global.
- Profilreihenfolge: Name, Sensoren, Design & UI, Trainingshilfe, Karten (GPS-Profile), FIT-Aufzeichnung, Strecken-Aufzeichnung. Indoor-Korrektur erhalten. „Aufnahme automatisch starten“ erster Sensorpunkt, danach „Sensoren automatisch verbinden“. Name bleibt separat editierbarer Accordiontitel.
- Design-Reihenfolge: Flat, Theme, Vollbild-Rahmen, Trenner, kleine Dashboard-Layout-Überschrift und vorhandene Auswahl. Kontextfooter nur im Designabschnitt für Dashboardaktionen. [Details](docs/profile-settings-revision.md).
- Dashboardeditor bewusst über Long-Press, alle oberen Kacheln gleich. 5 s Inaktivität ohne Feldauswahl, 12 s bei offener Auswahl. Daten/Graph/Visualisierung unabhängig; Trend-Darstellung bei ausgeblendeten Daten deaktivieren, Berechnung nicht ändern.
- Sichtbare TWO-/THREE-Kacheln pro Gruppe gemeinsam dynamisch skalieren. THREE maximal frühere 45 sp/10 sp. Keine unsichtbaren Felder als Größenreferenz.
- Dashboardgraphen von Flat/Glow unabhängig, Fadefüllung, Skalen innen mit Min/Zwischenwert/Max, obere Serienkreise ihrer Skalenseite zuordnen. 30/60/120-s-Glättung und sanfte 3-s-Rückskalierung erhalten.
- Funktionale Trainingszonen über TrainingZoneColors. Explizite Ausnahme: Flat unten deckende SRC_OVER-Balken (8 % Kachelhöhe), Glow radial. Bosch-Modus immer oben am Fensterhost, gleiche funktionale Farben/Übergänge/Akkuwarnung; Warnung nur bei verbundenem E-Bike.
- Fahrradlicht bei bestätigtem an immer oben: Fullscreen am goldenen Statusplatz links der Notch, sonst Dashboard rechts oben innerhalb sicherer Insets plus 16 dp. Keine untere automatische Lichtmarkierung. Sitzungszustand bis Disconnect halten, keine neue Sensorabfrage.
- Statuszeile: Links 25 dp, rechts 25 dp, bei passend gemeldeter Privacyfläche nur rechts weitere 4 dp. Echte Kamerainsets schützen, keine linke Verschiebung durch Privacyfläche. Keine zuverlässige Sichtbarkeit des grünen Punktes behaupten. [Status](docs/dashboard-status.md).
- Neue Layout-Enums nur anhängen, persistierte Ordinals/IDs erhalten. Gespeicherte Reihenfolge ohne/mit Karte gemäß DashboardTemplate/Picker. Keine Karte sichtbar: Kartenlebenszyklus pausieren, GPS für Aufnahme weiterlaufen lassen.
- Karten: Zoom 6–19 in 0,5-Schritten; gemeinsamer Fahrtrichtung-/Nordmodus mit GPS-Folgen. Manuelles Verschieben löst Folgen, Reaktivierung stellt letzten Modus her. Pinch und Pan gemeinsam, vorhandene Trägheit erhalten. [Bewegung](docs/map-motion.md).
- Drei gespeicherte Trackplätze je Profil, eigene Stile, live Aufzeichnung separat. Dashboard-Augenbuttons vertikal ohne Nummer, deckende Trackfarben. Manager-Auge in Trackfarbe; Platzwahl horizontal, belegt mit Auge, leer ohne Symbol, aktueller Platz stärker umrahmt. Kurz sichtbar: Platz freigeben. Kurz nicht sichtbar/immer lang: explizite Platzwahl. Überschreiben/Umplatzieren erhalten. Filter „Auf Karte angezeigt“ ganz oben; alle belegten Trackplätze erfassen, auch ausgeblendete. [Tracks](docs/three-map-tracks.md).
- Trackgeometrien auf Bibliotheksworker cachen, unveränderliche Render-Snapshots, keine Datei-/FIT-Dekodierung auf UI/onDraw. Profilgeneration gegen veraltete Workerantworten schützen.
- Farbdialog bleibt offen/selektiert, Helligkeit behält Zelle/Farbton. Erst beim Verlassen in denselben Profilplatz speichern. HEX-Direkteingabe weiterhin sofort. [Stile](docs/map-route-style.md).

## Sensoren und Aufzeichnung

- Sensorverbindungen und RecordingService/RideRuntime gemeinsam verwenden, kein zweiter Bosch-/GATT-Client für Anzeigen.
- Aktive Laufzeit = sichtbar ODER Aufnahme. Ohne Aufnahme im Hintergrund Suche/Verbindungsversuche stoppen, bei Rückkehr fortsetzen. Manuelle Gerätesuche 10 s. Während Aufnahme Wiederverbindung/GPS im Hintergrund erhalten; HRV und gelegentlicher Schaltwerkabruf nur sichtbar.
- Aufnahme bis manuellem Stopp, auch bei langen Pausen/Bike aus/Bildschirm aus. Repeated START darf nichts zurücksetzen. Unter 10 s verwerfen. Autostart nur gewähltes Profil, aktive HRV-Messung hat Vorrang, kein späterer Start vorgemerkt.
- Flexible Hauptquellen RecordingSources pro Messwert mit automatischem Fallback. Smartphone-Barometer unterstützen, kein Quellenwechsel durch reine Darstellungsänderung.
- SRAM vorerst erstes verbundenes Gerät als Schaltwerk. Letzten Akkustand dauerhaft anzeigen, niemals als frischen Rohwert aufzeichnen. Unter 20 % bei Aufnahmestart immer 10-s-Warnhinweis, kein Optionsschalter. Keine ANT+-/Gangunterstützung behaupten.
- Smartphone/Bosch/Schaltwerk-Akkucaches nur Darstellung; nach Verbindung frische Werte verwenden. Keine neuen FIT-Felder aus UI-Wünschen erfinden.
- Performance-/NP-Regeln nur auf ausdrücklichen Auftrag ändern. Aktuell NP-Abdeckung 90 %, andere Qualitätsregeln 95 %; bestätigte Stillstandspausen separat aus NP-Zeit/Fenstern ausnehmen, keine unbekannten Lücken mit Null füllen. [NP-Pausen](docs/confirmed-np-pauses.md).
- Private vollständige FIT-Quelle für Analyse behalten, öffentliche Exporte nach Speicherfeldern filtern. Normale Uploads niemals versehentlich aus der privaten Vollquelle. Originale unverändert erhalten. Expliziter Upload-Longpress darf öffentlichen FIT aus vorhandenem Datenbestand neu erzeugen.
- Import robuste Zeitachsen/Positionssegmente für Bosch-FIT/GPX erhalten. Keine echten Pausen verbinden oder Geschwindigkeitsspitzen als Messung erfinden. Fachliche Details vor Parseränderungen lesen.

## HRV, Cloud und Sicherung

- HRV-Tagesbefinden pro Kalendertag, nicht pro Messung. Letzte Antworten vorauswählen; ungewählte Auswahlfelder fehlen, Sliderstandard gültig. Automatisch erkannte Vortagsfahrt nur vorauswählen, manuelle Antworten gewinnen; „Kein Training“ entfernt zugehörige km/Minuten/RPE aus der Wertung, nicht die Fahrt.
- Tagesstart-Hinweis ab 04:00, nicht Mitternacht. HRV-Pulsvisualisierung auch in Flat, 0,3-s-Rampe. Vorhandene tolerante Messannahme/Artefakthinweise erhalten, keine neuen Verwerfungsgrenzen.
- HRV-Tag/Woche/Monat mit vorbereiteten Mittelwerten, Korridor und separaten stabilen Skalen; kein Autoskalieren beim Scrollen. Beschriftungen an Datenpunkten, exakt clippen. Tagesfooter erst bei expliziter Tageswahl. [Auswertung](docs/hrv-daily-assessment.md), [Kontextfarben](docs/hrv-context-colors.md).
- Eigener vorläufiger HRV-Score 2×ln(RMSSD in ms), keine erfundene Alterskorrektur/HRV4-Gleichheit. Bestehende Referenzberechnung erhalten.
- Drei getrennte Backupbereiche: Einstellungen (verschlüsseltes Passwort-ZIP inkl. Zugangsdaten), Strecken, HRV inkl. Tagesbefinden/letzter gültiger Messung je Tag. Lokal/WebDAV, Berechtigungen nicht als App-Einstellung sichern. Keine neue alte Gesamtsicherung einführen.
- Neue aktive Optionen in passenden Profilumfang, Sicherung und Validierung aufnehmen; alte ausgebauten Optionen nur am Kompatibilitätsrand lesen/ignorieren, nicht neu exportieren. Keine Zugangsdaten ins Log/Repo.
- Dreeve-Import bevorzugt Original-FIT, GPX-Fallback; importierte eigene Fahrten verknüpfen. Namensabgleich vom Dienst zur App. Wartung nur Daten lesen, keine schreibenden Schnellaktionen. Importfortschritt mit Abbrechen, kein unbeabsichtigtes Verlassen.
- Intervals HRV nur eigenes unverändertes Tageswellness aktualisieren; fremde HRV erhalten. Sammelupload nur fehlende Tage/Fahrten; eigene unveränderte HRV-Tage dürfen um fehlende Schlafbewertung-, Stress- und Verletzungsfelder ergänzt werden. Keine fremden Wellnessfelder überschreiben. [Intervals](docs/intervals-hrv.md).
- Kompakte In-/Out-Zeilen der Cloud manuell gemäß Nutzerauftrag pflegen, keine alten langen dynamischen Erklärungen wieder einführen. Fehler bei unerreichbarem Dienst aussagekräftig darstellen.
- Nachrichteninhalte nur RAM, nie FIT/Prefs/Diagnose. Android-Benachrichtigungszugriff explizit durch Nutzer; keine neuen Systemberechtigungen für Styling.

## APK und Veröffentlichung

- APK ausschließlich über `.github/workflows/android-debug.yml` in GitHub Actions. Push auf main startet einen Lauf; nicht zusätzlich dispatchen. Lokal nur benötigte Compilation-/Unit-Tests, keine APK bauen.
- Der aktuelle Workflow verlangt den dauerhaften Schlüssel im Secret ANDROID_DEBUG_KEYSTORE_BASE64 und verweigert temporäre Signaturen. Schlüssel nie ausgeben oder einchecken.
- Bis zum erfolgreichen Lauf warten, Artefakt herunterladen; SHA-256, Signatur, Version, kein Debug-/Test-Flag, ZIP-Ausrichtung prüfen. Update-Kompatibilität nur bei passendem Zertifikat.
- APK und SHA in downloads veröffentlichen, README/Buildnachweis/Übergabe aktualisieren. Reine Veröffentlichung mit `[skip ci]`, Appänderungen niemals damit überspringen.
- Tatsächlichen direkten Raw-Download laden und binär gegen Artefakt vergleichen. Kein neues APK ohne Auftrag; nicht geprüfte Geräteszenarien offen benennen.

Dashboardwechsel: DashboardPageViewport um Daten+Karte, Footer/Status außerhalb. 300-ms-Slide über gemeinsamen ValueAnimator, eine lebende MapView, begrenzter RAM-Snapshot; nach Ende/Cancel freigeben. Systemanimation aus → sofort, Delta0/Editor sofort. Keine Folgewisch-Warteschlange, App-/Fokuswechsel canceln. [Details](docs/dashboard-page-transition.md). Streckendetailwerte unabhängig von Einheit zum Icon zentrieren; AnalysisValueRow-Opt-in nur in AnalysisMetricTile, andere Nutzer unverändert.
