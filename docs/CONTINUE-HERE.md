# Aktueller Stand: APK0.8.73 / Build114 veröffentlicht –31.10.2026

[APK direkt](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.73-release-114.apk) · [Buildnachweis](build-114.md) · [Änderungen](release-0.8.73.md). Code143, Quelle`bbb55c18b1b368a64d0b837e91e6aaaab1333761`, 2558517Bytes, SHA-256`a07cf52d771ac823e8ad191dbddc6dfd160066a75aa6f6734d286602f0b7c616`. CI-Prüfsumme, Signatur/Update-Kompatibilität, Manifest, Version und ZIP-Ausrichtung geprüft. APK-Auftrag erledigt. Offener Intervals-HRV422 beim Schreiben der Tagesdaten, siehe unten.

## Seit Build113

[Streckenliste](route-list-groups.md): Titel bleiben einzeilig/END-Ellipse vor dem Auge, auch nach echtem ListView-Recycling. Nach geändertem linken Inset oder ungültigem Textlayout explizit forceLayout/Neumessung mit endgültiger Breite; Textscroll zurücksetzen. Auswahlfarbe auf der ganzen Fahrtkarte einschließlich Innenabständen bis zu den Trennern, nicht nur auf innerem Container. Eigenständige Köpfe weiter nicht auswählbar.

Kein zusätzliches oberes Listenpadding; normale Monats-/Distanz-/Zeit-/Tempoköpfe und Fast-Scroller-Ziele stehen direkt an derselben Position wie angeheftete Köpfe. Normale und gecachte Sticky-Köpfe durchgehend TERTIARY, bei Themewechsel aktualisiert. Kopf-Innenpadding bleibt gleich.

## Prüfung und Historie

32 relevante native Tests in`/workspace/.tools/route-list-layout-final-tests.log`, nach dem erkannten Textlayoutfehler alle8 gezielten Nachprüfungen erfolgreich in`route-title-recycle-final-tests.log`. Scrollen weg/zurück in320/360/420dp, Ellipse vor Auge, Kopfposition aller Sortierungen, Pixelprüfung von TERTIARY und Auswahlrändern. Typografie/Atlas/Diff geprüft. Keine vollständige Gesamtsuite und kein physischer Gerätetest.

[Build113](build-113.md): eigenständige Sticky-Köpfe, Footer standardaus und Panel-Symbol links nebenInfo; Longpress temporär bis Auswahlende. Track-Augenfarbe nach Reattach erhalten, fünf Sortierkacheln. [Build112](build-112.md): Stressfrage und zusätzliche Intervals-Wellnessfelder/eigene Ergänzungen sowie422-Diagnose; noch kein privater Livebeweis für kontospezifischen422. Fitness/Ermüdung/Form nur als möglich beantwortet, kein Implementierungsauftrag.

## Offener Intervals-HRV422

Neue Nutzerrückmeldung währendBuild114: „Intervals lehnte die HRV-Anfrage ab · Tagesdaten übertragen (HTTP422)“, 0hochgeladen/0vorhanden/1fehlgeschlagen. Bestätigter Fehler liegt nun beim TagesPUT, nicht beim MonatsGET. Kein gesicherter Grund aus der bisherigen sicheren Diagnose; bekannte Feldnamen/Datentypen mit offiziellerOpenAPI verglichen, passend. ÖffentlicherAPI-Beitrag16966 zeigt erfolgreichenPUT ohneBody-ID aufDatums-URL; fehlendeID deshalb nicht als bewieseneUrsache behandeln. Keine blinden Feld-/Wertänderungen und keine Änderung amHRV-Code in dieserAPK.

Nutzer bestätigt betroffenesDatum2026-08-28 (gültig, nichtzukünftig). Noch offen: ob automatischeheutigeMessung weiterhin hochlädt. MitAntwort weiteruntersuchen. Keine privatenZugangsdaten oderLivepayload vorhanden. [HRV-Details](intervals-hrv.md).

APK nurGitHubActions; Appcode nichtmit[skip ci] überspringen, reine Veröffentlichungmit[skip ci]. DirektenRawDownload binär gegenArtefakt prüfen. Snapshot vorBereinigung alsTag`vor-den-bereinigungen-2026-10-30`.
