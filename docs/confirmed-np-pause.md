# NP-Pause ohne Bluetooth-Abschaltung · 13.11.2026

Bestätigter Stillstand ohne verwertbare Leistungsdaten darf als NP-Pause gelten, auch wenn das E-Bike eingeschaltet und BLE verbunden bleibt. Nutzer lässt das Rad bei Pausen oft zehn Minuten oder länger an. Die vorherige zusätzliche Bedingung einer vollständig getrennten Radverbindung verhinderte in dieser Situation die Pause und belastete die Datenabdeckung.

ConfirmedBikePause benötigt weiter E-Bike-Aufnahme, frische GPS-Beobachtung (höchstens5s alt), Genauigkeit höchstens25m, Geschwindigkeit unter0,7m/s und mindestens10s zusammenhängende Stillstandsbelege; fehlende/alte/schlechte GPS-Daten und Bewegung bestätigen keine Pause. Vorhandene Leistung einschließlich tatsächlich übermittelter0W wird nicht als Leistungsdatenlücke behandelt. BLE-Status ist nicht mehr Eingang dieser Entscheidung.

Keine Änderung der90%-NP-Schwellen, kein Auffüllen von Leistungsdaten. Pausenflag betrifft nur NP-Zeit und Fenstergrenzen, normale Fahr-/Aufnahmezeit bleibt erhalten. Bei bestehenden FIT-Dateien gilt deren tatsächlich gespeichertes Flag; keine neuen historischen Messbelege erfinden.

Regression: synthetische Fahrt mit je10Minuten200W vor/nach15Minuten bestätigtem Stillstand ohne Leistungsdaten und ohne vorausgesetzte BLE-Abschaltung ergibt NP200W. Zehn Bestätigungssekunden bleiben Datenlücke,890s bestätigte Pause nur aus NP-Zeit ausgeschlossen; gesamte2100s bleiben normale Fahrzeit. Freshness-/Genauigkeits-/Bewegungsprüfungen erhalten. ConfirmedBikePauseTest in unified-style-np-fit-final-verified-tests.log erfolgreich.
