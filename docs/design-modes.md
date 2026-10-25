# Flat und Glow auf gemeinsamer UI-Basis

Stand 24.10.2026. Glow-Design ist auf Nutzerwunsch Standard. Unter Design & UI bei der Theme-Auswahl steht der globale Schalter Flat-Design. Aus = Glow, ein = Flat. Bestehende Nutzer starten ohne zusätzliche Migration mit Glow.

Die Varianten verwenden dieselben nativen Views, Layouts, Maße, Beschriftungen und Listener. BicycleAppearance entscheidet nur über Dekoration. Keine zweite Bedienlogik und keine modusspezifischen Datenmodelle. Auch nach einem Wechsel bleiben Eingaben, Auswahl, Fokus, Navigation und Aufnahmefunktionen erhalten.

- Flat: einfarbige PRIMARY-Hervorhebung, kein dekorativer Kontur-/Flächenverlauf, kein Eingabe-/Auswahl-/Zonen-Glow, keine dekorativen Schatten. Diagrammfüllungen ebenfalls einfarbig bei gleicher Datengeometrie. Der Bosch-Fahrmodus-Glow bleibt unabhängig aktivierbar. Der pulssynchrone HRV-Mess-Glow ist eine funktionale Ausnahme und bleibt ebenfalls in beiden Modi sichtbar.
- Glow: gemeinsame Flat-Geometrie mit abgeleitetem Flächen- und Konturverlauf, gecachten Nah-/Außen-Blurs an Fokusfeldern und aktiven Segmenten sowie Schatten aktiver Segmente und bestehender Hinweis-/HRV-Karten. Bestehende semantische Diagramm-/Zonenfarben erhalten.
- Alle bisherigen ACCENT-Hervorhebungen verwenden PRIMARY. ACCENT bleibt in ThemeTokens, gespeicherten benutzerdefinierten Paletten und Backups separat erhalten. Kein Überschreiben oder Zusammenlegen der beiden Farben. Kontrastschutz für kleine Icons und Text bleibt bestehen.

Die Einstellung uiTheme.flatDesign wird in DESIGN gesichert und wiederhergestellt; sie ist appweit und wird beim Fahrradprofilwechsel nicht ausgetauscht. Alte Sicherungen ohne das zusätzliche Feld bleiben lesbar. Einstellungenpalette und Dashboard erhalten Mode-Änderungen über vorhandene Theme-Abonnements. Header-Schatten und gecachte Input-Glows beachten denselben Modus; Z-Reihenfolge für fixe Header bleibt erhalten.

Der HTML-Designer benennt die Basisfarbe jetzt PRIMARY. Für bestehende Entwurfsdateien bleibt dessen Schema-1-Schlüssel accent samt Stop-Typ accent ein kompatibler technischer Name; er bezeichnet dort die PRIMARY-Basisfarbe des Werkzeugs, nicht die reservierte zweite Highlightfarbe der App. Die Intensitätsregler des Werkzeugs bleiben Entwurfsparameter und sind keine neuen App-Einstellungen.

Gezielte native Prüfungen decken Theme mit unterschiedlichen PRIMARY-/ACCENT-Farben, Modewechsel bei gleichem Control und gleicher Geometrie, unveränderte Auswahl/Listener/Eingabe, Flat-Füllung ohne Verlauf/Glow/Schatten, Rückwechsel sowie Sicherung der Einstellung und separaten Palette ab. Weitere bestehende Komponenten-, HRV-, Graph- und Sicherungsprüfungen erhalten. Keine Geräteprüfung oder APK-Veröffentlichung in diesem Auftrag.


## Freigegebener Chromatic-Entwurf in der App

