# Manuelle Tages-/Nachtkarte

Der Mond-Button unter dem Vollbild-Button schaltet zur Nachtkarte; die Sonne schaltet zurück. Zusammen mit Zentrieren, Ausrichten und Vollbild entsteht eine L-Anordnung. Die Auswahl bleibt lokal gespeichert und gilt unabhängig vom App-Farbthema. Der Button folgt dem gemeinsamen Ein-/Ausblenden während der Fahrt.

Nur die Rasterkarte erhält eine Nachtpalette: dunkler Boden, gedämpftes Grün, dunkles Wasser und helle Straßen/Beschriftungen. Die GPX-Linie wird nachts heller blau, bei unveränderter Breite und 50 % Deckkraft. Dashboard-Datenfelder und übrige Kartenebenen behalten ihre Darstellung.

Die Farbzuordnung erfolgt aus den vorhandenen Rasterpixeln, nicht aus semantischen Kartendaten. Daher ist sie eine Annäherung, kein offizielles Nacht-Theme des Kartenanbieters. Bestehende Quellen, Downloads, Cache-Laufzeiten und Originalkacheln bleiben erhalten. Ein begrenzter LRU-Cache hält abgeleitete Nachtkacheln; Umrechnung erfolgt im Karten-Layer-Thread, mit wiederverwendetem Pixelpuffer. Beim Rückwechsel und Schließen werden die abgeleiteten Kacheln freigegeben. Keine neue Bibliothek.

Tests prüfen Farbklassen/Transparenz, unveränderte Originalkacheln, Cache-Begrenzung und Freigabe, persistente Umschaltung, Button-Geometrie und gemeinsames Ausblenden sowie unveränderte GPX-Geometrie. Darstellung und Flüssigkeit auf dem echten Telefon sind anschließend zu prüfen.
