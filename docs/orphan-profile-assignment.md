# Verwaiste gefahrene Fahrten einem Profil zuordnen

Einstellungen → Profile zeigt „Profil zuordnen“ unter den Radprofil-Kacheln nur bei vorhandenen als Gefahren gespeicherten Fahrten ohne aktuelles Profil. Beim Öffnen des Streckenmanagers erscheint derselbe Dialog automatisch, sobald die Bibliothek geladen ist. Einmal pro Manageröffnung; Zurück/Dismiss unterdrückt die Wiederholung in dieser Öffnung. Beim erneuten Öffnen wird erneut angeboten, wenn verwaiste Fahrten bleiben.

## Auswahl

OrphanRideProfiles gruppiert ausschließlich TrackOrigin.RECORDED anhand der gespeicherten alten bikeId/UUID. Gelöschte Profile mit gleichem Namen bleiben getrennt; ohne frühere Kennung eine eigene Gruppe. IMPORTED und UNKNOWN bleiben ausgeschlossen. Es gelten die lokal vorhandenen Bibliothekseinträge, keine Metadatenreste bereits gelöschter Fahrten.

Accordionüberschrift: früherer Profilname und Anzahl. Inhalt: nur Fahrtnamen, darunter Zielprofil-Dropdown und „Zuordnen“. Ein einziges gleichnamiges aktuelles Profil desselben Typs darf vorausgewählt werden. Namen/Slots bewirken nie selbst eine Neuzuordnung. Mehrdeutigkeit und fehlende Kennung bleiben ohne Vorschlag. Ein abweichender Radtyp wird vor dem Bestätigungsbutton sichtbar benannt; nach Zuordnung bestimmt das aktuelle Zielprofil den Typ. Geöffnete Gruppen/Zielwahl der verbleibenden Gruppen bleiben bei Fortschritt erhalten.

Nach Erfolg verschwindet die Gruppe, bei letzter Gruppe schließt der Dialog; dann verschwindet auch der Einstieg in Profile. Schließen und reine Dropdownauswahl ändern keine Daten.

## Speicherung und Schutz

MapState koordiniert die Änderung auf seinem vorhandenen einzelnen Bibliotheksworker. Gruppenzusammensetzung, Herkunft und Existenz, unveränderte Metadaten sowie stabile Ziel-UUID/Typ vor Übernahme prüfen. Aufnahme, Speichern und aktive HRV-Messung sperren die Übernahme; bestehende Bibliotheksoperationen ebenfalls. Dismiss einer bereits bestätigten Operation hebt den ausdrücklichen Auftrag nicht auf; späte UI-Rückmeldung wird nur in derselben noch offenen Ansicht verarbeitet.

RideProfileColor übernimmt bestehende Metadaten in einer einzigen SharedPreferences-Transaktion pro Gruppe; Slot, UUID, Profilname, Typ und Profilfarbe des Ziels. Unbekannte zusätzliche Metadatenfelder bleiben. Keine Änderung von Fahrt-IDs, Titeln/RideNames, FIT-/GPX-/JSON-Originalbytes, Cloud-Links, Quittungen oder Uploadjobs. Vorhandene Sicherung sichert die neue UUID durch denselben bestehenden Metadatensatz; keine neuen Sicherungsfelder. Library-Revision/Profilkatalog invalidieren, damit Filter/Details/Buttons sofort folgen. Aktuelle Profile werden nicht anhand wiederverwendeter Slots ersetzt.

Automatisches Managerangebot bleibt ein rein lokaler Hinweis, ohne Cloud-Abfrage und ohne Upload. Normale importierte Fahrten lösen keinen Hinweis aus. Detektion in MapState anhand des bestehenden Entries-/Profilkatalog-Caches wiederverwenden, statt beim Sensortick jede Fahrt neu zu parsen.

## Prüfung

15 gezielte Tests erfolgreich: neue Gruppierung mit gleichnamigen gelöschten Profilen/fehlenden Kennungen/mehrdeutigen Vorschlägen, echte Bibliothekszuordnung mit Filter- und Sicherungsmetadaten/Originalhashes/Fahrtnamen/Dreeve-Link, gesperrter/staler Auftrag und verschwundenes Ziel sowie beide echten UI-Einstiege/Typwechselwarnung/Dismiss ohne Wiederholung/verschwindender Button. Dazu bisherige dynamische Profile, Profil-Löschung und Filter. Zusätzliche UI-Nachprüfung nach Foreground-Schutz erfolgreich. Typografie- und Diffprüfung erfolgreich. Kein Gerätetest, kein Gesamtlauf und keine neue APK.


26.10.2026: Der kontextuelle Button „Profil zuordnen“ steht jetzt im Profil-Aktionsfooter direkt links neben „Löschen“, statt bei den Profilkacheln. Sichtbarkeit, Sperren und gemeinsamer Zuordnungsdialog unverändert.
