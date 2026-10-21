# HRV-Ruhemessung ab 0.8.16

Über den Herz-Button rechts neben Start auf dem Dashboard. Die drei rechten
Dashboard-Buttons sind gegenüber den bisherigen rechten Buttons etwa 10 % schmaler;
Start behält den verbleibenden Platz. Bestehende Strecken- und Menüaktionen bleiben.

## Messen

Im aktiven Profil einen BLE-Pulsmesser verbinden, der echte RR-Intervalle liefert.
HRV verwendet dieselbe vorhandene GATT-Verbindung wie die Pulsanzeige. Puls in bpm
allein kann RR nicht ersetzen. Aufnahme/Speichern und Ruhemessung schließen sich aus.

Messung starten, standardmäßig 30 Sekunden ruhig sitzen und 180 Sekunden messen. Einstellungen → HRV-Messungen bietet Ruhe 15–60 s und Messen 60–300 s, jeweils in 15-s-Schritten. Der Zeitkreis
zeigt den Fortschritt, mittig stehen Puls und verbleibende Zeit. Abbruch liegt darunter;
der untere Graph zeigt echte RR-Intervalle in Millisekunden über der Messzeit.
Der größere Kreis nutzt während der Ruhephase FOREGROUND mit ACCENT-Endpunkt,
während der Messung ACCENT mit FOREGROUND-Endpunkt. Die Kreislinie ist 12,15 dp breit (vorher 9 dp, +35 %), der Endpunkt hat 16,38 dp
Durchmesser (vorher 11,7 dp, +40 % unabhängig von der dickeren Linie). Ein weicher radialer ACCENT-Glow pulsiert zwischen
45 % und 10 % Deckung; Starthelligkeit gegenüber 30 % um 50 % erhöht, Abfallzeit 1,5 s, Neustart beim nächsten Schlag. Zwischen
BLE-Paketen wird nur die Animation nach dem zuletzt empfangenen RR-Takt fortgesetzt,
keine Messfolge ergänzt. Bei ausbleibendem RR stoppt die Pulsanimation nach 5 s.
Zeit 18 statt 24 sp; bpm steht in der Dashboard-Einheitenschrift und deren 65-%-Farbe
hinter dem Pulswert, vertikal zur sichtbaren Zahl zentriert. Start/Erneut messen erhalten
900 Black und einen transparenten ACCENT-Pillenrahmen (1 dp, horizontaler Innenabstand).
Abbruch und Speichern erneut versuchen bleiben rahmenlose ACCENT-Textbuttons unter der
Zeit innerhalb des Kreises, mit normaler bedienbarer Touchfläche. Keine
zusätzliche kleine Inhaltsüberschrift. Alle Themefarben folgen Themewechseln.

Das Diagramm ist 286 dp hoch (vorher 220 dp, +30 %); die Achsbeschriftungen haben 50 % Deckung. Kurve, Raster und Zeitfenster bleiben unverändert.
Der ACCENT-Graph zeigt zuerst 0–60 s. Danach gleitet das 60-s-Fenster kontinuierlich
bis zum Ende der eingestellten Messdauer (beim Standard zuletzt 120–180 s); die X-Achse zeigt absolute Messsekunden mit Strichen alle 10 s.
Gezeichnet wird nur der sichtbare Ausschnitt, Rohdaten und Sicherung enthalten
weiterhin die gesamte Messung. Datenlücken werden nicht durch zusätzliche Messpunkte aufgefüllt.

Appwechsel, Schließen, Sensor-/Kontaktverlust oder mehr als fünf Sekunden ohne RR
brechen ab. Ergebnisse und Teilaufnahmen werden privat gespeichert. Nach Prozessabbruch
wird eine unvollständig gespeicherte Sitzung als unterbrochen wiederhergestellt,
niemals still als gültig freigegeben. Über Gespeicherte Messungen erneut öffnen.

## Kennzahlen und Abdeckung

Übernommen aus B-Cycle-HRV, Commit `895de63a98828cc7a7ece813c618e2eb48321daa`:
RMSSD, LnRMSSD, SDNN, pNN50, mittlerer Ruhepuls und Artefaktkorrektur.
Das lokale Modul hrv-core benötigt keine Android-/Netzwerkfunktionen.
62 gepinnte NeuroKit2-Referenzfälle, Kernprüfungen und Lizenzhinweise sind enthalten.

