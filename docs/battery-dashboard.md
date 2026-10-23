# Akku-Kombifelder, Smartphone und Dashboard 10

## Datenfeldauswahl

Unter Daten gibt es zusätzlich **Akku** und **Smartphone**, mit zentralem Batterie-
bzw. Smartphone-Icon. Akku-Füllstände werden direkt unter Akku ausgewählt; einzelne
Prozentfelder werden im Dashboard-Picker vorerst nicht mehr angeboten.

- E-Bike-Füllstand aus dem bestehenden aktuellen Bosch-Wert.
- Pulsmesser-Akku nur bei aktiviertem, live verbundenem externen Pulsmesser.
- Indoor-Trainer-Akku aus dem optionalen FTMS-Battery-Service, nur bei Live-Empfang.
- Smartphone-Akku aus Androids sticky ACTION_BATTERY_CHANGED, ohne Polling oder
  zusätzliche Berechtigung. Nur während aktiver Runtime (sichtbar oder Aufnahme)
  abonnieren; dieselbe bestehende Runtime-Anzeigeaktualisierung verwenden.
- Ein separater Powermeter ist weiterhin nicht angebunden. Sein stabiler Quellenschlüssel
  ist für spätere Unterstützung vorbereitet, wird aktuell nicht neu angeboten.

Smartphone bietet **Uhrzeit (HH:mm, lokale Gerätezeit)**. Der Smartphone-Akku steht
in der direkten Akku-Mehrfachauswahl. Die Uhrzeit
ist eine Live-Anzeige, kein Fahrtzähler, und läuft unabhängig von START/STOPP.

**Akkuanzeige:** direkt ein bis vier Akkus auswählen. Kein zusätzlicher Schalter
für eine Kombianzeige und keine separaten Prozent-Radiofelder. Auswahländerungen
werden sofort übernommen, der Dialog bleibt offen. Auch der letzte Akku bleibt
abwählbar; ohne Auswahl keine Daten. Alte Prozent-Feld-IDs bleiben für bestehende
Dashboards und Backups lesbar, werden aber nicht mehr zur Auswahl angeboten.

Angeboten werden nur aktuell verfügbare Akkustände: E-Bike oder Indoor-Trainer passend
zum aktiven Bikeprofil, verbundener Pulsmesser mit Akkudaten und Smartphone.
Keine Traineroption im E-Bike-Profil. Gespeicherte Quellen anderer/nicht verbundener
Geräte werden nicht als neue Auswahl angeboten; beim Bearbeiten zählt nur die
verfügbare Auswahl. Bereits konfigurierte Anzeigefelder behalten fehlende Quellen
als Strich, bis der Nutzer die Auswahl ändert.

Bosch meldet den Akkustand nur bei Änderung. Ein gültiger zuletzt empfangener Wert
bleibt während der laufenden LIVE-Verbindung sichtbar, ohne 60-Sekunden-Verfall.
Trennung, Bikewechsel und neuer Verbindungsaufbau verwerfen den Verbindungspuffer;
ohne neue Batteriemeldung wird kein alter Stand übernommen. Echte 0 % bleibt gültig.
Die übrigen Sensor-Frischeregeln und FIT-Aufzeichnung bleiben unverändert.

Persistenz über stabile Feld-IDs `BATTERIES:EBIKE,HEART,PHONE` usw.; keine zusätzlichen
per-Layout-Prefs. Neue Auswahlen in fester Quellenreihenfolge E-Bike, Puls, Trainer,
Smartphone, später Powermeter. Maximal vier verschiedene bekannte Quellen, ungültige
IDs nicht akzeptieren. Alte Feld-IDs unverändert. Dashboardseiten, Presets, Kartenfelder
und bestehendes Dashboard-Backup verwenden denselben Codec und behalten die Auswahl.

## Darstellung

