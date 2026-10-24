# Gemeinsame Eingabekontur und PRIMARY-Flächen

Die App verwendet jetzt dieselbe OKLCH-Chromatic-Engine wie der Designer. Freigegebene Vorlage: [input-design.json](designs/input-design.json), übernommen aus dem Nutzerexport. Die JSON bleibt zur Nachvollziehbarkeit im Repository; die App liest sie nicht zur Laufzeit. Zentrale Kotlin-Werte stehen in BicycleDesign und BicycleInputStyle, reine Farbrechnung in ChromaticColor. ThemeTokens behalten ihre fünf Rollen und bestehende Sicherungs-/Profilformate. Keine neue App-Einstellung.

Basis ist immer das aktuelle PRIMARY, unverändert. Highlight-Stärke 40 %, Schatten-Stärke 50 %. Chromatic Strength unterstützt 0–300 % und wird anhand der tatsächlichen Theme-ID gewählt: HELLFIRE/SOLAR FLARE 150 %, SUNSTRUCK 150 % bzw. DARK 148 %, ACID POP 140 %, DEEP DIVE 180 %, BLUE RIOT 70 %, PURPLE HAZE 170 %. Benutzerdefinierte Themes erhalten den Export-Ausgangswert 150 %. Die Theme-ID wird beim gemeinsamen Binden der View mitgeführt, damit Settings-Hellprojektion oder gleiche PRIMARY-Werte nicht die unterschiedlichen Theme-Stärken verlieren.

Kontur 2 dp, Winkel +66° (0° rechts, positiv im Uhrzeigersinn). Sechs sortierte Stops bei 0,473186 / 16,088327 / 47,949529 / 51,5 / 73,265 / 100 %: Hell 80 % Deckkraft, Dunkel 65 %, Basis, Hell, Basis, Dunkel. Innenfläche -115°, Basis bis 29,731865 % und Dunkel bei 100 %. Glow-Rundung 24 dp für Eingaben und Segmentauswahl einschließlich ihres Gruppenrahmens. Nur die Musterbreite 162 und -höhe 52 aus dem Designer werden nicht auf die App-Layouts übertragen: Feldmaße, Abstände, Touchflächen, Padding, Text, Masken, Einheiten, Validierung, Tastatur, Fokuslistener und Autosave bleiben erhalten.

Aus exakt derselben Kontur entstehen zwei gecachte Blur-Bitmaps: nah 4 dp innen/außen, 46 % Deckkraft und Intensität 200 %; weit 18 dp nur außen, 100 % Deckkraft und Intensität 600 %. Verstärkung des Alphakanals erfolgt nach dem Blur und vor Deckkraft/SCREEN. Farbe wird nicht zusätzlich aufgehellt. Drei separierbare Box-Pässe auf premultiplizierten Farb-/Alphakanälen bleiben die native schnelle Gauß-Näherung; Browser-Blur und native Näherung können sichtbar abweichen. Keine Graphenänderung oder Graphen-Blur.

Farben und kontrastgeschützte Füllschatten werden in begrenzten Caches gehalten; Blur-Neuberechnung nur bei geänderter Geometrie, PRIMARY oder Theme-Stärke. Fokusverlust, disabled, Flat-Modus und Detach geben den Blur-Cache frei; Rastergrenze 512.000 Pixel. Parent-Overlay, korrekt mitwandernder Scroll-Glow und Sticky-Clipping erhalten. Keine Farbrechnung in jedem Zeichenframe.

Der vorhandene Textkontrastschutz bleibt erhalten: Bei Bedarf weniger stark abdunkeln, damit der gewählte Vordergrund mindestens 4,5:1 behält. Deshalb kann die tatsächliche Innenfläche bei hellen Themes schwächer abgedunkelt sein als der ungeschützte Designerentwurf. Die freie Highlight-/Schattenpalette selbst entspricht dem Designer. Flat zeichnet weiterhin PRIMARY einfarbig ohne Shader/Glow/Schatten und behält seine bisherige Rundung; Bosch-Modusfarben unabhängig.

Prüfungen: 104 eingefrorene JavaScript-Referenzfälle über alle 14 Themes, zusätzliche Extremfarben/Weiß/Schwarz und Stärken bis 300 %, Kotlin-Vergleich mit höchstens einer sRGB-Codestufe Rundungstoleranz; die aktiven Solar-Flare-Exportfarben stimmen exakt. Konfigurationstest gegen vollständige JSON, Theme-ID bei gleicher Basis/Settings-Hellprojektion, native Blur-Verstärkung, Quelle unverändert, Scroll-/Sticky-/Cache-/disabled- und Flat-/Glow-Prüfungen sowie native Eingabe-/Listener-/Theme- und Sicherungsregressionen. Native Darstellung visuell geprüft; kein Hardware-Benchmark/Gesamtlauf. In APK 0.8.37 / Build 76 enthalten, siehe [Buildnachweis](build-76.md).

[Interaktiver Designer](input-designer.md) · [Designmodi](design-modes.md).

## Scrollkorrektur 24.10.2026

Der native ScrollView-Overlay-Canvas trägt beim Zeichnen über einen Elterncontainer bereits den negativen Scrollversatz. Die aus Fensterpositionen abgeleitete Feldposition ist dagegen schon viewportbezogen. Den Canvas-Versatz deshalb genau einmal neutralisieren, bevor Kontur-Blurs und Viewport-/Sticky-Clips gezeichnet werden. Verhindert doppelte Scrollgeschwindigkeit, ändert weder Blur noch Feld-/Scrollfunktion.

Regression zeichnet über den tatsächlichen Elterncontainer statt ScrollView.draw isoliert. So ist der frühere Fehler reproduzierbar; Farbwerte direkt neben dem Feld müssen nach Scrollen identisch mitwandern. Sticky-Header weiterhin aussparen.

## Gemeinsame Modi und PRIMARY, 24.10.2026

Die oben als ACCENT bezeichnete Stilbasis ist jetzt PRIMARY. ACCENT bleibt eigenständig gespeichert. Aktive Segmente und Fokusfelder verwenden dieselbe Kontur-/Blur-Dekoration. Flat-Design zeichnet die PRIMARY-Kontur einfarbig und unterdrückt beide Glows; Glow-Design (Standard) ergänzt die oben dokumentierte freigegebene Kontur, Rundung und OKLCH-Ableitung auf demselben Layout. Details design-modes.md.
