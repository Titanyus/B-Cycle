# B|CYCLE 0.8.84 – Kartenhintergrund, Dashboardwechsel und Profileinstellungen

- **Heller Bright-Hintergrund statt Standardgrau:** Die Hintergrundfarbe der unveränderten JSON wird jetzt an den tatsächlichen GL-Renderer übergeben und beim Wiederaufnehmen der Karte wiederhergestellt. Das korrigiert ungefüllte und transparent überlagerte Flächen. [Hintergrundkorrektur](vtm-bright-style.md).
- **Footer beim Dashboardwechsel:** Bereits die einlaufende Seite verwendet die einzige reale Karte in ihrer Zielgröße einschließlich nativer Bedienelemente. Die auslaufende Karte bleibt als begrenztes Standbild sichtbar. Footer-Zustand beim Loslassen bewahren; Abbruch und verspätete Antworten abgesichert. [Übergang](dashboard-page-transition.md).
- **Fahrradprofile → Aufzeichnung:** gemeinsamer Sticky-Abschnitt statt FIT-/Strecken-Aufzeichnung. Fahrt benennen, Höhenkorrektur aus Karte, Rahmen-Accordion für Sensor-Hauptwerte mit sechs Quellenwahlen, Rahmen-Accordion für zu speichernde Daten mit bisherigen Speichergruppen/Metadaten. Bestehende Werte/Keys/Locks beibehalten.
- **Dropdown-Abstände:** einheitlicher kleiner Abstand von 8 dp; zusätzliche unsichtbare Sensortrenner und doppelte Quellenzeilen-Paddings entfernt. [Profilanordnung](profile-recording-settings.md).
- **Trainingshilfe:** neuer Erklärungstext nach Nutzervorgabe; Zonenberechnung und Farblogik unverändert.

Acht eindeutige gezielte Fälle erfolgreich: zwei Hintergrund-/Quellenwechselprüfungen, vier Übergangs-/Abbruchfälle, zwei Profil-/Quellenwahl-/Sicherungsprüfungen. Nach Ergänzungen nur betroffene/offene Fälle wiederholt. Textänderung separat per Ressourcenprüfung validiert. Generator-Reproduzierbarkeit, Kompilation, Typografie/Atlas/Diff erfolgreich; kein Gesamtlauf. Optischen Abgleich der Karte und echten GL-Neuanschluss/Übergang am Smartphone noch prüfen.

Buildnachweis und Download folgen nach erfolgreichem GitHub-Actions-Lauf.
