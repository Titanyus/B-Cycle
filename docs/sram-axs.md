# SRAM AXS: Akku-Anbindung als erster Schritt

Stand26.10.2026 nach Build89. E-Bike → Sensoren → Schaltgruppe als reguläres Schalter-Accordion. Mehrere Komponenten einzeln über die vorhandene Sensorsuche hinzufügen; maximal8. Ein Schalter für die ganze Gruppe, je Komponente Name, letzter Prozentwert/Zeitstempel, Abfragestatus und Entfernen. Akkus abfragen erzwingt einen neuen Versuch; Komponente hinzufügen verwendet die bestehende Suche. Abfrage ist unabhängig von FIT, Aufzeichnungsquellen und Aufnahme-Autostart. Noch keine Gangdaten. Dashboard-Erweiterung siehe unten.

## Bluetooth

SRAM-Erkennung über Hersteller-ID0933, ServiceFE51/proprietäre d905-Familie oder Namen SRAM mit Leerzeichen als Erkennungshinweis. Vor Zuordnung vollständige GATT-Dienste prüfen: SRAM-Dienst oder Batteriedienst plus SRAM-Hinweis. Nach Gerätefeedback korrigiert: Die Prüfung ist nur ein optionaler Fähigkeitstest, ein erkannter SRAM-Kandidat bleibt bei fehlgeschlagener Prüfung auswählbar. Noch kein Nachweis von Akku-/Gangfähigkeit allein durch die Auswahl. Standardrollen FTMS/Bosch/CPS/Herz behalten Vorrang; bloßes Batterie-Service180F ist kein SRAM-Nachweis. Sensorsuche nur beim E-Bike für die neue Rolle öffnen. Bereits zugeordnete Komponenten ausschließen, bekannte andere Geräte weiter korrekt klassifizieren.

AxsBatteryGroup liest ausschließlich den Standarddienst180F/2A19 in kurzen, seriellen GATT-Sitzungen. Exakt1Byte, nur0–100 akzeptieren; FF, >100, leere/mehrbyte Antworten und Fehlerstatus sind kein Prozentwert. Kein SRAMBond, Bonding, CCCD oder proprietärer Steuerbefehl. Gerät je Versuch nach Erfolg/Fehler schließen, Timeout10s, danach nächste Komponente. Keine Dauersuche/Dauerverbindung. Während sichtbarer App und aktivierten angeforderten Verbindungen automatische Versuche höchstens alle5min je Gerät, auch bei Fehlern; manuelle Aktion darf umgehen. Hintergrund, Gruppen-Aus, manuelles Trennen und Profilwechsel schließen laufende Sitzungen; alte Callbacks werden per Generation/GATT-Identität ignoriert.

Letzte erfolgreiche Werte mit realem Abfragezeitpunkt privat im axs-battery-cache je physischer Adresse erhalten. Fehlgeschlagener Versuch überschreibt keinen Wert. Als letzter Stand kennzeichnen, keine Live-Messung behaupten. Cache ist Telemetrie, kein Einstellungsbackup. Profilzuordnungen/shiftEnabled hingegen in BikeProfileStore und Einstellungssicherung, Typprüfung/Adressprüfung/Limit/Dublettenschutz. Alte Profile ohne Felder: aktivierte leere Gruppe. Profilkopie behält wie andere Sensoren die Geräte des Zielprofils. Indoor/andere Profiltypen erhalten gespeicherte Daten, zeigen/verbinden die Gruppe aber nicht.

## Hardware und Grenzen

Nutzer nennt Apex-XPLR-Schaltwerk, zwei Apex-Controller und zwei Wireless Blips. Nach Build91 hat der Nutzer erfolgreiche Erkennung und Akkuabfrage am Apex-Schaltwerk bestätigt (36%). Weitere Komponenten sind damit nicht bestätigt. Sichtbarkeit allein ist kein Beweis eines lesbaren Akkudiensts. Komponenten zum Test aufwecken und offizielle SRAM-App schließen. Blips nicht als direkt erreichbare BLE-Komponenten oder automatisch bekannte Akkus darstellen. Falls ein Gerät den Standard-Akkudienst nicht anbietet, klare Rückmeldung; kein ausgedachter Prozentwert oder Schätzung aus Spannung. Passives Advertising als möglicher späterer Zusatz; seine Spannungs-/Prozentformate sind modellabhängig. Gangdaten separat später, da beim öffentlich getesteten GX-Eagle-Transmission verschlüsselt und proprietär gekoppelt.

