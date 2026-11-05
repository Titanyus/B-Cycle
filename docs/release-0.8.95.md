# B|CYCLE 0.8.95

- Eigene Karten-Einstellungen „Radwege“ und „Radrouten“: unabhängige Schalter, Farbe/HEX, Breite und Deckkraft wie bei den User-Tracks. Profil, Sicherung, Quick-Ebenenschalter und JSON-Export berücksichtigen beide Darstellungen.
- Optionaler Radrouten-Vektorlayer von Waymarked Trails für die Übersicht, zunächst aus. Größenabhängige Linienbreiten, begrenzter Cache und ein Lade-Worker; unter Beschriftungen und eigenen Tracks/GPS. Einzelne physische Radwege bleiben von den vorhandenen OpenFreeMap-Detaildaten abhängig. In sehr weiten Ansichten priorisiert der Routenabruf größere Netze (maximal100 übergeordnete Routen pro Kachel).
- Eindeutige Designer-Einträge „Feld- und Wirtschaftswege“ und „Fußwege und Radwege“ statt irreführender Sammelbezeichnung. Nativer Feldweg-Farbwechsel einschließlich Strichlinienfarbe geprüft.
- Lange mehrteilige Ortsnamen zweizeilig: etwa „Kirchheim / unter Teck“ oder „Dillingen / an der Donau“. Einzelwörter bleiben zusammen, kurze Namen unverändert. Gemeinsame Atlasfläche und Kollisionsmaße für beide Zeilen, gemeinsames Ein-/Ausblenden.

Gezielte Prüfungen erfolgreich: drei Ortsnamen-/Atlas-/Schilder-/Exportfälle; vier Cycling-/Native-/Profil-/Backup-/JSON-/Cache-/Farb-/Trackfälle; ein echter Karten-Einstellungs-/Raster-/Vektor-Integrationstest. Öffentliche Übersichts-/Detaildatenquelle geprüft. Typografie/UI-Katalog/Bright-Generator/Diff grün, kein Gesamtlauf und keine lokale APK. Optische Geräteprüfung offen. [Kartenoptionen](cycling-map-options.md), [Ortsnamen](vtm-label-stability.md).

[Buildnachweis und Download](build-138.md).
