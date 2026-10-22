# HRV-Auswertung

Eigener nativer Bildschirm: HRV-Einstellungen → HRV-Auswertung oder nach dem erfolgreichen Speichern des Tagesbefindens über „Zur Auswertung“. Vorhandene Zurück-Navigation und HRV-Header in Fensterfarbe erhalten. Sechs Kennzahlen wie im Vorbild: Puls, SDNN, RMSSD, pNN50, LF und HF. Oben Auswahl mit den Werten des ausgewählten Tages, darunter Balken, Mittelwert und Normalbereich. Standard: RMSSD und letzte 14 Kalendertage. Tageswert antippen und „Messung und Tagesbefinden“ öffnet vorhandene Details.

## Transparente eigene Berechnung

Keine Kopie der nicht vollständig veröffentlichten HRV4Training-Korridorformel und kein neuer Readinessscore. Arithmetische Mittelwerte in der angezeigten Einheit, auch für RMSSD; kein versteckter Median und keine Alterskorrektur.

- Zuerst die zentrale Tagesregel: nur die letzte Messung je gespeichertem lokalem Tag. Kein Ersatz durch eine ältere erfolgreiche Messung.
- Eigene abgelehnte Messungen, fehlende Kennzahlen und reine CSV-Kontexttage liefern keinen Diagrammwert. Keine Nullwerte als Ersatz oder Interpolation fehlender Tage.
- Linie: Mittelwert der verfügbaren Tageswerte im aktuellen und den sechs vorherigen Kalendertagen, mindestens fünf Werte.
- Normalbereich: Mittelwert ± eine Stichproben-Standardabweichung der verfügbaren Tageswerte aus den 60 **vorherigen** Kalendertagen, mindestens 14 Werte. Stichproben-SD mit Nenner n−1; Untergrenze mindestens null. Der aktuelle Wert beeinflusst seinen eigenen Korridor nicht. Dies ist ein beschreibender Schwankungsbereich, kein Konfidenzintervall und keine Trainingsempfehlung.
- Gemeinsamer persönlicher Bestand je Kennzahl über App- und Protokollgrenzen hinweg. Quelle und Messzeiten bleiben als Metadaten erhalten; sie starten keinen neuen Mittelwert/Normalbereich. Gleiche Einheit ist erforderlich, unbekannte CSV-LF/HF bleiben Originalfelder. Unterschiede der Messbedingungen können die Vergleichbarkeit beeinflussen.
- Importierte kompatible Kennzahlen dürfen angezeigt werden, obwohl ihre Rohdatenqualität nicht nachprüfbar ist; Herkunft klar am gewählten Tag. CSV-LF/HF mit unbekannter Einheit bleiben Originalfelder in den Details und werden nicht als ms² in das native Diagramm übernommen.
- Linie und Fläche verbinden nur unmittelbar aufeinanderfolgende Kalendertage derselben Kennzahl mit verfügbaren Werten/Statistiken. Lücken nicht überbrücken.

## Bedienung und Technik

Zweifinger-Zoom verändert ausschließlich den sichtbaren Zeitraum zwischen sieben Tagen und dem gesamten verfügbaren Verlauf. Horizontal wischen verschiebt den Ausschnitt innerhalb der Daten. Vertikales Scrollen der Seite bleibt möglich. Native Accessibility-Aktionen erlauben Zoomen und Blättern ohne Zweifingergeste. Kein neuer Zoom-/Statistik-Konfigurationsscreen.

HrvHistorySummary streamt zusätzlich wenige skalare korrigierte Kennzahlen, bekannte Frequenzwerte und Protokollzeiten. Große Roh-/NN-/Korrekturfolgen bleiben außerhalb der Verlaufsansicht. Dateiformate und archivierte Dateien unverändert; keine neue Datenbank, Abhängigkeit, Cachedatei oder Migration. HrvController berechnet alle Serien auf dem vorhandenen Speicherworker. Canvas-Diagramm und Auswahl lesen ausschließlich fertige kleine Serien. Details/Fragebögen verwenden vorhandene Worker-Pfade.

Die bisherige CSV-Lebenszykluskorrektur nach Build 64 bleibt enthalten. Noch keine neue APK für diese Auswertung beauftragt.

