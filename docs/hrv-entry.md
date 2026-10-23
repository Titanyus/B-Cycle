# Dashboard-Einstieg, HRV-Einstellungen und Tagesbefinden

Stand 22.10.2026, Arbeitsstand nach APK 0.8.23 / Build 62. Keine neue APK beauftragt.

## Dashboard und tägliches Angebot

Die untere Leiste enthält drei Aktionen: Strecken links außen, Start/Stop geometrisch in der Mitte, Menü rechts. Profilname, Long-Press-Fahrradwahl, Aufnahmebedienung und Ein-/Ausblenden der Leiste bleiben erhalten. Der dauerhafte Puls-/HRV-Button entfällt.

Einstellungen → HRV-Messungen enthält **HRV beim ersten Start zeigen** als Schalter mit erklärendem Accordion. Standard aus. Bei aktivierter Option wechselt die App beim Vordergrundstart automatisch zum Dashboard und zeigt bis zur bewussten Bestätigung erneut den HRV-Einstiegsdialog: abgerundete SECONDARY-Fläche, 1-dp-TERTIARY-Rand und diffuser nativer Schatten über abgedunkeltem Dashboard. Die Herzgrafik nach der Nutzervorlage wird direkt mit BlendMode.SCREEN auf die Fläche gezeichnet; Schwarz bleibt neutral. Darunter HRV-Messung, der gewünschte Dreiminuten-Text, Herz/Messskala/Sonnenaufgang für Ruhepuls/Readiness/Tagesform, der normale zentrale Button Jetzt starten und die schlichte Textaktion Später. Grafik als WebP (ca. 40 KiB), keine Animation oder neue Sensorabfrage. Kleine Hochformatfenster können den Dialoginhalt scrollen. Tippen außerhalb und Android-Zurück schließen den Hinweis; Accessibility des Hintergrunds wird wiederhergestellt. Der frühere Kreis und der 14-px-Blur entfallen.

Jetzt starten öffnet die vorhandene HRV-Messansicht und startet ihr normales Ruhe-/Messprotokoll. Er benötigt weiterhin frische echte RR-Daten. Ohne diese bleibt die Ansicht zunächst wartend; sie startet beim Eintreffen, solange sie sichtbar im Vordergrund ist. App-Hintergrund oder überlagerte Ansichten lösen keinen Start aus; bewusster Wechsel zur Historie/Fragebogen verwirft den ausstehenden automatischen Start. Schließen der Messansicht behält das bisherige Abbruchverhalten.

`HrvDailyLaunch` speichert erst Jetzt starten, Später oder einen Tap auf den Hintergrund unter `hrv-launch-state/handledDay`. Reines Anzeigen, App-Wechsel, Activity-Neuanlage, Android-Zurück oder spätes Aktivieren verbrauchen das Tagesangebot nicht. Der alte `lastDay`-Marker wird nicht mehr ausgewertet. Nach einem Tap bleibt der Hinweis an diesem lokalen Kalendertag aus; am nächsten Tag wird er wieder angeboten. Der Marker gehört nicht in die portable Einstellungssicherung.

Vor dem Angebot werden offene Einstellungsdialoge geschlossen und eine Karten-Vollansicht verlassen, damit das Dashboard samt HRV-Dialog direkt erreichbar ist. Bereits laufende Fahrtaufnahme/Speicherung/HRV und ein ausstehender HRV-Speicherfehler werden nicht unterbrochen; das noch unbestätigte Angebot bleibt für den nächsten Start erhalten. Es gibt keine neue tägliche Messsperre; manuelles Messen/Wiederholen bleibt möglich.

## Manueller Einstieg und Abschluss-Signale

Oben in den HRV-Einstellungen steht mittig **♡ HRV-Messung**, in ACCENT mit 900 Black und 1-dp-Pillenrahmen wie der Startbutton im Messkreis. Dieser Einstieg öffnet nur die Ansicht und startet nichts automatisch.

Unter den beiden bestehenden Zeitslidern, durch einen horizontalen Trenner abgesetzt:

- Vibrieren am Ende der Messung.
- Akustisches Signal am Ende der Messung.

Beide standardmäßig aus und unabhängig schaltbar. Ein vollständig beendetes Messprotokoll signalisiert einmal, auch wenn dessen Qualität nicht für eine gültige HRV-Schätzung reicht. Abbruch, Verbindungsabbruch, Historienöffnung und Speicher-Retry signalisieren nicht. Kurze Vibration und kurzer Systemton über den Benachrichtigungskanal respektieren Hardware und Systemeinstellungen; ToneGenerator wird zeitnah freigegeben. Nur die Android-Normalberechtigung VIBRATE ergänzt, keine neue Laufzeitabfrage.

`HrvSettings` und die bestehende Profil-Einstellungssicherung enthalten alle drei neuen öffentlichen Schalter. Alte Sicherungen ohne sie bleiben gültig und ergeben false. Protokoll, HRV-Rohdaten, Berechnung, Qualitätsregeln, Ergebnisdatei und vollständige Messungsbackups unverändert.

## Tagesbefinden

Aufsteh- und Schlafenszeit stehen bei gemeinsamer Aktivierung in einer Zeile mit gleich hohen Beschriftungen. Bei nur einer aktivierten Frage volle Breite. Sportart entfällt aus Formular und Konfiguration; historische Sportangaben bleiben lesbar und gesichert. Der alte SPORT-Konfigurationswert wird beim Lesen/Restaurieren normalisiert und nicht als aktive Einstellung weiterexportiert.