Bewusst geänderte Qualitätsregel gemäß Nutzerauftrag:
**Geringe RR-Abdeckung (<95 %) allein sperrt die Anzeige nicht.** Die Kennzahlen
werden aus den tatsächlich empfangenen und bereinigten RR-Intervallen berechnet.
Ein orangefarbener Hinweis nennt die Abdeckung und mögliche Verzerrung. Beispielsweise
können 180 Sekunden Empfang mit einem RR-Wert von etwa 800 ms pro Sekunde nur rund
144 Sekunden aufsummierte RR-Dauer ergeben. Es werden keine fehlenden Herzschläge
erfunden oder Intervalle auf die Messzeit gestreckt.

Die überarbeitete Korrektur behält dieselbe adaptive Erkennung, übernimmt aber nur
Durchläufe, die die Zahl verbleibender Auffälligkeiten verringern. Ein verschlechternder
oder stagnierender Durchlauf ersetzt die bisherige Folge nicht. Das Referenzprofil und
seine 62 Golden-Fälle bleiben separat unverändert; das Messprofil trägt die Version
`nk-ff419d9-int1024-best-pass-v2`.

**Mehr als 5 % veränderte Rohintervalle sind bei vollständig bereinigter Folge eine
orange Warnung statt einer Anzeigesperre.** Die Kennzahlen werden ausdrücklich als
möglicherweise verzerrte Schätzung angezeigt. Ein verschobener Peak verändert zwei
benachbarte Intervalle; die Quote ist keine Fehlbeatquote. Dies ist eine Produktregel,
keine wissenschaftliche Zusicherung der Verlässlichkeit oberhalb einer Prozentgrenze.
Verbleibende Auffälligkeiten sperren weiterhin die Anzeige. Technisches Scheitern,
Restauffälligkeiten und die frühere 5-%-Sperre haben getrennte Meldungen. Der veränderte
Anteil und Restzählwerte werden angezeigt und mit Klassifikationen gespeichert.

Weiter erforderlich: mindestens 60 Sekunden nutzbare RR-Daten (beim 60-s-Protokoll 55 s mit Randtoleranz), Zeit-/Paketkonsistenz,
Kontakt/Unterbrechung, gültige Korrektur und keine überlange RR-Gesamtdauer. Rohdaten
und Gründe bleiben erhalten. Bestehende gespeicherte Ergebnisse werden nicht automatisch
neu bewertet. Quellen, Grenzen und nächste Validierung: [HRV-Korrektur](hrv-artifact-research.md).

## Dauerhaft gespeicherte Daten

Privat unter `files/hrv-measurements/<uuid>/` und unabhängig vom Fahrrad:

- `draft.json`: Startzeit, Zeitzone, Sensorname und Messprotokoll.
- `capture.jsonl`: Originalpakete und monotone Empfangszeiten, fortlaufend gesichert.
- `result.json`: Abschlussmarker mit Phase, Mess-/RR-Dauer, rohen RR-Ticks, korrigierter
  NN-Folge, rohen/bereinigten Kennzahlen, Warnungen, Ablehnungsgründen,
  Korrekturprotokoll und Algorithmus-/Qualitätsversion.

**Fahrten sichern** nimmt diese Messungen lokal oder per WebDAV als zusätzliche
geprüfte `hrv-<uuid>.json`-Dateien im Sicherungsordner auf. Wiederherstellung überschreibt
keine vorhandenen abweichenden Messungen. Reine Einstellungssicherung enthält keine
Messdateien. Kein automatischer Upload zu intervals.icu, Dreeve oder Strava.

Persönliche Baselines, Readiness-/Trainingsbewertungen und Wellness-Synchronisation
sind spätere Aufgaben. Echte Gurt-/Geräteprüfung bleibt nach den automatisierten
Integrationsprüfungen erforderlich. Lizenz: hrv-core/THIRD_PARTY_NOTICES.md und
NeuroKit2-MIT-Lizenz in den App-Assets und der Bibliotheks-Lizenzanzeige.

Der Sitzungsbeginn verwendet den Zeitstempel des Start-Klicks, nicht den späteren
Workerstart. Vor Start empfangene, verzögert zugestellte Pakete gehören nicht zur
neuen Sitzung. Dieser Callback-/Worker-Randfall ist separat geprüft.

