# Daten & Speicher – Konfiguration und selektive Sicherung V1

## Navigation und Umzug

„Daten & Speicher“ ist über das zentrale Einstellungsmenü erreichbar. Der Fullscreen-Screen enthält zwei Abschnitte und sieben Einträge:

1. **GLOBALE DATEN**: Einstellungen sichern als Inline-Accordion und Einstellungen wiederherstellen mit direkter Dateiauswahl.
2. **STRECKENDATEN**: Lokale Speicherung im bisherigen Unterdialog sowie WebDAV, intervals.icu, Dreeve und Strava als Inline-Schalter-/Accordion-Zeilen.

Native Kotlin-Views verwenden die vorhandenen Komponenten, Theme-Farbrollen, Headerhöhe, festen Footer, Systemleisten und Navigation. Texte/Fehler/Bestätigungen/Accessibility stehen in `strings.xml`; Icons sind lokale Lucide-Vektoren in FOREGROUND. Neue Assets: `database`, `upload`, `chevron-right`; der Generator unterstützt hierfür zusätzlich SVG-Ellipsen.

`RouteExportView` wird wiederverwendet und zeigt je nach `RouteExportPart` ausschließlich die lokalen oder die Cloud-Optionen. Lokale Speicherung enthält den bisherigen automatischen Save-Schalter und die SAF-Ordnerauswahl. Cloud enthält den bisherigen WebDAV-Schalter, Adresse, Benutzer, geschütztes App-Passwort samt Sichtbarkeit, relativen Zielordner, WLAN-Option und Verbindungstest. Es wurden keine neuen Speicher-/Exportformate oder Dienste erfunden.

Beim Speichern wird nur der jeweilige Teil in die frisch gelesene vorhandene `RouteExportConfig` übernommen. Der alte Konfigurationseinstieg rechts im Streckenmanager wurde entfernt. Alle fünf Detail-Footer-Aktionen, Erfolgsnachweise, Uploads, Originaldateien, SAF-Berechtigungen und automatische Ausgaben bleiben erhalten. Der bestehende SAF-Ordnerdialog kehrt jetzt über Daten & Speicher zur lokalen Konfiguration zurück.

## Datenquellen und Ausschlüsse

`SettingsBackup` verwaltet ein **öffentliches explizites Schema**; es exportiert keine rohe Preferences-Datei oder Dateiarchive. Die Quellen sind weiterhin `bike` und `route-export` sowie die vorhandenen Datenmodelle:

| Sicherungsbereich | Exportierte Werte / Quelle |
| --- | --- |
| Design & UI | Aktive Theme-ID, vorhandene Custom-Themes mit fünf validierten Farbrollen (`ThemeManager`), Fahrmodus-Glow (`ModeGlowSettings`) |
| Dashboard | Vier Seiten mit acht stabilen Datenfeld-IDs (`DashboardLayout`) und Layout-ID (`DashboardTemplate`), drei globale Karten-Datenfeldbelegungen |
| Aufzeichnung & FIT | Auswahl aus `RecordField.options`, Geschwindigkeits-/Distanzquelle (`RecordingConfig`), automatischer lokaler Speicherschalter aus `RouteExportConfig` |
| Karten & Navigation | Kartenquelle (`MapSource`), Nachtmodus, Folgen, Ausrichtung, Track-/GPX-Sichtbarkeitspräferenzen |
| Fahrerprofil | Sechs optionale Angaben aus `RiderProfileStore`; Karvonen-Zonen/Alter werden neu berechnet |
| Cloud-Einstellungen | Server, Benutzer, relativer Zielordner, Upload-Schalter, WLAN-Option aus der vorhandenen Streckenkonfiguration |

