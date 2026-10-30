# B|CYCLE 0.8.66

Änderungen seit Build106:

- Drei gespeicherte GPX-/FIT-Tracks gleichzeitig auf der Dashboard-Karte, feste Plätze1–3 mit je eigener Farbe, Breite und Deckkraft. Die laufende Aufzeichnung bleibt separat.
- Explizite farbige Platzwahl im Streckenmanager: langer Augendruck immer, kurzer Druck bei ausgeblendeter Strecke. Sichtbare Strecke kurz antippen blendet sie aus und gibt den Platz frei. Nummer hinter dem Auge, aktueller Platz in der Auswahl markiert; gewählter belegter Platz wird ersetzt.
- Dashboard: Hauptauge blendet alle aus/ein, langer Druck zeigt drei nummerierte Augen-Buttons in deckenden Trackfarben. Einzelnes Ausblenden erhält die Zuordnung. Filter „Auf Karte angezeigt“ erfasst alle sichtbaren Tracks.
- Auswahl, Stile und Sichtbarkeit je Fahrradprofil sowie in Einstellungen-Sicherung/Wiederherstellung erhalten. Bisheriger Track/Stil migriert aufPlatz1. Geometrien werden auf dem vorhandenen Worker geladen und gecacht; Originaldateien unverändert. [Details](three-map-tracks.md).
- Datenschutzmeldung verschiebt die linke Statusgruppe nicht mehr. Rechts25dp ohne Meldung,29dp bei passender Meldung. Echte Kameraaussparungen/Systeminsets bleiben berücksichtigt; auch das obere Fahrradlicht wird nicht wegen einer falsch links gemeldeten Privacyfläche ausgeblendet.
- Vorhandener Diagnoseexport enthält bei Menüöffnung aufgenommene Statusgeometrie für spätere Geräteprüfung; keine zusätzliche Hintergrundaufzeichnung oder Live-Symbolinhalte.

Prüfung:69eindeutige gezielte Track-/Karten-/Profil-/Sicherungs-/UI-Fälle sowie15Status-/Fahrradlichtfälle grün. NativeMehrlinienzeichnung mit getrennter Deckkraft, echteFIT/GPX, schnelleProfilwechsel, Sicherung und echte Popupbedienung geprüft; nativeButtonvorschau visuell geprüft. Typografie/Atlas/Diff erfolgreich. KeineGesamtsuite und keinneuerphysischerGerätetest; tatsächlichePoco-Systemkoordinaten weiterhin nicht belegt.

Buildnachweis folgt nach Veröffentlichung.
