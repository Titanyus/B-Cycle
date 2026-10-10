# Entwicklungsregeln

1. Entwickle in Kotlin für Android 13+ (minSdk 33).
2. Priorisiere Performance, geringen Ressourcenverbrauch und schnelle Reaktionen.
3. Behalte die bestehende Architektur mit nativen, in Kotlin aufgebauten Android-Views bei, sofern keine wichtigen technischen Gründe dagegensprechen.
4. Implementiere neue Funktionen möglichst schlank.
5. Vermeide unnötige Bibliotheken, Abhängigkeiten und Refactorings.
6. Verwende vorhandene Komponenten und Funktionen wieder.
7. Prüfe nach Änderungen möglichst die Kompilierung und relevante Tests; nutze dafür die vorhandenen Gradle-Tasks.
8. Ändere funktionierende Bereiche nur, wenn es für die Aufgabe notwendig ist.
9. Arbeite schrittweise und konzentriere dich auf die angeforderte Funktion.
10. Schließe mit einer kurzen, verständlichen Zusammenfassung der Änderungen und Testergebnisse; benenne ungeprüfte Punkte.
