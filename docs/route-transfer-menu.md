# Upload-Menü in Streckendetails

Footer seit 18.10.2026: Anzeigen, Umbenennen, Höhe korrigieren, Upload, Löschen. Titel bleibt zusätzlich antippbar zum Umbenennen. Im Header steht Info statt Stift.

Upload öffnet ein am Wolkenbutton verankertes natives Dropdown. Es enthält Lokal speichern, WebDAV, intervals.icu, Dreeve und Strava. Jede Zeile zeigt Diensticon, Namen und eigenen Status. Bestätigte Erfolge erhalten einen Haken; abgeschaltete Dienste zeigen „aus“. Weitere Zustände: bereit, vorgemerkt/wartend, Übertragung/Verarbeitung, Fehler mit erneuter Versuchsmöglichkeit, fehlende Datei oder ungeeignete Fahrt. Erfolge hängen an Dienst/Konto und aktivem Dateistand; kein globaler Haken im Footer.

Ein Tipp startet ausschließlich das gewählte Ziel mit dessen vorhandener Queue. Ausgeschaltete, laufende und bereits erfolgreiche Cloudziele sind nicht erneut startbar. Andere Ziele bleiben unabhängig. Cloud-Uploads behalten die bestehende Grenze auf eigene FIT-Fahrten; Importe werden dadurch nicht automatisch freigegeben oder übertragen. Dateizugriffe/Hashing und Queue-Aufbau laufen über den vorhandenen MapState-Worker.

Lokal speichern erhält den bisherigen manuellen Export nach Downloads/B-Cycle, unabhängig vom Schalter für automatische lokale Speicherung. Eine erfolgreiche lokale Kopie zeigt einen eigenen Haken und kann bei Bedarf erneut exportiert werden. Ein lokaler Erfolg sagt nichts über Cloudziele aus.

Das Menü bleibt nach Auswahl offen. SharedPreferences-Listener aktualisieren die vorhandenen Uploadbelege; sie werden beim Schließen abgemeldet. Es gibt kein neues Netzwerkpolling und keinen Datei-Hash auf dem UI-Thread. Fehlermeldungen bleiben knapp und enthalten keine Zugangsdaten. Konfigurieren steht als fester Button unter der Liste und öffnet Daten & Speicher direkt bei Streckendaten. Zurück führt zu denselben Details.
