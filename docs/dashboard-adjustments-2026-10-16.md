# Dashboard- und Streckenanpassungen – 16.10.2026

## Darstellung

- Nur die Skalenbeschriftung der Live-Graphen 20 % kleiner: 12,96 statt 16,2 dp.
  Linien, Datenwerte, Einheiten, Graph-Geometrie und Aktualisierungsrate unverändert.
- Zonenvisualisierung mit derselben zentralen Lichtintensität wie der Rand-Glow:
  140/255 und drei additive Lichtbeiträge. Zonenfarben, 40-%-Geometrie und
  sanfte Farbübergänge erhalten.
- Design & UI → Fahrmodus-Anzeige: Seitlich (Standard) oder Oben. Oben zeichnet
  nur an der oberen Dashboardkante. Akkuwarnung folgt dieser Wahl auch bei
  ausgeschaltetem Modus-Glow, gleiche vier Sekunden Breathing und Farbübergänge.
  Sofortige Persistenz und abwärtskompatibles Design-Backup.
- Kombiniertes Akku-Raster im Dashboard und über der Karte 10 % kleiner als 0.7.29.

## Direkte Akkuauswahl

Keine separaten Prozent-Radiofelder und kein Kombianzeige-Schalter mehr. Direkt bis
vier Akkus auswählen; letzte Quelle bleibt abwählbar, leere Auswahl zeigt keine Daten.
Nur verfügbare Werte angeschlossener Geräte passend zum aktiven Bikeprofil plus
Smartphone anbieten. Indoor-Trainer nicht im E-Bike-Profil, E-Bike nicht bei Indoor.
Auswahl bleibt pro Kachel/Kartenfeld in den bisherigen stabilen IDs gespeichert.
Alte einzelne Prozent-Feld-IDs bleiben für bestehende Konfigurationen lesbar.

Bosch sendet Akkustandsänderungen. Der gültige Wert im bestehenden Verbindungspuffer
bleibt für die laufende LIVE-Verbindung erhalten, ohne 60-Sekunden-Verfall. Bei Trennung
oder Bikewechsel wird der Puffer verworfen. Keine alten Werte an eine neue Verbindung
übernehmen; 0 % gültig, außerhalb 0–100 ungültig. Keine Änderungen an FIT-Messwerten.

## Strecken

Best Efforts behalten die Auswahl der höchstens vier längsten auswertbaren Zeitfenster,
zeigen diese aber von links nach rechts mit aufsteigender Dauer. Keine Änderung der
Berechnung, Zeitfenster, Watt oder W/kg.

Doppelte Einträge können entstehen, wenn die interne Original-FIT höhenkorrigiert wird
und die korrigierte Download-Kopie beim nächsten Scan einen anderen Hash besitzt.
FitLibrary.add erkennt jetzt auch den exakten Hash der aktiven korrigierten Datei und
verwendet die vorhandene interne Fahrt-ID und deren Namen. Hashes werden pro
Dateiversion gecacht. Andere Fahrten werden nicht allein anhand gleicher Namen,
Startzeiten oder Kennzahlen zusammengelegt. Originalbytes und Korrektur bleiben erhalten.
Bereits vorhandene doppelte Einträge werden nicht automatisch gelöscht oder umbenannt.
Ohne die beiden Gerätedateien bleibt die konkrete Zuordnung des gemeldeten Paars offen;
der passende Fehlerpfad ist durch einen reproduzierbaren Test abgesichert.

## Prüfung

60 gezielte Tests bestanden, keine Fehler oder übersprungenen Tests, Kotlin kompiliert:
BatteryFieldsTest, BoschModeGlowTest, TrainingZoneUiTest, LiveGraphUiTest,
SettingsBackupTest, FitLibraryTest, BestEffortsTest und BestEffortsViewTest.
Keine erneute vollständige Testsuite. APK-Bereitstellung erfolgt künftig über
[GitHub Actions](github-actions.md).
