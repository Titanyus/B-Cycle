# Einheitliche UI-Navigation und automatische Speicherung

Die Änderungen sind ab Test-APK 0.7.12 enthalten.

## Verbindliche Konventionen

- Jeder appinterne Dialog hat links im Header `chevron-left` für Zurück.
- Headerhöhe bleibt zentral 64 dp. Titel sind zur gesamten Fensterbreite zentriert;
  gleich große seitliche Reserven verhindern eine Verschiebung durch das Chevron.
- Abschnittsüberschriften verwenden den bestehenden Systemfont in Fettschrift.
  Der schmale Header-/Messwertfont und die bestehenden Größen bleiben erhalten.
- Gültige Einstellungen werden automatisch gespeichert. Unvollständige oder
  ungültige Formulare überschreiben nicht die letzte gültige Konfiguration.
- Speichern/Abbrechen/Schließen als Dialognavigation entfallen. Zurück im Header
  und Android-Zurück führen zum selben Elternscreen und erhalten dessen Scrollposition.
- Ein Footer ohne fachliche Aktionen entfällt vollständig, einschließlich Platzbedarf.
- Importieren und die fünf Verwaltungsaktionen der Streckendetails bleiben erhalten.
  Fachliches Speichern einer Fahrt ist weiterhin eine Aktion; es ist kein Formulabschluss.
- Löschen, Standardbelegung zurücksetzen und Backup-Wiederherstellung erfordern
  weiterhin eine ausdrückliche Bestätigung. Zurück führt sie nicht aus.
- Jeder Einstellungs-Menüpunkt öffnet einen separaten Unterdialog. Erweitert ist ein
  eigener Screen. Hauptmenüzeilen haben Icons und doppelte normale Höhe (96 dp),
  ausschließlich dort; Untermenüs bleiben bei mindestens 48 dp.
- Die gesamte App ist ausschließlich für Hochformat vorgesehen.

## Speicherung und Validierung

Dashboard-Presets, Glow, Kartenoptionen, Graph-Auswahl und FIT-Feldauswahl verwenden
weiterhin ihre vorhandenen Preference-Schlüssel. Profile werden nur nach erfolgreicher
bestehender Gesamtvalidierung geschrieben. Unfertige Dezimalzahlen bleiben Eingaben;
optionale leere Felder werden beim Fokuswechsel bzw. Verlassen übernommen.
Datumsauswahl speichert jede gültige Änderung. Jahr und Datum im nativen Kalenderkopf
werden mit 18/28 sp und den bestehenden Themefarben hervorgehoben.

Lokale Speicher- und Cloud-Einstellungen bleiben unabhängig. Eine serielle
Schreibwarteschlange führt jede Änderung mit dem aktuellen anderen Konfigurationsteil
zusammen. Passwortverschlüsselung bleibt in RouteExportSettings; Aufdecken des
bereits gespeicherten Passworts löst keine erneute Speicherung aus. Aktiviertes WebDAV
benötigt eine formal gültige Adresse, Benutzer/Ordner und Passwort; der bestehende
Verbindungstest bleibt ein ausdrücklicher Netzwerkvorgang.

Umbenennungen übernehmen nichtleere Namen über die bestehende Streckenverwaltung.
Eine kurze Zusammenfassung unmittelbar aufeinanderfolgender Tastatureingaben
verhindert parallele Bibliotheksänderungen. Back verwirft die gültige Eingabe nicht.

## Zustands-Icons

Umschalt-Icons zeigen appweit den **aktuellen Zustand**:

| Funktion | Zustand | Icon |
| --- | --- | --- |
| Karte | hell / dunkel | sun / moon |
| Kartenausrichtung | Norden / Fahrtrichtung oben | compass / arrow-up-from-dot |
| Kartenansicht | eingebettet / Vollbild | minimize / maximize |
| Passwort | verborgen / sichtbar | eye-closed / eye |
| Verbindung | deaktiviert / aktiviert | unplug / link |
| Track | ausgeblendet / eingeblendet | eye-closed / eye |

