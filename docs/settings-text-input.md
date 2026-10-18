# Android-Tastatur in Einstellungen – 0.8.3

## Ursache und Korrektur

Daten & Speicher öffnet den Dialog vor der Installation der inline Dienstformulare. Androids AlertController sieht anfangs keine Texteingabe und setzt FLAG_ALT_FOCUSABLE_IM. Später angefügte EditTexts sind zwar bedienbar, ihr Fenster bleibt aber von der IME getrennt. Das Fahrerprofil enthält seine Felder bereits vor show() und ist deshalb nicht auf dieselbe Weise betroffen.

BicycleDialogBuilder hebt die IME-Sperre bei jedem OnShow zentral auf, sowohl für Vollbild- als auch kompakte Dialoge. Das umfasst Rückkehr von Hilfen/Unterdialogen. Kein automatisches Öffnen der Tastatur und keine fachlichen Änderungen an Speicherung oder Uploads. Bestehende IME-Inset-Behandlung und ADJUST_RESIZE bleiben erhalten.

## Inventar und gezielte Prüfung

| Bereich | Eingaben |
| --- | --- |
| WebDAV | Server, Benutzername, Passwort, Ordner |
| intervals.icu | Athletenkennung, API-Schlüssel |
| Dreeve | Adresse, API-Schlüssel |
| Strava | Client-ID, Client-Secret |
| Fahrer | Größe, Gewicht, Ruhepuls, Maximalpuls, Schwellenpuls, FTP |
| Profile | Fahrradname, Radumfang |
| Karten | Thunderforest-Schlüssel |
| Umbenennen | Dashboardname, Streckenname |

Alle EditText-Erzeugungsstellen geprüft. Geburtsdatum ist eine bewusste Kalenderauswahl ohne Tastatur; Schalter und Dropdowns sind keine Texteingaben. Modellspezifisch deaktivierte Pulsfelder bleiben deaktiviert.

Regressionstests prüfen Fensterflags, Tastaturfreigabe pro Feld, native Editor-Eigenschaft und InputConnection sowie Fokus. Die Cloudregression scheitert mit dem ursprünglichen Fensterverhalten und besteht mit der Korrektur. Zusätzlich nachträglich eingefügte Editoren in kompakten/Vollbilddialogen und Rückkehr vom Kinddialog geprüft. Ein echter Android-IME-/Gerätetest steht aus; Robolectric zeigt keine physische Bildschirmtastatur.
