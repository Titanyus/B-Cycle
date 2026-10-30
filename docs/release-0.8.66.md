# B|CYCLE 0.8.66

Änderungen seit Build106:

- Drei gespeicherte GPX-/FIT-Tracks gleichzeitig auf der Dashboard-Karte, feste Plätze 1–3 mit je eigener Farbe, Breite und Deckkraft. Die laufende Aufzeichnung bleibt separat.
- Explizite farbige Platzwahl im Streckenmanager: langer Augendruck immer, kurzer Druck bei ausgeblendeter Strecke. Sichtbare Strecke kurz antippen blendet sie aus und gibt den Platz frei. Nummer hinter dem Auge, aktueller Platz in der Auswahl markiert; gewählter belegter Platz wird ersetzt.
- Dashboard: Hauptauge blendet alle aus/ein, langer Druck zeigt drei nummerierte Augen-Buttons in deckenden Trackfarben. Einzelnes Ausblenden erhält die Zuordnung. Filter „Auf Karte angezeigt“ erfasst alle sichtbaren Tracks.
- Auswahl, Stile und Sichtbarkeit je Fahrradprofil sowie in Einstellungen-Sicherung/Wiederherstellung erhalten. Bisheriger Track/Stil migriert auf Platz 1. Geometrien werden auf dem vorhandenen Worker geladen und gecacht; Originaldateien unverändert. [Details](three-map-tracks.md).
- Datenschutzmeldung verschiebt die linke Statusgruppe nicht mehr. Rechts 25 dp ohne Meldung, 29 dp bei passender Meldung. Echte Kameraaussparungen/Systeminsets bleiben berücksichtigt; auch das obere Fahrradlicht wird nicht wegen einer falsch links gemeldeten Privacyfläche ausgeblendet.
- Vorhandener Diagnoseexport enthält bei Menüöffnung aufgenommene Statusgeometrie für spätere Geräteprüfung; keine zusätzliche Hintergrundaufzeichnung oder Live-Symbolinhalte.

Prüfung: 69 eindeutige gezielte Track-/Karten-/Profil-/Sicherungs-/UI-Fälle sowie15 Status-/Fahrradlichtfälle grün. Native Mehrlinienzeichnung mit getrennter Deckkraft, echte FIT/GPX, schnelle Profilwechsel, Sicherung und echte Popupbedienung geprüft; native Buttonvorschau visuell geprüft. Typografie/Atlas/Diff erfolgreich. Keine Gesamtsuite und kein neuer physischer Gerätetest; tatsächliche Poco-Systemkoordinaten weiterhin nicht belegt.

[Buildnachweis und Download](build-107.md).
