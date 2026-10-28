# HRV-Tagesauswertung – recherchierte Grundlage, noch nicht umgesetzt

Stand28.10.2026. Nutzer zeigt drei HRV4Training-Beispiele und bestätigt: oberhalb des Korridors erscheint dieselbe Empfehlung wie innerhalb. Bestehender Tagesfooter Auswertung bleibt bis eigenem Auftrag deaktiviert. Keine Tagesempfehlung oder neue Scoreformel implementiert.

## Dokumentiert

- [Offizielle FAQ](https://www.hrv4training.com/faq.html), Abschnitte What is the daily advice / Can a high score be bad: Tagesempfehlung kombiniert HRV relativ zu persönlichen Normalwerten, subjektive Angaben und jüngste Trends. Hohe HRV normalerweise positiv; seltene ungewöhnlich hohe Werte zusammen mit negativen subjektiven Trends können vorsichtiger bewertet werden. Kein vollständiger aktueller Entscheidungsbaum veröffentlicht. Die angezeigte HRV-Zahl wird als logarithmische RMSSD-Transformation beschrieben; nicht mit eigenständigem Readinessscore gleichsetzen, keine exakte Formel oder Alterskorrektur behaupten.
- [Daily score, baseline and normal range](https://www.hrv4training.com/blog2/daily-score-baseline-and-normal-range-an-overview): Normalbereich aus vergangenen60Tagen; kurzfristige Baseline ist davon getrennt. Tagesempfehlung kombiniert Tageswert/Normalbereich mit Schlafqualität, Muskelkater, Trainingsmotivation und subjektiver Leistungsfähigkeit im letzten Training (bzw. vorhandener Teilmenge).
- [Redefining HRV4Training’s daily advice](https://www.hrv4training.com/blog2/redefining-hrv4trainings-daily-advice): historische Beschreibung, Schwankungen relativ zu persönlichem Normalbereich statt nur relativ zur jüngsten Baseline. Subjektive Angaben über vergangene Nächte/Trainingstage, verfügbare Teilmenge. Dort genannte historische statistische Parameter sind kein Nachweis einer unveränderten aktuellen Formel.

Ein exakt vier Kalendertage umfassendes subjektives Fenster ist in den gelesenen offiziellen Quellen NICHT bestätigt. Der Nutzer beschreibt es aus HRV4Training; für B|CYCLE kann heute plus3vorherige Tage als eigene explizite Regel definiert werden, fehlende Tage/Antworten nicht nullfüllen.

## Sichtbar in den Nutzerbeispielen

1. HRV8.4, Normalbereich7.7–8.1, subjektive Trends positiv: Proceed as planned.
2. HRV7.5, Normalbereich7.6–8.2, subjektive Trends positiv: Limit intensity today, Zusatz dass subjektive Werte positiv verlaufen.
3. HRV6.9, Normalbereich7.2–8.6, subjektive Trends negativ: Take it easy today.

Daraus ergeben sich drei belegte Beispiele, kein vollständiger Entscheidungsbaum für normale/hohe HRV plus schlechtes Befinden. Insbesondere ungewöhnlich hoch darf aus der FAQ nicht pauschal als stärkerer Trainingsauftrag interpretiert werden.

## Mögliche eigene Umsetzung, erst abstimmen

Vorhandenen Tages-RMSSD bzw. transparentes lnRMSSD gegen eigenen persönlichen Normalbereich prüfen; HRV4Training-Skala nicht erforderlich. Empfehlung und subjektiven Kontext separat ausgeben, damit niedrige HRV und gutes Befinden zusammen verständlich bleiben. Eigener Vier-Tage-Mittelwert ist ein Zustandsmaß, noch kein steigender/fallender Trend; für Trendformulierungen muss eine Vergleichsreferenz definiert werden. Krankheit/Schmerzen und zu geringe Messhistorie nicht in einem Durchschnitt verschwinden lassen. Nicht aus Restpuls allein eine gegenteilige Trainingsfreigabe ableiten.

Bestehender B|CYCLE-Diagrammkorridor ist eigene beschreibende Statistik (Mittelwert±1Stichproben-SD vorheriger60Tage, mindestens14Werte). Ihn nicht als identisch zu HRV4Training oder als schon validierte Trainingssteuerung ausgeben. Bestehende Messdaten, Akzeptanz und Tagesauswahl unverändert.


Nachfolgend ausdrücklich beauftragt und als eigener Entwurf umgesetzt: [HRV-Tagesauswertung](hrv-daily-assessment.md). Eigener2ln-Wert ohne Alterskorrektur, eigener bestehender persönlicher Korridor, vier subjektive Kalendertage und bewusst keine Trendzeile. Die frühere Angabe nicht umgesetzt/deaktiviert ist Recherchehistorie.
