# Helle und dunkle Einstellungen

Über den Sonne-/Mond-Button rechts im Einstellungsheader wird zwischen hell und dunkel gewechselt. Das Icon zeigt den aktuellen Zustand. Bei Streckendetails bleibt rechts der Umbenennen-Button; die dortige Darstellung folgt der in den Einstellungen gewählten Helligkeit.

| Farbrolle | Helle Darstellung |
| --- | --- |
| PRIMARY | Unverändert aus dem gewählten Theme |
| SECONDARY | #FFFFFF |
| TERTIARY | #CCCCCC |
| FOREGROUND | #202020 |
| ACCENT | Unverändert aus dem gewählten Theme |

Standard ist dunkel. Der Schalter verändert ausschließlich die Darstellung von Einstellungs- und Konfigurationsdialogen. Dashboard, Rand-Glow, Nachrichtenfenster während der Fahrt und Kartenhelligkeit behalten ihre bestehende Palette. Theme-Auswahl und eigenes Theme werden nicht überschrieben.

`ThemeManager.settings` stellt eine abgeleitete Palette bereit und folgt der Basis-Themeauswahl. Änderungen werden auf angehängten Views sofort dargestellt. Einheiten und Zusatztexte verwenden 65 % des aktuellen FOREGROUND. Formularrahmen verwenden 1 dp, 50 % FOREGROUND, 6 dp Rundung; Text und Icons werden nicht mit abgedunkelt.

Der Zustand `uiTheme.settingsLight` wird je Bikeprofil gespeichert, bei Profilübernahme kopiert und im Design-Backup mitgesichert. Alte Sicherungen ohne diesen Wert behalten die aktuelle Auswahl, Profile ohne gespeicherten Wert starten dunkel.

Zusätzlich enthalten: Embers Inv. (#BF281B / #1D1D1D / #000000 / #FFFFFF / #BF281B), transparente Fahrradfoto-Mülltonne, schwarzes Karten-Vergrößern-Symbol, Vollbildoption am Anfang von Design & UI und kompakte Dauer-/Positionsdarstellung in den Benachrichtigungseinstellungen.

Prüfung: 57 gezielte Tests für Theme-/Headerumschaltung, unveränderte Dashboard-Palette, Profil- und Backup-Roundtrip, Themeauswahl, Sticky-Abschnitte, Konturen sowie Benachrichtigungs-Layout erfolgreich. Die korrigierte Header-Testfixture wurde gesondert erneut geprüft. Kein zusätzlicher Gerätetest.
