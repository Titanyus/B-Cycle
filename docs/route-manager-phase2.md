# Streckenmanager – Phase 2

Stand: 11.10.2026. Erste Migration auf die Design Engine aus Phase 1.
Umgesetzt ausschließlich für den Streckenmanager inklusive seiner Import-/Aktions-Dropdowns.
Andere Einstellungsbildschirme behalten ihren bisherigen Aufbau und ihre Farben.
Kein Farbwähler und kein neuer Theme-Menüpunkt. Die Lizenzseite erhält lediglich den Lucide-Lizenztext.

## Design und Komponenten

Verwendet werden `BicycleDesign`, `ThemeRole`, `ThemeTokens`, `ThemeManager` und
`BicycleComponents.header`, `heading`, `text`, `icon`, `background`, `bindTheme`.
Neue Maße liegen in den zentralen Design-Tokens, nicht in Farbpaletten.

Der zentrierte Header verwendet Bebas Neue, alle anderen Texte den Systemfont.
Primary färbt den Header, Secondary die Dialogfläche, Tertiary die Dropdowns,
Foreground Texte, Vektoren, Vorschauen, Scrollbalken und Trennlinien. Ein Themewechsel
ändert keine Maße, Schriftgrößen oder Bedienlogik. Anmeldungen beim Theme-Manager
werden beim Entfernen einer View beendet und beim erneuten Anhängen aktualisiert.

Die native `ListView` und ihre wiederverwendeten Zeilen bleiben erhalten.
Header, Filter und Android-Dialogfooter scrollen nicht mit. Es gibt keine
verschachtelte Scrollfläche, separate Track-Karten oder neue UI-Bibliothek.
Lange Namen werden einzeilig mit Ellipse dargestellt. Datum, Distanz, Zeit,
Höhenmeter und Status verwenden 14 sp. Distanz und Zeit wechseln bei Platzmangel
in eine vertikale Anordnung. Statussymbole lösen keine Aktionen aus; dafür gibt
es ausdrücklich das horizontale Drei-Punkte-Menü und die Aktionsschaltflächen.

## Lucide

Lokale Android-VectorDrawables aus **Lucide 0.468.0**, ohne Laufzeitabhängigkeit.
24-dp-Viewport, Strichstärke 2, runde Enden/Verbindungen, 48×48-dp-Touchflächen.
`BicycleIcon` ist der semantische Katalog; Views kennen keine SVG-Dateinamen.
`SlashedIcon` zeichnet für alle negativen Zustände dieselbe Diagonale im
24-Einheiten-Koordinatensystem. Sie wird zusammen mit dem Original eingefärbt.

Zuordnung:

| Bedeutung | Lucide |
| --- | --- |
| Aufgezeichnet / importiert / unbekannt | bike / log-in / circle-help |
| GPX / FIT | nur der Formattext GPX/FIT; kein Datei-Icon im Streckenmanager |
| Streckenaktionen / Streckeneinstellungen | ellipsis / ellipsis-vertical |
| Kartenanzeige | eye |
| Cloud | cloud-upload |
| Lokaler Speicher | smartphone |
| Laufender Upload | loader-circle mit sichtbarer Statusmeldung |
| Datum / Distanz / Dauer / Höhenmeter | calendar-days / chart-no-axes-combined / clock / mountain |
| Löschen / Umbenennen / Export / Import | trash-2 / pencil / download / download |
| Dropdown / Schließen / Zurück | chevron-down / x / arrow-left |

Der vollständige upstream-Lizenztext liegt in `assets/Lucide-LICENSE.txt` und ist
in der vorhandenen Lizenzseite erreichbar: ISC plus Feather-MIT-Attribution.
`scripts/import_lucide.py` erzeugt die Vektoren reproduzierbar aus diesem Versionstag.
Es wird nicht während eines Builds ausgeführt und ist keine App-Abhängigkeit.

## Tatsächliche Zustände und Aktionen

