# HRV-Ruhemessung ab 0.8.16

Über den Herz-Button rechts neben Start auf dem Dashboard. Die drei rechten
Dashboard-Buttons sind gegenüber den bisherigen rechten Buttons etwa 10 % schmaler;
Start behält den verbleibenden Platz. Bestehende Strecken- und Menüaktionen bleiben.

## Messen

Im aktiven Profil einen BLE-Pulsmesser verbinden, der echte RR-Intervalle liefert.
HRV verwendet dieselbe vorhandene GATT-Verbindung wie die Pulsanzeige. Puls in bpm
allein kann RR nicht ersetzen. Aufnahme/Speichern und Ruhemessung schließen sich aus.

Messung starten, 30 Sekunden ruhig sitzen, danach 180 Sekunden messen. Der Zeitkreis
zeigt den Fortschritt, mittig stehen Puls und verbleibende Zeit. Abbruch liegt darunter;
der untere Graph zeigt echte RR-Intervalle in Millisekunden über der Messzeit.
Fortschrittsbogen und Graph verwenden PRIMARY des aktiven Themes und folgen einem
Themewechsel. Datenlücken werden nicht durch zusätzliche Messpunkte aufgefüllt.

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

Unverändert bleiben die übrigen Prüfungen: mindestens 60 Sekunden nutzbare RR-Daten,
Zeit-/Paketkonsistenz, Kontakt/Unterbrechung, maximal 5 % betroffene Rohintervalle,
Restartefakte, Korrekturfehler und überlange RR-Gesamtdauer. Ungültige Messungen zeigen
keine freigegebenen HRV-Kennzahlen; Rohdaten und Gründe bleiben erhalten. Das ist
unsere explizite Anzeigeregel, keine Behauptung identischer Kubios-Qualitätsbewertung.

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