Ausgeschlossen: FIT/GPX-/Kartendateien, Streckenarchive/-geometrien/-IDs, Upload-/Speichernachweise, Fahrt-/Aufzeichnungssessions, Bluetooth-Adressen/Namen/Kopplungen/Enable-Caches, Standort-/Zoom-/aktuelle Seitenzustände, Berechtigungen, gerätegebundene SAF-Ordner-URIs und sämtliche Passwörter/Tokens/Schlüssel. Standort-, Kopplungs-, Passwort- und Ordnerrechte werden nicht auf ein neues Gerät kopiert. Bestehende Zielgeräte-Ordnerrechte bleiben bei der Wiederherstellung unangetastet; auf einem neuen Gerät wird der lokale Ordner separat gewählt.

Das Fahrerprofil ist bewusst Teil des manuellen, vom Nutzer auswählbaren Backups. Es wird weiterhin nicht automatisch in die Cloud synchronisiert oder in FIT-Rohdaten geschrieben.

## Format und Auswahl

JSON-Metadaten: `format = bicycle-settings`, `schemaVersion = 1`, `createdAt` als UTC-Instant, `appVersion`. Nutzdaten ausschließlich unter `sections` mit IDs `designUi`, `dashboard`, `recordingFit`, `mapsNavigation`, `riderProfile`, `cloudSettings`.

Die Auswahl bietet sechs Checkboxen, zunächst alle aktiviert, und Alle auswählen/abwählen. Ohne Auswahl ist die Ausführung deaktiviert. Exportiert werden ausschließlich die ausgewählten Bereiche. Der Speicherort wird über `ACTION_CREATE_DOCUMENT` gewählt; Vorschlag `BICYCLE_Settings_YYYY-MM-DD.json`. Erfolgsanzeige erst nach vollständigem Schreiben, Flush und Schließen. Ein ausstehender Export wird bei Activity-Neuerstellung über den vorhandenen Saved-Instance-State erhalten, nicht in einer zweiten Konfigurationsdatei.

Import verwendet `ACTION_OPEN_DOCUMENT`, liest ein begrenztes UTF-8-Dokument und validiert Format, Version, Metadaten und alle enthaltenen bekannten Bereiche, bevor eine Auswahl erscheint. Angezeigt werden nur tatsächlich vorhandene Bereiche. Danach ist eine ausdrückliche Bestätigung mit den gewählten Bereichsnamen erforderlich. Abbruch, Auswahl/Dateilesen und Bestätigungsvorschau schreiben keine Einstellungen.

## Validierung und Merge-Regeln

- Maximal 256 KiB; UTF-8 muss gültig sein. Vor dem JSON-Parser maximal 32 Ebenen; nach dem Rootobjekt keine angehängten Inhalte.
- Bekannte Werte besitzen strikt geprüfte Typen. Keine Boolean-/Zahlen-Strings, unvollständigen Zahlen oder unzulässigen Werte übernehmen.
- Dashboard-/Recording-/Layout-/Karten-IDs müssen in den vorhandenen Definitionen vorkommen; Seitenindizes eindeutig, jeweils 0–3; acht Felder je angegebener Seite, drei Kartenbelegungen. Ältere Sicherungen mit sieben Feldern bleiben gültig; das vorhandene achte Feld bleibt dann erhalten.
- Custom-Themes: höchstens 32, eindeutige IDs, vorhandene `ThemeValidator`-Prüfung einschließlich Kontrast. Presets werden nicht überschrieben.
- Profile: vorhandene `RiderProfileValidation`, einschließlich Geburt/Dezimalgewicht/Pulsordnung/FTP. Keine Alters-/Zonenwerte speichern.
- Cloud: HTTPS ohne URL-Zugangsdaten, Query oder Fragment; gültiger Benutzer, relativer Ordner ohne Traversal/Backslashes. Keine lokalen Dateipfade/URIs übernehmen. Auch der Export verweigert ungültige Konfigurationen bzw. in URLs eingebettete Zugangsdaten.
- Unbekannte JSON-Eigenschaften und Bereichsnamen werden ignoriert, niemals als Preferences-Schlüssel geschrieben. Ohne bekannten Bereich wird die Datei abgelehnt. Ungültige bekannte Werte/IDs werden abgelehnt, nicht durch Standardwerte ersetzt.
- Schema 1 ist die erste unterstützte Version. Andere Schemaversionen werden verständlich abgelehnt. Frühere Schema-1-Dateien mit fehlenden optionalen Feldern werden als Merge behandelt: fehlende Felder, Seiten und ganze nicht ausgewählte Bereiche bleiben unverändert. Explizites `null` bei optionalen Profil-/Kartenfeldern löscht genau diesen Wert; eine ausdrücklich angegebene Theme-Liste oder Recording-Feldliste ersetzt diese Liste.
- Fehlendes Accent in einem älteren Custom-Theme behält den aktuellen Accent desselben Themes bei; bei einem neuen Theme gilt FOREGROUND als vorhandene kompatible Initialisierung. Anschließend normale Theme-Validierung.

