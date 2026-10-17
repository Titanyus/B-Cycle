# Benannte Dashboards und spätere Fahrrad-Zuordnung

DashboardPages verwaltet 1–6 Seiten mit Namen und stabilen internen Slots. Bestehende vier Seiten und ihre Feld-, Graph-, Sichtbarkeits- und Visualisierungseinstellungen bleiben erhalten. Beim Löschen werden keine übrigen Seiten umnummeriert. Neue Seiten verwenden einen freien Slot mit frischen Standardwerten. Die zehn DashboardTemplate-Geometrien bleiben davon unabhängig. Die Einstellungssicherung enthält Katalog, Namen, aktive Seite und sämtliche vorhandenen Seiten; alte Vier-Seiten-Sicherungen bleiben lesbar.

Die Zuordnung zum Fahrrad ist ausdrücklich **vorbereitet, noch nicht aktiviert**. BikeUiConfiguration und BikeUiConfigurationScope definieren den versionierten zukünftigen Umfang:

- Design & UI vollständig: Dashboard-Katalog und aktive Seite, Layouts, Datenfelder, Graphen, Visualisierungen, Karten-Datenfelder, Farbtheme einschließlich Custom-Theme-Konfiguration, Vollbild sowie Fahrmodus-Anzeige und deren Position.
- Karten vollständig: Anbieter-/Kartenvorwahl, Tag/Nacht, Ausrichtung, Folgen, Streckenanzeigen, Zoom, Kartenposition und Höhenkorrektur. FIT-/GPX-Kartenauswahl später als stabile Bibliotheksreferenz übernehmen, niemals Originalstrecken pro Fahrrad duplizieren. GPX-Auswahl liegt aktuell im Bibliotheksindex und muss bei Aktivierung zusätzlich erfasst werden.

Es werden noch keine Fahrrad-Snapshots gespeichert oder beim Wechsel eingespielt. Deshalb ändert ein Fahrradwechsel aktuell weder Farbtheme noch Karten- oder Dashboard-Einstellungen. Beim späteren Aktivieren vorhandene globale Einstellungen als Startkonfiguration übernehmen, jede neue Option dieser beiden Einstellungsseiten im zentralen Umfang ergänzen, Snapshot über die dauerhafte Bike-Slot-ID zuordnen und beim Löschen bereinigen. Bike-IDs werden nicht wiederverwendet. Konten, Tokens und Anbieter-Zugangsdaten bleiben in ihren geschützten globalen Stores; die Kartenvorwahl wird dagegen fahrradbezogen.

Aufnahmebutton: Fahrradname und Radtyp-Icon mit Record-Symbol, während einer Aufnahme Stop-Symbol. Dieselbe native Long-Press-Erkennung wie beim Dashboard-Editor öffnet die Bike-Auswahl. Kein Start durch Long-Press; Bike-Wechsel bei Aufnahme/Speichern gesperrt.

Pro Kachel kann der vorhandene Trendindikator direkt unter dem Quellen-Dropdown abgeschaltet werden. Die Auswahl gehört zu DashboardTrendIndicators und damit zum zentralen Seiten-/Bike-Umfang. Fehlende Konfiguration und ältere Backups entsprechen aktiviert. Trendhistorie und -berechnung laufen unverändert weiter, auch bei ausgeblendetem Chevron; keine neuen Felder mit Trends versehen.

Die Bike-Auswahl am Aufnahmebutton enthält jetzt direkt die BikeProfileStrip-Kacheln im kompakten Modus, keine Textliste. Alle sechs möglichen Räder passen in eine Reihe; Radtyp, Kategorie-Vorschaubild und zweizeiliger Name verwenden dieselbe Halbierung der Manager-Geometrie. Nur vorhandene Räder, kein Anlegen/Umsortieren im Popup. Auswahl schließt das Popup; Appwechsel/Beenden ebenfalls. Die normale Bike-Verwaltung bleibt bei drei sichtbaren Kacheln.

Das Layout-Dropdown öffnet wieder ein dreispaltiges Kartenraster mit den zehn großen DashboardPreview-Vorschauen anstelle einer Textliste. Die bisherige Rastergeometrie ist zentral wiederverwendet; auf kurzen Displays ist das Dropdown scrollbar. Auswahl schließt nur das Layout-Popup und speichert unverändert beim gewählten benannten Dashboard.
