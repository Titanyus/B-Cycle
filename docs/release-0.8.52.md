# B|CYCLE 0.8.52

Optimierte APK nach Build91, versionCode122, Android13+.

- Dashboard: Schaltgruppe → Schaltwerk als eigenes Akkufeld; zusätzlich in der Akku-Mehrfachauswahl für Sammelfelder und Kartenfelder.
- Letzter erfolgreich gelesener Schaltwerk-Akkustand bleibt über Trennung und App-Neustart erhalten. Anzeige ohne erneuten Garagenbesuch; Abfragezeitpunkt in der Sensoransicht.
- Pro Profil Schalter „Akkuwarnung bei unter 20% Kapazität“, standardmäßig an und in der Einstellungssicherung enthalten. Bei Aufzeichnungsstart einmaliger zehnsekündiger Hinweis für einen bekannten Stand von0–19%, im Stil der Trinkerinnerung. Fehlende Werte und20% lösen keine Warnung aus.
- Sensoransicht kompakter: Gerätename, letzter Akkustand/Zeitpunkt, Abfragestatus und tatsächlich empfangene Messwertarten mit Chevron. Lange Einführung aus der Ansicht entfernt, Hilfe bleibt verfügbar.
- Neues Schaltwerk-Symbol und besser lesbares App-Icon aus den Nutzerdateien übernommen.

Vorerst wird gemäß Nutzerentscheidung das zuerst zugeordnete SRAM-Gerät als Schaltwerk verwendet; keine Vermischung mit Controllerakkus. Bestehende zusätzliche Zuordnungen bleiben erhalten. Keine Gangdaten, ANT+-Anbindung oder Änderung an FIT-Aufzeichnungen.

25 gezielte Tests während der Umsetzung erfolgreich, einschließlich Aufzeichnungsstart, Warnschwelle/Timeout, Sammelfelder, Profilgrenzen, Altbackup-/Booleanprüfung und bestehender Trinkerinnerung. Typografie-/UI-Katalog-/Diffprüfung erfolgreich. Kein erneuter vollständiger Testlauf. Nutzer hat erfolgreiche SRAM-Erkennung/Akkuabfrage in Build91 bestätigt; diese Erweiterung wurde nicht erneut am Gerät getestet.

Buildnachweis folgt nach Veröffentlichung.
