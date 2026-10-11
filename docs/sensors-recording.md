# Sensoren & Aufzeichnung

Der bisherige Sensormanager heißt im Raster und Dialog **Sensoren & Aufzeichnung**. GPS, E-Bike, Pulsmesser und Sensor hinzufügen bleiben unverändert bedienbar. Darunter trennt der zentrale Divider ein zunächst geschlossenes Akkordion **FIT-Aufzeichnung** ab.

Beim ersten Öffnen wird die vorhandene FIT-Auswahl als Formular im selben Scrollbereich aufgebaut: Hauptquelle für Geschwindigkeit und Strecke, alle bisherigen Checkboxen einschließlich der fünf Fahrtkennzahlen und ein Speichern-Button. Die bestehenden RecordField-IDs, SharedPreferences und START-Konfiguration bleiben unverändert. Die Auswahl ist zunächst ein Entwurf; Einklappen erhält ihn, **Speichern** übernimmt ihn für die nächste Fahrt. Schließen ohne Speichern verwirft ihn wie bisher Abbrechen im eigenen FIT-Dialog.

Es gibt keinen separaten FIT-Dialog oder zweite Speicherung mehr. Der frühere Platz oben mittig im Einstellungsraster bleibt eine leere, nicht bedienbare Zelle; die anderen Einträge behalten ihre Position. Auch nachträglich aufgebaute Checkboxen und Dropdowns verwenden die zentralen Bicycle-Design-/Theme-Komponenten.