- **Karte:** Normal nur bei ausgewählter, erfolgreich geladener Strecke und
  eingeschalteter `mapGpxVisible`-Darstellung. Tippen verwendet die bestehende
  `MapState.selectRoute`-Funktion zum Anzeigen/Ausblenden. Reine Sensorfahrten
  ohne GPS-Punkte haben einen deaktivierten, negativen Kartenstatus.
- **Cloud:** Nur selbst gefahrene FIT-Fahrten (`RECORDED`) erhalten die Aktion.
  Importierte GPX-/FIT-Tracks und unbekannte Herkunft haben weder eine
  Cloud-Schaltfläche noch eine Upload-Menüaktion. Der Cloud-Platz entfällt vollständig; Auge und Smartphone stehen
  direkt nebeneinander. GPX-Upload wurde nicht implementiert. Ein ausdrücklich als eigene
  aufgezeichnete FIT-Fahrt wiederhergestellter Import bleibt uploadfähig.
- **Uploadzustand:** `NOT_UPLOADED`, `WAITING`, `UPLOADING`, `UPLOADED`, `FAILED`
  stammen aus der vorhandenen dauerhaften `RouteUploads`-Warteschlange.
  Ausstehende, laufende und fehlgeschlagene Übertragungen zeigen eine Meldung;
  erst der bestätigte Erfolg zeigt das normale Cloud-Symbol. Wartende/laufende
  Uploads lassen sich über die Schaltfläche nicht mehrfach starten.
- **Übertragungsprüfung:** Weiterhin der vorhandene WebDAV-Client, HTTPS,
  bestehende Zugangsdaten und Android JobScheduler. Der Client bestätigt die
  Zieldatei bereits durch Größe und SHA-256. Die Warteschlange speichert jetzt
  zusätzlich SHA-256, Länge, Änderungszeit und Ziel. Gleiche laufende oder
  abgeschlossene Aufträge werden nicht neu angelegt. Ein alter Auftrag darf
  den Status einer neu vorgemerkten Dateiversion oder eines neuen Ziels nicht
  als erfolgreich überschreiben. Automatische Uploads nach STOPP nutzen
  dieselbe Warteschlange. Netzwerkprobleme bleiben mit Backoff wartend;
  dauerhafte Fehler können über die Aktion erneut versucht werden.
- **Geänderte Dateien:** Die UI vergleicht den vom Bibliotheksworker ermittelten
  Dateistempel mit dem gesicherten Upload. Abweichungen werden nicht als aktuell
  hochgeladen gezeigt. Vor/nach Übertragung wird zusätzlich der Inhalt gehasht.
  Originaldateien sind im normalen Appbetrieb unveränderlich; Umbenennen
  verändert lediglich Metadaten und invalidiert keine identische FIT-Datei.
  Frühere Erfolgseinträge ohne Prüfdaten müssen einmal erneut geprüft werden.
- **Lokaler Speicher:** Normal nur bei tatsächlich vorhandener, lesbarer
  Originaldatei in der privaten Streckensammlung. Prüfungen laufen auf dem
  vorhandenen Bibliotheksworker. Antippen einer vorhandenen Datei bestätigt
  ihren Speicherzustand; es erzeugt keine Kopie und löscht nichts. Fehlt die
  Originaldatei, entsteht eine verständliche Aufforderung zum erneuten Import,
  kein fingierter Speichererfolg. Ein FIT-Reimport kann eine fehlende identische
  Originaldatei unter der bestehenden ID wiederherstellen.

Datei-/Herkunfts-/Statusdaten stammen aus den bestehenden Bibliotheken,
Kartenstatus aus `MapState` und Darstellungseinstellungen, Cloudstatus allein
aus `RouteUploads`. Keine zweiten lokalen Upload- oder Kartenschalter.

## Datenmodell und Kompatibilität

`TrackOrigin` speichert `IMPORTED`, `RECORDED`, `UNKNOWN` in den vorhandenen
atomischen GPX-/FIT-Indizes. Alte FIT-Einträge verwenden ihren ausdrücklich
vorhandenen booleschen `recorded`-Wert; fehlende/ungültige Herkunft bleibt unbekannt.
Alte GPX-Einträge sind bekannte Importe, weil diese Bibliothek bisher nur den
Importweg besaß. Unbekannte Einträge erscheinen ausschließlich unter „Alle“.
Die eigentlichen Trackdateien werden für diese Migration nicht verändert.

