# B|CYCLE 0.8.17

HRV-Messungen erhalten eine robustere Artefaktkorrektur und eine klarere Ergebnisdarstellung. Verschlechternde Korrekturdurchläufe werden verworfen. Mehr als 5 % veränderte Rohintervalle allein verhindern bei vollständig bereinigten Daten keine Anzeige mehr; ein orangefarbener Hinweis erklärt die mögliche Verzerrung. Verbleibende Auffälligkeiten und Empfangs-/Speicherfehler bleiben getrennte Ablehnungsgründe. Die tatsächliche Verbesserung mit H10/Cycplus H2 muss anhand neuer Messungen geprüft werden.

- Einstellbare Ruhe-/Messzeit: 15–60 s bzw. 1–5 min in 15-s-Schritten, Standard 30/180 s; in der Einstellungssicherung enthalten.
- Größerer Messkreis mit kontinuierlichem Fortschritt, Phasenlabel über dem Puls, bpm wie Dashboardeinheit und schlichtem Textbutton im Kreis. Ruhe FOREGROUND, Messung ACCENT, kontrastierender Endpunkt und pulsierender ACCENT-Glow. Während Messung keine weiteren Infotexte.
- RR-Graph als gleitendes Einminutenfenster mit 10-s-Ticks. Vollständige Daten bleiben gespeichert.
- Ergebnis mit Zeitbereich, LF/HF-Spektralschätzung, Messqualität, Sensor und Protokoll. Nur HRV-Mess-/Ergebnisheader in Fensterfarbe. Fünf Minuten für Frequenzvergleiche empfehlen; LF/HF ist kein direkter autonomer Balancewert.
- Daten & Speicher → HRV-Pulsdaten: manueller ZIP-Export mit einer vollständigen JSON je gespeicherter Messung, auch abgelehnte; Rohpakete, RR/NN, Korrekturen, Qualitätsgründe und Versionen. Kein automatischer Upload.
- Dreeve: klare vierzeilige Fahrtenliste, Beschreibung in der Hilfe, Filter Alle Tracks / Noch nicht importiert und gemeinsamer kompakter Fastscroller mit SECONDARY-Schrift.
- ZIP-Import öffnet sofort dieselbe Auswahl mit Verarbeitungshinweis. Nach Hintergrundprüfung filtern, sortieren und gezielt übernehmen; Duplikate und Teilfehler erhalten.
- Als gefahren importierte GPX behalten den Dreeve-Link und lassen sich mit vollständigen Zeitstempeln manuell als FIT-Aktivität übertragen, beispielsweise zu intervals.icu. Geplante Tracks/Zusammenfassungen bleiben ausgeschlossen.
- Aktueller Profil-Radtyp gilt für Darstellung, Sicherung und neu abgeleitete FIT-Übertragung. Nach Profillöschung letzten Typ erhalten; keine Neuzuordnung durch wiederverwendete Slots. Originaldateien bleiben erhalten; vorhandene Remote-Aktivitäten werden nicht automatisch umklassifiziert.
- Stationäre Bosch-Flow-GPX-Pausen aus aktiver Leistungszeit herausrechnen. Qualitätsgrenzen für normierte Leistung bleiben erhalten. Lokale Prüfung der gemeldeten Fahrt: NP ca. 155,15 W, 26 min erkannte Pausen.
- Vollständige Titellaufschrift mit dokumentiertem Ablauf: 3 s einblenden, 4 s Halt, 4 s beschleunigen, 18 dp/s, 4 s abbremsen, 4 s Halt, 3 s ausblenden, 2 s leer.
- Versionierte Übergabe für neue Chats: [CONTINUE-HERE](CONTINUE-HERE.md). Keine privaten Chatprotokolle, Fahrten oder Schlüssel in Git.

[HRV und Export](hrv-measurement.md) · [Fachquellen und Grenzen](hrv-artifact-research.md) · [Dreeve](dreeve-import.md) · [Textscroller-Konvention](text-scrollers.md).

Optimierter Release über GitHub Actions mit dem dauerhaften Signierschlüssel der Builds 53/55. Physische Gurt-/Telefonprüfung und echte Dienstkonten sind zusätzlich zur automatisierten Prüfung erforderlich.
