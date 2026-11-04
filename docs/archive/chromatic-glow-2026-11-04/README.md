# Archiv: Chromatic-Kontur, Farbverlauf und Glow

Unveränderter Quellstand vor Entfernung der Hauptheader-Dekoration am 04.11.2026. Herkunftscommit und SHA-256 jeder Originaldatei stehen in [manifest.json](manifest.json). Dieser Ordner ist Dokumentation und wird nicht in die Android-App kompiliert. Keine neue Lizenz für fremde Projekte wird hiermit festgelegt; beiliegende Schriftlizenz beachten.

## Direkt wiederverwenden

- [Offline-Designer](docs/previews/input-designer.html): lokal im Browser öffnen, keine Dienste oder Pakete nötig. Enthält Farben, Font samt Lizenz, Vorschau, Parameter und JSON-Export/Import.
- [ChromaticColor.kt](app/src/main/java/de/bcycle/app/ChromaticColor.kt): reine Kotlin-Farbrechnung ohne Android-Abhängigkeit; Package/`internal` bei Übernahme anpassen.
- [chromatic-color.js](docs/previews/chromatic-color.js): dieselbe Rechnung ohne DOM/Canvas-Abhängigkeit.
- JavaScript-Referenzprüfung aus diesem Archiv: `node scripts/test-chromatic-color.cjs`.
- [Freigegebene Parameter](docs/designs/input-design.json), [ausführliche Berechnung](docs/input-focus-frame.md) und [Designerbedienung](docs/input-designer.md).

## Berechnungsverfahren

1. sRGB-Kanäle linearisieren, mit den D65-Matrizen nach OKLab/OKLCH umrechnen. PRIMARY bleibt unverändert; Hell-/Dunkeltöne werden mit 5 % Highlight und 10 % Schatten sowie der gewählten Chromatic-Stärke abgeleitet. Farbraumgrenzen über Chroma-Reduktion bei erhaltener Zielhelligkeit/Farbton behandeln; genaue Toleranzen im Originalcode.
2. Kontur aus sechs freigegebenen Stops: Hell, Dunkel, Basis, Hell, Basis, Dunkel; Winkel −54°, Breite 1,5 dp. Füllung mit +40° von Basis (bis 29,731865 %) nach Dunkel (100 %). Winkelkonvention: 0° nach rechts, positive Winkel im Uhrzeigersinn. Shader-Endpunkte über Projektion der Rechteckausdehnung auf die Verlaufsrichtung bestimmen.
3. Bei Textfüllungen Schatten nur so weit anwenden, dass der bisherige Textkontrast von 4,5:1 erhalten bleibt. Paletten und Kontrastschatten begrenzt cachen.
4. Dieselbe Kontur in ein Bitmap zeichnen. Drei separierbare Box-Blur-Pässe auf gemeinsam premultiplizierten Farb-/Alphakanälen nähern Gauß-Blur an. Nah: 3,5 dp, Intensität 3,0, Alpha 0,3; außen: 10 dp, Intensität 2,5, Alpha 1,0. Intensität nach Blur und vor Deckkraft/SCREEN anwenden. Alpha verstärken, Farbe nicht nochmals aufhellen.
5. Glow an Geometrie/Farbe/Stärke binden und cachen, Raster auf 512.000 Pixel begrenzen. Parent-Overlay mit realen Fensterkoordinaten, Scroll-/Sticky-Clipping und Freigabe bei Detach/Fokusverlust/Flat verwenden. Keine Palette oder Blur pro Frame neu rechnen.

## Android-Integration und ursprüngliche Header

`BicycleAccentGradient` baut Palette/Shader/Füllung, `BicycleInputStyle` enthält Stops/Theme-Stärken, `BicycleContourBlur` die Blur-Rechnung. `BicycleInputFrame` verbindet Kontur, Füllung, Overlay und Lebenszyklus; dessen archivierte Methode `header` enthält die bisherige Hauptheader-Variante mit Radius 0 und aktiver Kontur. `BicycleNavigation` und `BicycleDesign` zeigen die Aufrufe und alle damaligen Tokens. `BicycleChromaticSurface`, Auswahlkarte und Spezialbutton liefern weitere Anwendungsbeispiele. Tests sind als damalige Referenz mitgesichert, einschließlich historischer UI-Erwartungen.

Die Android-Dateien sind ein Quellsnapshot, keine eigenständige Android-Bibliothek. In einem anderen Projekt sind Theme-/Appearance-Binding, dp-Tokens, View-Tags (`R.id`), Lifecycle-/Overlayhost und Sticky-Clipping an die dortige UI anzupassen. `BicycleDesign` enthält weitere App-Komponenten als Kontext; nur benötigte Teile übernehmen. Der archivierte Designer-Generator benötigt zusätzlich die UI-Katalog-Infrastruktur des Herkunftsprojekts; die erzeugte HTML-Datei funktioniert allein.

## Verwendung nach der Änderung in B|CYCLE

Die Hauptheader verwenden einfarbig ihre jeweilige Theme-Rolle, ohne diese aktive Kontur, Verlauf und Glow. Die gemeinsame Engine bleibt für fokussierte Eingabefelder, aktive Auswahlkacheln, Glow-Schalter sowie Spezialbuttons im Einsatz. HRV- und Trainingszonen-Effekte sind davon unabhängige funktionale Darstellungen.
