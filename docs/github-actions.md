# APK-Builds mit GitHub Actions

## Automatisch und manuell

Workflow: [Android Debug APK](https://github.com/Titanyus/B-Cycle/actions/workflows/android-debug.yml).
Jeder Push auf `main` erzeugt einen Build, ohne Pfadfilter. Manuell unter Actions →
Android Debug APK → Run workflow → main. Ein neuer Push ersetzt einen noch laufenden
Build desselben Branches; andere Branches werden nicht automatisch gebaut.

Ubuntu 24.04, Temurin JDK 21, Gradle-Wrapper 8.11.1 mit SHA-256-Prüfung, Android-SDK 36,
Build-Tools 35.0.0, vorhandenes AGP 8.9.2/Kotlin 2.1.20. Gepinnte Action-Commit-SHAs,
Gradle-Cache, 30-Minuten-Timeout, GITHUB_TOKEN nur contents:read. Keine Release-Secrets,
kein Store-Upload, keine Änderungen an Sensor- oder Appfunktionen.

Der Build führt ausschließlich `./gradlew --no-daemon :app:assembleDebug --console=plain`
aus. Relevante Tests je Änderung separat auswählen. Keine pauschale vollständige
Testsuite bei jedem APK-Build; keine parallelen lokalen APK-Builds in der Codex-Cloud.

## APK herunterladen

Erfolgreichen Lauf öffnen → Artifacts → B-Cycle-debug-LAUFNUMMER. ZIP herunterladen,
entpacken und die APK installieren. Das Artefakt enthält eine benannte APK mit Version
und Laufnummer plus SHA-256-Prüfsumme. Der Build-Summary enthält den direkten Artefakt-Link.
Artefakte bleiben 30 Tage verfügbar; GitHub verlangt dafür eine Anmeldung.
Ein manueller Build kann nach Ablauf der Aufbewahrung eine neue APK erzeugen.
Build-Timestamp entspricht dem tatsächlichen Start der Build-Vorbereitung.

## Einmaliger fester Testschlüssel

Ohne Secret erzeugt Android einen temporären Debugschlüssel. Die APK ist gültig signiert,
aber weder mit den bisherigen Test-APKs noch zuverlässig mit anderen Actions-Läufen
als Update kompatibel. Bestehende Appdaten nicht durch ungeplante Deinstallation verlieren;
vor einem solchen Wechsel die vorhandene Einstellungssicherung und Fahrten exportieren.

Für nahtlose Updates **den bisherigen Testschlüssel** verwenden:

1. GitHub → Repository → Settings → Secrets and variables → Actions → New repository secret.
2. Name: `ANDROID_DEBUG_KEYSTORE_BASE64`.
3. Wert: Base64-Kodierung des bisherigen Android-Debug-Keystores. Der aktuelle Cloud-
   Keystore liegt unter `/workspace/.android-user/debug.keystore`. Diesen Schlüssel
   nicht neu erzeugen, veröffentlichen oder im Chat posten. Alias `androiddebugkey`,
   Standard-Debug-Passwörter `android` werden von Androids Debug-Konfiguration verwendet.
4. Danach einmal Run workflow auslösen; spätere Pushes bauen automatisch mit diesem Schlüssel.

Alternativ kann ein dafür berechtigter GitHub-CLI-Zugang in der bestehenden Cloud-Umgebung
folgendes ausführen, ohne den Schlüssel im Terminal auszugeben:

```sh
base64 -w0 /workspace/.android-user/debug.keystore | gh secret set ANDROID_DEBUG_KEYSTORE_BASE64 --repo Titanyus/B-Cycle
```

Der aktuelle integrierte GitHub-Zugang verweigert die Secrets-API mit HTTP 403.
Der Workflow selbst benötigt zum Lesen eines eingerichteten Secrets keine erweiterten
GITHUB_TOKEN-Rechte. Ein geheimer Schlüssel gehört niemals als Datei ins Repository.

## Fehler untersuchen

`gh run list --repo Titanyus/B-Cycle --workflow android-debug.yml --limit 5`
zeigt die letzten Läufe. Mit `gh run view RUN_ID --json status,conclusion,jobs,url`
den betroffenen Schritt ermitteln. Nur dessen Fehlerauszug auswerten, keine kompletten
Protokolle in den Chat übernehmen. Erst nach einer notwendigen Änderung erneut starten.

Falls Actions deaktiviert ist: Repository → Settings → Actions → General → Actions
zulassen. Workflow-Datei und Hauptbranch nicht umbenennen, ohne Trigger/Links anzupassen.

## Nachsignierter Test-Build

Solange das feste Schlüssel-Secret fehlt, kann Codex die erfolgreiche Actions-APK
herunterladen und mit dem bisherigen geschützten Testschlüssel nachsignieren. Es wird
dabei keine APK lokal gebaut. Prüfsumme und Signatur des Ergebnisses erneut prüfen.
[Build 5](test-build-5.md) ist auf diesem Weg entstanden. Reine APK-/Nachweis-Publikationen
verwenden [skip ci], damit daraus kein unnötiger weiterer Build entsteht; App- und
Workflow-Änderungen starten weiterhin automatisch einen Build.
