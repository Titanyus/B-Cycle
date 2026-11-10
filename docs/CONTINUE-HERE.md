# Aktueller Stand: APK0.8.109 / Build154

Aufträge implementiert, gezielt geprüft und angeforderte APK veröffentlicht. Keine offenen Implementierungsaufträge. Veröffentlichung und tatsächlichen Raw-Direktdownload abgeschlossen; bytegleich zum Actions-Artefakt.

[APK](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.109-release-154.apk) · [Buildnachweis](build-154.md) · [Änderungen](release-0.8.109.md)

Actions42906137477 erfolgreich aus Quellstand9b02e52a16e47ce37473f412d366cd053f0bb75c, VersionCode179, Android13+. 4462369Bytes; SHA-256e4cfde6314147a397472b44c1d0aa9f368396d3953282fde1006b06420b79731. Zertifikat SHA-25694d34c46ede28d12e6d37efd8a7d953a75b621596ec2e5b68f61899b8ea7499b. Artefakt und tatsächlichen Raw-Direktdownload bytegleich geprüft; Veröffentlichung236df47.

## Letzte Änderung

Commit0498b3a: Bright als normal gespeicherten Stil migrieren, anschließend keine virtuelle Wiederergänzung. Globaler Migrationsmarker, Sammlung nach Restore maßgeblich; leere Sammlung und unabhängige Arbeitskopie unterstützt. Allgemeine Beschriftung enthält Orte/Länder/sonstige Orte/Straßen/Wasser/Hausnummern mit Größe/Gewicht im vorhandenen placeLabels-JSON. Native Prüfung einer echten MVT-Kachel bestätigt32 Hausnummern; Anzeigegrenze17→14, Ebene standard aus.14 gezielte Methoden grün (MapLibraryTextRevisionTest4/MapStyleLibraryTest5/MapPlaceTextEditorTest1/MapControlAccessTest4), map-library-text-complete-tests.log; [Bericht/weitere Logs](map-bright-typography-house-numbers.md). Zentrale Prüfungen grün. Keine Sensor-/Aufzeichnungs-/GL-Lebenszyklusänderung.

Nutzer fragte nach Übernahme eigener Stile in APK: pro Stil JSON-Datei hochladen, Namen/Reihenfolge/Standard angeben. Bisher keine solchen Dateien übermittelt, keine eigenen Vorgabestile eingebettet.

## Bisheriger Stand und Grenzen

[Build153](build-153.md): Quellenpopup/2s-Entwicklerfreigabe, Straßen-/Hauskonturen, Stil-Löschen/Sortieren. [Build152](build-152.md): Natur-/Stadtflächen und Ortskonturen. [Build151](build-151.md): sichtbare Save-Aktionen. [Build150](build-150.md): Stile, Dashboard-Effizienz, Trainingsvarianten, gemischte Graphen. Frühere Prüfgrenzen in Fachberichten; keine grüne Gesamtsuite behaupten. Hausnummern-Gerätebild/Dichte weiter kontrollieren. Bosch-Ladeadressen am Rad bestätigen, unbekannt→Anroll-Fallback. Alte Dubletten unverändert.

APK nur auf Auftrag ausschließlich Actions, kein lokaler APK-Build/zweiter Dispatch neben Push. Veröffentlichung/Downloadnachweis mit[skip ci].
