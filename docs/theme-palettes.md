# 14 Farbpaletten · 13.11.2026

Die7bisherigen normalen PRIMARY-Farben bleiben. Die7Dark-Varianten sind durch neue Zwischentöne ersetzt. Alle14Themes verwenden SECONDARY #161522, TERTIARY #282735 und FOREGROUND #FFFFFF. ACCENT entspricht jeweils PRIMARY. Eigene Themes und funktionale Zonenfarben bleiben unabhängig.

| Theme | PRIMARY = ACCENT |
| --- | --- |
| Hellfire | #BF281B |
| Ember Rush | #D84413 |
| Solar Flare | #F16A09 |
| Gold Rush | #ED8C04 |
| Sunstruck | #E9AF00 |
| Citrus Charge | #C9DB02 |
| Acid Pop | #7ACD04 |
| Emerald Surge | #05C121 |
| Deep Dive | #06B5A1 |
| Tidal Blue | #0C96C6 |
| Blue Riot | #145CD7 |
| Indigo Rush | #311CDC |
| Purple Haze | #9224E0 |
| Neon Bloom | #D01F95 |

Die neuen Farben sind HSV-Mittelpunkte zwischen den angrenzenden normalen Farben: halber kürzester Farbtonbogen, Mittelwerte für Sättigung/Helligkeit, RGB auf8Bit gerundet. Neon Bloom schließt den Kreis von Purple Haze zurück zu Hellfire. Feste Farbwerte statt Laufzeitberechnung.

Normale IDs bleiben preset:<Name>-color. Alte preset:<Familie>-dark-IDs lösen auf das jeweils nachfolgende neue Zwischen-Theme auf; auch ältere Alias-IDs werden über dieselbe Kompatibilitätsauflösung weiter erkannt. ThemeManager schreibt beim Laden die kanonische neue ID. Profile und alte Sicherungen bleiben lesbar, ohne zusätzliche Migrationsflags oder eigene Paletten zu verändern. Die helle Einstellungsprojektion verwendet weiterhin ihre bestehende Berechnung.

Tests für14eindeutige Farben, gemeinsame Flächen, ACCENT-Gleichheit, alte IDs/Neustart, Auswahl und helle Projektion: fourteen-primary-themes-final-tests.log. UI-Katalogparser auf14einzelne palette-Einträge umgestellt und Katalog regeneriert.
