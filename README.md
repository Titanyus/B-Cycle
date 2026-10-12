# B|Cycle

Schlanker Radcomputer für **Android 13+**: Bosch-E-Bike-Daten, GPS,
BLE-Pulsmesser, Fahrradkarte und FIT-Aufzeichnung. Native Kotlin-Views,
kompakte Dashboards und frei belegbare Datenfelder.

## APK herunterladen

**[APK 0.7.6 herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/v0.7.6/downloads/B-Cycle-0.7.6-test.apk)**

- Größe: **1,021,457 Byte**, rund **1.02 MB**.
- Optimierte Release-APK, mit dem bisherigen Android-Testschlüssel signiert;
  als Update installierbar. Vorherige APKs bleiben unter [downloads](downloads) erhalten.
- Bei einer GitHub-Dateiseite „Download raw file“ auswählen;
  bei einem privaten Repository vorher anmelden.
- [SHA-256-Prüfsumme](downloads/B-Cycle-0.7.6-test.apk.sha256):
  `7c0d6939f4b1b195fc11e08062cdad36619cd2e8faf0c696c92b6b88510382a6`.

Vollständige Suite mit **280 Tests bestanden** und Release-Build erfolgreich in **2 Minuten**.
Nach der anschließenden APK-Versionierung bestanden **19 gezielte Profil-/Zonen-/FIT-Tests**;
Release-Build erfolgreich in **53 Sekunden**. Signatur und Version 0.7.6/44 separat geprüft.
Release-Lint-Vital erfolgreich; kein neuer vollständiger Android-Lint-Lauf.
Es bleibt die bekannte Compilerwarnung zum veralteten Android-`onBackPressed()`.

## Entwicklungsstand nach APK 0.7.6

- **Daten & Speicher** als siebte Kategorie im vorhandenen Einstellungsraster.
- Speicherort und Cloud-/WebDAV-Konfiguration aus dem Streckenmanager hierher verschoben;
  streckenbezogene Detailaktionen und gespeicherte Zugänge bleiben erhalten.
- Manuelle, selektive JSON-Sicherung/Wiederherstellung für sechs Einstellungsbereiche,
  mit Prüfung, Bestätigung und Rollback. Keine Streckendateien, Kopplungen oder Geheimnisse im Backup.
- Vollständige Suite mit **294 Tests** und Release-Build erfolgreich.

Diese Änderungen sind noch nicht in der oben angebotenen APK 0.7.6 enthalten.
Architektur, Backupformat, Quellen, Merge-Regeln, Ausschlüsse und Prüfungen:
[Daten & Speicher](docs/data-storage-backup.md).

## Neu in 0.7.6

- Streckenliste mit vier kompakten Spalten: Datum/Uhrzeit, Distanz, Bewegungszeit und Ø Tempo.
  Vorschau-Speichersymbol und kurzer Verarbeitungshinweis entfernt; Titelhöhe vereinheitlicht.
- **Fahrerprofil** im bislang freien sechsten Einstellungsplatz, mit `user-round`.
  Sechs optionale persönliche Angaben, lokale Speicherung und sofortige Vorschau der
  fünf Karvonen-Herzfrequenzzonen. Persönliche FTP bleibt von Estimated FTP getrennt.
- Keine Cloud-Synchronisierung persönlicher Daten, keine neuen FIT-Felder oder Trainingsanalysen.

Diese Änderungen sind in APK 0.7.6 enthalten.
Architektur, Rundung, Speicherung und Tests: [Fahrerprofil](docs/rider-profile.md),
[Streckenlisten-Detailkorrekturen](docs/route-manager-step2.md), [Buildnachweis 0.7.6](docs/milestone-076.md).

## Gesicherte Stände