## Quellen für das Vorbild

- [HRV4Training Pro](https://www.hrv4training.com/in-app-pro.html): Normalbereich im Baseline-Bildschirm als Pro-Funktion.
- [Daily score, baseline and normal range](https://www.hrv4training.com/blog2/daily-score-baseline-and-normal-range-an-overview): persönlicher Normalbereich aus 60 Tagen.
- [Interpreting HRV trends](https://www.hrv4training.com/blog2/interpreting-hrv-trends): Baseline als 7-Tage-Mittelwerte, kein Median. Ältere Veröffentlichung, kein Nachweis der vollständigen heutigen proprietären Korridorformel.

## Prüfung

27 unterschiedliche gezielte Tests erfolgreich: sieben neue Statistik-/Viewport-/Gesten-/UI-/Navigationsprüfungen, drei Tagesauswahltests, ein großer Streaming-Historientest, zwei HRV-Einstellungstests, zwei Hilfetests, der erweiterte echte Fragebogen-Speicher-/Navigationspfad, zwei Speichertests und neun CSV-Importtests. Native Vorschau visuell geprüft, einschließlich korrigierter Datumsbeschriftung am rechten Rand; [Testdaten-Vorschau](previews/hrv-trends.png). Geprüft sind auch echter Zweifinger-Zoom, horizontales Ziehen, Antippen eines älteren Tages und korrekte Messungs-ID, Zurück zu unveränderten HRV-Einstellungen, Kalenderfenster, Mindestdaten, fehlende Werte, Gruppenwechsel und unveränderte Archivdateien/Exporte. Typografie- und Atlasprüfung erfolgreich. Kein Gesamttestlauf, Hardwaretest oder neuer APK-Build.

Veröffentlichung: Diese Ergänzung/Korrektur ist in APK **0.8.26 / Build 65** enthalten. [Buildnachweis](build-65.md). Frühere Aussagen zu fehlender neuer APK beschreiben den Stand vor dieser Veröffentlichung.

Darstellung nach Build 65: Normalbereich als weiße Fläche mit 40 % Deckkraft. Obere und untere Grenze in Weiß als gepunktete Linie von 1 dp, nach den Balken gezeichnet, damit sie erkennbar bleiben. Lücken und Gruppenwechsel weiterhin nicht überbrücken; Mittelwert bleibt ACCENT. Berechnung unverändert, Geometrie über BicycleDesign. Noch nicht in APK 0.8.26.

Prüfstand dieser Ergänzung: 21 gezielte Tests erfolgreich (drei neue Rundungs-/Migration-/Ergänzungsprüfungen, neun CSV-Importtests, sieben Tagesbefinden-Tests, bestehende Slider-/Speicherfreigabeprüfung und native Auswertungsdarstellung). Korridor nativ visuell geprüft; Typografie und generierter Atlas konsistent. Kein Gesamtlauf und keine neue APK.

Veröffentlichung der Elf-Stufen-/Antwortimport-/Korridor-Erweiterung: in APK **0.8.27 / Build 66** enthalten. [Buildnachweis](build-66.md). Frühere Angaben zu fehlender APK sind Verlauf.

## Diagrammanpassung nach Build 66

Y-Achse je Kennzahl einmal aus der gesamten geladenen Serie berechnen, einschließlich Tageswerten, Mittelwert und Korridor. Keine Neuberechnung aus dem sichtbaren Ausschnitt; Zoom und Blättern ändern nur X. Puls: fünf bpm Abstand unter/über den Extremen; andere Kennzahlen fünf Prozent Abstand, obere Mindestreserve 0,1, Untergrenze mindestens null. Leere Serie 0–1, konstante Werte mit nichtleerem Bereich. Neue geladene Daten können beim erneuten Öffnen einen neuen Bereich ergeben.

Balken ACCENT; nur bei vorhandenem Korridor und Tageswert strikt unter der unteren Grenze weiß. Gleichheit, fehlender Normalbereich oder hohe Werte bleiben ACCENT; keine zusätzliche Trainingsbewertung. 7-Tage-Mittel weiß, Korridor weiß mit 30 % Deckkraft und bisherigen gepunkteten 1-dp-Grenzen. Auswahl ändert nicht die fachliche Balkenfarbe.

Diagrammhöhe 504 statt 360 dp (+40 %), Y-Achsenplatz 40 statt 48 dp. Nur das Diagramm nutzt den linken Seiteninset mit; übrige Controls behalten ihre regulären Abstände. Clipping am Seitenpadding ausgeschaltet, damit linke Skalentexte erhalten bleiben. Noch nicht in APK 0.8.27 enthalten.

Prüfstand feste Achsen/Farben/Größe: acht gezielte Auswertungstests erfolgreich, darunter Extrema einschließlich Korridor, konstante/leere Serien, feste Y-Skala bei echten Pinch-/Wischgesten, Farbentscheidung an/unter/ohne Untergrenze und bestehende Navigation/Streaming/Statistik. Native 504-dp-Ansicht mit linkem Platzgewinn visuell geprüft; keine abgeschnittenen Achsenbeschriftungen. Typografie-/Atlasprüfung erfolgreich, kein Gesamtlauf und keine APK.

## Aktuelle Werte bei wechselnder App

Spätere Nutzerklärung: In den letzten Tagen Messungen aus beiden Apps, eine Messung vorgestern fehlt. Eine einzelne Lücke allein versetzt den rückblickenden Mittelwert nicht: sechs von sieben Tagen reichen. Die bisherige harte Trennung von Importen und eigenen Messungen konnte aber nach einem Wechsel der täglich gewinnenden Quelle die native Gruppe ohne ausreichende Historie lassen.

Puls, RMSSD, SDNN und pNN50 teilen daher jetzt eine persönliche Referenzgruppe je Kennzahl über App-/Kurzprotokollgrenzen hinweg, soweit die Einheit bekannt ist. Das verhindert einen künstlichen Neustart von Mittelwert und Korridor nach dem Wechsel. Es belegt keine exakte Gleichwertigkeit unterschiedlicher Sensoren/Messbedingungen; Herkunft weiterhin am Tageswert zeigen. LF/HF unverändert nur bei bekannter nativer Einheit und getrennt nach Ruhe-/Messzeiten. Die zentrale letzte-Messung-pro-Tag-Regel, Ablehnung ungültiger Messwerte und Mindestdaten bleiben erhalten; keine Doppelzählung beider Apps und keine erfundenen Werte. Fenster bleiben bis einschließlich jeweiligem Tagesdatum (Normalbereich weiterhin vorherige 60 Tage). Keine lineare Extrapolation oder zentrierte Kurve.

Abschließende Nutzerentscheidung: keine Trennung der beiden Messreihen, ein gemeinsamer Bestand. Auch unterschiedliche eigene Protokollzeiten bilden keine getrennten Referenzgruppen mehr. Je Kennzahl werden alle verfügbaren kompatiblen Werte gemeinsam ausgewertet; Quellen/Protokolle bleiben reine Metadaten. CSV-LF/HF ohne erklärte Einheit werden weiterhin als Originalfelder erhalten und nicht stillschweigend in native ms²-Werte umgedeutet. Diese Entscheidung ersetzt die frühere Quellen-/Protokolltrennung auch für verfügbare Frequenzkennzahlen.

Prüfstand gemeinsamer Bestand: zehn gezielte Auswertungstests erfolgreich. Neue Fälle: fehlende vorgestrige Messung bei sechs von sieben verfügbaren Tagen; wechselnde täglich letzte App-Quelle mit aktueller Mittelwert-/Korridorberechnung; gemeinsame bekannte Frequenzkennzahlen über Protokollgrenzen. Tagesregel, Originaldateien, feste Achsen und Gesten weiterhin geprüft. Kein neuer Score, keine Extrapolation und kein APK-Build. Warum ein einzelner Messwert im aktuellen Nutzerbestand fehlt, ist ohne dessen Journal nicht festgestellt.

Veröffentlichung feste Achsen/Diagrammgröße/Farben/gemeinsamer Bestand: in APK **0.8.29 / Build 68** enthalten. [Buildnachweis](build-68.md). Frühere Angaben zu fehlender APK sind Verlauf.
