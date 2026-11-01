# Meilenstein B|CYCLE0.8.78 · Build119

Auf Nutzerwunsch am01.11.2026 gesichert: vollständiger Projektstand einschließlich Quellcode, Tests, Ressourcen, Buildkonfiguration, Dokumentation, geprüfter signierter APK und R8-Zuordnung für spätere Fehleranalyse.

- Git-Tag: `v0.8.78`.
- [Dauerhaftes GitHub-Release](https://github.com/Titanyus/B-Cycle/releases/tag/v0.8.78).
- [Direkter APK-Download](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.78-release-119.apk).
- [Änderungen](release-0.8.78.md) · [Buildnachweis und Prüfsumme](build-119.md).
- Tatsächlich gebauter App-Quellstand: `521e012a3e37ed9cf1dd85fb8e9c856b8ef57bb9`.
- APK0.8.78, versionCode148, Build119, Android13+.
- APK-SHA256: `998fa47f4765b1a0079ff78160c9c99d3ec4ec70cf7a52fc09fece9592103977`.
- Dauerhaftes Signierzertifikat SHA256: `94d34c46ede28d12e6d37efd8a7d953a75b621596ec2e5b68f61899b8ea7499b`.

Dauerhaft versionierte Dateien unter `downloads/`: unveränderte geprüfte APK, ihre SHA256-Datei und komprimiertes R8-Mapping `B-Cycle-0.8.78-build-119-mapping.txt.gz` einschließlich eigener Prüfsumme. Das GitHub-Release verlinkt die Dateien auf unveränderliche Git-Revisionen. Direkte Release-Asset-Uploads wurden vom GitHub-Upload-Endpunkt mit HTTP401 abgewiesen; deshalb Dateien im Repository gesichert. Mapping-SHA256: `f4b39a45d4ead078011e816a962d40458af5ab21a6cb88ec6474b510949f7a02`. Das R8-Mapping wurde nach dem Quelltag in einem separaten Sicherungscommit ergänzt; der Release-Link fixiert genau diesen Commit. Der Meilenstein-Tag umfasst zusätzlich die abschließenden Veröffentlichungs-/Prüfnachweise und die unten genannten Planungsentscheidungen; gegenüber dem gebauten Quellstand keine weiteren Appcodeänderungen.

Enthalten: HRV-Darstellung und schwebende Zusatzkennzahlen, aufgeräumte HRV-/Fahrradeinstellungen, Sensoranzeige nach Zuordnung/Suchfund sowie ruhende Aufnahmeanzeige zwischen den Übergängen. Gezielte Prüfungen und tatsächlicher Raw-Download erfolgreich; kein physischer Gerätetest.

[Dashboard-Energieanalyse und spätere Planung](dashboard-energy-analysis.md) ebenfalls gesichert. Noch kein VTM-Umbau und kein allgemeiner Akkuschonmodus. Sensorerfassung, vollständige Aufzeichnung und Analyse sollen bei späterer Akkuoptimierung unverändert zuverlässig bleiben. Trainingshilfe aktuell weich, harte Farbwechsel erst im späteren Sparmodus.
