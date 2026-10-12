# Entwicklungsregeln

1. Entwickle in Kotlin für Android 13+ (minSdk 33).
2. Priorisiere Performance, geringen Ressourcenverbrauch und schnelle Reaktionen.
3. Behalte die bestehende Single-Activity-Struktur mit nativen Kotlin-Views und getrennten Sensor-/Datenkomponenten bei, sofern keine wichtigen technischen Gründe dagegensprechen.
4. Implementiere neue Funktionen möglichst schlank.
5. Vermeide unnötige Bibliotheken, Abhängigkeiten und Refactorings.
6. Verwende vorhandene Komponenten und Funktionen wieder.
7. Prüfe nach Änderungen möglichst die Kompilierung und relevante Tests; nutze dafür die vorhandenen Gradle-Tasks.
8. Ändere funktionierende Bereiche nur, wenn es für die Aufgabe notwendig ist.
9. Arbeite schrittweise und konzentriere dich auf die angeforderte Funktion.
10. Schließe mit einer kurzen, verständlichen Zusammenfassung der Änderungen und Testergebnisse; benenne ungeprüfte Punkte.

UI: Verwende für neue Dialoge die zentralen BicycleDesign-/BicycleComponents-/BicycleDialog-Komponenten, ThemeManager und BicycleIcons; keine eigenen Farb- oder Geometrievarianten. Einstellungsansichten verwenden den Vollbild-Host mit fixem Header/Footer; kurze Bestätigungen bleiben kompakt. Footer: Abbrechen = X (Cancel), Schließen = Tür mit Pfeil (Close). Funktionale Bosch-Modusfarben bleiben vom App-Theme getrennt.
