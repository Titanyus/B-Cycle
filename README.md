# B-Cycle

Minimaler experimenteller Radcomputer für Android 13+ (API 33).
Live-Anzeige von Geschwindigkeit in km/h und **Fahrerleistung** in Watt.
Kotlin mit Android-Plattform-Views; keine UI-, Netzwerk-, Analyse- oder BLE-Bibliotheken.
Keine Internetberechtigung, GPS-Aufzeichnung oder dauerhaften Hintergrunddienste.
Diagnosedaten bleiben im Arbeitsspeicher, bis ausdrücklich eine Textdatei exportiert wird.
Cycplus-Herzfrequenz kommt in einem späteren Schritt.

## APK herunterladen

[Installierbare Test-APK 0.1.5 für Android 13+ herunterladen](https://github.com/Titanyus/B-Cycle/raw/refs/heads/main/downloads/B-Cycle-0.1.5-test.apk).
Die Datei liegt auch im Ordner `downloads`. Falls GitHub eine Dateiseite öffnet, dort
„Download raw file“ auswählen. Bei einem privaten Repository vorher bei GitHub anmelden.

Die APK ist 87.543 Byte groß, optimiert und mit demselben lokalen Testschlüssel wie
die bisherigen Versionen signiert. Sie kann als Update installiert werden.
SHA-256: `c7194fa61d456ee045a8ba556913334f398ec4d94441d3d41cb700dd7973e3a6`.
Build, 30 Tests (zehn Decoder-, sechs Diagnose-, vier Informationsdienst-, sechs
Verbindungsablauf- und vier Freigabe-Tests), Android-Lint und
APK-Signatur wurden geprüft.

## Zubehörtest mit 0.1.5

1. APK als Update installieren, Fahrrad mit Flow entsperren. Danach Flow über
   Android-App-Info → „Beenden erzwingen“ beenden und einige Sekunden warten.
   Auch andere aktive Rad-Apps wie Locus beenden, damit kein vorhandener Link übernommen wird.
2. In B-Cycle **„Fahrrad auswählen“** antippen, Bluetooth-Zugriff erlauben und das eigene
   Fahrrad aus der Liste auswählen. **Die Auswahl verbindet nicht mehr automatisch.**
   Das ausgewählte Fahrrad wird separat angezeigt.
3. **„Als Zubehör anbieten“** antippen. Ist noch eine eigene B-Cycle-Verbindung aktiv,
   trennt die App sie zuerst und wartet auf Androids Disconnect-Rückmeldung. Danach
   startet sie die Ankündigung. „Als Zubehör anbieten“ ohne vorherige Auswahl öffnet
   ebenfalls die Auswahl und startet den Modus nach Auswahl des Fahrrads.
4. Auf „Als Zubehör sichtbar“ warten. Falls dabei **„Android meldet noch eine bestehende
   Rad-Verbindung“** steht, wartet B-Cycle auf deren tatsächliche Trennung. Diese Meldung
   weist die Verbindung **keiner bestimmten App** zu: Android gibt deren Besitzer nicht an.
   Bleibt der Hinweis bestehen, den Diagnosebericht speichern.
   Die Ankündigung läuft höchstens zwei Minuten.
   Falls das Rad nicht von selbst verbindet, währenddessen die Zubehörsuche am Rad oder
   in Flow starten. Der genaue Menüweg für System Controller + Mini Remote ist nicht
   in der Bosch-PDF beschrieben. In der Liste könnte der **Bluetooth-Gerätename des Handys**
   erscheinen, nicht „B-Cycle“. Ein langer Gerätename wird bei Platzmangel aus der Ankündigung
   weggelassen; die Bosch-Service-Solicitation bleibt erhalten.
5. Zum Wechsel in Flow darf B-Cycle im Zubehör-Modus höchstens zwei Minuten im Hintergrund
   bleiben. Danach zeitnah zu B-Cycle zurückkehren. Android kann den Prozess zuvor beenden;
   es gibt keinen dauerhaften Hintergrunddienst.
6. Nach einer Verbindung treten und Messwerte prüfen. Bei fehlenden Werten „Werte erneut lesen“
   verwenden. Anschließend **„Diagnose speichern“** und `B-Cycle-0.1.5-diagnose.txt` hochladen.
   Auch bei „Als Zubehör sichtbar“ ohne anschließende Verbindung ist der Bericht hilfreich.

Dieser Modus testet eine konkrete Abweichung der bisherigen APK zur PDF: Das Zubehör
kündigt den Bosch-Dienst an, das Fahrrad soll den BLE-Link initiieren. Erst wenn Android
nach bestätigtem Ankündigungsstart eine neue GATT-Server-Verbindung vom ausgewählten Rad meldet,
öffnet B-Cycle den
GATT-Client für die Bosch-Daten auf dem vorhandenen Link. Ob Android diesen Link in der
vorgesehenen Rollenverteilung weiterverwendet und Bosch darüber Daten liefert, ist am
Fahrrad **noch nicht bestätigt**. Das ist ein neuer Verbindungsversuch, kein nachgewiesener Fix.

Das Protokoll von 0.1.3 hat einen Fehler dieses Versuchs offengelegt: Android meldete
`ACCESSORY_INCOMING` bei 2714 ms, noch vor `ACCESSORY_ADVERTISE_REQUEST` bei 2717 ms und
`ACCESSORY_ADVERTISE_STARTED` bei 2763 ms. Diese Verbindung konnte keine Reaktion auf die
Ankündigung sein. Dennoch öffnete die App sofort den GATT-Client; alle sechs Leseantworten
waren wieder leer. Der Test belegte daher keinen vom Rad als Reaktion auf Advertising
gestarteten Link.

0.1.4 akzeptiert solche frühen Callbacks nicht mehr. Ein beim Start bekannter oder vorzeitig
gemeldeter Link muss erst getrennt werden. Auch ein verspäteter Callback zu einem schon vorher
bekannten Link wird zurückgewiesen. Erst ein neuer Connected-Callback nach bestätigtem
Advertising wird angenommen. Stoppen während der asynchronen Ankündigung wird berücksichtigt.
Sechs Regressionstests prüfen unter anderem die Ereignisfolge aus dem hochgeladenen Protokoll.
Die korrigierte Abfolge ist noch nicht am Fahrrad getestet und beweist auch weiterhin nicht
unabhängig die physische GAP-Rolle.

Das nächste Protokoll mit 0.1.4 begann beim Wechsel aus einer vorhandenen B-Cycle-Live-Sitzung.
Android meldete beim Ankündigungsstart noch einen bestehenden Link. Erst bei 24176 ms
wurde dessen Trennung gemeldet; bis zum Stoppen bei 28701 ms blieb anschließend nur ein
kurzes Fenster für einen neuen Link. Die Formulierung „Flow beenden“ war irreführend:
Android weist diese Verbindung keiner App zu. Außerdem öffnete die frühere Geräteauswahl
im direkten Modus selbst eine Verbindung, was den Wechsel unnötig erschwerte.

0.1.5 trennt Auswahl und Verbindungsaufbau. **„Fahrrad auswählen“** sucht und wählt nur;
**„Als Zubehör anbieten“** startet den Zubehörtest, **„Direkt verbinden (Test)“** den
expliziten Vergleichstest. Beim Wechsel wird die eigene GATT-Verbindung zunächst per
Disconnect freigegeben. Der Client wird erst nach dessen Callback oder spätestens nach
20 Sekunden geschlossen; beim Zerstören der Activity werden ausstehende Clients sofort
geschlossen. Die Ankündigung wartet auf diese eigene Freigabe. Vier Tests prüfen die
Reihenfolge, Timeout-Begrenzung, späte Callbacks und begrenzten Ressourcenverbrauch.
Das löst die irreführende Bedienung und verbessert die Freigabe; ein Erfolg des Bosch-
Datenzugriffs am Fahrrad ist weiterhin nicht bestätigt.

## Bisheriger direkter Verbindungsweg zum Vergleich

1. Test-APK installieren. Fahrrad mit Bosch Flow verbinden und entsperren.
2. B-Cycle öffnen, „Fahrrad auswählen“ antippen und Bluetooth-Zugriff erlauben.
3. Fahrrad aus der Liste auswählen. Die Liste enthält bereits verbundene bzw. gekoppelte
   BLE-Geräte sowie Ergebnisse einer zehnsekündigen Suche, auch Geräte anderer Hersteller.
4. **„Direkt verbinden (Test)“** antippen. Falls Android eine Kopplungsbestätigung zeigt,
   diese bestätigen.
5. Status verfolgen: GATT-Verbindung, Kopplung, MTU, Bosch-Dienst, Notification-Abonnement,
   initiales Lesen, Live-Daten. Fehlende oder ungültige Werte werden nicht als Null angezeigt.
6. Geschwindigkeit und Fahrerleistung mit einer Fahrradanzeige vergleichen.
   Auch Stillstand, Treten, Aussetzen des Tretens, „Stoppen“ und Wiederverbindung prüfen.

Zielhardware: Xiaomi Poco F7 Ultra, Bosch Smart System mit System Controller und Mini Remote
ohne Display, Firmware laut Nutzer **v20.9.0**, Smartphone Android 16 laut Diagnose.
Beim ersten Hardwaretest mit 0.1.0 wurde
„Live verbunden · MTU 247“ erreicht, aber beim Drehen des Rads erschienen keine Messwerte.
Dieser Status bedeutet im Programm, dass der LDI-Dienst gefunden, das Abonnement bestätigt
und das initiale Paket gelesen und akzeptiert wurde. Welche Felder darin enthalten waren
und ob Notifications ankamen, wurde mit dem Diagnoseexport in 0.1.1 untersucht.
Der Nutzer trat während des Tests. Das Protokoll zeigt eine erfolgreiche Kopplung,
MTU 247, den LDI-Dienst und ein erfolgreich bestätigtes Notification-Abonnement.
Die initiale Leseantwort enthielt jedoch **0 Byte**; während der folgenden rund 19 Sekunden
kamen **keine Notifications** an. Es wurden weder Geschwindigkeit und Leistung noch
explizite Nullwerte geliefert. Die leere Antwort ist eine gültige Protobuf-Nachricht,
enthält aber keinerlei Messwerte. Mit 0.1.2 waren anschließend acht Leseantworten leer,
ohne Notifications während rund 30 Sekunden Verbindung. Ein weiterer Test mit von Anfang
an beendeter Flow-App und entsperrtem Rad ergab ebenfalls sechs leere Antworten und keine
Notifications während rund 25 Sekunden Verbindung. Eine parallel laufende Flow-App erklärt
dieses Verhalten damit nicht. Die Ursache ist weiterhin ungeklärt.

Version 0.1.2 ersetzt in diesem Fall „Live verbunden“ durch „Verbunden · noch keine Fahrdaten“.
Gelesene Fahrdaten und durch Notifications empfangene Live-Messwerte werden im Status
unterschieden. Leere Antworten aktualisieren das angezeigte Paketalter nicht mehr.
„Werte erneut lesen“ startet während einer bestehenden Verbindung genau einen weiteren
LDI-Lesezugriff; Notifications bleiben aktiv. Eine Behebung des Datenzugriffs am Fahrrad
ist noch nicht bestätigt.
Flow bleibt für das Entsperren zuständig; B-Cycle hat keine Entsperrfunktion.

## Diagnose beim direkten Verbindungsweg

1. Version 0.1.5 als Update installieren, Fahrrad mit Flow entsperren und in B-Cycle
   auswählen. Anschließend „Direkt verbinden (Test)“ antippen.
2. Nach dem Verbinden etwa 20–30 Sekunden mit drehendem Rad testen; für Fahrerleistung
   muss getreten werden. Das Smartphone dabei sicher bedienen, nicht während der Fahrt.
3. Während des Tests zwei- oder dreimal „Werte erneut lesen“ antippen, jeweils die Antwort
   abwarten. Das prüft, ob inzwischen Werte zum Abruf bereitstehen, obwohl Notifications fehlen.
4. Danach „Diagnose speichern“ antippen und im Android-Dateidialog beispielsweise
   „Downloads“ als Speicherort wählen. Beim Öffnen dieses Dialogs stoppt die BLE-Verbindung;
   der Diagnosebericht wird unmittelbar vorher festgehalten.
5. Die gespeicherte Datei `B-Cycle-0.1.5-diagnose.txt` für die Fehlersuche hochladen.
   Vor dem Export keine neue Suche starten: Diese setzt die Diagnose zurück.

Für einen separaten Vergleichstest kann Flow nach dem Entsperren vollständig beendet werden
(Android-App-Info → „Beenden erzwingen“), anschließend B-Cycle neu verbinden.
Das normale Wegwischen beendet Flow möglicherweise nicht. Falls das Fahrrad dadurch wieder
gesperrt wird, Flow erneut öffnen und diesen Vergleich abbrechen. Ein Einfluss der parallelen
Flow-Verbindung ist eine Hypothese, kein aus dem Protokoll nachgewiesener Fehler.
Für beide Tests getrennte Diagnose-Dateien speichern und die genaue System-Controller-Firmware
aus Flow notieren; die PDF beschreibt experimentelle LDI-Unterstützung ab v19.

Der Bericht enthält App-Version, Smartphone-Modell, Android-Version, Sitzungszeit,
GATT-Ablauf mit Statuscodes, Dienst- und Characteristic-UUIDs, Paketzähler, Decoderfehler,
die letzten vier Leseantworten (initial und manuell) sowie die letzten 24 LDI-Notifications als Hexdaten.
Geschwindigkeit und Fahrerleistung werden als empfangene Felder gezählt, auch bei Nullwerten.
Gerätenamen und Bluetooth-Adressen werden nicht protokolliert. Aus anderen Characteristics
werden keine Nutzdaten gelesen oder gespeichert. Die Historie ist begrenzt, wird beim Beenden
der App verworfen und wird nicht automatisch hochgeladen. Der Export benötigt keine zusätzliche
Speicherberechtigung.

Die App hält das Display während des Tests an. Der direkte Verbindungsweg stoppt beim
Verlassen des Vordergrunds. Der Zubehör-Modus erlaubt einen Wechsel in Flow für höchstens
zwei Minuten; danach wird auch er beendet. Trennungen setzen beide Messwerte zurück.
Der Diagnoseexport hält den Bericht fest und stoppt die Verbindung vor dem Dateidialog.
Wiederverbindung erfolgt manuell über die Suche bzw. erneute Geräteauswahl.

## Schnittstelle und Grenzen

Grundlage: `20260501_LiveDataInterface_V1_28042026.pdf`, Bosch Live Data Interface 1.0,
01.05.2026. Die PDF wurde ausgewertet und wird hier nicht weiterveröffentlicht.

Die PDF verlangt eine ungewöhnliche Rollenverteilung: Fahrrad als GAP Central / GATT Server,
Zubehör als GAP Peripheral / GATT Client. Smartphone-Unterstützung ist ausdrücklich außerhalb
ihres Geltungsbereichs (S. 7). „Zubehör“ bezeichnet ein BLE-Gerät, keine andere App im Flow-Menü.
Die PDF legt nicht fest, wie die Flow-Zubehörsuche auf einem Smartphone umgesetzt ist.
Die frühere pauschale Aussage, Flow auf demselben Smartphone könne diesen Zubehörweg nicht
nutzen, ließ sich daraus nicht ableiten. Maßgeblich ist die vom Rad initiierte Verbindung.
Der Zubehör-Koppelweg für den System Controller ohne Display ist in der PDF nicht beschrieben.

**„Direkt verbinden (Test)“ verwendet Androids normalen BLE-Clientweg.**
Sie versucht, einen vorhandenen Flow-Link zu nutzen oder eine direkte BLE-Verbindung zum
gewählten Fahrrad aufzubauen. Ob LDI auf diesem Link erreichbar ist, muss am Fahrrad
geprüft werden. Dies ist keine vollständige Implementierung des vorgeschriebenen GAP-Profils.
Eine funktionierende Flow-Verbindung garantiert nicht die Freigabe des separaten LDI-Dienstes.

**„Als Zubehör anbieten“ ergänzt ab 0.1.3 einen experimentellen Peripheral-Modus:**

- Connectable BLE-Advertising mit Bosch-LDI-UUID im **Service-Solicitation-Feld** (AD-Typ `0x15`).
- Scan Response mit optionalem Android-Bluetooth-Gerätenamen und eigenem Device Information
  Service. Der globale Bluetooth-Name des Handys wird nicht verändert.
- Eigener GATT-Server mit drei lesbaren DIS-Characteristics: Hersteller „B-Cycle“, Modell
  „B-Cycle Android“, App-Version. Keine Seriennummer, erfundenen Sensorwerte oder Motorbefehle.
- Advertising muss bestätigt sein; ein schon vorhandener Link muss erst getrennt werden.
  Die danach neu gemeldete Verbindung muss zum ausgewählten Rad gehören. Erst danach GATT-Client öffnen,
  Bonding prüfen, MTU anfragen, LDI suchen und abonnieren.
- Ankündigung endet nach Verbindungsannahme, Stoppen, Zeitlimit oder Zerstörung der Activity.
  Späte Callbacks früherer Sitzungen werden verworfen; DIS bleibt auf dem aktiven Link verfügbar.

Androids öffentliche Advertising-API bietet kein frei konfigurierbares Appearance-AD-Feld.
Dieses in der PDF geforderte Feld fehlt; ein DIS-Feld oder Manufacturer Data ersetzt es nicht.
Auch die physische GAP-Rolle nach dem GATT-Client-Anhängen wird nicht unabhängig verifiziert.
Die APK beansprucht deshalb weiterhin keine vollständige Konformität mit dem Zubehörprofil.

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
noch nicht signiert. Die bereitgestellte `B-Cycle-0.1.5-test.apk` ist eine optimierte APK
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
