# B|CYCLE 0.8.16

versionCode 86. Optimierter Release über GitHub Actions, signiert mit dem dauerhaften
Schlüssel von Build 53. APK und Prüfsumme werden nach erfolgreicher Prüfung direkt
unter downloads/ veröffentlicht. Für ältere anders signierte Installationen ist
weiterhin kein direktes Update möglich.

## HRV

- Herz-Button rechts neben Start; rechte Dashboard-Buttons etwa 10 % schmaler.
- Vorhandene Pulsmesserverbindung verwenden, 30 s Ruhephase und 180 s Messung.
- Zeitkreis mit Puls/Countdown, Abbruch und RR-Livegraph in den aktiven Themefarben.
- RMSSD, LnRMSSD, SDNN, pNN50 und Ruhepuls; geringe RR-Abdeckung allein führt zu
  einem orangefarbenen Hinweis statt gesperrter Anzeige. Artefakt-/Unterbrechungs-
  und übrige Qualitätsprüfungen bleiben aktiv.
- Ergebnisse, Rohdaten, Korrekturen und Qualitätsangaben dauerhaft personenbezogen
  speichern; gespeicherte Messungen wieder öffnen. Kein automatischer HRV-Cloudupload.
- [Messung, Datenumfang und Qualitätsregeln](hrv-measurement.md).

## Sichern, importieren und darstellen

- Vollständige Verzeichnissicherung lokal oder per konfiguriertem WebDAV: FIT plus
  Metadaten, GPX-Originale, Zusammenfassungen, Einstellungen, Fahrradbilder und HRV.
  Prüfsummen und portable Fahrradidentitäten; vorhandene Daten nicht überschreiben.
- Einstellungssicherung um fehlende öffentliche Profil-/Dienst-/UI-Konfiguration
  ergänzt. Zugangsdaten und Android-Berechtigungen bleiben ausgeschlossen.
- Import-/Export-Accordions erklären ihre Fähigkeiten mit → IN / ← OUT.
  Verbindliche Pflege der Texte und Einstellungen bei künftigen Änderungen.
- Dreeve: neue Fahrten bei der Fahrradzuordnung als Importiert oder Gefahren übernehmen,
  auch ohne Gear-ID und bei Zusammenfassungen. Duplikate behalten ihre Herkunft.
- NP und Performance aus importierten Messwertverläufen gezielt verifiziert;
  Zusammenfassungen ohne Verlauf erhalten keine erfundenen Kennzahlen.
- Titellaufschrift zeigt auch das ursprünglich unsichtbare Titelende, beschleunigt
  und bremst weich, wartet am Ende und blendet vor dem Neustart aus.

106 gezielte Hosttests und 12 HRV-Kerntests bestanden. Die Kernprüfungen enthalten
62 gepinnte NeuroKit2-Referenzfälle. Die Hosttests prüfen unter anderem
Messablauf, BLE auf Android 13/15/16, Speicherung, Sicherung, Import und Darstellung.
Echte Gurt-/Telefonprüfung und persönlicher WebDAV-Server bleiben praktisch zu prüfen.

**[Geprüfte APK direkt herunterladen – Build 55](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.16-release-55.apk)**

[Buildnachweis, Größe, SHA-256 und Signatur](build-55.md).