[v0.6.21](https://github.com/Titanyus/B-Cycle/tree/v0.6.21) sichert den vollständigen
Meilenstein vor der Bereinigung: Quellcode, Tests, Ressourcen, Build-Konfiguration,
Dokumentation und signierte APK. [v0.7.0](https://github.com/Titanyus/B-Cycle/tree/v0.7.0)
sichert zusätzlich den bereinigten Stand. [v0.7.1](https://github.com/Titanyus/B-Cycle/tree/v0.7.1)
sichert den aktuellen vollständigen Stand. Auch v0.6.20 und ältere Versionen bleiben erhalten.

## Neu in 0.7.5

- Fünf feste Aktionen im Streckendetail-Footer: Karte, Umbenennen, Cloud-Upload,
  lokal speichern und Löschen. Bestätigte Upload-/Speichererfolge bleiben erhalten;
  Fehlversuche und Abbrüche erzeugen keine Erfolgsanzeige.
- Originale Lucide-Icons `cloud-check` und `save-check`, ohne neue Icon-Bibliothek.
- Kompakte Streckenliste ohne Drei-Punkte-, Upload- oder Speicherbuttons.
  Das Auge rechts neben dem Titel blendet den Track auf der Dashboard-Karte ein/aus;
  die übrige Zeile öffnet die Details.
- Distanz, Bewegungszeit und Bewegungsdurchschnitt nebeneinander, mit schmalen
  Messwerten und kleinen, mittig ausgerichteten Einheiten im Dashboard-Grau.
  Fehlende oder widersprüchliche Daten werden nicht durch erfundene Werte ersetzt.
- Filter, Import, Herkunft, lokaler Originaldateistatus und sämtliche Verwaltungsfunktionen bleiben erhalten.

Details: [Detailaktionen](docs/route-detail-actions.md),
[Streckenliste Schritt 2](docs/route-manager-step2.md), [Buildnachweis](docs/milestone-075.md).

## Neu in 0.7.4

- **Estimated FTP:** beste zusammenhängende, zeitgewichtete 20-Minuten-Leistung
  × 0,95. Nur Fahrerleistung; Nullwerte zählen, Lücken und Pausen verbinden keine
  getrennten Abschnitte. Eine Schätzung dieser Fahrt, kein persönlicher FTP-Nachweis.
- Optionales, standardmäßig ausgeschaltetes FIT-Session-Developer-Field;
  Detailberechnung unabhängig von der Exportauswahl.
- Details im flächigen Dashboard-Stil mit vertikalen Trennlinien, Standardheader
  und Datum darunter. Einheiten mittig neben den Zahlen im identischen Dashboard-Grau.
- Neue **E-Bike**-Gruppe mit mittlerer Motorleistung, Rider/Bike Ratio und Akkuverbrauch
  bei geeigneten Daten. **Power** zeigt Average, Max und NP gemeinsam.
- Vollbildkarte erhält die tatsächliche Bildschirmfläche, native Pan-/Zoom-Gesten
  und Buttons zum Zoomen und erneuten Einpassen der Strecke.

Details: [Estimated FTP und Details-Update](docs/estimated-ftp.md),
[Buildnachweis 0.7.4](docs/milestone-074.md).

## Neu in 0.7.3

- **Streckendetails:** gesamten Streckeneintrag antippen. Kennzahlen, Streckenkarte
  und fünf kompakte Diagramme in einer Vollbildansicht. Zeit, Tempo, Höhenmeter,
  Kadenz und Puls lassen sich innerhalb ihrer Kachel umschalten, sofern verfügbar.
- Gemeinsame Distanzachse (Zeit als Fallback), tatsächliche Sensorwerte mit Lücken;
  Fahrerleistung bleibt von Motorleistung getrennt. FIT-Dateien bleiben unverändert.
- Alle neuen Texte in zentralen Ressourcen; Darstellung folgt dem aktiven Theme.
- Dashboard-Kartenquellenhinweis um 90° gedreht links unten, weiterhin antippbar
  und dauerhaft sichtbar. Auf kurzen Kartenflächen wird der Hinweis umgebrochen.
- Bisherige Streckenmanager-Aktionen und Filter bleiben erhalten.

Details: [Streckendetails V1](docs/route-analysis-v1.md),
[Kartenlayout](docs/map-bottom-layout.md), [Buildnachweis](docs/milestone-073.md).

## Neu in 0.7.2

Die drei unteren Kartenwerte sitzen tiefer; Minus ist mit ihrer Unterkante ausgerichtet,
Plus bleibt darüber. Die dynamische Layoutregel überschreibt den Abstand nicht mehr.
Kartenquellenangabe und Werte bleiben getrennt.
Details: [unteres Kartenlayout](docs/map-bottom-layout.md).

## Neu in 0.7.1

- Manuelle Tages-/Nachtkarte mit Mond-/Sonnenbutton direkt unter Vollbild;
  Auswahl bleibt gespeichert. Dashboard-Datenfelder bleiben unverändert.
- Einstellungsansichten im Vollbild mit festem Header und Footer.
- Zentrale fünfte Theme-Farbrolle ACCENT; BLACK OPS mit schwarzer Dialogfläche
  und grauen Eingabefeldern, kompatibel mit bisherigen Themes.
- Aufnahmezeit folgt dem START/STOPP-Zeitraum und bleibt nach STOPP stehen.
- Performanceberechnung berücksichtigt bestätigte 0-Watt-Ausrollphasen auch
  bei längerer Sensorpause, solange frische E-Bike-Geschwindigkeit vorliegt.
- Akkuwarn-Glow: vier Sekunden Breathing bei ≤20 % orange, bei ≤10 % rot,
  auch bei ausgeschaltetem Fahrmodus-Glow.

Details: [Nachtkarte](docs/map-night.md), [Vollbild-UI](docs/fullscreen-ui.md),
[Accent](docs/color-engine-v2.md), [Aufnahmezeit](docs/recording-clock.md),
[Performance](docs/performance-energy-0617.md), [Glow](docs/mode-glow.md)
und [gesicherter Build](docs/milestone-071.md).

## Neu in 0.7.0

Kleine Altlastenbereinigung: zentrale Versionsquelle für Diagnose, Karten-User-Agent
und FIT-Metadaten; zwei unbenutzte alte Icon-Renderer entfernt. Frühere Einstellungsicons
bleiben erhalten. Keine neuen Bibliotheken oder Änderungen an den Sensorfunktionen.
Details: [Bereinigung 0.7.0](docs/cleanup-070.md) und [gesicherter Build](docs/milestone-070.md).

## Neu in 0.6.21

- Kartenwerte und Record-Punkt rund 30 % größer, Lichtanzeige 50 % größer.
- Große Dashboard-Felder kompakter, mehr Platz für die Karte; kleine Felder unverändert.
- GPX-Linie dunkelblau, 6 dp breit, jetzt mit 50 % Deckkraft.
- Kartenbuttons und untere Bedienleiste verschwinden nach fünf Sekunden nur während
  erkannter Bewegung und erscheinen bei Stillstand oder fehlender frischer Geschwindigkeit.
- Durchschnitts-Chevrons erkennen auch die langsame Änderung nach längeren Fahrten.
  Nach längerer App-Pause werden etwa acht Sekunden neue Werte für den Trend benötigt.
- Lichtstatus inzwischen am echten Fahrrad bestätigt.

## Neu in 0.6.20

- Kartenicons: `locate-fixed` zum Zentrieren, `compass` für die Ausrichtung und
  `maximize` für Vollbild. Geschlossene Augen verwenden überall `eye-closed`,
  offene Augen weiterhin `eye`. Bedienung, Größen und Touchflächen bleiben bestehen.

- **Wetter → Sonne & Tageslicht**: Sonnenaufgang, Sonnenuntergang,
  Zeit bis Sonnenuntergang und verbleibendes Tageslicht.
  Vollständig offline aus gültiger GPS-Position und Datum berechnet;
  Sonnenzeiten in der Zeitzone des Handys, Restzeiten als `HH:mm`.
  Ohne frisches GPS keine Werte; nach Sonnenuntergang Restzeit `00:00`.
  Berechnung nur beim ersten Fix, Tages-/Zeitzonenwechsel, größerer Ortsänderung
  oder nach einer Stunde. Sunrise-/Sunset-Icons im Dashboard.
- **Fahrradlicht** unter E-Bike → Licht: An/Aus-Wert zur freien Feldbelegung
  und zur optionalen FIT-Speicherung. Eine rote Glühbirne rechts neben dem
  mittleren Kartenwert signalisiert eingeschaltetes Licht; links steht während
  der Aufzeichnung der rote Record-Punkt. Beide funktionieren auch ohne belegtes Feld.
- Der Streckenmanager-Shortcut unten verwendet dasselbe `route`-Icon wie das Einstellungsmenü.

**Der Lichtstatus wurde inzwischen am Fahrrad bestätigt.** Unsere erprobte Verbindung
nutzt MCSP und fragt den optionalen Booleanwert unter `0x181C` ab. Das Enum-Feld
`bike_light = 17` der Bosch-PDF gehört zur anderen LDI-Schnittstelle und wird nicht
mit der MCSP-Kodierung vermischt. Unbekannt ist kein „Aus“; bei fehlendem oder
veraltetem Status bleibt die Glühbirne verborgen. FIT speichert den Lichtstatus
als standardkonformes Developer Field, 0 = aus, 1 = an.

Sonnenzeiten sind Näherungen ohne örtliche Berge oder Hindernisse.
Details: [Offline-Sonnenwerte](docs/solar-weather.md) und
[Bosch-MCSP](docs/bosch-mcsp.md).

## Benutzung

1. APK als Update installieren und das Fahrrad mit Bosch Flow entsperren.
   **Flow darf weiterlaufen.** B|Cycle benötigt keine Registrierung als App im Flow-Zubehörmenü.
2. **☰ → Sensoren & Aufzeichnung → Sensor hinzufügen**: Bosch-E-Bike oder
   Standard-BLE-Pulsmesser auswählen, Bluetooth-Zugriff erlauben.
   Aktivierten Pulsmesser tragen, damit er sendet.
   Die Geräteauswahl wird lokal gespeichert; aktivierte Sensoren verbinden sich automatisch.
   GPS lässt sich im selben Manager aktivieren; dafür präzisen Standort erlauben.
3. **Im Stand ein Datenfeld antippen** und im festen Dropdown GPS, E-Bike,
   Pulsmesser oder Wetter auswählen. Darunter stehen die Werte in Gruppen.
   Während der Bewegung bleibt ein Klick ohne Wirkung und ohne Hinweis.
   Fahrerleistung und Motorleistung sind getrennte Felder.
4. **☰ → Design & UI**: Farbpalette, Dashboard-Vorlagen, Belegungen der drei
   transparenten Kartenfelder und optionalen Fahrmodus-Glow einstellen.
   Vier Dashboard-Seiten verwenden acht feste Layout-Vorlagen. Die neuen Vorlagen
   zeigen ein großes Feld über zwei Reihen mit je drei beziehungsweise zwei Feldern.
   Alle Feldbezeichnungen verwenden dieselbe feste Größe (13,5 sp) und den Grauton der Einheiten.
   Die Menüoberfläche hat zwei Zeilen mit je drei Plätzen; das `activity`-Icon
   ist derzeit ein Dummy ohne Funktion.
5. **☰ → Sensoren & Aufzeichnung → FIT-Aufzeichnung** aufklappen:
   Speicherfelder und die FIT-Hauptquellen für Geschwindigkeit und Distanz wählen.
   GPS und E-Bike können beide gespeichert werden; die zweite Quelle wird ein
   zusätzliches Developer Field. Anzeige und Aufzeichnungsauswahl sind unabhängig.
   Sonnenwerte dienen derzeit nur der Anzeige.
6. **START** setzt die Fahrtzähler zurück und beginnt die Aufzeichnung.
   Eine dauerhafte Benachrichtigung zeigt die aktive Fahrt.
   Die Aufzeichnung läuft auch bei ausgeschaltetem Display oder in anderen Apps weiter.
7. **STOPP** beendet die Fahrt und speichert die FIT-Datei in der privaten
   Streckensammlung. **Streckenmanager → ⋮** konfiguriert zusätzlich die lokale
   Ordnerspeicherung und Nextcloud-WebDAV; beide sind unabhängig schaltbar.
   Cloud-Upload ist für selbst aufgezeichnete Fahrten vorgesehen, nicht für manuell importierte Tracks.
8. **Streckenmanager** zeigt eigene Fahrten und importierte GPX-/FIT-Strecken:
   Vorschau, Datum, Distanz und Bewegungszeit, Auge zur Kartenanzeige sowie ein
   Dropdown mit Streckenaktionen. Wolke und Speichersymbol zeigen den Ausgabestatus.
   Bei einem Speicherfehler sind vollständige Zwischendaten unter
   **Erweitert → Ungespeicherte Fahrten speichern** erneut exportierbar.

## Dashboard und Karte

- Schwarzes Dashboard mit großen schmalen Ziffern, einheitlichen Feldgrößen,
  kurzen Bezeichnungen und Einheiten rechts vom Wert.
- Sieben Farbpaletten; einheitliche Dialoggeometrie unabhängig vom Theme.
  START, Streckenmanager-Shortcut und Menü übernehmen die gewählten Farben.
- Durchschnittskadenz ohne Tretpausen; die Aufzeichnung erhält weiterhin die
  tatsächlichen Sensorwerte einschließlich Nullwerten. Durchschnittsfelder
  besitzen einen Richtungsindikator.
- Mapsforge-Karte mit CyclOSM-Online-Kacheln und importierbaren Offline-Karten.
  Online-Kacheln benötigen Internet.
- Importierte Strecke: dunkelblau, **6 dp**, **50 % Deckkraft**.
  Aktuelle Position mit Höhenangabe bei verfügbarem GPS-Höhenwert;
  bei genordeter, nachgeführter Karte liegt die Position auf zwei Dritteln der Höhe.
- Das Auge links oben blendet die gewählte Strecke ein/aus und behält die Auswahl.
  Ohne frühere Auswahl wird die letzte eigene Fahrt mit Positionsdaten verwendet.
- Zentrieren, Norden und Vollbild oben rechts; Zoom +/− unten rechts.
  Kartenbuttons und untere Bedienleiste blenden sich gemeinsam nach fünf Sekunden aus;
  ein Kartentipp zeigt sie wieder.
- Drei globale Kartenfelder: schwarze Werte ohne Titel auf transparentem Hintergrund,
  frei belegbar unter **Design & UI**; leere Felder bleiben unsichtbar.

## Daten und Aufzeichnung

| Quelle | Verfügbare Werte |
| --- | --- |
| GPS · Fahrt | Tempo, Distanz, Bewegungs-/Gesamtdurchschnitt, Maximaltempo, Fahrtdauer, Bewegungszeit, Standzeit |
| GPS · Höhe | Höhe, Aufstieg, Abstieg, Steigung |
| E-Bike | Tempo, Distanz, Gesamtkilometer, Fahrer-/Motorleistung, Kadenz, Drehmomente, Unterstützungsmodus, Fahrradlicht |
| E-Bike · Akku | Akkustand, Temperatur, verbleibende und abgegebene Energie, Reichweiten |
| E-Bike · Statistik | Leistungs-/Kadenzdurchschnitt und Spitzenwerte, Durchschnittstempo, Fahreranteil |
| Pulsmesser | Puls, Durchschnitt, Maximum, Sensorakku, soweit unterstützt |
| Wetter | Sonnenaufgang, Sonnenuntergang, Zeit bis Sonnenuntergang, verbleibendes Tageslicht |

FIT unterstützt die ausgewählten GPS-, E-Bike- und Pulswerte sowie optionale
Fahrtkennzahlen: Normalized Power, mechanische Arbeit, geschätzte Kalorien,
Speed-to-Power Ratio und Efficiency Factor. Native Garmin-Felder werden verwendet,
wo ihre Bedeutung passt; zusätzliche Werte erhalten Developer Fields.
Garmins offizieller Decoder und Activity Validator prüfen die erzeugten Dateien.

GPS wird auf Genauigkeit, Alter und Sprünge geprüft. Höhenmeter und Steigung sind
gefilterte Näherungen. Fehlende Werte erscheinen als „—“ und werden nicht als Null
interpretiert. Die Verfügbarkeit von Bosch-Feldern hängt vom Rad und seiner Sitzung ab.
Bosch-Aktivitätswerte können eigene Zeitgrenzen haben; die Durchschnittskadenzanzeige
wird dagegen ohne Tretpausen berechnet. Namen der Unterstützungsmodi stammen vom Rad.

## Technik und Tests

Kotlin, native Android-Views, eine Activity mit getrennten Sensor-/Datenkomponenten.
Android 13+ (`minSdk 33`), Zielplattform Android 15 (`targetSdk 35`).
Kein Compose, keine zusätzlichen BLE-/GPS-Bibliotheken.
Mapsforge, Garmin FIT SDK und OkHttp werden für Karte, FIT und WebDAV genutzt.
JUnit, Robolectric und die zusätzlichen Prüfwerkzeuge bleiben außerhalb der APK.

Der APK-Build wurde mit 211 Tests geprüft: MCSP-Frames und schreibgeschützte Requests,
BLE-Puls, GPS-Filter und Durchschnittswerte, Speicherabläufe und Rücknavigation,
Themes und native UI-Darstellung, Performance-/Energiekennzahlen, Garmin-FIT,
Offline-Sonnenzeiten sowie Lichtstatus und Kartenoverlay. Tests ersetzen keinen
Hardwaretest. Bosch-Livedaten und Pulsmesser wurden in früheren Versionen am Rad
bestätigt; inzwischen auch die Lichtfunktion.

Zum Kompilieren des jeweiligen Quellcode-Standes: JDK 17 oder 21,
Android-SDK Plattform 35 und Build-Tools 35.0.0; `ANDROID_HOME` auf das SDK setzen.

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:assembleRelease
```

Debug-APK: `app/build/outputs/apk/debug/app-debug.apk`.
Release-APK: `app/build/outputs/apk/release/app-release-unsigned.apk`;
vor Installation signieren.

## Dokumentation und Lizenzen

- [Bosch-MCSP: Protokoll, Feldmapping und Quellen](docs/bosch-mcsp.md)
- [GPS: Filterung und Grenzen](docs/gps.md)
- [Sonnenwerte: Berechnung und Grenzen](docs/solar-weather.md)
- [Karten und Offline-Dateien](docs/maps.md)
- [FIT-Aufzeichnung](docs/fit.md)
- [Streckenverwaltung und WebDAV](docs/routes.md)
- [Bebas Neue: SIL Open Font License](docs/fonts.md)

Lizenztexte der verwendeten Bibliotheken und Icons sind in der App unter
**Erweitert → Bibliotheken & Lizenzen** bzw. **Schrift & Lizenz** zugänglich.
B|Cycle verändert keine Fahrradeinstellungen. Eine Diagnose wird nur nach
expliziter Auswahl exportiert; keine automatische Analyse oder Telemetrie.
