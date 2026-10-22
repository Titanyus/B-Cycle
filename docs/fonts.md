# Zentrale Schriftrollen

Stand 22.10.2026. Alle Schriftfamilien stehen in `BicycleDesign.kt`; Screens wählen ausschließlich eine benannte Rolle. Keine direkten Ressourcenladungen, Typeface.create-Aufrufe oder lokalen DEFAULT-Familien außerhalb dieser zentralen Datei.

| Rolle | Familie | Verwendung |
| --- | --- | --- |
| `narrowTypeface(context)` | **League Gothic** | Bisherige schmale Messwerte, Einheitenrollen, Fenstertitel, Theme-Namen und alle Texteingaben |
| `bodyTypeface` | Android-Systemschrift, normal | Beschriftungen, Fließtext, normale Aktionen |
| `compactTypeface` | Android sans-serif-condensed | Vorhandene kompakte Dashboard-Beschriftungen und HRV-Einheiten/Achsen |
| `strongTypeface` | Android sans-serif, Gewicht 900 | Bestehende starke Überschriften und hervorgehobene Aktionen |

Schriftgrößen bleiben nach fachlicher Rolle in BicycleDesign/DashboardTileStyle definiert. Die früheren lokalen Rücksetzungen bei Tagesbefinden, eigenen HRV-Fragen und Fahrtbenennung entfallen; diese Eingaben verwenden nun ebenfalls die gemeinsame schmale Eingaberolle mit settingsMetricSp (21 sp). InputType-Konfiguration kann die native Schrift zurücksetzen: Eingaben setzen die zentrale Rolle deshalb beim Anfügen erneut. Passwortmaskierung, Tastaturtyp, Validierung und gespeicherte Texte bleiben erhalten.

**Nur Fenstertitel im Hauptheader erzwingen Großbuchstaben für die Anzeige.** Die Originaltexte/Namen bleiben unverändert. Andere Rollen erzwingen keine Großschreibung; bestehende Eigennamen und Abkürzungen bleiben in ihrer tatsächlichen Schreibweise. Auch die Breitenberechnung der Titellaufschrift verwendet den angezeigten transformierten Titel, einschließlich Großschreibung mit geänderter Länge.

## Schriftquelle und Lizenz

League Gothic Version **2.001**, variable Breitenachse wdth mit Standard 100. Vollständige unveränderte Datei (65.972 Bytes) unter `app/src/main/res/font/league_gothic.ttf`. Quelle: [Google Fonts / League Gothic](https://github.com/google/fonts/tree/main/ofl/leaguegothic), Datei `LeagueGothic[wdth].ttf`; Projekt [theleagueof/league-gothic](https://github.com/theleagueof/league-gothic). Copyright 2010 The League Gothic Project Authors. **SIL Open Font License 1.1**; voller Text unter `app/src/main/assets/League-Gothic-OFL.txt` und im App-Lizenzdialog.

SHA-256 der eingebundenen Datei: `3b0e998c9a0034222394ffecdd383e6948259ad037b95555b139a217629ce1d0`. Echte Groß-/Kleinbuchstaben und ä/ö/ü/ß geprüft. Keine neue Android-Bibliothek oder externe Laufzeitabhängigkeit.

Bebas Neue wurde aus den App-Ressourcen entfernt. Ihr bisheriges unverändertes Original und ihre OFL-Lizenz liegen ausschließlich unter docs/previews/fonts als historischer HTML-Vergleich. Die zusätzliche Roboto-Condensed-Vergleichsschrift ist ebenfalls nur ein lizenzierter HTML-Bestandteil, kein Bestandteil der APK. Das alleinstehende HTML enthält Fonts und Lizenztexte bereits eingebettet.

## Absicherung

`python3 scripts/check-ui-typography.py` meldet neue lokale Schriftfamilien-Definitionen. Die manuelle begrenzte Regression-CI führt diesen Check zusammen mit dem Aktualitätscheck des HTML-Katalogs aus. Android-Tests prüfen zentrale Rollen, echte Kleinbuchstaben/Umlaute, Erhalt der Eingabedaten/Passwortmaske, Anfügen und erneutes Stylen sowie Header-/Scroller-Verhalten.
