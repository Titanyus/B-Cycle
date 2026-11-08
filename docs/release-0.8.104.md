# B|CYCLE 0.8.104

- Eigene FIT-Kopien mit RPE/Notiz bleiben beim Downloads-Scan und Wiederimport der Originalfahrt zugeordnet. Der nachgewiesene Doppelimport trotz nur einer lokalen Datei ist korrigiert; ältere bereits angelegte Duplikate bleiben erhalten. Keine Änderung an Originaldateien oder Cloudqueues.
- Fahrtzusammenfassung: unterer Bereich ab der früheren Trennerposition vollständig TERTIARY, ohne Trennlinie; Abstände und Speicherbedienung erhalten.
- Bosch-Autostart wartet auf Ladeinformationen und sperrt bei angeschlossenem Ladegerät/aktivem Laden. Bei unbekanntem Ladezustand erst beim Anrollen, nach Laden erst bei neuer Radbewegung. Zusätzliche Kontrolle beim Dienststart. Manueller Start und Indoor bleiben erhalten. Unterstützung der Ladeadressen am Nutzerrad noch bestätigen; der Anroll-Fallback gilt auch ohne verfügbare Ladeinformation.
- Dashboard-Kartenbuttons deckend TERTIARY, inaktive Symbole statt gesamter Fläche dimmen. Nordpfeil der Windrose PRIMARY.
- Trainingsvarianten mit eigenem Touchbereich und den Labels Quick+, Quick, Std, Ext, Ext+. Vertikales Scrollen bleibt möglich; Std ohne erkannte Varianten erklärt die fehlende Auswahl und öffnet keine Details. Trainingsdetails zeigen nur die Übersicht, Originaldaten bleiben gespeichert.
- Kompakter Karteneditor: Innenfläche links, Straßen-Outline rechts, zwei Farbfelder, unbeschriftete farbige RGBA-Regler und rund20% kleinere Abstände. Outlinefarbe, Deckkraft und tatsächliche Randstärke unabhängig; JSON, Profile, Sicherung und direkter GL-Farbpfad vollständig erweitert. Keine Änderung an der Bright-Basis oder der Belagsstrichelung.

Nur gezielte Speicher-, Protokoll-, Autostart-, Touch-, Darstellungs- und Editorprüfungen, keine Gesamttests/Profiling. Typografie/UI-Katalog/Bright/Diff grün, Vorschauen kontrolliert. [Exportidentität](fit-feedback-export-identity.md), [Ladeschutz und Quellen](bosch-charging-autostart.md), [Karteneditor](vtm-map-editor.md), [Training](training-library.md).
