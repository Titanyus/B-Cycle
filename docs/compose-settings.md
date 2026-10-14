# Compose-Einstellungen – separater Testbranch

Branch: `feature/compose-settings`. Basis: signierte Native-APK 0.7.13 / Commit
`d12be4de57e9281dbf1bbb7c463d469701b2f8a6`. Der Hauptbranch bleibt unverändert.

## Umgestellte Inhalte

| Bereich | Umsetzung |
| --- | --- |
| Haupt-Einstellungen und Erweitert | Compose-Listen, bestehende Aktionen und Maße |
| Sensoren & Aufzeichnung | Status, Verbindung/Trennen, Geräteauswahl, FIT-Accordion und Felder |
| Fahrerprofil | Compose-Eingaben, bestehende Validierung, Alter und Karvonen-Zonen |
| Design & UI | Theme-Vorschau, neun Dashboard-Presets, vier Seiten, Glow und Editor-Einstieg |
| Kartenmanager | Kartenquelle, Sichtbarkeit/Verfolgung und vorhandene Befehle |
| Dashboard-/Karten-Feldauswahl | feste Quellenwahl mit Icons, Liste, Graphen/Farbpunkte und Kachelmodus |
| Daten & Speicher | Untermenüs, Sicherungsauswahl, lokale Optionen und WebDAV-Formular |
| Erweitert | Informationen/Lizenzen, Wiederherstellung, Diagnose und Reset-Aktion |
| Streckenname ändern | Compose-Eingabe mit bestehender Umbenennung und automatischer Speicherung |

Der bestehende native Dialograhmen mit zentriertem Bebas-Neue-Header, Zurück-Chevron,
Fullscreen-/IME-Insets und fachlichen Footer-Aktionen bleibt erhalten. Datums-/Dateiauswahl
und kurze Bestätigungen verwenden weiterhin native Komponenten. Streckenliste,
Streckendetails, Diagrammrenderer, Dashboardfelder und Mapsforge-Karte werden nicht
umgeschrieben. Das ist eine Migration der Formular-/Einstellungsinhalte, kein Wechsel
der gesamten App-Architektur.

## Design und Daten

Compose verwendet direkt BicycleDesign, die fünf ThemeTokens und bestehende Lucide-
Vector-Ressourcen. Kein Material-Design-Theme und keine zusätzliche Iconbibliothek.
Abschnittsüberschriften: 23 sp / Systemfont 900 Black. Hauptmenü: 96-dp-Mindesthöhe,
28-sp-Text und 48-dp-Menü-Icons. Gemeinsame Eingabemaße/Radien, Foreground und der
vorhandene Dashboard-Einheitengrauton bleiben erhalten. Themewechsel aktualisieren
Compose und den nativen Rahmen gemeinsam. Themepresets passen ihr Spaltenraster bei
schmalen Hochformatflächen bzw. größerer Systemschrift an.

State liegt je Dialog in Compose. Gültige Änderungen gehen sofort in die vorhandenen
Preferences/Stores. Profile verwenden RiderProfileValidation und Karvonen, die FIT-
Auswahl RecordingConfig/RecordField, Quellen/Graphen DashboardField/GraphOverlayConfig.
Es gibt keine zweite Sensorverarbeitung, FIT-Aufzeichnung oder Performanceberechnung.
Back erhält Elternansicht und deren Scrollposition. Eine Composition übersteht das
Umsetzen des Inhalts in den bestehenden Fullscreen-Host; beim Schließen werden
Lifecycle und Beobachter freigegeben. Lange Feld-/FIT-Listen verwenden LazyColumn.

WebDAV bleibt beim vorhandenen WebDavClient und verschlüsselten RouteExportSettings.
Lokale und Cloud-Änderungen werden seriell mit der jeweils aktuellen anderen Einstellung
zusammengeführt. Passwortaufdecken löst keinen Speichervorgang aus. Passwort-Drafts
werden nicht in rememberSaveable / Android Saved Instance State abgelegt. Ordnerwahl,
Backup-Export/-Restore und bestätigte Reset-/Löschaktionen behalten ihre Dienste.

## Build und Tests

Kotlin-Compose-Compilerplugin: 2.1.20, passend zum vorhandenen Kotlin. Compose-BOM:
2025.03.00; Foundation/UI/Runtime ohne Material. Lifecycle 2.8.7, SavedState 1.2.1.
Android 13+ / Compile-/Target-SDK 36 unverändert. Build-JDK/SDK-Konfiguration wie im
bestehenden Projekt; kein zusätzlicher Dienst oder Emulator für die JVM-Prüfungen.

```sh
./gradlew :app:testComposeDebugUnitTest --tests '*ComposeSettingsTest' --tests '*ComposeDialogIntegrationTest'
./gradlew :app:testDebugUnitTest --tests '*FitWriterTest' --tests '*SettingsBackupTest' --tests '*RiderProfileTest' --tests '*ThemeManagerTest' --tests '*UiConventionsTest'
./gradlew :app:assembleRelease
```

13 Compose-Prüfungen und 31 ausgewählte bestehende Prüfungen bestanden. Dazu gehören
echte MainActivity-Dialoge und Back-Navigation, Feldauswahl mit Quellenwechsel,
unabhängige Graph-Modi, FIT-Feld-/Quellenpersistenz, lokale Optionen, Backup-All/None,
gültige/ungültige Profile, Passwortanzeige, 23-sp-/900-Typografie, Themewechsel und
Lifecycle-Freigabe. Ausgeblendete Lazy-List-Inhalte werden über die Scroll-Semantik
aufgerufen, statt nicht vorhandene native Widgets vorzutäuschen. Bestehende Tests
wurden nicht deaktiviert. Keine erneute vollständige Suite für diesen Testbranch.

Release-Build mit R8, Resource-Shrinking und Lint-Vital bestanden. Die signierte APK hat 2,873,785 Byte
(2,87 MB; Native-Vergleich: 1.087.274 Byte). Beide verwenden denselben Testschlüssel.
Signatur, Paketmetadaten und ZIP-Ausrichtung für 16-KB-Seiten sind geprüft.
SHA-256: `5e77565e29711ccbcd6df264165564b3d41111266dbc4ac9f95d4046e64ca705`. Laufzeit-/Akkuvorteile sind nicht
vermessen; tatsächliche Tastatur, Geräte-Suche, BLE, GPS, Datei-/Ordnerprovider und
Nextcloud müssen zusätzlich auf dem Telefon geprüft werden.

## Separate Test-App

Die Test-APK ist eine optimierte Release-Version mit App-ID `de.bcycle.app.compose`
und Launchername **B-Cycle Compose**. Sie ist parallel zur Native-App installierbar
und hat eigene Einstellungen/Dateien. Einstellungen können über die bestehende
Backup-Funktion übertragen werden; Passwörter werden wie bisher nicht exportiert.
Die Native-APK und der Hauptbranch werden durch diesen Branch nicht ersetzt.
