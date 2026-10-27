# UI-Atlas

[HTML herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/docs/previews/ui-catalog.html), speichern und im Browser öffnen. Die einzelne Datei enthält Schriften, Icons, Styles, JavaScript und Lizenzen; sie lädt nichts extern nach und verändert keine App-Einstellungen.

20 Bauteilgruppen zeigen Typografie, Eingaben, Buttons, Auswahlen, Schalter, Accordions, Slider, Header, Statusmeldungen, Profilkacheln, Importliste, Messwerte, Graph und HRV-Einstieg. Themeauswahl mit 14 tatsächlichen App-Paletten, helle Einstellungen, Breiten 320/360/412 und Schriftvergleich machen Varianten sichtbar. Ein Klick auf ein Element zeigt seine Browsermaße und die zugehörigen zentralen App-Werte mit Quelldatei und Zeile. Suche und Abstandsmarkierungen helfen beim Vergleichen.

Blau bezeichnet App-Werte beziehungsweise Browsermessungen. Orange bezeichnet Vorschläge. Die interaktiven Abstandsregler wirken **nur im HTML-Entwurf**; ihre aktuellen Ausgangswerte orientieren sich an den inzwischen übernommenen App-Regeln. Die anschließend beauftragte konkrete Slidergruppierung ist im App-Quellstand umgesetzt: 24 dp zwischen Fragenblöcken, 4 dp zur Skala und 50 % Deckkraft der Skalenbegriffe. Feldbezogene Inline-Rückmeldungen erhalten 4 dp Abstand nach oben. Der allgemeine Formular-Abstandsentwurf bleibt separat. Der aktuelle Schriftwechsel zu League Gothic und die zentralen Schriftrollen sind dagegen im Android-Quellstand umgesetzt, seit APK 0.8.24 enthalten.

Der Atlas ist ein prüfbares Layoutmodell, keine pixelgenaue Android-Emulation. Browser-Systemschrift, native Dropdowns, Slider, Textmetrik und Touchflächen können abweichen. CSS-px entsprechen bei 100 % näherungsweise dp und sp bei Schriftfaktor 1. Fachliche Aktionen und Speicherung sind nicht angebunden.

## Pflege

Vorlage: `docs/previews/ui-catalog.template.html`. Generator liest zentrale Konstanten, Themefarben und Icons aus dem App-Code und bettet die lokalen Schriftdateien ein:

```sh
python3 scripts/build-ui-catalog.py
python3 scripts/build-ui-catalog.py --check
python3 scripts/check-ui-typography.py
```

Nach Änderungen an den erfassten Designquellen den Katalog neu erzeugen; neue Komponenten müssen bewusst in der Vorlage ergänzt werden. Der Generator übernimmt keine beliebigen neuen Screens automatisch. Beide Prüfungen laufen auch im begrenzten manuellen Regressionstestskript. Die Schriftprüfung verhindert direkte lokale Schriftfamilien außerhalb von BicycleDesign. [Schriftrollen und Herkunft](fonts.md).

## Prüfstand 22.10.2026

56 unterschiedliche gezielte Android-Hosttests erfolgreich, unter anderem Eingaben/Passwortmaskierung, Header-Lauftext, Tagesbefinden, HRV, Dashboard und Fahrtbenennung. Browserprüfung mit Chromium: 20 Gruppen, 14 Themes, eingebettete Schriften, Inspektor, variable Abstände, Auswahlen, Accordion, Suche und Breiten 320/360/412 ohne Seitenüberlauf; keine externen Requests. Mobilansicht visuell geprüft. Kein vollständiger Gesamttestlauf, Hardwaretest oder APK-Build.

Nachtrag Slider/Feldhinweise: 8 gezielte bestehende Android-Tests (HrvQuestionnaireTest, CentralUiPresentationTest) erfolgreich. Browserprüfung einschließlich tatsächlich gemessener 4-dp-Abstände und 24-dp-Gruppenabstand erfolgreich; native Tagesbefinden-Ansicht und HTML-Slider visuell geprüft. Vorschau auf separatem Branch preview/ui-catalog, ausschließlich HTML; App-Änderungen lokal, main bislang unverändert. Kein APK-Build.

Beschriftungen über Controls jetzt einheitlich 4 dp (fieldLabelGap) in den drei gezeigten Beispielen. Zusätzlicher Entwurfsregler „Slider → Skalenbeschriftung“ (0–24 dp, Ausgang 4 dp) arbeitet unabhängig von Gruppen- und Labelabstand, ohne App-Einstellungen zu verändern.

Prüfstand Beschriftungsabstand/Skalenregler: 11 gezielte Android-Tests erfolgreich (6 Tagesbefinden, 5 Formular-/Seitenauswahl-Abstände). Browser prüft tatsächliche 4 dp in allen drei gezeigten Feldbeispielen und unabhängige Verstellung von Label- und Skalenabstand, zusätzlich bestehende Atlas-Bedienung/320–412-Breiten. Kein Gesamtlauf oder APK.

Finale Startwerte für APK 0.8.24: Gruppen 32 dp, Label → Slider 0 dp, Slider → Skala −8 dp. Der Skalenregler unterstützt −16 bis 24 dp. Eingabefeld-Labelabstand bleibt 4 dp.

APK 0.8.24 / Build 63 veröffentlicht; alle zuvor lokalen Schrift-/UI-Änderungen stehen jetzt auf main. Die Veröffentlichungsaussagen früherer Zwischenstände oben sind Verlauf. [Buildnachweis](build-63.md).

Ergänzung27.10.2026: Abschnitt06 enthält den neuen Dropdown-Rahmen als normale Variante, mit rechtem Schalter und mit rechtem Spezial-Füllung-Button. Inhalt bleibt im gemeinsamen Rahmen, Chevron folgt dem Klappzustand; Controls rechts schalten/handeln unabhängig. Inspector zeigt die zentralen16dp-/24dp-Innentokens. [Native Komponente und Regeln](dropdown-frame.md).
