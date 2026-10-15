# B-Cycle 0.7.19 – Test-APK

Stand: 15.10.2026. Basis: `be34edd` mit dem Sticky-Header-Versuch und Auswahlrahmen dieses Releases.

- Version 0.7.19 / versionCode 57; Paket `de.bcycle.app`.
- Android 13+, minSdk 33, Compile-/Target-SDK 36; native Views, Hochformat.
- Optimierte Release-APK: `downloads/B-Cycle-0.7.19-test.apk`, 1177554 Byte.
- SHA-256: `fb292cbe135b0287ac16bcf5d09bf0bd3be04a95b305a81caab4ae99d5034a31`.
- Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a` (identisch mit 0.7.18).

## Änderungen seit 0.7.18

Neue zentrale Kategoriekomponente, zunächst nur auf Bikes für Radtyp und Sensoren:
zentrierte bestehende Abschnittsschrift, TERTIARY, 1 physischer Pixel FOREGROUND
oben/unten und 12-dp-Innen-/Außenabstand. Nativer Sticky-Scrollhost verschiebt
dieselben Header; die nächste Kategorie verdrängt die alte. Viewport-Clipping
verhindert Überzeichnen der festen Titelleiste. FIT-Accordion bleibt separat.

ACCENT für aktive Auswahlrahmen (Bike/Theme/Dashboard und ausgewählte
Formularbuttons), aktive Kreismitten und Schalter. FIT-Auswahl wieder im
Accordion. Electric Blue und Acid Lime verwenden schwarze/graue Flächen und
ihre jeweilige Primärfarbe als ACCENT. Fünf Verlaufskurven mit deckender Füllung.
Keine neue Abhängigkeit, keine Änderung an Sensoren, FIT oder Berechnungen.

## Validierung

14 gezielte UI-Tests bestanden, 0 Fehler, 0 Fehlschläge, 0 übersprungen:
Sticky-Geometrie in zwei Hochformatbreiten, Verdrängen/Abschnittsende,
Rückwärtsscrollen, Drag über dem Header, native Renderprüfung der festen
Titelleiste, 1-Pixel-Linien, Themefarben, volle Breite, ACCENT-Auswahlrahmen,
Bike-/FIT-Bedienung, Themeauswahl und gemeinsame Controls. Zuvor wurden die
neuen Paletten separat über Theme-/Manager-/Auswahltests geprüft.

Kein neuer vollständiger Suite-Lauf. Release-Build mit R8, Resource-Shrinking
und Lint-Vital erfolgreich. Abschließender Test-/Build-Aufruf zusammen
46.2 Sekunden. Danach APK signiert; Signatur, Zertifikatvergleich,
ZIP-Ausrichtung (16 KiB), Paket-, Versions- und SDK-Metadaten erfolgreich geprüft.

Native Vorschauen mit synthetischen Bike-Daten:
[Seitenanfang](previews/bikes-sections.png) · [angedockt](previews/bikes-sticky.png).
Installation und reale Fahrt hier nicht getestet. Keine privaten FIT-Dateien,
Schlüssel oder Zugangsdaten veröffentlicht.
