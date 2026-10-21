# Konkrete Optimierungsmöglichkeiten – Codeprüfung vom 21.10.2026

Auf Wunsch nach Umsetzung von Tagesbefinden und weichen Dashboard-Kurven. Dies ist eine statische Codeprüfung, kein Geräteprofiling oder vollständiger Lasttest. Die unten vorgeschlagenen Optimierungen wurden nicht implementiert. Keine grundlegende Neuarchitektur, neue Datenbank oder externe Abhängigkeit erforderlich. Bestehende Datenformate, Sensorverhalten und Aufzeichnungen bewahren.

## Priorität 1: HRV-Historie mit kleinen Zusammenfassungen laden

**Befund:** `HrvStore.list()` liest für jede Messung die vollständige `result.json`, einschließlich RR-/NN-Folgen und Artefaktprotokoll. `HrvController.history()` gibt alle JSON-Objekte an die UI; `MainActivity.showHrvHistory()` benötigt für die Liste zunächst nur Datum, Gültigkeit und RMSSD und hält dennoch sämtliche Ergebnisse für den Detailklick.

**Nutzen:** Mit täglichen Messungen über Monate/Jahre weniger Speicherverbrauch und schnelleres Öffnen der Historie; zugleich passende Grundlage für die geplanten Verlaufsdiagramme.

**Kleinster Schritt:** Kleinen HistorySummary-Typ mit ID/Datum/RMSSD/Qualität einführen; Vollergebnis erst beim Öffnen einer Messung laden. Optional danach ein wiederherstellbarer Metadatenindex je Messung, ohne die vorhandenen Journale umzuschreiben. Erst mit synthetischen 100/500/1000 Messungen vergleichen.

**Regression:** Gleiche Sortierung/Anzahl, abgelehnte Messungen sichtbar, alter Journalbestand ohne Index, Detailwerte/Fragebogen identisch und unveränderte Backup-/Exportdateien. Bei Indexkorruption aus Originalen rekonstruieren.

Quelle: `app/src/main/java/de/bcycle/app/HrvStore.kt` (`list`), `HrvController.kt` (`history`), `MainActivity.kt` (`showHrvHistory`).

## Priorität 1: Fahrten- und Profilmetadaten als zusammenhängenden Snapshot verwenden

**Befund:** `MapState.entries` baut die komplette Liste neu, liest Anzeigenamen, löst den Radtyp je Fahrt auf und sortiert. `RideProfileColor.type/name/color` parsen für einzelne Fahrten wiederholt Profil-/Zuordnungs-JSON. Besonders konkret: `MainActivity.refreshDetails()` ruft `detailEntry()` und damit die gesamte Eintragsliste ab, bevor seine spätere Änderungssignatur den Rest der UI-Arbeit abkürzt. Eine unveränderte geöffnete Detailansicht braucht dafür nur eine Fahrt.

**Nutzen:** Weniger UI-Arbeit bei großen Bibliotheken und beim Aktualisieren offener Streckendetails, Filter und Profilfarben. Ein echter potenzieller Engpass; noch keine gemessene Zeitersparnis behaupten.

**Kleinster Schritt:** Erst einen direkten ID-Lookup aus einem revisionsgebundenen Metadaten-Snapshot anbieten. Profil-/Namenszuordnung pro Snapshot einmal parsen; vorhandene Revisionen nutzen. Invalidierung auch bei Profiltyp-/Namens-/Themeänderung, Fahrradlöschung, Umbenennung, Import und Restore berücksichtigen; bloßes routeRevision-Caching ohne diese Fälle wäre falsch.

**Regression:** App-Profil-Radtyp gewinnt weiterhin; gelöschtes Profil behält letzten Typ; Farben/Namen ändern sofort, Export-/Uploadmetadaten bleiben aktuell. Mit 50/500/2000 synthetischen Fahrten und offenem Detailscreen messen.

Quelle: `MapState.kt:55`, `RideProfileColor.kt:33`, `RideProfileColor.kt:46`, `RideProfileColor.kt:54`, `MainActivity.kt` (`detailEntry`, `refreshDetails`).

## Priorität 2: Dienststatus nach Revision indizieren

**Befund:** `ActivityServiceUploads.state()` parst den gesamten Upload-Jobbestand und durchsucht ihn für einen Fahrt-/Dienststatus. Das Transfermenü aktualisiert mehrere Ziele; bei großer Jobhistorie wiederholt sich diese Arbeit. `RouteUploads` besitzt bereits einen revisionsgebundenen Statuscache als vorhandenes Vorbild.

**Nutzen:** Transfermenüs bleiben bei wachsender Uploadhistorie flüssig, ohne Netzwerkprotokolle anzufassen.

**Kleinster Schritt:** Nur für Dreeve/Strava einen Snapshot/Index nach Dienstrevision und Zielkonto aufbauen. Datei-/Metadatenabgleich unverändert ausführen, Fehler-/Processing-Zustände und neuere Dateiversionen nicht pauschal zusammenfassen. Keine erfolgreichen Belege löschen oder allgemeine neue Queue-Abstraktion bauen.

