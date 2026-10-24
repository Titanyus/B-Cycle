# Interaktiver Eingabefeld-Designer

[Designer im Browser öffnen](https://raw.githack.com/Titanyus/B-Cycle/cd6b18ea96020f88b7ac59ba890fde5cc030c6ed/docs/previews/input-designer.html) · [HTML für Offline-Nutzung herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/cd6b18ea96020f88b7ac59ba890fde5cc030c6ed/docs/previews/input-designer.html)

Beim ersten Öffnen des Browser-Links zeigt raw.githack gegebenenfalls einen Herkunftshinweis. Mit „Open the page“ geht es zum Designer.

Die einzelne HTML-Datei enthält League Gothic samt Lizenz und die 14 App-Paletten, lädt keine externen Ressourcen und funktioniert nach dem Herunterladen offline. Kontur und aktive Innenfläche lassen sich unabhängig bearbeiten: Winkel, verschiebbare Farbstops, schmale Highlights, Theme-Ableitungen, eigene Farben, Konturstärke sowie Nah- und Außen-Blur mit SCREEN. Die Ebenenschalter helfen beim Vergleich. Felder und Segmentauswahl sind sofort in der Vorschau sichtbar.

Mit „JSON exportieren“ werden alle Parameter und abgeleiteten Farben gespeichert. Diese Datei kann später wieder importiert oder für die Übernahme in die App weitergegeben werden. „PNG exportieren“ speichert die Vorschau. Browseränderungen bleiben lokal gespeichert; Zurücksetzen stellt die aktuellen App-Standardwerte wieder her. Der Designer ändert keine App-Einstellung und überträgt keine Daten automatisch.

Erzeugung: `python3 scripts/build-input-designer.py`; Konsistenz: derselbe Aufruf mit `--check`. Quelle ist input-designer.template.html; Paletten und Schrift kommen aus dem Repository. Browser-Blur und native Box-Blur-Näherung können geringfügig abweichen. Die native App bleibt Referenz.

Geprüft in Chromium bei 1440 und 390 Pixel Breite: keine horizontale Überbreite, alle 14 Paletten und Schrift, Live-Änderungen, Stops ziehen, Ebenen, JSON-Export/Import, ungültiger Import, Wiederherstellung nach Reload. Öffentlicher Vorschau-Link mit HTTP 200 und denselben funktionalen Browserprüfungen nach Bestätigung des Herkunftshinweises geprüft.


## Nachtrag: Glow-Intensität und aktive Auswahl

Zusätzlich zur Deckkraft gibt es für nahen und äußeren Schein je einen Intensitätsregler von 0 bis 2000 % (0–20-fach). Standard 100 % erhält die bisherige Wirkung. Verstärkt wird ausschließlich der Alphakanal des bereits weichgezeichneten Konturbildes; danach folgen Deckkraft und SCREEN/Normal. Dadurch lassen sich dünne, weit verteilte Konturen aufhellen, ohne Blur-Radius oder Konturstärke zu verändern. Die Deckkraft bleibt bei 0–100 %. Sehr hohe Verstärkung sättigt den Schein entsprechend.

Die aktive Auswahl verwendet dieselbe Verlaufs-Kontur und beide Glows wie das Fokusfeld. Zusätzlich erhält sie den separat eingestellten Flächenverlauf. Inaktive Auswahl unverändert; Ebenenschalter wirken auf beide aktiven Beispiele.

JSON und lokale Speicherung enthalten `near.intensity` und `outer.intensity`. Alte Schema-1-Entwürfe ohne diese Werte werden mit 1 (= 100 %) geladen; unzulässige Werte werden abgewiesen. Neue Intensitätseinstellung und Auswahlkontur sind zunächst nur im Visualisierer umgesetzt, nicht in APK 0.8.35.

Gezielte Chromium-Prüfungen: nachgewiesene Helligkeitssteigerung außerhalb der aktiven Auswahl bei gleicher Blur-Breite, gemeinsame Kontur-/Glow-Ebenenschalter ohne Änderung der Innenfläche, Altimport/ungültige Werte und JSON-/Reload-Erhalt der Intensität. Bisherige Desktop-/Mobil-, Farb-/Stop- und Exportprüfungen weiter erfolgreich. Native App unverändert, kein APK-Build.


Die Stil-Basisfarbe heißt ab 24.10.2026 PRIMARY. Bestehende Schema-1-Entwürfe behalten die technischen Namen accent/Stop-Typ accent für diese Basisfarbe, damit Export/Import kompatibel bleiben. Das ist unabhängig vom weiterhin separat gespeicherten ACCENT der App. Siehe design-modes.md.
