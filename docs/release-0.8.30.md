# Version 0.8.30

- HRV-Übersicht: einzelne Messungen mit Papierkorb und Bestätigung vollständig löschen, einschließlich RR-Daten und Tagesbefinden. Ein erneuter CSV-Import kann gelöschte Messungen wieder ergänzen. Eine ältere archivierte Messung desselben Tages kann danach wieder sichtbar werden.
- HRV-Messbildschirm: Pulswert allein horizontal zentriert, bpm rechts daneben; Puls 25 % größer, Textblock 10 dp höher. Zweizeilig „Atme ruhig / und entspanne dich.“ statt „Bereit“. Start-Pille breiter und flacher bei unveränderter Touchhöhe.
- HRV-Auswertung: sehr kleine Zahlenwerte über den Balken. Balken und Mittelwertsegmente erscheinen beim Scrollen kontinuierlich am Diagrammrand.

Sechs gezielte Tests erfolgreich: drei für Löschbestätigung, Wiederimport und Messbildschirm sowie zuvor drei für Diagrammbeschriftung, Gesten und Rand-Clipping. Native Vorschauen geprüft; Typografie- und UI-Katalogprüfung erfolgreich. Kein Gesamttestlauf oder Hardwaretest. Messalgorithmus und bestehende Datenformate unverändert.
