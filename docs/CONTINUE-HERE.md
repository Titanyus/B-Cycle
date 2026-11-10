# Neue Änderungen und APK-Auftrag nach Build151

Natur und Stadtflächen unabhängig schaltbar (URBAN), Ortsnamen mit zweispaltigem Füll-/Kontur-RGBA und Konturstärke. JSON/benannte Stile/Backups erhalten. Drei gezielte native/UI-/Persistenzfälle grün, map-place-outline-tests.log; Details docs/vtm-map-editor.md. APK0.8.107/code177 für Actions152 vorbereiten und veröffentlichen; ausdrücklicher Auftrag vorhanden.

# Aktueller Stand: APK0.8.106 / Build151

Gemeldeten Kartenstil-Speicherfehler behoben und ausdrücklich angeforderte APK veröffentlicht. Keine offenen Implementierungsaufträge. Veröffentlichung und tatsächlicher direkter Raw-Download abgeschlossen; bytegleich zum Actions-Artefakt.

[APK](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.106-release-151.apk) · [Buildnachweis](build-151.md) · [Änderungen](release-0.8.106.md)

Actions42877730181 erfolgreich aus Quellstand631c960d173f0bde231dc5a531739a1f65ef6469, VersionCode176, Android13+. 4445809Bytes; SHA-25614c4737626c310f9afed8acab26462fae3fb05a8c6fb35bf88b03c0ca13f681b. Zertifikat SHA-25694d34c46ede28d12e6d37efd8a7d953a75b621596ec2e5b68f61899b8ea7499b. Artefakt und tatsächlichen Raw-Direktdownload bytegleich geprüft; Veröffentlichungaee1e87.

## Korrektur nach Build150

Native Standardbuttons waren von BicycleNavigation.hideNavigationFooter verborgen worden. Jetzt eigene zentrale Save/Cancel-Aktionen nebeneinander im Dialoginhalt. Vorherige Tests hatten unsichtbare Buttons programmgesteuert ausgelöst; jetzt isShown/getGlobalVisibleRect prüfen. Bright im Save-Dialog als Vorlage/Überschreibziel; normaler validierter Bright-Snapshot mit stabiler ID wird in derselben MapSourcePicker-Zeile geladen, keine doppelte Bright-Zeile. Commit6dfbca6. Fünf gezielte Fälle grün (map-style-save-visible-bright-tests.log), Typografie/UI-Katalog/Bright/Diff grün. [Details](vtm-map-editor.md).

## Enthaltene Aufträge

- Trainingsvarianten: letzte identische Belastung ohne anschließende Erholung als vollständige Wiederholung erkennen. Warmup/Cooldown und tatsächliche Kennzahlen erhalten. Hochgeladene5x5-VO2max-ZWO als Regressionstest. Commit56f6c03, drei gezielte Fälle grün, training-final-effort-variants-tests.log. [Details](training-library.md).
- Dashboard-Effizienz: Darstellungsblock hinter deckenden Seiten aussetzen, sofortige Rückkehr, kompakte Dialoge/Status/Fehler/Fahrtabschluss weiter versorgen. Exakter GraphDemand-Cache, Kachel-/Graph-/Trainingsbindings nur bei eigenen Änderungen; Designinvalidierung. Commit1752bf3,16 gezielte Fälle grün; [Bericht mit Logs/korrigierten Testannahmen](dashboard-efficiency-2026-11-09.md).
- Interne Kartenstile: Save im Editor, neue Namen oder Überschreiben, Auswahl nach Bright in Vektorkarten. Globale Sammlung und unabhängige Profil-Arbeitskopien; vollständige Stil-/Ebenen-/Cycling-Snapshots, JSON und Backup erhalten. Commit1ff033d, sieben relevante finale Fälle grün; [Details/Logs](vtm-map-editor.md). Zwei fachfremde Alt-Testannahmen im ersten breiteren Editorlauf dokumentiert; keine grüne Gesamtsuite behaupten.
- Gemischte Live-Graphen: Gesamtachse/Kurve eigener unveränderlicher Snapshot, keine sekündliche Verkleinerung mit älterer Kurve. Commit77dc4cc, vier gezielte Methoden grün, nativer Test Normal/Sparen × Zeit/Distanz. [Bericht/Logs](mixed-graph-snapshots.md).

Intervalle unverändert1s kurz normal/5s kurz sparend/10s vollständig in beiden Modi. Sensoren, Rohdaten, FIT, WakeLocks und GL-Lebenszyklus unverändert. Kein Profiling/Gesamtlauf. Native Vorschauen sowie zentrale Typografie/UI-Katalog/Bright/Diff-Prüfungen bestanden.

## Offene Gerätekontrollen

Bediengefühl/Stildarstellung am Smartphone bestätigen. Frühere Bosch-Ladeinformationen weiter nicht am Nutzerrad bestätigt; Anroll-Fallback bei unbekanntem Status. Bestehende ältere Fahrtdubletten bleiben erhalten. [Build150](build-150.md) und Fachberichte enthalten frühere Prüfnachweise/Gerätegrenzen.

APK ausschließlich Actions und nur bei Auftrag. Kein lokaler APK-Build, keinen zusätzlichen Dispatch neben Push. Reine Veröffentlichung/Downloadnachweis mit[skip ci].
