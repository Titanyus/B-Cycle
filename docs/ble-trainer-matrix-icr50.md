# Integrationsgrundlage: Matrix ICR50 über BLE

Stand: 12.10.2026. Für eine spätere Hometrainer-Integration in B|Cycle vorgemerkt.
Dieses vom Nutzer bereitgestellte Implementierungsbriefing beschreibt den erfolgreich
erprobten Referenzweg. Die Integration in B|Cycle ist noch nicht umgesetzt.

## Referenztest

Erfolgreich getestet mit ANT+ Explorer 0.1.2 auf Xiaomi 24122RKC7G, Android 16.

## Verbindung

- Bluetooth Low Energy (BLE), standardisiertes FTMS.
- Gerätename im Test: „JFICcycle“.
- Keine ANT-Dienste für diesen Weg erforderlich.
- Wahoo/Jepster vor dem Verbinden vom Trainer trennen.
- Gerät bewusst auswählen; kein vorheriges Android-Pairing nötig.

## GATT

- Fitness Machine Service: `00001826-0000-1000-8000-00805f9b34fb`.
- Indoor Bike Data: `00002ad2-0000-1000-8000-00805f9b34fb`.
- Benachrichtigungen lokal aktivieren und CCCD
  `00002902-0000-1000-8000-00805f9b34fb` schreiben.
- GATT-Operationen, insbesondere CCCD-Schreibzugriffe, seriell ausführen.
- Zusätzlich vorhanden: Cycling Speed/Cadence Service `1816`, Messmerkmal `2A5B`.
  Abonniert, separater Datenempfang bisher nicht bestätigt. FTMS liefert bereits Trittfrequenz.

## FTMS-Dekodierung

Little Endian; 16-Bit-Flags bestimmen Reihenfolge und vorhandene Felder.

| Flag | Feld | Dekodierung |
| --- | --- | --- |
| Bit 0 **nicht** gesetzt | Geschwindigkeit | `uint16 / 100` → km/h |
| Bit 2 gesetzt | Trittfrequenz | `uint16 / 2` → U/min |
| Bit 4 gesetzt | Distanz | `uint24` → Meter |
| Bit 6 gesetzt | Leistung | `int16` → Watt |
| Bit 9 gesetzt | Puls | `uint8` → bpm |
| Bit 11 gesetzt | Trainingszeit | `uint16` → Sekunden |

Weitere gesetzte Felder korrekt überspringen. Frames wechseln zwischen unterschiedlichen
Feldkombinationen: Nicht enthaltene Felder erhalten; ausdrücklich ungültige Werte als
fehlend behandeln. Unvollständige Frames verwerfen. **0 W ist ein gültiger Wert.**

## Bestätigter Empfang

- 27 Live-Messdatenereignisse im Diagnosebericht.
- Beispielwerte: 5 W, 40 U/min, 10,9 km/h, 0,12 km, 37 s.
- Kein Puls empfangen.
- Gerätewerte wurden übertragen; ihre physikalische Genauigkeit wurde nicht vermessen.

## Android und Lebenszyklus

- Kotlin, Android 13+, native Views wie bestehendes B|Cycle.
- `BLUETOOTH_SCAN` (`neverForLocation`) und `BLUETOOTH_CONNECT` deklarieren und
  zur Laufzeit „Geräte in der Nähe“ anfordern.
- BLE-Suche zeitlich begrenzen; Verbindung nach Geräteauswahl mit `TRANSPORT_LE`.
- Verbindung und tatsächlichen Messdatenempfang getrennt anzeigen.
- Nach 5 Sekunden ohne neue Messwerte Werte als veraltet markieren.
- UI-Updates auf dem Main-Thread; alte Callbacks nach Trennen ignorieren.
- Scan stoppen und GATT-Verbindung beim Beenden freigeben.

## Getestete Referenz

[ANT+ Explorer v0.1.2](https://github.com/Titanyus/ANTplusExplorer/tree/v0.1.2)

Unter `app/src/main/java/de/titanyus/antplusexplorer/`:

- `BleTrainerClient.kt`: Suche, Verbindung, Diensterkennung, Datenabos.
- `BleMeasurements.kt`: FTMS-/Cycling-Power-/CSC-Parser.
- `TrainerSnapshot.kt`: Datenmodell und Empfangsstatus.

Unter `app/src/test/java/de/titanyus/antplusexplorer/`:

- `BleMeasurementsTest.kt`: Parser-Testfälle.

## Übernahme und Umfang

BLE-Client und Parser in B|Cycles bestehende Sensor-/Datenarchitektur integrieren;
bestehende UI-Komponenten und Designgrundlagen verwenden.
**Keine ANT-Bibliothek für BLE übernehmen.**

Zunächst ausschließlich Datenempfang. **Keine Widerstands-, Leistungs-, Start-/Stop-
oder Kalibrierbefehle senden.** Langzeitstabilität und Wiederverbindung sind noch zu testen.