## Kontrollierte Übernahme und Rollback

Nach Auswahl wird aus den frisch gelesenen Zielwerten ein neuer, vollständig validierter `SettingsRestorePlan` erstellt. Er enthält ausschließlich erlaubte Änderungen für die beiden bestehenden Preferences-Dateien. Vor Änderungen werden die bisherigen Werte **nur der betroffenen Schlüssel** einschließlich fehlender Schlüssel gesichert. Jede Preferences-Datei wird per einem Editor atomar committed. Scheitert die erste oder zweite Übernahme, werden beide betroffenen Schlüsselmengen zurückgesetzt. Es werden keine fremden/zwischenzeitlich geänderten unbeteiligten Schlüssel mit einem vollständigen Clear überschrieben.

Technische Grenze: Android SharedPreferences bietet keine gemeinsame Transaktion über zwei Dateien und keine Crash-Atomizität über Prozessabbruch zwischen zwei Commits. Für abgefangene Schreibfehler gibt es kontrollierten Rollback. Scheitert dieser ebenfalls, erscheint ausdrücklich ein Rollback-Fehler und niemals eine Erfolgsanzeige; ein vollständiger Rücksetz-Erfolg kann bei defektem Speicher nicht garantiert werden.

Nach Erfolg lädt `ThemeManager.reloadFromPreferences` die übernommenen Custom-Themes/aktive Palette, Dashboard und Karten aktualisieren die vorhandenen Views, das Profil ist über seinen bestehenden Store sofort lesbar und Recording-Optionen über den vorhandenen Config-Getter verfügbar. Die unveränderliche Konfiguration einer bereits laufenden Aufnahme bleibt unverändert. Für ausgewählte Cloud-Konfiguration wird der bestehende Upload-Scheduler informiert.

## Zugangsdaten

Passwörter werden weder gelesen noch exportiert oder importiert. Die vorhandenen verschlüsselten Passwortwerte bleiben beim UI-Umzug und Rollback erhalten. Wird durch Import Server oder Benutzer geändert, wird Cloud deaktiviert und `needsPassword` in derselben vorhandenen Streckenkonfiguration gesetzt. Dasselbe gilt bei gewünschter Aktivierung ohne nutzbares Passwort. `hasPassword` und `password` geben dann keinen verwendbaren alten Zugang zurück; auch ein Verbindungstest kann damit kein altes Geheimnis an den importierten Server senden. Erst die bewusste Eingabe/Speicherung eines neuen Passworts hebt diese Sperre auf. Bei identischem bestehenden Konto können die vorhandenen gültigen Zugangsdaten erhalten bleiben. Die Oberfläche informiert gegebenenfalls kurz über die erforderliche Neueingabe.

## Dateien und Tests

Neue Dateien: `SettingsBackup.kt`, `DataSettingsView.kt`, drei Lucide-Vektoren; `SettingsBackupTest.kt`, `DataSettingsNavigationTest.kt` und diese Dokumentation.

