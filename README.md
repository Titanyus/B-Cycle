<p align="center">
  <img src="docs/assets/app-icon.png" width="128" height="128" alt="B|CYCLE App-Icon">
</p>

<h1 align="center">B|CYCLE</h1>

<p align="center">
  Dein Fahrradcomputer für Android – Live-Daten, Karten und Trainingsanalyse.
</p>

<p align="center">
  <a href="#apk-herunterladen">APK herunterladen</a> ·
  <a href="#key-features">Key Features</a> ·
  <a href="#erste-schritte">Erste Schritte</a> ·
  <a href="#dokumentation-und-lizenzen">Dokumentation</a>
</p>

<table>
  <tr>
    <td width="33%" align="center"><a href="docs/assets/showcase-dashboard.png"><img src="docs/assets/showcase-dashboard.png" width="280" alt="Dashboard mit großen Live-Werten und Kartenbedienung"></a></td>
    <td width="33%" align="center"><a href="docs/previews/bikes-sections.png"><img src="docs/previews/bikes-sections.png" width="280" alt="Bikes: vier Fahrradprofile, Bildauswahl und Radtyp"></a></td>
    <td width="33%" align="center"><a href="docs/previews/track-distributions.png"><img src="docs/previews/track-distributions.png" width="280" alt="Fahrtauswertung mit Leistungs- und Pulsverteilung in den persönlichen Trainingszonen"></a></td>
  </tr>
  <tr>
    <td align="center"><strong>Dein Dashboard</strong></td>
    <td align="center"><strong>Deine Bikes</strong></td>
    <td align="center"><strong>Deine Auswertung</strong></td>
  </tr>
</table>

Die Vorschauen zeigen native App-Ansichten mit Testdaten; Kartenkacheln sind in der Dashboard-Vorschau nicht geladen.

## Key Features

- **Individuelle Dashboards** – Bis zu sechs benannte Seiten, zehn Layout-Vorlagen und frei belegbare Datenfelder mit großen, gut ablesbaren Zahlen.
- **Live-Daten vom Rad** – Bosch-E-Bike, BLE-/FTMS-Indoor-Trainer, GPS und BLE-Pulsmesser; Fahrerleistung und Motorleistung bleiben getrennt.
- **Karten unterwegs** – Online- und Offline-Karten, Tages-/Nachtansicht, GPX-Strecken und Höhenanzeige an der aktuellen Position.
- **FIT-Aufzeichnung** – Gewünschte Messwerte und Hauptquellen selbst auswählen; Aufzeichnung auch bei ausgeschaltetem Bildschirm.
- **Fahrten im Detail** – Karte, fünf Verlaufsdiagramme, Bewegungszeit, Höhenmeter und Performance-Kennzahlen wie Normalized Power und Estimated FTP bei geeigneten Daten.
- **Trainingszonen** – Persönliche Leistungs- und Pulszonen, Zonenanteile, Verteilungskurven und bis zu vier Best Efforts mit Watt und W/kg.
- **Live-Graphen und Ambient-Glow** – Daten und Hintergrundgraphen je Kachel kombinieren, Trainingshilfe je Dashboard wählen; Fahrmodus und Akkuwarnung am Bildschirmrand erkennen.
- **Fahrradprofile** – Bis zu sechs Räder mit eigenen Namen, Fotos, Radkategorie, Radumfang, Sensorzuordnungen, FIT-Einstellungen sowie eigenen Themes, Dashboards und Kartenoptionen. E-Bike und Indoor sind nutzbar; normales Bike ist vorbereitet.
- **Dein Design** – 14 vorgegebene Farbthemen mit einheitlicher Bedienung, sofort gespeicherten Einstellungen und Hochformat-Layouts.
- **Export und Upload** – Lokal speichern oder eigene Fahrten an WebDAV, intervals.icu, Dreeve und Strava senden. Dienste arbeiten unabhängig; Dreeve benötigt eine eigene Instanz, Strava derzeit eine persönliche API-App.
- **Wetter und Höhe** – Wetter zur Tourmitte dauerhaft speichern und bei Bedarf GPS-Höhen nachträglich mit Mapterhorn korrigieren; Original-FIT bleibt erhalten.
- **Schlanke Android-App** – Native Kotlin-Views, Android 13+, rund 1,7 MB optimierte APK. Keine automatische Telemetrie.

## APK herunterladen

