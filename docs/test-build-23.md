# Test-Build 23 · 17.10.2026

[**APK herunterladen**](https://raw.githubusercontent.com/Titanyus/B-Cycle/test-build-23-apk/downloads/B-Cycle-0.7.31-test-build-23.apk)

## Neue Profilverwaltung

- **Einstellungen → Profile** bündelt Bikes, Karten und Design & UI. Oben die Fahrradkacheln, darunter die Accordions Bike, FIT-Aufzeichnung, Karten, Design und Trainingshilfe. Dashboard bearbeiten steht separat darunter.
- Bike-Name als normale antippbare Überschrift. Fester Footer: Umbenennen, Profil übernehmen und bestätigtes Profil löschen. Das letzte Profil bleibt erhalten.
- Theme, Dashboards einschließlich Feldern/Graphen/Trainingshilfe sowie Karteneinstellungen werden pro Fahrrad gespeichert. Vorhandene Profile übernehmen bei der Migration den bisherigen gemeinsamen Stand.
- **Profil übernehmen** zeigt andere Profile in einem Dropdown. Kopiert werden Bike-Konfiguration, FIT-Auswahl, Design, Trainingshilfe und Karten. **Name, Foto und sämtliche Sensorzuordnungen des Zielprofils bleiben unverändert.** Auch die Quelle bleibt erhalten.
- Upload-Dienste, Zugangsdaten, Fahrerprofil und Benachrichtigungen bleiben global. Die bestehende selektive Einstellungssicherung betrifft weiterhin das aktive Profil.
- Bike- und Dashboard-Kacheln verwenden gemeinsam die zuvor angeforderte animierte Drag-and-drop-Vorschau mit Accent-Rahmen, Einrücken der Nachbarn und Speichern erst beim Ablegen.

[Native Profilvorschau mit Testdaten](previews/profiles.png) · [Architektur und Migration](bicycle-profiles.md)

## Herkunft und Prüfung

[Erfolgreicher GitHub-Actions-Build 23](https://github.com/Titanyus/B-Cycle/actions/runs/39029363043), Quellstand `7e14ada5eab34ec2f0643a50246fb03ebaf778a4`.

**Vollständige Testsuite: 595 Tests erfolgreich, 0 Fehler, 0 übersprungen.** Enthalten sind Migration, unabhängige Profilwerte und Prozessneustart, FIT-Auswahl, Übernahme ohne Geräte-/Namens-/Fotoänderung, Übernahme-Dropdown, Aufnahme-Sperren, profilbezogene GPX-Auswahl einschließlich gelöschter Strecken, schnelle Profilwechsel mit ausstehenden Spinner-Callbacks, Themewechsel, feste/innere Aktionen sowie Drag-and-drop und Accessibility. Zusätzlich native Profilansicht visuell geprüft. Kein zusätzlicher Gerätetest mit echten BLE-Sensoren in dieser Umgebung.

Die APK wurde von GitHub Actions gebaut und anschließend ausschließlich mit dem bisherigen Testschlüssel nachsigniert. Kein lokaler APK-Build.
App-Version **0.7.31**, versionCode **69**. Als Update über die bisherigen mit diesem Testschlüssel signierten APKs installierbar.

Signatur und 16-KiB-ZIP-Ausrichtung geprüft.

Größe: 8,210,351 Bytes.

SHA-256: `8a25ae953e225adead4c25c1a0aef96e3d0a4e5fef179a8bcbcd04f0552db63c`

Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.
