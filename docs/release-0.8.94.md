# B|CYCLE 0.8.94

- Linienbreite im Karteneditor von 25–300 % des jeweiligen Basisstils einstellbar, einschließlich Radwegehervorhebung. Zoomabhängige Breiten und Füll-/Randproportionen bleiben erhalten. Profil, Sicherung und JSON-Export berücksichtigen den Wert.
- Feld- und Wirtschaftswege separat von kleinen Straßen editierbar: eigene Farbe, Alpha, Linienmuster und Breite. Standardmäßig halb so breit, braun gestrichelt, ohne breite Straßenumrandung. Die Wegeart belegt keinen bestimmten Belag.
- Radwegehervorhebung bis zur kleinsten Kartenzoomstufe freigegeben; beim Herauszoomen schrittweise dünner. Die Darstellung hängt weiterhin von den vorhandenen Quelldaten ab.
- Größere Orte (town/city, darunter Günzburg und Krumbach) zunächst Black/900 und 10 % kleiner. Im Designer ein zunächst geschlossenes Dropdown „Ortsbeschriftung“: city/town/village/hamlet getrennt auf Gewicht100–900 und Größe50–200 % einstellen. Kleine Orte behalten ohne Änderung ihre bisherigen Werte; Prioritäten und Einblendgrenzen erhalten. Werte im Profil, in der Sicherung und im JSON-Export enthalten.

Drei gezielte Native-/Editor-/Export-/Profil-/Sicherungsfälle erfolgreich, Schriftanpassung gezielt kompiliert, zusätzlich ein umfassender gezielter UI-/Profil-/Sicherungs-/JSON-/Native-Paint-Test für alle vier Schriftklassen erfolgreich. Typografie/UI-Katalog/Bright-Generator/Diff geprüft. Kein Gesamtlauf und keine lokale APK. Optische Geräteprüfung offen. [Editor](vtm-map-editor.md), [Ortsnamen](vtm-label-stability.md).

[Buildnachweis und Download](build-137.md).
