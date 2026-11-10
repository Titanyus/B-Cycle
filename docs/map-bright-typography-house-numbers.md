# Bright, allgemeine Beschriftung und echte Hausnummern nach Build153

## Bright als gewöhnlicher Stil

Die bisher virtuelle, immer ergänzte Bright-Zeile entfällt. Einmalige Migration nimmt die unveränderte Bright-Vorlage mit normaler UUID in die gespeicherte Sammlung auf, sofern noch keine benannte Bright-Version existiert. Vorhandene Namen/UUIDs/Arbeitskopien bleiben erhalten. savedVectorStylesInitializedV2 ist global, nicht pro Fahrrad. Anschließend keine erneute automatische Ergänzung: Bright kann wie jeder andere gespeicherte Stil gelöscht, umbenannt und sortiert werden. Gelöschtes Bright auch nach MAPS-Backup/Restore nicht wieder anlegen; Sammlung im Restore gilt als maßgeblich und setzt den Migrationsmarker. Ungültige Sammlung weiterhin nicht still überschreiben.

Quellen- und Speicherdropdown listen nur echte gespeicherte Stile. Ohne passenden aktiven Stil und ohne Bright keine andere Vorlage als angeblich aktiv anzeigen: Arbeitskopie bleibt erhalten, Quellenauswahl zunächst leer; ein tatsächlicher Klick lädt den gewählten Stil. Neue Stile lassen sich im Save-Dialog weiterhin anlegen, wenn die Sammlung vollständig leer ist. Die unveränderte Bright-JSON bleibt technische Renderingbasis für Deltas, kein unlösbarer zusätzlicher Listeneintrag.

## Beschriftung

Bisherige Überschrift Ortsbeschriftung in „Beschriftung“ geändert. Bestehende city/town/village/hamlet behalten ihre Namen/Ordinalpositionen und Normalwerte. Ergänzt Länder/Regionen, Kontinente, sonstige Ortsnamen sowie Straßen-, Gewässer- und Hausnummerntexte. Alle Klassen erhalten die bisherigen Gewichts100–900 und Größen50–200%-Regler; Ausgabe weiterhin im validierten placeLabels-Block, dadurch JSON/Stilsammlung/Profile/Backup kompatibel mit bisherigen Daten. Neue Typografie auf tatsächliche native Text-Paints/Atlasmaße anwenden, einschließlich Wassertext, der keinen Straßen-/Ortsprioritätszweig durchläuft. Unveränderte Klassen weiterhin mit ursprünglichem Theme rendern.

## Hausnummern

Quelle überprüft: vorhandene originale OpenFreeMap-Kachel app/src/test/resources/maps/openfreemap-14-8663-5675.pbf enthält32 housenumber-Features mit Schlüssel housenumber. Auch weitere vorhandene14er-Kachel enthält26 Einträge. Der native MVT-Decoder behält den Wert; LabelTileLoaderHook liest beliebige TextStyle.textKey-Werte, nicht nur name/ref.

Bisherige Runtime-Regel begann erst ab VTM17. Anzeigegrenze auf14 vorgezogen, passend zu den verfügbaren Quellkacheln; Hausnummernebene weiterhin ausdrücklich einzuschalten und standardmäßig aus. Native Prüfung dekodiert jetzt die echte Kachel: alle32 Nummern haben an Zoom14 eine passende Textanweisung und einen nichtleeren, originalen Textwert. Keine Nummern aus Gebäuden/Namen erfinden. Das bestätigt Daten/Theme-Anbindung; lokale Dichte, Kollisionen und das tatsächliche Gerätebild bleiben praktisch zu prüfen. Keine unbelegte Behauptung, das konkrete vorherige Gerätebild vollständig reproduziert zu haben.

## Prüfung

14 gezielte Methoden grün: MapLibraryTextRevisionTest4, MapStyleLibraryTest5, bestehender Orts-Editorfall1, MapControlAccessTest4. Migration/Löschung ohne Wiederkehr und nach Backup, leere Bibliothek, allgemeiner Beschriftungseditor und native Größen für Straße/Wasser/Hausnummer, echte MVT-Nummern, Profile/Stile/Sicherung, bisherige2s-Geste/Verwaltung geprüft. Log map-library-text-complete-tests.log. Erster Durchgang map-library-text-final-tests.log: eine alte Fixture erwartete einen virtuellen Bright-Eintrag; um echte migrierte Vorlage ergänzt. map-library-migration-tests.log danach grün. Alte gezielte Funktionsfixtures, die bewusst eine leere Bibliothek brauchen, setzen diesen Zustand ausdrücklich statt implizit auf einen Sonderfall zu vertrauen.

Typografie/UI-Katalog/Bright/Diff grün. Keine Gesamtsuite, kein Profiling, kein neuer APK-Auftrag zu dieser Änderung. Versionsnummer unverändert, bis neue APK beauftragt ist. Keine Sensor-/Aufzeichnungs-/GL-Lebenszyklusänderung.
