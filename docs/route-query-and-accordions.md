# Accordions, Dropdownflächen und Streckenfilter – Arbeitsstand nach 0.8.4

Kein APK-Build angefordert. Alle Sticky Header werden aufklappbar; der Fahrradtitel bleibt ein Umbenennen-Zugang. Bestehende Profile/Fahrer/Hilfe-Gruppen behalten ihre Bindungen, andere gemeinsame Sticky-Abschnitte werden einmalig vor der Messung gruppiert. Standard offen, außer ausdrücklich anfangs geschlossenen Auswahldialoggruppen/Hilfen. Native Eingaben und Listener bleiben erhalten.

Dropdown-Fenster TERTIARY, Kontur weiter 1 dp FOREGROUND 50 %, Rundung 6 dp. Die Basisbuttons behalten ihre eigene Fläche. Horizontale Abschnittstrenner 16 dp oben/unten; die zuvor bewusst kompakten Fahrdatenblöcke bleiben bei ihrem kleineren Abstand.

Benachrichtigungen: Schalter und Chevron unabhängig; im Inhalt Android-Zugriff, Erklärung und Status. Darunter Benachrichtigungsposition mit normaler Inhaltsüberschrift, kein zusätzlicher Sticky Header. Sekundenwert näher an der Sliderlinie. Die Appauswahl ist ein Sticky-Accordion.

Erweitert zeigt Wiederherstellung ungespeicherter Fahrten und GPS-Erklärung nicht mehr als Menüeinträge; ihre zugrundeliegenden Funktionen werden nicht entfernt.

Strecken: Footer mit Import, Filter, Sortieren. Kein oberes Filterdropdown, Auge bei fehlenden GPS-Daten vollständig ausgeblendet. Filter- und Sortierfenster über dem Footer öffnen, begrenzt und scrollbar. Die sechs Filtergruppen beginnen eingeklappt und zeigen eine Zusammenfassung. Sofortige Anwendung, keine Bestätigungs-/Abbrechen-Footer. Native Tastatur für Bereichsfelder. Ungültige Entwürfe ersetzen keine gültigen Filter. Datumsdialog übernimmt Änderungen unmittelbar. Bewegungszeit in h:mm, Distanz km, Geschwindigkeit km/h. Datum von/bis inklusive ganzer lokaler Tage, auch an Sommerzeitgrenzen. Schnellwahl Montag–Sonntag und vollständige Kalendermonate.

Importiert/Bike/E-Bike/Indoor mehrfach auswählbar; Fahrradfilter nach gespeicherter Profilzuordnung, zusätzliche Auswahl Ohne Zuordnung. Fehlende Werte bleiben ohne entsprechenden Filter sichtbar, werden bei aktiven Bereichen ausgeschlossen. Sortierung Datum/Distanz/Bewegungszeit/Ø Geschwindigkeit in beiden Richtungen, fehlende Werte stets am Ende. Sortierung stabil bei gleichen Werten. Filter zurücksetzen erhält die Sortierung. Filter-/Sortierzustand bleibt in der Manageransicht auch bei Rückkehr aus Details.

Detailkopf: links Herkunfts-/Radtyp-Icon und Dateiformat; rechts zentrierte Textzeilen Fahrradname/Datum. Der Zweizeilenplatz bleibt auch bei älteren Fahrten ohne bekannte Zuordnung erhalten. Keine künstlichen Radzuordnungen. Wettertrenner verwendet 16-dp-Abstände, kompakte Kennzahlengruppen bleiben erhalten.

Gezielte Tests decken Bereichskombinationen, Mehrfachauswahl, unbekannte Werte, inklusive Datumsgrenzen, Sortierfelder/-richtung, Reset, Popup-Eingaben/Entwürfe, h:mm, Wochenwahl, Accordion-Zustand/Erhalt der Eingaben, Benachrichtigungen, Auswahlmenü und Listen-/Detail-Navigation ab. Kein realer Gerätetest.
