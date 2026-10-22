# HRV4Training-CSV-Import

Stand 22.10.2026. Daten & Speicher → HRV-Pulsdaten → **HRV4Training-CSV importieren**. Android-Dateiauswahl, danach schreibfreie Vorschau mit Anzahl von Messwert-/Tagesdatensätzen, Zeitraum, Datumsformat, Gerätezeitzone und fehlerhaften Datensätzen. Erst „Importieren“ schreibt auf dem vorhandenen seriellen HRV-Worker. Kein Upload und keine zusätzliche Abhängigkeit.

[Native Vorschau mit synthetischen Testdaten](previews/hrv4training-import.png).

## Dateiformat und Datum

UTF-8, optional BOM, Komma als Trenner, Leerzeichen in Kopfzeilen, LF/CRLF, zitierte Kommas/Zeilenumbrüche und doppelte Anführungszeichen. Zusätzliche leere Abschlussfelder werden toleriert, nichtleere überzählige Spalten nicht still abgeschnitten. Limits: 4 MiB, 10.000 Datensätze, 256 benannte Spalten, insgesamt 250.000 Zellen und 16.384 Zeichen je Feld. Ungültige CSV-Struktur verhindert jeden Schreibvorgang; einzelne ungültige Datenzeilen werden ausgewiesen und übersprungen.

Der untersuchte Android-Export verwendet **Jahr–Tag–Monat**, trotz ISO-ähnlichem Aussehen. Erkennung über die gesamte Datei statt separat je Zeile; dadurch werden frühere mehrdeutige Tage nicht als anderer Monat importiert. ISO Jahr–Monat–Tag ebenfalls unterstützt. Bei ausschließlich mehrdeutigen Tagen erscheint ein Hinweis mit veränderbarer Formatwahl. Widersprüchlich eindeutige Formate werden abgelehnt. `local` ist maßgeblich, ersatzweise `date`.

Die CSV enthält keine Zeitzone. Vorschau zeigt die aktuelle Gerätezeitzone als Annahme; diese wird pro Eintrag gespeichert. Fehlende Messuhrzeit bleibt ausdrücklich unbekannt. Der intern notwendige Mitternachtszeitpunkt ist keine behauptete Messuhrzeit; Historie und Detail zeigen dann nur das Datum.

## Übernahme und Grenzen

Bekannte HRV-Kennzahlen: rMSSD, SDNN und mittleres NN in ms, pNN50 in Prozent sowie Puls in bpm. Fehlende Werte bleiben fehlend. LF/HF werden nicht ungeprüft als ms² ausgegeben; ihre Einheit ist im CSV nicht angegeben. Recovery Points, Signalqualität und sämtliche weiteren Spaltenwerte bleiben als gekennzeichnete Originalfelder erhalten. Auch Tage ohne Messung werden als Tagesdatensätze bewahrt.

Originale subjektive Skalen einschließlich Dezimalwerten bleiben unverändert in `external.originalColumns`; keine stillschweigende Rundung auf die neun BICYCLE-Sliderstufen. Direkt kompatible Angaben werden zusätzlich in das editierbare Tagesbefinden übernommen: Trainingskategorie, ganzzahlige RPE, Trainingsdistanz (m → km), Trainingsdauer (s → min), Notiz, Reisen, Krankheit, Alkohol sowie passende Zusatzfelder. Überlange oder nicht kompatible Angaben bleiben vollständig im Original erhalten. Import verändert weder globale Frageauswahl noch Fahrerprofil. Trainingsangaben erzeugen keine Fahrten und werden nicht erneut aus der lokalen Fahrtenbibliothek vorbefüllt.

Es gibt keine RR-/NN-Rohdaten, Sensor-/Protokollinformationen, Messdauer oder Korrekturhistorie. Keine erfundenen Rohdaten, BICYCLE-Artefaktfreigaben oder Abdeckungswerte. Externe Einträge haben eine eigene Detaildarstellung und Herkunft in der Historie; sie werden nicht als fehlgeschlagene eigene Messungen dargestellt. Ein fremdes Signalqualitätslabel ersetzt keine eigene Qualitätsprüfung. Bestehende Messungen/Berechnungen bleiben unverändert.

## Speicherung, Dubletten und Sicherung

Stabile UUID aus Quelle, lokalem Datum und gegebenenfalls Uhrzeit. Erneuter Import überspringt bestehende Identitäten, auch bei geändertem CSV-Inhalt oder zwischenzeitlich bearbeitetem Tagesbefinden. Keine Überschreibung oder Vermischung mit eigenen Messungen. Neue Einträge werden zunächst in einem unsichtbaren Staging-Verzeichnis geschrieben und erst vollständig atomar ins Journal verschoben. Unterbrochene Staging-Verzeichnisse werden beim Start bereinigt.

Ergebnis zählt importierte, vorhandene, ungültige und bei Speicherfehler verbleibende Datensätze getrennt. Bereits gespeicherte Teilerfolge werden nicht als vollständig fehlgeschlagener Import ausgegeben. Vorhandene vollständige Verzeichnissicherung und JSON-Analyseexport nehmen Originalfelder und getrennte Antworten mit. Schema 1 bleibt erhalten, externe Daten ergänzen den `external`-Block; native `result.json` werden nicht umgeschrieben.

## Prüfstand

18 unterschiedliche gezielte Prüfungen erfolgreich: sieben Importtests, zwei bestehende HRV-Speichertests, ein bestehender Historientest, sieben bestehende Tagesbefinden-Tests sowie eine zusätzliche lokale Prüfung des bereitgestellten Originalexports. Letztere und die private CSV sind keine Repository-Fixtures. Commitbare Tests verwenden ausschließlich synthetische Daten. Geprüft: Datumsreihenfolge, CSV-Sonderzeichen/Leerfelder, Größen-/UTF-8-Grenzen, Dublettenschutz einschließlich editierter Antworten, Staging-Recovery, Backup-/Analyseexport, Streaming-Historie, Teilerfolge bei Speicherfehlern, echter Daten-Menü-/Dateiauswahlpfad und schreibfreie Vorschau. Kein Gesamttestlauf und keine neue APK.

## Abgrenzung zur Score-Diskussion

Keine aus einem persönlichen Export abgeleitete Alterskorrektur als allgemeine Formel eingebaut. HRV4Training-Originalscore wird nur bewahrt. lnRMSSD ist bereits Bestandteil der nativen BICYCLE-Metriken. Diskussion zu wissenschaftlich begründeten Kennwerten: [hrv-score-plan.md](hrv-score-plan.md).