## Quellen

Nur Protokollinformationen und Standard-BLE-Technik verwendet; kein fremder Programmcode übernommen, keine Abhängigkeit hinzugefügt.

Für spätere Erweiterungen dauerhaft vorgemerkte Projektseiten und aktuelle Protokolldokumentation (Nutzerreferenzen):

- [GaborWnuk/sram-axs – Projekt](https://github.com/GaborWnuk/sram-axs)
- [tarekrached/esphome-sram-axs – Projekt](https://github.com/tarekrached/esphome-sram-axs)
- [karl-petter/sram-axs-for-ha – Projekt](https://github.com/karl-petter/sram-axs-for-ha)
- [Technische Protokolldokumentation: GaborWnuk/sram-axs, PROTOCOL.md (aktueller main-Stand)](https://github.com/GaborWnuk/sram-axs/blob/main/PROTOCOL.md)

Die Protokolldokumentation stammt aus dem Community-Projekt, nicht aus einer offiziellen SRAM-Veröffentlichung. Die folgenden festen Versionslinks dokumentieren den für die bisherige Implementierung geprüften Stand; für spätere Arbeiten zusätzlich die aktuellen Projektstände prüfen.

- [karl-petter/sram-axs-for-ha](https://github.com/karl-petter/sram-axs-for-ha/tree/316dbfd30d46eb7664074ae72713658d40da6f46): Standard-GATT-Batterie, kurz verbinden/lesen/trennen; beobachtet an eigenen Komponenten, keine allgemeine Hardwaregarantie.
- [tarekrached/esphome-sram-axs](https://github.com/tarekrached/esphome-sram-axs/blob/7c3106c6e4a85b7f599de6f39137dc7df478d90f/docs/protocol.md): Advertising/Wake-Verhalten, insbesondere keine Advertising-Prozentwerte bei Knopfzellen.
- [GaborWnuk/sram-axs](https://github.com/GaborWnuk/sram-axs/blob/366913e6e26df5019f42ec3086f768924b118b9b/PROTOCOL.md): offener Batteriedienst vs. verschlüsselte Gangdaten; Hardwareabdeckung GX Eagle Transmission und AXS Controller.

## Prüfung

Kotlin-Kompilierung und24 unterschiedliche gezielte Tests erfolgreich:5 neue Akku-/Runtime-/UI-Fälle,13 Sensorscannerfälle über SDK33/35/36,3 Sicherungsfälle und3 bestehende Sensor-Setup-Fälle. Fehlende Dienste in der Simulation mit vorhandenem SRAM-Dienst ohne Battery-Service nachbilden (Robolectric meldet bei vollständig leerer Dienstauswahl schon discoverServices=false). Letzter sauberer Akkulauf axs-verified-tests.log, Scannerprüfung axs-scan-final-tests.log, Backup/Setup axs-tests.log unter /workspace/.tools; gescheiterte frühere Akkufälle durch Simulationsaufbau erklärt und danach vollständig grün. Typografie-/UI-Katalog-/Diffprüfung erfolgreich. Kein vollständiger Gesamtlauf, Hardwaretest oder neue APK.

## Suchkorrektur nach Nutzerfeedback26.10.2026

Android-Systemsuche sieht SRAM + Seriennummer, System-Kopplung abgelehnt, Appsuche leer. Keine Systemkopplung notwendig: native BLE-GATT-Akkuabfrage separat. Scanner ignorierte spätere Scanantworten nach dem ersten (ggf. namenlosen) Paket und konnte SRAM in der begrenzten generischen Probe-Warteschlange verlieren. Nun nachträgliche SRAM-Erkennung, priorisierte Warteschlange und abschließende Kandidatenanzeige trotz Probe-Timeout/Fehler. Expliziter Suchabbruch meldet nichts mehr, bekannte CPS-Geräte werden nicht als ungeprüfte Schaltgruppe ausgegeben, erfolgreiche FTMS/CPS-Erkennung behält Vorrang. Keine neue Pairing-/Bonding-Anforderung und keine Änderung am Akku-Decoder; ob konkrete Apex-Hardware den Standarddienst lesbar anbietet, bleibt am Gerät zu testen.

## Dashboard und Startwarnung nach Build91

Vorerst wird gemäß Nutzerentscheidung das zuerst gespeicherte SRAM-Gerät als **Schaltwerk** verwendet. Keine Aggregation oder Vermutung über Controller/Blips, keine neue Rollenauswahl. Bestehende weitere Gerätezuordnungen/Abfragen bleiben erhalten, ihre Akkus werden hier nicht vermischt. Bei Entfernen des ersten Geräts ist das erste verbleibende Gerät maßgeblich.

- Dashboardquelle **Schaltgruppe**, Unterpunkt **Schaltwerk**, stabile Feld-ID `SHIFT_BATTERY`.
- Unter **Akku** zusätzlich **Schaltwerk** als `BatterySource.SHIFT` in der vorhandenen Ein-bis-vier-Mehrfachauswahl. Quellen-Enum nur am Ende erweitert; alte Feld-IDs/Layouts bleiben lesbar. Map-Felder verwenden dieselbe Anzeige. Gespeicherte Auswahl bleibt erhalten, fehlender Akku als Strich.
- Anzeige nutzt ausschließlich den letzten erfolgreichen Standard-BLE-Wert des ersten Geräts im aktiven E-Bike-Profil, wenn die Schaltgruppe aktiviert ist. Kurzverbindungen brauchen keine dauerhafte Live-Verbindung; Gruppen-Aus/anderer Profiltyp/fehlende Zuordnung liefert keinen Wert. Cache bleibt Telemetrie und wird nicht in FIT oder Einstellungssicherung exportiert.
- Pro Radprofil Schalter **Akkuwarnung bei unter 20% Kapazität**, anfangs an; optionales `shiftBatteryWarning` im vorhandenen Profil-JSON, bei Altdaten Standard true. In Profilkopie, Validierung und Einstellungssicherung enthalten.
- Gemeinsamer Aufnahmebeginn für manuell/Autostart merkt genau eine Warnung für einen gültigen letzten Stand von0–19% vor.20% und unbekannt warnen nicht. Falls der Dashboardinhalt gerade verdeckt ist, einmal bis zum sichtbaren Dashboard während derselben Aufnahme zurückstellen. Nach Aufnahmeende/Schalter-Aus verwerfen. Kein nachträglicher Alarm allein durch regelmäßige Messwerte.
- Hinweis mittig für10s, antippbar zum Schließen, im selben PRIMARY-Alertstil wie die Trinkerinnerung inklusive Schatten in Flat/Glow. Trinkkonfiguration beeinflusst ihn nicht; gleichzeitig fälligen Trinkhinweis währenddessen zurückstellen, dessen Intervall nicht verändern. Kein Ton/Vibrieren/Blockieren des Aufnahmestarts.
- Kompakte Sensoranzeige ohne langen Einführungstext: Gerätename, Prozent/Zeitpunkt, Abfragestatus und tatsächlich empfangene Messwertarten mit vorhandenem Chevron-Disclosure. Nur ein erfolgreich in dieser Gruppenkonfiguration gelesener Akkustand zählt als eine Art, keine Cache-Abfrage als neue Messung.

Schaltwerk-Symbol aus dem Nutzer-ZIP `Schaltwerk.zip`, unveränderte Pfadgeometrie als lokaler tintbarer Android-Vektor mit proportionaler Darstellung. Neues Launcher-Motiv aus `BClogo2-w.zip` unverändert übernommen; adaptive Maskengeometrie erhalten, heller Hintergrund passend zum Motiv. Kein neuer Dienst, Protokollbefehl oder externe Abhängigkeit.

Prüfung dieser Erweiterung:25 unterschiedliche gezielte Tests erfolgreich (5 neue Dashboard-/Warnungs-/Picker-/Runtimefälle,5 AXS,6 Akku,4 Sicherung,1 bestehender Dashboard-Trinkfall,4 bestehende Trink-UI-Fälle). Abschließend13 betroffene Fälle nach mehrzeiligem Warnlayout und strikter Altbackup-/Booleanprüfung nochmals grün. Logs shift-dashboard-tests.log und shift-dashboard-final-tests.log unter /workspace/.tools. Typografie, UI-Katalog, XML-Ressourcen und Diff erfolgreich geprüft. Keine Gesamtsuite, neue APK oder erneuter Hardwaretest.
