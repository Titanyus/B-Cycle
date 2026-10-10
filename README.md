# B-Cycle

Schlanker Radcomputer für Android 13+ (API 33), mit nativer Kotlin-Oberfläche und
Androids Bluetooth-LE-API. Keine UI-, Netzwerk-, Analyse- oder BLE-Bibliotheken im
APK, keine Internetberechtigung, kein GPS und kein dauerhafter Hintergrunddienst.
Cycplus-Herzfrequenz folgt später.

## APK herunterladen

[Installierbare APK 0.2.0 herunterladen](https://github.com/Titanyus/B-Cycle/raw/refs/heads/main/downloads/B-Cycle-0.2.0-test.apk).

Die Datei liegt auch im Ordner `downloads`. Bei einer GitHub-Dateiseite
„Download raw file“ auswählen; bei einem privaten Repository vorher anmelden.
Die APK ist 95.735 Byte groß, optimiert und mit demselben lokalen Testschlüssel
wie die bisherigen Versionen signiert. Sie kann als Update installiert werden.
SHA-256: `7dab6b06b082e4b8b0574c4a8592b84bbccd92ad4bbe29a4378eede28d1f3b9d`.

Build, 36 JVM-Tests, Android-Lint und APK-Signatur wurden geprüft.
Die automatische Verbindung und die zusätzlichen Felder benötigen noch den
Hardwaretest dieser Version. Geschwindigkeit und Fahrerleistung über denselben
MCSP-Verbindungsweg wurden vom Nutzer bei einer echten Fahrt bestätigt.

## Benutzung

1. Als Update installieren, Fahrrad mit Bosch Flow entsperren. **Flow darf weiterlaufen.**
2. Einmal **„Fahrrad auswählen“** antippen, Bluetooth-Zugriff erlauben und das eigene
   E-Bike wählen. Die App speichert Name und Bluetooth-Adresse lokal und verbindet
   sich unmittelbar. Auch nach vollständigem Schließen bleibt die Auswahl erhalten.
3. Beim nächsten Öffnen verbindet sich B-Cycle automatisch mit diesem Rad. Bei
   Verbindungsabbruch erfolgen weitere Versuche, mit Wartezeiten bis zu 30 Sekunden.
   Bluetooth muss eingeschaltet und das Rad erreichbar und entsperrt sein.
4. **„Stoppen“** hält die automatische Verbindung für diese App-Sitzung an.
   **„Verbinden“** setzt sie fort. Mit „Fahrrad auswählen“ kann ein anderes Rad
   gespeichert werden; die bisherige Auswahl bleibt bis zur neuen Auswahl erhalten.

Die App hält das Display während einer aktiven Verbindung an. Sie beendet ihren
Datenzugriff beim Verlassen des Vordergrunds und verbindet sich bei der Rückkehr
wieder, sofern nicht manuell gestoppt wurde. Während der Geräteauswahl und des
Diagnose-Dateidialogs ist die automatische Verbindung pausiert.
Keine Suche nach einer Smartphone-App im Flow-Zubehörmenü ist erforderlich.

## Angezeigte Daten

| Bereich | Werte |
| --- | --- |
| Fahrt | Geschwindigkeit, Fahrerleistung, Kadenz, Unterstützungsmodus, Motorleistung, Fahrer- und Motordrehmoment |
| Fahrrad & Akku | Akkustand, Gesamtkilometer, Akkutemperatur, verbleibende Energie, insgesamt vom Akku abgegebene Energie, Reichweite je Modus |
| Bosch-Aktivität | Durchschnitt und Maximum der Fahrerleistung und Kadenz, Durchschnittsgeschwindigkeit, Fahreranteil an der Antriebsenergie |

Die App abonniert diese belegten MCSP-Felder und fordert Ausgangswerte an.
„Werte erneut lesen“ wiederholt die Leseanfragen. Namen der Unterstützungsmodi
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

0.2.0 nutzt ausschließlich den erprobten MCSP-Kanal. Der PDF/LDI-Vergleich, dessen
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
Googles Protobuf-Encoder dient ausschließlich als unabhängige Quelle von Testpaketen
und wird nicht in die App eingebunden. Tests ersetzen keinen Hardwaretest.
