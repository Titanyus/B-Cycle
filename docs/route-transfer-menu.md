# Upload-Menü in Streckendetails

Footer seit 18.10.2026: Anzeigen, Umbenennen, Höhe korrigieren, Upload, Löschen. Titel bleibt zusätzlich antippbar zum Umbenennen. Im Header steht Info statt Stift.

Upload öffnet ein am Wolkenbutton verankertes natives Dropdown. Es enthält Lokal speichern, WebDAV, intervals.icu, Dreeve und Strava. Jede Zeile zeigt Diensticon, Namen und eigenen Status. Bestätigte Erfolge erhalten einen Haken; abgeschaltete Dienste zeigen „aus“. Weitere Zustände: bereit, vorgemerkt/wartend, Übertragung/Verarbeitung, Fehler mit erneuter Versuchsmöglichkeit, fehlende Datei oder ungeeignete Fahrt. Erfolge hängen an Dienst/Konto und aktivem Dateistand; kein globaler Haken im Footer.

Ein Tipp startet ausschließlich das gewählte Ziel mit dessen vorhandener Queue. Ausgeschaltete und laufende Cloudziele sind nicht erneut startbar; ein kurzer Tipp auf erfolgreiche Cloudziele bleibt ohne Aktion. Andere Ziele bleiben unabhängig. Cloud-Uploads erlauben gefahrene FIT und vollständig zeitgestempelte, als Gefahren gespeicherte GPX über den vorhandenen FIT-Konverter; geplante Tracks und Zusammenfassungen werden nicht als Aktivitäten hochgeladen. Dateizugriffe/Hashing und Queue-Aufbau laufen über den vorhandenen MapState-Worker.

Lokal speichern erhält den bisherigen manuellen Export nach Downloads/B-Cycle, unabhängig vom Schalter für automatische lokale Speicherung. Eine erfolgreiche lokale Kopie zeigt einen eigenen Haken und kann bei Bedarf erneut exportiert werden. Ein lokaler Erfolg sagt nichts über Cloudziele aus.

Das Menü bleibt nach Auswahl offen. SharedPreferences-Listener aktualisieren die vorhandenen Uploadbelege; sie werden beim Schließen abgemeldet. Es gibt kein neues Netzwerkpolling und keinen Datei-Hash auf dem UI-Thread. Fehlermeldungen bleiben knapp und enthalten keine Zugangsdaten. Konfigurieren steht als fester Button unter der Liste und öffnet Daten & Speicher direkt bei Streckendaten. Zurück führt zu denselben Details.

## Manuelle Prüfung und Wiederholung per langem Druck

Ab 600 ms auf eine erfolgreiche Zeile des aktivierten Ziels: Aktion über MapState-Worker, keine Netzwerk-/Hasharbeit im UI. Vor Ablauf des Zeitfensters loslassen, Scrollen/Mehrfingerkontakt, Detach oder Schließen bricht die Geste ab. Während anderer Bibliotheksaktionen gesperrt; Long-Click-Accessibility bleibt erhalten. Kurze Tipps behalten ihr Verhalten.

| Ziel | Verhalten bei langem Druck |
| --- | --- |
| Dreeve | GET der bekannten Aktivitäts-ID; bei vorhandenem, passendem ID-Nachweis kein Upload. Nur 404 bestätigt fehlende Aktivität; dann neue dauerhafte Uploadoperation mit eigenem Dateinamen. |
| intervals.icu | GET der bekannten Aktivitäts-ID; vorhanden belassen, bei 404 neuen Upload vormerken. |
| Strava | Mit bestehendem activity:write-Zugang erneut übertragen. Keine vermeintliche Löschung aus fehlenden Leserechten ableiten; alte Verknüpfung erhalten. Strava kann Duplikate ablehnen. |
| WebDAV | Dasselbe Dateiziel erneut über vorhandenen bedingten PUT und SHA-256-GET prüfen/wiederherstellen; unterschiedliche Zieldateien nicht überschreiben. |
| Lokal | Neue Kopie nach Downloads/B-Cycle. Alte Belege speichern keine eindeutige Datei-URI; daher keine behauptete Existenzprüfung und keine Löschung vorhandener Kopien. |

Bei alten Dienstbelegen ohne Aktivitäts-ID ist nur erneute Übertragung möglich. Netzfehler, Authentifizierungsfehler und unplausible Antworten sind keine Löschbestätigung und lassen Belege/Links unverändert. Konto, Aktivierung und Quelldatei vor Übernahme erneut prüfen. Nur die eindeutig fehlende Remote-ID und ihre Namensaufträge entfernen, andere Dienste/Versionen und lokale Namen erhalten. Fehlender Dreeve-Herkunftslink geplanter Tracks darf gelöscht werden, ohne daraus einen Aktivitätsupload zu machen. Keine periodische Fernlöschprüfung, kein neues Sicherungsformat oder zusätzliche Einstellung.

Prüfung am 23.10.2026: gezielte Tests für echte 600-ms-Grenze, kurze Tipps, Scroll-/Detach-Abbruch, Sperren und bestehende Menüaktualisierung; GET-Nachweise/404/Auth-/Serverfehler, Konto-/Dateiwechsel, neue dauerhafte Versuche nur für betroffene Ziele, Namens-/Linkerhalt, geplante Dreeve-Strecken, Strava-Schreibrechte und lokale Downloadkopien. Kein echter Cloudkontentest, keine APK für diesen Auftrag.

Stand 23.10.2026: die beschriebenen neuen Änderungen sind in APK **0.8.34 / Build 73** enthalten. [Buildnachweis](build-73.md). Frühere Angaben zu fehlender APK sind Verlauf.
