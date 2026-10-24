# Flat und Glow auf gemeinsamer UI-Basis

Stand 24.10.2026. Glow-Design ist auf Nutzerwunsch Standard. Unter Design & UI bei der Theme-Auswahl steht der globale Schalter Flat-Design. Aus = Glow, ein = Flat. Bestehende Nutzer starten ohne zusätzliche Migration mit Glow.

Die Varianten verwenden dieselben nativen Views, Layouts, Maße, Beschriftungen und Listener. BicycleAppearance entscheidet nur über Dekoration. Keine zweite Bedienlogik und keine modusspezifischen Datenmodelle. Auch nach einem Wechsel bleiben Eingaben, Auswahl, Fokus, Navigation und Aufnahmefunktionen erhalten.

- Flat: einfarbige PRIMARY-Hervorhebung, kein dekorativer Kontur-/Flächenverlauf, kein Eingabe-/Auswahl-/HRV-/Zonen-Glow, keine dekorativen Schatten. Diagrammfüllungen ebenfalls einfarbig bei gleicher Datengeometrie. Der Bosch-Fahrmodus-Glow bleibt unabhängig aktivierbar.
- Glow: gemeinsame Flat-Geometrie mit abgeleitetem Flächen- und Konturverlauf, gecachten Nah-/Außen-Blurs an Fokusfeldern und aktiven Segmenten sowie Schatten aktiver Segmente und bestehender Hinweis-/HRV-Karten. Bestehende semantische Diagramm-/Zonenfarben erhalten.
- Alle bisherigen ACCENT-Hervorhebungen verwenden PRIMARY. ACCENT bleibt in ThemeTokens, gespeicherten benutzerdefinierten Paletten und Backups separat erhalten. Kein Überschreiben oder Zusammenlegen der beiden Farben. Kontrastschutz für kleine Icons und Text bleibt bestehen.

Die Einstellung uiTheme.flatDesign wird in DESIGN gesichert und wiederhergestellt; sie ist appweit und wird beim Fahrradprofilwechsel nicht ausgetauscht. Alte Sicherungen ohne das zusätzliche Feld bleiben lesbar. Einstellungenpalette und Dashboard erhalten Mode-Änderungen über vorhandene Theme-Abonnements. Header-Schatten und gecachte Input-Glows beachten denselben Modus; Z-Reihenfolge für fixe Header bleibt erhalten.

Der HTML-Designer benennt die Basisfarbe jetzt PRIMARY. Für bestehende Entwurfsdateien bleibt dessen Schema-1-Schlüssel accent samt Stop-Typ accent ein kompatibler technischer Name; er bezeichnet dort die PRIMARY-Basisfarbe des Werkzeugs, nicht die reservierte zweite Highlightfarbe der App. Die Intensitätsregler des Werkzeugs bleiben Entwurfsparameter und sind keine neuen App-Einstellungen.

Gezielte native Prüfungen decken Theme mit unterschiedlichen PRIMARY-/ACCENT-Farben, Modewechsel bei gleichem Control und gleicher Geometrie, unveränderte Auswahl/Listener/Eingabe, Flat-Füllung ohne Verlauf/Glow/Schatten, Rückwechsel sowie Sicherung der Einstellung und separaten Palette ab. Weitere bestehende Komponenten-, HRV-, Graph- und Sicherungsprüfungen erhalten. Keine Geräteprüfung oder APK-Veröffentlichung in diesem Auftrag.


## Freigegebener Chromatic-Entwurf in der App

ChromaticColor als Kotlin-Port, themeabhängige Stärken und Designer-Exportwerte sind jetzt im Glow-Stil aktiv. JSON-Vorlage nur in docs/designs, keine Laufzeitdatei. Gemeinsame Fokus-/Auswahlkontur mit sechs Stops/Alpha, 2 dp bei 66°, Rundung 24 dp, Nah-/Außen-Glow 4/18 dp mit 2-/6-facher Verstärkung, Innenfläche mit verschobenem Basisstopp. Flat bleibt einfarbig und ohne Effekte mit bisheriger Rundung, Layout/Bedienung unverändert. Vorhandener Kontrastschutz bleibt aktiv. Details und konkrete Theme-Werte: input-focus-frame.md. Keine neuen Benutzereinstellungen oder Sicherungsfelder. In APK 0.8.37 / Build 76 enthalten, siehe build-76.md.
