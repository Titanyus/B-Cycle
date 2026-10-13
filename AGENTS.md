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

## Grundsätzliche UX- und Konfigurationsphilosophie

BICYCLE ist ein leistungsfähiger, individuell anpassbarer Fahrradcomputer. Seine Bedienung bleibt trotz umfangreicher Funktionen einfach, logisch und übersichtlich.

**Sinnvolle Standards. Gezielte Individualisierung. Keine unnötige Konfiguration.**

- Für technische Darstellungs- und Berechnungsparameter sinnvolle Standardwerte verwenden.
- Keine zusätzlichen Einstellungen, Regler oder Schalter einführen, nur weil etwas technisch konfigurierbar wäre.
- Nur Optionen anbieten, bei denen unterschiedliche Nutzer tatsächlich unterschiedliche Anforderungen haben.
- Einstellungen unmittelbar dort platzieren, wo die zugehörige Funktion verwendet oder konfiguriert wird; keine tief verschachtelten Strukturen.
- Vorhandene Einstellungen wiederverwenden, statt ähnliche Optionen mehrfach anzubieten.
- Neue Funktionen müssen mit ihren Standardwerten unmittelbar sinnvoll funktionieren.
- Fortgeschrittene Möglichkeiten dürfen die alltägliche Bedienung nicht belasten.
- Graphenglättung, Rendering-Intervalle und Skalierungsstufen bleiben interne Implementierungsdetails.
- Standards anhand realer Fahrten verbessern, statt die Verantwortung für technische Feinabstimmung dem Nutzer zu übertragen.
- Keine vorhandenen Einstellungsmöglichkeiten eigenmächtig entfernen. Vereinfachungen zunächst vorschlagen und abstimmen.

Vor jeder neuen Konfigurationsoption prüfen: Hat sie klaren praktischen Nutzen? Gibt es unterschiedliche legitime Nutzungsbedürfnisse? Ist sie verständlich? Ist ihr Effekt unmittelbar nachvollziehbar? Ist sie logisch auffindbar? Wenn diese Kriterien nicht erfüllt sind, einen sinnvollen internen Standard bevorzugen.

Produktziel: Flexibilität eines leistungsfähigen Fahrradcomputers mit der Klarheit und unmittelbaren Bedienbarkeit einer durchdachten, reduzierten Oberfläche. **Nicht maximale Konfigurierbarkeit, sondern maximale Nutzbarkeit.**
