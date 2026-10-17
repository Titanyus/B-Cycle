# Test-Build 26 · 17.10.2026

[**APK herunterladen**](https://raw.githubusercontent.com/Titanyus/B-Cycle/test-build-26-apk/downloads/B-Cycle-0.7.31-test-build-26.apk)

## Profilseite als lange Seite

- Fünf Haupt-Accordions entfallen; alle Bereiche stehen in einer einzigen scrollenden Seite.
- Große Sticky Header mit FOREGROUND-Linien oben/unten und ACCENT-Schrift: Sensoren, FIT-Aufzeichnung, Karten, Design & UI, Trainingshilfe. Bestehende FIT-Unterüberschriften nutzen dieselbe Standarddarstellung.
- Kein Header Bike/Radtyp/Theme/Dashboard-Layout/Fahrmodus-Anzeige; Fahrmodus-Erklärung entfällt. Nach der Kartenauswahl kein zusätzlicher horizontaler Trenner.
- Sensorschalter wieder in normaler Schaltertypografie. Bestehende graue FIT-Quell-Accordions bleiben bedienbar.
- Radtyp als Dropdown mit Icons und Bike/E-Bike/Indoor. Bike bleibt vorbereitet und deaktiviert; Aufnahmesperren und bisherige Auswahlwerte bleiben erhalten.
- Fahrradbild und anklickbarer Platzhalter belegen immer 16:9. Fotos werden mittig passend beschnitten dargestellt. Kein separater Bildauswahlbutton. Löschen stellt den Platzhalter ohne Höhenwechsel wieder her.
- Dashboard bearbeiten im festen Profilfooter neben Umbenennen, Profil übernehmen und Profil löschen.
- Ausklappbare Sensor-Details sind bisher nur ein Vorschlag, keine neue Funktion dieses Builds.

[Profilanfang mit Platzhalter](previews/profile-long-top.png) · [Design](previews/profile-long-design.png) · [FIT](previews/profile-long-fit.png)

Die ältere Designvariante bleibt als [Accordiontest](accordiontest.md) dokumentiert und über Build 25 abrufbar.

## Herkunft und Prüfung

[Erfolgreicher GitHub-Actions-Build 26](https://github.com/Titanyus/B-Cycle/actions/runs/39047144461), Quellstand `c823392d94fe0523f99c66afc303e065f703fa1f`.

**38 gezielte Tests erfolgreich**: Profilwechsel und Übernahme, FIT-Konfiguration, Sticky Header, Themes/Fahrmodus, Radtyp-Dropdown, Indoor-Wechsel mit Erhalt der Outdoor-Auswahl sowie Fotoauswahl und Löschung. Nach der letzten Anpassung am Platzhalter wurden die sieben Tests aus ProfileAccordionTest, BikesViewTest und IndoorUiTest erneut erfolgreich ausgeführt. Native Vorschauen visuell geprüft. Keine vollständige Suite und kein zusätzlicher Gerätetest.

APK durch GitHub Actions gebaut, anschließend mit dem bisherigen Testschlüssel nachsigniert. Signatur und 16-KiB-ZIP-Ausrichtung geprüft. Kein lokaler APK-Build.

Version **0.7.31**, versionCode **69**; als Update über bisherige mit diesem Testschlüssel signierte APKs installierbar.

Größe: 8,222,639 Bytes.

SHA-256: `b3d4a67b9eb0195fb62191c729fe203b8949b61bd3826b5e4e3f2094f7f29c90`

Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.
