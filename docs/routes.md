# Gemeinsame Streckenverwaltung

Code auf main nach APK 0.6.6; auf Benutzerwunsch noch keine neue APK.

`MapState` betreibt den vorhandenen Manager weiter: eine gemeinsame Liste und
Kartenauswahl für GPX-Importe und FIT-Aktivitäten. `RouteManagerView` verwendet
native Views mit festem Quellenfilter und recycelnden ListView-Zeilen. Vorschaulinien
enthalten höchstens 97 Punkte; es werden keine zusätzlichen Karten oder Sensoren erzeugt.

## Bedienung

- **FIT-Aufzeichnung**: unveränderte Speicherfeld-/Hauptquellenwahl für die nächste Fahrt.
- **Streckenmanager**: Dropdown Alle, Gefahren, Importiert; Datum, Format, Herkunft,
  verfügbare Distanz/Dauer und Streckenvorschau. Bei GPX ist das Datum der Import;
  bei eigenen FIT-Fahrten der FIT-Zeitstempel. Ohne GPX-Zeitangaben keine erfundene Dauer.
- **Strecke importieren**: GPX/FIT über Androids Dateiauswahl als importierte Strecke.
- **FIT-Fahrt übernehmen**: bestehende FIT-Aufzeichnung über Dateiauswahl als gefahrene Aktivität.
- Auge: die gewählte Strecke ein-/ausblenden. Eine gemeinsame Kartenauswahl.
- ⋮: Ansehen (bei Koordinaten), Umbenennen, unverändertes Original nach Downloads
  exportieren und bestätigtes Löschen aus der Sammlung. Importierte Strecken mit
  Koordinaten sind auch als geplante Strecke auswählbar. Kein Routing/Abbiegehinweis.
- Abbrechen/Schließen eines Unterdialogs kehrt zum vorherigen Dialog zurück.

Nextcloud-Konfiguration, Sicherungsstatus und Uploadaktionen folgen später und
werden noch nicht als funktionslose Menüpunkte angezeigt.

## Dauerhafte Dateien und Migration

Die vorhandene `GpxLibrary` und ihr atomarer Index bleiben erhalten. Der Index
wird um Metadaten und begrenzte Vorschauen erweitert; ältere Einträge werden
auf dem Datei-Worker nachgetragen. GPX-Originale werden nicht neu serialisiert.

`FitLibrary` verwaltet originale private FIT-Dateien und einen atomaren JSON-Index.
Neue Aufzeichnungen werden nach FIT-Finalisierung zunächst atomar privat bewahrt,
dann weiterhin nach Download/B-Cycle exportiert. Ein Prozessabbruch zwischen
Originalspeicherung und Indexeintrag ist durch das Einlesen neuer privater Dateien
beim nächsten Start abgedeckt. Ein Downloadfehler erhält auch das Journal für die
bestehende Wiederholungsfunktion. SHA-256 dient als stabile Dateiidentität und
verhindert doppelte Sammlungsdateien bei gleichem Inhalt.

Beim Laden/Öffnen werden über MediaStore erreichbare, fertig veröffentlichte alte
B-Cycle-FIT-Dateien aus Download/B-Cycle kopiert, ohne die Downloads zu verändern.
Android kann Zugriffe nach Neuinstallation oder bei fremdem Datei-Eigentümer
beschränken; dafür steht die gezielte Android-Dateiauswahl bereit. Keine allgemeine
Speicherberechtigung oder dauerhafte Berechtigung an Importquellen erforderlich.

Umbenennen ändert ausschließlich den Index. Export schreibt die Originalbytes.
Löschen entfernt die appinterne Kopie, niemals Downloads oder Importquellen. Eine
persistente Ausschlussliste verhindert, dass ein alter Download den gelöschten
Eintrag automatisch wiederherstellt. Bewusster manueller Import bleibt möglich.
Appdaten löschen/Deinstallieren entfernt den privaten Katalog; vorhandene Downloads
bleiben erhalten und können bei ausreichendem Zugriff erneut übernommen werden.

## FIT und Ressourcen

Garmins offizieller FIT-Java-Decoder 21.218.0 validiert die vollständige CRC und
liest Records/Sessions als Stream. Es gibt keine GPX-Konvertierung. Native FIT-
Distanz, Timerdauer und Positionen werden gemäß Profil ausgewertet. Ohne GPS-Punkte
bleibt die Aktivität verwaltbar, aber ohne Kartenaktion. GPS-Lücken bleiben getrennt.

Importgrenzen: FIT 100 MB/600.000 Records; GPX weiterhin 20 MB/200.000 Punkte.
Kartenpunkte werden durch den bestehenden MapTrack begrenzt; nur die ausgewählte
Strecke wird für die Karte vollständig als begrenzte Punktfolge gelesen.
Dateioperationen und Decoder laufen außerhalb des UI-Threads. Der Recording-Worker
schreibt unveränderliche FIT-Originale; allein der MapState-Worker verwaltet den Index.
Keine Datenbank, keine neue GPS-Quelle, kein Upload- oder Hintergrunddienst.

Automatisierte Prüfungen decken Garmin-Einheiten/CRC, FIT ohne GPS, unveränderte
Originale, doppelte Importe, private Speicherung trotz Downloadfehler, GPX-
Metadaten und Migration, Auswahl/Rename/Export/Delete nach Neustart sowie
Filter/Recycling/Rücknavigation ab. Die Bedienung am Poco muss noch getestet werden.