**Regression:** Kontowechsel, Pending/Processing/Failed/Saved, Wiederholung nach HTTP-202-Annahme, manuelle Benennung, anderer Radtyp, geänderte Dateien und App-Neustart. Erfolg nur bei bestätigter Aktivität.

Quelle: `ActivityServiceUploads.kt:20`, `ActivityServiceUploads.kt:23`, `RouteTransferMenu.kt:27`, `RouteUploads.kt` (`statusCache`).

## Priorität 2: Vor HRV-Trenddiagrammen eine klare Tagesregel festlegen

**Befund:** Ergebnisse und Tagesbefinden liegen aktuell pro Messung vor. Mehrere Messungen am selben Tag, unterschiedliche Messdauern, wiederholte Messungen nach einem schlechten Ergebnis und fehlende Antworten lassen sich daher nicht ohne fachliche Regel zu einem Tageswert zusammenziehen.

**Nutzen:** Spätere Vergleiche von Schlaf/Alkohol/Training mit HRV werden nachvollziehbar und nicht durch selektiv ausgewählte hohe Messwerte oder neutral interpretierte Leerfelder verzerrt.

**Kleinster Schritt:** Tagesaggregation als abgeleitete Sicht definieren (beispielsweise erste vergleichbare Morgenmessung; konkrete Auswahl noch mit Nutzer abstimmen), alle Einzelmessungen behalten. Messprotokoll, Qualität, geringe Abdeckung, korrigierte Werte und subjektive Antworten sichtbar mitführen. Keine historischen Resultate ändern und keinen versteckten Gesamtscore hinzufügen.

**Regression:** Zeitzonen/Sommerzeit, mehrere Messungen, fehlende Antworten, unterschiedlich lange Protokolle, Fahrradwechsel, Fahrt ohne Leistung und manuelle Trainingsangaben. Mit synthetischen Wochenverläufen prüfen.

Quelle: `HrvStore.kt`, `HrvQuestionnaire.kt`, Nutzerziel in `docs/hrv-score-plan.md` und `docs/hrv-questionnaire.md`.

## Priorität 2: Kleine verlässliche Tests auch vor Release in CI verfügbar machen

**Befund:** Der aktuelle GitHub-Workflow erstellt eine optimierte APK und kontrolliert die Signatur, führt aber keine Unit-/UI-Tests aus. Die gezielten Prüfungen laufen bisher lokal. Ältere Buildnachweise nennen historische UI-Testfehler; deren heutiger Stand wurde hier nicht durch einen Gesamtlauf neu bewertet.

**Nutzen:** Schutz vor Regressionen auch nach einem Umgebungswechsel; kein unnötiger APK-Build für reine Testläufe.

**Kleinster Schritt:** Separaten manuell startbaren Testworkflow mit einer kleinen stabilen Auswahl für Aufzeichnungs-/FIT-Kompatibilität, Sicherung und betroffene UI-Komponenten vorschlagen. Bekannte ältere Fehlschläge gezielt reproduzieren und reparieren; keine Tests zum Grünwerden deaktivieren und keine vorhandenen Referenzfixtures regenerieren.

**Regression:** Testworkflow baut/veröffentlicht keine installierbare APK, liest keinen Signierschlüssel und verändert keine Daten. Bestehender Release-Prozess bleibt erhalten.

Quelle: `.github/workflows/android-debug.yml:59`, `docs/build-59.md`, `docs/build-60.md`.

## Niedrigere Priorität: HRV-Navigation klein aus MainActivity herauslösen

Messung, Historie, Tagesbefinden und kommende Verlaufsansichten werden zu einem zusammenhängenden Bereich. Sobald die Trends umgesetzt werden, wäre eine kleine lokale HRV-Screen-Koordination in einer eigenen Datei hilfreich. Controller, Speicher und native Views sind bereits getrennt; kein MVVM-/DI-/Repository-Großumbau nötig. Vorher vor allem Profil-/Backing-/Header-/Zurück-Verhalten mit bestehenden Dialogen prüfen. Aktuell weniger dringlich als die beiden genannten Skalierungspunkte.

## Was ich bewusst nicht umbauen würde

- Live-Graph-Historie ist begrenzt und reduziert auf einem Displayworker; Dashboard-Pfade werden gecacht und nicht in jedem draw neu berechnet. Neue Interpolation bleibt reine Anzeigegeometrie.
- Die Aufzeichnung nutzt bereits einen eigenen fachlichen Feldkatalog und typisierte Speicherergebnisse; kein erneuter Schichtenumbau erforderlich.
- Streckenanalyse läuft bereits abseits der UI und bündelt Abschnitte aus einer Dekodierung; keine zweite pauschale FIT-Verarbeitung daneben einführen.
- Bestehende atomare HRV-Dateien, vollständige Sicherungen, Profilidentitäten und dauerhafte Signatur beibehalten. Sie lösen reale Kompatibilitäts-/Datenverlustprobleme.

Empfohlene Reihenfolge: zuerst HRV-Historie und Fahrtenmetadaten mit synthetischen Beständen messen, danach den konkreten Engpass klein beheben. Tagesregel vor Trendanalyse vereinbaren; Testworkflow separat vorbereiten. Keine der Empfehlungen erfordert neue externe Dienste.
