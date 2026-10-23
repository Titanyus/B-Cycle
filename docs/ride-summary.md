# Zusammenfassung nach dem Speichern

Nach dem Beenden einer Fahrt mit Speichern öffnet die App auf dem Dashboard den kompakten, zentrierten Dialog **Deine Daten**. Bestehende Dialoge und die Karten-Vollansicht werden dafür geschlossen. Der Standard-Zurück-Chevron/Android-Zurück schließen die Zusammenfassung.

Die Anzeige verwendet unverändert `AnalysisMetricTile` und die gespeicherte Auswertung von `MapState.loadAnalysis` wie in den Streckendetails:

- Zeile 1: Distanz, Bewegungszeit, Durchschnittstempo.
- Zeile 2: Durchschnittspuls, normalisierte Leistung.

Fehlende Kennzahlen bleiben „–“. Während des asynchronen Ladens erscheint ein kurzer Ladehinweis. Ein Analysefehler ändert den bereits erfolgreichen Speicherstatus nicht; die Fahrt bleibt in der Sammlung.

`RideRuntime` hält das noch nicht angezeigte `RideCompletion` im Speicher, auch bei einem Hintergrundwechsel. Die Anzeige wird beim Öffnen genau einmal verbraucht, unabhängig von späteren Render-/Statusmeldungen. Nur `Finished` mit vorhandener Fahrt-ID öffnet sie; nicht gespeicherte Fahrten behalten die bestehende Fehlermeldung. Automatische Wiederherstellung älterer Aufnahmen öffnet keine neue Zusammenfassung. Keine neuen FIT-/Journal-/Profilformate.

Unter den Kennzahlen steht der Speicherstatus einschließlich Teilfehlern/Warnungen. Die beim Speichern vorgemerkten Cloudziele (WebDAV, intervals.icu, Dreeve, Strava) werden anhand ihrer bestehenden Belege dargestellt: ausstehend, läuft, hochgeladen, fehlgeschlagen. Vormerken/HTTP-Annahme ist kein erfolgreicher Upload. Der Dialog beobachtet die bestehenden Upload-/Einstellungs-Preferences nur während er sichtbar ist und aktualisiert den Text ohne Polling, Hashing oder Netzarbeit auf dem UI-Thread. Die entsprechenden bisherigen Abschluss-Toasts entfallen; andere Speicherfehler-/Wiederherstellungsmeldungen bleiben erhalten.

Geprüft am 23.10.2026: `RideSummaryTest` mit synthetischer FIT-Aufnahme, Abgleich aller fünf Kacheln mit der gespeicherten Analyse, zwei Zeilen in drei Hochformatbreiten, einmaliger Anzeige nach Hintergrundwechsel und Aktualisierung eines WebDAV-Belegs von ausstehend zu hochgeladen. Zusätzlich gemischte Dienstzustände und lokale Teilfehler; keine echten Netzwerk-Uploads oder neuen Dateiformate. Native Vorschau visuell geprüft. HRV-Korrektur durch bestehenden nativen Rendering-Test geprüft.

Stand 23.10.2026: die beschriebenen neuen Funktionen sind in APK **0.8.31 / Build 70** enthalten. [Buildnachweis](build-70.md). Frühere Angaben zu fehlender APK sind Verlauf.
