# Interaktiver Eingabefeld-Designer

[Designer im Browser öffnen](https://raw.githack.com/Titanyus/B-Cycle/e44df1c8f1c509df0a4eb82558451fd67d707da5/docs/previews/input-designer.html) · [HTML für Offline-Nutzung herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/e44df1c8f1c509df0a4eb82558451fd67d707da5/docs/previews/input-designer.html)

Beim ersten Öffnen des Browser-Links zeigt raw.githack gegebenenfalls einen Herkunftshinweis. Mit „Open the page“ geht es zum Designer.

Die einzelne HTML-Datei enthält League Gothic samt Lizenz und die 14 App-Paletten, lädt keine externen Ressourcen und funktioniert nach dem Herunterladen offline. Kontur und aktive Innenfläche lassen sich unabhängig bearbeiten: Winkel, verschiebbare Farbstops, schmale Highlights, Theme-Ableitungen, eigene Farben, Konturstärke sowie Nah- und Außen-Blur mit SCREEN. Die Ebenenschalter helfen beim Vergleich. Felder und Segmentauswahl sind sofort in der Vorschau sichtbar.

Mit „JSON exportieren“ werden alle Parameter und abgeleiteten Farben gespeichert. Diese Datei kann später wieder importiert oder für die Übernahme in die App weitergegeben werden. „PNG exportieren“ speichert die Vorschau. Browseränderungen bleiben lokal gespeichert; Zurücksetzen stellt den Startentwurf des Designers wieder her. Der Designer ändert keine App-Einstellung und überträgt keine Daten automatisch.

Erzeugung: `python3 scripts/build-input-designer.py`; Konsistenz: derselbe Aufruf mit `--check`. Quelle ist input-designer.template.html; Paletten und Schrift kommen aus dem Repository. Browser-Blur und native Box-Blur-Näherung können geringfügig abweichen. Die Chromatic-Engine ist zunächst nur im Designer aktiv; die Android-App behält ihre bisherige Farbberechnung.

Geprüft in Chromium bei 1440 und 390 Pixel Breite: keine horizontale Überbreite, alle 14 Paletten und Schrift, Live-Änderungen, Stops ziehen, Ebenen, JSON-Export/Import, ungültiger Import, Wiederherstellung nach Reload. Öffentlicher Vorschau-Link mit HTTP 200 und denselben funktionalen Browserprüfungen nach Bestätigung des Herkunftshinweises geprüft.


## Nachtrag: Glow-Intensität und aktive Auswahl

Zusätzlich zur Deckkraft gibt es für nahen und äußeren Schein je einen Intensitätsregler von 0 bis 2000 % (0–20-fach). Standard 100 % erhält die bisherige Wirkung. Verstärkt wird ausschließlich der Alphakanal des bereits weichgezeichneten Konturbildes; danach folgen Deckkraft und SCREEN/Normal. Dadurch lassen sich dünne, weit verteilte Konturen aufhellen, ohne Blur-Radius oder Konturstärke zu verändern. Die Deckkraft bleibt bei 0–100 %. Sehr hohe Verstärkung sättigt den Schein entsprechend.

Die aktive Auswahl verwendet dieselbe Verlaufs-Kontur und beide Glows wie das Fokusfeld. Zusätzlich erhält sie den separat eingestellten Flächenverlauf. Inaktive Auswahl unverändert; Ebenenschalter wirken auf beide aktiven Beispiele.

JSON und lokale Speicherung enthalten `near.intensity` und `outer.intensity`. Alte Schema-1-Entwürfe ohne diese Werte werden mit 1 (= 100 %) geladen; unzulässige Werte werden abgewiesen. Neue Intensitätseinstellung und Auswahlkontur sind zunächst nur im Visualisierer umgesetzt, nicht in APK 0.8.35.

Gezielte Chromium-Prüfungen: nachgewiesene Helligkeitssteigerung außerhalb der aktiven Auswahl bei gleicher Blur-Breite, gemeinsame Kontur-/Glow-Ebenenschalter ohne Änderung der Innenfläche, Altimport/ungültige Werte und JSON-/Reload-Erhalt der Intensität. Bisherige Desktop-/Mobil-, Farb-/Stop- und Exportprüfungen weiter erfolgreich. Native App unverändert, kein APK-Build.


Die Stil-Basisfarbe heißt ab 24.10.2026 PRIMARY. Bestehende Schema-1-Entwürfe behalten die technischen Namen accent/Stop-Typ accent für diese Basisfarbe, damit Export/Import kompatibel bleiben. Das ist unabhängig vom weiterhin separat gespeicherten ACCENT der App. Siehe design-modes.md.


## Chromatic Color Engine

Schlanke, reine Rechenfunktionen in `docs/previews/chromatic-color.js`, vom Generator direkt in die einzelne Offline-HTML eingebettet. Keine Bibliothek, keine Netzabfragen und keine Android-Änderung. Die technischen Stop-Typen accent/light/dark bleiben kompatibel; in der Oberfläche heißen sie Basis/Hell/Dunkel. Eigene HEX-Farben und sämtliche bisherigen Verlauf-/Glow-Einstellungen bleiben erhalten. Alle 14 vorhandenen Themes unterstützt; Basis wird exakt übernommen.

Drei Regler: Highlight-Stärke (lighten) und Schatten-Stärke (darken) mit 0–100 %, Chromatic Strength (chromatic) mit 0–300 %. Standard 35/35/50 %. Highlight und Schatten sind unabhängig; Chromatic Strength beeinflusst ausschließlich den Farbton, nicht die Zielhelligkeit. Berechnete Basis-/Hell-/Dunkel-Farben werden mit HEX angezeigt und sind für Kontur, Innenfläche, Glows, Vorschau und Export gemeinsam wirksam.

Portierbare Berechnung: sRGB nach linearem RGB, D65-OKLab-Matrizen, C=hypot(a,b), h=atan2(b,a). Für Stärke t: Highlight-L=L+(1-L)*t, Schatten-L=L*(1-t). C wird nichtlinear mit (1-t)^1,5 bzw. (1-t)^1,15 reduziert. Die kontinuierliche Farbtonverschiebung ist k*sin(t*pi/2)*min(1,C/0,04)*sin(Zielton-h)*Maximalwinkel: Highlight-Zielton 100°/Winkel 32°, Schatten 300°/45°. Neutrale Farben erhalten keinen Farbstich. Highlight-/Schatten-Stärke 0 % liefert die Originalfarbe, 100 % exakt Weiß bzw. Schwarz.

Gamutgrenzen: Bei Bedarf C mit 20 Bisektionsschritten reduzieren, L und h als Ziele erhalten. Eine lokale Clipping-Toleranz von 0,01 OKLab-Distanz vermeidet den Sprung einer streng radialen Begrenzung an der nicht konvexen Gamutspitze von reinem Blau. Ziel-L bleibt von k unabhängig; das gerenderte 8-Bit-HEX kann durch diese begrenzte Gamutabbildung und Rundung geringfügig abweichen. Kein diskreter Hue-Sektorwechsel oder Wechsel bei 0°/360°. Funktionen verwenden nur Double-Arithmetik, Potenzen, trigonometrische Funktionen und Arrays; später direkt nach Kotlin übertragbar. Palette wird bei unveränderten vier Eingaben gecacht.

Schema 1 und Browser-Speicherschlüssel bleiben erhalten. Alte Entwürfe ohne chromatic erhalten 0,5; vorhandene lighten/darken-Werte und eigene Farben bleiben unverändert gespeichert, die abgeleiteten Farben werden neu berechnet. Export enthält chromatic, die bisherigen Regler und `derived.engine=oklch-chromatic-v1` sowie `derived.colors` und aufgelöste Stop-Farben. Ungültige Werte werden abgewiesen, ohne den aktuellen Entwurf zu ersetzen. Zurücksetzen ist der Designer-Startentwurf, keine Änderung der App.

Prüfung: `node scripts/test-chromatic-color.cjs` (ohne Pakete) prüft alle Themes, reine RGB-/Neutralfarben, Weiß/Schwarz, unabhängige Stärke, nichtlineare Entsättigung, Zielhelligkeit unter Gamut-/HEX-Toleranz und Kontinuität über alle Farbtöne. Chromium Desktop/Mobil: bestehende Stops/Gesten/Ebenen, neue Regler und HEX, alle Themes, eigene Farben, alte/ungültige JSONs, Export/Import/Reload, PNG und Glow-Intensität erfolgreich geprüft. Öffentlicher Vorschau-Link ausschließlich auf separatem Vorschau-Branch, kein App-Build.


## Chromatic Strength bis 300 %, je Theme

Der Regler und die Engine erlauben jetzt 0–300 % (intern 0–3); die Winkelauslenkung skaliert linear weiter, wird nicht mehr bei 100 % gekappt. Highlight-/Schatten-Stärke bleiben bei 0–100 %. Standard für jedes bislang unangepasste Theme ist weiter 50 %; keine geratenen individuellen Theme-Vorgaben.

Nur Chromatic Strength wird je Theme gemerkt: `chromaticByTheme` ordnet stabile Theme-IDs den Werten zu. Wechseln stellt den Wert einschließlich Zahlenfeld/Slider/Palette wieder her. Eine manuell geänderte PRIMARY-Basis verwendet den Chromatic-Wert des gewählten Themes. Die übrigen Verlauf-, Glow- und Highlight-/Schatteneinstellungen bleiben wie bisher gemeinsam. JSON und Browser-Speicherung enthalten die Theme-Tabelle und für Kompatibilität weiter den aktiven skalaren Wert `chromatic`.

Alte Dateien ohne Tabelle übernehmen ihren bisherigen globalen Wert zunächst für alle vorhandenen Themes; ohne alten Wert 50 %. Danach lassen sich alle Themes unabhängig anpassen. Bei neuer Tabelle gilt deren aktiver Theme-Wert; fehlende Einträge für andere Themes verwenden beim ersten Wechsel 50 %. Zurücksetzen entfernt alle individuellen Theme-Werte. Werte außerhalb 0–3 und ungültige Tabellen werden abgewiesen. Export/Import und Reload erhalten alle Theme-Werte.

Gezielte Prüfungen erfolgreich: Engine bis 300 % einschließlich Kontinuität/Gamut und unveränderter Weiß-/Schwarz-Endpunkte, 14 unabhängige Theme-Werte im Browser, echte Wirkung oberhalb 100 %, Regler-/HEX-/Vorschauwechsel, JSON-Datei/Reload, beide Altformate, Validierung, Zurücksetzen und Mobilbreite. Bestehende Verlauf-/Stop-/Glow-/PNG-Prüfungen weiterhin erfolgreich. Ausschließlich Designer, keine Android-Änderung oder APK.
