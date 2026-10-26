# SRAM AXS: Akku-Anbindung als erster Schritt

Stand26.10.2026 nach Build89. E-Bike → Sensoren → Schaltgruppe als reguläres Schalter-Accordion. Mehrere Komponenten einzeln über die vorhandene Sensorsuche hinzufügen; maximal8. Ein Schalter für die ganze Gruppe, je Komponente Name, letzter Prozentwert/Zeitstempel, Abfragestatus und Entfernen. Akkus abfragen erzwingt einen neuen Versuch; Komponente hinzufügen verwendet die bestehende Suche. Abfrage ist unabhängig von FIT, Aufzeichnungsquellen und Aufnahme-Autostart. Noch keine Gangdaten und keine zusätzlichen Dashboardfelder.

## Bluetooth

SRAM-Erkennung über Hersteller-ID0933, ServiceFE51/proprietäre d905-Familie oder Namen SRAM mit Leerzeichen als Erkennungshinweis. Vor Zuordnung vollständige GATT-Dienste prüfen: SRAM-Dienst oder Batteriedienst plus SRAM-Hinweis. Standardrollen FTMS/Bosch/CPS/Herz behalten Vorrang; bloßes Batterie-Service180F ist kein SRAM-Nachweis. Sensorsuche nur beim E-Bike für die neue Rolle öffnen. Bereits zugeordnete Komponenten ausschließen, bekannte andere Geräte weiter korrekt klassifizieren.

AxsBatteryGroup liest ausschließlich den Standarddienst180F/2A19 in kurzen, seriellen GATT-Sitzungen. Exakt1Byte, nur0–100 akzeptieren; FF, >100, leere/mehrbyte Antworten und Fehlerstatus sind kein Prozentwert. Kein SRAMBond, Bonding, CCCD oder proprietärer Steuerbefehl. Gerät je Versuch nach Erfolg/Fehler schließen, Timeout10s, danach nächste Komponente. Keine Dauersuche/Dauerverbindung. Während sichtbarer App und aktivierten angeforderten Verbindungen automatische Versuche höchstens alle5min je Gerät, auch bei Fehlern; manuelle Aktion darf umgehen. Hintergrund, Gruppen-Aus, manuelles Trennen und Profilwechsel schließen laufende Sitzungen; alte Callbacks werden per Generation/GATT-Identität ignoriert.

Letzte erfolgreiche Werte mit realem Abfragezeitpunkt privat im axs-battery-cache je physischer Adresse erhalten. Fehlgeschlagener Versuch überschreibt keinen Wert. Als letzter Stand kennzeichnen, keine Live-Messung behaupten. Cache ist Telemetrie, kein Einstellungsbackup. Profilzuordnungen/shiftEnabled hingegen in BikeProfileStore und Einstellungssicherung, Typprüfung/Adressprüfung/Limit/Dublettenschutz. Alte Profile ohne Felder: aktivierte leere Gruppe. Profilkopie behält wie andere Sensoren die Geräte des Zielprofils. Indoor/andere Profiltypen erhalten gespeicherte Daten, zeigen/verbinden die Gruppe aber nicht.

## Hardware und Grenzen

Nutzer nennt Apex-XPLR-Schaltwerk, zwei Apex-Controller und zwei Wireless Blips. Kein Hardwaretest durchgeführt; Sichtbarkeit ist kein Beweis eines lesbaren Akkudiensts. Komponenten zum Test aufwecken und offizielle SRAM-App schließen. Blips nicht als direkt erreichbare BLE-Komponenten oder automatisch bekannte Akkus darstellen. Falls ein Gerät den Standard-Akkudienst nicht anbietet, klare Rückmeldung; kein ausgedachter Prozentwert oder Schätzung aus Spannung. Passives Advertising als möglicher späterer Zusatz; seine Spannungs-/Prozentformate sind modellabhängig. Gangdaten separat später, da beim öffentlich getesteten GX-Eagle-Transmission verschlüsselt und proprietär gekoppelt.

## Quellen

Nur Protokollinformationen und Standard-BLE-Technik verwendet; kein fremder Programmcode übernommen, keine Abhängigkeit hinzugefügt.

- [karl-petter/sram-axs-for-ha](https://github.com/karl-petter/sram-axs-for-ha/tree/316dbfd30d46eb7664074ae72713658d40da6f46): Standard-GATT-Batterie, kurz verbinden/lesen/trennen; beobachtet an eigenen Komponenten, keine allgemeine Hardwaregarantie.
- [tarekrached/esphome-sram-axs](https://github.com/tarekrached/esphome-sram-axs/blob/7c3106c6e4a85b7f599de6f39137dc7df478d90f/docs/protocol.md): Advertising/Wake-Verhalten, insbesondere keine Advertising-Prozentwerte bei Knopfzellen.
- [GaborWnuk/sram-axs](https://github.com/GaborWnuk/sram-axs/blob/366913e6e26df5019f42ec3086f768924b118b9b/PROTOCOL.md): offener Batteriedienst vs. verschlüsselte Gangdaten; Hardwareabdeckung GX Eagle Transmission und AXS Controller.

## Prüfung

Kotlin-Kompilierung und24 unterschiedliche gezielte Tests erfolgreich:5 neue Akku-/Runtime-/UI-Fälle,13 Sensorscannerfälle über SDK33/35/36,3 Sicherungsfälle und3 bestehende Sensor-Setup-Fälle. Fehlende Dienste in der Simulation mit vorhandenem SRAM-Dienst ohne Battery-Service nachbilden (Robolectric meldet bei vollständig leerer Dienstauswahl schon discoverServices=false). Letzter sauberer Akkulauf axs-verified-tests.log, Scannerprüfung axs-scan-final-tests.log, Backup/Setup axs-tests.log unter /workspace/.tools; gescheiterte frühere Akkufälle durch Simulationsaufbau erklärt und danach vollständig grün. Typografie-/UI-Katalog-/Diffprüfung erfolgreich. Kein vollständiger Gesamtlauf, Hardwaretest oder neue APK.