Bewertungsslider verwenden weiterhin native SeekBar-Gesten, Tastatur, Accessibility und diskrete Positionen. Bei mittigem Ursprung läuft die ACCENT-Hervorhebung von der Mitte zur Position nach links oder rechts. Muskelkater und Müdigkeit beginnen stattdessen bei 0 und markieren von links. Unberührte Standardstellungen bleiben fehlende Antworten; historische Werte nicht zurücksetzen. RPE behält 0–10 und „Gar nicht“ links, ohne zusätzlichen sichtbaren „RPE x / 10“-Text.

Am Ende **Zur Auswertung** in ACCENT. Bis der spätere Auswertungsscreen existiert, führt er zur vorherigen Ansicht zurück. Erst erfolgreiche Speicherung bestätigt die Navigation; bei Fehler bleiben Antworten und Formular erhalten. Keine HRV-Werte aus Fragebogenantworten berechnen oder verändern.

## Prüfung

22 unterschiedliche gezielte Android-Hosttests: HrvEntryTest (3), HrvSettingsTest (2), HrvQuestionnaireTest (6), HrvControllerTest (3), HrvViewTest (3), SelectionSpacingTest (4) sowie die bestehende Theme-/Navigationsprüfung aus SettingsDesignMigrationTest (1). Nach konkreten Änderungen nur betroffene Fälle erneut geprüft. Kein vollständiger Gesamtlauf. Die begrenzte manuelle CI-Auswahl nimmt Einstieg, Einstellungen und Controller für künftige Läufe mit auf; für diesen UI-Auftrag kein zusätzlicher GitHub-Lauf oder APK-Build.

Geprüft: opt-in/dauerhafter Tagesmarker, App-Wechsel, Außen-Tap, frische RR als Voraussetzung, automatischer Vordergrundstart, laufendes Ruheprotokoll/Abbruch, Abschluss-Signal einmal statt bei Abbruch, Profil-Einstellungssicherung, aktive SPORT-Bereinigung bei erhaltenem Altformat, seitliche Zeitfelder, neutrale/Null-Ursprünge, tatsächliche Slider-Zeichenfarben, Speichernfehler vor Navigation und Rückkehr zur vorherigen Ansicht. Auch bestehende unveränderte HRV-Ergebnis-/Fragebogen-Backup-Prüfungen bestanden. Native Darstellungen von Herzfläche, Einstellungen und Formular gerendert und visuell geprüft.

Kein neuer Test mit physischem Pulsmesser, Lautsprecher oder Vibrationsmotor. Der frühere 14-px-Blur entfällt im neuen Dialogentwurf. Der diffuse native Elevation-Schatten benötigt weiterhin eine Sichtprüfung auf dem Gerät; der Software-Canvas-Screenshot bildet Hardware-Schatten nicht vollständig ab.

Nachfolgende Erweiterung: „Zur Auswertung“ öffnet nach erfolgreichem Speichern des Tagesbefindens den neuen HRV-Verlauf, statt nur zurückzuspringen. Auch über HRV-Einstellungen → HRV-Auswertung erreichbar. Details [HRV-Auswertung](hrv-trends.md). Diese Ergänzung ist noch nicht in APK 0.8.25 enthalten.

Veröffentlichung: Diese Ergänzung/Korrektur ist in APK **0.8.26 / Build 65** enthalten. [Buildnachweis](build-65.md). Frühere Aussagen zu fehlender neuer APK beschreiben den Stand vor dieser Veröffentlichung.

23.10.2026, weitere Messbildschirm-Korrektur: „Starten“ statt „Messung starten“. Alle Inhalte innerhalb des Kreises (Phasentext, Puls/Einheit, Zeit, Aktionsbutton) gegenüber Build 69 um 10 dp nach unten versetzt. Kreisgeometrie bleibt unverändert.

Stand 23.10.2026: die beschriebenen neuen Funktionen sind in APK **0.8.31 / Build 70** enthalten. [Buildnachweis](build-70.md). Frühere Angaben zu fehlender APK sind Verlauf.

## Dialogentwurf nach Build 72

Uservorlage am 23.10.2026: Neon-Herz auf schwarzem Hintergrund über Screen,
SECONDARY/TERTIARY und Standardbutton statt rotem Sonderbutton. Lokale
Gestaltungstokens in BicycleDesign, Bild über image_gen nach bereitgestellter
Vorlage aufbereitet, als WebP eingebunden. Kein Algorithmus-, Messzeit-,
Tagesmarker- oder Sicherungsformatwechsel. Der feste Dreiminuten-Text ist
die ausdrücklich gewünschte Beschriftung und verändert die konfigurierten
Ruhe-/Messzeiten nicht. Neue Readiness-/Tagesform-Symbole sind Beschriftungen
des Einstiegs, keine neu berechneten Kennwerte.

Drei gezielte Tests für den neuen Dialog erfolgreich: ursprünglicher HRV-Start mit
frischen RR-/Vordergrundgrenzen, wiederkehrendes Tagesangebot/Lifecycle sowie
Später ohne Start, Inhaltstap ohne Bestätigung, kleines Hochformat mit erreichbaren
Aktionen und tatsächliche Screen-Pixelverrechnung (Schwarz neutral, Neon sichtbar).
Native 360×800-Vorschau visuell geprüft: [Dialog](previews/hrv-daily-prompt.png).
Typografie-/UI-Katalogprüfung erfolgreich; keine neue APK.
