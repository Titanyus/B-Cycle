# Bikes – erste Profilverwaltung

Die bisherige Sensorenseite heißt **Bikes** und verwendet das vorhandene Bike-Icon.
Die vier Profilkacheln bleiben im normalen Scrollinhalt. Darunter stehen der
antippbare Name (direkte Eingabe, gültige Änderungen sofort speichern), Radtyp,
Fahrradkategorie als Dropdown, Radumfang, Sensorverwaltung und FIT-Auswahl.
Ein gemeinsamer Fullscreen-Header, keine zusätzliche Footer-Navigation.

## Profile und Migration

- Vier feste lokale Slots; Namen, Kategorie, Radumfang, Start-Verbindungsoption,
  Sensorzuordnungen, Kategorie-Schalter und FIT-Konfiguration pro Profil.
- Bike 1 übernimmt die bisherigen Bosch-/Pulsmesser-Zuordnungen, GPS-Einstellung
  und FIT-Feldauswahl/-Hauptquellen. Die anderen Slots übernehmen keine Zuordnung.
- Die aktive Auswahl bleibt über App-Neustarts erhalten. Wechsel bei Aufnahme oder
  Speichervorgang gesperrt. Alte GATT-Verbindungen werden beendet; veraltete
  Callback-Generationen bleiben durch die bestehenden Verbindungsclients gesperrt.
- Dashboard-Belegungen bleiben gemeinsam. Keine neuen FIT-Felddefinitionen oder
  Reskalierung bereits fertiger Bosch-Geschwindigkeits-/Distanzdaten.
- Radumfang 500–4000 mm als Profilwert, noch kein zusätzlicher Radsensor-Decoder.
  Ungültige Eingaben überschreiben nicht den zuletzt gültigen Wert.
- Der vorhandene explizite Einstellungsbackup exportiert weiter die aktive
  FIT-Auswahl. Lokale Sensorzuordnungen werden weiterhin nicht exportiert; kein
  unkontrolliertes Serialisieren der vollständigen Profil-Preferences in Backups.

`BikeProfileStore` hält die vier Profile versioniert und spiegelt nur das aktive
Profil in die bisherigen Runtime-/Backup-Schlüssel. Bestehende Restore-Aufrufe auf
aktive FIT-Schlüssel bleiben dadurch wirksam. Unveränderte Profil-JSON-Daten werden
nicht bei jedem Sensortick erneut dekodiert; Kachelbilder werden nur bei Änderungen
neu aufgebaut. Keine zweite Sensorverarbeitung oder parallele Aufzeichnung.

## Typen und Bilder

Bike, E-Bike und Indoor-Bike sind im Modell vorhanden, **nur E-Bike ist aktiv**.
Für Bike/Indoor sind passende Sensorrollen vorbereitet, einschließlich einer
inaktiven Trainerrolle. Zusätzliche Powermeter-/FTMS-Anbindung folgt separat.

Mountainbike, Rennrad, Trekkingbike, Gravelbike und Citybike sind als Kategorien
wählbar. `BikePreview` zeichnet leichte kategorienspezifische Platzhalter;
endgültige Bilder folgen nach Sichtprüfung. Keine neue Iconbibliothek.

## Sensorverwaltung

- Zentral **Sensor suchen / verbinden**, keine getrennte Gerätesuche pro Zeile.
- Kategorien GPS, E-Bike, Powermeter und Pulsmesser; Powermeter vorerst deaktiviert.
- Kategorie-Schalter, gespeicherter Name, Status, verfügbarer Akku und Disconnect.
  GPS hat keinen Gerätesensorakku. Nicht verfügbare Akkudaten erscheinen als „–“.
- Ausschalten oder Disconnect behalten Zuordnungen. „Zuordnung entfernen“ entfernt
  gezielt nur die gespeicherte Verbindung dieser Kategorie.
- Disconnect blockiert Wiederverbindung für die aktuelle App-Sitzung; die zentrale
  Verbindungsaktion oder erneutes bewusstes Einschalten hebt die Blockade auf.
