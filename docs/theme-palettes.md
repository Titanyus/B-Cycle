# Vorgegebene Themes

Alle bisherigen Themes werden durch 14 feste Basispaletten ersetzt. Die Oberfläche bietet ausschließlich diese Vorgaben an. Je Familie gibt es eine farbige und eine DARK-Variante; PRIMARY ist gleich. FOREGROUND ist immer #FFFFFF. ACCENT ist bei farbigen Flächen #FFFFFF, bei DARK gleich PRIMARY.

| Familie | PRIMARY | SECONDARY farbig | TERTIARY farbig | SECONDARY DARK | TERTIARY DARK |
| --- | --- | --- | --- | --- | --- |
| HELLFIRE | #BF281B | #812218 | #550E08 | #0F0A09 | #211918 |
| SOLAR FLARE | #F16A09 | #B64E01 | #863701 | #100B08 | #221A15 |
| SUNSTRUCK | #E9AF00 | #B78901 | #8F6A00 | #100E07 | #242014 |
| ACID POP | #7ACD04 | #5EA000 | #477B01 | #0A0F07 | #192215 |
| DEEP DIVE | #06B5A1 | #018979 | #006559 | #07100F | #142221 |
| BLUE RIOT | #145CD7 | #003998 | #00215E | #080B10 | #151B23 |
| PURPLE HAZE | #9224E0 | #6601A1 | #400068 | #0B0910 | #1A1624 |

IDs: `preset:<familie>-color` bzw. `preset:<familie>-dark`, Familie kleingeschrieben mit Bindestrichen. Die exakten vom Nutzer gewählten Paletten bleiben auch bei geringem Kontrast oder subtilen Flächenabständen erhalten. Vorhandene per-Fläche-Fallbacks für lesbare Icons bleiben bestehen. Benutzerdefinierte Altdaten werden weiterhin streng geprüft; es gibt keinen UI-Einstieg zur Erstellung eigener Farben.

Vorschauen zeigen nur die drei Flächenfarben. Globaler Hellmodus gilt ausschließlich für Einstellungen und berechnet die [leicht getönten hellen Werte](settings-appearance.md). Die eigentliche Palette und profilbezogene Themeauswahl bleiben erhalten.

## Zuordnung alter Themes

| Bisherige ID | Neues Theme |
| --- | --- |
| preset:bicycle-red | HELLFIRE |
| preset:black-ops / preset:embers-inv | HELLFIRE DARK |
| preset:solar-flare | SUNSTRUCK DARK |
| preset:electric-blue | BLUE RIOT DARK |
| preset:acid-lime | ACID POP DARK |
| preset:ultraviolet | PURPLE HAZE |
| preset:petrol-attack | DEEP DIVE |

Die Zuordnung erfolgt beim Lesen von Profilen und Sicherungen. Neue IDs haben eindeutige Suffixe, damit das frühere FIRE (gespeichert als solar-flare) nicht mit dem neuen orangefarbenen SOLAR FLARE verwechselt wird. Sonstige Profilwerte und Sensorzuordnungen bleiben erhalten.
