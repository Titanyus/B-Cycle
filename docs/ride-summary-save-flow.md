# Fahrtabschluss mit ausdrücklichem Speichern (08.11.2026)

Ersetzt den früheren sofortigen externen Export am Fahrtende. Nutzerentscheidung: intern sofort sichern, in der Zusammenfassung Notiz/RPE bearbeiten, danach über **jeden regulären Abschlussweg** die vorgesehenen externen Sicherungen starten. Keine Abbrechen-/Verwerfen-Aktion.

## Datenfluss und Haltbarkeit

1. RecordingStore beendet das Journal, erzeugt/behält die öffentliche Original-FIT und private vollständige Analysedatei, führt gegebenenfalls die aktivierte Höhenkorrektur aus und bestimmt den Fahrtnamen.
2. RideFinishTransfers.hold schreibt die vorgesehenen Ziele, den bisherigen Exportdateinamen, lokalen Zielbaum und nicht geheime Konto-Fingerabdrücke dauerhaft unter `ride-finish-transfers`. Noch keine externe Datei und keine neue Uploadqueue dieser Fahrt.
3. Erst nach erfolgreichen internen Dateien und dauerhafter Vormerkung wird die bisherige Staging-Bereinigung ausgeführt. Zusätzliche Guards prüfen permanente Dateien und beschränken die Journalbereinigung auf `pending-rides`. Interne Original-/Analysedateien werden nicht gelöscht. Bei einem internen Vorbereitungsfehler bleibt das Journal für den bisherigen Wiederherstellungspfad bestehen.
4. Die Zusammenfassung zeigt die intern gespeicherten Daten. Notiz/RPE werden bei jeder Änderung intern gespeichert. Während `held=true` werden keine externen Annotation-Aufträge gestartet, auch keine Debounce-Timer pro Eingabe benötigt.
5. Speichern, Zu den Details, Android-Zurück und Außentipp laufen alle über denselben Dismiss-Abschluss. Details öffnet erst danach die Streckendetails. Auch ein App-Wechsel schließt die aktive Zusammenfassung und gibt die Vormerkung frei; andere Dialogseiten behalten ihre bisherige Hintergrundregel.
6. release markiert den Plan freigegeben und stellt ihn einmal auf den seriellen Fahrtabschluss-Worker. Erst dieser schreibt den externen lokalen Export und registriert die jeweiligen Cloudjobs. Kein FIT-/Datei-/Netzwerkzugriff auf dem UI-Thread beim Buttondruck. Die Cloudjobs behalten ihre vorhandenen WLAN-, Konto-, Hash- und Wiederholungsregeln.

Jedes erfolgreich erledigte **Einplanen** wird aus der Restliste entfernt. Fehlgeschlagene Ziele bleiben erhalten, andere Ziele werden trotzdem versucht. Keine Behauptung von Upload-Erfolg allein wegen Queueannahme. Ein kurzer Hinweis meldet verbliebene Ziele, die Fahrt bleibt intern gesichert.

Bei Prozessabbruch übernimmt recover beim nächsten Runtime-/Appstart auch noch gehaltene Pläne; erfolgreich registrierte Cloudjobs liegen ohnehin bereits in den bestehenden persistenten Queues. Wiederholte Freigabe desselben abgeschlossenen Plans ist wirkungslos. Konto-Fingerabdruckwechsel führt nicht zu einem stillen Upload an ein anderes Konto; die betreffende Vormerkung bleibt offen. Passwörter/Tokens liegen nicht im Plan. Eine absichtlich neu gewählte Cloudverbindung kann weiterhin über die vorhandenen manuellen Uploadaktionen bedient werden.

Lokale FIT-Exporte werden vor ihrer Veröffentlichung per exaktem Exporthash der ursprünglichen Fahrt zugeordnet, damit Downloads-Scan und Wiederimport trotz RPE-/Notizbytes keine zweite Fahrt anlegen. [Nachgewiesener Fehler und Korrektur](fit-feedback-export-identity.md).

Löschen der Fahrt entfernt auch ihre Vormerkung. Original-/Analyse-FIT bleiben sonst unverändert. Der lokale Export erhält die **aktuelle** öffentliche Kopie einschließlich Feedback, seine Quittung bezieht sich wie im bestehenden manuellen Export auf die unveränderte öffentliche Fahrtquelle. Nach dem ersten Export erfolgt ein Feedbackabgleich, damit eine inzwischen in den Details bearbeitete Notiz ebenfalls die neu registrierte Kopie erreicht.

Für Intervals/Strava/Dreeve bleiben die bestehenden unveränderlichen Upload-Payloads und die anschließenden unterstützten Metadatenupdates erhalten: Intervals natives RPE plus Beschreibung; Strava/Dreeve Beschreibung. WebDAV und der externe Ordner erhalten die annotierte öffentliche FIT. Keine neue Aktivität nur wegen einer späteren Notiz.

## Zusammenfassung und kompakte RPE-Auswahl

Ab Folgeauftrag08.11.2026 unter den Akkuwerten gemeinsame randfüllende TERTIARY-Fläche ab früherer Trennerposition; Trenner unsichtbar, seine Geometrie und alle unteren Abstände erhalten. Buttons mit expliziter TERTIARY-Controlrolle, sonst gleiche Kontur-/Ripplegestaltung.

