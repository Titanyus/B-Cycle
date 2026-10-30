# Aktueller Auftrag und Stand

Datum 30.10.2026. Nutzer hat Sicherung des bisherigen Stands, anschließende Codebereinigung und danach einen gleitenden Dashboardwechsel beauftragt. Währenddessen weitere UI-Korrekturen ergänzt. Keine davon ersetzt den Dashboard-Auftrag.

## Gesicherter Stand / letzte APK

- Git-Tag `vor-den-bereinigungen-2026-10-30` auf GitHub, Quelle `ed35551`.
- Letzte veröffentlichte APK: [0.8.68 / Build 109](build-109.md), Code 138; noch vor den Bereinigungen.
- Vollständige frühere Übergabe: [Archiv](archive/CONTINUE-vor-bereinigung-2026-10-30.md). Historische offene APK-Aufträge dort sind erledigt.

## Abgeschlossen

[Bereinigung und ergänzte UI-Korrekturen](cleanup-2026-10-30.md): alte Modus-/Licht-Optionen und seitlicher Bosch-Zeichenpfad weg, alte Warnoption aus Profilmodell weg, alte Sicherungen weiterhin lesbar aber bereinigte neue Exporte. Zentrale Kacheldekoration auch im Fahrradfilter. Unbenutzter Code/Texte entfernt. AGENTS.md auf aktuelle Regeln verdichtet, unveränderte Historie archiviert.

Zusätzliche Nutzerwünsche: Dashboard-Layout-Kacheln mit Standardabstand nach kleiner Überschrift; horizontale Trenner 2 dp/25 %; Rahmen-Ripple deckt Titel und Chevron einschließlich gesamten Rahmens ab; Trink-Erinnerung im Rahmen ohne Trenner davor; Feldauswahl Daten/Trend/Graph jeweils im Rahmen; Höhenzeile bei Fahrten ohne GPS entfernt. Bedienung/Listener/Persistenz erhalten. Siehe Prüfstand im Bereinigungsdokument. Gezielte Prüfungen erfolgreich; keine Gesamtsuite/kein realer Gerätetest.

## Noch auszuführen

Gleitender Dashboardwechsel bis 0,5 s, vorgesehen 300 ms. Dashboardbereich einschließlich Karte und Kartenbuttons gemeinsam seitlich bewegen, Footer bleibt stehen. Vorhandene Swipe-Geste auf Datenfeldern und Karten-Pan/Pinch beibehalten. Ein lebender Karten-/Sensorbaum, keine zweite MapView. Auf reduzierte/deaktivierte Systemanimation, schnelle Folgewischgesten, Appwechsel, Profilwechsel und Bitmap-/Listener-Freigabe achten. Eigenen Änderungscommit nach Bereinigung erstellen und gezielt prüfen. Keine neue APK ohne weiteren Auftrag.