[![Android Optimized APK](https://github.com/Titanyus/B-Cycle/actions/workflows/android-debug.yml/badge.svg)](https://github.com/Titanyus/B-Cycle/actions/workflows/android-debug.yml)

**[Aktuelle APK-Builds in GitHub Actions](https://github.com/Titanyus/B-Cycle/actions/workflows/android-debug.yml)**

Bei jedem Push auf `main` entsteht automatisch eine optimierte Release-APK. Im erfolgreichen Lauf
unter **Artifacts → B-Cycle-release-…** das ZIP herunterladen und die APK entpacken.
GitHub-Anmeldung für den Artefakt-Download erforderlich; Artefakte bleiben 30 Tage erhalten.
Manuell: **Actions → Android Optimized APK → Run workflow**.

Android 13+ (minSdk 33), Compile-/Target-SDK 36. Die Build-Zusammenfassung nennt
Version, Commit, Signierung, Download und SHA-256. Ein fester Testschlüssel ist für
Update-Kompatibilität nötig; [einmalige Einrichtung und Build-Anleitung](docs/github-actions.md).
Ältere signierte Test-APKs bleiben unter [downloads](downloads) erhalten.

**[Version 0.8.1 – neue optimierte APK herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/v0.8.1/downloads/B-Cycle-0.8.1.apk)**

**Build 35 vom 18.10.2026**, rund **1,8 MB**, mit bisherigem Testschlüssel signiert und als Update installierbar.
Optimierter GitHub-Actions-Release mit R8 und Ressourcenverkleinerung.
Die zugehörigen gezielten Tests einschließlich Garmin-FIT-Validierung sind bestanden.
[Änderungen und SHA-256](docs/build-35.md) ·
[GitHub-Release](https://github.com/Titanyus/B-Cycle/releases/tag/v0.8.1) ·
[Erfolgreicher Actions-Build](https://github.com/Titanyus/B-Cycle/actions/runs/39102855713).
Der [Meilenstein 0.8](docs/milestone-0.8.md) mit vollständigem 608-Test-Lauf bleibt erhalten.
Ein physischer Gerätetest der neuen APK steht noch aus.

## Neu in APK 0.8.1

- Übersetzbare lokale Hilfeseiten über Info im Header; Umbenennen über den Streckentitel bleibt erhalten.
- Upload-Menü mit eigenem Status/Haken je Dienst und direktem Konfigurieren; neuer Streckendetail-Footer.
- Segmentierte Trainingshilfe: Aus, Alle Zonen, Zone 2.
- Indoor-Auswahl Hometrainer/Rollentrainer (Rollentrainer noch deaktiviert).
- Helle Auswahlrahmen/Flächen, lesbarer Datumsdialog, einheitliche Auswahltexte und Sensoricons.
- Bosch-Fahrmodus als Schalter-/Accordion-Zeile in Design & UI; FIT-Gerätename B|CYCLE.

## Neu in APK 0.8

- Helle Einstellungen: weiße Header-Icons; Puls-/Leistungsverteilung ohne Glow.
- Fahrerprofil: aufklappbare Sticky-Abschnitte und vollständiger Geburtsdatumsrahmen.
- Lokale Speicherung inline unter Daten & Speicher; Streckendetails mit „Norm. Leistung“.
- Optimierte APK und dauerhaftes Meilenstein-Release einschließlich R8-Mapping.

## Neu in APK 0.7.31

- **Profile:** Bikes, Karten und Design & UI zusammengefasst; fünf Accordions und direkter Dashboard-Editor.
- **Eigene Einstellungen je Rad:** Theme, Dashboards, Trainingshilfe und Karten wechseln mit dem ausgewählten Profil. Bestehende Profile starten mit den bisherigen gemeinsamen Werten.
- **Profil übernehmen:** Konfiguration aus einem anderen Profil kopieren; Name, Bild und gekoppelte Geräte des Zielprofils bleiben erhalten. Upload-Dienste bleiben global.
- **Umsortieren:** Bikes und Dashboard-Seiten mit animierter Einfügevorschau und Accent-Rahmen ziehen; Reihenfolge erst beim Loslassen speichern.

## Neu in APK 0.7.30

- **Graphen und Glow:** Kleinere Skalenbeschriftung, hellere Zonenvisualisierung. Rand-Glow oben oder seitlich unter Design & UI wählbar, einschließlich Akkuwarnung.
- **Akku:** Anzeige 10 % kleiner. Direkte Auswahl bis vier verfügbarer Akkus passend zu den verbundenen Geräten; ohne Kombischalter und separate Prozentfelder. Bosch-Akkustand bleibt während der Verbindung auch bei Sendepausen erhalten.
- **Strecken:** Best Efforts mit kürzeren Zeitfenstern links. Höhenkorrigierte Download-Kopien erzeugen beim erneuten Import keinen zweiten Eintrag mehr; bestehende Duplikate werden nicht automatisch gelöscht.

## Neu in APK 0.7.29

- **Dashboard-Vollbild:** Android-Statusleiste auf dem Dashboard standardmäßig ausgeblendet; abschaltbar unter Design & UI. Dialoge und Android-Navigation bleiben wie bisher.
- **Akkuanzeige:** Vertikale Balken und größere Darstellung je nach Platz.
- **Akkuauswahl:** Smartphone-Auswahl korrigiert, alle unterstützten Quellen angeboten. Kombianzeige über Schalter; einzelne Prozentfelder sind bei aktiver Kombination ausgegraut.
- **Benachrichtigungen:** Abschnittsüberschriften jetzt als zentrale Sticky Header. Reale Nachrichtenanzeige vom Nutzer bestätigt.

## Neu in APK 0.7.28

- **Akku-Kombifelder:** Bis vier Akkustände als kompaktes 2×2-Raster mit Sensoricon, Balken und Prozent, Warnfarben unter 20/10 %. Je Datenfeld auswählen, auch über der Karte und in Backups erhalten.
- **Smartphone:** Neue Datenkategorie mit Uhrzeit und Smartphone-Akku. [Native Vorschauen und Details](docs/battery-dashboard.md).
- **Dashboard 10:** Drei große Einzelreihen und unten eine Zweierreihe, ohne Karte; eigene Layout-Vorschau und vollständige Editor-Bedienung.
- **Wetterauswahl:** Sonnenauf-/untergang und Tageslicht auch bei Indoor auswählbar; ohne GPS keine Daten und kein zusätzlicher GPS-Start.

## Neu in APK 0.7.27

- **Benachrichtigungen:** Neue Einstellungen nach Design & UI, Android-Freigabe und separate App-Auswahl mit Messenger-Filter.
- Abgerundetes Nachrichtenfenster mit farbigem App-Icon, Absender, SECONDARY-Hintergrund und 3-dp-ACCENT-Rahmen. Drei Positionen mit Layout-Vorschauen, einstellbare 5–180 Sekunden und scrollbarer Text. Keine dauerhafte Speicherung von Nachrichten.
- **Indoor-Messwertkorrektur:** Zwei Faktoren je Bike für Leistung und Geschwindigkeit/Distanz, wirksam auf Live-Anzeige, Berechnungen und neue FIT-Aufzeichnungen. Bestehende Fahrten unverändert.

## Neu in APK 0.7.26

- **ZIP-Archivimport:** FIT/GPX einschließlich `.fit.gz` und `.gpx.gz` in einem Durchgang übernehmen, auch aus Unterordnern.
- Originaldateien und Fahrtdatum erhalten, identische Dateien überspringen; Fortschritt und Abschlussbilanz im Streckenmanager.
- **Rein lokal:** Kein automatischer Cloud-Upload. TCX und CSV-Metadaten werden nicht importiert.

## Neu in APK 0.7.25

- **Indoor-Bike über BLE/FTMS:** Tempo, Fahrerleistung, Kadenz und Distanz des
  Trainers in Dashboard und FIT; optionaler Pulsmesser, GPS bleibt aus.
- Ausschließlich Datenempfang, keine Trainersteuerung. Getrennte Gerätezuordnung,
  automatische Wiederverbindung, Frischeprüfung und Hintergrundaufnahme.
- FIT als Indoor Cycling, bestehende Performancewerte und unveränderte
  Aufnahmegrenzen. [Einrichtung und Prüfgrenzen](docs/indoor-ftms.md).

- Bis zu sechs Bikes, drei Kacheln nebeneinander mit horizontalem Scrollen.
  „Neues Rad“, bestätigtes Löschen und Verschieben per langem Drücken/Ziehen.
- Vorhandene Profile bleiben erhalten; Fotos und Sensorzuordnungen bleiben an
  stabilen Profilkennungen. Indoor bietet ausschließlich Trainer und optionalen Pulsmesser.
- Header-Abstände in Streckenliste und Details wie bei Bikes; Wettergruppen
  zentriert und durch mittige Punkte getrennt, Himmel und Temperatur ohne inneren Punkt.
  Kennzahlenbereiche in Streckendetails wieder mit kompakten 2-dp-Abständen zu den Trennern;
  Streckenlisten-Trenner ebenfalls mit 55 % Deckkraft.
- Indoor-Vorschau: [native App-Ansicht](docs/previews/bikes-indoor.png).
- [Alle Änderungen und Buildnachweis](docs/milestone-0725.md).

## Neu in APK 0.7.24

- Zentrale Seiten-/Kategorieabstände, schattenfreie Sticky Header und dünne
  0,75-dp-Linien; Icon-Text-Buttons und Sensor-Icons vereinheitlicht.
- Eigenes Fahrradbild pro Bike, zweizeilige Profilnamen, quadratische Karte und
  größere Datums-/Wetterangaben in Streckendetails.
- [UI-Konventionen und Bildspeicherung](docs/ui-content-spacing.md).

- Dreeve- und Strava-FIT-Uploads unter Daten & Speicher → Streckendaten, mit
  getrennten Jobs, bestätigten Dienst-IDs und dauerhaftem Verarbeitungsstatus.
- Strava benötigt eine persönliche registrierte API-App, Dreeve eine eigene
  Instanz mit aktiviertem Dateiimport/API-Schlüssel. Zugangsdaten nur in der App.
- [Einrichtung und Grenzen](docs/cloud-activity-uploads.md). Echte Uploads und
  der Strava-Anmelde-Roundtrip sind auf dem Gerät noch zu prüfen.
- BLE-Anzeige zählt unterschiedliche empfangene Messwertarten pro Verbindung.
- Offene Dialoge und Eingaben bleiben beim normalen App-Wechsel erhalten.

## Erste Schritte

1. APK installieren und unter **Einstellungen → Bikes** dein E-Bike oder Indoor-Bike einrichten. Das Bosch-Rad vorher mit Flow entsperren; beim Trainer andere verbundene Trainings-Apps trennen.
2. Gewünschte Sensoren und bei Outdoor-Betrieb GPS aktivieren und über **Sensor suchen / verbinden** zuordnen. Standort- und Bluetooth-Berechtigungen bei Bedarf erlauben.
3. Unter **Design & UI** Theme und Dashboard-Layout wählen. Ein Datenfeld lange drücken, um den Editor zu öffnen; Daten, Graph und Visualisierung getrennt einstellen.
4. In **Bikes → FIT-Aufzeichnung** die Hauptquellen für Geschwindigkeit und Distanz sowie zusätzliche Speicherfelder auswählen. Die Anzeige ist von der FIT-Auswahl unabhängig.
5. Mit **START** die Aufnahme beginnen, mit **STOPP** beenden. Fahrtzähler starten erst mit der Aufnahme und bleiben danach stehen.
6. Unter **Strecken** eine Fahrt für Details antippen; das Auge blendet sie auf der Dashboard-Karte ein. Exportziele unter **Daten & Speicher → Streckendaten** konfigurieren.

## Technik und Tests

Kotlin und native Android-Views, eine Activity mit getrennten Sensor- und Datenkomponenten.
Android 13+ (`minSdk 33`), `compileSdk` und `targetSdk 36`.
Mapsforge, Garmin FIT SDK und OkHttp übernehmen Karten, FIT-Dateien und Netzwerkzugriffe.

Für Meilenstein 0.8 wurden alle 608 Tests der vollständigen JVM-/Robolectric-Suite erfolgreich ausgeführt.
APK-Builds laufen zentral über GitHub Actions; lokale Builds sind nur bei konkretem Bedarf vorgesehen.
[Build-Anleitung und Signierung](docs/github-actions.md).
Tests ersetzen keinen Hardwaretest. Der Matrix-Empfang wurde vom Nutzer bestätigt; Korrekturfaktoren, Wiederverbindung
und längere Aufnahmen bleiben am Gerät zu prüfen. Die reale Benachrichtigungsfunktion wurde vom Nutzer bestätigt.
Neue Dreeve-/Strava-Uploads und der Strava-Anmeldevorgang müssen ebenfalls noch
mit echten Nutzerkonten geprüft werden.

Build mit JDK 21, Android-SDK Plattform 36 und Build-Tools 35.0.0;
`ANDROID_HOME` auf das SDK setzen:

```sh
./gradlew :app:testDebugUnitTest :app:assembleRelease
```

Die Release-Datei liegt unter `app/build/outputs/apk/release/app-release-unsigned.apk`
und muss vor der Installation signiert werden.

## Dokumentation und Lizenzen

- [Dreeve und Strava: Einrichtung und Grenzen](docs/cloud-activity-uploads.md)
- [Indoor-Trainer über FTMS](docs/indoor-ftms.md)
- [Fahrradprofile](docs/bicycle-profiles.md)
- [Accordiontest – Designkonventionen der Profilseite](docs/accordiontest.md)
- [Dashboard-Layouts](docs/dashboard-presets.md)
- [Bosch-MCSP: Protokoll, Feldmapping und Quellen](docs/bosch-mcsp.md)
- [Matrix ICR50: getestetes BLE-/FTMS-Protokoll](docs/ble-trainer-matrix-icr50.md)
- [Compile-/Target-SDK-Modernisierung](docs/sdk-modernization.md)
- [GPS: Filterung und Grenzen](docs/gps.md)
- [Sonnenwerte: Berechnung und Grenzen](docs/solar-weather.md)
- [Karten und Offline-Dateien](docs/maps.md)
- [FIT-Aufzeichnung](docs/fit.md)
- [Streckenverwaltung und WebDAV](docs/routes.md)
- [ZIP-Archivimport](docs/route-archive-import.md) – FIT/GPX inklusive GZIP, lokale Duplikaterkennung, seit APK 0.7.26.
- [intervals.icu einrichten](docs/intervals-icu-plan.md)
- [Wetterdaten und dauerhafte Speicherung](docs/ride-weather.md)
- [Bebas Neue: SIL Open Font License](docs/fonts.md)

Lizenztexte der verwendeten Bibliotheken und Icons sind in der App unter
**Erweitert → Bibliotheken & Lizenzen** bzw. **Schrift & Lizenz** zugänglich.
B|Cycle verändert keine Fahrradeinstellungen. Eine Diagnose wird nur nach
expliziter Auswahl exportiert; keine automatische Analyse oder Telemetrie.

## Versionshistorie

<details>
<summary>Frühere Versionen und gesicherte Meilensteine</summary>

## Neu in APK 0.7.23

- **intervals.icu-FIT-Upload** unter Daten & Speicher → Streckendaten, unabhängig von WebDAV.
- **Wetter zur Tourmitte** in den Streckendetails; erfolgreich geladene Daten bleiben dauerhaft offline verfügbar.
- Quellen und Dienste zentral unter Erweitert. intervals.icu-Upload vom Nutzer auf dem Gerät bestätigt; echte Wetterabrufe in der Entwicklungsumgebung weiterhin nicht geprüft.
- Streckennamen über Titel/Stift ändern; Höhe korrigieren ersetzt Umbenennen im Footer.
- Gemeinsamer Header-Schatten: 12 dp, maximal 40 % Deckkraft; Bestätigungsdialoge mit 8 dp Mindestabstand zum Beschreibungstext.
- Plus/Minus im Dashboard-Editor gedimmt; redundante GPS-Aktivierung in Karten und Kategorie-Beschriftung in Bikes entfernt.
- Neues App-Icon, WebP-Einstellungsbild und Geburtsdatum im Stil der übrigen Datenfelder.

[intervals.icu einrichten](docs/intervals-icu-plan.md) · [Wetterdaten und Speicherung](docs/ride-weather.md)

## Neu in APK 0.7.22

- Originales BICYCLE-Motiv unter den Haupt-Einstellungen, mit Version und genauer Build-Zeit.
- Höhenmeterzählung zählt bestätigte Rampen vollständig; keine abgeschnittenen Restmeter an jeder Kuppe.
- Optionale Mapterhorn-Höhenkorrektur nach Aufnahme unter Karten; Original-FIT bleibt erhalten.
- Manuelle Korrektur eigener FIT-Fahrten in den Streckendetails, konsistente Höhen-/Aufstiegs-/Abstiegswerte.
- Einheitliche Accordion-Typografie, größere Graph-Auswahlpunkte und dezente Header-Schatten.
- OpenTopoMap sowie OpenCycleMap mit eigenem Thunderforest-Schlüssel.

[Einstellungen mit Motiv und Build-Anzeige](docs/previews/settings-brand.png) ·
[Höhenkorrektur und Prüfgrenzen](docs/elevation-gain.md) ·
[Kartenquellen](docs/map-providers.md)

## Neu in APK 0.7.21

- Sticky-Kategorien auf Fahrer, Bikes, Daten & Speicher und Design & UI.
- Design & UI: Theme, Dashboard-Layout, Fahrmodus-Anzeige.
- Kompakte einzeilige Zahlenfelder in 28-sp-Bebas-Neue mit dezentem Rahmen;
  WebDAV-Felder mit gleichem Rahmen und Systemschrift.
- FIT-Aufzeichnung als fester Abschnitt, zusätzliche Datenquellen einzeln aufklappbar.
- Streckendetails: geglättete, deckende Diagrammflächen ohne zusätzliche Konturlinie.

## Neu in APK 0.7.20

- Sticky-Kategorieheader mit dauerhaftem 12-dp-Abstand nach oben.
- Suchbutton und andere erhöhte Buttons scrollen unter dem Header hindurch.
- GPS, E-Bike, Powermeter und Pulsmesser: Schalterbeschriftung in 23 sp / 900 Black.
- [Aktuelle Sensoren-Vorschau](docs/previews/bikes-sticky-sensors.png).

## Neu in APK 0.7.19

- **Sticky-Kategorien als Versuch auf Bikes:** Radtyp und Sensoren, bestehende
  Schrift zentriert, TERTIARY-Hintergrund, oben/unten 1 px FOREGROUND und großzügige
  Abstände. Die nächste Kategorie schiebt den angedockten Header heraus.
- **Auswahlrahmen in ACCENT** für Bike-, Dashboard- und Theme-Kacheln.
- Aktive Kreisfüllungen und Schalter in ACCENT; Beschriftung/Kreiskontur FOREGROUND.
- FIT-Aufzeichnung wieder als zunächst geschlossenes Accordion unter den Bike-Sensoren.
- Electric Blue und Acid Lime im Stil von Embers/Fire: schwarzer Dialoghintergrund,
  graue Eingabefelder, bisherige Primärfarbe auch als ACCENT.
- Flächenfüllung der fünf Strecken-Verlaufsdiagramme auf 100 % Deckkraft.

[Bikes-Vorschau](docs/previews/bikes-sections.png) ·
[Angedockter Kategorieheader](docs/previews/bikes-sticky.png) ·
[Technik und Umfang](docs/sticky-sections.md)

## Neu in APK 0.7.18

- **EMBERS:** roter Header und Akzent #BF281B, schwarzer Dialoghintergrund
  #000000, dunkelgraue Eingabeflächen #292929, weiße Texte/Icons #FFFFFF.
- **FIRE:** dieselbe Struktur mit gelbem Header und Akzent #FFB400.
- Bestehende Theme-Auswahlen und sämtliche Funktionen bleiben erhalten.

## Neu in APK 0.7.17

- Verlaufsdiagramme: dezente, bildschirmabhängige Glättung, weiche Übergänge ohne
  Überschwingen und kräftigere Flächenfüllung in den fünf bestehenden Diagrammfarben.
  Echte Nullwerte und Datenlücken bleiben erhalten.
- Verteilungskurven: Zonenflächen vollständig deckend, ohne Farbverlauf.
  Nur die weiße Kurvenfüllung behält ihren transparenten Verlauf.
- Haupt-Einstellungen: Fahrer, Bikes, Strecken, Karten, Design & UI,
  Daten & Speicher, Erweitert. Menüeinträge und Seitentitel heißen Karten/Strecken.
- EMBERS ersetzt BLACK OPS: rote Eingabeflächen und Akzent #BF281B.
- FIRE ersetzt SOLAR FLARE: grauer Header, schwarzer Dialoghintergrund,
  gelbe Eingabeflächen und Akzent #FFB400, weiße Texte/Icons.
  Die bestätigte Kontrastausnahme gilt ausschließlich für die gelbe Preset-Fläche;
  bestehende Theme-Auswahlen bleiben durch stabile IDs erhalten.

## Neu in APK 0.7.16

- **Bikes:** vier lokale Fahrradprofile mit antippbarem Namen, Kategorie-Dropdown,
  Radumfang, zentraler Sensorsuche und FIT-Hauptquellen auf einer Seite. Zunächst
  nur E-Bike aktiv; Bike/Indoor vorbereitet. Bestehende Zuordnungen in Bike 1 übernommen.
- **Aufnahmegrenzen:** App-Fahrtzeit und Fahrtstatistiken starten erst mit START
  und bleiben nach STOPP stehen; Live-Sensoranzeige bleibt möglich.
- **Streckendetails:** eigene Kategorie Höhe mit separatem Aufstieg und Abstieg.
- **Best Efforts:** bis zu vier längste auswertbare Leistungsfenster aus
  5 s, 1 min, 5 min, 8 min, 20 min, 1 h, 2 h; große Wattwerte und kleinere W/kg.
- **Zonenbalken:** Streckenanteile in sieben Leistungs- und fünf Pulszonen,
  bekannte Profilfarben, blasse volle Hintergrundspur und kräftige Anteilsfüllung.
- **Verteilungskurven:** darunter zwei quadratische, zeitgewichtete Diagramme für
  Leistung und Puls, weiße geglättete Kurven mit Dashboard-Glow und Verlaufsfüllung.

[Bikes-Vorschau](docs/previews/bikes-top.png) ·
[Sensorbereich](docs/previews/bikes-sensors.png) ·
[FIT-Bereich](docs/previews/bikes-fit.png) ·
[Neue Diagramme mit Testdaten](docs/previews/track-distributions.png)

## Neu in APK 0.7.15

- Gemeinsame Scrollregeln und beschriftete Aktionsfooter für Streckenmanager und Details.
- Drei zentrale Dashboard-Kachelstile mit erhaltenen Größen und Schriftregeln.
- Einheiten und sekundäre Kennzahlentexte in Weiß mit 65 % Deckkraft.
- Zentrale Kartensteuerungsmaße, kleine Schriftrollen, UI-Texte und Bedienbeschreibungen.
- Gemeinsame Toast-/Statusdarstellung; keine Änderung an Sensoren, FIT oder Performanceberechnung.
- Korrekte Zentrierung des frei positionierten Editor-Beenden-Buttons ohne Formularspacing.

Die geplante Benachrichtigungsfunktion ist [vorbereitet](docs/notifications-preparation.md),
aber noch nicht Teil dieser APK. Ihr Ziel ist eine längere, ausführlichere Anzeige
ohne dauerhafte Speicherung von Nachrichten.

## Neu in APK 0.7.14

- Haupt-Einstellungen: 23 sp / 900 Black, 32-dp-Icons und 72-dp-Zeilenhöhe.
- Daten & Speicher und Kartenmanager: Schalter für aktivierbare Optionen;
  verbliebene Checkboxen als Kreis mit gefüllter Mitte, weiterhin Mehrfachauswahl.
- Fahrerprofil: sieben persönliche Coggan-Leistungszonen. Dashboard-Kategorie
  „Leistung“ mit Leistungszone, HF-Zone, FTP-Auslastung und HF-Reserve.
- Feldauswahl: Accordions „Graph“ und „Visualisierung“, unabhängiger Zonen-Glow
  je Kachel, aktuelle 3-Sekunden-Mittel oder vorhandene Fahrt-Durchschnitte.
  Weiche Zonenfarben und 1,5-Sekunden-Übergänge, kompatible Einstellungssicherung.
- Streckendetails: größere Datumszeile, seitliche Gruppenbezeichnungen und feine
  Trenner; Überschriften über Karte und Diagrammen entfallen.
- Diagramme: auf fünf Einheiten aufgerundete Obergrenzen, maximal drei runde
  Zwischenmarken; Puls und Höhe ab gemessenem Minimum. Diagramm „Leistung“.
- Native Kotlin-Views im Hauptbranch; das Compose-Experiment bleibt separat.
  Sensorverarbeitung, FIT-Felder und Performance-Berechnung bleiben erhalten.

## Neu in APK 0.7.13

- Haupt-Einstellungen: Menüschrift 28 sp und führende Menü-Icons 48 dp, passend zu den hohen Zeilen.
- Abschnittsüberschriften appweit 23 sp, wie die Streckendetails. Bisherige System-Fettschriften verwenden zentral Gewicht 900 Black (Roboto/Systemfont).
- Graph-Darstellung direkt im Datenauswahl-Dialog der Kachel; unabhängig pro Seite/Kachel gespeichert. Die bisherige globale Auswahl dient als Ausgangswert.
- Sonne/Mond links oben direkt neben dem Track-Auge; rechts entfällt die zweite Buttonzeile.
- Vier gezielte Tests und Release-Kotlin-Kompilierung bestanden. Alle genannten Änderungen sind in APK 0.7.13 enthalten.

## Neu in APK 0.7.12

- **Dashboard-Feldauswahl:** Daten, Graph und Visualisierung sind unabhängig wählbar; „Keine Daten“ blendet Wert/Einheit/Beschriftung aus und erhält die Feld-ID.
- Graph-Farbverlauf um 80 % verstärkt; bei überlagerten Daten Hauptlinie 30 % und Farbverlauf 15 % dunkler.
- Doppelte Leuchtsaum-Breiten: 11,2/19,2 dp, Hauptlinie unverändert 2,6 dp.
- [Einheitliche Navigation](docs/ui-navigation.md): Zurück-Chevron in jedem Header,
  Titel zur Fensterbreite zentriert, gültige Einstellungen automatisch gespeichert.
- Fette Abschnittsüberschriften appweit; leere Footer entfallen vollständig.
- Haupt-Einstellungen als hohe Icon-Zeilen, Erweitert als separater Screen.
- Umschalt-Icons zeigen den aktuellen Zustand, einschließlich sun/moon und compass/arrow-up-from-dot.
- Kartenwerte direkt über der unteren Bedienleiste, Zoom darüber; Datumskopf deutlicher.
- [Details zur Graph-Darstellung](docs/live-graph-overlays.md).

## Neu in APK 0.7.11

- [Weiche Kartenbewegung](docs/map-motion.md): 450-ms-Übergänge mit gemeinsamer Position
  und Drehung; nur bei Bewegung und sichtbarer GPS-Verfolgung.
- [Dashboard-Editor V2](docs/dashboard-editor.md): Long-Press oder „Design & UI → Dashboard
  bearbeiten“, gemeinsame Bearbeitung der oberen Kacheln und drei Kartenfelder,
  gestrichelte PRIMARY-Rahmen und „EDITOR BEENDEN“ mittig auf der Karte.
- Oberfläche auf Hochformat festgelegt; Layoutprüfungen verwenden Hochformat-Größen.
- Abschluss für Editor/Kartenanimation: **354 Tests und Release-Build bestanden** (1 min 57 s).
- Kleinere Graph-Anfangsbereiche und Schritte: Tempo 0–20/5 km/h, Kadenz 0–60/10 U/min,
  Leistung 0–120/30 W, Puls 50–100/20 bpm, Höhe ungefähr ±25/25 m; zwei Skalierungstests bestanden.
- Graph-Overlay-Auswahl mit passenden Farbpunkten wie im Dashboard.
- Quellen-Dropdown mit den bestehenden GPS-/Fahrradsymbolen, Herz für Pulsmesser
  und Wolke mit Sonne für Wetter; Icons verwenden die Theme-Vordergrundfarbe.
- Alle genannten Änderungen sind in APK 0.7.11 enthalten.

## Neu in APK 0.7.10

- Direktwert-Chevrons zeigen denselben Trend wie ihre jeweiligen Durchschnittsfelder;
  GPS-Tempo folgt dem Durchschnitt in Bewegung.
- Live-Graphlinien mit kräftigerem 2,6-dp-Kern, sanftem Leuchtsaum und nach unten auslaufender Füllung.
- Live-Graph-Skalenschrift um 80 % vergrößert; farbige Overlay-Punkte neben der großen Feldbezeichnung.
- Dünne Grenzlinien links/rechts laufen oben und unten transparent aus; Kurven und Lichtsaum enden an den Grenzen.
- Sieben gezielte Darstellungstests und Release-Kompilierung für diese Anpassungen bestanden.
- Glow-Helligkeit wieder auf dem vorherigen Niveau, verbreiterter Glow unverändert; zwei gezielte Pixeltests bestanden.
- Acht gezielte Tests für die zuvor umgesetzte Chevron-/Graph-Korrektur und Release-Kompilierung erfolgreich.

## Entwicklungsstand nach APK 0.7.8

- Live-Graphen für große Dashboardfelder: bis zu zwei transparente, leicht geglättete
  Kurven direkt hinter dem Messwert; Auswahl im vorhandenen Felddialog.
- Ganze Fahrtdistanz, ruhige stufenweise Skalen, Aktualisierung alle zehn Sekunden.
  Sensorwerte, FIT und Performance Engine unverändert.
- Graph-Auswahl persistent je Feld/Seite und in der Dashboard-Sicherung enthalten.
- Compile-/Target-SDK 36, weiterhin Android 13+; native Zurück-Anbindung.
- Zahlenschrift in Dreier-Reihen von 32 auf 45 sp vergrößert (rund 40 Prozent).
- UX- und Konfigurationsgrundsätze verbindlich in [AGENTS.md](AGENTS.md) hinterlegt.
- Matrix-ICR50-Briefing für eine spätere BLE-/FTMS-Integration dokumentiert.

**In APK 0.7.9 enthalten.** Der Funktionsstand wurde mit 336 Tests und Release-Build geprüft.
[Implementierung und Prüfnachweis](docs/live-graph-overlays.md).

## Neu in 0.7.8

- Dashboard 9 mit acht unabhängig belegbaren Feldern: **2 / 3 / 3**.
- Passendes Thumbnail vervollständigt die drei Reihen mit je drei Vorlagen.
- Bisherige Feldbelegungen und ältere Sicherungen mit sieben Feldern bleiben kompatibel.
- Bezeichnungen behalten die gemeinsame Größe und das Einheitengrau.
- Dreier-Kacheln mit festen 32-sp-Zahlen, gleichmäßigem Raster und kompakter Einheitenanordnung.
- Direktwert-Chevrons für Tempo, Fahrerleistung, Kadenz und Puls verwenden den zugehörigen Durchschnittstrend; Watt-Trend auch zwischen ganzzahligen Watt-Schritten.
- Alle Glow-Effekte weiterhin 25 % breiter in der Mitte; die letzte Helligkeitserhöhung ist zurückgenommen.
- FIT-Detail-Neuberechnung berücksichtigt bestätigte lange Rollphasen und verkraftet kurze echte Datenlücken bei ausreichender Abdeckung.
- Streckenliste mit zweistelligem Jahr und drei dp tieferem Kennzahlenblock.
- Fahrtdetails mit deutschen Bezeichnungen; Kadenz & Puls steht vor Leistung.

Diese Ergänzungen sind in der oben verlinkten APK 0.7.8 enthalten.
Details: [Dashboard-Vorlagen](docs/dashboard-presets.md).

## Neu in 0.7.7

- **Daten & Speicher** als siebte Kategorie im vorhandenen Einstellungsraster.
- Speicherort und Cloud-/WebDAV-Konfiguration aus dem Streckenmanager hierher verschoben;
  streckenbezogene Detailaktionen und gespeicherte Zugänge bleiben erhalten.
- Manuelle, selektive JSON-Sicherung/Wiederherstellung für sechs Einstellungsbereiche,
  mit Prüfung, Bestätigung und Rollback. Keine Streckendateien, Kopplungen oder Geheimnisse im Backup.
- Zwei zusätzliche Dashboard-Vorlagen mit Thumbnails: groß / 3 / 3 und groß / 2 / 2.
- Feldbezeichnungen auf allen acht Vorlagen gleich groß, 13,5 sp und im Grau der Einheiten.

Diese Änderungen sind in APK 0.7.7 enthalten. Dokumentation:
[Daten & Speicher](docs/data-storage-backup.md), [Dashboard-Vorlagen](docs/dashboard-presets.md),
[Buildnachweis 0.7.7](docs/milestone-077.md).

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


</details>
