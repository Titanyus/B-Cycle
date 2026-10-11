# Design Engine – Phase 1

Diese Phase ist eine technische Grundlage. Es gibt keinen Farbwähler, keinen
Theme-Menüpunkt und keine festgelegte Einstellungsseite. Bestehende Dialoge,
Streckenmanager, Dashboard und Karte werden noch nicht umgestellt. Ihr Aussehen
bleibt erhalten. Die späteren Schritte benötigen die Freigabe des Nutzers.

## Gemeinsames Modell

`BicycleTheme` ist die zentrale, versiegelte Schnittstelle. `PresetTheme` und
`CustomTheme` sind unveränderliche Datenobjekte mit ID, Name, `ThemeTokens` und
Kennzeichnung `isCustom`. Tokens bestehen ausschließlich aus vier deckenden
ARGB-Farben: Primary (Header), Secondary (Fläche), Tertiary (Eingabe), Foreground
(Text, Icons, Trennlinien). Die Komponenten unterscheiden nicht zwischen Preset
und Custom. IDs verwenden getrennte Namensräume `preset:` und `custom:`.

`BicycleThemes.presets` ist ein zentraler, unveränderlicher Katalog, der später
um weitere abgestimmte Presets erweitert werden kann. BICYCLE Red ist der
Standard: `#D00000`, `#900000`, `#740000`, `#FFFFFF`.

## Verwaltung und Speicherung

Der anwendungsweite Zugang ist `RideRuntime.get(context).themes`. Der Manager
kennt keine Activity, keinen Menüpunkt und keine bestimmte Einstellungsseite.
Er verwendet die vorhandenen `bike`-SharedPreferences, ausschließlich mit den
neuen Schlüsseln `uiTheme.activeId` und `uiTheme.customThemes`; bestehende Sensor-,
Dashboard-, Karten- und Fahrt-Einstellungen bleiben erhalten.

Custom-Themes liegen als versioniertes JSON (Version 1) vor. Es werden maximal
32 Themes und maximal 32 KiB gespeichertes JSON geladen. Falsch formatierte
Einträge, falsche Farbwerte, doppelte IDs und unbekannte Speicherversionen werden
nicht aktiviert. Fehlt die aktive ID oder ist sie ungültig, wird BICYCLE Red
aktiviert und seine ID gespeichert.

Technische APIs, noch ohne Benutzeroberfläche:

- `activate(id)`: prüft das Ziel vor Aktivierung, speichert die ID; ungültige IDs
  stellen den Standard wieder her und liefern `false`.
- `saveCustom(theme)`: erstellt oder bearbeitet nach Validierung. Ungültige
  Entwürfe ändern weder die Sammlung noch das aktive Theme; Probleme werden
  als `ThemeValidation` zurückgegeben.
- `copyTheme(sourceId, name)`: kopiert Preset oder Custom mit eigener UUID;
  liefert bei ungültigem Zielnamen oder fehlender Quelle `null`.
- `deleteCustom(id)`: entfernt nur Custom-Themes. Wird das aktive gelöscht,
  fällt der Manager auf BICYCLE Red zurück. Presets sind unveränderlich.
- `observe(callback)`: liefert sofort das aktive Theme auf dem Hauptthread
  und danach Änderungen; `ThemeSubscription.close()` beendet die Anmeldung.
  Auf dem Hauptthread erfolgen Farbänderungen unmittelbar, aus Hintergrundthreads
  nach Übergabe an den Hauptthread. Wiederholte gleichwertige Aktivierungen
  erzeugen keine unnötigen UI-Updates.

## Validierung

Alle vier Werte müssen deckend sein. Text und Icons benötigen auf jeder der
drei Flächen mindestens **4,5:1** Kontrast. Das entspricht dem WCAG-AA-Kriterium
für normale Schrift und übertrifft die Anforderung von 3:1 für UI-Grafiken.
Berechnet werden linearisierte sRGB-Werte und relative Luminanz nach WCAG.

Primary, Secondary und Tertiary müssen gleichgerichtet heller oder dunkler
werden. Jede benachbarte Stufe benötigt mindestens **0,01** Luminanzunterschied.
Dieser Wert ist eine dokumentierte Produktregel für die Flächenabstufung, kein
WCAG-Grenzwert. Er verhindert identische oder praktisch gleiche Flächen und
lässt helle sowie dunkle Paletten zu. Diese automatische Prüfung ersetzt keinen
späteren visuellen Test auf dem Gerät, insbesondere für Fokus und Bedienzustände.

IDs sowie Namen werden ebenfalls geprüft. Neue benutzerdefinierte Namen dürfen
1–60 Zeichen umfassen; Steuerzeichen und äußere Leerzeichen sind unzulässig.
Transparente Farben sind nicht zugelassen, da sonst der Kontrast von unbekannten
Hintergründen abhängt. Funktionale Zustände dürfen später etwa durch Umrandung,
Unterstreichung, Alpha oder Beschriftung zusätzlich unterscheidbar gemacht werden,
ohne neue grundlegende Farbrollen einzuführen.

## Design und wiederverwendbare Komponenten

`BicycleDesign` verwaltet ausschließlich feste Geometrie und Typografie:
Schriftgrößen, Headerhöhe, Drei-Spalten-Raster, Abstände, Eckenradien,
Icon-Größe, 48-dp-Berührungsflächen und Trennlinien von genau einem Pixel.
`ThemeTokens` enthält keinerlei Layoutwert.

`BicycleComponents` bietet native, opt-in Komponenten für Header, Dialogfläche,
Listenzeile, Text, Eingabe, Button, Icon, Divider, ein 2×3-Menüraster und
Scrollflächen mit festem Verhalten. Sie beziehen ausschließlich
die aktive Palette vom Manager. Ein Farbwechsel verändert die Drawable-Füllung,
Text- oder Icon-Farbe; Geometrie und Schrift bleiben unverändert. Angemeldete Views
beenden ihre Anmeldung beim Entfernen aus dem Fenster und beziehen beim erneuten
Anhängen die aktuelle Palette. Es gibt kein Polling und keine zusätzlichen
Bibliotheken. Die vorhandenen Bildschirme verwenden diese Bausteine erst nach
der Freigabe für eine spätere Migration.

Tests prüfen Kontrast und Abstufung, beschädigte Speicherdaten, CRUD/Kopieren,
Wiederherstellung, Hauptthread-Updates und Abmeldung, unveränderte bestehende
Einstellungen sowie identische Layoutmaße beim Wechsel auf ein helles Theme.

Prüfstand Phase 1: 135 Tests bestanden, Release-Build erfolgreich, Android-Lint
mit 0 Fehlern und 36 bereits bestehenden Warnungen. Es wurde keine neue
installierbare APK veröffentlicht und keine visuelle Bildschirmmigration begonnen.
