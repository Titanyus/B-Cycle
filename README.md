# B-Cycle

Schlanker Radcomputer für Android 13+ (API 33), mit nativen Kotlin-Views,
Android-Bluetooth-LE und GPS. Keine UI-, Netzwerk-, Analyse- oder BLE-Bibliotheken
im APK, keine Internetberechtigung. Sieben frei belegbare Datenfelder,
Bosch-MCSP, Standard-BLE-Pulsmesser und lokale FIT-Fahrtaufzeichnung.

## APK herunterladen

[Installierbare APK 0.4.0 herunterladen](https://github.com/Titanyus/B-Cycle/raw/refs/heads/main/downloads/B-Cycle-0.4.0-test.apk).

Die Datei liegt auch im Ordner `downloads`. Bei einer GitHub-Dateiseite
„Download raw file“ auswählen; bei einem privaten Repository vorher anmelden.
Die optimierte APK ist mit demselben lokalen Testschlüssel wie die bisherigen
Versionen signiert und kann als Update installiert werden. Das neue weiße Logo
auf Rot unterstützt adaptive und monochrome Android-Icons.

APK-Größe: 226.990 Byte.
SHA-256: `d19d8782bf6afc2e11a2544a0e577eee2d43f93a2bf1f6e7ff34f3b676d3bb62`.

Build und APK-Signatur wurden geprüft; alle 69 Tests sind bestanden.
Android-Lint meldet 13 Warnungen und keine Fehler.
Dashboard, GPS und Bosch-Daten wurden in den früheren Versionen vom Nutzer am Rad
bestätigt. Die neuen Hintergrund- und Speicherabläufe werden mit Robolectric auf
Android 13/15, die FIT-Dateien mit dem offiziellen Garmin-SDK geprüft. Ein echter
Cycplus-Test sowie eine Fahrt mit ausgeschaltetem Display auf dem Poco stehen aus.

## Benutzung

1. Als Update installieren, Fahrrad mit Bosch Flow entsperren. **Flow darf weiterlaufen.**
2. **☰ → Sensoren verwalten → Sensor hinzufügen** öffnet die Auswahl für Bosch
   E-Bike oder BLE-Pulsmesser. „Fahrrad auswählen“ steht zusätzlich direkt im Menü.
   Bluetooth-Zugriff erlauben und Sensor wählen. Aktivierten Pulsmesser tragen,
   damit er sendet; er muss den Standard-Heart-Rate-Service unterstützen.
3. Rad und Pulsmesser werden lokal gespeichert und verbinden sich automatisch
   beim Öffnen und während gestarteter Fahrten. Der Sensormanager zeigt ihren
   Zustand und ermöglicht Verbinden/Trennen; dort wird auch GPS ein-/ausgeschaltet.
4. Ein Datenfeld antippen: zunächst sind **GPS**, **E-Bike** und **Pulsmesser**
   eingeklappt. Kategorie öffnen und einen Wert wählen. Alle sieben Felder sind
   frei belegbar. Fahrerleistung (eigene Watt) und Motorleistung bleiben getrennt.
5. **☰ → Aufzeichnung einstellen** bestimmt die zu speichernden Werte und getrennt
   die FIT-Hauptquellen für Geschwindigkeit und Distanz (jeweils GPS oder E-Bike).
   Die andere Quelle bleibt als zusätzliches FIT-Developer-Feld erhalten, wenn sie
   angehakt ist. GPS-Route/Position ist ebenfalls abwählbar. Die Auswahl gilt für
   die nächste Fahrt und ist unabhängig von den angezeigten Dashboard-Feldern.
6. **START** setzt die Fahrtzähler zurück und beginnt die Aufzeichnung. Für GPS
   präzisen Standort erlauben, für die Aufzeichnungsanzeige Benachrichtigungen.
   Die Fahrt läuft mit Aufzeichnungsbenachrichtigung und CPU-Wake-Lock auch bei
   ausgeschaltetem Display oder beim Wechsel in andere Apps weiter.
7. **STOPP · SPEICHERN** beendet die Fahrt und legt automatisch eine `.fit` unter
   **Download/B-Cycle/** ab. Auch die Benachrichtigung hat eine STOPP-Aktion.
   Der Aufzeichnungsdienst bleibt bis zum Abschluss des Exports aktiv.
8. Bei fehlgeschlagenem Speichern bleiben private Zwischendaten erhalten.
   **☰ → Ungespeicherte Fahrten speichern** versucht den Export erneut; das gilt
   auch für vollständige Messpunkte einer unterbrochenen Fahrt nach Prozessende.

Die Messung läuft außerhalb gestarteter Fahrten bei sichtbarer App und endet
beim Verlassen. Während einer Fahrt teilen sich Activity und Aufzeichnungsdienst
Sensorverbindungen; es entsteht kein zweiter Bosch-GATT-Client. Keine Suche nach
einer Smartphone-App im Flow-Zubehörmenü ist erforderlich. „Werte erneut lesen“
wurde aus der Bedienoberfläche entfernt. WebDAV folgt später.

Das Dashboard verwendet Schwarz, Weiß, dünne graue Linien, große schmale Ziffern
und eine rote Bedienleiste. Hochformat: zwei Felder, großes Feld, zweimal zwei
Felder. Querformat: großes Feld links, sechs Felder rechts. Belegung, Sensoren und
Aufzeichnungsauswahl werden lokal gespeichert. Die Ziffernschrift ist
[Bebas Neue, SIL OFL](docs/fonts.md). Details: [FIT-Aufzeichnung](docs/fit.md).

## Angezeigte Daten

| Bereich | Werte |
| --- | --- |
| GPS · Fahrt | Geschwindigkeit, gefahrene Distanz, Bewegungs- und Gesamtdurchschnitt, Maximaltempo, Fahrtdauer, Bewegungszeit, Standzeit |
| GPS · Höhe | Höhe, Höhenzugewinn, Höhenverlust, Steigung |
| Fahrt | Geschwindigkeit, Fahrerleistung, Kadenz, Unterstützungsmodus, Motorleistung, Fahrer- und Motordrehmoment |
| E-Bike · Strecke | Gefahrene Distanz aus Kilometerstand-Differenzen, Gesamtkilometerstand |
| Fahrrad & Akku | Akkustand, Akkutemperatur, verbleibende Energie, insgesamt vom Akku abgegebene Energie, Reichweite je Modus |
| Pulsmesser | Puls, Durchschnittspuls, Maximalpuls, Akkustand (wenn unterstützt) |
| Bosch-Aktivität | Durchschnitt und Maximum der Fahrerleistung und Kadenz, Durchschnittsgeschwindigkeit, Fahreranteil an der Antriebsenergie |

Die App abonniert diese belegten MCSP-Felder und fordert Ausgangswerte an.
GPS-Felder sind eigene Quellen und beeinflussen den Bosch-Datenzugriff nicht.
Geschwindigkeit und gefahrene Distanz sind ausdrücklich aus **beiden Quellen**
auswählbar. Die E-Bike-Fahrdistanz beginnt beim ersten empfangenen Kilometerstand;
der Gesamtkilometerstand wird durch START nicht verändert.
GPS-Positionen werden auf Genauigkeit, Alter und Sprünge geprüft. Geschwindigkeit
wird leicht, Höhe stärker geglättet; Höhenmeter haben eine Änderungsschwelle,
Steigung ein Streckenfenster. Details und Grenzen: [GPS-Fahrtwerte](docs/gps.md).
Namen der Unterstützungsmodi
werden vom Fahrrad gelesen; die App setzt keine feste ECO/TOUR/eMTB/TURBO-Liste voraus.
Bei fehlenden Namen wird die Stufe angezeigt. Reichweiten werden nur bei passender
Listenlänge mit Modusnamen beschriftet, sonst mit Stufennummern.

**Die Verfügbarkeit hängt vom Fahrrad und seiner laufenden Sitzung ab.** Noch nicht
empfangene Werte erscheinen als „—“, empfangene Nullwerte als 0. Teilupdates erhalten
vorherige Werte; fehlende Pakete werden nicht als Null interpretiert. Das gemeinsame
Paketalter beweist nicht die Aktualität jedes einzelnen Werts. Insbesondere Kadenz
kann laut öffentlichen Beobachtungen je nach Verbindungszeitpunkt fehlen.
Aktivitätswerte stammen vom Bosch-System und aktualisieren sich teilweise selten;
sie sind keine von B-Cycle aufgezeichnete Tour. Akkuenergie gesamt ist ein kumulativer
Akkuwert, kein Energieverbrauch dieser Fahrt.

Ab 0.2.0 nutzt die Bosch-Anbindung ausschließlich den erprobten MCSP-Kanal. Der PDF/LDI-Vergleich, dessen
Decoder und die Diagnosezähler auf der Hauptseite sind entfernt. Eine begrenzte
Diagnose bleibt für die Überprüfung zusätzlicher Daten verfügbar. Der Test im Stand
mit frei drehendem Rad lieferte 0 W; bei echter Fahrt bestätigte der Nutzer auch
Fahrerleistung. Dieses Verhalten begründet keinen zusätzlichen Startbefehl.

Zielhardware: Xiaomi Poco F7 Ultra, Android 16, Bosch Smart System mit System
Controller und Mini Remote ohne Display, Bedieneinheit-Firmware **v20.9.0**.
Flow bleibt für das Entsperren zuständig; B-Cycle verändert keine Fahrradeinstellungen.
Protokoll, Einheiten, Quellen und Grenzen: [Bosch-MCSP-Datenzugriff](docs/bosch-mcsp.md).

## Diagnose

„Diagnose speichern“ exportiert nach eigener Auswahl eine Textdatei. Der Bericht
enthält App-/Android-Version, Sitzungszeit, GATT-Ablauf, Anfrage- und Antwortstatus,
Paketzähler, Decoderfehler und Statistiken der bekannten numerischen Felder.
Die letzten 24 numerischen Messwerte sowie unabhängig davon die letzten 24 Wattwerte
bleiben erhalten. Gesamte MCSP-Rohpakete, Gerätenamen, Bluetooth-Adressen,
Modusnamen und Komponentenkennungen werden nicht protokolliert.
GPS ergänzt Annahme-/Verwerfungszähler und die zusammengefassten Fahrtwerte,
ohne Koordinaten oder Positionshistorie.

Die Historie ist begrenzt, bleibt bis zum Export im Arbeitsspeicher und wird nicht
automatisch hochgeladen. Ein neuer Verbindungsversuch startet einen neuen Bericht.
Für den Export ist keine zusätzliche Speicherberechtigung nötig.
Die gespeicherte Geräteauswahl liegt ausschließlich in privaten App-Einstellungen;
Android-Backup und Geräteübertragung dieser Einstellungen sind deaktiviert.

## Bauen

Vollständiges JDK 17 oder 21, Android-SDK Plattform 35 und Build-Tools 35.0.0.
`ANDROID_HOME` auf das SDK setzen oder eine ignorierte `local.properties` mit
`sdk.dir` anlegen.

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:assembleRelease
```

Debug-APK: `app/build/outputs/apk/debug/app-debug.apk`.
Release-APK: `app/build/outputs/apk/release/app-release-unsigned.apk`.
Die bereitgestellte APK ist eine optimierte Release-APK mit lokalem Android-Testschlüssel.

In der eingerichteten Cloud-Umgebung:

```sh
export JAVA_HOME=/workspace/.tooling/jdk-21.0.8+9
export ANDROID_HOME=/workspace/.android-sdk
export ANDROID_USER_HOME=/workspace/.android-user
export GRADLE_USER_HOME=/workspace/.gradle-home
cd /workspace/B-Cycle
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:assembleRelease
```

Die Tests prüfen MCSP-Frames, Fragmentierung, den Startdialog, zusätzliche Feldtypen,
Skalierungen, unsigned Kilometerstände, gepackte Reichweiten, Modusnamen, Teilupdates,
begrenzte Diagnosehistorien, serielle Anfragen und automatische Verbindungsversuche.
Zusätzlich prüfen sie sieben frei belegbare Felder, gespeicherte Auswahl, Quellgruppen,
native Android-Schrift und Größen, Standortberechtigung und GPS-Abmeldung,
Stillstand mit Positions-/Höhenrauschen, Sprünge, Geschwindigkeitsspitzen,
Empfangslücken, Bewegung ohne gelieferten GPS-Speed, Durchschnittswerte,
Steigung sowie getrennten Auf-/Abstieg. Googles Protobuf-Encoder und Robolectric
sowie der offizielle Garmin-FIT-Decoder dienen ausschließlich Tests und werden nicht in die App eingebunden.
Weitere Tests prüfen BLE-Pulsdaten, CCCD-Freigabe und veraltete GATT-Callbacks,
Aufzeichnung ohne sichtbare Activity, STOPP, Download-Veröffentlichung, feste
Speicherfeld-Auswahl und Wiederholung nach einem Speicherfehler.
Robolectric-SDKs werden durch Gradle bezogen und im Test über den Offline-Resolver
geladen, damit die Proxy-Konfiguration auch in der Cloud gilt.
Tests ersetzen keinen Hardwaretest.
