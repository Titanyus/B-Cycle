# Profilbezogene Fahrtnamen

Unter Profil → FIT-Aufzeichnung → Fahrten benennen steht ein aufklappbarer Bereich mit freiem Text (bis 60 Zeichen) und fünf unabhängigen Auswahlsegmenten: Datum, Startzeit, Distanz, Fahrtzeit (unverändert die Bewegungsdauer), Durchschnittstempo. Oben gemeinsame Streckendetail-Icons, darunter kleine Beschriftungen; Darstellung wie Zonenauswahl. Änderungen werden direkt gespeichert und bei Aufnahme/Speichern gesperrt.

Beispiel: `Feierabendrunde · 21.10.2026 17:30h · 32,4km · 1:15:20h · 25,8km/h`.

Die Reihenfolge ist fest: eigener Text, Datum/Startzeit, Distanz, Bewegungszeit, Bewegungsschnitt. Datum und Startzeit stehen mit einem Leerzeichen zusammen. Sonst trennen Mittelpunkte. Keine Leerzeichen vor Einheiten; Startzeit HH:mmh, Bewegungsdauer h:mm:ssh. Dezimaltrennzeichen nach Sprache des Geräts. Fehlende Kennzahlen werden weggelassen; es werden keine Nullwerte erfunden. Ohne jeden nutzbaren Bestandteil heißt die Fahrt „Fahrt“. Standard ist Fahrt + Datum/Startzeit.

Vorlage, Zeitzone und Sprache werden beim START in einer Begleitdatei zum Aufnahmejournal eingefroren. Der endgültige Name entsteht beim Speichern aus den tatsächlichen Daten der vollständigen privaten FIT-Quelle, unabhängig von der öffentlichen FIT-Feldauswahl. Originalmesswerte und FIT-Dateinamen/IDs bleiben unverändert. Anzeigename, manuelle Einzeldateiexporte und verknüpfte Aktivitätsnamen verwenden die vorhandene RideNames-Verarbeitung. Bewusste Benennung gewinnt gegenüber späteren Dienst-Automationsnamen. Bestehende oder importierte Fahrten werden nicht rückwirkend umbenannt.

Bei fehlgeschlagenem Export bleiben Journal und Namensvorlage erhalten. Ein erneuter Speicherversuch verwendet dieselbe Vorlage; bereits manuell geänderte Namen werden erhalten. Neue Journale haben die Vorlage, ältere ausstehenden Journale ohne Vorlage behalten das bisherige Namensverhalten.

Speicher: `rideNamingV1` im bestehenden profilbezogenen UI-Bankmechanismus. Profilwechsel und Profilübernahme berücksichtigen die Vorlage. FIT-Einstellungssicherung enthält die aktive Vorlage; Profilsicherung enthält die Vorlagen aller Profile. Wiederherstellung validiert Text, Felder und Typen, zusätzlich zu den vorhandenen Aufzeichnungseinstellungen. Gemeinsam verwendete Preference-Dateien werden beim Wiederherstellen zusammengeführt.

Optik-Konvention: Gemeinsamer umlaufender Rahmen der segmentierten Mehrfachauswahl wie bei der Trainingshilfe. 8 dp Abstand zwischen Namensfeld und Auswahlgruppe sowie zum vorangehenden Control. Icons und kleine Labels werden als gemeinsamer zentrierter Block mit 2 dp Abstand gezeichnet; native Radio-/Checkbox-Semantik, Touchhöhe und Speicherverhalten erhalten. Zentrale Umsetzung: BicycleSegmentButton.stackedIcon, BicycleComponents.segmentedControlRow/controlSpacing.
