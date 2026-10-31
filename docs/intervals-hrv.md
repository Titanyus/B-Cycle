# HRV zu intervals.icu

Stand31.10.2026. Basis veröffentlicht inBuild106; aktuelle Ergänzungen veröffentlicht in[0.8.71/Build112](build-112.md).

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

Unter dem bisherigen Fahrtenbutton stehen Beschreibung und „Alle HRV-Daten zu intervals.icu hochladen“, mit unabhängigem Status und Abbruch. Der HRV-Worker erstellt nur tägliche API-Zusammenfassungen; Netzwerk läuft auf dem Einstellungsworker. Monatliche GET-Abfragen erkennen bereits vorhandene HRV-Tage. Ein vorhandener `hrv` **oder** `hrvSDNN` (auch Nullwert0) schützt vor einem erneuten HRV-Upload. Nur nachweislich eigene unveränderte Tage dürfen fehlende neue Zusatzfelder erhalten. JSON-null bedeutet fehlend. Ein Tag mit nur Gewicht, Schlaf oder Ruhepuls blockiert HRV nicht.

Fehlende Tage werden unmittelbar vor PUT erneut gelesen. Es werden nur HRV und fehlende Zusatzfelder ergänzt, keine fremden Wellnesswerte überschrieben. Eigene unveränderte HRV-Tage werden ausschließlich um fehlende sleepScore-/stress-/injury-Felder ergänzt; vorhandene Werte und HRV bleiben unangetastet. Fehlerhafte Antworten, Authentifizierungs-/Dienstfehler, Kontowechsel und Abbruch erlauben keinen blinden Upload; erneuter Start prüft remote neu. Während Messung/Aufnahme/Verarbeitung kein Start, beim Beginn solcher Arbeit abbrechen.

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

## Korrektur nach Build 110 – 30.10.2026

Der Sammelabruf nutzt nun ausdrücklich GET `/athlete/{id}/wellness.json?oldest=...&newest=...` gemäß dem `wellness{ext}`-Endpunkt der aktuellen OpenAPI. Vorher wurde für diese Bereichsabfrage `wellness` ohne Dateiendung verwendet. PUT und Einzelabruf bleiben bei `/wellness/{date}`. Die Monatsantwort liefert die Standardfelder der API; kein zusätzlicher Feldfilter. HTTP-200 mit JSON-null beim Einzelabruf bedeutet fehlender Tagesdatensatz; sonstige fehlerhafte Antworten bleiben Fehler, kein blinder Upload.

Im Sammelergebnis stehen Datum und konkrete Fehlermeldung zuerst; bei HTTP-Fehlern zusätzlich Statuscode. Ein Serverfehler wird nicht mehr ausschließlich als „Vorgang unterbrochen“ angezeigt. Keine Antwortkörper, API-Schlüssel oder sonstigen Zugangsdaten in der Diagnose. Bestehende fremde HRV-Tage bleiben geschützt. Zusätzliche Tests prüfen `.json`, Nulltag, fehlerhafte Antworten und sichere HTTP-Fehlerangaben. Der Nutzer bestätigte funktionierende FIT-Uploads (400 Fahrten); ein privater Live-HRV-Upload konnte hier nicht geprüft werden.

## HTTP422 nach Build111 – 31.10.2026

Nutzer meldet422 beim Sammelupload, während ein einzelner Tagesupload inzwischen erfolgreich war. Das grenzt den Fehler auf den zusätzlichen Bereichsabruf ein, beweist die Ursache aber noch nicht. Bereichs-GET nun ohne zusätzliche fields-Liste; die API liefert ihre Standardfelder. Insbesondere wird die Metadaten-ID nicht als auswählbares Wellnessfeld angefordert. Einzel-PUT enthält nur den berechneten Patch, kein redundantes id-Feld (Datum steht bereits im URL). Antwortdatum und bestätigte Messwerte werden weiterhin geprüft; keine blinden Uploads und keine Belege bei422.

Fehlerdiagnose kennt Bereichsabruf, Einzelprüfung und Übertragung. Bei422 kann ausschließlich ein vom Server als unbekannt benannter, eigener bekannter Feldname extrahiert werden (id bzw. unsere übertragenen Wellnessfelder). Maximal4KiB werden kurz imRAM gelesen; keine Antwortkörper, Athletenkennungen oder Zugangsdaten in UI/Logs. Nativer HTTP-Testserver prüft422 beiGET undPUT, unveränderte Messwerte, keine falschen Upload-Belege und keine Ausgabe des Testschlüssels/der Kontokennung. Bestehende Fremddatenschutz-/Wiederaufnahme-/FIT-Clienttests bestehen. Ein erneuter privater Live-Sammelupload ist noch nötig.