- Automatik bezieht sich auf den Start des ausgewählten Profils/App-Prozesses;
  Ausschalten dieser Startoption trennt keine bereits laufende Verbindung.
- Bereits zugeordnete Geräte erscheinen nicht erneut als neue Treffer.
- Suche maximal zehn Sekunden. Bekannte/gekoppelte Geräte bleiben auffindbar.
  Anzeigen nur nach passender Service-UUID im Advertisement oder nach bestätigter
  Diensterkennung einer kurzen, seriellen GATT-Prüfverbindung (maximal drei Sekunden
  pro Kandidat, begrenzte Warteschlange). Namen werden nicht zur Typerkennung benutzt.
- In dieser Phase unterstützt die Suche Bosch MCSP und standardisierte Pulsmesser.
  Probeverbindungen abonnieren keine Messdaten und werden wieder geschlossen.
- Profilwechsel, Schließen und App-Hintergrund stoppen die Suche samt Probe.
- GPS-Aus wird beim Aufnahmestart respektiert. Neue Outdoor-Profile starten mit
  aktiviertem GPS; eine vorhandene explizite GPS-Aus-Einstellung wird migriert.

## FIT

Deutlich benannte **FIT-Hauptquelle** für Geschwindigkeit und Distanz samt kurzer
Erklärung: Hauptquelle = Standardwert für andere Programme; zusätzliche angehakte
Quellen = getrennte Developer Fields. Bestehende Auswahl einschließlich optionaler
Performancewerte bleibt pro Bike erhalten. Ausschließlich reale vorhandene Quellen
anbieten; kein fiktiver Powermeterwert. Abgelöste Formularinstanzen dürfen keine
Einstellungen in ein inzwischen anderes Profil schreiben.

## Vorschau

Echte native Android-Testdarstellung mit Beispieldaten, keine Bildmontage:

- [Profilbereich](previews/bikes-top.png)
- [Sensorbereich](previews/bikes-sensors.png)
- [FIT-Bereich](previews/bikes-fit.png)

## Verifikation

Vollständige Suite: **403 Tests bestanden**, keine Fehler oder übersprungenen
Tests. Nach abschließenden UI-Anpassungen zusätzlich 18 gezielte Prüfungen für
Migration/Profilpersistenz, Runtime-/Aufnahmesperre, BLE-Suche auf API 33/35/36,
Native-UI, Themewechsel und FIT-Auswahl; danach erneut vier UI-/Themeprüfungen.
Debug- und Release-Kotlin-Kompilierung erfolgreich.

Die BLE-Prüfungen verwenden Android-Testdoubles; physische Mehrgeräteverbindungen
sind noch am Telefon zu testen. Keine neue APK erzeugt oder veröffentlicht.

## FIT-Accordion und aktive Controls

Die FIT-Aufzeichnung steht wieder als standardmäßig geschlossenes Accordion unter den Sensoren. Es bleibt auf derselben scrollenden Seite und behält seinen Offen-Zustand bei Bikewechsel innerhalb dieser Ansicht. Werte und Sofortspeicherung pro Bike bleiben erhalten. Auswahlkreise und Schalter verwenden bei Aktivierung zentral ACCENT; Kreis-Kontur und Beschriftung bleiben FOREGROUND, deaktivierte Controls gedimmt.

## Radumfang: einzeilige Eingabe

Im Bike-Dialog stehen Beschriftung, Eingabewert und mm in einer Zeile.
Der Wert verwendet Bebas Neue und den gemeinsamen Wert `analysisMetricSp`
(28 sp), wie die Zonenwerte im Fahrerprofil. Transparenter Eingabehintergrund,
rechte Ausrichtung und 1 physischer Pixel TERTIARY als Rahmen. Eingabevalidierung
(500–4000 mm) und automatische Speicherung bleiben erhalten. Zunächst nur hier
angewandt, keine globale Umstellung anderer Eingaben.

[Native Vorschau](previews/bikes-wheel-row.png). Gezielt geprüft über
BikesViewTest und die Release-Kotlin-Kompilierung; keine neue APK gebaut.