- Titel weiterhin anklickbarer Fahrtname. Eigenes `showBack=false` am Dialogbuilder entfernt nur hier das Zurück-Chevron; normale Dialoge unverändert.
- Statistikwerte ohne Icons weiterhin32dp kompakter.
- Keine sichtbare Frage „Wie anstrengend …“ mehr in der Zusammenfassung; zugängliche RPE-Beschreibung bleibt am Picker.
- Darunter eine Zeile: links dreizeiliges Notizfeld in normaler Schrift, rechts etwa30% Breite für CR10-Zahl/Beschreibung. Feste Schriftgrößen für alle Zustände einschließlich „–“ und0–10:64,8sp Zahl und12,6sp Beschreibung (gewähltes72/14-Paar anschließend10% kleiner). Keine dynamische Schriftanpassung an Wert, Breite oder Höhe; der gemeinsame sichtbare Block wird vertikal mittig zum Notizfeld angeordnet. League Gothic wie in den Streckendetails.
- Nach oben wischen erhöht, nach unten verringert den Wert. Grenzen0/10 ohne Umlauf. Keine Bewertung bleibt „– / Noch nicht bewertet“; bewusste0 zeigt „0 / Keine Anstrengung“. Ein erster Tap erlaubt0. Andere Einstufungen erhalten. Tastatur-Up/Down und Accessibility-Scroll/SetProgress unterstützt. Nur der Picker beansprucht seine vertikale Wischgeste.
- Kein horizontaler RPE-Slider in der Zusammenfassung. Der bestehende Slider im Bearbeitungsdialog der Streckendetails bleibt unabhängig erhalten.
- Ein Druck auf Dialoginhalt außerhalb der fokussierten Notiz entfernt Fokus und schließt die Tastatur; Text bereits intern gespeichert. Window-Callback berücksichtigt Bildschirmposition und sichtbaren Feldbereich.
- Darunter links „Speichern“, rechts „Zu den Details“, gleich hohe nebeneinanderliegende Buttons. Mit16dp Abstand folgt die kleine900-Black-Überschrift „Fahrt wird nach dem Speichern übertragen zu:“, darunter nur die Zielicons und kleine11sp-Beschriftungen; keine Kalender-/Statusicons bei vorgemerkten Zielen. Accessibility-Status „Vorgemerkt“ bleibt erhalten. RideSummaryTransfers besitzt seine Typografie selbst und wird nicht vom allgemeinen Dialogstyling auf14sp vergrößert.

Ohne zusätzlich konfigurierte externe Ziele bleibt die interne Fahrt trotzdem gesichert; keine neue Cloud-/Ordnerkonfiguration wird eigenmächtig aktiviert.

## Prüfungen und Grenzen

Nur gezielte Robolectric-Tests mit synthetischen Fahrten und isolierten temporären Appverzeichnissen; keine persönlichen Daten, keine echte Cloudübertragung, kein Gesamtlauf/APK.

- RecordingResultTest / RecordingStoreBaselineTest: permanente FIT-/Analysekopien vor Stagingbereinigung, Vormerkung statt sofortigem Upload, interner Vorbereitungsfehler bewahrt Journal, unveränderte Originalbytes, bestehende Wiederherstellung und Statusmeldungen.
- RideFinishTransfersTest: gehaltene Pläne führen keine externen Operationen aus; neueste Notiz/RPE im öffentlichen Export, nur fehlgeschlagene Ziele wiederholen; idempotente Freigabe, gehaltene Vormerkung beim Wiederanlauf, Kontoschutz; tatsächlicher SAF-Testprovider erhält annotierte FIT und passende Quittung.
- RideSummaryTest / RideSummaryExitTest: Detailsübergang, Save/Back/Outside-cancel/Background geben frei, Notiz bleibt intern, kein Zurück-Icon, horizontale Buttonzeile/Vormerkstatus und bündige RPE-/Notizgeometrie auf schmalen Breiten.
- RideRpePickerTest: leer≠0, beide Wischrichtungen,0–10-Begrenzung, Accessibility-Änderung, dreizeiliges Feld und Zeichnen.
- RideFeedbackTest / RideNameSyncTest: bestehende Notiz-/RPE-Dateiformate, Autosave und kontogebundene Metadata-/Namensregeln.
- RecordingUpdateUiTest / SummaryPresentationTest: bestehende Abschlussanzeigen und Fahrrad-/Akkuwerte.

Logs: `/workspace/.tools/summary-save-tests.log`, `summary-save-ui-tests.log`, `summary-save-transfer-final-tests.log`, `summary-save-recovery-ui-tests.log`. Zwischenzeitliche Testfixture-/Importprobleme korrigiert; finaler Stand siehe CONTINUE-HERE. UI-Screenshot `app/build/reports/ui/ride-summary.png` visuell geprüft. Zentrale Typografie-/UI-Katalog-/Bright-/Diffprüfungen. Physisches Wischgefühl und persönliche Cloudziele bleiben am Gerät zu bestätigen.

Die automatische Freigabeprüfung hatte anfangs einen kombinierten Änderungs-/Kompilierungsschritt wegen möglicher Quelllöschung abgelehnt. Der weitere Weg wurde mit expliziten Guards für interne Dateien/Staginggrenzen und isolierten Retentions-/Recovery-/Abbruchtests freigegeben und geprüft. Kein Zugriff auf persönliche Fahrtdaten.
