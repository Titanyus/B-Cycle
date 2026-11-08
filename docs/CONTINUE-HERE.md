# Neue Korrektur nach Build149 (noch nicht in einer APK)

Nutzerauftrag: Varianten auch für die hochgeladene 5x5-VO2max-ZWO erkennen, die nach der fünften Belastung direkt ausrollt. TrainingVariants erkennt nun einen letzten identischen Belastungspräfix ohne abschließende Erholung, zählt ihn als fünfte Wiederholung und erhält dieses Ende bei Quick+/Quick/Std/Ext/Ext+. Vollständige Paare, Warmup/Cooldown, Kennzahlen und Importoriginale bleiben erhalten. Beispieldatei als Testfixture, drei gezielte Struktur-/Kennzahl-/Grenzfalltests grün (training-final-effort-variants-tests.log). Kein APK-Auftrag seit dieser Korrektur, kein neuer Build/Push. [Details](training-library.md).

# Aktueller Arbeitsstand: APK0.8.104 / Build149 veröffentlicht

Alle folgenden Aufträge sind vollständig implementiert, gezielt geprüft und in der ausdrücklich angeforderten APK0.8.104/code174/Build149 enthalten. Veröffentlichung und tatsächlicher direkter Raw-Download abgeschlossen; bytegleich zum geprüften Actions-Artefakt.

- **Doppelimport beim Fahrtabschluss:** exakte Exporthash-Zuordnung vor lokaler Veröffentlichung, eine RPE-Datei bleibt eine Fahrt nach Scan/Neustart, alte Versionen und Löschschutz. Vorhandene ältere Duplikate unverändert erhalten. [Ursache/Tests/Grenzen](fit-feedback-export-identity.md), Commit841a08e.
- **Fahrtzusammenfassung:** ab früherer Trennerposition unter Akkuwerten durchgehend TERTIARY, Linie unsichtbar, bisherige Geometrie/Abstände erhalten; Buttons explizite TERTIARY-Rolle. [Ablauf](ride-summary-save-flow.md), Commit3b3672d.
- **Bosch-Autostart:** Ladegerät/Laden sperrt; initiale1,5s-Statusphase, bei fehlendem Signal nutzergewählt erst bei frischem Anrollen, nach erkanntem Laden erst neue Speedgeneration. Service prüft erneut. Indoor/manueller Start erhalten. [Dokumentation/Quellen/Gerätegrenze](bosch-charging-autostart.md), Commit6207f5d.
- **Dashboardkarte:** Buttons TERTIARY/100%, deaktivierte Symbole/Text statt gesamtem Button dimmen; Windrosen-Nordpfeil PRIMARY, vorhandene Windrosen-Geometrie/Alpha erhalten. Commit138c62e.
- **Training:** echte Touchfläche der Variantenpille, vertikales Scrollen erhalten, nicht verfügbare Varianten erklären statt Details zu öffnen. Labels Quick+→Quick→Std→Ext→Ext+. Details nur Übersicht, Rohdaten bleiben gespeichert. [Aktueller Umfang/Prüfung](training-library.md), Commit5ad4080.
- **Karteneditor:** links Innenfläche/rechts Straßen-Outline, Farbe/Deckkraft/echte Randbreite unabhängig, kompakte farbige RGBA-Regler,38dp statt48dp Pitch, zwei Farbfelder und Texte entfernt. JSON/Profil/Backup und Livefarbenpfad erweitert. [Formel/Prüfung](vtm-map-editor.md).

## Prüfnachweise

Nur erforderliche gezielte Läufe, keine Gesamtsuite/Profiling. Unter /workspace/.tools:

- ride-export-duplicate-before.log (Fehler exakt reproduziert), ride-export-duplicate-fixed.log plus ride-export-duplicate-final.log und ride-export-identity-deletion-test.log (10 unterschiedliche finale Speicherfälle grün).
- summary-tertiary-lower-tests.log: vorhandene Summary-/Aufnahme-UI-Fälle grün, Vorschau ride-summary.png geprüft.
- charging-autostart-map-controls-tests.log plus charging-autostart-map-controls-final-tests.log: Protokoll/LiveData/Runtime/Lade-/Service-Race und Kartenfarben grün; zwei alte UI-Testannahmen auf heutige Elemente eingegrenzt.
- training-pill-touch-overview-tests.log, training-pill-native-touch-scroll-test.log und training-pill-unavailable-touch-test.log: echte Touchfolge/Scrollabbruch/fehlende Varianten sowie Detaildaten/Navigation grün.
- map-outline-editor-tests.log und map-outline-editor-native-final-tests.log: drei Editorfälle, davon zwei für alle vier Straßenklassen und Profil-/Backup-Persistenz erneut grün; Vorschau map-editor-outline.png geprüft.

Typografie/UI-Katalog/Bright/Diff grün. Hardware-Ladeadressen noch am Nutzerrad bestätigen:0x00c4 in öffentlicher Registry als bool bestätigt,0x0089 dort nur benannt/Typ unbekannt; unbekannte Kodierung bleibt unbekannt, Anroll-Fallback schützt. Kein unbelegter vollständiger Hardware-Nachweis. Optischer neuer Editorabgleich am Gerät noch offen.

# Letzte veröffentlichte APK:0.8.104 / Build149

[APK](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.104-release-149.apk) · [Buildnachweis](build-149.md) · [Änderungen](release-0.8.104.md)

Actions42499777399 erfolgreich aus Quellstand6be53f297ed502ca730b3289e695be35bb529d87, VersionCode174, Android13+. Dauerhafte Signatur SHA-25694d34c46ede28d12e6d37efd8a7d953a75b621596ec2e5b68f61899b8ea7499b. 4445389Bytes; APK-SHA-25624e8dcec0c31344295eb1ca76d3d0833fbaeaa85a1f977090822c6de1ac9b815. Artefakt und tatsächlichen Raw-Direktdownload bytegleich geprüft; Veröffentlichung3a809b3.

Die neue Korrektur oben ist noch nicht in Build149 enthalten. Bosch-Ladeinformationen und optischer Editorabgleich am Gerät bleiben als Gerätegrenzen dokumentiert. APK ausschließlich Actions, keine lokale APK und keinen zweiten Dispatch neben Push.
