# B|CYCLE 0.8.94

- Linienbreite im Karteneditor von 25–300 % des jeweiligen Basisstils einstellbar, einschließlich Radwegehervorhebung. Zoomabhängige Breiten und Füll-/Randproportionen bleiben erhalten. Profil, Sicherung und JSON-Export berücksichtigen den Wert.
- Feld- und Wirtschaftswege separat von kleinen Straßen editierbar: eigene Farbe, Alpha, Linienmuster und Breite. Standardmäßig halb so breit, braun gestrichelt, ohne breite Straßenumrandung. Die Wegeart belegt keinen bestimmten Belag.
- Radwegehervorhebung bis zur kleinsten Kartenzoomstufe freigegeben; beim Herauszoomen schrittweise dünner. Die Darstellung hängt weiterhin von den vorhandenen Quelldaten ab.
- Größere Orte (town/city, darunter Günzburg und Krumbach) in Black/900 und 10 % kleiner. Dörfer, Weiler und Ortsteile unverändert; Prioritäten und Einblendgrenzen erhalten.

Drei gezielte Native-/Editor-/Export-/Profil-/Sicherungsfälle erfolgreich, Schriftanpassung gezielt kompiliert. Typografie/UI-Katalog/Bright-Generator/Diff geprüft. Kein Gesamtlauf und keine lokale APK. Optische Geräteprüfung offen. [Editor](vtm-map-editor.md), [Ortsnamen](vtm-label-stability.md).

Buildnachweis folgt nach erfolgreichem GitHub-Actions-Lauf.
