# B|Cycle

Schlanker Radcomputer für **Android 13+**: Bosch-E-Bike-Daten, GPS,
BLE-Pulsmesser, Fahrradkarte und FIT-Aufzeichnung. Native Kotlin-Views,
kompakte Dashboards und frei belegbare Datenfelder.

## APK herunterladen

**[APK 0.7.2 herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/v0.7.2/downloads/B-Cycle-0.7.2-test.apk)**

- Größe: **951,293 Byte**, rund **951 KB**.
- Optimierte Release-APK, mit dem bisherigen Android-Testschlüssel signiert;
  als Update installierbar. Vorherige APKs bleiben unter [downloads](downloads) erhalten.
- Bei einer GitHub-Dateiseite „Download raw file“ auswählen;
  bei einem privaten Repository vorher anmelden.
- [SHA-256-Prüfsumme](downloads/B-Cycle-0.7.2-test.apk.sha256):
  `d50d6ea3b89f3213ed7d89014870f12c2f6054e10cf8c7a15328b90a88e6dea7`.

Für diese kleine Layoutkorrektur: **15 gezielte Kartentests bestanden** und
Release-Build erfolgreich; Signatur und Version geprüft. Die vollständige Suite
mit 239 Tests wurde beim vorigen Stand 0.7.1 erfolgreich ausgeführt.
Release-Lint-Vital erfolgreich; kein neuer vollständiger Android-Lint-Lauf.
Es bleibt die bekannte Compilerwarnung zum veralteten Android-`onBackPressed()`.
Die Testergebnisse beziehen sich auf den bereitgestellten APK-Arbeitsstand.

## Gesicherte Stände

[v0.6.21](https://github.com/Titanyus/B-Cycle/tree/v0.6.21) sichert den vollständigen
Meilenstein vor der Bereinigung: Quellcode, Tests, Ressourcen, Build-Konfiguration,
Dokumentation und signierte APK. [v0.7.0](https://github.com/Titanyus/B-Cycle/tree/v0.7.0)
sichert zusätzlich den bereinigten Stand. [v0.7.1](https://github.com/Titanyus/B-Cycle/tree/v0.7.1)
sichert den aktuellen vollständigen Stand. Auch v0.6.20 und ältere Versionen bleiben erhalten.

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
   Vier Dashboard-Seiten verwenden sechs feste Layout-Vorlagen.
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
