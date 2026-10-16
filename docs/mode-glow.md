# Fahrmodus-Glow

## Umsetzung und Dateien

- `BoschModeGlow.kt`: zentrale funktionale Modusfarben, gecachter Drawable und Einstellungszugriff.
- `DashboardView.kt`: gemeinsamer Datenbereich zeichnet den Glow als Hintergrund; die normalen Kachelhintergründe sind transparent, damit das Licht hinter den Werten sichtbar ist. Der unveränderte gedrückte Zustand bleibt bestehen.
- `BicycleDesign.kt`: anpassbare Parameter `modeGlowWidth = 24dp`, `modeGlowAlpha = 140/255`.
- `RideRuntime.kt`: ausschließlich lesender Zugriff auf den Modus mit den bereits bestehenden Verbindungs-/Aktualitätsregeln.
- `MainActivity.kt`: Glow aktualisieren und Schalter in „Design & UI“ speichern; vorgemerkte Dashboard-Wechsel-Infobox entfernt.
- `BicycleDialog.kt`: zentrale Theme-Tints für die native Switch-Komponente.
- `BoschModeGlowTest.kt`: fünf gezielte Tests mit mehreren Szenarien.
- `AGENTS.md`: funktionale Bosch-Modusfarben ausdrücklich unabhängig vom App-Theme.

## Tatsächliche Bosch-Zuordnung

Die bestehende MCSP-Implementierung dekodiert `ASSIST_MODE` an Adresse `0x1809` (Push-Kennung `98-09`). Dieser Wert ist ein Index in die vom Rad gelieferten konfigurierten Namen (`0x180D`), kein fest vorgegebener ECO/TURBO-Enum. Der vorhandene Decoder und seine Referenz bestätigen 0–4, mit OFF an Index 0; der Diagnosebericht 0.2.0 enthält reale empfangene Indizes 1 und 2. Es wurden weder BLE-Abfragen noch die Moduserkennung geändert.

