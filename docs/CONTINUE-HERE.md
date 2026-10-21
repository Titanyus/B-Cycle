# Übergabe an einen neuen Chat

Stand: 21.10.2026. Repository `Titanyus/B-Cycle`, Android/Kotlin, native Views.

Diese Datei ersetzt den fehlenden Chatverlauf als Arbeitsübergabe. Sie enthält keine persönlichen Fahrten, RR-Daten, Zugangsdaten oder Signierschlüssel. Zuerst `AGENTS.md` lesen, dann Git-Status und diese Datei prüfen. Uncommittete Änderungen können aus der bisherigen Umgebung stammen: erhalten und vor Weiterarbeit prüfen. Git überträgt nur committete und gepushte Dateien; ein anderer Chat kann lokale Änderungen der alten Umgebung nicht automatisch sehen.

## Aktueller Auftrag

Eine neue signierte, verkleinerte Release-APK nach Abschluss der folgenden Änderungen erstellen; direkt als APK auf der GitHub-Startseite verlinken, kein ZIP als APK-Download. GitHub Actions baut; lokale Umgebung für gezielte Tests/Compile nutzen. Dauerhaften bestehenden Signierschlüssel wiederverwenden. Schlüssel ausschließlich aus sicherem Speicher/Actions-Secrets beziehen, niemals in diese Datei oder öffentliches Git schreiben.

## Entscheidungen und Änderungen im aktuellen Arbeitsstand

- HRV: Ruhephase 15–60 s, Messphase 60–300 s, beide in 15-s-Schritten, Standard 30/180 s. Öffentliche Einstellungen vollständig sichern/wiederherstellen.
- HRV-Artefakte: neue Messkorrektur übernimmt nur bessernde Durchläufe. Mehr als 5 % veränderte Rohintervalle allein blockieren bei vollständig bereinigter Folge nicht mehr; orange Warnung. Restauffälligkeiten, Kontakt-/Empfangsabbruch und technische Fehler bleiben getrennte Ablehnungsgründe. Ältere Ergebnisse nicht rückwirkend verändern. Rohdaten erhalten. Wissenschaftliche Quellen und Grenzen: `hrv-artifact-research.md`.
- Nutzer berichtete nur eine erfolgreiche von sechs Messungen mit H10/Cycplus H2. Fehlgeschlagene Rohmessungen liegen noch nicht zur Prüfung vor; keine Erfolgsquote versprechen.
- HRV-Kreis: groß, Ruhe FOREGROUND/Endpunkt ACCENT, Messung umgekehrt, Endpunkt 1,3 × Linienbreite. Monotones kontinuierliches Fortschreiten. ACCENT-Glow 30 → 10 % in 1,5 s, Neustart im Pulsrhythmus; Visualisierung verändert niemals RR. Phase als kleine Beschriftung im Kreis über Puls, Zeit darunter, bpm wie Dashboardeinheit. Rahmenloser Textbutton im Kreis unter Zeit. Keine übrigen Textinfos während Messung. Detaillierte Zusammenfassung danach, einschließlich Pulsmesser, Protokoll, Zeit-/Frequenzwerten und Qualität. Nur HRV-Mess-/Ergebnisheader in Fensterfarbe.
- HRV-Graph: 60-s-Fenster, erst links füllen, anschließend kontinuierlich verschieben, 10-s-Ticks. Vollständige Daten speichern. LF/HF neu berechnen, Methode/versionierte Parameter speichern und Schätzgrenzen erklären.
- Daten & Speicher: Sticky-Abschnitt HRV-Pulsdaten, manueller ZIP-Export mit einer vollständigen JSON je Messung (auch abgelehnte), Rohpaketen, RR/NN, Korrekturen, Qualitätsgründen und Versionsangaben. Kein automatischer Upload.
- Dreeve: vier Zeilen pro Fahrt (Titel; Datum · km · Zeit; Fahrradname (Typ); GPX-Verfügbarkeit). Oben nur geladene Anzahl, Beschreibung im Info-Dialog. Oberster Importfilter Alle Tracks / Noch nicht importiert. Keine 50-Fahrten-Grenze.
- Dreeve als gefahren importierte GPX behalten bestätigte Dreeve-Verknüpfung und können bewusst als echte FIT-Aktivität zu anderen Diensten übertragen werden. Geplante Tracks/Zusammenfassungen bleiben davon ausgeschlossen. Keine automatische Übertragung beim Import.
- Aktueller Typ des zugeordneten lokalen Fahrradprofils gilt. Nach Profillöschung letzten Typ über stabile UUID erhalten; Slotwiederverwendung darf alte Fahrten nicht neu zuordnen. Originaldateien nicht überschreiben; Übertragung/Sicherung mit aktuellem Typ ableiten. Keine automatische nachträgliche Änderung schon vorhandener Remote-Aktivitäten behaupten.
- Normierte Leistung: reale Bosch-Flow/Dreeve-GPX enthielt 19 stationäre Pausen, insgesamt 26 min. Erkannte Pausen werden aus aktiver Auswertung herausgerechnet, keine globale Lockerung der Power-Qualitätsgrenzen. Lokale Prüfung: NP ca. 155,15 W. Private Originaldatei nicht ins Repo übernehmen.
- ZIP-Import: Auswahlfenster sofort öffnen, während Entpacken mittig Verarbeitungshinweis; danach gleiche Filter/Sortierung/Mehrfachauswahl wie Dreeve. Nur gewählte Fahrten importieren, Originale erhalten, temporäre Daten bereinigen.
- Gemeinsamer Fastscroller für Strecken-/Importlisten: kompakter Infokasten, SECONDARY-Schrift, Themewechsel unterstützen.
- Textscroller: 3 s einblenden, 4 s Halt, 4 s beschleunigen, 18 dp/s, 4 s abbremsen, 4 s Halt, 3 s ausblenden, 2 s leer, Loop. Gemeinsame Konvention: `text-scrollers.md`.

