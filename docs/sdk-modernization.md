# Compile-/Target-SDK-Modernisierung

Stand: 13.10.2026. Der folgende vorgemerkte Auftrag ist umgesetzt.
Compile-/Target-SDK: **36**, Mindest-SDK: weiterhin **33** (Android 13).
AGP 8.9.2, Gradle 8.11.1, Kotlin 2.1.20 und JVM-Ziel 17 bleiben erhalten;
Build-JDK 21. Android-36-Plattform Revision 2 ist installiert.

Robolectric wurde für API 36 von 4.14.1 auf 4.16.1 erweitert. Die Offline-Testartefakte
für API 33, 35 und 36 sind zentral in Gradle definiert. Der native Zurück-Callback
wird für Karten-Vollbild registriert und danach wieder freigegeben. Native Dialoge
behalten ihre vorhandene Zurück-Navigation und Elternansichten. Keine neue Bibliothek
für App-Navigation und keine Änderungen an Sensor-/FIT-Berechnung.

Fullscreen, IME/Insets, Zurück-Navigation, Pulsmesser-GATT und Aufzeichnungslifecycle
sind auf API 33/35/36 geprüft. Die vollständige Suite mit **336 Tests** und der
Release-Build einschließlich Release-Lint-Vital bestehen. Eine tatsächliche Fahrt
unter Android 16 sowie Dateiprovider und JobScheduler-Quoten müssen zusätzlich am
Gerät geprüft werden. SDK-Anhebung allein verspricht keine kleinere oder schnellere APK.
Die API-34-Abfrage für MSL-Höhe bleibt erforderlich.

[Gesamter Implementierungs- und Buildnachweis](live-graph-overlays.md).

## Ursprünglicher Prüfplan (12.10.2026)

## Bestandsaufnahme

| Baustein | Aktueller Stand | Vorgesehener Schritt |
| --- | --- | --- |
| minSdk | 33 (Android 13) | 33 beibehalten |
| compileSdk | 35 (Android 15) | Basis-API 36 (Android 16) |
| targetSdk | 35 | Nach separater Verhaltensprüfung auf 36 |
| Android Gradle Plugin | 8.9.2 | Kompatibilität mit API 36 prüfen; nur erforderliches kompatibles Update |
| Gradle | 8.11.1 | Nur bei Bedarf passend zum gewählten AGP aktualisieren |
| Kotlin | 2.1.20 | Kein pauschales Update als Teil der SDK-Anhebung |
| JVM-Ziel | Java/Kotlin 17 | Ohne konkreten Bedarf beibehalten |
| Build-JDK | Bestehendes JDK 21 | Kompatibilität der endgültigen Toolchain prüfen |
| Testumgebung | Robolectric 4.14.1, SDK-Tests 33/35 | API-36-Unterstützung prüfen und gezielt ergänzen |

Zum Zeitpunkt der ursprünglichen Bestandsaufnahme war nur die Android-35-Plattform installiert.
Die erforderliche Android-36-Plattform und passende Testartefakte sind bei Umsetzung bereitzustellen.

`compileSdk` bestimmt die verfügbaren Android-APIs beim Kompilieren. `targetSdk` aktiviert
entsprechende Plattform-Verhaltensregeln. `minSdk` legt die älteste unterstützte Android-Version fest.
Ein neueres Compile-/Target-SDK verlangt daher keine Aufgabe von Android 13.

## Phase 1: Compile-SDK und Toolchain

1. Die offiziellen API-/AGP-Mindestanforderungen und Release-Hinweise für Basis-API 36 prüfen.
2. Android-36-Plattform installieren; nur tatsächlich erforderliche AGP-/Gradle-/Buildtool-
   Anpassungen vornehmen. Kein blindes Update aller Bibliotheken.
3. `compileSdk = 36`, zunächst weiter `targetSdk = 35` und `minSdk = 33`.
4. Native Kotlin-Views, Single-Activity-Architektur und bestehende Abhängigkeiten erhalten.
5. Neue Compiler-/Lint-Hinweise einzeln bewerten und nur betroffene Stellen anpassen.
6. Release-Build, Signatur/Updatefähigkeit, APK-Größe und relevante Tests prüfen;
   Vergleich mit dem unveränderten Ausgangsstand dokumentieren.

## Phase 2: Target-SDK und Plattformverhalten

Erst anschließend `targetSdk = 36` setzen und die betroffenen Funktionen prüfen.
Die SDK-Anhebung soll das vorhandene Layout und die Benutzerführung erhalten.

### Navigation

