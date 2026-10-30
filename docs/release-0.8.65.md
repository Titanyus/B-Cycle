# B|CYCLE 0.8.65

Alle Änderungen seit Build105:

- Untere Dropdownrahmen der FIT-Aufzeichnung vollständig sichtbar; transparenter Hintergrund der Dashboard-Fahrradauswahl mit unveränderten Kachelfarben und vollständigen Kachelrahmen.
- Gemeinsamer8dpAbstand zwischen Überschrift und Positionskacheln für Benachrichtigungen und Trinkerinnerung.
- intervals.icu: täglicher automatischer HRV-Upload und eigener Sammelbutton für fehlende HRV-Tage. Vorhandene fremde HRV-Werte erhalten, nachweislich eigene unveränderte Werte automatisch aktualisierbar. Bestehendes Konto/WLAN-Regel gelten gemeinsam mit FIT-Uploads; keine Roh-RR/Notizen/Erfindung eines fremden Scores. [Zuordnung und Grenzen](intervals-hrv.md).
- Dreeve vor intervals.icu unterCloud; kompakte Angaben DreeveIN „FIT-Streckendaten | Wartungsintervalle“, intervalsOUT „FIT-Streckendaten | HRV-Daten“.
- Tagesbefinden: Vortagsfahrten und zeitgewichteter Puls liefern eine editierbare Trainingsvorauswahl: Z1leicht, Z2/3moderat, Z4/5schwer. Bestehende Tagesantworten gewinnen; ohne auswertbaren Puls keine Intensität raten.
- Subjektive Anstrengung nur bei nachgewiesenem oder ausgewähltem Training. „KeinTraining“ entfernt auch Kilometer, Minuten, Ride-IDs und RPE aus der Tagesantwort, Originalfahrten erhalten. [Details](hrv-questionnaire.md).

Prüfung:17gezielteUI-Fälle sowie69eindeutigeHRV-/API-/Upload-/Formular-/Zonen-/Navigationstests über gezielte Läufe erfolgreich. Native Rahmenpixel und Abstände inFlat/Glow, HTTP-Testserver, echter Messabschluss mit Outbox, gespeicherteFIT→Vorauswahl sowie bestehendeFIT-Uploadpfade geprüft. Typografie/Atlas/Diff erfolgreich. KeineGesamtsuite und kein neuerLive-Konto-/Gerätetest. Dreeve-Bestätigungsverzögerung nicht geändert.

[Buildnachweis und Download](build-106.md).