Aktuelle offizielle OpenAPI am31.10.2026 gelesen; Forum nennt422 bei nicht erkannten Wellnessfeldern ([API-Thread, Beitrag243](https://forum.intervals.icu/t/609/243), [Companion-Fall](https://forum.intervals.icu/t/124208/58)). Nicht durch ignoreMissingFields oder blindes Weglassen unbestätigter Messwerte umgehen.

Zusätzliche Nutzerfrage beantwortet, noch kein Implementierungsauftrag: Wellness liefertCTL/Fitness undATL/Ermüdung; Form ausCTL−ATL, bei Darstellung absolute/prozentuale Intervals-Variante berücksichtigen. Keine Fitnessdaten-Abholung oder neue Anzeige eingebaut.

## Weitere Wellnessfelder und eigene Stressfrage – 31.10.2026

Schlafbewertung: sleepScore erhält die eigene subjektive0–10-Bewertung unverändert als Zahl; kein erfundener Garmin-/100-Punkte-Score. sleepQuality bleibt zusätzlich die bisherige4-stufige Umrechnung. Verletzung: explizit nein→injury1/Excellent, ja→injury4/Injured, unbeantwortet auslassen. Freitext zur Verletzung bleibt lokal. Stress: neue Tagesfrage „Wie gestresst bist du?“ mit Wenig/Durchschnittlich/Sehr/Extrem, direktstress1/2/3/4. Keine Auswahl voraussetzen und keine Alltagsregelmäßigkeit in Stress umdeuten.

Stress ist ein optionales eigenständiges Int-Feld im Tagesfragebogen, Bereich1–4, in JSON/Tageszusammenführung/HRV-Sicherung enthalten und bei weiterer Messung vorausgewählt. Neue Frage imAlltag-Abschnitt; über globale Fragenauswahl abschaltbar. Konfigurationsschema2: bisherigeSchema1-Konfigurationen bekommen die Frage einmal hinzu, danach bleibt eine ausdrückliche Deaktivierung erhalten. Alte Fragebogen-Datensätze ohneStress bleiben gültig; keine nachträgliche Ableitung für alte Tage. HRV-Wertberechnung und Empfehlungen nicht verändert.

Zusätzliche Prüfungen: native Vierfachauswahl samt langem Label/Leerzustand/Vorauswahl, Validierung/Migration, echte HRV-Backup-Rückübertragung, Schlafscore und boolesche Verletzungszuordnung, alle vier Stressstufen, Fremdwertschutz und eigene Updates.34 gezielte Tests erfolgreich, Log `/workspace/.tools/hrv-wellness-stress-tests.log`. Bisherige Gruppen-/Uploadtests40 erfolgreich.

Nutzerentscheidung31.10.: Sammelupload ergänzt eigene unveränderte HRV-Tage um fehlende sleepScore-/stress-/injury-Felder. Tages-GET prüft direkt vor PUT erneut Eigentumsbeleg und fehlende Werte. Der ergänzende PUT enthält ausschließlich die noch fehlenden Zusatzfelder, keine HRV-Felder. Bestehende Werte einschließlich nachträglich fremder Zusatzfelder bleiben erhalten. Bestätigte Ergänzungen erweitern den bisherigen Beleg. Ohne passenden Beleg oder nach Änderung der entfernten HRV wird übersprungen.

Abschließende Prüfung31.10.:36 HRV-/Stress-/Fragebogen-/Sicherungstests erfolgreich, einschließlich Ergänzung aller drei Felder, bestehender Fremdfelder, geänderter HRV zwischen Monats- und Tagesabruf sowie wiederholtem Sammellauf. Log`hrv-supplement-final-tests.log`.

## Neue Rückmeldung während Build114 – TagesPUT422

Nutzer meldet ausdrücklich „Tagesdaten übertragen (HTTP422)“, 0hochgeladen/0vorhanden/1fehlgeschlagen. Damit tritt der Fehler im TagesPUT auf; die bisherige Vermutung eines ausschließlich scheiternden BereichsGET erklärt diesen Befund nicht. Unsere Feldnamen/Datentypen stimmen mit dem bekannten offiziellenWellness-Schema überein. Die konkrete zurückgewiesene Eigenschaft ist anhand der aktuellen Diagnose noch unbekannt. Kein unbestätigtes Weglassen von Messwerten und keine blinde Wiederholung. Nutzer bestätigt Datum2026-08-28. Gültiger vergangenerTag, kein belegterDatumsfehler. Ob automatischer heutigerUpload weiterhinfunktioniert, nochunbeantwortet. Kein privater Livezugang. [Öffentlich dokumentierter erfolgreicherPUT ohneBody-ID](https://forum.intervals.icu/t/16966/5); fehlendeID daher kein belegterGrund. Build114 enthält ausschließlich die bereits geprüften Listen-Korrekturen, noch keinen neuenHRV-Fix.
