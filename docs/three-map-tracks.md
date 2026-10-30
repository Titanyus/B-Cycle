# Drei farblich zugeordnete Tracks auf der Dashboard-Karte

Umgesetzt nach Build106. Die nachträglich bestätigte explizite Platzwahl im Streckenmanager ersetzt dort die zunächst vorgeschlagene automatische FIFO-Auswahl.

## Bedienung

- Drei feste Plätze1–3 für alle lokalen GPX-Strecken und FIT-Fahrten mit darstellbaren GPS-Daten. Fahrten/Zusammenfassungen ohne Geometrie bleiben wie bisher nicht auf der Karte auswählbar.
- Im Streckenmanager: kurzer Druck auf eine sichtbare Strecke entfernt sie aus ihrem Platz. Kurzer oder langer Druck auf eine nicht sichtbare Strecke öffnet drei deckend farbige Buttons ohne Nummern. Langer Druck auf eine sichtbare Strecke öffnet dieselbe Auswahl mit markiertem aktuellem Platz (kontrastierender stärkerer Rahmen). Ziel wählen ersetzt dessen bisherigen Track; Umplatzieren gibt den alten Platz frei. Dieselbe Strecke belegt nie zwei Plätze. Das Auge übernimmt die deckende Trackfarbe, ohne Nummer dahinter. Belegte Ziele zeigen ein Auge, freie Ziele sind reine Farbflächen. Der aktuelle Platz bleibt durch den stärkeren Rahmen markiert. Namen bleiben in der bestehenden Zeile.
- Dashboard: kurzer Druck auf das Hauptauge blendet alle aus bzw. alle wieder ein. Langer Druck öffnet darunter drei vertikal angeordnete Buttons nur mit Auge. Ihre Hintergrundfarben entsprechen den Platzstilen, immer100%Deckkraft unabhängig von der Linien-Deckkraft. Auge/Schrift verwenden kontrastierendes Weiß oder Schwarz. Individuelles Ausblenden behält Platz, Stil und Zuordnung. Nach gemeinsamem Ausblenden kann ein einzelner Button nur seinen Track wieder aktivieren.
- Der Filter „Auf Karte angezeigt“ erfasst alle tatsächlich sichtbaren Tracks, keine nur zugeordneten ausgeblendeten Tracks.
- Lange Streckenzeilen-Drücke zur Mehrfachlöschung bleiben unverändert, Augen-Longpress ist ein eigenes Touchziel. Popup schließt bei Außenklick/Zurück/Seitenwechsel; Kartenpopup beim Dashboard-Editor oder Pause. Profilwechsel verwirft ein altes Popup.

## Stile, Speicherung und Migration

MapLineStyleView bietet laufende Aufzeichnung plus Track1/2/3 als unabhängige bestehende Linien-Accordions. MapRouteStyles behält recorded/saved und ergänzt saved2/saved3; die bisherige gespeicherte Linienkonfiguration wird Track1. Farbe, Breite, Deckkraft und Nacht-Rückfälle bleiben zentral. Bestehende Linien-/Positions-/Höhen-/Pinch-Zeichnung unverändert; drei gespeicherte Linien erhalten ihre eigenen Stile und bisherigen Start-/Endmarkierungen.

MapRouteSlotsV1 sichert drei IDs, Einzelsichtbarkeit und Auswahlreihenfolge. Leere Plätze bleiben erhalten, keine Umnummerierung nach Löschen. Programmatische Standardauswahl (z.B. bisherige Importpfade) verwendet einen freien Platz bzw. ersetzt den ältesten belegten Platz; die explizite Managerwahl verwendet ausschließlich den gewählten Platz. Sichtbarkeit verändert das Alter nicht. Die bisherige einzelne GPX-/FIT-Auswahl migriert in Platz1; displayGpxId/displayFitId bleiben als Kompatibilitätsreferenz auf einen primären Track erhalten.