`RouteDetails.ascentM` ist optional: FIT bevorzugt Garmins offiziellen
`session.total_ascent`; fehlt er, können vorhandene GPS-Höhen der Records
verwendet werden. GPX-Höhen werden streamend mit 3-m-Hysterese summiert.
Fehlende Höhen und Segmentwechsel werden nicht verbunden. Alte Metadaten
werden einmal auf dem Bibliotheksworker ergänzt; fehlende Angaben bleiben leer.
Keine erfundenen Höhenmeter. `RouteFileStamp` ist ein vom Worker ermittelter
Dateistempel, kein persistierter UI-Schalter. Unterstützte Formate bleiben GPX/FIT;
TCX wird nicht angezeigt oder neu unterstützt.

## Erhaltene Funktionen

Importdialoge für geplante GPX/FIT-Strecken und wiederhergestellte eigene
FIT-Aufzeichnungen, Filter, Auswahl/Anzeige/Einpassen von Strecken, Originaldateien,
Streckenvorschau, Umbenennen, Downloads-Export und Löschen mit Bestätigung bleiben
vorhanden. Import und Aktionen bleiben verankerte Dropdowns ohne Titel/Abbrechen.
Unterdialoge führen weiterhin zum vorherigen Manager zurück. Aufzeichnung,
FIT-Encoding, Sensoren, Dashboard und Kartenrenderer werden nicht verändert.
Die unabhängigen Speicher-/WebDAV-Einstellungen bleiben erhalten.

## Prüfung und Grenzen

Verifikation: `:app:testDebugUnitTest :app:assembleRelease :app:lintDebug`.
Abgeschlossen: **142 Tests bestanden, 0 fehlgeschlagen, 0 übersprungen**;
Release-Build erfolgreich; Lint **35 Hinweise, 0 Fehler**. Zusätzlich bleibt
der bestehende Kotlin-Hinweis zum veralteten `onBackPressed()` bestehen.
Kein Check wurde deaktiviert; Abhängigkeiten und Versionierung bleiben unverändert.
Tests prüfen Herkunft/Migration, Höhenmeter und fehlende Originaldateien,
Dateiänderung während Upload, unabhängige Uploadfehler, Deduplizierung einschließlich
manuellem/automatischem Zugriff und einem während des Uploads geänderten Ziel.
Bestehende WebDAV-Tests prüfen die HTTPS-Kommunikation gegen MockWebServer.
UI-Tests prüfen Dropdowns, Rücknavigation, Kartensichtbarkeit, native Listen,
lange Namen, viele/leere Einträge, Landscape, schmale Ansicht und zwei Themes.
Bei Farbwechsel werden identische Geometrien und Schriftgrößen geprüft.

Native PNG-Vorschauen liegen nach den Tests unter `app/build/reports/settings/`:
`route-manager-red`, `route-manager-light`, `route-manager-narrow`,
`route-manager-landscape`, `route-manager-empty`, `route-manager-dialog`.

Abweichungen zum Photoshop-Mockup: Die 48-dp-Touchflächen stehen kompakt rechts
neben den Messwerten. Auf schmalen Displays können die Messwerte untereinander
oder mit Zeilenumbruch erscheinen. Reicht der Platz auch dafür nicht, wechseln
die Aktionsbuttons gemeinsam in die Zeile darunter, weiterhin ohne freie Cloud-Spalte. Höhenmeter und Uploadfehler erzeugen bei
Bedarf weitere Zeilen. Die sichtbaren Schaltflächen bleiben gleich groß;
importierte Strecken haben nach der ergänzenden Nutzeranweisung keinen Cloud-Button.
Es werden die vorhandenen validierten BICYCLE-Red-Farben und Bebas Neue verwendet,
keine aus dem Bild geschätzte Palette oder zusätzliche Schriftfamilie.

