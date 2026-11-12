# Einstellungsübersicht und Glow-Rahmen · 12.11.2026

Die Hauptseite besitzt in Flat und Glow drei gerahmte, abgerundete TERTIARY-Kästen. Überschriften Bike, Training und HRV verwenden die bisherigen League-Gothic-Menürollen. Zwei gleich breite, gleich hohe SECONDARY-Spalten mit eigenem Dropdown-Rahmen und horizontalen Trennern: links Aktionen, rechts Einstellungen. Auch die noch leere Training-Spalte rechts bleibt bestehen. Kein Gruppen-Hintergrundbild; bestehendes Markenbild und Buildanzeige unverändert darunter, Erweitert separat.

Bike links: Gefahrene Strecken, Streckenvorlagen, Statistik, Wartung. Rechts: Fahrer, Fahrradprofile, Benachrichtigungen, Backup & Cloud. Training links: Training. HRV links: Messung, Auswertung. Rechts: Einstellungen & Tagesbefinden. Messung ersetzt den bisherigen separaten oberen Pillenbutton; alle bisherigen Aktionen/Tags und Navigationspfade erhalten. Das Trainingssymbol ist nun biceps-flexed, funktionale Leistungssymbole unverändert.

Glow-Sticky-Kästen bekommen zusätzlich eine 2dp-Kontur: FOREGROUND mit controlBorderAlpha (25%), entsprechend normalen Dropdowns. Der gemeinsame Hintergrund zeichnet den äußeren Rahmen um Kopf und Inhalt; der native Sticky-Kopf führt die Kontur beim Pinnen fort, ohne zusätzlichen View oder geänderte Scroll-/Touchgeometrie. Normale Rahmen-Dropdowns hatten diesen Rahmen bereits. Flat unverändert.

## Prüfung

SettingsBoxesTest prüft beide Designs bei288/360/480dp, alle13 Ziele, gleiche Spaltenhöhen/-breiten, Hintergrundfarben, leere rechte Trainingsspalte und Markenblock. Native Vorschaubilder unter app/build/reports/ui/settings-boxes-flat.png und settings-boxes-glow.png visuell geprüft. SettingsMenuTest mit7 Methoden einschließlich unveränderter Buildanzeige und Navigation grün. Zwei gezielte BicycleStickySectionsTest-Methoden für Pinnen/Verdrängen, native Scrollgesten und Typografie grün. Zusammen10 Methoden in settings-boxes-final-tests.log und nach letzter Abstands-/Versionsanpassung erneut in settings-boxes-release-tests.log. Zentrale Typografie-, Atlas-, Bright- und Diffprüfungen grün.

Ein zusätzlicher Lauf älterer Sticky-Tests meldete3 Fehler: ein Test erwartet noch ein altes selektiertes Kachel-Drawable, ein anderer den früher separaten Strecken-Aufzeichnungskopf, der dritte eine ältere Kontrollfüllfarbe. Diese Assertions wurden nicht als Teil dieser Darstellungsänderung umgeschrieben; keine grüne Gesamtsuite behauptet. Protokoll settings-boxes-glow-border-tests.log. Geräteprüfung von Rahmen und Aufteilung in der nächsten APK bleibt erforderlich.
