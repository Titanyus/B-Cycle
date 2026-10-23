# Kartenquellen und UI-Ergänzungen

## Quellen

- CyclOSM und OpenStreetMap: bestehende Quellen unverändert.
- OpenTopoMap: HTTPS-Rasterkacheln von a.tile.opentopomap.org, Zoom 2–17,
  Attribution OpenStreetMap/SRTM/OpenTopoMap mit CC-BY-SA-Verweis.
- OpenCycleMap: HTTPS-Kacheln von tile.thunderforest.com/cycle, Zoom 2–19,
  mit eigenem Thunderforest-API-Schlüssel. Attribution Thunderforest/OpenStreetMap.

Der neue Schlüssel wird direkt unter Kartenquelle eingebbar, wenn OpenCycleMap
gewählt ist. 32 hexadezimale Zeichen; unvollständige Eingaben überschreiben keinen
vorherigen Schlüssel. Ohne Schlüssel bleibt die zuvor gewählte Karte aktiv.
Wird eine OpenCycleMap-Einstellung ohne lokalen Schlüssel wiederhergestellt,
verwendet die Darstellung CyclOSM. Kein schlüsselloser Thunderforest-Request.

Speicherung ausschließlich in den privaten App-Einstellungen map-provider.
Nur im passwortgeschützten AES-256-ZIP der Einstellungssicherung enthalten, nie im öffentlichen JSON; Android-Backup ist für
die App bereits deaktiviert. Kein Schlüssel in URLs der Konfiguration, Dateinamen
oder eigenen Diagnosemeldungen. Formatprüfung ist keine serverseitige
Authentifizierungsprüfung. Nachtansicht und Track-/Positionslayer bleiben erhalten.
Quellenwechsel und Schlüsselwechsel ersetzen den Downloadlayer korrekt;
OpenTopoMap begrenzt auch Kamera, Zoomtasten und Strecken-Einpassung auf Zoom 17.
Dashboard und Streckendetailkarte verwenden dieselbe Quellenauflösung.

## Höhenlinien und Schummerung

OpenTopoMap enthält diese Darstellungen im fertigen Rasterbild. Sie können dort
nicht getrennt an- oder abgeschaltet werden. Deshalb wurden keine funktionslosen
Schalter ergänzt. Unabhängige Optionen erfordern zusätzliche, passend lizenzierte
Kartenebenen beziehungsweise eine andere Renderingquelle.

## Anbieterstatus und Grenzen der Prüfung

Das offizielle OpenTopoMap-Repository kündigt die Ablösung der bisherigen
Rasterkacheln durch Vektorkacheln an:
https://github.com/der-stefan/OpenTopoMap

OpenCycleMap-Schlüssel und Nutzungsbedingungen:
https://www.thunderforest.com/docs/apikeys/
https://www.thunderforest.com/terms/

Der GitHub-Quelltext war lesbar. Direkte Anbieter-/Kachelzugriffe sind in dieser
Entwicklungsumgebung durch die Netzwerk-Domainfreigabe blockiert (Proxy 403).
Keine Live-Verfügbarkeit oder gültige Thunderforest-Zugangsdaten getestet;
keine fremden Schlüssel verwendet. Die Rasterquelle ist deshalb am Gerät zu prüfen.

## UI und Validierung

Graph-Auswahl: 8 dp vor der ersten Auswahl, Farbpunkte von 7 auf 10,5 dp
vergrößert. Accordions appweit wie Dropdowns: 16 sp, normale Systemschrift,
linksbündig. Header mit nach unten auslaufendem Schatten, ohne zusätzliche
Layoutfläche, Shader einmalig vorbereitet.

31 gezielte Tests insgesamt: Feldauswahl, Zonen-/FIT-Accordions, Sticky-Geometrie,
Schattenverlauf, Karten-URL/Schlüssel/Zoomgrenzen, Karten-Einstellungen,
Kartensteuerung/Nachtansicht und Themewechsel. Ein erster URL-Test erwartete den
expliziten HTTPS-Port 443 nicht; die URL-Komponenten wurden korrekt geprüft und
die beiden Provider-Tests danach erfolgreich wiederholt. Release-Kotlin-
Kompilierung erfolgreich; keine vollständige Testsuite und keine neue APK.
