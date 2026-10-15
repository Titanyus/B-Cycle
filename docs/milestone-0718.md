# B-Cycle 0.7.18 – Test-APK

Stand: 15.10.2026. Basis: `1002721` mit den Paletteänderungen dieses Releases.

- Version 0.7.18 / versionCode 56; Paket `de.bcycle.app`.
- Android 13+, minSdk 33, Compile-/Target-SDK 36; native Views, Hochformat.
- Optimierte Release-APK: `downloads/B-Cycle-0.7.18-test.apk`, 1173458 Byte.
- SHA-256: `c350f61a040a817cc5b29e536579a0286c2d9ea626e11a283e6283557f34df24`.
- Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a` (identisch mit 0.7.17).

## Änderungen

Farbrollen in Reihenfolge PRIMARY/SECONDARY/TERTIARY/FOREGROUND/ACCENT:

- EMBERS: #BF281B / #000000 / #292929 / #FFFFFF / #BF281B.
- FIRE: #FFB400 / #000000 / #292929 / #FFFFFF / #FFB400.

Die bestätigte Kontrastausnahme von FIRE folgt dem gelben Hintergrund von
TERTIARY nach PRIMARY. Keine namensabhängige Layoutlogik; alle anderen
Kontrastprüfungen bleiben erhalten. Interne IDs `preset:black-ops` und
`preset:solar-flare` erhalten bestehende Auswahlen.

## Validierung

14 gezielte Tests für Palettenwerte, Kontrastprüfung, Theme-Manager,
Auswahl und Neustart bestanden; 0 Fehler, 0 Fehlschläge, 0 übersprungen.
Keine neue vollständige Suite. Release-Build einschließlich R8,
Resource-Shrinking und Lint-Vital erfolgreich. Test-/Build-Aufruf zusammen
49.8 Sekunden. Danach APK signiert und Signatur, Zertifikatvergleich,
ZIP-Ausrichtung (16 KiB), Paket-, Versions- und SDK-Metadaten erfolgreich geprüft.

Installation und echte Fahrt hier nicht praktisch getestet. Keine privaten
FIT-Dateien, Schlüssel oder Zugangsdaten veröffentlicht.
