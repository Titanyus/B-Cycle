# B|CYCLE 0.8.82 – Kartenfarben und Dashboardwechsel

- **Ländliche Kartenflächen unterscheidbar:** Wald deckend grün, Wiesen hellgrün und Felder gelbgrün. Fehlende Regeln für Acker-, Busch- und Feuchtflächen ergänzt. [Bright-Stil](vtm-bright-style.md).
- **Alte Karte gleitet beim Dashboardwechsel mit hinaus:** Android übersprang beim Aufnehmen der alten Seite die vorbereitete GL-Kartenaufnahme. Die bestehende Zeichenroutine wird jetzt auch im übergeordneten Seitenbaum ausgeführt. Keine zweite Karte oder zusätzliche Übergangsaufnahme. [Korrektur und Bildtest](dashboard-page-transition.md).
- **Trenner im Haupt-Einstellungsmenü:** jeweils 12 dp Abstand oben und unten, zuvor jeweils 32 dp.

Gezielte Prüfung: zwei betroffene Vektorkartentests, drei Karten-/Dashboard-Übergangsfälle und ein bestehender Menütest erfolgreich. Der neue Bildtest hat den fehlenden Karteninhalt vor der Korrektur reproduziert. Typografie, Atlas und Diff erfolgreich; kein Gesamtlauf. Robolectric prüft die Android-Zeichenroutine mit vorbereiteten Kartenpixeln; tatsächlichen PixelCopy-/Compositor-Übergang am Smartphone noch bestätigen.

Buildnachweis und Download folgen nach erfolgreichem GitHub-Actions-Lauf.