ChromaticColor als Kotlin-Port, themeabhängige Stärken und Designer-Exportwerte sind jetzt im Glow-Stil aktiv. JSON-Vorlage nur in docs/designs, keine Laufzeitdatei. Gemeinsame Fokus-/Auswahlkontur mit sechs Stops/Alpha, 2 dp bei 66°, Rundung 24 dp, Nah-/Außen-Glow 4/18 dp mit 2-/6-facher Verstärkung, Innenfläche mit verschobenem Basisstopp. Flat bleibt einfarbig und ohne Effekte mit bisheriger Rundung, Layout/Bedienung unverändert. Vorhandener Kontrastschutz bleibt aktiv. Details und konkrete Theme-Werte: input-focus-frame.md. Keine neuen Benutzereinstellungen oder Sicherungsfelder. In APK 0.8.37 / Build 76 enthalten, siehe build-76.md.

Aktueller Preset-Nachtrag: Nutzerdatei „B-Cycle-Eingabedesign (8).json“ ersetzt die bisherigen Build-76-Stilwerte. Jetzt 60 % Hell / 30 % Dunkel, Nah 3,5 dp / 40 % / 200 %, Außen 13 dp / 100 % / 250 % und aktualisierte Theme-Stärken bis 260 %. Details und vollständige Werte in input-focus-frame.md; in APK 0.8.38 / Build 77 enthalten.

## Gemeinsame Chromatic-Dekoration: Header, Footer und Auswahl

Nach Build 76 erweitert, in APK 0.8.38 / Build 77 enthalten. Nur Seiten- und Dialogheader bekommen im Glow-Modus die zentrale Kontur, Füllung und den Glow. Abschnitts-/Accordionüberschriften bleiben unverändert. Die bestehende SECONDARY-Ausnahme der HRV-Header bleibt erhalten; PRIMARY-Header verwenden die zentrale Verlaufsfüllung. Footer spiegeln den vorhandenen Header-Schatten nach oben (12 dp, maximale Alpha 102/255), ohne zusätzliche Höhe oder Touchfläche. Flat unterdrückt diese Effekte.

Aktive Schalterdaumen, native Checkbox-/Radio-Zeilen in Schalterdarstellung und ausgewählte Fahrradkacheln nutzen dieselbe Chromatic-Kontur. Schalterposition, native Gesten, Einfach-/Mehrfachauswahl und Listener bleiben erhalten. Spinner-Popups behalten TERTIARY und bekommen Kontur plus Glow; 8 dp Innenreserve im Popup schützt die Dekoration. Geschlossene Spinner berücksichtigen ihren Rahmen innerhalb der bisherigen 48-dp-Mindesthöhe, damit der Text unten in beiden Modi vollständig passt.

Segmentauswahlen (z. B. Aus / Alle Zonen / Zone 2) bleiben in beiden Modi eckig mit 6 dp Rundung. Die 24-dp-Rundung normaler Glow-Eingabefelder wird nicht auf diese Auswahlbuttons übertragen.

Zwei ausdrücklich wählbare native Komponenten: `BicycleComponents.specialFilled` (Special-Füllung) und `specialOutline` (Special-Rahmen). Beide: normaler Textgrad, weiße Schrift, 900 Black, Pillenform, unverändertes natives Button-Verhalten. Glow: gemeinsame Kontur und Nah-/Außen-Glow, bei Füllung zusätzlich zentraler Flächenverlauf. Flat: PRIMARY einfarbig, ohne Glow/Verlauf/Schatten. Keine vorhandenen Aktionen automatisch ersetzt. Beispiele im [UI-Katalog](previews/ui-catalog.html); dessen CSS-Glow ist eine Näherung, kein Android-Pixelvergleich.

Native Regressionen prüfen Moduswechsel, Geometrie/Listener, echte Verlaufs-Pixel, Auswahlwechsel ohne zurückbleibenden Rahmen, Popup-TERTIARY und Außen-Glow, Spinner-Textgrenzen sowie den gespiegelten Footer-Schatten. Browserprüfung: alle 14 Themes, beide Special-Varianten, Flat/Glow und schmale Hochformatbreiten. Kein Geräte-/GPU-Benchmark.

## HRV-/Abschluss-Ausnahmen nach Build 77