MapRouteSlots.KEY und MapRouteStyles.key gehören zum Karten-Profilumfang. Fahrradwechsel/Kopie, reine Kartensicherung und verschlüsselte Gesamteinstellungssicherung enthalten Plätze/Einzelsichtbarkeit/Reihenfolge/Stile aller Profile. Aktive und inaktive Profilsnapshots werden validiert; unzulässige IDs, Duplikate, falsche Arraylängen/Versionen und falsche Sichtbarkeitstypen vor Restore ablehnen. Ältere Kartensicherung ohne Plätze entfernt die neue aktive Konfiguration und nutzt die vorhandenen Einzelreferenzen. Fehlende/gelöschte lokale Strecken geben ihre Plätze frei; Originaldateien werden nicht verändert und nicht in die Einstellungssicherung eingebettet.

## Datenpfad und Leistung

Ein bestehender Bibliotheksworker dekodiert ausgewählte GPX-/FIT-Geometrien. Cache nach ID/Dateipfad/Länge/Änderungszeit, auf die drei ausgewählten Geometrien begrenzt. Reine Sichtbarkeitsänderungen dekodieren keine Datei. Keine FIT-Datei auf dem UI- oder Kartenzeichen-Thread lesen. Unveränderliche Linien-/Stilsnapshots an Mapsforge; GPS und laufender Track separat. Kameraeinpassung umfasst die sichtbaren ausgewählten Strecken. Verborgene Karten pausieren weiterhin wie bisher.

Publikation der Auswahl und Kompatibilitätsreferenzen auf dem UI-Thread. Profilgeneration verhindert das Zurückschreiben eines alten Dekodierergebnisses nach einem Fahrradwechsel; Kartenoverlays werden beim Wechsel sofort entfernt. Während eines Ladens geänderte reine Sichtbarkeit bleibt erhalten. Alte Auswahl-Snapshots dürfen nicht das aktuelle Profil überschreiben.

## Prüfung

69eindeutige gezielte Fälle grün: ThreeMapTracksTest12, MapRouteStyleTest4, RouteManagerViewTest4, RouteQueryTest7, RouteQueryUiTest5, RouteTrackLayerTest6, MapPanelTest10, SettingsBackupTest17, SettingsBackupSupplementTest4. Gruppen über mehrere Läufe, keine Doppelzählung der Wiederholungen. Geprüft: freie/ersetzte/umplatzierte Slots, Duplikat-/Versionvalidierung, gemischte echteFIT/GPX, unveränderte Originalbytes, Cachewiederverwendung, Löschen, Profilwechsel einschließlich schneller Wechsel bei wartendem Worker, aktive/inaktive Sicherungslogik, individuelle Farben/Alpha und gleichzeitige laufende Linie, echte Dashboard- und Manager-Popup-Bedienung, Nummern/Recycling/Filter. Native Vorschau [Buttons](previews/map-route-slot-buttons.png).

Erste Testdaten hatten einen FIT mit ausschließlich voneinander getrennten GPS-Punkten und einen falsch benannten Backupabschnitt; Fixtures korrigiert. Ein älterer Toolbar-Test erwartete SECONDARY statt der bereits früher beauftragten TERTIARY-Footerfarbe; Erwartung aktualisiert, keine Toolbar-Gestaltung dafür geändert. Finale Testläufe erfolgreich. Logs in /workspace/.tools/three-map-tracks-{tests,integration-tests,refined-tests,final-tests}.log. Typografie/Atlas/Diff geprüft. Keine Gesamtsuite und kein neuer physischer Gerätetest.


Veröffentlichung 30.10.2026: Änderungen nach Build 106 sind in **APK 0.8.66 / Build 107** enthalten. [Download und Buildnachweis](build-107.md). Frühere Angaben zu fehlender APK sind Umsetzungshistorie.


Nach Build 107: Darstellung ohne sichtbare Platznummern, vertikale Dashboard-Auswahl, trackfarbige Manager-Augen sowie Belegt-Augen in der weiterhin horizontalen Platzwahl. Bedienung und Persistenz unverändert. 26 gezielte Fälle (ThreeMapTracksTest 12, RouteManagerViewTest 4, MapPanelTest 10) erfolgreich; native Vorschau aktualisiert und visuell geprüft. Typografie/Atlas/Diff erfolgreich. Kein neuer physischer Gerätetest.