Geändert: Einstellungen-/MainActivity-Navigation, die bestehende `RouteExportView`/`RouteExportSettings`, Streckenmanager-Header, semantischer Icon-Katalog, `ThemeManager`, zentrale Textressourcen und SVG-Importer. Bestehende Einstellungs-, Profil-, Streckenmanager-, Export- und Designmigrationstests wurden auf die neuen Einstiegspunkte angepasst; entfernte Konfigurationseinstiege werden auf Abwesenheit geprüft. Die Speicher-/Upload-/FIT-/Sensorservices und Rohdatenformate bleiben erhalten.

Neue Tests decken alle/einzelne Sicherungsbereiche, Metadaten, echte konfigurierte Werte, Ausschlüsse, selektive/unabhängige Übernahme, Profil/Karvonen, Theme-Reload, ältere optionale Felder/Accent, unbekannte Schlüssel, ungültige Versionen/Typen/URLs/IDs/Profile, Größen-/Tiefengrenzen, erste/zweite Commitfehler und Rollbackfehler, geschlossene Streams einschließlich Close-Fehler, tatsächliche SAF-Intents, Exportabbruch, Pflichtbestätigung, Activity-Neuerstellung, die neuen und entfernten Einstiegspunkte, unveränderte Ordner/Zugänge sowie Fullscreen und alle Preset-Themes ab.

Abschlussprüfung am 12.10.2026: vollständige Suite mit **294 Tests**, 0 Fehler, 0 Fehlschläge, 0 übersprungen. Release-Build und Release-Lint-Vital erfolgreich in **1m 51s**. Bekannte Produktionswarnung `onBackPressed()` bleibt. Keine Tests deaktiviert; kein zusätzlicher vollständiger Lint-Lauf. Praktische Prüfung der Geräte-Dateiprovider, Ordnerrechte und echten Nextcloud-Verbindung steht aus. Der Quellstand liegt nach APK 0.7.6; es wurde noch keine neue signierte Test-APK veröffentlicht.

Der Stand ist inzwischen in [APK 0.7.7](milestone-077.md) enthalten.

## Inline-Accordions für Dienste und Sicherung (17.10.2026)

WebDAV, intervals.icu, Dreeve und Strava sind direkt auf Daten & Speicher als Schalter-/Chevron-Zeilen eingebettet. Höhe, normale Beschriftung und Icon entsprechen den Sensorzeilen. Der native Aktivierungsschalter und der Chevron funktionieren unabhängig: Aufklappen ändert keine Aktivierung und löst weder Upload noch Verbindungstest aus. Alle Details sind anfangs eingeklappt. Die bisherigen Formulare und Auto-Save-Listener bleiben erhalten; echte Switch-Instanzen werden aus den Formularen in die Kopfzeile versetzt. Konten bleiben global, Dienste arbeiten unabhängig.

Einstellungen sichern öffnet ebenfalls einen Inline-Bereich mit bisheriger Bereichsauswahl und Aktion zum Schreiben über die Android-Dateiauswahl. Einstellungen wiederherstellen bekommt denselben Zeilenstil, öffnet aber weiterhin sofort die Dateiauswahl, gefolgt von Bereichsauswahl und Bestätigung. Lokale Streckenspeicherung behält ihren bestehenden Unterdialog.

Alle Dienst-Worker gehören zur gemeinsamen Datenseite und werden beim Schließen aufgeräumt. DialogNavigation führt mehrere registrierte Cleanup-Callbacks aus. Seite und offene Accordions bleiben beim Wechsel zu einer anderen App erhalten.

Hell/Dunkel ist global; der Umschalter steht ausschließlich auf der Haupt-Einstellungsseite. Die Theme-Auswahl bleibt profilbezogen. Design-Backups enthalten den globalen Helligkeitswert; alte Themes werden beim Import auf die neuen [14 festen Paletten](theme-palettes.md) abgebildet.
