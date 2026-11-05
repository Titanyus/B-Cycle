# B|CYCLE 0.8.93

- FLAT-Sticky-Header global SECONDARY statt abgedunkeltem PRIMARY; obere Rahmenlinie und TERTIARY-Inhalte erhalten.
- Kompakter Karteneditor ohne Überschrift/Erklärung. Farbfeld neben Objektauswahl, HEX in der Beschriftungszeile, vier RGBA-Regler mit 48-dp-Touchhöhe ohne zusätzliche Zeilenpolster. Vier Iconaktionen für Objektreset, Gesamtreset, JSON und Schließen nebeneinander.
- Alpha übernimmt vorhandene Objekttransparenz und setzt absolute Deckkraft 0–255. Alte RGB-Änderungen behalten ohne expliziten Alpha-Wert die Originaltransparenzen. JSON/Profil/Sicherung unterstützen das optionale Alpha-Feld; Flächen/Linien/Hintergrund weiter direkt aktualisiert, Text über bisherigen Atlasaufbau.
- Zuletzt gewähltes Objekt beim erneuten Öffnen wiederherstellen, auch nach App-Neustart.

Gezielte Editor-/JSON-/Profil-/Sicherungs-/native Stil-/Alpha-/Reset-/Queue- und Sticky-Header-Tests erfolgreich (drei Testklassen, kein Gesamtlauf). Typografie, UI-Katalog, Bright-Generator und Diff-Prüfung erfolgreich. Darstellung/Bedienung am Gerät noch beurteilen. [Editor](vtm-map-editor.md).

[Buildnachweis und Download](build-135.md).
