# Ursprünglicher Architekturplan: Fahrradprofile

**Status:** Fahrradprofile und die gemeinsame Profilseite sind umgesetzt. Maßgeblich ist [Fahrradbezogene Profile](bicycle-profiles.md); die folgenden ursprünglichen Überlegungen sind historisch.

Vorgesehen sind vier frei benennbare Fahrradprofile mit Typ E-Bike, normales Rad
oder Indoor-Bike. Das bestehende E-Bike wird bei der Migration zum ersten Profil.

Jedes Profil erhält seine Sensorzuordnungen und passende Quellenfähigkeiten:
Bosch/GPS/Puls beim E-Bike, GPS/Power-/Geschwindigkeits-/Kadenzsensor/Puls beim
normalen Rad, BLE-/FTMS-Trainer und optionale Sensoren beim Indoor-Bike.
Indoor benötigt kein GPS. Matrix ICR50 siehe bestehendes BLE-Briefing.

Dashboard-Belegung und FIT-Feldauswahl pro Rad speichern. Sensor-/Feldauswahl und
FIT-Hauptquellen zentral aus konfigurierten Fähigkeiten ableiten, nicht aus einem
kurzfristigen Verbindungsstatus. Vorhandene Datenfelder und stabile IDs erhalten.
Während einer laufenden Aufnahme kein Profilwechsel.

Die Behebung der Aufnahmegrenzen erfolgt vorher. Diese Datei hält den anschließend
umzusetzenden Profilauftrag fest und stellt keine bereits vorhandene Funktion dar.

Der erste native E-Bike-Aufbau ist jetzt in [Bikes](bicycle-profiles.md) umgesetzt.
Die übrigen Radtypen und zusätzliche BLE-Leistungs-/Trainerquellen bleiben vorbereitet.