Messscreen-Header und Header des Fahrtabschlussdialogs sind immer schlicht SECONDARY passend zum Inhalt, ohne dekorativen Rahmen/Glow. Die HRV-Angebotsgrafik bleibt unabhängig vom Modus die SCREEN-Neon-Grafik. HRV-Start/Erneut messen verwendet die gemeinsame Special-Rahmen-Dekoration auch auf der vorhandenen Textaktions-View, sodass dynamische Abbruch-/Fehlerzustände erhalten bleiben. Sticky-Kategorieheader jetzt League Gothic 30 sp mit Groß-/Kleinschreibung statt Systemschrift/900 Black; kein zusätzlicher Glow für Kategorien. In APK 0.8.39 / Build 78 enthalten.

## Sticky-Typografie nach Build 78

Alle Sticky-Kategorieheader zeigen League Gothic 30 sp jetzt ausschließlich in Großbuchstaben an. BicycleComponents.sectionTypography ist die gemeinsame Rolle, BicycleSectionHeader markiert den Titel, BicycleDialogStyle erhält die Rolle beim allgemeinen Styling. Damit wird die bisherige nachträgliche Body-Schrift auf Strecken-/FIT-Aufzeichnung, HRV-Fragenauswahl, Benachrichtigungs-Appauswahl und Daten-/Speichergruppen verhindert. Rohtext, Namen und Accessibility-Beschreibung behalten ihre Schreibweise; Farben, Geometrie und Accordion-/Scrollfunktion unverändert. Keine neue APK beauftragt.

## Eingabeecken und Flat-Sticky-Linien nach Build 80

Texteingaben verwenden jetzt auch in Glow die normalen 6-dp-Ecken statt des pillenartigen 24-dp-Exportradius. Rahmen und Nah-/Außen-Glow folgen derselben Eingabekontur; Farben, Stops und Blurwerte unverändert. Explizite Special-Pillen und andere Controls nicht umgestalten. Sticky-Header zeichnen ihre weiße obere/untere Begrenzung nur in Glow; Flat blendet die Linien bei unveränderter Höhe und Abstand aus. Typografie, Accordion-/Sticky-Funktion und alle Eingabefunktionen unverändert.

## HRV-Puls und gemeinsame Wert-Slider

Der HRV-Mess-Glow ist unabhängig von Flat/Glow sichtbar. Jeder Herzschlag startet einen weichen 300-ms-Anstieg von der bestehenden Helligkeit bis zur bisherigen Spitze 45 %, danach Abfall über 1500 ms bis 10 %. Bestehende kurze Rhythmusinterpolation erhalten; neue RR-Pakete beginnen von der zuvor berechneten Deckkraft statt von einer sprungartigen Rücksetzung. Reine Animation, keine RR-/Artefakt-/Messdatenänderung.

Die Einstellungsregler in Trink-Erinnerung/Trackdarstellung/Benachrichtigungsdauer verwenden unabhängig vom Designmodus solide PRIMARY-Pillen mit FOREGROUND-Wert; kein zusätzlicher Glow oder Flächenverlauf im Griff. Tagesbefinden unverändert. Details slider-inventory.md.

## Dezenteres Preset 9, 25.10.2026

Nutzerexport B-Cycle-Eingabedesign (9).json vollständig in die zentrale Stilkonfiguration übernommen: Highlight5 %, Schatten10 %, HELLFIRE COLOR/Custom-Fallback50 % Chromatic Strength (andere Theme-Werte erhalten). Kontur1,5dp/−54°, aktualisierte Stop-Positionen und durchgehend100 % Konturdeckkraft. Flächenwinkel+40°. Nah3,5dp/30 %/300 %, Außen10dp/100 %/250 %. Vorlage unverändert im Repository, kein Laufzeit-JSON. Eingabe-Rundung6dp, explizite Special-Pillen, Wert-Slider und HRV-Puls-Ausnahme unverändert. Noch keine neue APK beauftragt.
