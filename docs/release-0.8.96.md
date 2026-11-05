# B|CYCLE 0.8.96

- Straßen und Wege mit ausdrücklich unbefestigtem Belag gestrichelt; befestigte, fehlende und unbekannte Belagsangaben durchgezogen. Die Wegeart allein entscheidet nicht über den Belag.
- Farbige Radwegehervorhebung, Grundlinie und Umrandung verwenden bei unbefestigtem Belag dasselbe deutliche Strichmuster. Farben, Transparenz und Linienbreiten bleiben erhalten. Separat geladene Radrouten ohne Belagsdaten bleiben durchgezogen.
- Im Designer ersetzt ein kompakter Belagshinweis die freie Musterwahl für Straßen und Wege. Andere Objektmuster bleiben erhalten. JSON dokumentiert die feste Regel als roadSurfaceStyle=unpaved-dashed; ältere Exporte bleiben gültig.

Sechs gezielte Fälle erfolgreich: mehrere Straßen-/Wegeklassen und normale/Tunnel-/Brückenlagen, bekannte/fehlende Beläge, native Dash-/Solid-Varianten, Radweg-Overlay, Live-Farben/Reset, Breiten, Editor, Profil/Backup/Export und JSON. Typografie/UI-Katalog/Bright-Generator/Diff grün. Kein Gesamtlauf und keine lokale APK; optische Geräteprüfung offen. [Umsetzung und Prüfung](map-road-surfaces.md).

Buildnachweis folgt nach erfolgreichem GitHub-Actions-Lauf.
