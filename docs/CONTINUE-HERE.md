# Aktueller Stand: APK0.8.107 / Build152

Auftrag umgesetzt, geprüft und ausdrücklich angeforderte APK veröffentlicht. Keine offenen Implementierungsaufträge. Tatsächlichen Direktdownload nach Veröffentlichung noch abgleichen.

[APK](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.107-release-152.apk) · [Buildnachweis](build-152.md) · [Änderungen](release-0.8.107.md)

Actions42881124412 erfolgreich aus Quellstande7036df0228cc54cb8ac83a7e194abf34df11c98, VersionCode177, Android13+. 4445901Bytes; SHA-2563d7e0a5b2a8f235b609c95f05e4e2e0161b89323e716a9bad23a2e86332921f6. Zertifikat SHA-25694d34c46ede28d12e6d37efd8a7d953a75b621596ec2e5b68f61899b8ea7499b. Artefakt geprüft; direkter Raw-Downloadnachweis folgt.

## Letzte Änderung

Commit9798b86: URBAN trennt Siedlungs-landuse von NATURE (Wald/Wiesen/Parks/landwirtschaftliche Flächen). PLACE_TEXT bekommt bestehenden outline-Block mit RGB/Alpha/Breite; Editor rechts RGBA und25–300% Konturstärke, links Schriftfüllung. Native Stroke-Paints und Atlasaufbereitung bei Konturänderungen neu laden; bisheriger Standard ohne Override erhalten. JSON/benannte Stile/Profil-/Backup-Enumerationen erweitert. Drei native/UI-/Persistenzfälle grün, map-place-outline-tests.log. Zentrale Prüfungen grün. [Details](vtm-map-editor.md).

## Bisheriger Stand und Grenzen

[Build151](build-151.md): sichtbare Stil-Save-Aktionen und überschreibbares Bright ohne doppelte Kartenquellenzeile. [Build150](build-150.md): interne Stilsammlung, Dashboard-Effizienz, Trainingsvarianten und gemischte Graphen. Früher dokumentierte Testannahmen und Gerätegrenzen in den Fachberichten; keine grüne Gesamtsuite behaupten. Bosch-Ladeadressen weiter am Nutzerrad zu bestätigen, unbekannt→Anroll-Fallback. Alte Fahrtdubletten unverändert.

Keine Sensor-/Rohdaten-/Aufzeichnungsänderung, kein Profiling. Optische Kontrolle am Gerät offen. APK nur auf Auftrag über Actions, kein lokaler APK-Build oder zweiter Dispatch neben Push. Veröffentlichung/Downloadnachweis mit[skip ci].
