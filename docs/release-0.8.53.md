# B|CYCLE 0.8.53

Enthält alle Änderungen seit APK 0.8.52 / Build 92.

- Backup & Cloud mit getrennten Sicherungen und Wiederherstellungen für Einstellungen, Strecken und HRV, jeweils lokal oder über WebDAV. Einstellungen immer vollständig und passwortgeschützt; HRV mit letzter gültiger Tagesmessung und Tagesbefinden. Keine Gesamtsicherungen mehr; alte Gesamt-/HRV-Exporte müssen nicht eingelesen werden können. HRV4Training-CSV bleibt erhalten.
- Cloud-Bereich mit kompakten Transferangaben, Nextcloud-Symbol und Passwortauge innerhalb des Eingabefelds. Streckenimport hier vorerst deaktiviert. Einstellungsmenü neu gruppiert; Fahrradprofile und Backup & Cloud umbenannt.
- Kompakte Wartungszeilen und wiederverwendbarer Dropdown-Rahmen, drehendes Chevron, Spezial-Rahmen-Pillen für weiterhin deaktivierte Wartungsaktionen.
- Importierte FIT-Teilrecords werden zusammengeführt. GPX-Bewegungsschätzung berücksichtigt wiederholte Positionen und Zeitjitter; vorhandene native Messwerte haben Vorrang. Abgeleitete Metadaten werden aktualisiert, Originaldateien und Verknüpfungen bleiben erhalten. GPS-basierte Geschwindigkeit bleibt eine Schätzung, keine Rekonstruktion verlorener Messgenauigkeit.
- Fast-Scroller im Streckenmanager korrigiert; Dreeve-Import mit modalem Fortschritt und Abbrechen. Glow-Sticky-Header fahren bündig an den Hauptheader, anfänglicher Abstand bleibt; Wartungsabruf unten, Trenner bei der Fahrradzuordnung.

Gezielte Prüfungen während der Umsetzung: Importauswertung/Metadaten, 13 Import-/Scrollerfälle, zehn Wartungs-/Dropdownfälle, Einstellungsmenü und Passwortfeld, 32 Backup-/Navigation-/Transportfälle. Relevante Tests erfolgreich; keine unnötige Gesamtsuite für den APK-Auftrag wiederholt. WebDAV über simulierten TLS-Server, private FIT-/GPX-Beispiele ausschließlich lokal untersucht. Kein erneuter Test am Android-Gerät oder privaten Cloudserver. Keine Änderungen an den Originaldateien und keine privaten Daten veröffentlicht.

Buildnachweis folgt nach Veröffentlichung.
