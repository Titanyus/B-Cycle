# B|CYCLE0.8.78 – Build119

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.78-release-119.apk)
- [SHA-256-Datei](../downloads/B-Cycle-0.8.78-release-119.apk.sha256)
- [GitHub-Actions-Lauf](https://github.com/Titanyus/B-Cycle/actions/runs/41341772588)
- Quellstand: `521e012a3e37ed9cf1dd85fb8e9c856b8ef57bb9`
- Version0.8.78, versionCode148, Android13+.
- Größe: 2592945Bytes (2,59MB).
- SHA-256: `998fa47f4765b1a0079ff78160c9c99d3ec4ec70cf7a52fc09fece9592103977`.
- Signierzertifikat SHA-256: `94d34c46ede28d12e6d37efd8a7d953a75b621596ec2e5b68f61899b8ea7499b`.

Optimierter Release aus GitHub Actions. CI-Prüfsumme, Version, Manifest ohne Debug-/Test-Flags, ZIP-Ausrichtung und dauerhafte Signatur geprüft. Update-Zertifikat wie Build118. Tatsächlichen direkten Raw-Download erneut geladen und binär identisch mit dem geprüften Actions-Artefakt bestätigt.

[Änderungen](release-0.8.78.md): HRV-Kacheln/Dropdown, Empfehlung und Messdiagramm; HRV-Einstellungen; Sensorübersicht und entfernter GPX-Einstellungsschalter; ruhende Aufnahmeanimation zwischen unveränderten Symbolzuständen. Kein VTM-Umbau, kein allgemeiner Akkuschonmodus. Trainingshilfe bleibt ausdrücklich weich überblendet.

[CPU-/GPU-Abschätzung und festgehaltene Sparmodus-Vorschläge](dashboard-energy-analysis.md), keine Verbrauchsmessung.

Gezielte Prüfungen: fünf HRV-Fälle, ein Menüfall, ein Sensorlebenszyklusfall, Animationskurve und Indoor-/Outdoor-Lebenszyklus sowie Timer-/Ruhephasen-/Abbruchfall. Nur fehlgeschlagene/betroffene Fälle wiederholt. Typografie/Atlas/Diff geprüft. Kein Gesamtlauf oder physischer Gerätetest. Animations-Zeitablauftest mit explizit gesteuerten Android-Frames; unveränderte Ruhephasen ohne Redraws bestätigt.
