# B|CYCLE 0.8.49

- Indoor-Typ **Rollentrainer** aktiviert, neben Hometrainer. Ein gemeinsames Rollentrainer-Profil für eingespanntes Fahrrad und Zwift-Ride-Rahmen. Auswahl pro Profil gespeichert, kopiert und mit den Einstellungen gesichert; alte Profile verwenden Hometrainer.
- Indoor-Distanz bevorzugt einen frischen Trainerzähler. Fehlt dieser, wird die Strecke aus zeitlich zusammenhängenden Geschwindigkeitsmessungen berechnet. Keine Hochrechnung bei Datenlücken, keine Sprünge beim Quellenwechsel; Korrekturfaktor einmalig berücksichtigt. Dieselbe Fahrtstrecke in Dashboard und FIT.
- Messwertanzahl in den Sensordetails mit kleinem Chevron aufklappbar. Liste der tatsächlich empfangenen Messwertarten samt Einheiten, ohne berechnete Ersatzwerte als Sensordaten auszugeben.
- **Profil zuordnen** steht im Profil-Footer direkt links von Löschen; weiterhin nur bei verwaisten eigenen Fahrten sichtbar. Gemeinsamer Zuordnungsdialog und Bestätigung unverändert.

18 unterschiedliche gezielte Tests erfolgreich: Distanzintegration, native Trainerzähler, synthetische Indoor-Fahrt ohne Distanzzähler bis zur FIT-Ausgabe, Profilwahl/Migration/Sicherung, aufklappbare Sensorliste, Pulsgurt und Zuordnungsfooter. Typografie-, UI-Atlas- und Diffprüfung erfolgreich. Kein Gesamttestlauf oder KICKR-Gerätetest.

KICKR CORE/CORE2 benötigen weiterhin einen Test mit realer Hardware/Firmware. Zwift Ride/Click und virtuelle Schaltung sind für später vorgemerkt; noch keine Controller-Anbindung, ERG-, Widerstands- oder Steigungssteuerung. Keine neuen Abhängigkeiten oder Änderungen vorhandener FIT-/Journalformate.

Optimierter Release über GitHub Actions. [Buildnachweis und Download](build-89.md).