Reine Befehle wie Importieren oder Löschen bleiben Aktionssymbole.
Accessibility-Beschreibungen dürfen zusätzlich die nächste Aktion erklären.
Icon-Assets stammen aus derselben Lucide-Version und verwenden bestehende Farbrollen.

## Kartenpositionen

Die Kartenwerte liegen ohne unteren Außenabstand direkt über der Bedienleiste.
Zoom +/- liegen rechts darüber mit kleinem Abstand; der rechte Kartenwert bleibt
frei. Der Editor-Beenden-Button hat den zentralen horizontalen Innenabstand.

## Prüfungen

Die vorhandenen UI-Tests wurden von Entwurf/Speichern/Abbrechen auf sofortige
Persistenz und Header-Zurück umgestellt. Zusätzliche Prüfungen sichern leere Footer,
Fensterzentrierung, getrenntes Erweitert, Profile inklusive ungültiger Eingaben,
Datumskopf und Glyphen für tatsächliche Karten-Zustände. Wiederholtes Kartenlayout
verwendet dasselbe Ausrichtungs-Drawable, um erneute Layoutschleifen zu verhindern.

Abschlusslauf am 14.10.2026: `:app:testDebugUnitTest :app:assembleRelease` erfolgreich
in **2 Minuten 21 Sekunden**. **357 Tests, 0 Fehler, 0 Fehlschläge, 0 übersprungen**.
Release-Lint-Vital und R8 erfolgreich. Die bestehenden Tests für Sensoren, FIT,
Performance und Kartenbewegung bleiben aktiv; keine Tests wurden deaktiviert.

Vorhandene Test-Deprecation-Hinweise zu Android-Zurück sowie ein nichtfunktionaler
Android-Analytics-Verzeichnishinweis bleiben bestehen. Ein Praxistest mit realer
Tastatur und Systemnavigation auf dem Telefon steht noch aus. Der Quellstand ist
auf GitHub gesichert. Er ist anschließend zusammen mit der Graph-Anpassung in
[Test-APK 0.7.12](milestone-0712.md) enthalten.

## Präzisierung nach APK 0.7.12

Abschnittsüberschriften übernehmen appweit die Größe der Streckendetails:
23 sp über BicycleDesign.titleSp. System-Fettschriften verwenden zentral
BicycleDesign.strongTypeface, angefordertes Gewicht 900 Black im Android-sans-serif-
Systemfont (Roboto auf Standard-Android; Geräte ohne diese Familie verwenden die
System-Ersatzschrift). Es wird keine zusätzliche Schriftdatei eingebunden.
Bebas-Neue-Fenstertitel und Messwerte bleiben unverändert. Das gilt auch für dynamisch
aufgebaute Feldauswahl-/FIT-Abschnitte und Profile. Nur im Haupt-Einstellungsmenü
werden Menüschrift und führende Menü-Icons verdoppelt: 28 sp und 48 dp.
Untermenüs sowie Navigations-Chevrons behalten ihre üblichen Maße.

Sonne/Mond steht nun links oben direkt rechts neben dem Track-Auge (2 dp Abstand).
Die rechte obere Leiste besteht nur aus Zentrierung, Ausrichtung und Vollbild.
Auf kurzen Karten werden die vorhandenen Buttonhöhen im verfügbaren Raum angepasst;
die zweite rechte Buttonzeile entfällt. Verhalten, Nachtfarben, gespeicherter Zustand
und gemeinsames Ausblenden bleiben erhalten.

Abschlussprüfung: vier gezielte Tests (Menügrößen, Kachel-Modus/Persistenz/Backup,
23-sp-/900-Typografie sowie Kartenposition/Nachtzustand) und Release-Kotlin-
Kompilierung erfolgreich in 12 Sekunden. Keine vollständige Suite oder neue APK
für diese Änderungen. Die bestehenden Tests wurden auf die geänderte Bedienung
angepasst, nicht deaktiviert.
