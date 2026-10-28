# Reihenfolge der Dashboardauswahl

Nutzerreihenfolge28.10.2026. Zuerst Gruppe ohne Karte, danach mit Karte. Je Gruppe drei Vorschaukarten pro Zeile, Reihenfolge links nach rechts und dann von oben nach unten.

Ohne Karte: `1/1/1/1` → `1/1/1/2` → `1/1/1/3/3` → `1/1/2/2/2`.

Mit Karte: `2` → `3` → `2/2` → `2/3` → `2/3/3` → `1/3` → `1/2/2` → `1/3/3` → `2/1/2/2`.

Die zunächst angegebene zweite Vorlage1/1/1/3 war nach Rückfrage nicht als Änderung gewünscht: bisherige zwei Kacheln in der letzten Zeile bleiben. Keine neue Vorlage und keine Belegungsmigration.

DashboardTemplate.pickerOrder ist ausschließlich die Anzeigeordnung. Enumreihenfolge bleibt unverändert, weil bisherige Standardseiten daraus abgeleitet werden. Grid filtert diese Ordnung für das aktive Profil; Indoor zeigt weiter nur die vier Vorlagen ohne Karte. Gespeicherte IDs, Belegungen, Anzahl und funktionales Auswählen unverändert.

Gezielter nativer Test prüft komplette Kartenreihenfolge, Indoor-Teilliste, Vollständigkeit und unveränderte Enum-Standardseiten. Noch nicht in APK0.8.55/Build96 enthalten.

Geöffneter Auswahlbereich erhält zentral16dp Abstand unter dem Accordionkopf; kleine Überschriften berücksichtigen bereits vorhandenes Containerpadding. Beide Designs nativ auf den tatsächlichen sichtbaren Abstand geprüft.


Veröffentlichung28.10.2026: Diese Änderungen sind in **APK0.8.56/Build97** enthalten. [Buildnachweis und Download](build-97.md). Frühere Aussagen zu fehlender neuer APK sind Umsetzungshistorie.
