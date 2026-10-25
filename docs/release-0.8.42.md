# B|CYCLE 0.8.42

- Glow: aufklappbare Sticky-Abschnitte als zusammenhängende TERTIARY-Karten mit 18-dp-Rundung, ohne Rahmen. Geschlossene Header vollständig gerundet; offene Karten umfassen Header und Inhalt. Flat bleibt unverändert. Bestehende Sticky-/Aufklapp-/Scrollfunktionen erhalten.
- Gemeinsame Dialogbasis aus der HRV-Angebotskarte: normale Flächen und Header TERTIARY, diffuser Schatten auch in Flat. Trink-Hinweis als PRIMARY-Variante ohne Header, mit unveränderter Position, Dauer und Bedienung.
- Dezenteres freigegebenes Glow-Preset9 in die zentralen Appwerte übernommen, einschließlich vollständiger Konturdeckkraft und unabhängiger Theme-Farbstärken.
- Einheitliche Einstellungs-Slider für Trink-Erinnerung, Trackdarstellung und Benachrichtigungsdauer: Wert im PRIMARY-Pillengriff, Titel links, gemeinsamer Zeilenabstand. Tagesbefinden unverändert.
- FIT-Sticky-Überschrift verwendet zuverlässig die zentrale Schriftrolle. HRV-Puls-Glow wieder in beiden Designs, mit weichem 300-ms-Anstieg.

Keine Änderungen an Messdaten, Aufzeichnungsformaten oder Dienst-/Eingabelogik. Gezielte native Komponenten-, Profil-, Sticky-, Dialog-, HRV-, Slider- und Flat-/Glow-Prüfungen; native Vorschauen visuell geprüft. Kein vollständiger Gesamttestlauf oder Hardwaretest.

Drei ältere UI-Testfehler bleiben unverändert: StickyAccordionSettingsTest Zeile51 (Spinnerlayout), Zeile129 (alte Popup-SECONDARY-Erwartung), ProfileAccordionTest Zeile53 (alte Strong-Schrifterwartung). Alle drei mit identischen Fehlerstellen am unveränderten Vorgängerstand1a64a50 nachgewiesen. Keine Tests entfernt/deaktiviert und keine Appfunktion zur Anpassung an veraltete Erwartungen geändert.
