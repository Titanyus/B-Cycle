# Interaktiver Eingabefeld-Designer

[Designer im Browser öffnen](https://raw.githack.com/Titanyus/B-Cycle/d52c9fae8cbf715513bf0beb32dbb7fd7b218809/docs/previews/input-designer.html) · [HTML für Offline-Nutzung herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/d52c9fae8cbf715513bf0beb32dbb7fd7b218809/docs/previews/input-designer.html)

Beim ersten Öffnen des Browser-Links zeigt raw.githack gegebenenfalls einen Herkunftshinweis. Mit „Open the page“ geht es zum Designer.

Die einzelne HTML-Datei enthält League Gothic samt Lizenz und die 14 App-Paletten, lädt keine externen Ressourcen und funktioniert nach dem Herunterladen offline. Kontur und aktive Innenfläche lassen sich unabhängig bearbeiten: Winkel, verschiebbare Farbstops, schmale Highlights, Theme-Ableitungen, eigene Farben, Konturstärke sowie Nah- und Außen-Blur mit SCREEN. Die Ebenenschalter helfen beim Vergleich. Felder und Segmentauswahl sind sofort in der Vorschau sichtbar.

Mit „JSON exportieren“ werden alle Parameter und abgeleiteten Farben gespeichert. Diese Datei kann später wieder importiert oder für die Übernahme in die App weitergegeben werden. „PNG exportieren“ speichert die Vorschau. Browseränderungen bleiben lokal gespeichert; Zurücksetzen stellt die aktuellen App-Standardwerte wieder her. Der Designer ändert keine App-Einstellung und überträgt keine Daten automatisch.

Erzeugung: `python3 scripts/build-input-designer.py`; Konsistenz: derselbe Aufruf mit `--check`. Quelle ist input-designer.template.html; Paletten und Schrift kommen aus dem Repository. Browser-Blur und native Box-Blur-Näherung können geringfügig abweichen. Die native App bleibt Referenz.

Geprüft in Chromium bei 1440 und 390 Pixel Breite: keine horizontale Überbreite, alle 14 Paletten und Schrift, Live-Änderungen, Stops ziehen, Ebenen, JSON-Export/Import, ungültiger Import, Wiederherstellung nach Reload. Öffentlicher Vorschau-Link mit HTTP 200 und denselben funktionalen Browserprüfungen nach Bestätigung des Herkunftshinweises geprüft.
