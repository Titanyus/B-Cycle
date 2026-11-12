# Einstellungsübersicht und Glow-Rahmen · 12.11.2026

Die Hauptseite besitzt in Flat und Glow drei gerahmte, abgerundete TERTIARY-Kästen. Überschriften Bike, Training und HRV verwenden die bisherigen League-Gothic-Menürollen. Zwei gleich breite, gleich hohe SECONDARY-Spalten mit eigenem Dropdown-Rahmen und horizontalen Trennern: links Aktionen, rechts Einstellungen. Auch die noch leere Training-Spalte rechts bleibt bestehen. Kein Gruppen-Hintergrundbild; bestehendes Markenbild und Buildanzeige unverändert darunter, Erweitert separat.

Bike links: Gefahrene Strecken, Streckenvorlagen, Statistik, Wartung. Rechts: Fahrer, Fahrradprofile, Benachrichtigungen, Backup & Cloud. Training links: Training. HRV links: Messung, Auswertung. Rechts: Einstellungen & Tagesbefinden. Messung ersetzt den bisherigen separaten oberen Pillenbutton; alle bisherigen Aktionen/Tags und Navigationspfade erhalten. Das Trainingssymbol ist nun biceps-flexed, funktionale Leistungssymbole unverändert.

Glow-Sticky-Kästen bekommen zusätzlich eine 2dp-Kontur: FOREGROUND mit controlBorderAlpha (25%), entsprechend normalen Dropdowns. Der gemeinsame Hintergrund zeichnet den äußeren Rahmen um Kopf und Inhalt; der native Sticky-Kopf führt die Kontur beim Pinnen fort, ohne zusätzlichen View oder geänderte Scroll-/Touchgeometrie. Normale Rahmen-Dropdowns hatten diesen Rahmen bereits. Flat unverändert.

## Prüfung

SettingsBoxesTest prüft beide Designs bei288/360/480dp, alle13 Ziele, gleiche Spaltenhöhen/-breiten, Hintergrundfarben, leere rechte Trainingsspalte und Markenblock. Native Vorschaubilder unter app/build/reports/ui/settings-boxes-flat.png und settings-boxes-glow.png visuell geprüft. SettingsMenuTest mit7 Methoden einschließlich unveränderter Buildanzeige und Navigation grün. Zwei gezielte BicycleStickySectionsTest-Methoden für Pinnen/Verdrängen, native Scrollgesten und Typografie grün. Zusammen10 Methoden in settings-boxes-final-tests.log und nach letzter Abstands-/Versionsanpassung erneut in settings-boxes-release-tests.log. Zentrale Typografie-, Atlas-, Bright- und Diffprüfungen grün.

Ein zusätzlicher Lauf älterer Sticky-Tests meldete3 Fehler: ein Test erwartet noch ein altes selektiertes Kachel-Drawable, ein anderer den früher separaten Strecken-Aufzeichnungskopf, der dritte eine ältere Kontrollfüllfarbe. Diese Assertions wurden nicht als Teil dieser Darstellungsänderung umgeschrieben; keine grüne Gesamtsuite behauptet. Protokoll settings-boxes-glow-border-tests.log. Geräteprüfung von Rahmen und Aufteilung in der nächsten APK bleibt erforderlich.

Nachanpassung: Chevrons entfernt, Überschriften16dp und Menüzeilen8dp eingerückt. Innen-/Außenradius identisch6dp. HRV-Messung heart-pulse, Auswertung chart-line, Einstellungen wrench; Zahnradrollen systemweit wrench. Gezielte Prüfung: settings-menu-refine-tests.log.

## Neue Anpassung nach Build157

Menülabels auf Hauptseite „Strecken“ und „Nachrichten“. Menüzeilen verwenden dieselbe bodyTypeface/choiceSp14 und iconSize24dp wie Rahmen-Dropdown-Köpfe; Gruppenüberschriften unverändert. Flat: gerahmte Hauptgruppen4dp seitlich außen/16dp innen; Standard-Sticky-Abschnitte4dp seitlich und Inhalt16dp vom Kopf-/Kastenrand entfernt. Glow-Abstände unverändert. Themewechsel berücksichtigt Einzüge und Zentrierung.

Aktiver Track und User Track1–3 teilen eine SECONDARY-Rahmenfläche mit drei horizontalen Trennern. Originale BicycleFrameDropdown-Köpfe/Inhalte/Schalter und IDs bleiben erhalten; einzelne Dropdownflächen ohne eigenen Rahmen und ohne zusätzliche Zwischenmargins. Radwege/Radrouten separat unverändert.

Prüfung: settings-grouped-track-final-tests.log für Menü/Navigation/Trackwerte/Sticky. Ein zusätzlich gestarteter CyclingOptionsTest scheiterte bereits bei nativer Zoominitialisierung mit „Zoom limit is out of range“ (settings-grouped-track-tests.log); keine grüne Gesamtsuite behauptet.

BicycleGroupedBox vereinheitlicht gerahmte Listen global:8dp Gesamtzwischenraum zwischen Einträgen, davon2dp Linie und je3dp davor/danach, wie bisheriger Dropdown-Abstand. Menüs und Trackgruppe nutzen denselben Container; gruppierte Dropdowns bekommen keine zusätzlichen automatischen Margins. Prüfung grouped-box-spacing-tests.log erfolgreich.
