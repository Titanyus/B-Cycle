# Kartenstil-Speichern korrigiert, neue APK ausdrücklich angefordert

Native Save/Cancel-Buttons waren von allgemeiner Dialognavigation verborgen; jetzt sichtbare Aktionen im Inhalt. Bright im Save-Dialog vorhanden und überschreibbar, eigener Bright-Snapshot wird in derselben Kartenquellenzeile geladen. Fünf gezielte Fälle mit Sichtbarkeit/realer Fläche sowie Stil-/Profil-/Backupabläufen grün, map-style-save-visible-bright-tests.log. [Details](vtm-map-editor.md). Nutzer hat neue APK angefordert; Version0.8.106/code176 für Actions151 vorbereiten, bauen und veröffentlichen.

# Aktueller Stand: APK0.8.105 / Build150

Alle vier Aufträge seit Build149 implementiert, gezielt geprüft und in der ausdrücklich angeforderten APK veröffentlicht. Keine offenen Implementierungsaufträge. Veröffentlichung und tatsächlicher direkter Raw-Download abgeschlossen; bytegleich zum Actions-Artefakt.

[APK](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.105-release-150.apk) · [Buildnachweis](build-150.md) · [Änderungen](release-0.8.105.md)

Actions42801216387 erfolgreich aus Quellstandfe75f58d811834a576765532819e636c4751e5f6, VersionCode175, Android13+. 4445809Bytes; SHA-256e66e2a432d1d4898dc0a6b88bba97b806b3b5d753a3797f913809cc6fb53c163. Zertifikat SHA-25694d34c46ede28d12e6d37efd8a7d953a75b621596ec2e5b68f61899b8ea7499b. Artefakt und tatsächlicher direkter Raw-Download bytegleich geprüft; Veröffentlichungec66542.

## Enthaltene Aufträge

- Trainingsvarianten: letzte identische Belastung ohne anschließende Erholung als vollständige Wiederholung erkennen. Warmup/Cooldown und tatsächliche Kennzahlen erhalten. Hochgeladene5x5-VO2max-ZWO als Regressionstest. Commit56f6c03, drei gezielte Fälle grün, training-final-effort-variants-tests.log. [Details](training-library.md).
- Dashboard-Effizienz: Darstellungsblock hinter deckenden Seiten aussetzen, sofortige Rückkehr, kompakte Dialoge/Status/Fehler/Fahrtabschluss weiter versorgen. Exakter GraphDemand-Cache, Kachel-/Graph-/Trainingsbindings nur bei eigenen Änderungen; Designinvalidierung. Commit1752bf3,16 gezielte Fälle grün; [Bericht mit Logs/korrigierten Testannahmen](dashboard-efficiency-2026-11-09.md).
- Interne Kartenstile: Save im Editor, neue Namen oder Überschreiben, Auswahl nach Bright in Vektorkarten. Globale Sammlung und unabhängige Profil-Arbeitskopien; vollständige Stil-/Ebenen-/Cycling-Snapshots, JSON und Backup erhalten. Commit1ff033d, sieben relevante finale Fälle grün; [Details/Logs](vtm-map-editor.md). Zwei fachfremde Alt-Testannahmen im ersten breiteren Editorlauf dokumentiert; keine grüne Gesamtsuite behaupten.
- Gemischte Live-Graphen: Gesamtachse/Kurve eigener unveränderlicher Snapshot, keine sekündliche Verkleinerung mit älterer Kurve. Commit77dc4cc, vier gezielte Methoden grün, nativer Test Normal/Sparen × Zeit/Distanz. [Bericht/Logs](mixed-graph-snapshots.md).

Intervalle unverändert1s kurz normal/5s kurz sparend/10s vollständig in beiden Modi. Sensoren, Rohdaten, FIT, WakeLocks und GL-Lebenszyklus unverändert. Kein Profiling/Gesamtlauf. Native Vorschauen sowie zentrale Typografie/UI-Katalog/Bright/Diff-Prüfungen bestanden.

## Offene Gerätekontrollen

Bediengefühl/Stildarstellung und Graphverhalten am Smartphone bestätigen. Frühere Bosch-Ladeinformationen weiterhin nicht am Nutzerrad bestätigt:0x00c4 öffentliche bool-Bestätigung,0x0089 nur benannt/Typ unbekannt; bei unbekanntem Zustand nutzergewählt Anroll-Fallback. Bestehende ältere Fahrtdubletten bleiben erhalten. [Vorheriger Build149](build-149.md) und Fachberichte enthalten die zugehörigen Nachweise.

APK ausschließlich über Actions, nur bei Nutzerauftrag. Kein lokaler APK-Build, keinen zusätzlichen Dispatch neben Push. Reine Veröffentlichung/Downloadnachweis mit[skip ci].