Referenz: [Decoder Card, derselbe bereits verwendete Stand](https://github.com/radiohound/bosch-smart-system-ble/blob/97def2bbbfae0d373e67d9cd3911c1dae35f4323/docs/DECODER-CARD.md).

| Unterstützungs-Slot | Funktionale Farbe |
| --- | --- |
| 1 | Grün #80FF28 |
| 2 | Blau #009DFF |
| 3 | Pink #FF36BA |
| 4 | Rot #FF3030 |

Die Zuordnung folgt den konfigurierten Slots, unabhängig davon, wie der Nutzer die Modi in Flow benennt. 0, fehlende Werte und nicht unterstützte Indizes ergeben keinen Glow. Sind Namen vorhanden, wird ein fehlender, leerer oder als OFF bezeichneter Slot ebenfalls ausgeblendet. Ohne Namensliste wird der bereits dokumentierte 0–4-Index verwendet. Die Farben werden nicht aus Theme-Rollen abgeleitet.

## Rendering und Layout

Zwei spiegelgleiche elliptische Radialgradienten bilden einen weichen Lichtschein. Das Maximum liegt am äußeren Rand auf halber Containerhöhe; Intensität und Breite fallen nach innen, oben und unten aus. Es wird keine leuchtende Konturlinie gezeichnet. Der Drawable liegt über dem schwarzen Hintergrund und hinter vorhandenen Datenwerten und Trennlinien.

Die vertikale Skalierung verwendet ausschließlich die tatsächlichen Drawable-/Dashboard-Abmessungen. `onSizeChanged` übernimmt die neuen Grenzen; die Glow-Breite beträgt nun 28,8dp (20 % breiter). Kein Bezug zur Bildschirmhöhe oder Feldanzahl. Der Drawable zeichnet ausschließlich innerhalb seiner Grenzen und clippt zusätzlich. Er erzeugt keine Overlay-Views, nimmt keine Touch-Ereignisse entgegen und kann weder Karte noch untere Aktionsleiste überdecken.

Es gibt keine Blur-Berechnung, Bitmap-Erzeugung im Produktivcode oder Daueranimation. Farbwechsel und AUS werden 1,5 Sekunden überblendet. Drei additive Lichtbeiträge erhöhen die bisherige Helligkeit dreifach, begrenzt durch die maximale Displayhelligkeit. Shader entstehen nur bei Größen- oder Farbänderungen. Wiederholte Sensorupdates mit demselben gültigen Modus verwenden dieselben Shader-Objekte. Deaktivierung blendet den bisherigen Shader weich aus; anschließend bleibt das Dashboard schwarz. Die begrenzte Animation nutzt vorhandene Shader und stoppt danach.

## Einstellungen, Aktualität und Wiederherstellung

Der bestehende scrollbare „Design & UI“-Dialog enthält den Abschnitt Fahrmodus-Glow, den vorgegebenen Erklärungssatz und einen nativen, zentral gestalteten Switch. Die vier Theme-Rollen färben dessen Beschriftung und Bedienelement, nicht den Glow.

Die vorhandenen `bike`-SharedPreferences speichern `modeGlowEnabled`; fehlender Schlüssel bedeutet AUS. „Speichern“ übernimmt den Schalter zusammen mit den bisherigen Dashboard-Einstellungen, „Abbrechen“ verwirft den Entwurf. Eine neu erstellte Activity liest denselben gespeicherten Zustand.

Der aktuelle Modus wird nur bei bestehendem LIVE-Verbindungszustand und frischer Modusmessung verwendet. Hier gilt unverändert die vorhandene 60-Sekunden-Aktualitätsgrenze für ASSIST_MODE. Bei Trennung/fehlender Messung erlischt der Glow; bei Wiederverbindung erscheint er erst mit einer neuen gültigen Messung. Der normale UI-Updatepfad übernimmt auch spätere Abläufe der Aktualitätsgrenze.

## Prüfungen

Die fünf neuen Tests prüfen:

- Alle vier Slotfarben, OFF, unbekannte Indizes und abweichende/fehlende Einträge einer vorhandenen Namensliste.
- Pixelbasierte Symmetrie, weichen Intensitätsabfall in beide Richtungen, schwarzen Innenbereich und unveränderte Pixel außerhalb des Containers.
- Identische Shader bei vielen identischen Modusupdates, Neuaufbau nach Größenwechsel und Entfernen bei Deaktivierung.
- Alle Dashboard-Presets bei unterschiedlichen Höhen; exakte Containergrenzen, erhaltene Geometrie und weiter funktionierende Feldklicks.
- Moduswechsel, Verbindungsverlust, Wiederverbindung mit neuer Messung und veraltete Messung.
- Standard AUS, Speichern, Activity-Neuerstellung, Wiederherstellung, Abbrechen und Theme-Unabhängigkeit.

Die Verbindungsszenarien simulieren vorhandene Verbindungsszustände und Sensorcallbacks; es wurde kein echtes Bosch-Rad für diese Tests verwendet. Die native Robolectric-Grafikprüfung ersetzt keine Sichtprüfung auf dem Smartphone/GPU.

Vollständige Testsuite und Release-Build: **153 Tests bestanden, keine Fehler oder übersprungenen Tests; BUILD SUCCESSFUL in 1 Minute 1 Sekunde.** Die bestehende `onBackPressed()`-Deprecation bleibt; keine neuen Kotlin-Warnungen durch den Glow. Bestehende Tests bleiben erhalten. Nach Abschluss keine Veröffentlichung oder zusätzliche Funktionen ohne Freigabe.

## Akkuwarnung mit Breathing

Bei gültigem, aktuellem E-Bike-Akkustand ≤20 % wird Gelb/Orange (`#FFB000`),
bei ≤10 % Rot (`#FF3030`) eingeblendet. Cosinus-Überblendung: zwei Sekunden
zur Warnfarbe, zwei Sekunden zurück zur aktuellen Modusfarbe. Bei ausgeschaltetem
Modus-Glow oder Modus OFF/fehlendem Modus pulsiert die Warnung aus Schwarz zur Farbe
und zurück. Der Warnungs-Höhepunkt ist 50 % breiter; die ursprüngliche Breite kehrt
am Zyklusende zurück. Die Farben sind funktionale Statusfarben, unabhängig vom Theme.

Keine neue Sensorabfrage oder dauerhafte Hintergrundanimation: Der vorhandene
Akkustand muss LIVE, gültig (0–100) und höchstens 60 Sekunden alt sein. Andernfalls
oder oberhalb von 20 % endet die Warnung. Unveränderte Warnstufen und UI-Updates
setzen den Zyklus nicht zurück. Shader und Matrix werden wiederverwendet;
Breathing läuft mit rund 30 Frames/s nur bei sichtbarem Drawable. Die bisherige
1,5-Sekunden-Modusüberblendung bleibt erhalten. Felder, Touchflächen und Layout
ändern sich nicht. Automatische Warnung ist unabhängig vom gespeicherten Glow-Schalter.

Regressionstests prüfen Schwellen, ungültige/veraltete Werte und Verbindungstrennung,
Farben/Mischung, Vier-Sekunden-Zyklus, Breitenänderung, Shader-Wiederverwendung,
Warnung bei deaktiviertem Modus-Glow sowie Aussetzen der Frameplanung bei Unsichtbarkeit.

## Breite und Intensität aller Effekte

Die radiale Breite in der Mitte bleibt beim Faktor 1,5 gegenüber dem ursprünglichen
Faktor 1,2, also 25 % breiter. Am 13.10.2026 wurde ausschließlich die letzte
Helligkeitserhöhung zurückgenommen: `modeGlowAlpha` wieder 140 statt 175.
Dies gilt zentral für Modusfarben, Übergänge, OFF-Fades und Akkuwarnung.
Die additive Darstellung bleibt an die maximale Displayhelligkeit begrenzt.
Vier-Sekunden-Breathing, 1,5-Sekunden-Fade, zusätzliche Warnbreite, Farben und
Shader-Wiederverwendung bleiben erhalten.

Für die Rücknahme wurden nur die beiden vorhandenen nativen Pixeltests für symmetrische
Verläufe/Clipping und Akku-Breathing/Breitenverhalten ausgeführt. Beide bestanden;
Kompilierung und gezielter Testlauf zusammen 7 Sekunden. Die Korrektur ist inzwischen
in der [Test-APK 0.7.10](milestone-0710.md) enthalten.

## Position Oben oder Seitlich

Design & UI → Fahrmodus-Anzeige bietet die Auswahl Seitlich (bisheriger Standard)
oder Oben. Oben zeichnet denselben sanften Lichtverlauf horizontal entlang der
oberen Dashboardkante, ohne seitliche Lichtquellen. Tiefe, Helligkeit, Farbwechsel
und Warn-Breathing bleiben erhalten; Positionswechsel startet keine Animation neu.
Akkuwarnungen folgen der Position auch bei ausgeschaltetem Fahrmodus-Glow.
`modeGlowPlacement` speichert SIDES/TOP; alte Einstellungen verwenden SIDES.
Design-Backups übernehmen die Wahl; alte Backups ohne Feld erhalten die aktuelle Wahl.
Ungültige Importwerte werden abgelehnt.

Rand- und Zonen-Glow nutzen dieselben zentralen Helligkeitsparameter: 140/255
und drei additive Lichtbeiträge (BicycleDesign.ambientGlowPasses).
