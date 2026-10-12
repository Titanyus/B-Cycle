# Color Engine V2 – ACCENT

ThemeTokens/ThemeRole enthalten jetzt PRIMARY, SECONDARY, TERTIARY, FOREGROUND und
ACCENT. ACCENT ist ein Farbwert, kein Layoutmerkmal und kein zusätzlicher Theme-Typ.
Farbige Presets behalten ihre bisherigen vier Werte und ACCENT = FOREGROUND.
BLACK OPS verwendet PRIMARY #535353 (unverändert), SECONDARY #000000,
TERTIARY #292929, FOREGROUND #FFFFFF und ACCENT #D1261B.
Header bleiben unverändert PRIMARY.

Der zentrale semantische Icon-Katalog definiert diese positiven Hervorhebungen:
`ActiveOnMap`, `CloudUploaded`, `LocalSaved`. Alle anderen Icons, negative Zustände,
Standardtexte und Trennlinien bleiben FOREGROUND. Die drei Zustände im Streckenmanager
verwenden diese zentrale Zuordnung; Geometrie, Touchflächen und Statuslogik bleiben gleich.
Die bestehenden Divider-/SectionDivider-Deckkraftregeln werden nicht verändert.
Bosch-Modusfarben, Akkuwarnungen und Karten-/Aufzeichnungsstatusfarben bleiben funktional
und verwenden nicht ACCENT.

Neue Custom-Themes speichern fünf Farben in JSON-Version 2. Version 1 bleibt lesbar;
fehlendes ACCENT wird beim Laden auf FOREGROUND gesetzt (auch in Version 2).
Ein vorhandener ungültiger ACCENT-Wert wird nicht als fehlend behandelt. Änderungen,
Kopieren, aktive Theme-ID und Observer nutzen weiterhin denselben ThemeManager.
Kein zusätzlicher Menüpunkt oder Farbwähler.

Alle fünf Farben müssen deckend sein. FOREGROUND benötigt weiterhin 4,5:1 auf allen
Flächen; ACCENT für die zentralen Highlights mindestens 3:1 auf SECONDARY.
Für andere Hintergrundrollen löst `iconColor` den Kontrast ebenfalls auf und fällt
bei weniger als 3:1 auf FOREGROUND zurück. Standardtexte bleiben FOREGROUND.
Benachbarte Flächen müssen erkennbar verschieden sein, dürfen jedoch heller oder
 dunkler werden; eine monoton dunklere Palette ist keine Voraussetzung mehr.

#D1261B erreicht auf Schwarz 4,00:1, auf #292929 nur 2,77:1. Highlights auf der
schwarzen Dialogfläche können daher rot sein; auf grauen Flächen gilt die allgemeine
Foreground-Rückfallregel. Der Header behält seine bisherige Primary-Farbe.
Es gibt keinerlei Sonderregel anhand des Namens BLACK OPS oder anderer Theme-Namen.

Tests prüfen Preset-Kompatibilität, Accent-Kontrast/Transparenz, Migration von vier
Farben, Speichern/Kopieren/Neustart, korrekte native Icon-Pixel und unveränderte Maße
sowie Themewechsel zwischen Standard- und Highlight-Icons.
