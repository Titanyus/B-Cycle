# Test-Build 27 · 17.10.2026

[**APK herunterladen**](https://raw.githubusercontent.com/Titanyus/B-Cycle/test-build-27-apk/downloads/B-Cycle-0.7.31-test-build-27.apk)

## Kompaktere Einstellungen

- Schmale Einstellungswerte 25 % kleiner: zentral 21 statt 28 sp für Eingaben, Trainingszonen und Indoor-Faktoren; zugehörige schmale Zonen-Einheiten und Theme-Namen ebenfalls reduziert. Normale Beschriftungen, Fenstertitel und Dashboard-/Fahrtauswertungswerte behalten ihre Größen.
- Sticky-Überschriften wieder FOREGROUND; Linien und Geometrie erhalten.
- Dashboard bearbeiten neben Dashboard-Umbenennen/-Löschen im Design-Bereich; aus dem Profilfooter entfernt.
- Sensoren: Icon/Name/Schalter und eigener Chevron. Status, Gerät, Akku, Disconnect/Entfernen ausklappbar. Schalten verändert den Offen-Zustand nicht, Aufklappen verändert den Sensorzustand nicht.
- FIT-Aufzeichnung: bisherige Hauptquelle, Geschwindigkeit und Distanz mit normalem Titel/Dropdown in einer Zeile. Strecken-Aufzeichnung: bisherige aufzuzeichnende Werte und Quell-Accordions.
- Theme-Auswahl als flaches vollbreites Dropdown. Name links, fünf schräge Farbfelder rechts mit tatsächlichen Werten in der Reihenfolge PRIMARY/SECONDARY/TERTIARY/FOREGROUND/ACCENT. Gleiche Zeilen im Popup, gewähltes Theme mit ACCENT-Rahmen/Haken. Reserviert bleibt deaktiviert. Theme-Definitionen unverändert.
- Eigener Abschnitt Bosch-Fahrmodus unterhalb des Dashboard-Bereichs: Schalter sowie Titel „Sichtbar im Dashboard“ und Oben/Seitlich-Dropdown in einer Zeile.

[Theme-Dropdown](previews/theme-dropdown.png) · [Design/Bosch-Fahrmodus](previews/profile-long-design.png) · [Aufzeichnung](previews/profile-long-fit.png)

## Herkunft und Prüfung

[Erfolgreicher GitHub-Actions-Build 27](https://github.com/Titanyus/B-Cycle/actions/runs/39049978925), Quellstand `543bc7c43292dcf25783ce6774c8af2eda4fd112`.

**44 gezielte Tests erfolgreich**, ohne Fehler oder übersprungene Tests: Theme-Auswahl aller sieben Themes und fünf Farben je Theme, deaktivierter Reserviert-Eintrag, große Schrift/schmale Hochformate, unabhängige Sensordetails, FIT-Quellenzeilen, Profilwechsel/Übernahme, Indoor, Fahrerprofil, Sticky Header und Bosch-Fahrmodus. Nach der abschließenden Abstandsanpassung Theme-/Profiltests und nach Ergänzung der Schriftgrößenprüfung Fahrerprofiltests nochmals erfolgreich ausgeführt. Native Vorschauen visuell geprüft. Keine vollständige Suite und kein zusätzlicher Gerätetest.

APK durch GitHub Actions gebaut, anschließend mit dem bisherigen Testschlüssel nachsigniert. Signatur und 16-KiB-ZIP-Ausrichtung geprüft. Kein lokaler APK-Build.

Version **0.7.31**, versionCode **69**; als Update über bisherige mit diesem Testschlüssel signierte APKs installierbar.

Größe: 8,226,735 Bytes.

SHA-256: `a9df001d1a415eef2bb16481352f4b781a1c9602a4fbc313a61c01e355cd13b9`

Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.