Einträge als **Icon · vertikaler Balken · Prozent**, ohne zusätzliche Überschrift. Ab zwei Einträgen
zwei Spalten, ab drei zwei Reihen. Gemeinsamer Canvas-Renderer, feste Sensoridentität
über SensorIcons/BicycleIcons, bestehende schmale Zahlenschrift. Bis vier Einträge
passen auch in schmale Dreierkacheln. Der Balken füllt sich von unten nach oben.
Durch die schmaleren Balken größere Darstellung; die verfügbare Breite/Höhe begrenzt
die Skalierung. Nach der Vergrößerung für 0.7.29 wird die kombinierte Darstellung
nochmals um 10 % reduziert, einheitlich für Dashboard und Karte. Quellen ohne gültigen, aktuell verfügbaren Wert
bleiben mit **„—“** im Raster; echte 0 % bleiben sichtbar. Kein Zusammenschieben der
Slots, kein Ersetzen fehlender Werte durch 0 oder historische Daten.

Dashboard: helle Icons/Zahlen/Balken. Karte: dunkle Icons/Zahlen/Balken. Warnfarben
betreffen Füllung und Prozent: **unter 20 % Gelb/Orange, unter 10 % Rot**. Genau 20 %
bleibt neutral, genau 10 % orange. Auf der Karte dunklere Warnfarbtöne für Kontrast.
Keine Änderung an den bisherigen Bosch-Modus-/Akkuwarn-Glow-Schwellen.

Normale Kartenfelder bleiben 62 dp hoch. Bei einem Kombifeld mit drei/vier Quellen
wird ausschließlich die untere Karten-Datenzeile **76 dp** hoch. Aufnahme-/Lichtzeichen
bleiben im mittleren Feld erhalten, über dem Raster. Zoom-/Statusabstände folgen der
bereits zentral verwendeten tatsächlichen Zeilenhöhe. Touchgesten laufen wie bisher
zur Karte durch; Bearbeitung nur im Dashboard-Editor.

Keine neuen FIT-Felder, keine Änderungen an Originalwerten, Sensor-Decodern, Mittelwerten,
Performance oder Aufzeichnung. Batterie-Snapshots sind reine Anzeige-Metadaten und zählen
nicht als zusätzliche BLE-Messwertarten. Rendering/Icons werden nur bei Verwendung
initialisiert, unveränderte Werte lösen keine wiederholte Text-Neuvermessung aus.

## Dashboard 10

`FULL_DATA` ist die zehnte Vorlage und wird an die bestehenden neun angehängt:
`1 / 1 / 1 / 2`, insgesamt fünf Felder. Drei große Einzelfelder verwenden den vorhandenen
ONE-Kachelstil, die untere Doppelreihe TWO. Gesamte nutzbare Dashboardhöhe, keine Karte;
untere Start-/Strecken-/Einstellungsleiste bleibt sichtbar. Karten-Rendering/Download-
Lebenszyklus pausiert für diese Seite; GPS und FIT sind davon unabhängig.

Vorschau im bestehenden Layout-Picker, pro Seite auswählbar und im Backup enthalten.
Editor-Beenden-Button bei dieser Vorlage als eigener Button über der unteren Leiste;
bei Kartenlayouts bleibt er auf der Karte. Graphen/Zonen-Visualisierung weiterhin unabhängig,
die drei großen Felder unterstützen die bestehenden Graph-Overlays.

Benachrichtigungen an Position Unten werden ohne Karte auf maximal 320 dp begrenzt;
Mitte/Oben bleiben unverändert. Die Position Unten bleibt am unteren nutzbaren Rand.

## Wetterauswahl

Die vorhandenen vier Felder Sonnenaufgang, Sonnenuntergang, Zeit bis Sonnenuntergang
und verbleibendes Tageslicht waren durch den Indoor-Quellenfilter verborgen. Sie bleiben
jetzt auch dort auswählbar. Ohne gültige GPS-Position keine Berechnung und keine Werte;
Indoor startet dafür weiterhin kein GPS. Bestehende Offline-Solarberechnung unverändert.

## Tests und Vorschauen

BatteryFieldsTest: Quellen-/Anzahl-/ID-Prüfung, pro-Slot-Persistenz, Grenzwerte 0/9/10/19/20,
Android-Broadcast und Abmeldung, Checkboxen/Limit, Dashboard-/Kartenrendering, fehlende
Werte und Aufnahme-/Lichtanzeige. FullDataDashboardTest: echte Fünffeld-Geometrie, Editor,
Karte GONE und Rückkehr zu Kartenlayout. SettingsBackupTest: Feld und Vorlage gemeinsam
sichern/wiederherstellen. Bestehende Tests für Kartenlayouts bleiben unverändert fachlich
geprüft; ihre Karten-Geometrieschleifen gelten für Vorlagen mit Karte. Neue Vollbild-
Geometrie wird separat geprüft. Kategorien-/Quellenlisten um die gewünschten Einträge
und die wieder sichtbaren Wetterfelder ergänzt.

