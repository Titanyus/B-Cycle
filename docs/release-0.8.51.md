# B|CYCLE 0.8.51

Optimierte APK nach Build90, versionCode121, Android13+.

- intervals.icu: neuer Sammelupload unter Daten & Speicher. Prüft vorhandene Aktivitäten vor dem Upload; berücksichtigt auch bestätigte Dreeve-Importe mit FIT oder zeitgestempeltem GPX. Fortschritt, Bilanz und Abbruch; unklare Zuordnungen werden übersprungen.
- Dreeve: bereits importierte eigene FIT-Fahrten zeigen bei bestehender Kontoverknüpfung den richtigen Uploadstatus.
- SRAM-Suche verarbeitet spätere Scanantworten und zeigt erkannte Kandidaten auch nach einer nicht abgeschlossenen Geräteprüfung. Keine Android-Systemkopplung erforderlich; tatsächlich lesbare Akkudaten weiter am Gerät prüfen.
- Dreeve-Importliste kompakter mit drei Zeilen, FIT-/GPX-Kennzeichen über dem Schalter und passender Druckmarkierung.
- HRV-Auswertung: Pinch-Zoom funktioniert auch bei eng zusammenliegenden Fingern kontinuierlich weiter. Scrollen, Nachlauf, Bereichsgrenzen und feste Y-Skala bleiben erhalten.

44 gezielte Tests für Sammelupload, Verknüpfungen, Sensorsuche und bestehende Transferwege sowie26 gezielte Import-/UI-/HRV-Gestenfälle erfolgreich. Typografie, UI-Katalog und Diff geprüft. Kein erneuter vollständiger Testlauf. Kein privates Cloudkonto, privater Dreeve-Server oder physisches SRAM-Gerät getestet.

[Buildnachweis und Download](build-91.md).
