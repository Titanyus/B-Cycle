# Zusammenfassung nach dem Speichern

Nach dem Beenden einer Fahrt mit Speichern öffnet die App auf dem Dashboard den kompakten, zentrierten Dialog **Zusammenfassung**. Bestehende Dialoge und die Karten-Vollansicht werden dafür geschlossen. Der Standard-Zurück-Chevron/Android-Zurück schließen die Zusammenfassung.

Die Anzeige verwendet unverändert `AnalysisMetricTile` und die gespeicherte Auswertung von `MapState.loadAnalysis` wie in den Streckendetails:

- Zeile 1: Distanz, Bewegungszeit, Durchschnittstempo.
- Zeile 2: Durchschnittspuls, normalisierte Leistung.

Fehlende Kennzahlen bleiben „–“. Während des asynchronen Ladens erscheint ein kurzer Ladehinweis. Ein Analysefehler ändert den bereits erfolgreichen Speicherstatus nicht; die Fahrt bleibt in der Sammlung.

`RideRuntime` hält das noch nicht angezeigte `RideCompletion` im Speicher, auch bei einem Hintergrundwechsel. Die Anzeige wird beim Öffnen genau einmal verbraucht, unabhängig von späteren Render-/Statusmeldungen. Nur `Finished` mit vorhandener Fahrt-ID öffnet sie; nicht gespeicherte Fahrten behalten die bestehende Fehlermeldung. Automatische Wiederherstellung älterer Aufnahmen öffnet keine neue Zusammenfassung. Keine neuen FIT-/Journal-/Profilformate.

Unter den Kennzahlen steht der Speicherstatus einschließlich Teilfehlern/Warnungen. Die beim Speichern vorgemerkten Cloudziele (WebDAV, intervals.icu, Dreeve, Strava) werden anhand ihrer bestehenden Belege dargestellt: ausstehend, läuft, hochgeladen, fehlgeschlagen. Vormerken/HTTP-Annahme ist kein erfolgreicher Upload. Der Dialog beobachtet die bestehenden Upload-/Einstellungs-Preferences nur während er sichtbar ist und aktualisiert die Statussymbole ohne Polling, Hashing oder Netzarbeit auf dem UI-Thread. Die entsprechenden bisherigen Abschluss-Toasts entfallen; andere Speicherfehler-/Wiederherstellungsmeldungen bleiben erhalten.

Geprüft am 23.10.2026: `RideSummaryTest` mit synthetischer FIT-Aufnahme, Abgleich aller fünf Kacheln mit der gespeicherten Analyse, zwei Zeilen in drei Hochformatbreiten, einmaliger Anzeige nach Hintergrundwechsel und Aktualisierung eines WebDAV-Belegs von ausstehend zu hochgeladen. Zusätzlich gemischte Dienstzustände und lokale Teilfehler; keine echten Netzwerk-Uploads oder neuen Dateiformate. Native Vorschau visuell geprüft. HRV-Korrektur durch bestehenden nativen Rendering-Test geprüft.

Stand 23.10.2026: die beschriebenen neuen Funktionen sind in APK **0.8.31 / Build 70** enthalten. [Buildnachweis](build-70.md). Frühere Angaben zu fehlender APK sind Verlauf.

Nach Build 72: Unter dem Speicher-/Uploadstatus steht „Zu den Details“. Erst nach dem Laden der gespeicherten Analyse aktiv; öffnet dieselbe Fahrt anhand ihrer stabilen ID in der vorhandenen Streckendetailansicht und schließt die Zusammenfassung. Keine neue Datenberechnung oder Auswahl einer anderen Fahrt. Noch keine APK.

Prüfstand nach Build 72: 32 gezielte Tests in den Umsetzungsschritten erfolgreich (Aufnahmeregression auf Android 13/15/16, Detailsnavigation, Skalenrückkehr/Fenstergrenzen, Graphgeometrie, Akku-Persistenz/Rohdatentrennung und Zehnsekundengrenze); vier betroffene Tests nach letzter Absicherung nochmals erfolgreich. Native Summary geprüft. Typografie-/Atlasprüfung erfolgreich, kein Hardware-/Diensttest oder neuer APK-Build.


Stand26.10.2026: Nur E-Bike-Fahrten erhalten unter einem horizontalen Trenner die letzten gültigen **gespeicherten** Werte für Akkustand und Akkutemperatur. Die private vollständige Analyse-FIT bleibt bevorzugte Quelle, auch wenn der öffentliche Export diese Felder ausschließt. Fehlende Werte bleiben „–“, echte0und negative Temperaturen bleiben gültig. Kein Rückgriff auf Livewerte oder den profilbezogenen Dashboard-Akkupuffer. Wettertemperatur ist keine Akkutemperatur. Bestehende FIT-/Journalformate unverändert.

Speichern und Cloudziele erscheinen als zentrierte, bei Bedarf umbrechende Gruppen aus Aktionssymbol, Haken/X/Pending und kurzer Zielbezeichnung. `TransferSymbols` ist die gemeinsame Symbolquelle für Daten & Speicher, Transfermenü und Zusammenfassung. Pending bleibt Pending bis zum bestätigten Beleg; bestehende Warnungen und lokale Teilfehler bleiben als verständlicher Zusatztext erhalten. Anzeige ohne neue Uploads oder Statusänderungen.

Hinter Zusammenfassung und täglicher HRV-Karte wird ausschließlich das Dashboard nativ mit12dp weichgezeichnet. Die Dialoge bleiben scharf und erhalten in **Flat und Glow**24dp schwarzen diffusen Schatten. Gemeinsame referenzgezählte Freigabe verhindert eine vorzeitige Aufhebung bei Dialogübergängen. Keine Änderung allgemeiner Einstellungsdialoge oder ihrer Farben. Zusammenfassungsheader bleibt SECONDARY.

Prüfstand26.10.2026:16gezielte Tests erfolgreich (Zusammenfassung2, Darstellung3, Akku3, HRV-Einstieg6, Dialogflächen2). Letzter Akkutest nach signed-Opt-in separat erfolgreich:0% und negative Temperatur aus privater Analyse, trotz deaktivierter Exportfelder; fehlende/ungültige spätere Werte überschreiben gültige Werte nicht. Native Summary-Vorschau geprüft. Kein Hardware-/Cloudtest.
