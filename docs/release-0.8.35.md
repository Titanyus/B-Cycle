# Änderungen in 0.8.35

- HRV-Tagesbefinden gilt pro Kalendertag; erneute Messungen übernehmen Antworten. Tagesantworten bleiben auch nach Messungslöschung erhalten und sind in Sicherung/Export enthalten. Sichtbare Sliderstandards gelten ohne zusätzliche Berührung.
- Automatisches HRV-Angebot wechselt erst um 04:00 Ortszeit zum nächsten Tag; Messdatum und Tagesbefinden bleiben kalendertäglich.
- Gemeinsame feste HH:mm-Zeitmaske; Schlafdauer, Einschlaf- und Aufwachzeit als beschriftete Zeilen. Maßeinheiten stehen geghostet innerhalb der Eingabefelder, ohne gespeicherte Zahlen zu verändern.
- Globale Auswahllisten zeigen Schalter statt Auswahlpunkten, bei erhaltener Einfach-/Mehrfachauswahl.
- HRV-Balkenbeschriftungen behalten beim Scrollen dieselbe Auswahl, auch bei schmaler Skalierung.
- Fokusrahmen mit schmalem Verlaufshighlight, echter naher und äußerer SCREEN-Unschärfe; aktive ACCENT-Flächen mit zentralem Verlauf. Keine Änderungen an Graphen durch diese Blur-Gestaltung.
- Separater [HTML-Designer](input-designer.md) für Konturen und Innenflächen mit Live-Vorschau, JSON- und PNG-Export. Keine zusätzliche Konfigurationsseite in der App.

Gezielte Tests in den Umsetzungsschritten erfolgreich; native Vorschauen und Browser-Designer geprüft. Bestehende Profile/FIT und Messdaten bleiben lesbar. Keine vollständige Testsuite oder Geräteprüfung.