## Abschlusscheck

Diese Übergabe nach Implementierung und Prüfung um tatsächliche Test-/Build-Ergebnisse und den APK-Link ergänzen. Noch offene Punkte ausdrücklich nennen, nicht als erledigt darstellen. `git diff` und relevante Tests sind maßgeblich; diese Liste allein ist kein Umsetzungsnachweis. Sichtbare IN-/OUT-Angaben, Hilfen, Backup und Projektdokumentation bei geänderten Wegen aktualisieren.

Bei jedem zukünftigen größeren Auftrag und vor einem Umgebungswechsel diese Datei aktualisieren: Ziel, Entscheidungen, erledigt/offen, Tests, Build-/Commit-IDs. Keine vollständigen privaten Chatprotokolle ins öffentliche Repository kopieren.

## Nachweis im Arbeitsstand 0.8.17

Implementierung der oben aufgeführten Punkte abgeschlossen. 144 unterschiedliche Android-Hosttests und 19 HRV-Kerntests bestanden (einschließlich 62 Referenzfällen innerhalb der Kerntests). Native HRV-Messansicht gerendert und visuell kontrolliert. Die bereitgestellte private Flow-GPX wurde nur lokal geprüft; keine persönlichen Daten übernommen.

Drei ältere UI-Tests schlagen im breiteren Lauf fehl und wurden am unveränderten Ausgangscommit `734d5e3` identisch reproduziert: `SettingsDesignMigrationTest.fieldDropdownAndNewRowsUseActiveThemeWhileSourceSelectionRemainsFixed`, `SettingsDesignMigrationTest.mainScreensAndAdvancedDialogsRecolorWithoutChangingGeometryOrNavigation`, `SettingsSectionsTest.newSectionsUseOneScrollHostAndNumericRowsStayInlineAcrossThemes`. Veraltete Annahmen zu Dropdownhintergrund, Schriften und Bosch-Abschnitt; in diesem Auftrag nicht korrigiert. Diese Fehler nicht als neue Regression oder als bestandene Tests ausgeben.

Offen für die Praxis: neue H10-/H2-Messungen auswerten, insbesondere zuvor abgelehnte Rohfolgen. Echte WebDAV-/intervals-/Dreeve-/Strava-Konten und physische Bedienung am Telefon sind durch Mock-/Hosttests nicht abgedeckt. Die fünf fehlgeschlagenen Messungen des Nutzers liegen nicht als RR-Rohdaten vor. Kein bestimmter Erfolg der neuen Korrektur zugesichert.

APK-Veröffentlichung folgt nach erfolgreichem CI-Build; Abschlusslink und Buildnachweis werden hier ergänzt.