Der Kreisfortschritt läuft bildsynchron auf einer monotonen Zeitleiste pro Phase über die eingestellte Ruhe- bzw. Messdauer. Neue Puls-/Statusmeldungen starten diese Zeitleiste nicht neu; kein Fortschritt nur in den 250-ms-Statusschritten.

## Ergebnis, Frequenzbereich und gezielter Export

Während der Messung stehen Ruhephase/Messung im Kreis über dem Puls; außerhalb erscheinen keine weiteren Infotexte. Nach Abschluss gibt es die Gruppen Zeitbereich, Frequenzbereich und Messqualität mit Sensor und tatsächlichem Protokoll. HRV-Mess-/Ergebnisheader folgen der Fensterfarbe SECONDARY, alle übrigen Header behalten PRIMARY.

LF (0,04–0,15 Hz) und HF (0,15–0,4 Hz) in ms² sowie LF/HF werden für neue geeignete Messungen aus bereinigten NN berechnet. Natürlicher kubischer Spline bei 4 Hz innerhalb beobachteter Endpunkte, Welch mit maximal 256 Samples (64 s), 50 % Überlappung, linearer Trendabzug und Hannfenster, einseitige Leistungsdichte und trapezförmige Integration einschließlich Bandgrenzen. Bei kürzeren Serien bis mindestens 50 s kürzeres Fenster und Zero-Padding auf nächste Zweierpotenz; dieses verbessert nicht die tatsächliche Auflösung. Parameter und Methodenversion werden gespeichert. Empfehlung fünf Minuten; kurze Aufnahmen, fehlende RR und langsame Atmung beeinflussen die Spektralschätzung. Kein direkter Sympathikus-/Parasympathikus-Balancewert. Alte Ergebnisse bleiben unverändert.

Das 60-s-Protokoll erlaubt 5 s Randverlust durch Ausschluss des ersten unsicheren Ankerpakets und Zustellgrenzen (mindestens 55 s nutzbare RR); längere Protokolle brauchen mindestens 60 s. Dies ist eine dokumentierte Produktregel, keine wissenschaftliche Norm.

Daten & Speicher → HRV-Pulsdaten: Sticky-Abschnitt mit Exportbutton. Manueller ZIP-Download enthält eine JSON je gespeicherter Messung, auch abgebrochene/abgelehnte. Enthalten: vollständiges gespeichertes Ergebnis, Sensor, Uhrzeit/Zeitzone, Protokoll, Rohpakete als Base64 plus dekodierte Werte/monotone Empfangszeit, RR-Ticks (1024/s), bereinigte NN, Korrekturindizes/-durchläufe/-Restklassen, Qualitätsgründe und Versionsangaben. Ziel Downloads/B-Cycle, kein automatischer Upload. Eine laufende Messung erst beenden. Reine Einstellungssicherung enthält die Protokollzeiten, keine Messdaten.

## Kurze Unterbrechungen der Live-Kurve

Die grafische Lückenerkennung betrachtet zusätzliche Zeit über das beobachtete RR-Intervall hinaus: Punktabstand minus aktuelles RR größer als max(1500 ms, 1,5 × RR). Bisher wurde fälschlich der gesamte Abstand gegen diese Schwelle geprüft, wodurch kurze Empfangsverzögerungen sichtbare Linienunterbrechungen auslösen konnten. Längere nicht durch RR erklärte Lücken bleiben als Pfadunterbrechung sichtbar. Keine zusätzlichen RR, unveränderte Empfangszeitanker, Messqualitätsregeln, Korrekturen und Speicherung; nur das Gap-Flag des Live-Graphen ändert sich. Vier gezielte Prüfungen für Glow, native Beschriftung/Pillenrahmen/Themewechsel, Controller-Empfangszeiten und echte Graphunterbrechungen erfolgreich. Kein neuer Gurt-/Gerätetest und keine APK für diese Folgeänderung.

Weitere gewünschte Auswertung und recherchierter Vergleich: [HRV-Score und Verlauf – Entwurf](hrv-score-plan.md). Noch kein Score oder Langzeitdiagramm implementiert.

## Tagesbefinden nach gespeicherter Messung

Optionaler Button unten sowie im historischen Ergebnis öffnet den konfigurierbaren Fragebogen. Antworten beeinflussen keine HRV-Messung/Bewertung. Separate atomare Datei, Einstellungssicherung für Fragenauswahl/Geschlecht, vollständige Sicherung und Analyseexport für Antworten. [Details und Prüfungen](hrv-questionnaire.md).
