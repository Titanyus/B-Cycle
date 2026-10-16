# ZIP-Archivimport

Unter **Strecken → Strecke importieren → ZIP-Archiv importieren** ein lokales Archiv über die Android-Dokumentauswahl wählen. Die bisherigen Einzelimporte bleiben erhalten.

- FIT und GPX in beliebigen ZIP-Unterordnern, auch `.fit.gz` / `.gpx.gz` (Strava-Export).
- Andere Formate (insbesondere TCX), Bilder, CSV-Metadaten und verschachtelte ZIPs werden nicht interpretiert; die Abschlussbilanz zählt nicht unterstützte Dateien.
- Originalbytes der entpackten FIT-/GPX-Dateien bleiben unverändert. Dateibasename als Titel; FIT-Fahrtdatum bzw. erste verfügbare GPX-Punktzeit übernehmen. Ohne GPX-Zeit bleibt der bisherige Importzeit-Fallback.
- Keine Änderung der ausgewählten Dashboard-Strecke. Importierte Dateien behalten Herkunft IMPORTED, auch wenn es historische eigene Fahrten sind.
- Datei-Hash verhindert byteidentische Duplikate innerhalb des Archivs und gegenüber vorhandenen privaten Originalen. FIT verwendet seine bestehende Hash-ID, GPX behält stabile UUIDs. Bereits umbenannte Einträge und ihre Herkunft bleiben erhalten. Verschieden codierte Versionen derselben Fahrt sind keine byteidentischen Duplikate.
- Verarbeitung auf dem vorhandenen seriellen Strecken-Worker, jeweils nur eine temporäre Datei. Fortschritt gedrosselt inline, Abschlussbilanz bleibt im Manager sichtbar. Einzelne ungültige Dateien überspringen; bei beschädigtem ZIP-Strom oder erreichtem Gesamtlimit bleiben bereits erfolgreich gespeicherte Fahrten erhalten. Wiederholung überspringt diese.
- Keine ZIP-Pfade auf das Dateisystem übertragen. Private zufällige temporäre Dateinamen, Bereinigung bei Erfolg/Fehler. Bestehende Parsergrenzen: FIT 100 MiB, GPX 20 MiB. Archivgrenzen: 100.000 Einträge, 10 GiB entpackte ZIP-Bytes bzw. zusätzlich expandierte GZIP-Nutzdaten. Unbekannte Einträge werden ebenfalls begrenzt gelesen.

## Cloud-Verhalten

**Archivimport ist ausschließlich lokal und startet keinerlei Upload.** WebDAV, Intervals.icu, Strava und Dreeve werden nicht automatisch angesprochen, unabhängig von ihren Einstellungen für neue Aufnahmen. Archivimporte erzeugen keine erfundenen Cloud-IDs oder Erfolgsmeldungen.

Für später: gezielter Massenexport zu einem ausgewählten Dienst (z. B. nur das noch leere Intervals.icu). Vorhandene bestätigte BICYCLE-Uploadbelege und Dienst-/Konto-IDs wiederverwenden. Aktivitäten, die außerhalb von BICYCLE hochgeladen wurden, benötigen zusätzlich einen belastbaren dienstseitigen Abgleich; lokaler Datei-Hash allein reicht dafür nicht. Keine garantierte dienstübergreifende Duplikaterkennung behaupten. Dieser Massenexport ist noch nicht implementiert.

## Prüfung

RouteArchiveImportTest: gemischte FIT/GPX/GZIP-Archive, Originalbytes und Datum, neue und bestehende Duplikate, beschädigte Einträge, Pfadsicherheit, Größen-/Anzahlgrenzen, wiederholter Import, persistente Bibliotheken und unveränderte Auswahl. RouteManagerViewTest: Fortschritt/Abschluss inline neben vorhandenen Listeninteraktionen. Bestehende FitLibraryTest/GpxLibraryTest weiter ausführen. Echtes Strava-Gesamtarchiv noch nicht erprobt.

Validierung am 16.10.2026: 17 gezielte Tests aus den vier genannten Testklassen, keine Fehler/übersprungenen Tests; `:app:assembleRelease` einschließlich R8 und Lint Vital erfolgreich. Keine neue Test-APK veröffentlicht; die veröffentlichte 0.7.25 bleibt unverändert.
