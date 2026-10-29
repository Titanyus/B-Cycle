# Kompaktes Einstellungsmenü

Nutzerentwurf29.10.2026: alle Haupteinträge in League Gothic, Größe wie Sticky-Header über BicycleDesign.sectionTitleSp (30sp). Beschriftungen behalten ihre bestehende Groß-/Kleinschreibung. Zeilenminimum54dp statt72dp, Innenpadding vertikal6dp statt8dp. Haupticons24dp statt32dp (75%), Chevrons18dp statt24dp (75%). Maße zentral in BicycleDesign.

Nur SettingsMenuView-Hauptseite; Unterseite Erweitert weiter bisherige normale Zeilen/Schrift/Iconmaße. Reihenfolge, zwei Gruppentrenner, Art-/Buildanzeige, Listener, Navigation und Speicherfunktionen unverändert. Kein pauschales Umstellen anderer Listen auf diese Typografie.

Prüfung: sieben SettingsMenuTest-Fälle grün, native240/320dp-Hochformatbreiten beixxhdpi, tatsächliche Zeilenhöhe/Schrift-/Iconmaße und unveränderte Einstiegs-/Zurück-/Scroll-/Appwechselpfade. Log /workspace/.tools/settings-menu-league-compact-final-tests.log. Im ersten Lauf6/7grün; League-Gothic-Fontmetriken plus altes8dpInnenpadding überschritten54dp geringfügig, mit zentralem6dpInnenpadding korrigiert. Kein Schrift-Clipping oder Funktionswechsel als Lösung.

Native Vorschau: [Einstellungsmenü](previews/settings-menu-compact.png). Keine APK für diese Änderung angefordert.
