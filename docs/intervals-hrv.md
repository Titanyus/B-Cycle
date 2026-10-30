# HRV zu intervals.icu

Stand 30.10.2026, nach Build105. Implementiert, noch nicht als APK veröffentlicht.

Die bestehende intervals.icu-Aktivierung gilt für Fahrten und HRV. Athleten-ID, geschützter API-Schlüssel und WLAN-Vorgabe bleiben gemeinsam; keine neue Konfiguration und kein zusätzliches Backupfeld. Cloud-Reihenfolge: Lokal, WebDAV, Dreeve, intervals.icu, Strava. Kompakte Angaben: Dreeve IN „FIT-Streckendaten | Wartungsintervalle“, intervals OUT „FIT-Streckendaten | HRV-Daten“.

## Zuordnung

| BICYCLE | intervals.icu | Umrechnung |
|---|---|---|
| RMSSD | hrv | unveränderte Millisekunden |
| SDNN | hrvSDNN | unveränderte Millisekunden |
| Ruhepuls | restingHR | ganze bpm, gerundet |
| Schlafdauer | sleepSecs | Stunden → Sekunden; alternativ Differenz vorhandener Einschlaf-/Aufwachzeit mit Mitternachtswechsel |
| Schlafqualität, Motivation | sleepQuality, motivation | eigene 0–10 → API 4–1, `1 + round((10-x)*3/10)` |
| Muskelkater, Müdigkeit | soreness, fatigue | eigene 0–10 → API 0–4, `round(x*4/10)` |

Fehlende Antworten werden ausgelassen, echte Null bleibt ein Wert. Uhrzeitdifferenz ist eine lokale Uhrzeitdauer ohne behauptete Zeitzonen-/DST-Schlafkorrektur. Explizite Schlafdauer gewinnt. Körperliche Verfassung ist keine Stimmung, Alltagsregelmäßigkeit kein Stress: diese Felder werden nicht umgedeutet. Energie, RPE/Vortagstraining, LF/HF, pNN50, eigener Score, RR-Daten und persönliche Freitexte haben keinen passenden benutzten Standardkanal und bleiben lokal.

Die gemeinsame Tagesauswahl und ihre Qualitätsprüfung entsprechen der vorhandenen HRV-Auswertung. HRV4Training-Messungen mit RMSSD sind ebenfalls verfügbar; reine Antworttage werden ausgelassen. Ein später abgelehnter Tagesdatensatz darf nicht heimlich einen älteren Wert reaktivieren. Datum aus gespeicherter Messzeitzone/Importtag, keine UTC-Umgruppierung und keine 04:00-Verschiebung.

## Sammelupload

Unter dem bisherigen Fahrtenbutton stehen Beschreibung und „Alle HRV-Daten zu intervals.icu hochladen“, mit unabhängigem Status und Abbruch. Der HRV-Worker erstellt nur tägliche API-Zusammenfassungen; Netzwerk läuft auf dem Einstellungsworker. Monatliche GET-Abfragen erkennen bereits vorhandene HRV-Tage. Ein vorhandener `hrv` **oder** `hrvSDNN` (auch Nullwert 0) genügt zum Überspringen, unabhängig von der Herkunft. JSON-null bedeutet fehlend. Ein Tag mit nur Gewicht, Schlaf oder Ruhepuls blockiert HRV nicht.

Fehlende Tage werden unmittelbar vor PUT erneut gelesen. Es werden nur HRV und fehlende Zusatzfelder ergänzt, keine fremden Wellnesswerte überschrieben. Auch eigene vorhandene HRV-Tage bleiben beim expliziten Sammelupload unangetastet. Fehlerhafte Antworten, Authentifizierungs-/Dienstfehler, Kontowechsel und Abbruch erlauben keinen blinden Upload; erneuter Start prüft remote neu. Während Messung/Aufnahme/Verarbeitung kein Start, beim Beginn solcher Arbeit abbrechen.

## Automatisch und dauerhaft

Erfolgreiches lokales Speichern einer Messung oder Tagesantwort stellt die aktuelle Tageszusammenfassung in eine private dauerhafte Outbox. Ungültige Tagesauswahl entfernt eine ältere Vormerkung. Keine automatische Weiterleitung des gesamten Importarchivs; dafür ist der neue Sammelbutton da. Löschen bleibt lokal, keine entfernte Tageslöschung.

Separater Job8306/IntervalsHrvUploadService mit vorhandener Internetberechtigung, persisted/network constraints, frühestens30s nach Einreihung und exponentiellem Retry bei vorübergehenden Fehlern. JobScheduler-Latenz ist kein exakter Sendetermin. Start und Einstellungsänderung planen ausstehende Daten neu. FIT- und HRV-Belege bleiben getrennt. HRV-Sammellauf und automatische HRV-Jobs teilen eine Sperre.

Automatisch dürfen eigene Tageswerte aktualisiert werden, wenn die vorhandenen HRV-Felder dem bestätigten letzten B|CYCLE-Payload desselben Athleten/Tages entsprechen. Fremde/geänderte HRV-Werte werden erhalten. Bereits vorhandene Zusatzfelder werden nur aktualisiert, wenn sie noch dem eigenen bestätigten Wert entsprechen. Fehlt SDNN in der neuen Messung, wird ein nachweislich eigener alter SDNN-Wert geleert. Keine Erfolgsbehauptung ohne passendes Antwortobjekt. Eine neuere lokale Änderung während eines PUT bleibt vorgemerkt; der bestätigte ältere Stand ermöglicht ihre anschließende sichere Aktualisierung.

Die Wellness-API bietet hier kein bedingtes atomisches „nur wenn unverändert“-PUT. GET und PUT minimieren das Konkurrenzfenster, können gleichzeitige Fremdschreibvorgänge aber nicht ausschließen. Herkunft ist durch lokalen bestätigten Vergleich belegt, kein exklusives Remote-Besitzmerkmal. Belege/Outbox sind lokale Übertragungsdaten, keine neuen Benutzereinstellungen; vorhandene Konto-Sicherung bleibt unverändert.

## Quellen und Prüfung

- Aktuelle [OpenAPI](https://intervals.icu/api/v1/docs), Wellness GET/PUT `/athlete/{id}/wellness/{date}` und Bereichsabfrage.
- [Wellness-Felder und heutige Skalenrichtung](https://forum.intervals.icu/t/5776) (API-Autor, Post33651). Ältere Ankündigung1504 hatte positive Skalen noch umgekehrt; diese wird ausdrücklich nicht verwendet.
- IntervalsHrvTest10: Mapping, Sammelduplikate, fremde Werte, GET/PUT-Rennen, eigene Aktualisierung, neuere lokale Änderung währendPUT, Dienstfehler/Abbruch/Konto/Wiederaufnahme, echte Import-/Tagesauswahl, Schlafmaske/SDNN, UI-Reihenfolge/Button.
- HrvControllerTest ergänzt: echter Messabschluss stellt RMSSD ein, Tagesantwort aktualisiert Schlafqualität in derselben Outbox.
- Bestehende FIT-Client-/Sammeluploadtests weiterhin erfolgreich. Keine private Live-Übertragung und kein Geräteserver-Test; HTTP-Testserver genutzt.


Veröffentlichung30.10.2026: Änderungen nachBuild105 sind in **APK0.8.65/Build106** enthalten. [Download und Buildnachweis](build-106.md). Frühere Angaben zu fehlender APK sind Umsetzungshistorie.
