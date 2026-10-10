# B-Cycle

Minimaler experimenteller Radcomputer für Android 13+ (API 33).
Live-Anzeige von Geschwindigkeit in km/h und **Fahrerleistung** in Watt.
Kotlin mit Android-Plattform-Views; keine UI-, Netzwerk-, Analyse- oder BLE-Bibliotheken.
Keine Internetberechtigung, GPS-Aufzeichnung oder dauerhaften Hintergrunddienste.
Diagnosedaten bleiben im Arbeitsspeicher, bis ausdrücklich eine Textdatei exportiert wird.
Cycplus-Herzfrequenz kommt in einem späteren Schritt.

## APK herunterladen

[Installierbare Test-APK 0.1.7 für Android 13+ herunterladen](https://github.com/Titanyus/B-Cycle/raw/refs/heads/main/downloads/B-Cycle-0.1.7-test.apk).

Die Datei liegt auch im Ordner `downloads`. Falls GitHub eine Dateiseite öffnet, dort
„Download raw file“ auswählen. Bei einem privaten Repository vorher bei GitHub anmelden.

Die APK ist 91.639 Byte groß, optimiert und mit demselben lokalen Testschlüssel wie
die bisherigen Versionen signiert. Sie kann als Update installiert werden.
SHA-256: `01c31397e59b8c6601b2d7839f84c9f461ea8acf1bb3a0016452260e4ff2861f`.
Build, 36 Tests (zehn LDI-Decoder-, zehn MCSP-Protokoll-, sieben Diagnose-, fünf
Schreibwarteschlangen- und vier Freigabe-Tests), Android-Lint und APK-Signatur wurden geprüft.

## Live-Test mit 0.1.7

1. APK als Update installieren und Fahrrad mit Flow entsperren. **Flow darf weiterlaufen**;
   seine Verbindung muss nicht beendet werden.
2. In B-Cycle **„Fahrrad auswählen“** antippen, Bluetooth-Zugriff erlauben und das eigene
   Fahrrad aus der Liste auswählen. Die Auswahl selbst baut keine Verbindung auf.
3. **„Live-Test starten“** antippen. Die App öffnet den Bosch-Smart-System-Datenkanal,
   abonniert Geschwindigkeit und Fahrerleistung und fordert beide Werte aktiv an.
   Eine Suche nach einer Smartphone-App im Flow-Zubehörmenü ist nicht erforderlich.
4. Etwa 30 Sekunden treten und Anzeige sowie **Empfangszähler** prüfen.
   Bei fehlenden Werten zwei- oder dreimal **„Werte erneut lesen“** antippen.
   Die Schaltfläche ist aktiv, sobald weitere Anfragen möglich sind.
5. **„Diagnose speichern“** und `B-Cycle-0.1.7-diagnose.txt` hochladen.
   Vor dem Export keine neue Suche starten, da diese die Diagnose zurücksetzt.

Die App hält das Display während des Tests an und beendet ihren Datenzugriff beim
Verlassen des Vordergrunds. Der Diagnoseexport hält den Bericht unmittelbar vor dem
Stoppen und dem Android-Dateidialog fest. Wiederverbindung erfolgt manuell.
Die Bedienung und den Test mit drehendem Rad an einem sicheren Standort durchführen.

Der Empfangszähler zählt Benachrichtigungen auf dem ausgewählten Bosch-Datenkanal,
Leseantworten und Paketfehler. „Tempo/Watt erkannt“ bezieht sich auf die aktuelle
Testsitzung. Andere Bosch-Pakete können den Empfangszähler erhöhen, ohne die beiden
Messwerte zu liefern. Transport- oder Startdialog-Pakete aktualisieren deshalb auch
nicht das angezeigte Alter des letzten Messwertpakets.

## Was 0.1.7 anders macht

Das Protokoll von 0.1.6 bestätigt einen erfolgreichen Zugriff auf den dokumentierten
LDI-Dienst: Kopplung, MTU 247, Dienstsuche und Benachrichtigungsfreigabe gelangen.
Die Leseantwort enthielt dennoch **0 Byte**, und bis zum Stoppen nach rund 7,8 Sekunden
kamen **keine Notifications** an. Das blockierende Warten aus 0.1.5 war beseitigt;
der Empfang von Messwerten blieb aus. Frühere Tests über längere Zeit ergaben ebenfalls
leere Antworten, auch mit beendeter Flow-App. Die Ursache des leeren LDI bleibt offen.

Im selben Dienstverzeichnis des Fahrrads ist ein zweiter Bosch-Kanal vorhanden:
**MCSP**, Service `00000010-eaa2-11e9-81b4-2a2ae2dbcce4`, Notify-Characteristic
`00000011-eaa2-11e9-81b4-2a2ae2dbcce4` und Anfrage-Characteristic
`00000012-eaa2-11e9-81b4-2a2ae2dbcce4`.
Öffentliche, an anderer Bosch-Hardware geprüfte Protokoll-Dokumentation beschreibt
dort Geschwindigkeit, Fahrerleistung sowie einen MobileApp-Startdialog.

0.1.7 verwendet diesen Kanal als neuen Live-Test. Es abonniert gezielt die zwei
Messwertadressen und fragt deren aktuelle Werte an. Anfragen werden nacheinander
gesendet; wenn das Rad die dokumentierten App-Startfragen stellt, beantwortet B-Cycle
sie. Die BLE-Ankündigung und der lokale Zubehör-GATT-Server der bisherigen Versuche
sind entfernt, ebenso die Advertising-Berechtigung. Bluetooth-Suche und -Verbindung
genügen. Es werden keine Einstellungen, Unterstützungsgrenzen oder Entsperrfunktionen
des Fahrrads verändert.

Laut Nutzer empfängt Locus auf demselben Rad Geschwindigkeit, Fahrerleistung,
Unterstützungsmodus und Gesamtkilometer, mit sporadischen Aussetzern insbesondere
bei der Kadenz. Locus war beim Test mit 0.1.6 nicht verbunden. **Welchen konkreten
Datenweg Locus verwendet, ist unbekannt.** Der Unterstützungsmodus steht nicht in der
offiziellen PDF, wurde öffentlich aber auch als zusätzliches LDI-Feld beobachtet;
dieser Wert allein beweist daher keinen anderen Datenweg.

Der MCSP-Empfang durch B-Cycle ist auf dem Nutzerfahrrad **noch nicht bestätigt**.
Der neue Test basiert auf einer anderen belegten Schnittstelle; er ist kein
nachgewiesener Fix für die Ursache der leeren LDI-Antworten.

Protokollablauf, Grenzen und Quellen: [Bosch-MCSP-Datenzugriff](docs/bosch-mcsp.md).

## PDF-Schnittstelle zum Vergleich

**„PDF-Schnittstelle (Test)“** öffnet separat den bisherigen direkten LDI-Datenzugriff.
Fahrrad auswählen, diesen Test starten, etwa 30 Sekunden treten, „Werte erneut lesen“
und anschließend „Diagnose speichern“. Für die beiden Datenwege getrennte Berichte
speichern.

Grundlage: `20260501_LiveDataInterface_V1_28042026.pdf`, Bosch Live Data Interface 1.0,
01.05.2026. Die PDF wurde ausgewertet und wird hier nicht weiterveröffentlicht.

- Service `0000eb20-eaa2-11e9-81b4-2a2ae2dbcce4`.
- Live-Data-Characteristic `0000eb21-eaa2-11e9-81b4-2a2ae2dbcce4`, Read und Notify.
- Android-Bonding, MTU-Anfrage 247 und Prüfung auf mindestens 247 vor dem Datenzugriff.
- Dienst- und Characteristic-Suche pro Verbindung; CCCD `0x2902` aktivieren, Ausgangswerte lesen.
- Protobuf-Feld 1: Geschwindigkeit in 1/100 km/h; Feld 5: Fahrerleistung in W.
- Unbekannte Felder überspringen, Teilupdates zusammenführen, explizite Nullwerte erhalten
  und beschädigte Pakete vollständig verwerfen.

Die PDF definiert 13 optionale Felder: Geschwindigkeit, Kadenz, Fahrerleistung,
Helligkeit, Akkuladestand, UTC-Zeit, Gesamtkilometer, Lichtzustand, Systemsperre,
Ladegerät angeschlossen, Lichtreserve, Diagnoseprogramm aktiv und Stillstand.
Ein Lesezugriff soll die aktuell verfügbaren Felder zurückgeben. Eine Notification
darf nur geänderte Felder enthalten; fehlende Felder bedeuten dort „unverändert“.
Die Dienstsuche liefert UUIDs und Zugriffseigenschaften, keine dynamische Liste
der aktuell lieferbaren Messwertnamen.

Die PDF beschreibt Fahrrad als GAP Central / GATT Server, Zubehör als GAP Peripheral /
GATT Client. Smartphones sind ausdrücklich außerhalb ihres Geltungsbereichs (S. 7).
Der direkte Smartphone-Test beansprucht deshalb keine vollständige Profilkonformität.
Abschnitt 2.1.3.3 verlangt keine generelle Trennung paralleler Links.
Die dafür früher in B-Cycle eingeführte Wartebedingung war eine unbegründete Testbedingung.

LDI-Unterstützung ist laut Appendix ab Bedieneinheit-v19 experimentell.
Android steuert die Link-Layer-Datenlänge und Verschlüsselung; DLE ≥ 251 und die genaue
LE-Secure-Connections-Sicherheitsstufe sind hier nicht verifizierbar.
Das Protokoll meldet den Ausfall einzelner Datenquellen nicht zuverlässig (S. 14).
Das angezeigte Paketalter beweist deshalb nicht die Aktualität jeder Messgröße.

## Diagnose

Der Bericht enthält App-Version, Smartphone-Modell, Android-Version, Sitzungszeit,
GATT-Ablauf mit Statuscodes, Dienst- und Characteristic-UUIDs, Paketzähler und Decoderfehler.
Beim MCSP-Test werden Anfragen, Startdialog und Antwortstatus protokolliert.
Von empfangenen MCSP-Nutzdaten werden ausschließlich die letzten 24 erkannten numerischen
Geschwindigkeits- und Fahrerleistungswerte behalten. Gemischte MCSP-Rohpakete, Komponenten-
Kennungen und gespeicherte Fahrradlogs werden verworfen.
Beim PDF-Test bleiben die letzten vier Leseantworten und letzten 24 LDI-Notifications als
Hexdaten erhalten. Beide Messwerte werden als empfangene Felder gezählt, auch bei Nullwerten.

Gerätenamen und Bluetooth-Adressen werden nicht protokolliert.
Die Historie ist begrenzt, wird beim Beenden der App verworfen und nicht automatisch
hochgeladen. Der Export benötigt keine zusätzliche Speicherberechtigung.

Zielhardware: Xiaomi Poco F7 Ultra, Android 16 laut Diagnose, Bosch Smart System mit
System Controller und Mini Remote ohne Display, Bedieneinheit-Firmware **v20.9.0**.
Flow bleibt für das Entsperren zuständig; B-Cycle hat keine Entsperrfunktion.

## Bauen

Vollständiges JDK 17 oder 21 mit `javac`, Android-SDK Plattform 35 und Build-Tools 35.0.0.
`ANDROID_HOME` auf das SDK setzen oder eine ignorierte `local.properties` mit `sdk.dir` anlegen.

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
./gradlew :app:assembleRelease
```

Debug-APK: `app/build/outputs/apk/debug/app-debug.apk`.
Release-APK: `app/build/outputs/apk/release/app-release-unsigned.apk`, mit R8 optimiert,
noch nicht signiert. Die bereitgestellte `B-Cycle-0.1.7-test.apk` ist eine optimierte APK
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
diese Bibliothek wird nicht in die App eingebunden. MCSP-Tests prüfen veröffentlichte
Hexbeispiele, gepackte Frames, Fragmentierung, Statusantworten, den Startdialog,
Puffergrenzen und das Verwerfen fremder Daten. Die Warteschlange wird auf Reihenfolge,
fehlende Callbacks, begrenzten Rückstau und Stoppen mit späten Callbacks geprüft.
Build, Protokolltests und Lint ersetzen keinen Test am Fahrrad oder auf dem Smartphone.
