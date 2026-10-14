# Entwicklungsregeln

1. Entwickle in Kotlin für Android 13+ (minSdk 33).
2. Priorisiere Performance, geringen Ressourcenverbrauch und schnelle Reaktionen.
3. Behalte die Single-Activity und getrennten Sensor-/Datenkomponenten bei. Im ausdrücklich beauftragten Branch feature/compose-settings verwenden Einstellungs- und Formularinhalte Jetpack Compose; Dashboard, Karte, Streckenliste und Analyse bleiben native Views. Der gemeinsame Dialograhmen, Navigation und Speicherarchitektur werden weiterverwendet.
4. Implementiere neue Funktionen möglichst schlank.
5. Vermeide unnötige Bibliotheken, Abhängigkeiten und Refactorings.
6. Verwende vorhandene Komponenten und Funktionen wieder.
7. Prüfe nach Änderungen möglichst die Kompilierung und relevante Tests; nutze dafür die vorhandenen Gradle-Tasks.
8. Ändere funktionierende Bereiche nur, wenn es für die Aufgabe notwendig ist.
9. Arbeite schrittweise und konzentriere dich auf die angeforderte Funktion.
10. Schließe mit einer kurzen, verständlichen Zusammenfassung der Änderungen und Testergebnisse; benenne ungeprüfte Punkte.

UI: Verwende für neue Dialoge die zentralen BicycleDesign-/BicycleComponents-/BicycleDialog-Komponenten, ThemeManager und BicycleIcons; keine eigenen Farb- oder Geometrievarianten. Einstellungsansichten verwenden den Vollbild-Host mit fixem Header/Footer; kurze Bestätigungen bleiben kompakt. Navigation: jeder Dialog hat im Header links den Zurück-Chevron; Titel sind unabhängig davon zur gesamten Fensterbreite zentriert. Speichern/Abbrechen/Schließen entfallen als Footer-Navigation; nur fachliche Aktionen bleiben dort. Gültige Einstellungsänderungen werden automatisch gespeichert; ungültige bzw. unvollständige Eingaben überschreiben keine gültigen Einstellungen. Destruktive Aktionen behalten eine Bestätigung. Abschnittsüberschriften verwenden appweit 23 sp (BicycleDesign.titleSp), wie die Streckendetails, und den zentralen strongTypeface mit Gewicht 900 Black (Roboto über Android-Systemfont, soweit verfügbar). Alle bisherigen System-Fettschriften verwenden diesen zentralen Schnitt; Bebas-Neue-Messwerte und Fenstertitel bleiben erhalten. Funktionale Bosch-Modusfarben bleiben vom App-Theme getrennt.

## Einheitliche Navigation und Zustands-Icons

Jeder Einstellungs-Menüpunkt öffnet einen eigenen Unterdialog. „Erweitert“ ist ein separater Screen, kein Accordion. Nur im Haupt-Einstellungsmenü sind die Zeilen doppelt so hoch und Menüschrift sowie führende Menü-Icons doppelt so groß (28 sp / 48 dp); Untermenüs verwenden die normale Listenhöhe. Header-Chevron und Android-Zurück führen zum vorherigen Screen und erhalten dessen Scrollposition. Ein Footer ohne fachliche Aktionen wird vollständig entfernt, inklusive seines Platzbedarfs.

Umschalt-Icons zeigen **den aktuellen Zustand**, nicht die nächste Aktion. Beispiele: helle Karte = sun, dunkle Karte = moon; Norden oben = compass, Fahrtrichtung oben = arrow-up-from-dot; Passwort verborgen = eye-closed, sichtbar = eye. Reine Befehls-Icons (Import, Löschen usw.) bleiben Aktionssymbole. Accessibility-Beschreibungen dürfen zusätzlich die nächste Aktion erklären.

## Ausrichtung und Dashboard-Bearbeitung

Die App-Oberfläche wird ausschließlich im Hochformat verwendet. Layout- und UI-Tests prüfen unterschiedliche Hochformat-Größen; keine neuen Querformat-Fälle hinzufügen. Bestehende responsive Komponenten nicht ohne konkreten Auftrag umbauen.

Dashboardfelder werden nur im bewusst aktivierten Editor bearbeitet: Long-Press auf eine obere Kachel oder „Design & UI → Dashboard bearbeiten“. Kartenfelder haben keinen Long-Press-Einstieg. Der gemeinsame Editor verwendet PRIMARY-Rahmen und den zentralen Beenden-Button auf der Karte. Keine Stillstandssperre für diese Bearbeitung ergänzen; bestätigte Änderungen verwenden die bestehende Persistenz.

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

## Compose-Branch

Release und composeDebug verwenden Compose-Einstellungen und die separate App-ID
`de.bcycle.app.compose` (B-Cycle Compose). Debug behält die native Vergleichsoberfläche
für die bisherigen UI-Tests. Das ist ein Build-Schalter, keine neue Nutzeroption.
Compose verwendet die bestehenden BicycleDesign-/ThemeTokens-/Lucide-Ressourcen;
keine Material-Palette oder zweite Konfigurationsablage hinzufügen. Formulare verwenden
die vorhandenen Validierungen, Sensoren und Export-/Speicherdienste. Native Datums-
und Dateiauswahl sowie kurze Bestätigungen bleiben erlaubt. ComposeDialogContent
verwaltet seine Composition/Lifecycle, übersteht die Übertragung in BicycleFullscreen
und gibt Beobachter beim endgültigen Schließen frei. Keine AndroidView-Wrapper um
alte Formularlayouts als vermeintliche Migration verwenden.
