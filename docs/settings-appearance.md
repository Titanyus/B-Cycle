# Helle und dunkle Einstellungen

Sonne/Mond steht ausschließlich rechts im Header der Haupt-Einstellungsseite. Das Icon zeigt den aktuellen Zustand. Unterseiten haben keinen weiteren Umschalter, folgen aber sofort der globalen Auswahl. Die Themeauswahl bleibt je Fahrradprofil gespeichert.

| Farbrolle | Helle Darstellung |
| --- | --- |
| PRIMARY | Unverändert aus dem gewählten Theme |
| SECONDARY | #FFFFFF |
| TERTIARY | Basis #E3E3E3 bei farbigen Themes, #D0D0D0 bei DARK-Themes; jeweils mit 8 % PRIMARY gemischt |
| FOREGROUND | #101010 mit 5 % PRIMARY gemischt |
| ACCENT | Unverändert aus dem gewählten Theme |

Die RGB-Kanäle werden einzeln gemischt und auf ganze Farbwerte gerundet. Beispiel HELLFIRE: Schrift #191111, graue Fläche #E0D4D3; HELLFIRE DARK verwendet im Hellmodus für die graue Fläche weiterhin #CFC3C2. Diese Parameter sind interne Standards ohne zusätzliche Regler.

Standard ist dunkel; alle [14 vorgegebenen Themes](theme-palettes.md) sind Basispaletten für diese Darstellung. Theme-Vorschauen zeigen nur PRIMARY, SECONDARY und TERTIARY. Themetitel sind 21 sp Bebas Neue, wie die schmalen Eingabewerte. Keine Reserviert-Karte oder eigene Farbwahl.

`ThemeManager.settings` stellt ausschließlich für Dialoge eine abgeleitete Palette bereit. Dashboard, Rand-Glow, Nachrichtenfenster während der Fahrt und Kartenhelligkeit behalten ihre eigene bestehende Darstellung. Die Themeauswahl wird nicht überschrieben. Einheiten/Zusatztexte verwenden 65 % des aktuellen FOREGROUND. Formularrahmen verwenden zentral 1 dp, 50 % FOREGROUND und 6 dp Rundung.

`uiTheme.settingsLight` ist global, wird bei Profilwechsel und Profilübernahme weder gesichert noch ersetzt. Alte Werte in Profil-Snapshots werden ignoriert. Design-Backups sichern den globalen Wert; alte Sicherungen ohne diesen Schlüssel behalten die aktuelle Auswahl. Technische ältere Theme-IDs werden auf die neuen Presets abgebildet.

Seitenheader auf PRIMARY verwenden im Hellmodus weiße Schrift, während normale Inhalte die dunkle, getönte Textfarbe behalten. Sticky-Kategorien auf TERTIARY behalten die normale Textfarbe. Diese Rolle gilt auch beim Öffnen von Unterseiten im bereits aktiven Hellmodus und bei späteren Themewechseln.

Die fünf Verlaufsgraphen in den Streckendetails verwenden in Hell und Dunkel dieselben Diagrammfarben aus der jeweiligen Basispalette. Ihre Flächenfüllung bleibt bei 100 %. Puls- und Leistungsverteilung unten verwenden im Hellmodus hingegen die dunkle Textfarbe für Kurve und transparenten Füllverlauf; auf dunkler Darstellung bleibt die Kurve weiß. Zonenhintergründe und Messdaten werden nicht geändert.
