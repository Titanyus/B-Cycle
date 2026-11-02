# B|CYCLE 0.8.85 – Kartenfläche beim Dashboardwechsel erhalten

Beim Dashboardwechsel bleibt die einzige Kartenfläche jetzt dauerhaft in ihrem Fenster und Elterncontainer. Die Seiten liefern nur ihre Position und Zielgröße. So entfällt das Umhängen der GLSurfaceView, das deren GL-Kontext verlor und zu ungültigen VTM-Texturverweisen passen würde: rechteckige Ortsnamen, flimmernde/fehlende Kacheln nach dem Wechsel.

Die einlaufende Karte berücksichtigt weiterhin den aktuellen Footer und ihre echten Bedienelementmaße. Die auslaufende Karte bleibt als begrenztes Standbild sichtbar; Abbruch, kartenlose Seiten, Vollbild und Software-Seitenaufnahme erhalten. Keine zweite Karte, keine Änderung der Bright-JSON oder Aufzeichnungen. [Technik und Prüfung](dashboard-page-transition.md).

Vier gezielte Fälle erfolgreich: Vektorkarte mit beiden Footerzuständen und Abbruch/Commit, dabei 0 Fenster-Detach/Attach und gleiche Host-/Layer-/Theme-Identität; kartenlose Vorschau; echte Dashboardintegration; sichtbare Kartenpixel in auslaufender Seitenaufnahme und Bitmapfreigabe. Kompilation/Typografie/Atlas/Diff erfolgreich, kein Gesamtlauf. Echte Geräte-GPU und fehlerfreie Text-/Kacheldarstellung beim Zoomen nach Update noch prüfen.

Buildnachweis und Download folgen nach erfolgreichem GitHub-Actions-Lauf.
