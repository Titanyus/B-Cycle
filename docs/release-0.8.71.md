# B|CYCLE0.8.71

- Streckenübersicht nach Monat/Jahr oder passenden Distanz-, Fahrzeit- und Tempobereichen gegliedert. Jeder Bereich zeigt die gefilterte Aktivitätszahl, Distanz, Fahrzeit und das gewichtete Durchschnittstempo. Der Fast-Scroller springt zu Gruppenanfängen.
- HRV-Messring ohne separaten Abschlusskreis, sowohl in der Ruhe- als auch in der Messphase.
- Neue optionale Stressfrage im Tagesbefinden: Wenig, Durchschnittlich, Sehr oder Extrem. Die Antwort bleibt pro Tag gespeichert und ist in der HRV-Sicherung enthalten.
- Intervals erhält Schlafbewertung, Stress und Verletzungsstatus zusätzlich zu den bestehenden Tageswerten. Der Sammelupload ergänzt fehlende Zusatzfelder bei nachweislich eigenen unveränderten HRV-Tagen und schützt vorhandene/fremde Werte.
- Intervals-Bereichsabruf vereinfacht, redundantes ID-Feld beim Tagesupload entfernt und HTTP422-Diagnose präzisiert.

Geprüft mit gezielten nativen UI-, Modell-, HTTP- und Sicherungstests. Keine vollständige Gesamtsuite oder physischer Gerätetest. Ob die Änderung den gemeldeten422 beim privaten Konto behebt, muss ein erneuter echter Sammelupload zeigen.

[Buildnachweis und Download](build-112.md).
