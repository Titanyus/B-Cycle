# B-Cycle

Minimaler experimenteller Radcomputer für Android 13+ (API 33).
Live-Anzeige von Geschwindigkeit in km/h und **Fahrerleistung** in Watt.
Kotlin mit Android-Plattform-Views; keine UI-, Netzwerk-, Analyse- oder BLE-Bibliotheken.
Keine Internetberechtigung, GPS-Aufzeichnung oder Hintergrunddienste.
Diagnosedaten bleiben im Arbeitsspeicher, bis ausdrücklich eine Textdatei exportiert wird.
Cycplus-Herzfrequenz kommt in einem späteren Schritt.

## APK herunterladen

[Installierbare Test-APK 0.1.1 für Android 13+ herunterladen](https://github.com/Titanyus/B-Cycle/raw/refs/heads/main/downloads/B-Cycle-0.1.1-test.apk).
Die Datei liegt auch im Ordner `downloads`. Falls GitHub eine Dateiseite öffnet, dort
„Download raw file“ auswählen. Bei einem privaten Repository vorher bei GitHub anmelden.

Die APK ist 67.063 Byte groß, optimiert und mit demselben lokalen Testschlüssel wie
Version 0.1.0 signiert. Sie kann als Update installiert werden.
SHA-256: `ba89696211a286588b9499ffb883543cf963babb358d069063237d024027596b`.
Build, 14 Tests (zehn Decoder-Tests und vier Diagnose-Tests), Android-Lint und
APK-Signatur wurden geprüft.

## Testablauf

1. Test-APK installieren. Fahrrad mit Bosch Flow verbinden und entsperren.
2. B-Cycle öffnen, „Fahrrad suchen“ antippen und Bluetooth-Zugriff erlauben.
3. Fahrrad aus der Liste auswählen. Die Liste enthält bereits verbundene bzw. gekoppelte
   BLE-Geräte sowie Ergebnisse einer zehnsekündigen Suche, auch Geräte anderer Hersteller.
4. Falls Android eine Kopplungsbestätigung zeigt, diese bestätigen.
5. Status verfolgen: GATT-Verbindung, Kopplung, MTU, Bosch-Dienst, Notification-Abonnement,
   initiales Lesen, Live-Daten. Fehlende oder ungültige Werte werden nicht als Null angezeigt.
6. Geschwindigkeit und Fahrerleistung mit einer Fahrradanzeige vergleichen.
   Auch Stillstand, Treten, Aussetzen des Tretens, „Stoppen“ und Wiederverbindung prüfen.

Zielhardware: Xiaomi Poco F7 Ultra, Bosch Smart System mit System Controller und Mini Remote
ohne Display, laut Nutzer aktuelle Firmware. Beim ersten Hardwaretest mit 0.1.0 wurde
„Live verbunden · MTU 247“ erreicht, aber beim Drehen des Rads erschienen keine Messwerte.
Dieser Status bedeutet im Programm, dass der LDI-Dienst gefunden, das Abonnement bestätigt
und das initiale Paket gelesen und akzeptiert wurde. Welche Felder darin enthalten waren
und ob Notifications ankamen, ist noch ungeklärt. Version 0.1.1 ergänzt hierfür einen
Diagnoseexport; eine Behebung des Problems am Fahrrad ist noch nicht bestätigt.
Flow bleibt für das Entsperren zuständig; B-Cycle hat keine Entsperrfunktion.

## Diagnose bei fehlenden Messwerten

1. Version 0.1.1 als Update installieren, Fahrrad mit Flow entsperren und in B-Cycle verbinden.
2. Nach „Live verbunden“ etwa 20–30 Sekunden mit drehendem Rad testen; für Fahrerleistung
   muss getreten werden. Das Smartphone dabei sicher bedienen, nicht während der Fahrt.
3. Danach „Diagnose speichern“ antippen und im Android-Dateidialog beispielsweise
   „Downloads“ als Speicherort wählen. Beim Öffnen dieses Dialogs stoppt die BLE-Verbindung;
   der Diagnosebericht wird unmittelbar vorher festgehalten.
4. Die gespeicherte Datei `B-Cycle-0.1.1-diagnose.txt` für die Fehlersuche hochladen.
   Vor dem Export keine neue Suche starten: Diese setzt die Diagnose zurück.

Der Bericht enthält App-Version, Smartphone-Modell, Android-Version, Sitzungszeit,
GATT-Ablauf mit Statuscodes, Dienst- und Characteristic-UUIDs, Paketzähler, Decoderfehler,
die letzten vier initialen Leseantworten sowie die letzten 24 LDI-Notifications als Hexdaten.
Geschwindigkeit und Fahrerleistung werden als empfangene Felder gezählt, auch bei Nullwerten.
Gerätenamen und Bluetooth-Adressen werden nicht protokolliert. Aus anderen Characteristics
werden keine Nutzdaten gelesen oder gespeichert. Die Historie ist begrenzt, wird beim Beenden
der App verworfen und wird nicht automatisch hochgeladen. Der Export benötigt keine zusätzliche
Speicherberechtigung.

Die App arbeitet nur im Vordergrund und hält das Display während des Tests an.
Beim Verlassen des Vordergrunds oder einer Trennung werden beide Messwerte zurückgesetzt.
Wiederverbindung erfolgt manuell über die Suche bzw. erneute Geräteauswahl.

## Schnittstelle und Grenzen

Grundlage: `20260501_LiveDataInterface_V1_28042026.pdf`, Bosch Live Data Interface 1.0,
01.05.2026. Die PDF wurde ausgewertet und wird hier nicht weiterveröffentlicht.

Die PDF verlangt eine ungewöhnliche Rollenverteilung: Fahrrad als GAP Central / GATT Server,
Zubehör als GAP Peripheral / GATT Client. Smartphone-Unterstützung ist ausdrücklich außerhalb
ihres Geltungsbereichs (S. 7). „Zubehör“ bezeichnet ein BLE-Gerät, keine andere App im Flow-Menü.
Flow auf demselben Smartphone kann das eigene BLE-Advertising nicht wie einen externen Sensor entdecken.
Der Zubehör-Koppelweg für den System Controller ohne Display ist in der PDF nicht beschrieben.

**Diese erste APK ist bewusst ein Diagnosetest über Androids normalen BLE-Clientweg.**
Sie versucht, einen vorhandenen Flow-Link zu nutzen oder eine direkte BLE-Verbindung zum
gewählten Fahrrad aufzubauen. Ob LDI auf diesem Link erreichbar ist, muss am Fahrrad
geprüft werden. Dies ist keine vollständige Implementierung des vorgeschriebenen GAP-Profils.
Eine funktionierende Flow-Verbindung garantiert nicht die Freigabe des separaten LDI-Dienstes.

Der GATT-Datenablauf folgt der PDF:

- Service `0000eb20-eaa2-11e9-81b4-2a2ae2dbcce4`.
- Live-Data-Characteristic `0000eb21-eaa2-11e9-81b4-2a2ae2dbcce4`, Read und Notify.
- Android-Bonding, MTU-Anfrage 247 und Prüfung auf mindestens 247 vor dem Datenzugriff.
- Dienst- und Characteristic-Suche pro Verbindung; CCCD `0x2902` aktivieren, Ausgangswerte lesen.
- Protobuf-Feld 1: Geschwindigkeit in 1/100 km/h; Feld 5: Fahrerleistung in W.
- Unbekannte Felder überspringen, Teilupdates zusammenführen, explizite Nullwerte erhalten,
  beschädigte Pakete vollständig verwerfen. Keine Motor- oder Konfigurationsbefehle.

Laut Appendix ist die LDI-Unterstützung ab Bedieneinheit-v19 zunächst experimentell.
Android steuert die Link-Layer-Datenlänge (DLE) und Verschlüsselung; DLE >= 251 und die genaue
LE-Secure-Connections-Sicherheitsstufe sind hier nicht verifizierbar. Bei einer MTU unter 247
wird abgebrochen, um möglicherweise abgeschnittene Daten zu vermeiden.

Das Protokoll meldet den Ausfall einzelner Datenquellen nicht zuverlässig (S. 14).
Ein fehlendes Feld bedeutet in Notifications „unverändert“. Das angezeigte Paketalter
beweist deshalb nicht die Aktualität jeder einzelnen Messgröße.

## Bauen

Vollständiges JDK 17 oder 21 mit `javac`, Android-SDK Plattform 35 und Build-Tools 35.0.0.
`ANDROID_HOME` auf das SDK setzen oder eine ignorierte `local.properties` mit `sdk.dir` anlegen.

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
./gradlew :app:assembleRelease
```

Debug-APK: `app/build/outputs/apk/debug/app-debug.apk`.
Release-APK: `app/build/outputs/apk/release/app-release-unsigned.apk`, mit R8 optimiert,
noch nicht signiert. Die bereitgestellte `B-Cycle-0.1.1-test.apk` ist eine optimierte APK
mit dem lokalen Android-Testschlüssel, nicht mit einem Veröffentlichungsschlüssel.

In der eingerichteten Cloud-Umgebung:

```sh
export JAVA_HOME=/workspace/.tooling/jdk-21.0.8+9
export ANDROID_HOME=/workspace/.android-sdk
export ANDROID_USER_HOME=/workspace/.android-user
export GRADLE_USER_HOME=/workspace/.gradle-home
cd /workspace/B-Cycle
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:assembleRelease
```

Die JVM-Tests nutzen Googles Protobuf-Encoder als unabhängige Quelle für Testpakete;
diese Bibliothek wird nicht in die App eingebunden. Build, Decoder-Tests und Lint ersetzen
keinen Test am Fahrrad oder auf dem Smartphone.
