# Kartenbedienung, Textstile und Stilverwaltung nach Build152

Nutzerauftrag: Straßenbeschriftung wie Ortsnamen editieren, Hausnummern schaltbar/editierbar, Ebenen nur auf der Karte, Entwicklerbuttons hinter2s-Haltegeste, normale Kartenquellenwahl neben Auge mit Cycling-Schaltern, Stile löschen/sortieren. Anschließend ausdrücklich neue APK beauftragt.

## Beschriftung

ROAD_TEXT jetzt wie PLACE_TEXT mit eigenem outline-Block: links Füll-RGBA, rechts Kontur-RGBA plus25–300% relative Stärke. Vorhandene Straßenkonturen ohne Override unverändert; Ortsnamen behalten weiterhin ihre doppelten weißen Standardkonturen. Änderungen an Textkonturen invalidieren native Text-/Atlasaufbereitung, keine bloße GL-Farbänderung.

HOUSE_TEXT und HOUSE_NUMBERS ergänzen die bestehenden Ziel-/Ebenen-Enums am Ende. Originale OpenMapTiles-housenumber-Features ab VTM17 als Caption mit k=housenumber, standardmäßig aus. Keine Nummern erzeugen oder aus Namen ableiten. Eigene Füllung/Kontur/Alpha/Stärke, Standard11px/#555555 mit weißer1,2-Kontur; niedrige Priorität hinter Straßen-/Ortsnamen. Werte in bestehendem Stil-JSON/benannten Stilen/Kartensicherung, Ebenenschalter in MapLayersPopup. Runtime-Instruktion zusätzlich zur unveränderten generierten Bright-Basis; keine Änderung der Referenzdatei oder Sprites.

## Bedienung

MapSourcePopup neben Auge, derselbe MapSourcePicker wie Einstellungen mit Raster-/Vektorkategorien und gespeicherten Stilnamen. Darunter horizontaler Trenner und getrennte Schalter für Radwege/Radrouten. Kartenquellen-/Stilwechsel und Fahrradprofilprüfung erhalten. Thunderforest ohne Schlüssel meldet fehlende Konfiguration und setzt Auswahl zurück. Auswahl der Ebenen aus Einstellungen entfernt; Cycling-Stil-/Trackeinstellungen bleiben dort erhalten.

Layers/Editor zunächst verborgen. DOWN auf Quellenbutton startet2000ms-Termin, erst danach beide Buttons rechts daneben freigeben. MOVE über Touchslop/CANCEL/Settings/MapPause verwirft den Termin, Haltegeste löst keinen anschließenden Quellenklick aus. Freigabe nur innerhalb der Aktivität, bleibt über normale Karten-/Dashboardbedienung erhalten; showMenu blendet Entwicklerbuttons wieder aus. Bestehende automatische Sichtbarkeit der übrigen Kartencontrols gilt weiter. Geometrie und Reservierung des oberen rechten Controls berücksichtigen zusätzliche linke Buttons; kein Umhängen der MapView/GL-Lebenszyklusänderung.

## Stilverwaltung

Speicherdialog mit sichtbaren Aktionen ↑/↓/Löschen für ausgewählten gespeicherten Snapshot. Reine Original-Bright-Vorlage und Neuer Stil haben keine löschbare Sammlungsversion. Gespeicherter Bright-Snapshot kann wie eigene Stile sortiert/gelöscht werden. Gemeinsame Liste behält Arrayreihenfolge in Save-/Quellendropdown und Sicherung; UUIDs/Profilbezüge unverändert. Nach Bewegung ausgewähltes Ziel weiter vorauswählen. Löschen entfernt den Snapshot und gegebenenfalls aktive ID, erhält Arbeitskopie und aktuelle Karte. Andere Profil-Arbeitskopien unverändert. Dialog scrollbar, damit Aktionen bei wenig Platz zugänglich bleiben.

## Prüfung

Neun gezielte Methoden im abschließenden Lauf grün: MapControlAccessTest4 plus MapStyleLibraryTest5. Native Straßen-/Hausnummern-Paints und Konturen, Sichtbarkeit/JSON, echte2s-Geste inklusive Abbruch/Settings, entfernte Einstellungs-Ebenen, echte Quellenpopup-Cycling-Schalter, sichtbare Dialogverwaltung/Reihenfolge/Löschen, bisherige Save-/Bright-/Profil-/Backup-/Entwurfsfälle. Log map-access-complete-tests.log. Vorherige Läufe map-access-labels-tests.log und map-access-labels-final-tests.log: eine bisherige Testannahme über den nun ausdrücklich entfernten Einstellungs-Schalter auf die gespeicherte Sichtbarkeit und fehlendes UI aktualisiert. Zusätzlicher management-Lauf map-access-management-tests.log grün. Compilefehler zweier neuer Deklarationen/Resource-Referenzen vor der ersten Testausführung korrigiert.

Typografie/UI-Katalog/Bright/Diff grün. Keine Gesamtsuite, kein Profiling, Sensoren/FIT/Rohdaten unverändert. Tatsächliche Hausnummerndichte hängt von den vorhandenen Vektorkacheln ab; optischer und Gestenabgleich am Gerät bleibt Abschlusskontrolle.
