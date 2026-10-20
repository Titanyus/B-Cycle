# APK-Builds mit GitHub Actions

## Automatisch und manuell

Workflow: [Android Optimized APK](https://github.com/Titanyus/B-Cycle/actions/workflows/android-debug.yml).
Jeder Push auf `main` erzeugt einen Build, ohne Pfadfilter. Manuell unter Actions →
Android Optimized APK → Run workflow → main. Ein neuer Push ersetzt einen noch laufenden
Build desselben Branches; andere Branches werden nicht automatisch gebaut.

Ubuntu 24.04, Temurin JDK 21, Gradle-Wrapper 8.11.1 mit SHA-256-Prüfung, Android-SDK 36,
Build-Tools 35.0.0, vorhandenes AGP 8.9.2/Kotlin 2.1.20. Gepinnte Action-Commit-SHAs,
Gradle-Cache, 30-Minuten-Timeout, GITHUB_TOKEN nur contents:read. Keine Release-Secrets,
kein Store-Upload, keine Änderungen an Sensor- oder Appfunktionen.

Der Build führt ausschließlich `./gradlew --no-daemon :app:assembleRelease --console=plain`
aus. Relevante Tests je Änderung separat auswählen. Keine pauschale vollständige
Testsuite bei jedem APK-Build; keine parallelen lokalen APK-Builds in der Codex-Cloud.

## APK herunterladen

Erfolgreichen Lauf öffnen → Artifacts → B-Cycle-release-LAUFNUMMER. ZIP herunterladen,
entpacken und die APK installieren. Das Artefakt enthält eine benannte APK mit Version
und Laufnummer plus SHA-256-Prüfsumme und R8 mapping.txt. Der Build-Summary enthält den direkten Artefakt-Link.
Artefakte bleiben 30 Tage verfügbar; GitHub verlangt dafür eine Anmeldung.
Ein manueller Build kann nach Ablauf der Aufbewahrung eine neue APK erzeugen.
Build-Timestamp entspricht dem tatsächlichen Start der Build-Vorbereitung.

## Einmaliger fester Testschlüssel

Ab 0.8.16 ist `ANDROID_DEBUG_KEYSTORE_BASE64` erforderlich. Ohne Secret bricht
Actions ab, statt einen neuen temporären Schlüssel zu verwenden. Vor Veröffentlichung
muss das APK-Zertifikat außerdem mit dem dauerhaften Fingerabdruck von Build 53
übereinstimmen. Früher verwendete temporäre Schlüssel bleiben historisch inkompatibel.
Bestehende Appdaten vor einem Wechsel anders signierter Installationen sichern.

Für nahtlose Updates **den bisherigen Testschlüssel** verwenden:

1. GitHub → Repository → Settings → Secrets and variables → Actions → New repository secret.
2. Name: `ANDROID_DEBUG_KEYSTORE_BASE64`.
3. Wert: Base64-Kodierung des bisherigen Android-Debug-Keystores. Der aktuelle Cloud-
   Keystore lag in der früheren Umgebung unter `/workspace/.android-user/debug.keystore` und ist hier nicht vorhanden. Den bisherigen Schlüssel
   nicht neu erzeugen, veröffentlichen oder im Chat posten. Alias `androiddebugkey`,
   Standard-Debug-Passwörter `android` werden von Androids Debug-Konfiguration verwendet.
4. Danach einmal Run workflow auslösen; spätere Pushes bauen automatisch mit diesem Schlüssel.

Alternativ kann ein dafür berechtigter GitHub-CLI-Zugang in der bestehenden Cloud-Umgebung
folgendes ausführen, ohne den Schlüssel im Terminal auszugeben:

```sh
/workspace/.signing/upload-to-github.sh
```

Stand 20.10.2026: Der Benutzer hat einen neuen dauerhaften Schlüssel autorisiert,
weil der bisherige nicht wiederherstellbar ist. Er liegt geschützt außerhalb des Repos
unter `/workspace/.signing/bicycle-release.keystore`; die ebenfalls geschützte
Base64-Datei und das obige Uploadskript liegen daneben. Alias und Passwörter entsprechen
der bestehenden CI-Konfiguration. Der neue Schlüssel ist weder mit früheren
Installationen noch mit dem temporären Schlüssel von Build 52 kompatibel.

Öffentlicher Zertifikat-Fingerabdruck (SHA-256):
`94D34C46EDE28D12E6D37EFD8A7D953A75B621596EC2E5B68F61899B8EA7499B`.

**Am 20.10.2026 auf GitHub verifiziert:** Der manuell gestartete
[Build 53](https://github.com/Titanyus/B-Cycle/actions/runs/39423155545) ist erfolgreich.
Die heruntergeladene APK hat exakt den oben genannten Zertifikat-Fingerabdruck;
das Actions-Secret funktioniert. Die integrierte Secrets-API bleibt mit HTTP 403 gesperrt,
der tatsächliche Workflow kann das eingerichtete Secret verwenden.

Den Keystore zusätzlich außerhalb dieser Cloud-Umgebung sicher aufbewahren;
Actions-Secrets lassen sich später nicht wieder herunterladen. Eine private lokale
Sicherung liegt unter `/workspace/.signing/bicycle-signing-backup.zip`.
Private Schlüssel/Base64 niemals committen, in öffentliche Build-Artefakte aufnehmen
oder im Chat ausgeben.

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

## Optimierte APK ab 0.8

Der Release-Build aktiviert R8-Codeoptimierung/Minifizierung und Ressourcenverkleinerung. Die APK ist nicht debuggable. Release verwendet dieselbe Signing-Konfiguration wie Debug. Update-Kompatibilität erfordert denselben dauerhaft hinterlegten Schlüssel; ohne verfügbaren bisherigen Schlüssel kann Nachsignieren diese Kompatibilität nicht herstellen. Mapping für spätere Fehleranalyse zusammen mit dem Meilenstein aufbewahren. Meilenstein 0.8 wird als Git-Tag und GitHub-Release mit dauerhaftem APK-Download gesichert.

## Verschlüsseltes Offline-Backup

Am 20.10.2026 auf dem separaten Branch
[`signing-key-backup-2026-10-20`](https://github.com/Titanyus/B-Cycle/tree/signing-key-backup-2026-10-20/signing-backup)
gesichert: age-passwortverschlüsseltes ZIP, SHA-256 und Entschlüsselungsanleitung.
Das separat mitgeteilte Passwort ist nicht auf GitHub gespeichert. Download über
die GitHub-API und vollständige Entschlüsselung wurden gegen die lokale Original-ZIP
geprüft. Hauptbranch und APK-Builds wurden dafür nicht geändert.
