# Aktueller Stand: APK0.8.117 / Build162

Angeforderte APK veröffentlicht. Keine offenen Implementierungsaufträge. Raw-Direktdownload noch prüfen.

[APK](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.117-release-162.apk) · [Buildnachweis](build-162.md) · [Änderungen](release-0.8.117.md)

Actions43462101944 erfolgreich aus Quellstandaeff55d35f7212e558cebe220304539f58bd9178, VersionCode187, Android13+. 4645728Bytes; SHA-2561caddedd03a41123a1d646d299a1451ea9e8e69ea5193c368ae350ddd28fb713. Dauerhafte Signatur bestätigt.

## Letzte Änderungen

- Ein einheitlicher Stil auf bisheriger Glow-Basis; Flat-Auswahl entfernt, alte Einstellungen bleiben lesbar.
- Dashboard-Zonenindikatoren als flache8%-Striche mit natürlichen Farbübergängen.
- 14 verschiedene PRIMARY-Farben mit identischem ACCENT und gemeinsamen dunklen Flächen; Dark-Varianten durch benannte Zwischentöne ersetzt.
- Bestätigte NP-Pausen ohne Leistungsdaten auch bei weiter eingeschaltetem/verbundenem E-Bike.
- Zusätzlichen Höhenkommentar in FIT-Dateien entfernt, der FITFileViewer zum Absturz bringen konnte.

Gezielte Tests erfolgreich: fourteen-primary-themes-final-tests.log, unified-style-np-fit-final-verified-tests.log und fit-memo-and-zone-strip-guard-tests.log. Alte Theme-/Stil-Auswahl und Backups, Zonenstriche/HSV-Endfarben, NP-Pausen mit unveränderter Fahrzeit sowie kommentarfreie Höhenkorrektur geprüft. Offline-Katalog mit14Themes ohne Stilumschalter im Browser geprüft. Typografie-/Atlas-/Bright-/Diffchecks grün. Keine Gesamtsuite oder Laufzeitmessung behauptet; kein lokaler APK-Build.

14Themes: sieben normale plus sieben HSV-Zwischentöne, alte Dark-IDs über Alias weiter lesbar. [Tabelle](theme-palettes.md). Kein Flat-UI-Umschalter mehr, alte Flat-Settings retired/bei Restore ignoriert, Dashboard-Zonenstriche unabhängig vom UI-Stil. [Details](unified-ui-style.md).

NP-Pausenerkennung wartet nicht mehr auf Rad-/BLE-Abschaltung; frische/ausreichend genaue10s-Stillstandsbelege und fehlende Leistung bleiben erforderlich,90%-Qualitätsgrenzen und Fahrzeit unverändert. Nutzer bestätigt Anfangspause und lässt Rad oft lange an. [Details](confirmed-np-pause.md).

Private Nutzer-FIT und Kommentarbereinigung: Garmin-Validierung ohne Fehler. FITFileViewer-Absturz lokal exakt über Originaldecoder reproduziert, verursacht durch alte Mapterhorn-memo_glob-Attribution. Künftig weglassen, nur eigene Altkommentare beim Korrigieren entfernen. Lokale bereinigte Kopie /workspace/shared/downloads/B-Cycle-bereinigt.fit, alle anderen Daten unverändert, keine NP nachgetragen; private Nachweise .tools/np-nov13. NIEMALS private FIT/Samples ins Repo. [Nachweis](fit-comment-compatibility.md).

Vorherige Trainingshilfe-Kombibox samt Feldauswahl und drei neuen WebPs aus Build161 erhalten. Zentrale Footer PRIMARY außer Dashboard TERTIARY, FIT-Kombibox und Trainings-Stärkesortierung/Variantenpille aus Build160 erhalten.

## Bisheriger Stand und Grenzen

Build154: Bright als normalen Stil einmalig migrieren, dauerhaftes Löschen ohne virtuelle Wiederergänzung; allgemeine Beschriftungsgrößen/-gewichte für alle Textarten, echte Hausnummern32 im Quellfixture bestätigt, Anzeigegrenze14 statt17. Ebene standardmäßig aus. [Details](map-bright-typography-house-numbers.md). Nutzer kann eigene Stile als JSON zur Einbettung übergeben; bisher keine übermittelt.

Bisherige Gerätegrenzen weiterhin: Hausnummernanzeige/Dichte, Bosch-Ladeerkennung am Rad, alte Dubletten unverändert. Keine Sensor-/Aufzeichnungs-/GL-Lebenszyklusänderung durch diese UI-Aufträge. APK nur auf Auftrag über Actions, kein lokaler APK-Build/zusätzlicher Dispatch neben Push. Veröffentlichung/Downloadnachweis mit[skip ci].
