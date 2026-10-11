# Kleine Altlastenbereinigung – 0.7.0

Vergleichsbasis und Rückkehrpunkt: Meilenstein `v0.6.21`.

- `AppVersion` bezieht Name und Buildnummer aus dem generierten Gradle-BuildConfig.
  Diagnosekopf, Diagnosedateiname und Karten-User-Agent verwenden diese Quelle
  statt der alten festen 0.6.11-Angaben.
- FIT `device_info.software_version` übernimmt Major/Minor (0.70 für 0.7.0),
  gemäß Garmin uint16 mit Skalierung 100. Die eindeutige Buildnummer steht in
  `developer_data_id.application_version` (uint32, 38 für diesen Build).
  Die feste, veraltete 0.4 bzw. 401 wurde entfernt. Sensormesswerte, FIT-Felderauswahl,
  Journalformat und Auswerteberechnungen bleiben unverändert.
- `RouteActionIcon` war ohne Aufrufer; `RouteSymbolDrawable` war nur noch in einem
  Komponenten-Test verwendet. Beide Renderer entfernt; der Komponenten-Test prüft
  jetzt den produktiv verwendeten zentralen Lucide-Renderer und seine Theme-Anbindung.
  Die bewusst aufbewahrten früheren Einstellungsicons bleiben bestehen.
- Keine neuen Bibliotheken, keine Änderungen an Architektur, Sensoren, Karten-Cache,
  Aufzeichnungslifecycle oder Bedienabläufen.

R8 entfernte die alten Renderer bereits aus Release. Die Bereinigung vereinfacht
primär den Quellcode; daraus wird keine messbare Akku- oder Laufzeitverbesserung behauptet.
Die übrigen Profiling-Kandidaten aus der Bestandsaufnahme sind weiterhin offen.
