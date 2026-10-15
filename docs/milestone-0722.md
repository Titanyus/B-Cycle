# B-Cycle 0.7.22 – Test-APK

Version 0.7.22 / versionCode 60; Android 13+, Compile-/Target-SDK 36.
Build-Zeit: 15.10.2026 15:54:01 CEST. Basis 8112652 mit Einstellungsbild und Versionsanhebung.

## Änderungen

- Originales Nutzermotiv unter den sieben Haupt-Einstellungszeilen, vollständig
  und ohne Farbveränderung. Scrollt mit; Untermenüs bleiben unverändert.
- Version sowie genauer Build-Zeitstempel im unteren Bildbereich zentriert.
  Datum und Uhrzeit einschließlich Zeitzonenoffset werden in der Gerätezeitzone dargestellt.
- Korrigierte Höhenmeterzählung und optionale nachträgliche FIT-Höhenkorrektur
  mit Mapterhorn unter Karten. Original bleibt erhalten, korrigierte aktive Datei
  wird für Analyse, Speichern und Upload verwendet.
- Accordion-Typografie wie Dropdowns, größere Farbpunkte, dezente Header-Schatten.
- OpenTopoMap und OpenCycleMap mit eigenem Thunderforest-Schlüssel.

## Prüfung

Vier native Einstellungs-Tests bestanden; zwei Hochformatbreiten, Bildproportionen,
Position/Farbe der Build-Angabe, Navigation und unveränderte Untermenüs geprüft.
Gerenderte Vorschau: [Einstellungen](previews/settings-brand.png).
Für die vorausgehende Höhenkorrektur und deren Integration bestanden 47 gezielte Tests,
für die Höhenzählung zuvor 39 Tests; keine erneute vollständige Suite für das Bild.

Optimierter Release-Build mit R8, Resource-Shrinking und Lint-Vital erfolgreich
in 52 Sekunden. Signatur, gleiches Zertifikat wie bisher, ZIP-Ausrichtung
(16 KiB), Version und SDK-Metadaten geprüft. Kein Hardwaretest.
Der echte Mapterhorn-Abruf bleibt wegen der Domain-Netzwerksperre in dieser
Entwicklungsumgebung ungeprüft; Details in [Höhenkorrektur](elevation-gain.md).

APK: `downloads/B-Cycle-0.7.22-test.apk`, 2062346 Byte.
SHA-256: `ae6aa62feb2d98cda92afb4872a638fd97a6b3f11c467171b621f5da22b68add`.
Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.
Mit bisherigem Testschlüssel als Update installierbar.
