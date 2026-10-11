# Fahrwerte und Bedienung – 0.6.16

- GPS-Gesamtdurchschnitt: Strecke und Nenner stammen vom selben akzeptierten GPS-Zeitpunkt. Laufende UI-/Pulsmesser-Aktualisierungen erhöhen weiterhin die Fahrtdauer, verdünnen aber nicht mehr eine noch unveränderte Strecke. Verzögerte GPS-Zustellung wird berücksichtigt. Keine künstlich monotone Begrenzung: echte Pausen und langsameres Fahren dürfen den Durchschnitt senken.
- Ø Kadenz: lokales Mittel ausschließlich positiver neuer Bosch-Kadenzmessungen. Keine Mehrfachzählung gecachter Werte bei anderen BLE-Paketen. START und Fahrradwechsel setzen es zurück; Null und fehlende Werte tragen nicht zum Mittel bei. Sensordaten bleiben unverändert. Die FIT-Lap-/Session-Zusammenfassung nimmt ebenfalls Tretpausen aus; Record-Messages behalten ausdrücklich Kadenz 0.
- Durchschnitts-Chevrons: GPS-Bewegungs-/Gesamtschnitt, Bosch-Tempo/-Leistung, lokale Kadenz und Herzfrequenz. Richtung aus rund zehn Sekunden Verlauf, mindestens acht Sekunden Anlauf, neutrale Änderungen bis 0,05 ohne Pfeil. Pro Feld maximal zwölf Einträge, höchstens ein Eintrag pro Sekunde; fehlende Werte und Unterbrechungen über 15 Sekunden verwerfen den Verlauf. Gilt auch für belegte Kartenfelder.
- FIT-Bewegungszeit: offizielles Garmin Session-Feld `total_moving_time` (59, Sekunden, Skalierung 1000). Timer-/Gesamtzeit werden nicht in Bewegungszeit umgedeutet. Berechnung aus der gewählten Geschwindigkeitsquelle: mindestens 0,7 m/s, keine Brücke über fehlende Geschwindigkeit oder Messlücken über zehn Sekunden. Import bevorzugt vorhandene Session-Bewegungszeit, sonst Record-Geschwindigkeit. GPX verwendet zusammenhängende Zeit-/Positionspaare. Ohne nutzbare Bewegungszeit wird kein Bewegungsschnitt erfunden.
- Streckenmanager (nachträgliche UI-Korrektur, noch nicht in APK 0.6.16): Kennzahlenzeile enthält ausschließlich Kilometer und Bewegungszeit, keinen Durchschnitt. Ohne bekannte Bewegungszeit erscheint nur die bekannte Distanz; die Gesamtzeit wird nicht als Bewegungszeit ausgegeben. Bewegungszeit wird im vorhandenen privaten Index gespeichert und für alte Einträge einmalig nachgelesen; Originaldateien und Importdatum bleiben erhalten.
- Dashboard-Leiste: START, Streckenmanager direkt, Einstellungen. Beide rechten Buttons gleich breit und Secondary-Farbe; gemeinsames Ein-/Ausblenden bleibt bestehen.
- Lokale Lucide-0.468.0-Vektoren: GPS `satellite-dish`, lokale Speicherung `hard-drive-download`; bestehende Lizenz gilt, keine neue Abhängigkeit.
- Glow: 28,8dp statt 24dp maximale Breite, drei additive Lichtbeiträge (Sättigung am Displaymaximum), 1,5 Sekunden Überblendung einschließlich AUS. Shader werden bei Ziel-/Größenwechseln erzeugt, während der Überblendung wiederverwendet. Keine dauernde Animation und keine Layoutänderung.
- WebDAV: akzeptiert vollständige Nextcloud-WebDAV-Wurzel; Zielordner relativ dazu oder leer für die Wurzel. Passwort-Auge zeigt/versteckt den Entwurf; gespeichertes Passwort wird erst beim expliziten Tippen entschlüsselt. Downloads-Kurzbutton entfernt, bestehender Standardpfad als Fallback erhalten.

Die automatisierten Prüfungen ersetzen keine echte Fahrt oder den Test mit dem eigenen Nextcloud-Server.

## Verifiziert

Vollständige Testsuite und geschrumpfter Release-Build: 164 Tests, 0 Fehler, 0 Auslassungen; 1 Minute 7 Sekunden. Neue Regressionen prüfen synchronisierte GPS-Durchschnitte bei zusätzlichen Bildschirmticks/verzögerter Zustellung, Kadenzpausen ohne Veränderung der offiziellen FIT-Records, Garmin-Validierung inklusive Bewegungszeit, begrenzte Trendhistorie, unterbrochene Glow-Fades sowie Breite/Theme/Navigationsziel der neuen Dashboard-Abkürzung. Bewegungszeit bleibt beim Index-Neuladen erhalten.

APK 0.6.16 (Code 32): 901.581 Bytes, vorhandenes Testzertifikat, Signatur erfolgreich geprüft. Nur APK und SHA-256-Datei wurden auf GitHub veröffentlicht (Commit `26d64e1d4a0dc1a3e17d6c8e317717b4ccd5a7d3`); heruntergeladene APK bytegenau verglichen. Bekannte Warnung: veraltetes Android-`onBackPressed()` in bestehender Navigation; keine neue Compilerwarnung.

## Chevron bei langsam veränderlichen Durchschnittswerten

Die Trendanzeige vergleicht weiterhin Werte über ungefähr zehn Sekunden. Die frühere
Mindeständerung von 0,05 ließ den Pfeil auf längeren Fahrten verschwinden, obwohl
der Schnitt kontinuierlich stieg. Jetzt wird nur numerisches Rundungsrauschen
(Änderung bis 0,000001) ignoriert. Ein unveränderter Schnitt hat keinen Pfeil.
Nach mehr als 15 Sekunden ohne Anzeige beginnt das Beobachtungsfenster neu;
bei anhaltendem Trend erscheint das Chevron nach acht Sekunden wieder.
