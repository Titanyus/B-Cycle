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

Buildnachweis, Download und Signierungsstatus werden nach dem Actions-Lauf ergänzt.
