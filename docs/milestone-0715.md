# B-Cycle 0.7.15 – Test-APK

Stand: 14.10.2026. Hauptbranch-Basis: `53bc8ac`.

- Version 0.7.15 / versionCode 53; Paket `de.bcycle.app`.
- Android 13+, minSdk 33, Compile-/Target-SDK 36. Native Views, Hochformat.
- Optimierte Release-APK: `downloads/B-Cycle-0.7.15-test.apk`, 1128402 Byte.
- SHA-256: `4db4384e8bf9812f5579b8539dcd11892df5356aec47ee084fb2032dfd54b755`.
- Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.
- Signatur mit 0.7.14 identisch; ZIP-Ausrichtung (16 KiB) und Paket-/SDK-Metadaten geprüft.
- Compose-Experiment unverändert; keine neue Bibliothek oder Benachrichtigungsberechtigung.

## Änderungen

Enthält die seit 0.7.14 vorgenommenen UI-Zentralisierungen: Formularspacing,
Scrollregeln, gemeinsame Footer mit kleinen Labels, drei Dashboard-Kachelstile,
65 % weißer Text statt deckendem Grau, Kartensteuerungsrollen, Ressourcentexte,
Accessibility-Hinweise, getrennte kleine Schriftrollen und BicycleFeedback.
Fachliche Maße der drei Kachelvarianten und Kartenpositionen bleiben erhalten.
Zentralisierung kleiner Schriftrollen bedeutet keine globale Größenangleichung.

Ein früherer Formularspacing-Effekt auf den frei positionierten Editorbutton wurde
bei der vollständigen Suite erkannt und behoben. Der Theme-Test prüft jetzt den
lokalisierbaren Menütitel. Neue Prüfungen sichern ressourcenbasierte Feldtitel,
stabile IDs und höfliche, nicht wiederholte Inline-Statusmeldungen.

## Validierung

Vollständige Suite: **386 Tests, 0 Fehler, 0 Fehlschläge, 0 übersprungen**,
abschließender Lauf 1m 37s. Release-Build: **57 Sekunden**, R8, Resource-Shrinking
und Lint-Vital erfolgreich. Bestehende Testwarnungen zu onBackPressed bleiben;
keine Warnungsunterdrückung oder entfernte Tests. Keine zusätzliche volle Lint-Prüfung.

Die APK ist mit dem bisherigen Testschlüssel als Update installierbar. Keine
privaten FIT-Dateien, Schlüssel, Zugangsdaten oder Build-Caches veröffentlicht.
Das Verhalten auf realen Geräten wurde hier nicht praktisch getestet.

[Benachrichtigungs-Vorbereitung](notifications-preparation.md) beschreibt den
anschließenden Funktionsschritt. In dieser APK gibt es noch keinen NotificationListener.
