# B|CYCLE 0.8.87

- Einfache Auswahl-Dropdowns verwenden SECONDARY in Flat und Glow.
- Hauptheader einfarbig; Felder, Buttons, aktive Auswahlkacheln und Schalter in beiden Designs wie Flat. Alte dekorative Chromatic-/Blur-Engine aus Appquellen entfernt und samt Verfahren/Offline-Designer im Wiederverwendungsarchiv gesichert. Header-/Footer-Schatten, Trainingshilfe-Glow und abgerundete Sticky-Kästen bleiben designspezifisch; funktionale HRV-/Bosch-Darstellungen erhalten.
- Vier Track-Dropdown-Icons zeigen die jeweils gewählte Trackfarbe und aktualisieren sich bei Farbänderung.
- Haupttitel „Gefahrene Strecke“ vollständig mit angepasster Schriftgröße bei Platzmangel.
- Vektorkarte ohne POI-/Flughafenbeschriftungen. Orts-/Straßennamen nach Ortsgröße/Kartenrang bzw. Straßenklasse priorisiert; bei gleicher Bedeutung bleiben sichtbare Namen bevorzugt. Aktuelle Merkmale über Tile-/Zoom-/Stilwechsel erhalten, entfallene entfernen. Bright-JSON, Farben und generierte Assets unverändert. [Verfahren und VTM-Quellen](vtm-label-stability.md).
- HRV-Einstellungen mit gemeinsamen TERTIARY-Sticky-Karten „HRV“ und „Tagesbefinden“; bisherige Werte und Funktionen erhalten.
- „Feld belegen“ ohne führenden Abstand; Restfläche nach der Trinkerinnerung TERTIARY.

Gezielte Tests für die betroffenen Controls, Auswahl/Persistenz, Trackfarben, POI-/Namensprioritäten, echte Beschriftungsplatzierung, Kartenlayerwechsel und Einstellungsflächen erfolgreich. Zentrale Typografie-/Atlas-/Bright-/Diff-Prüfungen erfolgreich. Kein Gesamtlauf und keine lokale APK. Gerätesichtprüfung, besonders die Beschriftungsruhe bei echtem Pinch-Zoom, steht aus.

[Buildnachweis und Download](build-128.md).
