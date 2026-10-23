# Optionale FIT-Metadaten

Unter Profil → Strecken-Aufzeichnung steht als erster Eintrag vor den anderen Aufzeichnungsgruppen das Accordion **Metadaten**. Sechs native Schalter zeigen `Bezeichnung (aktueller Profilwert)`, etwa `Fahrradname (Matrix ICR50)`. Alle sechs sind standardmäßig aktiv. Bei einer Umbenennung aktualisiert sich der angezeigte Profilname auch im offenen Formular. Die Auswahl gilt pro Profil für neue Aufnahmen; vorhandene Dateien werden nicht nachträglich verändert.

| Auswahl | Garmin-Nachricht/Feld | Wert |
| --- | --- | --- |
| Fahrradname | bike_profile (6), name (0) | Name des lokalen Fahrradprofils |
| Sportart im Fahrradprofil | bike_profile.sport (1) | cycling (2) |
| Fahrradtyp im Fahrradprofil | bike_profile.sub_sport (2) | generic (0), indoor_cycling (6), e_bike_fitness (28) |
| Profil-ID | bike_profile.id (14) | stabiler lokaler Profilslot, uint8, 0–254 |
| Profilindex | bike_profile.message_index (254) | stabiler lokaler Profilslot ohne Flags, 0–4095 |
| Aufzeichnendes Gerät | device_info (23), product_name (27) | B\|CYCLE, samt vorhandener Versions-/Creatorinfo |

`bike_profile` ist eine Standardnachricht aus Garmins FIT-Einstellungsprofil; Garmin-Decoder können sie auch in unseren Aktivitätsdateien lesen. Andere Dienste müssen sie nicht auswerten. Die numerischen Profilkennungen sind keine dienstübergreifende UUID. Unsere Profilslots werden nicht wiederverwendet und mitgesichert. Liegt ein Slot außerhalb der FIT-Feldgrenze, wird das betreffende Feld ausgelassen, statt eine Kollision durch Kürzen/Modulo zu erzeugen. Unsere vollständige Fahrrad-UUID bleibt unabhängig davon in den vorhandenen App-/Backup-Metadaten.

Sportart/Subsport in den eigentlichen Aktivitätsnachrichten (`session`, `lap`, `sport`) bleiben erhalten; die beiden Schalter betreffen zusätzliche `bike_profile`-Angaben. Auch die notwendige FIT-Dateiidentifikation bleibt erhalten, wenn die optionale Gerätebeschreibung abgewählt wird. Keine Garmin-Hardware, Seriennummer oder fremde Herstellerkennung erfinden: Geräteinfo identifiziert die erzeugende App B|CYCLE.

Die Metadatenauswahl liegt separat von Messfeld-IDs und Developer-Indizes in `RecordingConfig.metadata`, dem Profil-JSON und `recordMetadata`. Alte Profile/Sicherungen ohne die optionalen Schlüssel bleiben lesbar und verwenden die Standardauswahl. Die Einstellungs-/Profilsicherung enthält aktive und inaktive Profile samt Auswahl; unbekannte/mehrfache Metadaten-Schlüssel werden bei der Sicherungsplanung abgewiesen.

`RecordingStore.begin` hält Name, Typ, lokalen Slot und Auswahl im vorhandenen privaten `.profile.json` fest. Der Encoder verwendet diesen Snapshot auch bei späterem Profilwechsel/Umbenennung/Löschen oder Wiederanlauf, nicht das aktuell aktive Profil. Das binäre RideJournal-Format bleibt unverändert. Export-FIT und interne vollständige Analyse-FIT erhalten dieselbe Metadatenauswahl. Für alte Journale ohne Profil-Snapshot wird kein Fahrrad geraten; die bisherige Geräteinfo bleibt kompatibel.

Bei einem späteren profilabhängigen Radtyp-Overlay wird `bike_profile.sub_sport` mit den Aktivitätsfeldern aktualisiert, sofern dieses optionale Feld bereits vorhanden ist. Abgewählte Felder werden dadurch nicht ergänzt. Namen und lokale Kennungen bleiben Aufnahme-Snapshots. Messwerte, FIT-Feldauswahl und Rohdaten ändern sich nicht. Fremde importierte Originale werden durch die neue Einstellung nicht umgeschrieben oder als eigene B|CYCLE-Aufnahmen ausgegeben.

## Prüfung
17 gezielte Tests erfolgreich: Garmin-FitDecoder und ActivityFileValidationPlugin prüfen Bike/E-Bike/Indoor, Unicode-Namen, numerische Kennungen, jede einzelne Abwahl und vollständig deaktivierte Metadaten. Weitere Prüfungen decken vorhandene Bike-Typ-Overlays, Snapshot-Erhalt nach Profilwechsel/Löschen, unabhängige Profile, alte/neue Sicherungen und die echte Indoor-FIT-Einstellungsansicht ab. Native Beschriftungen/Schalter in drei Hochformatbreiten einschließlich Umbenennung im offenen Formular geprüft. Bestehende FIT-/Indoor- und Profilregressionen erfolgreich; kein Gesamttestlauf, keine Dreeve-/Garmin-Connect-Importprüfung und keine APK erstellt.

Stand 23.10.2026: die beschriebenen neuen Funktionen sind in APK **0.8.31 / Build 70** enthalten. [Buildnachweis](build-70.md). Frühere Angaben zu fehlender APK sind Verlauf.

Nach Build 70: Alle Datenfeld-Auswahlen in den Dropdowns der Kategorie Strecken-Aufzeichnung verwenden ebenfalls native Schalter statt Checkboxen. Feld-IDs, Vorauswahl, automatische Speicherung und profilabhängige Filter bleiben erhalten. Metadaten ist das erste Dropdown innerhalb dieser Kategorie.
