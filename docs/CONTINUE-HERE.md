# Aktueller Stand: APK0.8.72 / Build113 veröffentlicht –31.10.2026

[APK direkt](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.72-release-113.apk) · [Buildnachweis](build-113.md) · [Änderungen](release-0.8.72.md). Code142, Quelle`0976e258fba3f74b30b4e62835b2399352513ed1`, 2558385Bytes, SHA-256`7d92c44ab5755d7486fc2cb98e27e5cebcc5bb4fb10e0479d2620018e6f55dd3`. CI-Prüfsumme, Signatur/Update-Kompatibilität, Manifest, Version und ZIP-Ausrichtung geprüft. Letzter veröffentlichter Build; seitdem lokale Darstellungskorrekturen, siehe unten. Kein neuer Buildauftrag.

## Seit Build112

[Streckenliste](route-list-groups.md): Monats-/Bereichsköpfe als eigenständige nicht auswählbare Adaptereinträge mit eigenem Viewtyp. Fahrtmarkierung auf Fahrt begrenzt. Sticky-Kopf mit Kennzahlen unter Hauptheader, nächster Kopf schiebt ihn weg; anfänglicher Seitenabstand scrollt weg. Gecachte Zeichenview/Geometrie, bei Themenwechsel erneuern; Pan/Fling auch auf dem angehefteten Kopf, kein Durchklicken auf verdeckte Fahrten. Fast-Scroller-Positionen berücksichtigen zusätzliche Köpfe.

Footer standardaus. Panel-bottom-Symbol links neben Info schaltet manuell; Longpress blendet temporär bis Auswahlende ein. Manuelle Einblendung bleibt nach Auswahlende erhalten; manuelles Ausblenden auch während Auswahl möglich. Neue Seite beginnt wieder verborgen. Auswahl-/Zurück-/Löschlogik unverändert.

Track-Augen: generische Iconbindung überschreibt die Trackfarbe bei Reattach. Zusätzlicher Icon-Farbrefresh stellt aktuelle Trackplatzfarbe nach Standardbindung wieder her; Schleifen aus Detach/Reattach/Recycling mit belegten/leeren Plätzen geprüft. Farben deckend, Sichtbarkeitslogik unverändert.

Sortierpopup: seitenbreite einzeilige Auswahl im Fahrtbenennungsstil, vier Kriterien plus Richtung. Globale Inline-Abstandsregel verhindert vertikal versetzte Kacheln; native Geometrie bei320/360/480dp in beiden Designs. Änderungen sofort, vorhandene Filter bleiben erhalten.

## Prüfungen und offene Grenzen

Logs`/workspace/.tools/`: `route-sticky-colour-footer-final-tests.log`67 erfolgreich; `route-sticky-sort-release-tests.log`81 relevante Prüfungen, darin3 Sortier-/Adaptererwartungen anschließend korrigiert und alle8 gezielten Nachprüfungen erfolgreich in`route-sort-geometry-final-tests.log`. FinaleKachelvorschau visuell geprüft; Typografie/Atlas/Diff erfolgreich. Keine vollständige Gesamtsuite oder physischer Gerätetest.

[Build112](build-112.md): HRV-Ring ohneEndpunkt, Streckengruppen, Stressfrage und zusätzliche Intervals-Wellnessfelder/eigene Ergänzungen sowie422-Diagnose. Noch kein privater Livebeweis für Behebung des kontospezifischen422. Fitness/Ermüdung/Form nur als möglich beantwortet, kein Implementierungsauftrag.

APK ausschließlichGitHubActions; Appcode nichtmit[skip ci] überspringen, Veröffentlichungscommitmit[skip ci]. DirektenRawDownload binär gegenArtefakt prüfen. Snapshot vorBereinigung alsTag`vor-den-bereinigungen-2026-10-30`.

## Nach Build113 – lokal, noch unveröffentlicht

Streckenliste: kein zusätzliches oberes Listenpadding mehr; Gruppenköpfe stehen beim Einstieg und Fast-Scroll direkt an derselben Position wie angeheftet. TERTIARY für normale und gecachte Sticky-Köpfe. Auswahlfarbe auf der ganzen Fahrtkarte samt Innenabständen bis zu den Trennern, eigenständige Köpfe weiter nicht selektierbar. Titel einzeilig/END-Ellipse ohne horizontalen Textscroll; geänderten linken Inset im Layout direkt neu messen, damit Recyclen die Kürzung vor dem Auge nicht verliert. 32 relevante native Tests über`route-list-layout-final-tests.log` und anschließend8 erfolgreiche Titel-/Geometrie-Nachprüfungen in`route-title-recycle-final-tests.log` abgeschlossen. Der Recyclingtest deckte ein ungültiges Textlayout auf; explizites forceLayout vor exakter Neumessung behebt dies. Typografie/Atlas/Diff geprüft. Keine neueAPK beauftragt.
