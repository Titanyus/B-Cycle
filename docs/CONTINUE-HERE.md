# Weitere Gruppierungen nach Build158 (noch unveröffentlicht)

BicycleGroupedBox für Sensorliste (kleine Überschrift Sensoren, bestehender Sticky-Sensorabschnitt/Autostart/Suche erhalten), Radwege/Radrouten (Überschrift Radwege) sowie Backup-Gruppen Sichern3/Wiederherstellen3/Importieren2/Cloud5. Originale Frames, Schalter, IDs, Callbacks/Installationen erhalten, Einzelrahmen entfernt. Dynamische Sensortrenner nur zwischen sichtbaren Einträgen; leere Sensorbox samt kleiner Überschrift verborgen. Bereits vorhandene Glow16dp-/Standalone8dp-Korrektur ebenfalls noch unveröffentlicht. Keine neue APK beauftragt.

Prüfung: grouped-settings-expansion-final-tests.log. Zusätzlicher breiterer Lauf grouped-settings-expansion-tests.log meldete3 Fehler (alter bike_fit_settings-Tag fehlt, Vergleich aller Prefs nach Backup-Aufklappen, fehlende contentDescription des HRV4-Importbuttons); keine grüne Gesamtsuite behaupten. [Details](settings-boxes.md).

# Noch unveröffentlicht nach Build158

Glow-Sticky-Inhalt seitlich16dp vom Außenkasten statt8dp (Bildschirm24dp=8außen+16innen), Flat4+16 unverändert. BicycleGroupedBox als eigenständiges Formularfeld verwendet normalen controlGap8dp zur Überschrift/vorherigem Inhalt; Menüspalten unverändert. Trackdarstellung nutzt diesen standalone-Modus, beseitigt fehlenden Abstand unter Überschrift. Keine neue APK beauftragt.

# Aktueller Stand: APK0.8.113 / Build158

Angeforderte APK veröffentlicht. Keine offenen Implementierungsaufträge. Tatsächlicher Raw-Direktdownload bytegleich zum Actions-Artefakt geprüft; Veröffentlichung97aad187.

[APK](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.113-release-158.apk) · [Buildnachweis](build-158.md) · [Änderungen](release-0.8.113.md)

Actions43281768608 aus Quellstand2e56c705f2c883727d5bd8396b85d5bb87bf7ebd, VersionCode183, Android13+. 4463845Bytes; SHA-256d3130f442cd44916fba2fc3640fdd2542a9776a73f587ed3402b70906ce65d98. Dauerhafte Signatur bestätigt.

## Letzte Änderungen

- Menülabels „Strecken“ und „Nachrichten“; Menüschrift und Icons wie Rahmen-Dropdowns.
- Flat:4dp seitlicher Außenabstand und16dp Innenabstand, Glow unverändert.
- Vier Track-Dropdowns als gemeinsamer gerahmter Kasten, alle Schalter und Einstellungen erhalten.
- Globale Kastenart BicycleGroupedBox mit8dp Zwischenraum:3dp Abstand,2dp Trennlinie,3dp Abstand.

Gezielte Menü-/Navigations-/Trackmethoden erfolgreich, settings-grouped-track-final-tests.log und grouped-box-spacing-tests.log. Regression prüft globale page-Normalisierung ohne Löschen des ersten Kastenpaddings, vertikale Zentrierung sowie kurzen HRV-Kasten. Flat/Glow bei288/360/480dp. Zentrale Prüfungen grün. BicycleGroupedBox wird von Menüspalten und Trackgruppe gemeinsam genutzt. Radwege/Radrouten bleiben separat. Ein zusätzlich gestarteter CyclingOptionsTest scheiterte vor UI-Prüfung an nativer Zoominitialisierung; Details settings-boxes.md.

Vorherige Änderungen aus Build155/156 weiter enthalten: drei zweispaltige TERTIARY/SECONDARY-Kästen, Markenbild/Build, biceps-flexed, wrench, keine Chevrons, Einzüge16/8dp; Glow-Sticky-Kontur und Graphen bis zum Rand. [Details](settings-boxes.md).

## Bisheriger Stand und Grenzen

Build154: Bright als normalen Stil einmalig migrieren, dauerhaftes Löschen ohne virtuelle Wiederergänzung; allgemeine Beschriftungsgrößen/-gewichte für alle Textarten, echte Hausnummern32 im Quellfixture bestätigt, Anzeigegrenze14 statt17. Ebene standardmäßig aus. [Details](map-bright-typography-house-numbers.md). Nutzer kann eigene Stile als JSON zur Einbettung übergeben; bisher keine übermittelt.

Bisherige Gerätegrenzen weiterhin: Hausnummernanzeige/Dichte, Bosch-Ladeerkennung am Rad, alte Dubletten unverändert. Keine Sensor-/Aufzeichnungs-/GL-Lebenszyklusänderung durch diese UI-Aufträge. APK nur auf Auftrag über Actions, kein lokaler APK-Build/zusätzlicher Dispatch neben Push. Veröffentlichung/Downloadnachweis mit[skip ci].
