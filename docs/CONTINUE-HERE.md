# Aktueller Stand: APK0.8.108 / Build153

Aufträge implementiert, gezielt geprüft und angeforderte APK veröffentlicht. Keine offenen Implementierungsaufträge. Tatsächlichen Direktdownload nach Veröffentlichung noch abgleichen.

[APK](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.108-release-153.apk) · [Buildnachweis](build-153.md) · [Änderungen](release-0.8.108.md)

Actions42897947067 erfolgreich aus Quellstand8934ccbf6838d1fb713fb4f9c4bd49e07e1f1c34, VersionCode178, Android13+. 4462157Bytes; SHA-2567b43a80f3a7f7e6d362fdb310cfdfc3d0dbfcb51d818c703c41aa33c44a9d1ea. Zertifikat SHA-25694d34c46ede28d12e6d37efd8a7d953a75b621596ec2e5b68f61899b8ea7499b. Artefakt geprüft; direkter Raw-Downloadnachweis folgt.

## Letzte Änderung

Commit4eacd70: ROAD_TEXT/HOUSE_TEXT mit Füll-/Kontur-RGBA/Breite; Hausnummern echte OpenMapTiles-Features ab VTM17, default aus und niedrige Priorität. MapSourcePopup neben Auge mit gemeinsamer Quellen-/Stilwahl und Cycling-Schaltern.2s-Hold schaltet Layers/Editor frei; Abbruch/Pause/Settings cancelt Timer, showMenu versteckt Buttons. Ebenen-Accordion aus Einstellungen entfernt. Stile im Save-Dialog ↑/↓/Löschen, UUIDs/Arbeitskopien erhalten, gleiche Reihenfolge in Quellenauswahl/Sicherung. Dialog scrollbar. Neun Fälle grün (MapControlAccessTest4,MapStyleLibraryTest5), Log map-access-complete-tests.log. [Details/weitere Logs](map-controls-and-label-editor.md). Zentrale Typografie/UI-Katalog/Bright/Diff grün. Keine Sensor-/Aufzeichnungs-/GL-Lebenszyklusänderung.

## Bisheriger Stand und Grenzen

[Build152](build-152.md): getrennte Natur-/Stadtflächen und Ortskonturen. [Build151](build-151.md): sichtbare Save-Aktionen und Bright-Überschreiben. [Build150](build-150.md): interne Stile, Dashboard-Effizienz, Trainingsvarianten und gemischte Graphen. Frühere Testannahmen/Gerätegrenzen in den Fachberichten; keine grüne Gesamtsuite behaupten. Bosch-Ladeadressen weiter am Rad bestätigen, unbekannt→Anroll-Fallback. Alte Dubletten unverändert. Hausnummerndichte abhängig von den Vektorkacheln; optische/Gestenkontrolle am Gerät offen.

APK ausschließlich Actions und auf Auftrag, kein lokaler APK-Build/zweiter Dispatch neben Push. Veröffentlichung/Downloadnachweis mit[skip ci].