Native Vorschauen mit Testdaten:
[Dashboard](previews/battery-dashboard.png), [Kartenfelder](previews/battery-map.png).
Praktische Lesbarkeit und Android-/Sensor-Akkumeldungen auf dem Nutzergerät noch prüfen.

Nach Build 72: Letzten gültigen Bosch-Akkustand je portabler Fahrradprofil-UUID in privatem bike-battery speichern. Bei Verbindungsende, Fahrtende oder App-Neustart anzeigen, bis ein neuer gültiger Wert kommt. Echte 0 bleibt gültig; null/ungültige Werte löschen die letzte Anzeige nicht. Kein Austausch zwischen Profilen/Indoor und kein Bezug zu wiederverwendeten Slots. Reine Anzeige, nie als Messwert in Journal/FIT/Performance übernehmen. Kein Einstellfeld und deshalb nicht in der Einstellungssicherung. Noch keine APK.

Prüfstand nach Build 72: 32 gezielte Tests in den Umsetzungsschritten erfolgreich (Aufnahmeregression auf Android 13/15/16, Detailsnavigation, Skalenrückkehr/Fenstergrenzen, Graphgeometrie, Akku-Persistenz/Rohdatentrennung und Zehnsekundengrenze); vier betroffene Tests nach letzter Absicherung nochmals erfolgreich. Native Summary geprüft. Typografie-/Atlasprüfung erfolgreich, kein Hardware-/Diensttest oder neuer APK-Build.

## Aufzeichnungsfrische bei unverändertem Bosch-Akku

Bosch meldet Akkustandsänderungen; längeres Fahren ohne Unterstützung kann denselben Wert über viele Minuten behalten. Deshalb auch im Aufnahme-Sample kein 120-s-Ablauf für BATTERY mehr: innerhalb der laufenden Live-Verbindung gültigen tatsächlich empfangenen Stand (0–100) weiterführen. Bei Trennung Rohpuffer löschen, gespeicherten Anzeige-Fallback niemals aufzeichnen; neue Verbindung braucht ihren eigenen ersten Wert. Andere Messwerte/Frischegrenzen unverändert. Keine höhere Ersatzfrist, sondern Gültigkeit über die aktuelle Verbindung.

Die private Analyse-FIT erhält alle vorhandenen Journalfelder unabhängig von der normalen FIT-Auswahl. Streckendetails lesen diese bevorzugt. Ein gefilterter Export ohne Akkustände beweist deshalb nicht, dass intern ebenfalls keine vorhanden sind. Verbrauch wird aus Start-/Endstand mit bisheriger Abdeckungs-/Nachladeprüfung berechnet; fehlende alte Daten nicht aus dem heutigen gemerkten Akkustand rekonstruieren. Keine neue APK.

Konkreter Nutzerexport am 23.10.2026 geprüft: 7.668 Record-Messpunkte, kein gültiger Standard-/Developer-Akkustand. Ohne private Analysequelle kann für diese Fahrt kein tatsächlicher Verbrauch bestimmt werden. Private FIT-/Messwerte nicht ins Repository übernehmen.

Prüfstand Akku-Aufnahme: drei gezielte Tests erfolgreich. Fünf Minuten unveränderte Live-Ladung bleibt im Sample, Offline-/Reconnect-Fallback bleibt draußen, ungültige Werte/echte Null korrekt; private Analyse zeigt Verbrauch trotz deaktiviertem Akku-Export. Bestehende Abdeckungs-/Nachladeprüfung erhalten. Keine APK.

Stand 23.10.2026: die beschriebenen neuen Änderungen sind in APK **0.8.34 / Build 73** enthalten. [Buildnachweis](build-73.md). Frühere Angaben zu fehlender APK sind Verlauf.