Ein realer Nextcloud-/Dreeve-Upload und die visuelle Freigabe auf dem Smartphone
stehen noch aus. Es entsteht keine neue Dreeve-API-Anbindung: Die selbst gefahrenen
FIT-Dateien gehen über den eingerichteten Nextcloud-WebDAV-Zugang zur Analyse.
Andere Einstellungsdialoge einschließlich der Streckeneinstellungen sind noch nicht
auf das neue Design migriert. Kein GitHub-Push oder veröffentlichter APK-Release
in dieser Phase; der lokale Release-Build dient der Kompilierungsprüfung.

## Geänderte und neue Dateien

- `app/src/main/assets/Lucide-LICENSE.txt`
- `app/src/main/java/de/bcycle/app/BicycleDesign.kt`
- `app/src/main/java/de/bcycle/app/BicycleIcons.kt`
- `app/src/main/java/de/bcycle/app/FitLibrary.kt`
- `app/src/main/java/de/bcycle/app/FitRead.kt`
- `app/src/main/java/de/bcycle/app/GpxImport.kt`
- `app/src/main/java/de/bcycle/app/GpxLibrary.kt`
- `app/src/main/java/de/bcycle/app/MainActivity.kt`
- `app/src/main/java/de/bcycle/app/MapState.kt`
- `app/src/main/java/de/bcycle/app/RouteDetails.kt`
- `app/src/main/java/de/bcycle/app/RouteManagerView.kt`
- `app/src/main/java/de/bcycle/app/RouteUploads.kt`
- `app/src/main/java/de/bcycle/app/TrackState.kt`
- `app/src/main/res/drawable/ic_lucide_arrow_left.xml`
- `app/src/main/res/drawable/ic_lucide_bike.xml`
- `app/src/main/res/drawable/ic_lucide_calendar_days.xml`
- `app/src/main/res/drawable/ic_lucide_chart_no_axes_combined.xml`
- `app/src/main/res/drawable/ic_lucide_chevron_down.xml`
- `app/src/main/res/drawable/ic_lucide_circle_help.xml`
- `app/src/main/res/drawable/ic_lucide_clock.xml`
- `app/src/main/res/drawable/ic_lucide_cloud_upload.xml`
- `app/src/main/res/drawable/ic_lucide_download.xml`
- `app/src/main/res/drawable/ic_lucide_ellipsis.xml`
- `app/src/main/res/drawable/ic_lucide_ellipsis_vertical.xml`
- `app/src/main/res/drawable/ic_lucide_eye.xml`
- `app/src/main/res/drawable/ic_lucide_file.xml`
- `app/src/main/res/drawable/ic_lucide_loader_circle.xml`
- `app/src/main/res/drawable/ic_lucide_log_in.xml`
- `app/src/main/res/drawable/ic_lucide_mountain.xml`
- `app/src/main/res/drawable/ic_lucide_pencil.xml`
- `app/src/main/res/drawable/ic_lucide_smartphone.xml`
- `app/src/main/res/drawable/ic_lucide_trash_2.xml`
- `app/src/main/res/drawable/ic_lucide_x.xml`
- `app/src/test/java/de/bcycle/app/FitLibraryTest.kt`
- `app/src/test/java/de/bcycle/app/GpxManagerTest.kt`
- `app/src/test/java/de/bcycle/app/RouteExportTest.kt`
- `app/src/test/java/de/bcycle/app/RouteManagerViewTest.kt`
- `app/src/test/java/de/bcycle/app/TrackStateTest.kt`
- `docs/route-manager-phase2.md`
- `scripts/import_lucide.py`

## Korrektur nach visueller Rückmeldung

GPX/FIT werden ohne zusätzliches Datei-Icon dargestellt. Die sichtbaren Aktionsbuttons
stehen direkt nebeneinander neben den Messwerten, sofern der verfügbare Platz reicht.
Importierte Strecken reservieren keinen Cloud-Platz mehr. Die Korrektur wurde mit
6 erfolgreichen UI-/Navigationstests inklusive Kompilierung und nativen Ansichten
bei zwei Themes, schmalem Display und Landscape geprüft. Kein neuer APK-Build.
