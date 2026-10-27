# HRV: Mondphasen und schlichte datierte Header

Die obere Rasterlinie des HRV-Diagramms zeigt die vier Hauptphasen: Neumond, zunehmender Halbmond (rechts hell), Vollmond und abnehmender Halbmond (links hell). Symbole liegen auf dem jeweiligen lokalen Kalendertag, auch ohne Messwert. Sie bewegen sich mit dem Diagramm, werden an dessen horizontalen Grenzen abgeschnitten und verkleinern sich bei großem Zeitbereich ohne wechselnde Ausblendung. Ereignisse werden bei Änderung des Datenzeitraums oder der Zeitzone einmalig berechnet, nicht beim Zeichnen.

`LunarPhases` berechnet die tatsächlichen Hauptphasen offline nach Jean Meeus, Astronomical Algorithms, Kapitel49, einschließlich periodischer/planetarer Korrekturen und angenäherter Delta-T-Umrechnung. Keine Netzwerkabfrage oder zusätzliche Bibliothek. Es handelt sich um Kalenderinformationen; HRV-Kennwerte und Readiness bleiben unverändert. April2024-Referenzereignisse wurden auf eine Abweichung unter fünf Minuten geprüft, Zeitzonenwechsel und Jahresgrenzen separat.

Der Auswertungsheader lautet „Auswertung: dd.MM.yyyy“. Beim frischen Öffnen ist es der aktuelle lokale Tag. Erst eine bewusste Tagesauswahl im Diagramm setzt ein anderes Datum; automatische Initialauswahl, Metrikwechsel, Scrollen oder Zoom überschreiben ihn nicht. Die Hilfeseite wird unabhängig vom dynamischen Titel anhand der View/Host-Zuordnung ermittelt.

Messung, Auswertung, Messergebnis, Tagesbefinden und Verlauf verwenden schlichte Secondary-Header ohne Glow/Rahmen. Bestehendes Ausblenden der Android-Statusleiste und Fortsetzen des Seitenhintergrunds erhalten. Schlichte Header haben keinen zusätzlichen Headerschatten.
