# B|CYCLE 0.8.15

versionCode 85. APK-Erstellung über den bestehenden GitHub-Actions-Release-Workflow.

- Globales Trendindikator-Schalter-Accordion in der Feldauswahl, mit elf einzeln
  wählbaren aktuellen/Durchschnittsfeldern. Dashboard-Seiten und Kartenwerte verwenden
  dieselbe Auswahl. Bestehende Kachelflags werden übernommen; Dashboard-Sicherung und
  künftiger Bike-UI-Konfigurationsumfang enthalten die neue Einstellung.
- Trend-Chevrons um 25 % kleiner, Stapelabstand zusätzlich um 35 % reduziert,
  Deckkraft 100/85/70 %. Zusammenhängender Zeichenpfad verhindert doppelte Deckkraft
  an der Spitze. Bestehende Trendberechnung, Historie und Überblendung erhalten.
- Dreeve-Liste aktualisiert sich nach jeder geladenen API-Seite auch mit dem
  gemeinsamen Dialog-Styling. Regression mit 398 Fahrten prüft Scrollen und Auswahl.
  Kein doppelter Fortsetzungsfooter. Konfigurieren entfällt auf der Importseite;
  Importmenüpunkt ohne eingerichtete Anbindung deaktiviert.
- Langdruck-Mehrfachauswahl im Streckenmanager: TERTIARY ohne Rahmen, ACCENT-Haken.

73 gezielte Tests bestanden (keine Fehler oder übersprungenen Fälle). Sie prüfen Trenddarstellung/-steuerung, Feldauswahl und Sicherungen sowie
Dreeve-Import, Dienstkonfiguration, Streckenmanager und Vollbilddialoge.
Echte Cloudkonten und physisches Gerät sind nicht geprüft.

## Build 52 vom 20.10.2026

[GitHub Actions erfolgreich](https://github.com/Titanyus/B-Cycle/actions/runs/39415297946),
Quellstand c6eea7aadbf9a185e86be23a5e3db057355dedf2.
[Artefakt B-Cycle-release-52 herunterladen](https://github.com/Titanyus/B-Cycle/actions/runs/39415297946/artifacts/12261431534)
(GitHub-Anmeldung erforderlich). Das ZIP enthält die Release-APK, ihre SHA-256-Datei
und das R8-Mapping. Optimierter Release mit R8 und Ressourcenverkleinerung;
kein lokaler APK-Build.

**Signierung: temporärer neuer CI-Schlüssel. Kein kompatibles Update für die
bisherige Testinstallation.** Der Workflow meldet, dass
ANDROID_DEBUG_KEYSTORE_BASE64 nicht eingerichtet ist. Der bisherige geschützte
Testschlüssel wurde in dieser neuen Cloud-Umgebung nicht gefunden. Keine
Deinstallation zum Umgehen des Signaturunterschieds empfehlen.

Für eine Update-APK den bisherigen Schlüssel aus der alten Umgebung als Actions-Secret
ANDROID_DEBUG_KEYSTORE_BASE64 einrichten und anschließend denselben geprüften Stand
erneut bauen. Alternativ die Actions-APK in einer Umgebung mit dem bisherigen
Schlüssel nachsignieren und Version, Signatur, APK-Prüfsumme und Zip-Ausrichtung prüfen.
Die bisherige Signatur lässt sich nicht aus einer vorhandenen APK zurückgewinnen.

Der erfolgreiche Job und das hochgeladene Artefakt wurden über die öffentliche
GitHub-Seite geprüft. Binärer Download und lokale APK-Prüfung sind derzeit durch
den gesperrten GitHub-API-Zugriff blockiert. api.github.com wurde als zusätzliche
Netzwerkfreigabe im Umgebungsentwurf gespeichert; diese Änderung ist noch nicht
als angewendete Laufzeitkonfiguration bestätigt.
