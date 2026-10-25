# HRV-Artefaktkorrektur: Grundlagen und Entscheidungen

Geprüft am 20.10.2026. Ziel: tolerantere Anzeige nach erfolgreicher Korrektur,
nachvollziehbare Ablehnungen und reproduzierbare Prüfung mit echten Gurtmessungen.

## Quellen

1. **Lipponen JA, Tarvainen MP (2019): A robust algorithm for heart rate variability
   time series artefact correction using novel beat classification.**
   Journal of Medical Engineering & Technology 43(3), 173–181.
   [DOI](https://doi.org/10.1080/03091902.2019.1640306),
   [PubMed / Abstract](https://pubmed.ncbi.nlm.nih.gov/31314618/).
   Adaptive Schwellen aus aufeinanderfolgenden RR-Differenzen und Klassifikation
   zusätzlicher, fehlender, verschobener sowie ektoper Beats. Im Abstract berichtet:
   100 % Sensitivität für simulierte zusätzliche/fehlende Beats, 96,96 % für echte
   atriale/ventrikuläre Ektopien und 99,94 % Spezifität. Das sind Studienergebnisse
   dieses Verfahrens, keine Leistungswerte unserer App. Hier Abstract geprüft;
   der vollständige Verlagsartikel war nicht offen verfügbar.
2. **Peltola MA (2012): Role of editing of R–R intervals in the analysis of heart
   rate variability.** Frontiers in Physiology 3:148.
   [Offener Volltext](https://pmc.ncbi.nlm.nih.gov/articles/PMC3358711/),
   [DOI](https://doi.org/10.3389/fphys.2012.00148).
   Review der Lösch-, Interpolations- und Filterverfahren. Ihre Effekte hängen
   unter anderem von Kennzahl, Fehlertyp, Reihenlänge und Population ab.
   Eine Prozentzahl allein belegt keine unverzerrten RMSSD-/SDNN-Ergebnisse.
3. **Tarvainen MP et al. (2014): Kubios HRV – Heart rate variability analysis
   software.** Computer Methods and Programs in Biomedicine 113, 210–220.
   [DOI](https://doi.org/10.1016/j.cmpb.2013.07.024).
   Bibliografie geprüft; Hintergrundquelle zur Software, kein Beleg dafür,
   dass unser Port ihrer aktuellen proprietären Implementierung entspricht.
4. **Schaffarczyk M et al. (2022): Validity of the Polar H10 Sensor for Heart Rate
   Variability Analysis during Resting State and Incremental Exercise in
   Recreational Men and Women.** Sensors 22:6536.
   [Offener Volltext](https://pmc.ncbi.nlm.nih.gov/articles/PMC9459793/),
   [DOI](https://doi.org/10.3390/s22176536).
   25 Teilnehmende, H10 gegen ECG: starke Übereinstimmung insbesondere der linearen
   Werte, kennzahl-/intensitätsabhängige Unterschiede bei DFA a1. Verwendet Kubios
   Premium 3.5.0 mit automatischer Korrektur. Diese Untersuchung schließt Daten
   mit >5 % Artefakten aus: Ein konkretes Studienkriterium, keine allgemeine
   App-Anzeigeregel. Kein Nachweis zur Genauigkeit des Cycplus H2.
5. **Kubios: Preprocessing of HRV data**
   [Herstellerdokumentation](https://www.kubios.com/blog/preprocessing-of-hrv-data/).
   Trennt Rauschabschnitte von korrigierbaren Einzelbeats und automatische von
   festen schwellenbasierten Verfahren. Beschreibt die adaptive dRR-Schwelle mit
   Quartilabweichung und Faktor 5,2 sowie unterschiedliche Korrekturen je Fehlertyp.
   Herstellerquelle, keine unabhängige Vergleichsstudie.

## Umsetzung und Grenzen

Unsere Erkennung basiert schon über den gepinnten NeuroKit2-Port auf dem Ansatz
von Lipponen/Tarvainen. Ein Faktor 5,2 in der Erkennung ist keine 5-%-Freigabegrenze.
Das Messprofil v2 ändert keine Detektorschwellen oder Interpolationsmethode. Es
verwirft einen Korrekturversuch, wenn danach gleich viele oder mehr Auffälligkeiten
übrig bleiben. Das getrennte v1-Referenzprofil bleibt byte-/fixture-kompatibel.
Weniger Detektorauffälligkeiten beweisen allein keine physiologische Richtigkeit.

Die bisherige Sperre bei >5 % **veränderten Rohintervallen** wird zur deutlichen
Unsicherheitswarnung. Seit25.10.2026 gilt dies ausdrücklich auch für verbleibende Detektorauffälligkeiten, wenn die sonstigen technischen Prüfungen bestehen. Bei einem verschobenen Peak können zwei
Intervalle verändert werden; dieser Anteil darf nicht als Fehlbeatquote bezeichnet
oder direkt mit Kubios-Prozentanzeigen verglichen werden. Die App zeigt in diesem
Fall eine Schätzung mit möglicher Verzerrung, keine Zusicherung guter Qualität.
Restauffälligkeiten allein sperren neue Messungen nicht mehr: Anzahl und möglicher Einfluss werden orange angezeigt. Technische Fehler, unvollständige Sitzungen und die übrigen Konsistenzprüfungen sperren weiterhin. Keine zusätzliche Glättung zur Erzwingung
eines Ergebnisses; Empfangslücken werden nicht mit erfundenen Beats aufgefüllt.

Aus getrennten Messungen verschiedener Apps lässt sich nicht ableiten, dass
identische RR-Reihen analysiert wurden. Für eine konkrete Ursachenprüfung braucht
es mindestens eine bei uns abgelehnte Rohmessung. Nächste Validierung:

- Gespeicherte RR-Ticks und Paketzeiten reproduzierbar erneut auswerten; ursprüngliche
  Ergebnisse und Algorithmus-/Policy-Version beibehalten.
- Für dieselbe Rohfolge alte/neue Klassifikationen, Restfehler, veränderte Intervalle
  und rohe/korrigierte RMSSD, lnRMSSD, SDNN vergleichen.
- Synthetisch bekannte fehlende/zusätzliche/verschobene Peaks und Fehlercluster
  gegen die unverfälschte Folge prüfen; Kennzahlfehler statt nur Erfolgsquote messen.
- Ruhige H10- und H2-Aufnahmen, starke respiratorische Variabilität, Reihenränder
  und Bluetooth-Batches unterscheiden. Ohne synchrones ECG sind Detektorlabels
  keine bestätigte medizinische Rhythmusklassifikation.

Automatische Prüfungen decken Referenzparität, ein tatsächlich verschlechterndes
Randbeispiel, erfolgreiche Korrektur über der bisherigen 5-%-Sperre, verbleibende
Artefakte, Speicherung/Backup und Warn-/Fehlerdarstellung ab. Eine Validierung an
den vom Nutzer beschriebenen fehlgeschlagenen Messungen steht noch aus.

## Frequenzbereich

LF/HF verwendet eine explizit versionierte Welch-Schätzung (4-Hz-Tachogramm, natürlicher kubischer Spline, Hannfenster bis 64 s, 50 % Überlappung, linear detrend). Testsignale mit bekannten LF-/HF-Amplituden prüfen die physikalische Leistung A²/2. Das ist kein identischer Nachbau der vollständigen Kubios-Pipeline und keine klinische Validierung. Grundlage für Banddefinitionen und kurze Ruhemessungen: Task Force (1996), *Heart rate variability: standards of measurement, physiological interpretation and clinical use*, DOI [10.1161/01.CIR.93.5.1043](https://doi.org/10.1161/01.CIR.93.5.1043). LF/HF nicht als direkte autonome Balance interpretieren; fünf Minuten unter vergleichbaren Bedingungen empfehlen.
