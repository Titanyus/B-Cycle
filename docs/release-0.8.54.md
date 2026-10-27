# B|CYCLE 0.8.54

Enthält alle Änderungen seit APK0.8.53 / Build93.

- Leistungs-/Pulsverteilungen in Streckendetails als fein gegliederte, gleich breite Balken statt Kurve und farbiger Hintergrundfläche. Normalerweise5W/2bpm-Schritte, lineare Achse und unterschiedliche Balkenzahl je Zone. Bei großen Spitzen angepasste feste Schrittweite, maximal128Balken. Zonenfarben aus vorhandener Palette, exakte Grenzen, farbige Z-Marker und vertikale Trennlinien. Zeitgewichtung, Rohdaten, Aufnahme und Kennzahlen bleiben erhalten.
- Zentrale Trendberechnung aus den gewählten FIT-Primärquellen einschließlich Fallback. Aktuelle Werte, Durchschnitts- und GPS-Varianten teilen denselben Chevronstand, auch zwischen Dashboard und Kartenfeldern. Bestehende Auswahl, wo Chevrons sichtbar sind, bleibt unverändert.
- Dropdown-Rahmen für Sicherungen, HRV-Wiederherstellung, lokale Speicherung und Cloudziele; global auch bei Wartungs-Bauteilen: Icon · Titel · Chevron · optionaler Schalter/Aktionsbutton. Native Schalter und fachliche Aktionen unverändert. Direkte Wiederherstellungsbuttons linksbündig mit Symbol.
- Karteneinstellung Höhe auf Karte anzeigen, je Fahrradprofil gespeichert und gesichert; steuert nur das aktuelle Höhenlabel.
- Volle Flat-Sticky-Breite bei Wartung und globale16dp-Abstände der Aktionstrenner. Trainingshilfe in Feldwahl breiter,16dp unter Datenquellen-Dropdown. Benachrichtigungsdauer kurz mit s.

Gezielte Prüfungen während Umsetzung erfolgreich: Karten-/Wartungs-/Backupdarstellung,19 Trend-/Feldwahlfälle,14 Rahmen-/Backup-Navigationsfälle,3 globale Rahmen-/Wartungsfälle,8 Histogramm-/Native-Darstellungsfälle. Keine unnötige Gesamtsuite für den APK-Auftrag wiederholt. Native Vorschau mit synthetischen Daten; kein erneuter Gerätetest oder privater Cloudzugriff.

Buildnachweis folgt nach Veröffentlichung.
