# B-Cycle

Minimaler experimenteller Radcomputer für Android 13+ (API 33).
Live-Anzeige von Geschwindigkeit in km/h und **Fahrerleistung** in Watt.
Kotlin mit Android-Plattform-Views; keine UI-, Netzwerk-, Analyse- oder BLE-Bibliotheken.
Keine Internetberechtigung, Speicherung, GPS-Aufzeichnung oder Hintergrunddienste.
Cycplus-Herzfrequenz kommt in einem späteren Schritt.

## APK herunterladen

[Installierbare Test-APK 0.1.0 für Android 13+ herunterladen](https://github.com/Titanyus/B-Cycle/raw/refs/heads/main/downloads/B-Cycle-0.1.0-test.apk).
Die Datei liegt auch im Ordner `downloads`. Falls GitHub eine Dateiseite öffnet, dort
„Download raw file“ auswählen. Bei einem privaten Repository vorher bei GitHub anmelden.

Die APK ist 54.775 Byte groß, optimiert und mit einem lokalen Testschlüssel signiert.
SHA-256: `b2e18d5e82476a6f7ebfc154bb41048bd5f23d43f2fc883e128af0e934068f16`.
Build, zehn Decoder-Tests, Android-Lint und APK-Signatur wurden geprüft; der reale
Bosch-Verbindungstest steht aus.

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
ohne Display, laut Nutzer aktuelle Firmware. Der tatsächliche Datenzugriff auf dieser Hardware
ist **noch nicht getestet**. Bei Fehlern bitte den angezeigten Status und die exakte
Bedieneinheit-Firmware notieren. Android- und Geräte-Bluetooth-Einstellungen alleine bestätigen
keinen LDI-Zugriff. Flow bleibt für das Entsperren zuständig; B-Cycle hat keine Entsperrfunktion.

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
noch nicht signiert. Die bereitgestellte `B-Cycle-0.1.0-test.apk` ist eine optimierte APK
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