- Die bekannte veraltete `MainActivity.onBackPressed()`-Anbindung modernisieren.
- API-33+-fähige native Zurück-Anbindung verwenden, einschließlich relevantem Android-16-
  Predictive-Back-Verhalten. Keine neue Navigationsbibliothek allein hierfür einführen.
- Zurück aus Karten-Vollbild, Details, Untermenüs und nativen Dialogen muss dieselben
  vorhandenen Ziele erreichen. Entwürfe/Abbrechen und unterbrochene Dateiauswahl erhalten.

### Fullscreen, Systemleisten und Tastatur

- `BicycleFullscreen` und Hauptansicht auf Edge-to-edge-, Systemleisten-, Display-Cutout-
  und IME-Verhalten prüfen. Vorhandene Insets-Behandlung weiterverwenden.
- Fixe Header/Footer, Dashboard/Karte und lange scrollende Einstellungsseiten prüfen.
- WebDAV-Passwort-, Fahrerprofil- und Umbenennen-Eingaben müssen bei offener Tastatur
  bedienbar bleiben. Keine neue UI-Geometrie oder Theme-Sonderlogik einführen.
- Rotation und gegebenenfalls große Displays unter Android 16 prüfen, ohne die
  vorhandenen responsiven Reihen und Kartenlayouts zu verändern.

### Sensoren, Aufzeichnung und Speicher

- BLE-Suche, Geräteauswahl, Datenempfang und Wiederverbindung von Bosch und Pulsmesser prüfen.
- GPS-Zugriff und Foreground Service mit `connectedDevice|location` prüfen, insbesondere
  bei App-Wechsel, ausgeschaltetem Display und Anhalten/Fortsetzen.
- Benachrichtigung, START/STOP und vollständigen Abschluss der FIT-Datei erhalten.
- Nextcloud-/WebDAV-Aufträge und JobScheduler unter Android 16 auf tatsächliches Verhalten
  prüfen; OS-weite Änderungen nicht pauschal einer Target-SDK-Anhebung zuschreiben.
- SAF-Datei-/Ordnerdialoge, selektive Sicherung und Speicherung bestehender Einstellungen erhalten.
- Matrix-ICR50 bleibt ein separater zukünftiger Integrationsauftrag, siehe
  [BLE-/FTMS-Briefing](ble-trainer-matrix-icr50.md). Keine ANT-Abhängigkeit hinzufügen.

## Optimierungsziel und Grenzen

R8-Minifizierung und Resource-Shrinking sind bereits aktiv. Ein höheres SDK allein
verspricht daher keine messbar schnellere oder kleinere APK. Tatsächliche Verbesserungen
sind kompatible Toolchain-Korrekturen, weniger veraltete API-Anbindungen und gezielte
Vereinfachungen in betroffenen Stellen. APK-Größe, Buildzeit und gegebenenfalls Laufzeit-
verhalten vor/nach der Umstellung messen, ohne funktionierende Datenberechnung umzubauen.

Die vorhandene API-34-Prüfung in `GpsSensor` für MSL-Höhe bleibt bei `minSdk = 33`
erforderlich. Sensor-, FIT-, Performance- und Theme-Verhalten nicht nebenbei verändern.

## Prüfung bei Umsetzung

- Gezielte Prüfungen für Navigation, Insets/IME, BLE-/GPS-Lebenszyklus und Hintergrundaufzeichnung.
- Plattformprüfung auf Android 13 (API 33), Android 15 (API 35) und Android 16 (API 36).
- Bei einer tatsächlichen Target-SDK-Umstellung abschließend vollständige Testsuite und Release-Build.
- Signierte Test-APK separat bereitstellen; reale Android-16-Prüfung für BLE, Display-Off,
  Dateiprovider und Hintergrundverhalten zusätzlich zu JVM-/Robolectric-Tests vorsehen.

Die ursprüngliche Vormerkung enthielt nur die Analyse. Die oben dokumentierte Umsetzung
installiert API 36 und prüft die aktualisierte Konfiguration; die signierte Test-APK 0.7.9
ist im [Buildnachweis](milestone-079.md) dokumentiert.

## Offizielle Referenzen für die Umsetzung

- [Android-16-Setup und SDK-Konfiguration](https://developer.android.com/about/versions/16/setup-sdk)
- [Target-spezifische Android-16-Verhaltensänderungen](https://developer.android.com/about/versions/16/behavior-changes-16)
- [Android-16-Verhaltensänderungen für alle Apps](https://developer.android.com/about/versions/16/behavior-changes-all)
- [Minimale AGP-Versionen je API-Level](https://developer.android.com/build/releases/gradle-plugin#api-level-support)

Die endgültigen kompatiblen Toolchain-Versionen vor der Umsetzung gegen diese Referenzen bestätigen.
