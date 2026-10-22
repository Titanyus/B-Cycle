# HRV-Auswertung

Eigener nativer Bildschirm: HRV-Einstellungen → HRV-Auswertung oder nach dem erfolgreichen Speichern des Tagesbefindens über „Zur Auswertung“. Vorhandene Zurück-Navigation und HRV-Header in Fensterfarbe erhalten. Sechs Kennzahlen wie im Vorbild: Puls, SDNN, RMSSD, pNN50, LF und HF. Oben Auswahl mit den Werten des ausgewählten Tages, darunter Balken, Mittelwert und Normalbereich. Standard: RMSSD und letzte 14 Kalendertage. Tageswert antippen und „Messung und Tagesbefinden“ öffnet vorhandene Details.

## Transparente eigene Berechnung

Keine Kopie der nicht vollständig veröffentlichten HRV4Training-Korridorformel und kein neuer Readinessscore. Arithmetische Mittelwerte in der angezeigten Einheit, auch für RMSSD; kein versteckter Median und keine Alterskorrektur.

- Zuerst die zentrale Tagesregel: nur die letzte Messung je gespeichertem lokalem Tag. Kein Ersatz durch eine ältere erfolgreiche Messung.
- Eigene abgelehnte Messungen, fehlende Kennzahlen und reine CSV-Kontexttage liefern keinen Diagrammwert. Keine Nullwerte als Ersatz oder Interpolation fehlender Tage.
- Linie: Mittelwert der verfügbaren Tageswerte im aktuellen und den sechs vorherigen Kalendertagen, mindestens fünf Werte.
- Normalbereich: Mittelwert ± eine Stichproben-Standardabweichung der verfügbaren Tageswerte aus den 60 **vorherigen** Kalendertagen, mindestens 14 Werte. Stichproben-SD mit Nenner n−1; Untergrenze mindestens null. Der aktuelle Wert beeinflusst seinen eigenen Korridor nicht. Dies ist ein beschreibender Schwankungsbereich, kein Konfidenzintervall und keine Trainingsempfehlung.
- Quellen und Protokolle getrennt: importierte HRV4Training-Zusammenfassungen bilden eine eigene Referenzgruppe. Eigene Messungen mit gleichen gespeicherten Ruhe-/Messzeiten teilen eine Gruppe; unbekannte alte Protokolle bilden eine weitere Gruppe. Keine stillschweigende Gleichsetzung unterschiedlicher Protokolle.
- Importierte kompatible Kennzahlen dürfen angezeigt werden, obwohl ihre Rohdatenqualität nicht nachprüfbar ist; Herkunft klar am gewählten Tag. CSV-LF/HF mit unbekannter Einheit bleiben Originalfelder in den Details und werden nicht als ms² in das native Diagramm übernommen.
- Linie und Fläche verbinden nur unmittelbar aufeinanderfolgende Kalendertage derselben Gruppe mit verfügbaren Werten/Statistiken. Lücken und Gruppenwechsel nicht überbrücken.

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
