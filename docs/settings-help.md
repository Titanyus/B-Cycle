# Lokale Hilfe auf Einstellungsseiten

Seit 18.10.2026 öffnet das Lucide-Info-Symbol rechts im Seitenheader eine lokale Hilfeseite. Hauptmenü, Fahrer, Profile, Karten, Design, Feldauswahl, Benachrichtigungen, Strecken und Details, Daten & Speicher und Erweitert besitzen passende Inhalte. Diagnose, Wiederherstellung, Zurücksetzen, Quellen/Lizenzen und separate Export-/Uploadseiten sind ebenfalls zugeordnet.

`SettingsHelpPage` ordnet Abschnitte zu; alle Titel und Texte stehen in übersetzbaren Android-Ressourcen (`values/settings_help.xml`). Die Hilfe arbeitet ohne Netzwerk und enthält keine Kontodaten. Neue Einstellungsseiten müssen eine passende Zuordnung und kurze Erklärungen ihrer Funktionen erhalten; der allgemeine Navigationshinweis ist nur ein Fallback.

Abschnitte verwenden BicycleSectionHeader.accordion und BicycleStickyScrollView. Sie sind anfangs geschlossen und unabhängig aufklappbar. Normale Text-/Abstands-/Theme-Konventionen bleiben erhalten. Hilfeseiten selbst haben keinen weiteren Hilfe-Button. Zurück verwendet DialogNavigation und stellt die unveränderte Elternansicht wieder her. Auf der Hauptseite bleibt der globale Hell/Dunkel-Schalter links neben Info; der Titel bleibt zur gesamten Fensterbreite zentriert.

Streckendetails: Das Stiftsymbol im Header entfällt. Antippen des Titels benennt die Fahrt weiterhin um; zusätzlich gibt es Umbenennen im Footer. Info öffnet die Auswertungshilfe. Bestätigungsdialoge erhalten keinen zusätzlichen Hilfezugang.

Direkt unter Strecken-Aufzeichnung erklärt ein kurzer Text die zusätzlichen FIT-Felder, ihre Unabhängigkeit vom Dashboard und den GPS-Schalter für Positionsdaten.

## Gruppierung ab 0.8.2

Die Sticky-Accordions bilden die Hauptabschnitte der eigentlichen Seite ab. Einzelne Funktionen stehen darin als normale kleinere fette Unterüberschriften mit Beschreibung (8 dp Abstand, 16 dp zwischen Funktionen), nicht als zusätzliche Sticky Header.

Fahrer: Persönliche Daten, Trainingsparameter, Herzfrequenzzonen und Leistungszonen. Die drei Pulsmodelle werden ausführlich innerhalb Trainingsparameter erklärt. Profile: aktueller Fahrradname, Sensoren, gegebenenfalls Indoor-Korrektur, FIT-Aufzeichnung, Strecken-Aufzeichnung, Karten, Design & UI, Trainingshilfe. Dynamischer Fahrradname und tatsächlich sichtbare Hauptabschnitte werden aus der geöffneten Profilseite übernommen. Weitere Seiten gruppieren analog nach ihren Hauptbereichen. Alle Texte bleiben übersetzbare Ressourcen; kein weiterer Info-Button innerhalb der Hilfe.
