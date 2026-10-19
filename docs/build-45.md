# B|CYCLE 0.8.8 – optimierte APK

Build 45 vom 19.10.2026, versionCode 78.

**[APK herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/v0.8.8/downloads/B-Cycle-0.8.8.apk)**

## Änderungen

- Bis zu drei gestapelte Trend-Chevrons für Tempo, Kadenz, Leistung und Puls. Zeitgewichteter aktueller 3-Sekunden-Wert gegen bestehenden Fahrtdurchschnitt; kleine Ruhebereiche und Hysterese verhindern Flackern. Darstellung mit 500-ms-Überblendung. Keine Änderung der aufgezeichneten Messwerte oder der Mittelwertberechnung.
- Stop-/Aufnahmepunkt-Wechsel während Aufnahme bei allen Radprofilen, mit unverändertem Zwei-Sekunden-Wechsel und STOP-Aktion.
- Zeiteinheit auf dem Dashboard einheitlich „h“; Zeitwerte behalten h:mm:ss und dynamische Schriftanpassung bei einer/zwei Kacheln nebeneinander.
- Filter-/Sortierinhalte mit 8 dp Innenabstand oben/unten. Fenster und Gruppenbuttons TERTIARY, ausgeklappte Gruppen als abgerundete SECONDARY-Flächen. Normale Buttons und Texteingaben im Inhalt TERTIARY. Vorhandene Auswahlzustände und unmittelbare Filterung erhalten.
- Trinkerinnerung lediglich vorgemerkt, nicht implementiert.
- [Konventionen und Grenzen](trend-strength.md).

## Prüfung

31 gezielte Tests bestanden: Grenzwerte und Hysterese, Glättung/fehlende Werte, aktuelle und durchschnittliche Felder, Kartenspans, getrennte Chevron-Glyphen und Animation, Indoor-/E-Bike-Aufnahmeindikator und STOP-Aktion, Dashboardlayouts sowie Filter-/Sortierbedienung, Abstände und Flächenfarben. Kein vollständiger Testlauf und kein physischer Gerätetest.

[GitHub Actions erfolgreich](https://github.com/Titanyus/B-Cycle/actions/runs/39209866256), Quellstand 980d65fad535b14deae875a803c1c1ae98a72379. Release mit R8 und Ressourcenverkleinerung; kein lokaler APK-Build. Artefakt-Prüfsumme geprüft, mit bisherigem Testschlüssel nachsigniert; Zertifikat, Version 0.8.8/78, nicht-debuggable Manifest und 16-KiB-Zip-Ausrichtung geprüft.

- Größe: 1,862,820 Bytes.
- SHA-256: `995037efa5f8fc93e0424b521576afee77b479b8aad3b8729814faf291c43a05`.
- Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.
- [Prüfsumme](../downloads/B-Cycle-0.8.8.apk.sha256) · [R8-Mapping](../downloads/B-Cycle-0.8.8-mapping.txt.gz).
